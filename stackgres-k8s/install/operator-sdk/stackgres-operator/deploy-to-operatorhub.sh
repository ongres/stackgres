#!/bin/sh

UPSTREAM_NAME="OperatorHub"
UPSTREAM_GIT_URL="https://github.com/k8s-operatorhub/community-operators"
FORK_GIT_URL="${FORK_GIT_URL:-$1}"
PROJECT_NAME="stackgres"
DO_PIN_IMAGES=false

. "$(dirname "$0")/deploy.sh"
