#!/bin/sh

set -ex

cd "$(dirname "$0")"

STACKGRES_VERSION=$(grep '<artifactId>stackgres-parent</artifactId>' "../../src/pom.xml" -A 2 -B 2 \
 | grep -o '<version>\([^<]\+\)</version>' | tr '<>' '  ' | cut -d ' ' -f 3)

mkdir -p "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION"
cat << EOF > "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/demo-operator.yml"
apiVersion: v1
kind: Namespace
metadata:
  name: stackgres
---
EOF
for CRD in stackgres-operator/crds/*.yaml
do
  cat "$CRD" >> "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/demo-operator.yml"
  echo --- >> "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/demo-operator.yml"
done
helm repo add stable https://kubernetes-charts.storage.googleapis.com
helm repo update
helm dependency update stackgres-operator
helm dependency update stackgres-cluster

helm template --namespace stackgres stackgres-operator \
  stackgres-operator \
  --set-string adminui.service.type=LoadBalancer \
  >> "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/demo-operator.yml"

helm template --namespace default simple \
  stackgres-cluster \
  --set nonProductionOptions.createMinio=false \
  > "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/demo-simple-config.yml"

helm template --namespace default simple \
  stackgres-cluster \
  --set config.create=false \
  --set profiles=null \
  --set cluster.instances=2 \
  --set nonProductionOptions.createMinio=false \
  > "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/demo-simple-cluster.yml"

rm -rf target/minio
helm fetch stable/minio \
  --version 5.0.26 \
  --untar --untardir target
helm template minio \
  target/minio \
  --set buckets[0].name=stackgres,buckets[0].policy=none,buckets[0].purge=true \
  > "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION/demo-minio.yml"

