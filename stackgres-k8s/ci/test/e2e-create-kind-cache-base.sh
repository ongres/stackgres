#!/bin/sh

set -e

if ! [ -d "/tmp/kind-cache/kind-base" ] || [ "$RESET_KIND_BASE" = true ]
then
  rm -rf "/tmp/kind-cache/kind-base"
  KIND_NAME=kind-base KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache/kind-base" \
    sh stackgres-k8s/e2e/e2e reset_k8s
  for IMAGE_TAG in $IMAGE_TAGS
  do
    IMAGE_TAG="$IMAGE_TAG" KIND_NAME=kind-base KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache/kind-base" \
      sh stackgres-k8s/e2e/e2e load_operator_k8s
  done
  (cat /tmp/pulled-images-* || true) | sort > /tmp/pulled-images-base
  E2E_PULLED_IMAGES_PATH=/tmp/pulled-images-base KIND_NAME=kind-base KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache/kind-base" \
    sh stackgres-k8s/e2e/e2e load_cached_images_from_local_repository 
  KIND_NAME=kind-base KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache/kind-base" \
    sh stackgres-k8s/e2e/e2e delete_k8s
fi

