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
echo "Cloning Upstream OperatorHub"
rm -rf target/upstream-operatorhub
git clone https://github.com/k8s-operatorhub/community-operators target/upstream-operatorhub
echo "Cloning OperatorHub fork for StackGres from $GIT_URL"
rm -rf target/operatorhub
git clone "$GIT_URL" target/operatorhub
if [ "$(git -C target/operatorhub rev-list --max-parents=0 HEAD)" != "$(git -C target/upstream-operatorhub rev-list --max-parents=0 HEAD)" ]
then
  >&2 echo "Git repository $GIT_URL seems not a fork of https://github.com/k8s-operatorhub/community-operators"
  exit 1
fi

echo "Copying new files to path operators/stackgres/$STACKGRES_VERSION from quay.io/stackgres/helm-operator-bundle:$STACKGRES_VERSION-openshift"
(
mkdir "target/operatorhub/operators/stackgres/$STACKGRES_VERSION"
cd "target/operatorhub/operators/stackgres/$STACKGRES_VERSION"
docker pull quay.io/stackgres/helm-operator-bundle:$STACKGRES_VERSION-openshift
docker save quay.io/stackgres/helm-operator-bundle:$STACKGRES_VERSION-openshift | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -F layer.tar \
  | while read LAYER
    do
      docker save quay.io/stackgres/helm-operator-bundle:$STACKGRES_VERSION-openshift | tar xO "$LAYER" | tar xv
    done
)
git -C target/operatorhub add .
git -C target/operatorhub status

# echo "Pinning images:"
# echo
# (
# cd "target/operatorhub/operators/stackgres/$STACKGRES_VERSION"
# IMAGES="$(grep 'image:' "manifests/stackgres.clusterserviceversion.yaml" | tr -d ' ' | cut -d : -f 2-)"
# echo "$IMAGES" \
#   | while read -r IMAGE
#     do
#       DIGEST="$(docker buildx imagetools inspect "$IMAGE" | grep '^Digest:' | tr -d ' ' | cut -d : -f 2-)"
#       IMAGE_NAME="${IMAGE%%:*}"
#       IMAGE_NAME="${IMAGE_NAME%%@sha256}"
#       echo "Pinning $IMAGE to $IMAGE_NAME@$DIGEST"
#       sed -i "s#image: $IMAGE\$#image: $IMAGE_NAME@$DIGEST#" "manifests/stackgres.clusterserviceversion.yaml"
#     done
# echo
# )
# git -C target/operatorhub diff
# echo "Pinning done!"

git -C target/operatorhub add .
git -C target/operatorhub status
echo "Creating commit"
git -C target/operatorhub commit -m "operator stackgres (${STACKGRES_VERSION})"
echo "To push use the following command"
echo git -C "$PROJECT_PATH"/stackgres-k8s/install/operator-sdk/stackgres-operator/target/operatorhub push
echo
echo "To create the PR go to: https://github.com/k8s-operatorhub/community-operators/compare"
