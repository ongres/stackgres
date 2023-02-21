#!/bin/sh

set -e

PROJECT_PATH="$(dirname "$0")/../../../../../.."
STACKGRES_VERSION="$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)"

cd "$PROJECT_PATH"/stackgres-k8s/src

BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-17-runtime:1.14-7"

RESTAPI_IMAGE_NAME="${RESTAPI_IMAGE_NAME:-"stackgres/restapi:main-jvm"}"
TARGET_RESTAPI_IMAGE_NAME="${TARGET_RESTAPI_IMAGE_NAME:-$RESTAPI_IMAGE_NAME}"

docker build -t "$TARGET_RESTAPI_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  --build-arg STACKGRES_VERSION="$STACKGRES_VERSION" \
  -f api-web/src/main/docker/Dockerfile.jvm api-web
