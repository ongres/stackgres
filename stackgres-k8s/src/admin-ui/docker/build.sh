#!/bin/sh

set -e

PROJECT_PATH="$(dirname "$0")/../../../.."
STACKGRES_VERSION="$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)"

cd "$PROJECT_PATH/stackgres-k8s/src"

ADMINUI_IMAGE_NAME="${ADMINUI_IMAGE_NAME:-"stackgres/admin-ui:main"}"
BASE_IMAGE="registry.access.redhat.com/ubi8/nginx-124@sha256:1fd6987287106e1acfaf0c912848ff48f1a8b1a0907eefe81044fc73a5fbdb31"
TARGET_ADMINUI_IMAGE_NAME="${TARGET_ADMINUI_IMAGE_NAME:-$ADMINUI_IMAGE_NAME}"

docker build -t "$TARGET_ADMINUI_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  --build-arg STACKGRES_VERSION="$STACKGRES_VERSION" \
  -f admin-ui/docker/Dockerfile admin-ui
