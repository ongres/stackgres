#!/bin/sh

set -e

if [ -z "$UPSTREAM_NAME" ]
then
  >&2 echo "Must set UPSTREAM_NAME env var"
  exit 1
fi

if [ -z "$UPSTREAM_GIT_URL" ]
then
  >&2 echo "Must set UPSTREAM_GIT_URL env var"
  exit 1
fi

if [ -z "$FORK_GIT_URL" ]
then
  >&2 echo "Must set FORK_GIT_URL env var"
  exit 1
fi

if [ -z "$PROJECT_NAME" ]
then
  >&2 echo "Must set PROJECT_NAME env var"
  exit 1
fi

PROJECT_PATH=../../../../

cd "$(dirname "$0")"

STACKGRES_VERSION="${STACKGRES_VERSION:-$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)}"

mkdir -p target
UPSTREAM_SUFFIX="$(printf %s "$UPSTREAM_NAME" | tr '[A-Z] ' '[a-z]-' | tr -dc '[a-z0-9]-')"
UPSTREAM_GIT_PATH=target/"upstream-$UPSTREAM_SUFFIX"
FORK_GIT_PATH=target/"fork-$UPSTREAM_SUFFIX"
HELM_OPERATOR_BUNDLE_IMAGE_TAG="${STACKGRES_VERSION}$HELM_OPERATOR_BUNDLE_IMAGE_TAG_SUFFIX"

if [ -d "$UPSTREAM_GIT_PATH" ] && git -C "$UPSTREAM_GIT_PATH" remote -v | grep -qF "$UPSTREAM_GIT_URL"
then
  echo "Resetting Upstream $UPSTREAM_NAME from $UPSTREAM_GIT_URL"
  git -C "$UPSTREAM_GIT_PATH" fetch
  git -C "$UPSTREAM_GIT_PATH" reset --hard HEAD
  git -C "$UPSTREAM_GIT_PATH" checkout main
  git -C "$UPSTREAM_GIT_PATH" reset --hard origin/main
else
  echo "Cloning Upstream $UPSTREAM_NAME from $UPSTREAM_GIT_URL"
  rm -rf "$UPSTREAM_GIT_PATH"
  git clone "$UPSTREAM_GIT_URL" "$UPSTREAM_GIT_PATH"
fi

if [ -d "$FORK_GIT_PATH" ] && git -C "$FORK_GIT_PATH" remote -v | grep -qF "$FORK_GIT_URL"
then
  echo "Resetting OperatorHub fork for StackGres from $FORK_GIT_URL"
  if ! git -C "$FORK_GIT_PATH" remote -v | tr -s '[:blank:]' ' ' | grep -qxF "upstream $UPSTREAM_GIT_URL (fetch)"
  then
    git -C "$FORK_GIT_PATH" remote add upstream "$UPSTREAM_GIT_URL"
  fi
  git -C "$FORK_GIT_PATH" fetch upstream
  git -C "$FORK_GIT_PATH" reset --hard HEAD
  git -C "$FORK_GIT_PATH" checkout main
  git -C "$FORK_GIT_PATH" reset --hard upstream/main
else
  echo "Cloning OperatorHub fork for StackGres from $FORK_GIT_URL"
  rm -rf "$FORK_GIT_PATH"
  git clone "$FORK_GIT_URL" "$FORK_GIT_PATH"
fi

if [ "$(git -C "$FORK_GIT_PATH" rev-list --max-parents=0 HEAD)" != "$(git -C "$UPSTREAM_GIT_PATH" rev-list --max-parents=0 HEAD)" ]
then
  >&2 echo "Git repository $FORK_GIT_URL seems not a fork of $UPSTREAM_GIT_URL"
  exit 1
fi

echo "Copying new files to path operators/$PROJECT_NAME/$STACKGRES_VERSION from quay.io/stackgres/helm-operator-bundle:$HELM_OPERATOR_BUNDLE_IMAGE_TAG"
(
rm -rf "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
mkdir -p "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
cd "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
docker pull quay.io/stackgres/helm-operator-bundle:$HELM_OPERATOR_BUNDLE_IMAGE_TAG
docker save quay.io/stackgres/helm-operator-bundle:$HELM_OPERATOR_BUNDLE_IMAGE_TAG | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -F layer.tar \
  | while read LAYER
    do
      docker save quay.io/stackgres/helm-operator-bundle:$HELM_OPERATOR_BUNDLE_IMAGE_TAG | tar xO "$LAYER" | tar xv
    done
)
find "$FORK_GIT_PATH" -name '.wh*' \
  | while read FILE
    do
      rm "$FILE"
    done
cp ci-"$UPSTREAM_SUFFIX".yaml "$FORK_GIT_PATH/operators/$PROJECT_NAME/ci.yaml"
git -C "$FORK_GIT_PATH" add .
git -C "$FORK_GIT_PATH" status

if [ "$DO_PIN_IMAGES" = true ]
then
  echo "Pinning images:"
  echo
  (
  cd "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
  IMAGES="$(grep 'image:' "manifests/stackgres.clusterserviceversion.yaml" | tr -d ' ' | cut -d : -f 2-)"
  echo "$IMAGES" \
    | while read -r IMAGE
      do
        DIGEST="$(docker buildx imagetools inspect "$IMAGE" | grep '^Digest:' | tr -d ' ' | cut -d : -f 2-)"
        IMAGE_NAME="${IMAGE%%:*}"
        IMAGE_NAME="${IMAGE_NAME%%@sha256}"
        echo "Pinning $IMAGE to $IMAGE_NAME@$DIGEST"
        sed -i "s#image: $IMAGE\$#image: $IMAGE_NAME@$DIGEST#" "manifests/stackgres.clusterserviceversion.yaml"
      done
  echo
  )
  git -C "$FORK_GIT_PATH" diff
  echo "Pinning done!"
fi

git -C "$FORK_GIT_PATH" add .
git -C "$FORK_GIT_PATH" status
echo "Creating commit"
git -C "$FORK_GIT_PATH" commit -m "operator $PROJECT_NAME (${STACKGRES_VERSION})"
echo "To push use the following command"
echo git -C "$PROJECT_PATH"/stackgres-k8s/install/operator-sdk/stackgres-operator/"$FORK_GIT_PATH" push
echo
if [ "$UPSTREAM_GIT_URL" != "${UPSTREAM_GIT_URL#https://github.com}" ]
then
  if [ "$FORK_GIT_URL" != "${FORK_GIT_URL#https://github.com}" ]
  then
    echo "To create the PR go to: $UPSTREAM_GIT_URL/compare/main...$(printf %s "$FORK_GIT_URL" | cut -d / -f 4):$(printf %s "$FORK_GIT_URL" | cut -d / -f 5):main?expand=1"
  fi
  if [ "$FORK_GIT_URL" != "${FORK_GIT_URL#git@github.com}" ]
  then
    echo "To create the PR go to: $UPSTREAM_GIT_URL/compare/main...$(printf %s "$FORK_GIT_URL" | cut -d / -f 1 | cut -d : -f 2):$(printf %s "$FORK_GIT_URL" | cut -d / -f 2 | cut -d . -f 1):main?expand=1"
  fi
fi
