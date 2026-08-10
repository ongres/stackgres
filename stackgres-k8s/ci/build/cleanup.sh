#!/bin/sh

set -e

# Container engine used to list and remove the images. Used as a command prefix,
# so values with arguments like `podman --remote` are supported.
: "${CONTAINER_ENGINE:=docker}"

DOCKER_IMAGES="$($CONTAINER_ENGINE images --format '{{ .ID }}' | sort | uniq | xargs $CONTAINER_ENGINE inspect --format '{{ .Id }} {{ .Parent }}')"
DOCKER_IMAGES_PARENTS="$(echo "$DOCKER_IMAGES" | cut -d ' ' -f 2 | grep -v '^$' | sort | uniq)"
echo "Found $(echo "$DOCKER_IMAGES" | wc -l) images with $(echo "$DOCKER_IMAGES_PARENTS" | wc -l) parents"
while [ -n "$DOCKER_IMAGES_PARENTS" ]
do
    NEW_DOCKER_IMAGES="$(echo "$DOCKER_IMAGES_PARENTS" | xargs $CONTAINER_ENGINE inspect --format '{{ .Id }} {{ .Parent }}')"
    DOCKER_IMAGES="$(echo "$DOCKER_IMAGES"; echo "$NEW_DOCKER_IMAGES")"
    DOCKER_IMAGES_PARENTS="$(echo "$NEW_DOCKER_IMAGES" | cut -d ' ' -f 2 | grep -v '^$' | sort | uniq)"
    echo "Found $(echo "$NEW_DOCKER_IMAGES" | wc -l) images with $(echo "$DOCKER_IMAGES_PARENTS" | wc -l) parents"
done
DOCKER_IMAGES="$(echo "$DOCKER_IMAGES" | sort | uniq)"

echo "Cleanup all build images ..."

$CONTAINER_ENGINE images | grep '^registry\.gitlab\.com/ongresinc/stackgres/build/' \
  | sed 's/ \+/ /g' | cut -d ' ' -f 3 \
  | while read -r ID
    do
      CHILDS=""
      PARENTS="$ID"
      while [ -n "$PARENTS" ]
      do
        NEW_PARENTS=""
        for PARENT in $PARENTS
        do
          NEW_PARENTS="$NEW_PARENTS $(echo "$DOCKER_IMAGES" | grep " sha256:$PARENT" | cut -d ' ' -f 1 | cut -d ':' -f 2)"
        done
        CHILDS="$NEW_PARENTS $CHILDS"
        PARENTS="$NEW_PARENTS"
      done
      for CHILD in $CHILDS
      do 
        $CONTAINER_ENGINE rmi "$CHILD" 2>/dev/null || true
      done
      $CONTAINER_ENGINE rmi "$ID" 2>/dev/null || true
    done

echo "done"

echo

echo "Cleanup all build intermediate images ..."

$CONTAINER_ENGINE images --filter label=build-of \
  | sed 's/ \+/ /g' | cut -d ' ' -f 3 \
  | while read -r ID
    do
      CHILDS=""
      PARENTS="$ID"
      while [ -n "$PARENTS" ]
      do
        NEW_PARENTS=""
        for PARENT in $PARENTS
        do
          NEW_PARENTS="$NEW_PARENTS $(echo "$DOCKER_IMAGES" | grep " sha256:$PARENT" | cut -d ' ' -f 1 | cut -d ':' -f 2)"
        done
        CHILDS="$NEW_PARENTS $CHILDS"
        PARENTS="$NEW_PARENTS"
      done
      for CHILD in $CHILDS
      do 
        $CONTAINER_ENGINE rmi "$CHILD" 2>/dev/null || true
      done
      $CONTAINER_ENGINE rmi "$ID" 2>/dev/null || true
    done

echo "done"
