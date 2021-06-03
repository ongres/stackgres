#!/bin/sh

set -e

CURRENT_DIR="$(dirname "$(realpath "$0")")"
PROJECT_DIR="$(dirname "$CURRENT_DIR")"

if [ ! -f "${PROJECT_DIR}/stackgres-k8s/src/api-web/target/swagger-merged.yaml" ]
then
  echo "Please build Stackgres operator and swagger first:"
  echo
  echo "cd stackgres-k8s/src"
  echo "./mvnw clean package -DskipTests"
  echo "sh api-web/src/main/swagger/build.sh"
  echo
  exit 1
fi
cp -v "${PROJECT_DIR}/stackgres-k8s/src/api-web/target/swagger-merged.yaml" "${CURRENT_DIR}/themes/sg-doc/static/sg-swagger.yaml"

rm -Rf "${CURRENT_DIR}/data/crds" && mkdir -p "${CURRENT_DIR}/data/crds"
find "${PROJECT_DIR}/stackgres-k8s/src/common/src/main/resources/crds" -name '*.yaml' \
  | while read -r FILE
    do
      cp -v "$FILE" "${CURRENT_DIR}/data/crds"
    done

STACKGRES_VERSION="${STACKGRES_VERSION:-$(
  grep '<artifactId>stackgres-parent</artifactId>' "${PROJECT_DIR}/stackgres-k8s/src/pom.xml" -C 2 --color=never \
    | grep --color=never -o '<version>\([^<]\+\)</version>' | tr '<>' '  ' | cut -d ' ' -f 3)}"

echo "current_version: \"$STACKGRES_VERSION\"" > "${CURRENT_DIR}/data/versions.yml"
