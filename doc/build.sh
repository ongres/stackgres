#!/bin/sh

set -e

rm -Rf "$(dirname "$0")/data/crds"
mkdir -p "$(dirname "$0")/data/crds"
find "$(dirname "$0")/../stackgres-k8s/src/common/src/main/resources/crds" -name '*.yaml' \
  | while read -r FILE
    do
      cp "$FILE" "$(dirname "$0")/data/crds"
    done

STACKGRES_VERSION="${STACKGRES_VERSION:-$(sh stackgres-k8s/ci/build/version.sh)}"
echo "current_version: \"$STACKGRES_VERSION\"" > "$(dirname "$0")/data/versions.yml"

if [ ! -f "$(dirname "$0")/../stackgres-k8s/src/api-web/target/swagger-merged.yaml" ]
then
  echo "Please build Stackgres operator and swagger first:"
  echo
  echo "cd stackgres-k8s/src"
  echo "./mvnw clean package -DskipTests"
  echo "sh api-web/src/main/swagger/build.sh"
  echo
  exit 1
fi
cp "$(dirname "$0")/../stackgres-k8s/src/api-web/target/swagger-merged.yaml" "$(dirname "$0")/themes/sg-doc/static/sg-swagger.yaml"

mkdir -p "$(dirname "$0")/generated"
(
cd "$(dirname "$0")/../stackgres-k8s/install/helm/stackgres-operator"
helm-docs \
  -o "generated.md" \
  -f "values.yaml" \
  -t "VALUES.md.gotmpl"
)
CRD_PATH="$(dirname "$0")/../stackgres-k8s/src/common/src/main/resources/crds"
cat "$CRD_PATH/index.txt" \
  | {
    while read -r CRD_FILE
    do
      crdoc --resources "$CRD_PATH/$CRD_FILE" \
        -o "$(dirname "$0")/generated/${CRD_FILE%.yaml}.md" \
        --template "$(dirname "$0")/CRD.tmpl"
    done
    }
mv "$(dirname "$0")/../stackgres-k8s/install/helm/stackgres-operator/generated.md" \
  "$(dirname "$0")/generated/stackgres-operator.md"
