#!/bin/sh

IMAGES="$(yq -r '.[".images"]|to_entries[]|.value' stackgres-k8s/ci/build/config.yml)"
for IMAGE in $IMAGES
do
  if printf %s "$IMAGE" | grep -q '^registry.gitlab.com/ongresinc/stackgres/'
  then
    continue
  fi

  TAG="${IMAGE##*:}"
  NEW_TAG="$(crane ls "${IMAGE%:*}" \
    | grep "^${TAG%-*}.*[0-9]$" \
    | while read LINE
      do
        printf '%s ' "$LINE"
        printf %s "$LINE" | sed 's/[^0-9]\+/ /g'
        printf '\n'
      done \
    | while read V V1 V2 V3 V4
      do
        printf '%016d%016d%016d%016d %s\n' "$V1" "$V2" "$V3" "$V4" "$V"
      done \
    | sort -k 1 \
    | cut -d ' ' -f 2 \
    | tail -n 1)"
    
  if [ "x$TAG" = "x$NEW_TAG" ]
  then
    echo "Tag for image $IMAGE unchanged"
    continue
  fi
  
  if [ "x$NEW_TAG" = x ]
  then
    echo "$IMAGE not found!"
    continue
  fi
  
  echo "New tag for $IMAGE is ${IMAGE%:*}:$NEW_TAG"
  OLD_IMAGE_PATTERN="$(printf %s "$IMAGE" | sed 's/\./\\./g')"
  NEW_IMAGE_PATTERN="$(printf %s "${IMAGE%:*}:$NEW_TAG" | sed 's/\./\\./g')"
  sed -n "s#$OLD_IMAGE_PATTERN#$NEW_IMAGE_PATTERN#p" stackgres-k8s/ci/build/config.yml
  sed -i "s#$OLD_IMAGE_PATTERN#$NEW_IMAGE_PATTERN#" stackgres-k8s/ci/build/config.yml
done
