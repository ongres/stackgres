#!/bin/sh

export UPSTREAM_NAME="OperatorHub"
export UPSTREAM_GIT_URL="https://github.com/k8s-operatorhub/community-operators"
export FORK_GIT_URL="${FORK_GIT_URL:-$1}"
export PROJECT_NAME="stackgres"
export DO_PIN_IMAGES=false
exec sh $(printf %s "$-" | grep -q x && printf %s -x) "$(dirname "$0")/deploy.sh"
