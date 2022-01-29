#!/bin/sh

set -e

BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-17-runtime:1.11"

CLUSTER_CONTROLLER_IMAGE_NAME="${CLUSTER_CONTROLLER_IMAGE_NAME:-"stackgres/cluster-controller:main-jvm"}"
TARGET_CLUSTER_CONTROLLER_IMAGE_NAME="${TARGET_CLUSTER_CONTROLLER_IMAGE_NAME:-$CLUSTER_CONTROLLER_IMAGE_NAME}"

docker build -t "$TARGET_CLUSTER_CONTROLLER_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  -f cluster-controller/src/main/docker/Dockerfile.jvm cluster-controller
