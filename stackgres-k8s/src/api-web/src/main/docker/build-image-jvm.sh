#!/bin/sh

set -e

RESTAPI_IMAGE_NAME="${RESTAPI_IMAGE_NAME:-"stackgres/restapi:development-jvm"}"
BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-11"
TARGET_RESTAPI_IMAGE_NAME="${TARGET_RESTAPI_IMAGE_NAME:-$RESTAPI_IMAGE_NAME}"

docker build -t "$TARGET_RESTAPI_IMAGE_NAME" --build-arg BASE_IMAGE="$BASE_IMAGE" -f api-web/src/main/docker/Dockerfile.jvm api-web
