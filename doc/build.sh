#!/bin/sh

set -e

rm -Rf "$(dirname "$0")/data/crds"
mkdir -p "$(dirname "$0")/data/crds"
find "$(dirname "$0")/../stackgres-k8s/install/helm/stackgres-operator/crds" -name '*.yaml' \
  | while read -r FILE
    do
      cp "$FILE" "$(dirname "$0")/data/crds"
    done

STACKGRES_VERSION="${STACKGRES_VERSION:-$(
  grep '<artifactId>stackgres-parent</artifactId>' "$(dirname "$0")/../stackgres-k8s/src/pom.xml" -A 2 -B 2 \
    | grep -o '<version>\([^<]\+\)</version>' | tr '<>' '  ' | cut -d ' ' -f 3)}"
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
