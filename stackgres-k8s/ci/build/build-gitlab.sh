#!/bin/sh

. "$(dirname "$0")/build-functions.sh"

set -e

TEMP_DIR="/tmp/$CI_PROJECT_ID"
mkdir -p "$TEMP_DIR"

echo "Copying project files ..."

cp -r . "$TEMP_DIR/stackgres-build-$CI_JOB_ID"

echo "done"

set +e

(
set -e
cd "$TEMP_DIR/stackgres-build-$CI_JOB_ID"
docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"

echo

echo "Building $* ..."

sh stackgres-k8s/ci/build/build.sh "$@"

echo "done"

echo

echo "Extracting files ..."

for MODULE in "$@"
do
  sh stackgres-k8s/ci/build/build-functions.sh extract "$MODULE" \
    'stackgres-k8s/src/api-web/target/swagger*' \
    'stackgres-k8s/src/*/target/surefire-reports/TEST-*.xml' \
    'stackgres-k8s/src/admin-ui/node_modules'
done

echo "done"

echo

echo "Copying extracted files ..."

for FILE in stackgres-k8s/ci/build/target \
  stackgres-k8s/src/.mvn \
  stackgres-k8s/src/.m2/repository \
  stackgres-k8s/src/api-web/target/swagger* \
  stackgres-k8s/src/*/target/surefire-reports/TEST-*.xml \
  stackgres-k8s/src/admin-ui/node_modules
do
  if [ -e "$FILE" ]
  then
    mkdir -p "$CI_PROJECT_DIR/${FILE%/*}"
    cp -rf "$FILE" "$CI_PROJECT_DIR/${FILE%/*}/."
  fi
done

echo "done"

echo

)
EXIT_CODE="$?"

echo "Cleaning up ..."

cd "$CI_PROJECT_DIR"
rm -rf "$TEMP_DIR/stackgres-build-$CI_JOB_ID"

echo "done"

exit "$EXIT_CODE"
