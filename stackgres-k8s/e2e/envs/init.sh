#!/bin/bash

set -e

export STACKGRES_PATH=$(cd ..; pwd)
export UTILS_PATH=$(cd utils; pwd)
export PATH=$PATH:$UTILS_PATH  

export IMAGE_NAME="${IMAGE_NAME:-stackgres/operator:development-jvm}"
export KIND_NAME="${KIND_NAME:-kind}"
export KUBERNETES_VERSION="${KUBERNETES_VERSION:-1.12.10}"
export KUBECONFIG="$(kind get kubeconfig-path  --name "$KIND_NAME")"

