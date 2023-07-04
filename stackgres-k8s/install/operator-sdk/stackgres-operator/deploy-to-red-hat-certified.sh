#!/bin/sh

set -e

if [ "$#" -lt 1 ]
then
  >&2 echo "Must specify git URL"
  exit 1
fi

GIT_URL="$1"

PROJECT_PATH=../../../../

cd "$(dirname "$0")"

STACKGRES_VERSION="${STACKGRES_VERSION:-$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)}"

mkdir -p target
echo "Cloning Upstream Red Hat Certified"
rm -rf target/upstream-red-hat-certified
git clone https://github.com/redhat-openshift-ecosystem/community-operators-prod target/upstream-red-hat-certified
echo "Cloning Red Hat Certified fork for StackGres from $GIT_URL"
rm -rf target/red-hat-certified
git clone "$GIT_URL" target/red-hat-certified
if [ "$(git -C target/red-hat-certified rev-list --max-parents=0 HEAD)" != "$(git -C target/upstream-red-hat-certified rev-list --max-parents=0 HEAD)" ]
then
  >&2 echo "Git repository $GIT_URL seems not a fork of https://github.com/redhat-openshift-ecosystem/certified-operators"
  exit 1
fi

echo "Copying new files to path operators/stackgres/$STACKGRES_VERSION from quay.io/stackgres/helm-operator-bundle:$STACKGRES_VERSION-openshift"
(
mkdir "target/red-hat-certified/operators/stackgres/$STACKGRES_VERSION"
cd "target/red-hat-certified/operators/stackgres/$STACKGRES_VERSION"
docker pull quay.io/stackgres/helm-operator-bundle:$STACKGRES_VERSION-openshift
docker save quay.io/stackgres/helm-operator-bundle:$STACKGRES_VERSION-openshift | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -F layer.tar \
  | while read LAYER
    do
      docker save quay.io/stackgres/helm-operator-bundle:$STACKGRES_VERSION-openshift | tar xO "$LAYER" | tar xv
    done
)
git -C target/red-hat-certified add .
git -C target/red-hat-certified status

echo "Pinning images:"
echo
(
cd "target/red-hat-certified/operators/stackgres/$STACKGRES_VERSION"
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
git -C target/red-hat-certified diff
echo "Pinning done!"

git -C target/red-hat-certified add .
git -C target/red-hat-certified status
echo "Creating commit"
git -C target/red-hat-certified commit -m "operator stackgres (${STACKGRES_VERSION})"
echo "To push use the following command"
echo git -C "$PROJECT_PATH"/stackgres-k8s/install/operator-sdk/stackgres-operator/target/red-hat-certified push
echo
echo "To create the PR go to: https://github.com/redhat-openshift-ecosystem/certified-operators/compare"