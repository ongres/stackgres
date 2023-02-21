#!/bin/sh

set -e

PROJECT_PATH="$(dirname "$0")/../../../../../.."
STACKGRES_VERSION="$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)"

cd "$PROJECT_PATH"/stackgres-k8s/src

BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-17-runtime:1.14-8"

JOBS_IMAGE_NAME="${JOBS_IMAGE_NAME:-"stackgres/jobs:main-jvm"}"
TARGET_JOBS_IMAGE_NAME="${TARGET_JOBS_IMAGE_NAME:-$JOBS_IMAGE_NAME}"

docker build -t "$TARGET_JOBS_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  --build-arg STACKGRES_VERSION="$STACKGRES_VERSION" \
  -f jobs/src/main/docker/Dockerfile.jvm jobs
