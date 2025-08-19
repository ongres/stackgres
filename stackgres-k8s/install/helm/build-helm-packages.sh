#!/bin/sh

set -e

cd "$(dirname "$0")"

STACKGRES_VERSION="$(sh ../../ci/build/version.sh)"
IMAGE_TAG="${STACKGRES_VERSION}"
if [ "${STACKGRES_VERSION##*-}" = "SNAPSHOT" ]
then
  IMAGE_TAG='main\(-[^-]\+\)\?-jvm'
fi
ADMINUI_IMAGE_TAG="${IMAGE_TAG%-jvm}"

mkdir -p stackgres-operator/crds
for CRD in ../../src/common/src/main/resources/crds/*.yaml
do
  cp "$CRD" stackgres-operator/crds/.
done
yq -r '.version' stackgres-operator/Chart.yaml | grep -xF "$STACKGRES_VERSION"
yq -r '.appVersion' stackgres-operator/Chart.yaml | grep -xF "$STACKGRES_VERSION"
yq -r '.operator.image.tag' stackgres-operator/values.yaml | grep "^$IMAGE_TAG$"
yq -r '.restapi.image.tag' stackgres-operator/values.yaml | grep "^$IMAGE_TAG$"
yq -r '.adminui.image.tag' stackgres-operator/values.yaml | grep "^$ADMINUI_IMAGE_TAG$"
helm lint stackgres-operator

yq -r '.version' stackgres-cluster/Chart.yaml | grep -xF "$STACKGRES_VERSION"
yq -r '.appVersion' stackgres-cluster/Chart.yaml | grep -xF "$STACKGRES_VERSION"
helm lint stackgres-cluster

rm -rf target/charts
mkdir -p target/charts
cp -a stackgres-operator target/charts/.
cp -a stackgres-cluster target/charts/.
cp ../operator-sdk/stackgres-operator/config/manifests/bases/stackgres.clusterserviceversion.description.txt \
  target/charts/stackgres-operator/README.md
rm -rf target/packages
mkdir -p target/packages
helm package target/charts/stackgres-operator -d target/packages
mv "target/packages/stackgres-operator-$STACKGRES_VERSION.tgz" target/packages/stackgres-operator.tgz
helm package target/charts/stackgres-cluster -d target/packages
mv "target/packages/stackgres-cluster-$STACKGRES_VERSION.tgz" target/packages/stackgres-cluster-demo.tgz
mkdir -p "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION"
rm -rf "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/helm"
cp -a target/packages "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/helm"
