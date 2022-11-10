#!/bin/sh

set -e

cd "$(dirname "$0")"

STACKGRES_VERSION=$(grep '<artifactId>stackgres-parent</artifactId>' "../../src/pom.xml" -A 2 -B 2 \
 | grep -o '<version>\([^<]\+\)</version>' | tr '<>' '  ' | cut -d ' ' -f 3)
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

helm schema-gen stackgres-operator/values.yaml > stackgres-operator/values.schema.json
helm lint stackgres-operator

grep "^version: \"$STACKGRES_VERSION\"$" stackgres-cluster/Chart.yaml
grep "^appVersion: \"$STACKGRES_VERSION\"$" stackgres-cluster/Chart.yaml

helm schema-gen stackgres-cluster/values.yaml > stackgres-cluster/values.schema.json
helm lint stackgres-cluster

rm -rf "target/packages"
mkdir -p "target/packages"
helm package stackgres-operator -d "target/packages"
mv "target/packages/stackgres-operator-$STACKGRES_VERSION.tgz" target/packages/stackgres-operator.tgz
helm package stackgres-cluster -d "target/packages"
mv "target/packages/stackgres-cluster-$STACKGRES_VERSION.tgz" target/packages/stackgres-cluster-demo.tgz
mkdir -p "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION"
rm -rf "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/helm"
cp -a target/packages "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/helm"
