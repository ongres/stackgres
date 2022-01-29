#!/bin/sh

set -e

BASE_IMAGE="registry.access.redhat.com/ubi8-minimal:8.5"

CLUSTER_CONTROLLER_IMAGE_NAME="${CLUSTER_CONTROLLER_IMAGE_NAME:-"stackgres/cluster-controller:main"}"
TARGET_CLUSTER_CONTROLLER_IMAGE_NAME="${TARGET_CLUSTER_CONTROLLER_IMAGE_NAME:-$CLUSTER_CONTROLLER_IMAGE_NAME}"

docker build -t "$TARGET_CLUSTER_CONTROLLER_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  -f cluster-controller/src/main/docker/Dockerfile.native cluster-controller
