#!/bin/sh

set -e

BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-17-runtime:1.14-4.1665493383"

JOBS_IMAGE_NAME="${JOBS_IMAGE_NAME:-"stackgres/jobs:main-jvm"}"
TARGET_JOBS_IMAGE_NAME="${TARGET_JOBS_IMAGE_NAME:-$JOBS_IMAGE_NAME}"

docker build -t "$TARGET_JOBS_IMAGE_NAME" \
  --build-arg BASE_IMAGE="$BASE_IMAGE" \
  -f jobs/src/main/docker/Dockerfile.jvm jobs
