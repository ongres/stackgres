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
OPERATOR_BUNDLE_IMAGE_TAG="${STACKGRES_VERSION}$OPERATOR_BUNDLE_IMAGE_TAG_SUFFIX"

if ! [ -d "$UPSTREAM_GIT_PATH" ] || ! git -C "$UPSTREAM_GIT_PATH" remote -v | tr -s '[:blank:]' ' ' | grep -qF "origin $UPSTREAM_GIT_URL "
then
  echo "Cloning Upstream $UPSTREAM_NAME from $UPSTREAM_GIT_URL"
  rm -rf "$UPSTREAM_GIT_PATH"
  git clone "$UPSTREAM_GIT_URL" "$UPSTREAM_GIT_PATH"
fi

echo "Resetting Upstream $UPSTREAM_NAME from $UPSTREAM_GIT_URL"
git -C "$UPSTREAM_GIT_PATH" fetch
git -C "$UPSTREAM_GIT_PATH" reset --hard HEAD
git -C "$UPSTREAM_GIT_PATH" checkout main
git -C "$UPSTREAM_GIT_PATH" reset --hard origin/main
git -C "$UPSTREAM_GIT_PATH" stash save --keep-index --include-untracked
git -C "$UPSTREAM_GIT_PATH" stash drop || true

if ! [ -d "$FORK_GIT_PATH" ] || ! git -C "$FORK_GIT_PATH" remote -v | tr -s '[:blank:]' ' ' | grep -qF "origin $FORK_GIT_URL "
then
  echo "Cloning OperatorHub fork for StackGres from $FORK_GIT_URL"
  rm -rf "$FORK_GIT_PATH"
  git clone "$FORK_GIT_URL" "$FORK_GIT_PATH"
fi

echo "Resetting OperatorHub fork for StackGres from $FORK_GIT_URL"
if ! git -C "$FORK_GIT_PATH" remote -v | tr -s '[:blank:]' ' ' | grep -qF "upstream $UPSTREAM_GIT_URL "
then
  git -C "$FORK_GIT_PATH" remote add upstream "$UPSTREAM_GIT_URL"
fi
git -C "$FORK_GIT_PATH" fetch upstream
git -C "$FORK_GIT_PATH" reset --hard HEAD
git -C "$FORK_GIT_PATH" checkout main
git -C "$FORK_GIT_PATH" reset --hard upstream/main
git -C "$FORK_GIT_PATH" stash save --keep-index --include-untracked
git -C "$FORK_GIT_PATH" stash drop || true

if [ "$(git -C "$FORK_GIT_PATH" rev-list --max-parents=0 HEAD)" != "$(git -C "$UPSTREAM_GIT_PATH" rev-list --max-parents=0 HEAD)" ]
then
  >&2 echo "Git repository $FORK_GIT_URL seems not a fork of $UPSTREAM_GIT_URL"
  exit 1
fi

show_push_and_pr_instructions() {
  echo
  echo "To push use the following command"
  echo
  echo git -C "$PROJECT_PATH"/stackgres-k8s/install/operator-sdk/stackgres-operator/"$FORK_GIT_PATH" push -f
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
}

