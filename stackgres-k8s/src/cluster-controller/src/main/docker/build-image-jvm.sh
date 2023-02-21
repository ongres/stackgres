#!/bin/sh

set -e

PROJECT_PATH="$(dirname "$0")/../../../../../.."
STACKGRES_VERSION="$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)"

cd "$PROJECT_PATH"/stackgres-k8s/src

BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-17-runtime:1.14-8"

CLUSTER_CONTROLLER_IMAGE_NAME="${CLUSTER_CONTROLLER_IMAGE_NAME:-"stackgres/cluster-controller:main-jvm"}"
TARGET_CLUSTER_CONTROLLER_IMAGE_NAME="${TARGET_CLUSTER_CONTROLLER_IMAGE_NAME:-$CLUSTER_CONTROLLER_IMAGE_NAME}"

docker build -t "$TARGET_CLUSTER_CONTROLLER_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  --build-arg STACKGRES_VERSION="$STACKGRES_VERSION" \
  -f cluster-controller/src/main/docker/Dockerfile.jvm cluster-controller
