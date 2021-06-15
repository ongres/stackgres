#!/bin/sh

. "$(dirname "$0")/build-functions.sh"

set -e

if [ -n "$1" ]
then
  MODULES="$*"
else
  MODULES="$(jq -r '.modules | to_entries[] | .key' stackgres-k8s/ci/build/target/config.json)"
fi

generate_image_hashes

retrieve_image_digests

if [ "$MODULES" = "hashes" ]
then
  exit
fi

for MODULE in $MODULES
do
  build_image "$MODULE"
done
