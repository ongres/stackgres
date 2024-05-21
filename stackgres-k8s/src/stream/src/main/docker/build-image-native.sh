#!/bin/sh

set -e

BASE_IMAGE="registry.access.redhat.com/ubi8-minimal:8.7-1085"

STREAM_IMAGE_NAME="${STREAM_IMAGE_NAME:-"stackgres/stream:main"}"
TARGET_STREAM_IMAGE_NAME="${TARGET_STREAM_IMAGE_NAME:-$STREAM_IMAGE_NAME}"

docker build -t "$TARGET_STREAM_IMAGE_NAME" \ --build-arg BASE_IMAGE="$BASE_IMAGE" \
  -f stream/src/main/docker/Dockerfile.native stream
