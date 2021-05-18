#!/bin/sh

[ "$DEBUG" != true ] || set -x
SHELL_XTRACE="$(! echo $- | grep -q x || echo "-x")"

set -e

cd "$(dirname "$0")/../../.."

KIND_NAME="${KIND_NAME:-kind}"
CI_PIPELINE_ID="${CI_PIPELINE_ID:-$(date +%s)}"
KIND_CONTAINERD_CACHE_SHARED_TYPE="${KIND_CONTAINERD_CACHE_SHARED_TYPE:-zfs}"
KIND_CONTAINERD_SHARED_CACHE_SIZE="${KIND_CONTAINERD_SHARED_CACHE_SIZE:-100G}"

PPID="$(ps -o ppid "$$")"
PPID="$(printf '%s' "$PPID" | tail -n 1 | tr -d ' ')"
PPPID="$(cat /proc/"$PPID"/status | grep '^PPid:' | tr '\t' ' ' | cut -d ' ' -f 2)"
CALLER_UID="$(cat /proc/"$PPPID"/status | grep '^Uid:' | tr '\t' ' ' | cut -d ' ' -f 2)"
CALLER_GID="$(cat /proc/"$PPPID"/status | grep '^Gid:' | tr '\t' ' ' | cut -d ' ' -f 2)"

shared_cache_requires_reset_or_update() {
  shared_cache_requires_reset \
    || [ "$KIND_CONTAINERD_SHARED_CACHE_UPDATE" = true ]
}

