#!/bin/sh

set -e

cd "$(dirname "$0")"

STACKGRES_VERSION="$(grep '<artifactId>stackgres-parent</artifactId>' "../../src/pom.xml" -A 2 -B 2 \
 | grep -o '<version>\([^<]\+\)</version>' | tr '<>' '  ' | cut -d ' ' -f 3)"

rm -Rf "target/stackgres-operator" "target/templates"
mkdir -p "target"

# Fix for Error: chart requires kubeVersion: X which is incompatible with Kubernetes Y
cp -a stackgres-operator "target/."
sed -i '/^kubeVersion: .*$/d' "target/stackgres-operator/Chart.yaml"

find target/stackgres-operator -type l \
  | while read LINK
    do
      ln -f -s "../$(readlink "$LINK")" "$LINK"
    done

mkdir -p "target/templates"
cat << EOF > "target/templates/stackgres-operator.qute.yaml"
apiVersion: v1
kind: Namespace
metadata:
  name: {namespace}
---
EOF

for CRD in ../../src/common/src/main/resources/crds/*.yaml
do
  cat "$CRD" >> "target/templates/stackgres-operator.qute.yaml"
  printf "\n---\n" >> "target/templates/stackgres-operator.qute.yaml"
done

# Helm 3.6.0+ required to set the --kube-version
helm template --namespace '$namespace$' stackgres-operator "target/stackgres-operator" \
  --kube-version="v1.20" \
  --set-string adminui.service.type='$adminui-service-type$' \
  --set-string grafana.autoEmbed='true' \
  --set-string qutePreprocess='true' \
  >> "target/templates/stackgres-operator.qute.yaml"

sed -i 's/\$namespace\$/{namespace}/g' "target/templates/stackgres-operator.qute.yaml"
sed -i 's/\$adminui-service-type\$/{adminui-service-type}/g' "target/templates/stackgres-operator.qute.yaml"

sed -i 's/#grafana.preprocess.start/{#if grafana-autoEmbed}/g' "target/templates/stackgres-operator.qute.yaml"
sed -i 's/#grafana.preprocess.end/{\/if}/g' "target/templates/stackgres-operator.qute.yaml"

sed -i 's/#comment.preprocess.start/{|/g' "target/templates/stackgres-operator.qute.yaml"
sed -i 's/#comment.preprocess.end/|}/g' "target/templates/stackgres-operator.qute.yaml"

sed -i 's/password:.*/password: {webapi-password}/g' "target/templates/stackgres-operator.qute.yaml"
sed -i 's/clearPassword:.*/clearPassword: {webapi-password-clear}/g' "target/templates/stackgres-operator.qute.yaml"
