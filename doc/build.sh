#!/bin/sh

set -e

PROJECT_PATH=".."

cd "$(dirname "$0")"

rm -Rf "data/crds"
mkdir -p "data/crds"

STACKGRES_VERSION="${STACKGRES_VERSION:-$(sh "$PROJECT_PATH/stackgres-k8s/ci/build/version.sh")}"
echo "current_version: \"$STACKGRES_VERSION\"" > "data/versions.yml"

COMPONENT_VERSIONS_FILE="$(ls -1 "$PROJECT_PATH/stackgres-k8s/src/common/src/main/resources"/versions-*.properties \
  | sort -V | tail -n 1)"
POSTGRES_MAJOR_VERSIONS="$(awk '/^postgresql=/{flag=1; sub(/^postgresql=/,"")}
  flag{line=$0; gsub(/[ \\]/,"",line); printf "%s",line; if ($0 !~ /\\$/) flag=0}' \
  "$COMPONENT_VERSIONS_FILE" \
  | tr ',' '\n' | sed 's/-build-.*//' | cut -d . -f 1 | sort -n | uniq)"
echo "postgres_min_version: \"$(printf '%s\n' "$POSTGRES_MAJOR_VERSIONS" | head -n 1)\"" >> "data/versions.yml"
echo "postgres_max_version: \"$(printf '%s\n' "$POSTGRES_MAJOR_VERSIONS" | tail -n 1)\"" >> "data/versions.yml"

# Files that are plain copies of files in the repository are symbolic links
# (relative to the folder that contains the link). Generated files are written
# as regular files. Both are committed.
find "$PROJECT_PATH/stackgres-k8s/src/common/src/main/resources/crds" -name '*.yaml' \
  | sort \
  | while read -r FILE
    do
      ln -s "../../../stackgres-k8s/src/common/src/main/resources/crds/${FILE##*/}" "data/crds/${FILE##*/}"
    done
ln -sfn "../../stackgres-k8s/install/helm/stackgres-operator/Chart.yaml" \
  "data/stackgres_operator_Chart.yaml"
ln -sfn "../../stackgres-k8s/install/operator-sdk/stackgres-operator/openshift-operator-bundle/metadata/annotations.yaml" \
  "data/stackgres_operator_openshift_annotations.yaml"
sed -n 's#^ *RUN wget "https://get.helm.sh/helm-v\([^-]\+\)-.*$#version: \1#p' \
  "$PROJECT_PATH/stackgres-k8s/ci/build/Dockerfile-ci" \
  > "data/helm_version.yaml"
(
  cat "$(ls -1 "$PROJECT_PATH/stackgres-k8s/src/operator/src/main/resources"/*/postgresql-default-values-pg*.properties \
    | tail -n 1)" \
  || echo :
) \
  | sed 's/=\(.*\)$/: "\1"/' | yq . > "data/postgresql_default_values.yaml"
(
  cat "$PROJECT_PATH/stackgres-k8s/src/operator/src/main/resources/postgresql-blocklist.properties" \
  || echo :
) \
  | yq 'split(" ")' > "data/postgresql_blocklist.yaml"
(
  cat "$PROJECT_PATH/stackgres-k8s/src/operator/src/main/resources/pgbouncer-default-values.properties" \
  || echo :
) \
  | sed 's/=\(.*\)$/: "\1"/' | yq . > "data/pgbouncer_default_values.yaml"
(
  cat "$PROJECT_PATH/stackgres-k8s/src/operator/src/main/resources/pgbouncer-blocklist.properties" \
  || echo :
) \
  | yq 'split(" ")' > "data/pgbouncer_blocklist.yaml"

if [ -f "$PROJECT_PATH/stackgres-k8s/src/restapi/target/swagger-merged.yaml" ]
then
  cp "$PROJECT_PATH/stackgres-k8s/src/restapi/target/swagger-merged.yaml" "themes/sg-doc/static/sg-swagger.yaml"
else
  echo "Keeping the committed themes/sg-doc/static/sg-swagger.yaml since the swagger was not built. To update it:"
  echo
  echo "sh stackgres-k8s/ci/build/build.sh restapi-java"
  echo "sh stackgres-k8s/ci/build/build-functions.sh extract restapi-java stackgres-k8s/src/restapi/target/swagger-merged.yaml"
  echo
fi

mkdir -p "generated"
CRD_PATH="$PROJECT_PATH/stackgres-k8s/src/common/src/main/resources/crds"
cat "$CRD_PATH/index.txt" \
  | {
    while read -r CRD_FILE
    do
      crdoc --resources "$CRD_PATH/$CRD_FILE" \
        -o "generated/${CRD_FILE%.yaml}.md" \
        --template "./CRD.tmpl"
      TARGET_PATH="$(ls -d "content/en/06-crd-reference/"*"-$(echo "${CRD_FILE%.yaml}" | tr 'A-Z' 'a-z')")"
      INCLUDE_LINE="$(cat "$TARGET_PATH/_index.template.md" | grep -nxF '{{% include "generated/'"${CRD_FILE%.yaml}"'.md" %}}' | cut -d : -f 1)"
      head -n "$((INCLUDE_LINE - 1))" "$TARGET_PATH/_index.template.md" > "$TARGET_PATH/_index.md"
      cat "generated/${CRD_FILE%.yaml}.md" >> "$TARGET_PATH/_index.md"
      tail -n +"$((INCLUDE_LINE + 1))" "$TARGET_PATH/_index.template.md" >> "$TARGET_PATH/_index.md"
    done
    }
(
cd "$PROJECT_PATH/stackgres-k8s/install/helm/stackgres-operator"
helm-docs \
  -o "generated.md" \
  -f "values.yaml" \
  -t "VALUES.html.gotmpl"
)
mv "$PROJECT_PATH/stackgres-k8s/install/helm/stackgres-operator/generated.md" \
  "generated/stackgres-operator.md"
TARGET_PATH="content/en/04-administration-guide/01-installation/02-installation-via-helm/01-operator-parameters"
INCLUDE_LINE="$(cat "$TARGET_PATH/_index.template.md" | grep -nxF '{{% include "generated/stackgres-operator.md" %}}' | cut -d : -f 1)"
head -n "$((INCLUDE_LINE - 1))" "$TARGET_PATH/_index.template.md" > "$TARGET_PATH/_index.md"
cat "generated/stackgres-operator.md" >> "$TARGET_PATH/_index.md"
tail -n +"$((INCLUDE_LINE + 1))" "$TARGET_PATH/_index.template.md" >> "$TARGET_PATH/_index.md"
