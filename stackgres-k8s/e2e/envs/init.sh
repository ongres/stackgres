#!/bin/bash
export STACKGRES_PATH=$(cd ..; pwd)
export UTILS_PATH=$(cd utils; pwd)
export PATH=$PATH:$UTILS_PATH  

export KUBECONFIG="$(kind get kubeconfig-path --name="kind")"

