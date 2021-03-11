#!/bin/sh

set -e

JOBS_IMAGE_NAME="${JOBS_IMAGE_NAME:-"stackgres/jobs:development-jvm"}"
BASE_IMAGE="registry.access.redhat.com/ubi8/openjdk-11"
TARGET_JOBS_IMAGE_NAME="${TARGET_JOBS_IMAGE_NAME:-$JOBS_IMAGE_NAME}"

docker build -t "$TARGET_JOBS_IMAGE_NAME" --build-arg BASE_IMAGE="$BASE_IMAGE" -f jobs/src/main/docker/Dockerfile.jvm jobs
