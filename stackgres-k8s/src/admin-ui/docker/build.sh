#!/bin/sh

set -e

PROJECT_PATH="$(dirname "$0")/../../../.."
STACKGRES_VERSION="$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)"

cd "$PROJECT_PATH/stackgres-k8s/src"

ADMINUI_IMAGE_NAME="${ADMINUI_IMAGE_NAME:-"stackgres/admin-ui:main"}"
BASE_IMAGE="registry.access.redhat.com/ubi8/nginx-120:1-117.1692780863"
TARGET_ADMINUI_IMAGE_NAME="${TARGET_ADMINUI_IMAGE_NAME:-$ADMINUI_IMAGE_NAME}"

docker build -t "$TARGET_ADMINUI_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  --build-arg STACKGRES_VERSION="$STACKGRES_VERSION" \
  -f admin-ui/docker/Dockerfile admin-ui