# Onboarding to file-based catalogs is a one-time migration of the ALREADY
# PUBLISHED catalog: 'make fbc-onboarding' renders the existing released bundles
# (whose images live in registry.connect.redhat.com) into catalog templates and
# rendered catalogs, and adds NO new version. It must be merged as its own PR,
# and tested, before any new version is added. Set ONBOARDING_DONE=true in the
# deploy-to-*.sh script once that PR is merged so subsequent runs add versions
# instead of repeating the onboarding.
if [ "$DO_ADD_FBC" = true ] && [ "$ONBOARDING_DONE" != true ]
then
  echo "Onboarding $PROJECT_NAME to file-based catalogs"
  cp ci-"$UPSTREAM_SUFFIX".yaml "$FORK_GIT_PATH/operators/$PROJECT_NAME/ci.yaml"
  wget https://raw.githubusercontent.com/redhat-openshift-ecosystem/operator-pipelines/main/fbc/Makefile -O "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  sed -i 's/podman run/docker run/' "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  # Upstream Makefile bug: '--user $(id -u):$(id -g)' is consumed by make as
  # (undefined) variables and becomes 'docker run --user :', so the container
  # runs as root and writes root-owned files. Double the '$' so the shell does
  # the substitution instead of make.
  sed -i 's/--user $(id -u):$(id -g)/--user $$(id -u):$$(id -g)/' "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  # The Makefile mounts the registry credentials under /root, but the container
  # now runs as the host user (uid != 0) which cannot traverse the root-owned
  # /root (mode 700), so opm falls back to anonymous and gets 401 on
  # registry.redhat.io. Mount the credentials under $HOME instead and point opm
  # there via HOME.
  sed -i 's#/root/.docker/config.json#$${HOME}/.docker/config.json#g' "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  sed -i 's#--security-opt label=disable#--security-opt label=disable -e HOME=$${HOME}#' "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  make -C "$FORK_GIT_PATH/operators/$PROJECT_NAME" fbc-onboarding
  # Onboarding embeds each bundle's full manifests ("olm.bundle.object"), so the
  # StackGres catalogs (huge CRDs x many versions) blow past GitHub's 100 MB
  # per-file limit. Re-render to the compact "olm.csv.metadata" form (CRDs are
  # pulled from the bundle image at install time); OLM and the Red Hat pipelines
  # accept it. e.g. community v4.16 drops from ~150 MB to ~42 MB.
  for CATALOG_DIR in "$FORK_GIT_PATH"/catalogs/v4.*/"$PROJECT_NAME"
  do
    [ -d "$CATALOG_DIR" ] || continue
    opm render "$CATALOG_DIR" --migrate-level=bundle-object-to-csv-metadata --output=yaml \
      > "$CATALOG_DIR/catalog.yaml.new"
    mv "$CATALOG_DIR/catalog.yaml.new" "$CATALOG_DIR/catalog.yaml"
  done
  git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/catalog-templates"
  git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/ci.yaml"
  git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/Makefile"
  git -C "$FORK_GIT_PATH" add "$PWD/$FORK_GIT_PATH/catalogs"/v4.*/"$PROJECT_NAME"
  git -C "$FORK_GIT_PATH" status
  git -C "$FORK_GIT_PATH" commit -s -m "onboarding $PROJECT_NAME to file-based catalogs"
  git -C "$FORK_GIT_PATH" reset --hard HEAD
  sed -i 's/^ONBOARDING_DONE=.*$/ONBOARDING_DONE=true/' "$0"
  show_push_and_pr_instructions
  exit 0
fi

