#!/bin/sh

set -e

cd "$(dirname "$0")"

STACKGRES_VERSION="$(sh ../../ci/build/version.sh)"

rm -Rf "target/stackgres-operator" "target/stackgres-cluster" "target/templates"
mkdir -p "target"

# Fix for Error: chart requires kubeVersion: X which is incompatible with Kubernetes Y
cp -a stackgres-operator "target/."
cp -a stackgres-cluster "target/."
sed -i '/^kubeVersion: .*$/d' "target/stackgres-operator/Chart.yaml"
sed -i '/^kubeVersion: .*$/d' "target/stackgres-cluster/Chart.yaml"
find target/stackgres-operator -type l \
  | while read LINK
    do
      ln -f -s "../$(readlink "$LINK")" "$LINK"
    done
find target/stackgres-cluster -type l \
  | while read LINK
    do
      ln -f -s "../$(readlink "$LINK")" "$LINK"
    done

mkdir -p "target/templates"

helm template --namespace stackgres stackgres-operator \
  "target/stackgres-operator" \
  --set skipInstallConfig=true \
  --kube-version 1.31 \
  --set-string adminui.service.type=LoadBalancer \
  > "target/templates/stackgres-operator-demo-template.yml"

(
  BLOCK_SEPARATOR_INDEXES="$(grep '^---$' -n "target/templates/stackgres-operator-demo-template.yml")"
  BLOCK_SEPARATOR_INDEXES="$(printf '%s' "$BLOCK_SEPARATOR_INDEXES" | cut -d : -f 1)"
  INDEX=1
  PREVIOUS_BLOCK_SEPARATOR_INDEX=
  while true
  do
    BLOCK_SEPARATOR_INDEX="$(printf '%s' "$BLOCK_SEPARATOR_INDEXES" | head -n "$INDEX" | tail -n 1)"
    if [ "$BLOCK_SEPARATOR_INDEX" = "$PREVIOUS_BLOCK_SEPARATOR_INDEX" ]
    then
      tail -n +"$((PREVIOUS_BLOCK_SEPARATOR_INDEX+1))" "target/templates/stackgres-operator-demo-template.yml" \
        > "target/templates/stackgres-operator-demo-template-$INDEX.yml"
      break
    fi
    if [ -n "$PREVIOUS_BLOCK_SEPARATOR_INDEX" ]
    then
      tail -n +"$((PREVIOUS_BLOCK_SEPARATOR_INDEX+1))" "target/templates/stackgres-operator-demo-template.yml" \
        | head -n "$((BLOCK_SEPARATOR_INDEX-PREVIOUS_BLOCK_SEPARATOR_INDEX-1))" \
        > "target/templates/stackgres-operator-demo-template-$INDEX.yml"
    fi
    INDEX="$((INDEX+1))"
    PREVIOUS_BLOCK_SEPARATOR_INDEX="$BLOCK_SEPARATOR_INDEX"
  done
)

{
  cat << EOF
---
apiVersion: v1
kind: Namespace
metadata:
  name: stackgres
EOF
  for CRD in ../../src/common/src/main/resources/crds/*.yaml
  do
    printf '\n%s\n' '---'
    cat "$CRD"
  done
  for RESOURCE in target/templates/stackgres-operator-demo-template-*.yml
  do
    if [ "$(yq -r '[.]
      | any(
          (
            has("metadata")
            and (.metadata|has("annotations"))
            and (.metadata.annotations|has("helm.sh/hook"))
            | not)
          or (.metadata.annotations["helm.sh/hook"]
            | match("install"))
        )' "$RESOURCE")" = false ]
    then
      continue
    fi
    printf '\n%s\n' '---'
    cat "$RESOURCE"
  done
} > "target/templates/stackgres-operator-demo.yml"

helm template --namespace default simple \
  "target/stackgres-cluster" \
  --kube-version 1.27 \
  --set configurations.create=true \
  --set cluster.create=false \
  > "target/templates/stackgres-simple-config-demo.yml"

helm template --namespace default simple \
  "target/stackgres-cluster" \
  --kube-version 1.27 \
  --set configurations.create=false \
  --set cluster.create=true \
  --set profiles=null \
  --set cluster.sgInstanceProfile=size-s \
  --set cluster.instances=2 \
  --set instanceProfiles=null \
  --set nonProductionOptions.disableClusterPodAntiAffinity=true \
  > "target/templates/stackgres-simple-cluster-demo.yml"

rm -rf target/minio

helm template --namespace default minio \
  ../../e2e/helm/minio-8.0.10.tgz \
  --kube-version 1.27 \
  --set buckets[0].name=stackgres,buckets[0].policy=none,buckets[0].purge=true \
  | grep -v '^ \+namespace: "\?default"\?$' \
  > "target/templates/minio-demo.yml"

mkdir -p "target/public/downloads/stackgres-k8s/stackgres"
rm -rf "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION"
cp -a target/templates "target/public/downloads/stackgres-k8s/stackgres/$STACKGRES_VERSION"
