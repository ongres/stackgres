#!/bin/sh

set -e

OPERATOR_IMAGE_NAME="${OPERATOR_IMAGE_NAME:-"stackgres/operator:main-jvm"}"
BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-11:1.3-10"
TARGET_OPERATOR_IMAGE_NAME="${TARGET_OPERATOR_IMAGE_NAME:-$OPERATOR_IMAGE_NAME}"

docker build -t "$TARGET_OPERATOR_IMAGE_NAME" --build-arg BASE_IMAGE="$BASE_IMAGE" -f operator/src/main/docker/Dockerfile.jvm operator