shared_cache_requires_reset() {
  shared_cache_requires_reset_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

shared_cache_requires_reset_btrfs() {
  ! df -T /tmp/kind-cache/kind-shared | tail -n 1 | tr -s ' ' | cut -d ' ' -f 2 | grep -q '^btrfs$' \
    || ( [ "$KIND_CONTAINERD_SHARED_CACHE_RESET" = true ] \
      && [ "$(cat /tmp/kind-cache-reset-pipeline || true)" != "$CI_PIPELINE_ID" ] )
}

shared_cache_requires_reset_zfs() {
  ! zfs list -t snapshot kind-cache/kind-shared@base \
    || ( [ "$KIND_CONTAINERD_SHARED_CACHE_RESET" = true ] \
      && [ "$(cat /tmp/kind-cache-reset-pipeline || true)" != "$CI_PIPELINE_ID" ] )
}

reset_others_shared_caches() {
  reset_others_shared_caches_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

reset_others_shared_caches_btrfs() {
  umount /tmp/kind-cache/* 2>/dev/null || true
  zfs destroy -r kind-cache 2>/dev/null || true
  zpool destroy kind-cache 2>/dev/null || true
}

reset_others_shared_caches_zfs() {
  umount /tmp/kind-cache-btrfs 2>/dev/null || true
  umount /tmp/kind-cache 2>/dev/null || true
}

reset_shared_cache() {
  reset_shared_cache_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

reset_shared_cache_btrfs() {
  echo "Resetting cache using btrfs ..."
  remove_containers_using_shared_cache
  umount /tmp/kind-cache-btrfs 2>/dev/null || true
  umount /tmp/kind-cache 2>/dev/null || true
  losetup -a | grep '/kind-cache\.btrfs' | cut -d : -f 1 | xargs -r -I % losetup -d '%'
  rm -rf /tmp/kind-cache-btrfs
  rm -f /tmp/kind-cache.btrfs
  truncate -s"$KIND_CONTAINERD_SHARED_CACHE_SIZE" /tmp/kind-cache.btrfs
  losetup -f > /tmp/kind-cache.device
  losetup "$(cat /tmp/kind-cache.device)" /tmp/kind-cache.btrfs
  mkdir -p /tmp/kind-cache-btrfs
  mkfs.btrfs "$(cat /tmp/kind-cache.device)"
  mkdir -p /tmp/kind-cache-btrfs
  mount "$(cat /tmp/kind-cache.device)" /tmp/kind-cache-btrfs
  mkdir -p /tmp/kind-cache-btrfs/kind-shared
  mount -o bind /tmp/kind-cache-btrfs /tmp/kind-cache
  echo "done"
}

reset_shared_cache_zfs() {
  echo "Resetting cache using zfs ..."
  remove_containers_using_shared_cache
  umount /tmp/kind-cache/* 2>/dev/null || true
  zfs destroy -r kind-cache 2>/dev/null || true
  zpool destroy kind-cache 2>/dev/null || true
  losetup -a | grep '/kind-cache\.zfs' | cut -d : -f 1 | xargs -r -I % losetup -d '%'
  rm -rf /tmp/kind-cache
  rm -f /tmp/kind-cache.zfs
  truncate -s"$KIND_CONTAINERD_SHARED_CACHE_SIZE" /tmp/kind-cache.zfs
  losetup -f > /tmp/kind-cache.device
  losetup "$(cat /tmp/kind-cache.device)" /tmp/kind-cache.zfs
  zpool create -f -m legacy -O atime=off -O compression=lz4 -O recordsize=128K  kind-cache "$(cat /tmp/kind-cache.device)"
  zfs create -s -V "$KIND_CONTAINERD_SHARED_CACHE_SIZE" kind-cache/kind-shared
  mkdir -p /tmp/kind-cache/kind-shared
  mkfs.ext4 -F "$(get_zfs_dev kind-cache/kind-shared)"
  mount "$(get_zfs_dev kind-cache/kind-shared)" /tmp/kind-cache/kind-shared
  echo "done"
}

update_shared_cache() {
  echo "Updating shared cache ..."
  KIND_NAME=kind-shared KIND_CONTAINERD_CACHE_PATH=/tmp/kind-cache/kind-shared \
    sh $SHELL_XTRACE stackgres-k8s/e2e/e2e reset_k8s
  for IMAGE_TAG in $IMAGE_TAGS
  do
    IMAGE_TAG="$IMAGE_TAG" KIND_NAME=kind-shared \
      KIND_CONTAINERD_CACHE_PATH=/tmp/kind-cache/kind-shared \
      sh $SHELL_XTRACE stackgres-k8s/e2e/e2e load_operator_k8s
  done
  (cat /tmp/pulled-images-* || true) | sort > /tmp/pulled-images-base
  E2E_PULLED_IMAGES_PATH=/tmp/pulled-images-base KIND_NAME=kind-shared \
    KIND_CONTAINERD_CACHE_PATH=/tmp/kind-cache/kind-shared \
    sh $SHELL_XTRACE stackgres-k8s/e2e/e2e load_cached_images_from_local_repository 
  KIND_NAME=kind-shared KIND_CONTAINERD_CACHE_PATH=/tmp/kind-cache/kind-shared \
    sh $SHELL_XTRACE stackgres-k8s/e2e/e2e delete_k8s
  chown "$CALLER_UID:$CALLER_GID" ~/.kube/config
  chown -R "$CALLER_UID:$CALLER_GID" stackgres-k8s/e2e/target
  echo "done"
}

post_update_shared_cache() {
  if [ "$KIND_CONTAINERD_CACHE_SHARED_TYPE" = zfs ]
  then
    echo "Generating shared cache snapshot using zfs ..."
    zfs destroy kind-cache/kind-shared@base 2>/dev/null || true
    zfs snap kind-cache/kind-shared@base
    echo "done"
  fi
  echo "$CI_PIPELINE_ID" > /tmp/kind-cache-reset-pipeline
}

cache_requires_reset() {
  cache_requires_reset_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

cache_requires_reset_btrfs() {
  ! [ -f /tmp/kind-cache-btrfs/"$KIND_NAME" ] \
    || [ "$KIND_CONTAINERD_CACHE_RESET" = true ]
}

cache_requires_reset_zfs() {
  ! [ -f /tmp/kind-cache/"$KIND_NAME" ] \
    || [ "$KIND_CONTAINERD_CACHE_RESET" = true ]
}

reset_cache() {
  reset_cache_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

reset_cache_btrfs() {
  echo "Resetting cache for $KIND_NAME using btrfs ..."
  rm -rf /tmp/kind-cache-btrfs/"$KIND_NAME"
  mkdir -p /tmp/kind-cache-btrfs/"$KIND_NAME"
  cp -r --reflink /tmp/kind-cache-btrfs/kind-shared/. \
    /tmp/kind-cache-btrfs/"$KIND_NAME"/.
  echo "done"
}

reset_cache_zfs() {
  echo "Resetting cache for $KIND_NAME using zfs ..."
  docker rm -fv "$KIND_NAME"-control-plane
  umount /tmp/kind-cache/"$KIND_NAME" 2>/dev/null || true
  rm -rf /tmp/kind-cache/"$KIND_NAME"
  zfs destroy kind-cache/"$KIND_NAME" 2>/dev/null || true
  zfs clone kind-cache/kind-shared@base kind-cache/"$KIND_NAME"
  mkdir -p /tmp/kind-cache/"$KIND_NAME"
  mount "$(get_zfs_dev kind-cache/"$KIND_NAME")" /tmp/kind-cache/"$KIND_NAME"
  echo "done"
}

remove_containers_using_shared_cache() {
  docker ps --format '{{ .ID }} {{ .Mounts }}' --no-trunc | grep '\(^\|,\)/tmp/kind-cache/' \
    | tr -s ' ' | cut -d ' ' -f 1 | xargs -r -I % docker rm -fv %
}

get_zfs_dev() {
  [ -n "$1" ]
  ZDEV="$( (ls -1 /dev/zd* 2>/dev/null || true) \
    | while read ZDEV
      do
        ZPOOL="$(/lib/udev/zvol_id "$ZDEV")"
        echo "$ZDEV:$ZPOOL"
      done)"
  ZDEV="$(printf '%s' "$ZDEV" | grep ":$1$")"
  printf '%s' "$ZDEV" | cut -d : -f 1
}

"$@"