if [ "x$PREVIOUS_VERSION" != xnone ]
then
  PREVIOUS_VERSION="${PREVIOUS_VERSION:-$(
    ls -1d "$FORK_GIT_PATH/operators/$PROJECT_NAME"/*/manifests \
      | cut -d / -f 5 | grep -v '.-rc.' | sort -t ' ' -k 1Vr | head -n 1)}"
  if [ ! -d "$FORK_GIT_PATH/operators/$PROJECT_NAME/$PREVIOUS_VERSION" ] || [ "x$PREVIOUS_VERSION" = x ]
  then
    echo "Can not detect previous version. Set environment variable PREVIOUS_VERSION to set the previous version, or set it to "none" if no previous version is available"
    exit 1
  fi
fi

echo "Copying new files to path operators/$PROJECT_NAME/$STACKGRES_VERSION from quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG"
(
rm -rf "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
mkdir -p "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
cd "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
docker pull quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG
if docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -qF layer.tar
then
  docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -F layer.tar \
    | while read LAYER
      do
        docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar xO "$LAYER" | tar xzv
      done
else
  docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -F manifest.json \
    | while read MANIFEST
      do
        docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar xO "$MANIFEST" | jq -r '.[]|.Layers[]' \
          | while read LAYER
            do
              docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar xO "$LAYER" | tar xzv
            done
      done
fi
)
find "$FORK_GIT_PATH" -name '.wh*' \
  | while read FILE
    do
      rm "$FILE"
    done
# Non-FBC projects seed ci.yaml from the template. FBC projects keep the ci.yaml
# produced by onboarding (it carries the fbc catalog_mapping), so leave it alone.
if [ "$DO_ADD_FBC" != true ]
then
  cp ci-"$UPSTREAM_SUFFIX".yaml "$FORK_GIT_PATH/operators/$PROJECT_NAME/ci.yaml"
fi

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
        if [ -z "$DIGEST" ]
        then
          echo "Digest not found for image $IMAGE"
          exit 1
        fi
        IMAGE_NAME="${IMAGE%%:*}"
        IMAGE_NAME="${IMAGE_NAME%%@sha256}"
        echo "Pinning $IMAGE to $IMAGE_NAME@$DIGEST"
        sed -i "s#\([iI]\)mage: $IMAGE\$#\1mage: $IMAGE_NAME@$DIGEST#" "manifests/stackgres.clusterserviceversion.yaml"
      done
  echo
  )
  git -C "$FORK_GIT_PATH" diff | cat
  echo "Pinning done!"
fi

if command -v deploy_extra_steps > /dev/null 2>&1
then
  deploy_extra_steps
fi

if [ "x$PREVIOUS_VERSION" != xnone ]
then
  if [ ! -d "$FORK_GIT_PATH/operators/$PROJECT_NAME/$PREVIOUS_VERSION" ] || [ "x$PREVIOUS_VERSION" = x ]
  then
    echo "Can not detect previous version. Set environment variable PREVIOUS_VERSION to set the previous version, or set it to "none" if no previous version is available"
    exit 1
  fi
  if command -v set_previous_version_override > /dev/null 2>&1
  then
    set_previous_version_override
  else
    echo "Setting replaces to stackgres.v$PREVIOUS_VERSION"
    sed -i "s/^\( *\)\(version: $STACKGRES_VERSION\)$/\1\2\n\1replaces: stackgres.v$PREVIOUS_VERSION/" \
      "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml
  fi
fi

if [ "$DO_ADD_FBC" = true ]
then
  # Generate release-config.yaml to drive Red Hat's FBC auto-release. On merge,
  # the pipeline builds and publishes the bundle image, then opens a follow-up PR
  # that adds this bundle to the listed catalog templates (one per supported OCP
  # version, produced by onboarding) in the given channels. We deliberately do
  # NOT render catalogs here: the pipeline renders them against the certified
  # image it just published (registry.connect.redhat.com), which is the only
  # image with the correct package name.
  # Channels the version belongs to, by the pre-release types each one admits when
  # searching for the previous version to replace:
  #   stable    -> GA only
  #   candidate -> GA + rc
  #   fast      -> GA + rc + alpha + beta
  # A version is added to every channel whose regexp it matches.
  STABLE_REGEXP='^[0-9]\+\.[0-9]\+\.[0-9]\+$'
  CANDIDATE_REGEXP='^[0-9]\+\.[0-9]\+\.[0-9]\+\(-rc[0-9.]*\)\?$'
  FAST_REGEXP='^[0-9]\+\.[0-9]\+\.[0-9]\+\(-\(alpha\|beta\|rc\)[0-9.]*\)\?$'
  CHANNELS="$(sh channels.sh "$STACKGRES_VERSION" \
    "stable:$STABLE_REGEXP" \
    "fast:$FAST_REGEXP" \
    "candidate:$CANDIDATE_REGEXP")"
  # Print the head bundle of a channel in a catalog template (the entry that no
  # other entry replaces or skips). That is the version the new bundle must
  # replace in that channel; deriving it from the template (the source of truth
  # for each channel's contents) keeps every channel to a single head. Empty when
  # the channel is absent from the template.
  channel_head() {
    yq -r --arg ch "$1" '
      .entries[]
      | select(.schema == "olm.channel" and .name == $ch)
      | .entries as $e
      | (($e | map(.replaces // empty)) + ($e | map(.skips // []) | add // [])) as $referenced
      | (($e | map(.name)) - $referenced)
      | .[0] // empty
    ' "$2"
  }
  BUNDLE_NAME_PREFIX="$([ "$RENAME_CSV" = true ] && printf %s "$PROJECT_NAME" || printf stackgres)"
  CATALOG_NAMES="$(yq -c \
    '.annotations["com.redhat.openshift.versions"] / "-" | map(sub("^v4\\.";"")|tonumber)|[range(.[0];.[1]+1)]|map("v4." + (.|tostring))' \
    openshift-operator-bundle/metadata/annotations.yaml)"
  RELEASE_CONFIG="$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION/release-config.yaml"
  {
    echo '---'
    echo 'catalog_templates:'
    for CATALOG_NAME in $(printf %s "$CATALOG_NAMES" | yq -r '.[]')
    do
      TEMPLATE_FILE="$FORK_GIT_PATH/operators/$PROJECT_NAME/catalog-templates/$CATALOG_NAME.yaml"
      # Only target templates that onboarding actually produced for this OCP version.
      [ -f "$TEMPLATE_FILE" ] || continue
      # Emit one entry per channel: each channel keeps its own upgrade edge, so the
      # bundle replaces that channel's own head rather than a single shared version
      # (which would fork the graph and leave the channel with multiple heads).
      for CHANNEL in $(printf %s "$CHANNELS" | tr ',' ' ')
      do
        echo "  - template_name: $CATALOG_NAME.yaml"
        echo "    channels:"
        echo "      - $CHANNEL"
        # Determine what this bundle replaces in THIS channel: the channel's own
        # head in the template (unless the caller opted out with PREVIOUS_VERSION=none).
        if [ "x$PREVIOUS_VERSION" = xnone ]
        then
          REPLACES=
        else
          REPLACES="$(channel_head "$CHANNEL" "$TEMPLATE_FILE")"
        fi
        # Set replaces only when the target is present in this template; otherwise
        # the new bundle becomes the channel head in that catalog.
        if [ -n "$REPLACES" ] \
          && grep -qF "name: $REPLACES" "$TEMPLATE_FILE"
        then
          echo "    replaces: $REPLACES"
        fi
      done
    done
  } > "$RELEASE_CONFIG"
  echo "Generated release-config.yaml:"
  cat "$RELEASE_CONFIG"
fi

if [ "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml \
  != "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/"${PROJECT_NAME}.clusterserviceversion.yaml" ]
then
  if [ "$RENAME_CSV" = true ]
  then
    sed -i "s/^  name: stackgres\.v\(.*\)$/  name: ${PROJECT_NAME}.v\1/" \
      "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml
  fi
  mv "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/"${PROJECT_NAME}.clusterserviceversion.yaml"
  sed -i "s/^  operators\.operatorframework\.io\.bundle\.package\.v1: stackgres$/  operators.operatorframework.io.bundle.package.v1: ${PROJECT_NAME}/" \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/metadata/annotations.yaml
fi

# Pass Red Hat YAML checks
find "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION" -name '*.yaml' | xargs -I % sh -c 'yq -y . % > %.new && mv %.new %'

operator-sdk bundle validate "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"

git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/$STACKGRES_VERSION"
git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/ci.yaml"
git -C "$FORK_GIT_PATH" status
git -C "$FORK_GIT_PATH" commit -s -m "operator $PROJECT_NAME (${STACKGRES_VERSION})"
git -C "$FORK_GIT_PATH" reset --hard HEAD
show_push_and_pr_instructions
