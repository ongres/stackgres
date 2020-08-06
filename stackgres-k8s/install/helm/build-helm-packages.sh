#!/bin/sh

set -e

cd "$(dirname "$0")"

STACKGRES_VERSION=$(grep '<artifactId>stackgres-parent</artifactId>' "../../src/pom.xml" -A 2 -B 2 \
 | grep -o '<version>\([^<]\+\)</version>' | tr '<>' '  ' | cut -d ' ' -f 3)

grep -F "version: $STACKGRES_VERSION" stackgres-operator/Chart.yaml
grep -F "appVersion: $STACKGRES_VERSION" stackgres-operator/Chart.yaml
helm lint stackgres-operator

grep -F "version: $STACKGRES_VERSION" stackgres-cluster/Chart.yaml
grep -F "appVersion: $STACKGRES_VERSION" stackgres-cluster/Chart.yaml
helm lint stackgres-cluster

helm repo add stable https://kubernetes-charts.storage.googleapis.com
helm repo update
helm dependency update stackgres-operator
helm dependency update stackgres-cluster
mkdir -p "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION"
tar czf "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/helm-operator.tgz" -C stackgres-operator .
tar czf "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/helm-cluster.tgz" -C stackgres-cluster .
