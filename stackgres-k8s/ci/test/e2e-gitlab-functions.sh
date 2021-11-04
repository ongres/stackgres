#!/bin/sh

[ "$DEBUG" != true ] || set -x
export SHELL_XTRACE
SHELL_XTRACE="$(! echo $- | grep -q x || echo "-x")"

set -e

[ -n "$CI_PROJECT_ID" ]

cd "$(dirname "$0")/../../.."

mkdir -p stackgres-k8s/ci/test/target
TEMP_DIR="/tmp/$CI_PROJECT_ID"
mkdir -p "$TEMP_DIR"

