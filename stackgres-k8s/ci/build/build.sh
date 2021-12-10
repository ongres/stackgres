#!/bin/sh

# shellcheck disable=SC1090
. "$(dirname "$0")/build-functions.sh"

set -e

generate_image_hashes

show_image_hashes

if [ -n "$1" ]
then
  MODULES="$*"
else
  MODULES="$(jq -r '.modules | to_entries[] | .key' stackgres-k8s/ci/build/target/config.json)"
fi

find_image_digests stackgres-k8s/ci/build/target/all-images \
  > stackgres-k8s/ci/build/target/image-digests

echo "Retrieved image digests:"
sort stackgres-k8s/ci/build/target/all-images | uniq \
  | while read -r IMAGE_NAME
    do
      printf ' - %s => %s\n' "$IMAGE_NAME" "$(
        { grep "^$IMAGE_NAME=" stackgres-k8s/ci/build/target/image-digests || echo '=<not found>'; } \
          | cut -d = -f 2-)"
    done
echo "done"

echo

if [ "$MODULES" = "hashes" ]
then
  exit
fi

for MODULE in $MODULES
do
  build_image "$MODULE"
done
