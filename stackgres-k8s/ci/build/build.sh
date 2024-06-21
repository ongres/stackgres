#!/bin/sh

# shellcheck disable=SC1090
. "$(dirname "$0")/build-functions.sh"

set -e

if [ "$1" = "hashes" ] || [ "$1" = "digests" ]
then
  COMMAND="$1"
  shift
fi

init_config

if [ "$#" -gt 0 ]
then
  MODULES="$*"

  COMPLETED_MODULES=""

  for MODULE in $MODULES
  do
    CURRENT_MODULE="$MODULE"
    while true
    do
      PARENT_MODULE="$(jq -r ".stages[]|to_entries[]|select(.key == \"$CURRENT_MODULE\").value" stackgres-k8s/ci/build/target/config.json)"
      if [ -z "$PARENT_MODULE" ]
      then
        echo "Module $CURRENT_MODULE stage not defined in stackgres-k8s/ci/build/config.yaml"
        exit 1
      fi
      if [ "$PARENT_MODULE" = null ]
      then
        break
      fi
      if ! printf ' %s ' "$COMPLETED_MODULES" | tr '\n' ' ' | grep -qF " $PARENT_MODULE "
      then
        COMPLETED_MODULES="$PARENT_MODULE $COMPLETED_MODULES"
      fi
      CURRENT_MODULE="$PARENT_MODULE"
    done
    if ! printf ' %s ' "$COMPLETED_MODULES" | tr '\n' ' ' | grep -qF " $MODULE "
    then
      COMPLETED_MODULES="$COMPLETED_MODULES $MODULE"
    fi
  done

  MODULES="$COMPLETED_MODULES"
else
  MODULES="$(jq -r '.modules | to_entries[] | .key' stackgres-k8s/ci/build/target/config.json)"
fi

generate_image_hashes $MODULES

show_image_hashes

if [ "$COMMAND" = "hashes" ]
then
  exit
fi

BUILD_HASH="$(cat stackgres-k8s/ci/build/target/build_hash)"

find_image_digests "stackgres-k8s/ci/build/target/all-images.$BUILD_HASH" \
  > "stackgres-k8s/ci/build/target/found-image-digests.$BUILD_HASH"
cp "stackgres-k8s/ci/build/target/found-image-digests.$BUILD_HASH" "stackgres-k8s/ci/build/target/image-digests.$BUILD_HASH"

echo "Retrieved image digests:"
sort "stackgres-k8s/ci/build/target/all-images.$BUILD_HASH" | uniq \
  | while read -r IMAGE_NAME
    do
      printf ' - %s => %s\n' "$IMAGE_NAME" "$(
        { grep "^$IMAGE_NAME=" "stackgres-k8s/ci/build/target/image-digests.$BUILD_HASH" || echo '=<not found>'; } \
          | cut -d = -f 2-)"
    done
echo "done"

echo

if [ "$COMMAND" = "digests" ]
then
  exit
fi

echo "Building: $MODULES" 

for MODULE in $MODULES
do
  build_image "$MODULE"
done
