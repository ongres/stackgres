#!/bin/sh

set -e

BASE_IMAGE="registry.access.redhat.com/ubi8-minimal:8.7-1031"

OPERATOR_IMAGE_NAME="${OPERATOR_IMAGE_NAME:-"stackgres/operator:main"}"
TARGET_OPERATOR_IMAGE_NAME="${TARGET_OPERATOR_IMAGE_NAME:-$OPERATOR_IMAGE_NAME}"

docker build -t "$TARGET_OPERATOR_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  -f operator/src/main/docker/Dockerfile.native operator
