#!/bin/sh
# Generate the docs content that is derived from the operator sources
# (port of doc/build.sh pointed at web/): CRD reference pages, helm operator
# parameters page, Hugo data files, and the Operator API swagger spec.
#
# Outputs are gitignored — run this before `hugo` when building the site.
# Requires: yq, crdoc, helm-docs.
#
# Swagger source, in order of preference:
#   1. stackgres-k8s/src/restapi/target/swagger-merged.yaml — the real
#      artifact from the operator build (production path; see doc/build.sh
#      for how to produce it).
#   2. $SG_SWAGGER_DOWNLOAD_URL — fetched snapshot; ONLY for previews
#      (GitLab Pages), never for a production build.
#   3. Neither → skipped with a warning (the Operator API page renders empty).

set -e

cd "$(dirname "$0")/../.."

WEB="web"
DOC_CONTENT="$WEB/content/en/doc"
DATA="$WEB/data"
CRD_PATH="stackgres-k8s/src/common/src/main/resources/crds"
GENERATED="$(mktemp -d)"
trap 'rm -rf "$GENERATED"' EXIT

STACKGRES_VERSION="${STACKGRES_VERSION:-$(sh stackgres-k8s/ci/build/version.sh)}"
if [ -z "$STACKGRES_VERSION" ]
then
  STACKGRES_VERSION="$(sed -n 's|.*<version>\(.*\)</version>.*|\1|p' stackgres-k8s/src/pom.xml | head -n 1)"
fi

echo "current_version: \"$STACKGRES_VERSION\"" > "$DATA/versions.yml"
COMPONENT_VERSIONS_FILE="$(ls -1 stackgres-k8s/src/common/src/main/resources/versions-*.properties \
  | sort -V | tail -n 1)"
POSTGRES_MAJOR_VERSIONS="$(awk '/^postgresql=/{flag=1; sub(/^postgresql=/,"")}
  flag{line=$0; gsub(/[ \\]/,"",line); printf "%s",line; if ($0 !~ /\\$/) flag=0}' \
  "$COMPONENT_VERSIONS_FILE" \
  | tr ',' '\n' | sed 's/-build-.*//' | cut -d . -f 1 | sort -n | uniq)"
echo "postgres_min_version: \"$(printf '%s\n' "$POSTGRES_MAJOR_VERSIONS" | head -n 1)\"" >> "$DATA/versions.yml"
echo "postgres_max_version: \"$(printf '%s\n' "$POSTGRES_MAJOR_VERSIONS" | tail -n 1)\"" >> "$DATA/versions.yml"

rm -rf "$DATA/crds"
mkdir -p "$DATA/crds"
find "$CRD_PATH" -name '*.yaml' \
  | while read -r FILE
    do
      cp "$FILE" "$DATA/crds"
    done
cp "stackgres-k8s/install/helm/stackgres-operator/Chart.yaml" \
  "$DATA/stackgres_operator_Chart.yaml"
cp "stackgres-k8s/install/operator-sdk/stackgres-operator/openshift-operator-bundle/metadata/annotations.yaml" \
  "$DATA/stackgres_operator_openshift_annotations.yaml"
sed -n 's#^ *RUN wget "https://get.helm.sh/helm-v\([^-][^-]*\)-.*$#version: \1#p' \
  "stackgres-k8s/ci/build/Dockerfile-ci" \
  > "$DATA/helm_version.yaml"
(
  cat "$(ls -1 stackgres-k8s/src/operator/src/main/resources/*/postgresql-default-values-pg*.properties \
    | tail -n 1)" \
  || echo :
) \
  | sed 's/=\(.*\)$/: "\1"/' | yq . > "$DATA/postgresql_default_values.yaml"
(
  cat "stackgres-k8s/src/operator/src/main/resources/postgresql-blocklist.properties" \
  || echo :
) \
  | yq 'split(" ")' > "$DATA/postgresql_blocklist.yaml"
