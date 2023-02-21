#!/bin/sh

set -e

PROJECT_PATH="$(dirname "$0")/../../../../../.."
STACKGRES_VERSION="$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)"

cd "$PROJECT_PATH"/stackgres-k8s/src

BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-17-runtime:1.14-8"

OPERATOR_IMAGE_NAME="${OPERATOR_IMAGE_NAME:-"stackgres/operator:main-jvm"}"
TARGET_OPERATOR_IMAGE_NAME="${TARGET_OPERATOR_IMAGE_NAME:-$OPERATOR_IMAGE_NAME}"

docker build -t "$TARGET_OPERATOR_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  --build-arg STACKGRES_VERSION="$STACKGRES_VERSION" \
  -f operator/src/main/docker/Dockerfile.jvm operator
