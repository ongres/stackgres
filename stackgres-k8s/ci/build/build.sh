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

ALL_MODULES="$(jq -r '.modules | to_entries[] | .key' stackgres-k8s/ci/build/target/config.json)"

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

echo "Modules to build: $COMPLETED_MODULES" 

for MODULE in $COMPLETED_MODULES
do
  build_image "$MODULE"
done
