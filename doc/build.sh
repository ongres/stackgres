#!/bin/sh

set -e

rm -Rf "$(dirname "$0")/data/crds"
mkdir -p "$(dirname "$0")/data/crds"
find "$(dirname "$0")/../stackgres-k8s/src/common/src/main/resources/crds" -name '*.yaml' \
  | while read -r FILE
    do
      cp "$FILE" "$(dirname "$0")/data/crds"
    done
cp "$(dirname "$0")/../stackgres-k8s/install/helm/stackgres-operator/Chart.yaml" \
  "$(dirname "$0")/data/stackgres_operator_Chart.yaml"
cp "$(dirname "$0")/../stackgres-k8s/install/operator-sdk/stackgres-operator/openshift-operator-bundle/metadata/annotations.yaml" \
  "$(dirname "$0")/data/stackgres_operator_openshift_annotations.yaml"
sed -n 's#^ *RUN wget "https://get.helm.sh/helm-v\([^-]\+\)-.*$#version: \1#p' \
  "$(dirname "$0")/../stackgres-k8s/ci/build/Dockerfile-ci" \
  > "$(dirname "$0")/data/helm_version.yaml"
(
  cat "$(ls -1 "$(dirname "$0")"/../stackgres-k8s/src/operator/src/main/resources/postgresql-default-values-pg*.properties \
    | tail -n 1)" \
  || echo :
) \
  | sed 's/=\(.*\)$/: "\1"/' | yq . > "$(dirname "$0")/data/postgresql_default_values.yaml"
(
  cat "$(dirname "$0")"/../stackgres-k8s/src/operator/src/main/resources/postgresql-blocklist.properties \
  || echo :
) \
  | yq 'split(" ")' > "$(dirname "$0")/data/postgresql_blocklist.yaml"
(
  cat "$(dirname "$0")"/../stackgres-k8s/src/operator/src/main/resources/pgbouncer-default-values.properties \
  || echo :
) \
  | sed 's/=\(.*\)$/: "\1"/' | yq . > "$(dirname "$0")/data/pgbouncer_default_values.yaml"
(
  cat "$(dirname "$0")"/../stackgres-k8s/src/operator/src/main/resources/pgbouncer-blocklist.properties \
  || echo :
) \
  | yq 'split(" ")' > "$(dirname "$0")/data/pgbouncer_blocklist.yaml"

STACKGRES_VERSION="${STACKGRES_VERSION:-$(sh stackgres-k8s/ci/build/version.sh)}"
echo "current_version: \"$STACKGRES_VERSION\"" > "$(dirname "$0")/data/versions.yml"

if [ ! -f "$(dirname "$0")/../stackgres-k8s/src/restapi/target/swagger-merged.yaml" ]
then
  echo "Please build Stackgres operator and swagger first:"
  echo
  echo "cd stackgres-k8s/src"
  echo "./mvnw clean package -DskipTests"
  echo "sh restapi/src/main/swagger/build.sh"
  echo
  exit 1
fi
cp "$(dirname "$0")/../stackgres-k8s/src/restapi/target/swagger-merged.yaml" "$(dirname "$0")/themes/sg-doc/static/sg-swagger.yaml"

mkdir -p "$(dirname "$0")/generated"
CRD_PATH="$(dirname "$0")/../stackgres-k8s/src/common/src/main/resources/crds"
cat "$CRD_PATH/index.txt" \
  | {
    while read -r CRD_FILE
    do
      crdoc --resources "$CRD_PATH/$CRD_FILE" \
        -o "$(dirname "$0")/generated/${CRD_FILE%.yaml}.md" \
        --template "$(dirname "$0")/CRD.tmpl"
      TARGET_PATH="$(ls -d "$(dirname "$0")/content/en/06-crd-reference/"*"-$(echo "${CRD_FILE%.yaml}" | tr 'A-Z' 'a-z')")"
      INCLUDE_LINE="$(cat "$TARGET_PATH/_index.template.md" | grep -nxF '{{% include "generated/'"${CRD_FILE%.yaml}"'.md" %}}' | cut -d : -f 1)"
      head -n "$((INCLUDE_LINE - 1))" "$TARGET_PATH/_index.template.md" > "$TARGET_PATH/_index.md"
      cat "$(dirname "$0")/generated/${CRD_FILE%.yaml}.md" >> "$TARGET_PATH/_index.md"
      tail -n +"$((INCLUDE_LINE + 1))" "$TARGET_PATH/_index.template.md" >> "$TARGET_PATH/_index.md"
      sed -i -z 's/```/\n```\n/g' "$TARGET_PATH/_index.md"
    done
    }
(
cd "$(dirname "$0")/../stackgres-k8s/install/helm/stackgres-operator"
helm-docs \
  -o "generated.md" \
  -f "values.yaml" \
  -t "VALUES.html.gotmpl"
)
mv "$(dirname "$0")/../stackgres-k8s/install/helm/stackgres-operator/generated.md" \
  "$(dirname "$0")/generated/stackgres-operator.md"
TARGET_PATH="$(dirname "$0")/../doc/content/en/04-administration-guide/01-installation/02-installation-via-helm/01-operator-parameters"
INCLUDE_LINE="$(cat "$TARGET_PATH/_index.template.md" | grep -nxF '{{% include "generated/stackgres-operator.md" %}}' | cut -d : -f 1)"
head -n "$((INCLUDE_LINE - 1))" "$TARGET_PATH/_index.template.md" > "$TARGET_PATH/_index.md"
cat "$(dirname "$0")/generated/stackgres-operator.md" >> "$TARGET_PATH/_index.md"
tail -n +"$((INCLUDE_LINE + 1))" "$TARGET_PATH/_index.template.md" >> "$TARGET_PATH/_index.md"
