#!/bin/sh

set -e

BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-17-runtime:1.14-7"

DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME="${DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME:-"stackgres/distributedlogs-controller:main-jvm"}"
TARGET_DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME="${TARGET_DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME:-$DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME}"

docker build -t "$TARGET_DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  -f distributedlogs-controller/src/main/docker/Dockerfile.jvm distributedlogs-controller
