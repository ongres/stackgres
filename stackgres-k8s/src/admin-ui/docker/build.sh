#!/bin/sh

set -e
ADMINUI_IMAGE_NAME="${ADMINUI_IMAGE_NAME:-"stackgres/admin-ui:${IMAGE_TAG%-jvm}"}"
BASE_IMAGE="registry.access.redhat.com/ubi8/nginx-120:1-92"
TARGET_ADMINUI_IMAGE_NAME="${TARGET_ADMINUI_IMAGE_NAME:-$ADMINUI_IMAGE_NAME}"

docker build -t "$TARGET_ADMINUI_IMAGE_NAME" --build-arg BASE_IMAGE="$BASE_IMAGE" -f admin-ui/docker/Dockerfile admin-ui
