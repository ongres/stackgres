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

grep "^version: \"$STACKGRES_VERSION\"$" stackgres-operator/Chart.yaml
grep "^appVersion: \"$STACKGRES_VERSION\"$" stackgres-operator/Chart.yaml
grep "^operator:$" -A 5 stackgres-operator/values.yaml | grep "^    tag: \"$IMAGE_TAG\"$"
grep "^restapi:$" -A 5 stackgres-operator/values.yaml | grep "^    tag: \"$IMAGE_TAG\"$"
grep "^adminui:$" -A 5 stackgres-operator/values.yaml | grep "^    tag: \"$ADMINUI_IMAGE_TAG\"$"
helm lint stackgres-operator

grep "^version: \"$STACKGRES_VERSION\"$" stackgres-cluster/Chart.yaml
grep "^appVersion: \"$STACKGRES_VERSION\"$" stackgres-cluster/Chart.yaml
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
