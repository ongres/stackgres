#!/bin/sh

. "$(dirname "$0")/build-functions.sh"

set -e

mkdir -p stackgres-k8s/ci/build/target

UID="$(id -u)"

yq . stackgres-k8s/ci/build/config.yml > stackgres-k8s/ci/build/target/config.json

if [ -n "$1" ]
then
  MODULES="$*"
else
  MODULES="$(jq -r '.modules | to_entries[] | .key' stackgres-k8s/ci/build/target/config.json)"
fi

generate_image_hashes

for MODULE in $MODULES
do
  build_image "$MODULE"
done
