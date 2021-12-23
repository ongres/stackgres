#!/bin/sh

[ "$DEBUG" != true ] || set -x

set -e

cd "$(dirname "$0")/../../.."

mkdir -p stackgres-k8s/ci/test/target

if [ "$#" -gt 0 ]
then
  "$@"
fi
