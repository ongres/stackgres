#!/bin/sh

set -e

[ -n "$KIND_NAME" ]

if ! [ -d "/tmp/kind-cache/kind-base" ] || [ "$RESET_KIND_BASE" = true ]
then
 rm -rf "/tmp/kind-cache/kind-base"
 KIND_NAME=kind-base KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache/kind-base" sh stackgres-k8s/e2e/e2e reset_k8s
 KIND_NAME=kind-base KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache/kind-base" sh stackgres-k8s/e2e/e2e load_operator_k8s
 KIND_NAME=kind-base KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache/kind-base" sh stackgres-k8s/e2e/e2e delete_k8s
fi

cp --reflink -r /tmp/kind-cache/kind-base "/tmp/kind-cache/$KIND_NAME"