(
  cat "stackgres-k8s/src/operator/src/main/resources/pgbouncer-default-values.properties" \
  || echo :
) \
  | sed 's/=\(.*\)$/: "\1"/' | yq . > "$DATA/pgbouncer_default_values.yaml"
(
  cat "stackgres-k8s/src/operator/src/main/resources/pgbouncer-blocklist.properties" \
  || echo :
) \
  | yq 'split(" ")' > "$DATA/pgbouncer_blocklist.yaml"

if [ -f "stackgres-k8s/src/restapi/target/swagger-merged.yaml" ]
then
  cp "stackgres-k8s/src/restapi/target/swagger-merged.yaml" "$WEB/static/sg-swagger.yaml"
elif [ -n "$SG_SWAGGER_DOWNLOAD_URL" ]
then
  echo "WARNING: downloading swagger snapshot from $SG_SWAGGER_DOWNLOAD_URL (preview builds only)"
  curl -fsSL "$SG_SWAGGER_DOWNLOAD_URL" -o "$WEB/static/sg-swagger.yaml"
else
  echo "WARNING: no swagger available — the Operator API page will render empty."
  echo "Build the operator first (see doc/build.sh) or set SG_SWAGGER_DOWNLOAD_URL for a preview."
fi

cat "$CRD_PATH/index.txt" \
  | while read -r CRD_FILE
    do
      crdoc --resources "$CRD_PATH/$CRD_FILE" \
        -o "$GENERATED/${CRD_FILE%.yaml}.md" \
        --template "doc/CRD.tmpl"
      TARGET_PATH="$(ls -d "$DOC_CONTENT/06-crd-reference/"*"-$(echo "${CRD_FILE%.yaml}" | tr 'A-Z' 'a-z')")"
      INCLUDE_LINE="$(grep -nxF '{{% include "generated/'"${CRD_FILE%.yaml}"'.md" %}}' "$TARGET_PATH/_index.template.md" | cut -d : -f 1)"
      head -n "$((INCLUDE_LINE - 1))" "$TARGET_PATH/_index.template.md" > "$TARGET_PATH/_index.md"
      cat "$GENERATED/${CRD_FILE%.yaml}.md" >> "$TARGET_PATH/_index.md"
      tail -n +"$((INCLUDE_LINE + 1))" "$TARGET_PATH/_index.template.md" >> "$TARGET_PATH/_index.md"
      perl -0777 -pi -e 's/```([a-z]*)/\n```$1\n/g' "$TARGET_PATH/_index.md"
    done

(
cd "stackgres-k8s/install/helm/stackgres-operator"
helm-docs \
  -o "generated.md" \
  -f "values.yaml" \
  -t "VALUES.html.gotmpl"
)
mv "stackgres-k8s/install/helm/stackgres-operator/generated.md" \
  "$GENERATED/stackgres-operator.md"
TARGET_PATH="$DOC_CONTENT/04-administration-guide/01-installation/02-installation-via-helm/01-operator-parameters"
INCLUDE_LINE="$(grep -nxF '{{% include "generated/stackgres-operator.md" %}}' "$TARGET_PATH/_index.template.md" | cut -d : -f 1)"
if [ -n "$INCLUDE_LINE" ]
then
  head -n "$((INCLUDE_LINE - 1))" "$TARGET_PATH/_index.template.md" > "$TARGET_PATH/_index.md"
  cat "$GENERATED/stackgres-operator.md" >> "$TARGET_PATH/_index.md"
  tail -n +"$((INCLUDE_LINE + 1))" "$TARGET_PATH/_index.template.md" >> "$TARGET_PATH/_index.md"
else
  # no include marker in the template — the generated table goes at the end
  cat "$TARGET_PATH/_index.template.md" "$GENERATED/stackgres-operator.md" > "$TARGET_PATH/_index.md"
fi

echo "generated: CRD reference pages, operator parameters page, data files"
