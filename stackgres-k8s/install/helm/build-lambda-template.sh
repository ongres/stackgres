#!/bin/sh

set -e

cd "$(dirname "$0")"

STACKGRES_VERSION="$(grep '<artifactId>stackgres-parent</artifactId>' "../../src/pom.xml" -A 2 -B 2 \
 | grep -o '<version>\([^<]\+\)</version>' | tr '<>' '  ' | cut -d ' ' -f 3)"

rm -Rf "target"
mkdir -p "target"

# Fix for Error: chart requires kubeVersion: X which is incompatible with Kubernetes Y
cp -a stackgres-operator "target/."
cp -a stackgres-cluster "target/."
sed -i '/^kubeVersion: .*$/d' "target/stackgres-operator/Chart.yaml"
sed -i '/^kubeVersion: .*$/d' "target/stackgres-cluster/Chart.yaml"

mkdir -p "target/templates"
cat << EOF > "target/templates/stackgres-operator-demo.yaml"
apiVersion: v1
kind: Namespace
metadata:
  name: {namespace}
---
EOF

for CRD in ../../src/jobs/src/main/resources/crds/*.yaml
do
  cat "$CRD" >> "target/templates/stackgres-operator-demo.yaml"
  echo --- >> "target/templates/stackgres-operator-demo.yaml"
done

helm template --namespace '$namespace$' stackgres-operator "target/stackgres-operator" \
  --set-string adminui.service.type='$adminui-service-type$' \
  --set-string grafana.autoEmbed='true' \
  --set-string grafana.preprocess='true' \
  >> "target/templates/stackgres-operator-demo.yaml"

sed -i 's/\$namespace\$/{namespace}/g' "target/templates/stackgres-operator-demo.yaml"
sed -i 's/\$adminui-service-type\$/{adminui-service-type}/g' "target/templates/stackgres-operator-demo.yaml"

sed -i 's/#grafana.preprocess.start/{#if grafana-autoEmbed}/g' "target/templates/stackgres-operator-demo.yaml"
sed -i 's/#grafana.preprocess.end/{\/if}/g' "target/templates/stackgres-operator-demo.yaml"
