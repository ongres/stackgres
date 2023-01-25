#!/bin/sh

set -e

cd "$(dirname "$0")"
PROJECT_PATH=../../../..

STACKGRES_VERSION="${STACKGRES_VERSION:-$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)}"

sh "$PROJECT_PATH"/stackgres-k8s/e2e/e2e get_component_images "$STACKGRES_VERSION"
sh "$PROJECT_PATH"/stackgres-k8s/e2e/e2e get_operator_images "$STACKGRES_VERSION"