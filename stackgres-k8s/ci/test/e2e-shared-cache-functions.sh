#!/bin/sh

[ "$DEBUG" != true ] || set -x
SHELL_XTRACE="$(! echo $- | grep -q x || echo "-x")"

set -e

cd "$(dirname "$0")/../../.."

KIND_NAME="${KIND_NAME:-kind}"
CI_PIPELINE_ID="${CI_PIPELINE_ID:-$(date +%s)}"
KIND_CONTAINERD_CACHE_SHARED_TYPE="${KIND_CONTAINERD_CACHE_SHARED_TYPE:-disabled}"
KIND_CONTAINERD_SHARED_CACHE_SIZE="${KIND_CONTAINERD_SHARED_CACHE_SIZE:-100G}"

PPID="$(ps -o ppid "$$")"
PPID="$(printf '%s' "$PPID" | tail -n 1 | tr -d ' ')"
PPPID="$(cat /proc/"$PPID"/status | grep '^PPid:' | tr '\t' ' ' | cut -d ' ' -f 2)"
CALLER_UID="$(cat /proc/"$PPPID"/status | grep '^Uid:' | tr '\t' ' ' | cut -d ' ' -f 2)"
CALLER_GID="$(cat /proc/"$PPPID"/status | grep '^Gid:' | tr '\t' ' ' | cut -d ' ' -f 2)"
RUN_ON_HOST="$(cat /proc/1/cgroup | grep -q '[^/]$' && echo "run_on_host" || echo "run")"

shared_cache_requires_reset_or_update() {
  shared_cache_requires_reset_or_update_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

shared_cache_requires_reset_or_update_disabled() {
  false
}

shared_cache_requires_reset_or_update_btrfs() {
  shared_cache_requires_reset \
    || ( [ "$KIND_CONTAINERD_SHARED_CACHE_UPDATE" = true ] \
      && [ "$(cat /tmp/kind-cache-reset-pipeline || true)" != "$CI_PIPELINE_ID" ] )
}

shared_cache_requires_reset_or_update_zfs() {
  shared_cache_requires_reset \
    || ! "$RUN_ON_HOST" zfs list -t snapshot kind-cache/kind-shared@base >/dev/null 2>&1 \
    || ( [ "$KIND_CONTAINERD_SHARED_CACHE_UPDATE" = true ] \
      && [ "$(cat /tmp/kind-cache-reset-pipeline || true)" != "$CI_PIPELINE_ID" ] )
}

shared_cache_requires_reset() {
  shared_cache_requires_reset_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

shared_cache_requires_reset_disabled() {
  false
}

shared_cache_requires_reset_btrfs() {
  ! [ -f /tmp/kind-cache-btrfs.reset ] \
    || ( [ "$KIND_CONTAINERD_SHARED_CACHE_RESET" = true ] \
      && [ "$(cat /tmp/kind-cache-reset-pipeline || true)" != "$CI_PIPELINE_ID" ] )
}

shared_cache_requires_reset_zfs() {
  ! "$RUN_ON_HOST" zfs list kind-cache/kind-shared >/dev/null 2>&1 \
    || ( [ "$KIND_CONTAINERD_SHARED_CACHE_RESET" = true ] \
      && [ "$(cat /tmp/kind-cache-reset-pipeline || true)" != "$CI_PIPELINE_ID" ] )
}

reset_others_shared_caches() {
  reset_others_shared_caches_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

reset_others_shared_caches_disabled() {
  reset_others_shared_caches_btrfs
  reset_others_shared_caches_zfs
}

reset_others_shared_caches_btrfs() {
  "$RUN_ON_HOST" umount /tmp/kind-cache/* 2>/dev/null || true
  "$RUN_ON_HOST" mount zfs destroy -r kind-cache 2>/dev/null || true
  "$RUN_ON_HOST" zpool destroy kind-cache 2>/dev/null || true
  "$RUN_ON_HOST" rm -f /tmp/kind-cache.zfs
}

reset_others_shared_caches_zfs() {
  "$RUN_ON_HOST" umount /tmp/kind-cache 2>/dev/null || true
  "$RUN_ON_HOST" rm -f /tmp/kind-cache.btrfs
}

reset_shared_cache() {
  reset_shared_cache_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

reset_shared_cache_btrfs() {
  echo "Resetting cache using btrfs ..."
  "$RUN_ON_HOST" rm -f /tmp/kind-cache-btrfs.reset
  remove_containers_using_shared_cache
  "$RUN_ON_HOST" umount /tmp/kind-cache 2>/dev/null || true
  losetup -a | grep '/kind-cache\.btrfs' | cut -d : -f 1 | xargs -r -I % losetup -d '%'
  "$RUN_ON_HOST" rm -rf /tmp/kind-cache
  truncate -s"$KIND_CONTAINERD_SHARED_CACHE_SIZE" /tmp/kind-cache.btrfs
  losetup -f > /tmp/kind-cache.device
  losetup "$(cat /tmp/kind-cache.device)" /tmp/kind-cache
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache
  "$RUN_ON_HOST" mkfs.btrfs "$(cat /tmp/kind-cache.device)"
  touch /tmp/kind-cache-btrfs.reset
  echo "done"
}

reset_shared_cache_zfs() {
  echo "Resetting cache using zfs ..."
  remove_containers_using_shared_cache
  "$RUN_ON_HOST" umount /tmp/kind-cache/* 2>/dev/null || true
  "$RUN_ON_HOST" zfs destroy -r kind-cache 2>/dev/null || true
  "$RUN_ON_HOST" zpool destroy kind-cache 2>/dev/null || true
  losetup -a | grep '/kind-cache\.zfs' | cut -d : -f 1 | xargs -r -I % losetup -d '%'
  "$RUN_ON_HOST" rm -rf /tmp/kind-cache
  rm -f /tmp/kind-cache.zfs
  truncate -s"$KIND_CONTAINERD_SHARED_CACHE_SIZE" /tmp/kind-cache.zfs
  losetup -f > /tmp/kind-cache.device
  losetup "$(cat /tmp/kind-cache.device)" /tmp/kind-cache.zfs
  "$RUN_ON_HOST" zpool create -f -m legacy -O atime=off -O compression=lz4 -O recordsize=128K  kind-cache "$(cat /tmp/kind-cache.device)"
  "$RUN_ON_HOST" zfs create -s -V "$KIND_CONTAINERD_SHARED_CACHE_SIZE" kind-cache/kind-shared
  "$RUN_ON_HOST" mkfs.ext4 -F /dev/zvol/kind-cache/kind-shared
  echo "done"
}

pre_update_shared_cache() {
  pre_update_shared_cache_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

pre_update_shared_cache_btrfs() {
  echo "Mounting shared cache using btrfs ..."
  "$RUN_ON_HOST" umount /tmp/kind-cache 2>/dev/null || true
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache
  "$RUN_ON_HOST" mount "$(cat /tmp/kind-cache.device)" /tmp/kind-cache
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache/kind-shared
  echo "done"
}

pre_update_shared_cache_zfs() {
  echo "Mounting shared cache using zfs ..."
  "$RUN_ON_HOST" umount /tmp/kind-cache/kind-shared 2>/dev/null || true
  "$RUN_ON_HOST" rm -rf /tmp/kind-cache/kind-shared
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache/kind-shared
  "$RUN_ON_HOST" mount /dev/zvol/kind-cache/kind-shared /tmp/kind-cache/kind-shared
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
  post_update_shared_cache_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

post_update_shared_cache_btrfs() {
  "$RUN_ON_HOST" touch /tmp/kind-cache/kind-shared/updated
}

post_update_shared_cache_zfs() {
  echo "Generating shared cache snapshot using zfs ..."
  "$RUN_ON_HOST" zfs destroy kind-cache/kind-shared@base 2>/dev/null || true
  "$RUN_ON_HOST" zfs snap kind-cache/kind-shared@base
  echo "done"
}

pre_cache() {
  pre_cache_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

pre_cache_disabled() {
  flock /tmp/kind-cache-index-lock sh -c \
    '
    [ -f /tmp/kind-cache-index ] || printf "%s\n" -1 > /tmp/kind-cache-index
    echo "$(( ( $(cat /tmp/kind-cache-index) + 1 ) % (E2E_JOBS + E2E_EXCLUSIVE_JOBS) ))" \
      > /tmp/kind-cache-index
    '
}

pre_cache_btrfs() {
  true
}

pre_cache_zfs() {
  true
}

cache_requires_reset() {
  cache_requires_reset_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

cache_requires_reset_disabled() {
  [ "$KIND_CONTAINERD_CACHE_RESET" = true ]
}

cache_requires_reset_btrfs() {
  "$RUN_ON_HOST" umount /tmp/kind-cache 2>/dev/null || true
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache
  "$RUN_ON_HOST" mount "$(cat /tmp/kind-cache.device)" /tmp/kind-cache
  ! "$RUN_ON_HOST" [ -f /tmp/kind-cache/"$KIND_NAME"/updated ] \
    || [ "$KIND_CONTAINERD_CACHE_RESET" = true ]
}

cache_requires_reset_zfs() {
  ! "$RUN_ON_HOST" zfs list kind-cache/"$KIND_NAME" >/dev/null 2>&1 \
    || [ "$KIND_CONTAINERD_CACHE_RESET" = true ]
}

reset_cache() {
  reset_cache_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

reset_cache_disabled() {
  echo "Resetting cache for $KIND_NAME ..."
  "$RUN_ON_HOST" rm -rf /tmp/kind-cache/kind-"$(cat /tmp/kind-cache-index)"
  echo "done"
}

reset_cache_btrfs() {
  echo "Resetting cache for $KIND_NAME using btrfs ..."
  docker rm -fv "$KIND_NAME"-control-plane 2>/dev/null
  "$RUN_ON_HOST" umount /tmp/kind-cache 2>/dev/null || true
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache
  "$RUN_ON_HOST" mount "$(cat /tmp/kind-cache.device)" /tmp/kind-cache
  "$RUN_ON_HOST" rm -rf /tmp/kind-cache/"$KIND_NAME"
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache/"$KIND_NAME"
  "$RUN_ON_HOST" cp -r --reflink /tmp/kind-cache/kind-shared/. \
    /tmp/kind-cache/"$KIND_NAME"/.
  "$RUN_ON_HOST" touch /tmp/kind-cache/"$KIND_NAME"/updated
  echo "done"
}

reset_cache_zfs() {
  echo "Resetting cache for $KIND_NAME using zfs ..."
  docker rm -fv "$KIND_NAME"-control-plane 2>/dev/null
  "$RUN_ON_HOST" umount /tmp/kind-cache/"$KIND_NAME" 2>/dev/null || true
  "$RUN_ON_HOST" rm -rf /tmp/kind-cache/"$KIND_NAME"
  "$RUN_ON_HOST" zfs destroy kind-cache/"$KIND_NAME" 2>/dev/null || true
  "$RUN_ON_HOST" zfs clone kind-cache/kind-shared@base kind-cache/"$KIND_NAME"
  echo "done"
}

mount_cache() {
  mount_cache_"$KIND_CONTAINERD_CACHE_SHARED_TYPE"
}

mount_cache_disabled() {
  echo "Creating cache folder for $KIND_NAME ..."
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache/kind-"$(cat /tmp/kind-cache-index)"
  echo "done"
}

mount_cache_btrfs() {
  echo "Mount cache for $KIND_NAME using btrfs ..."
  docker rm -fv "$KIND_NAME"-control-plane 2>/dev/null
  "$RUN_ON_HOST" umount /tmp/kind-cache 2>/dev/null || true
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache
  "$RUN_ON_HOST" mount "$(cat /tmp/kind-cache.device)" /tmp/kind-cache
  echo "done"
}

mount_cache_zfs() {
  echo "Mount cache for $KIND_NAME using zfs ..."
  docker rm -fv "$KIND_NAME"-control-plane 2>/dev/null
  "$RUN_ON_HOST" umount /tmp/kind-cache/"$KIND_NAME" 2>/dev/null || true
  "$RUN_ON_HOST" rm -rf /tmp/kind-cache/"$KIND_NAME"
  "$RUN_ON_HOST" mkdir -p /tmp/kind-cache/"$KIND_NAME"
  "$RUN_ON_HOST" mount /dev/zvol/kind-cache/"$KIND_NAME" /tmp/kind-cache/"$KIND_NAME"
  echo "done"
}

remove_containers_using_shared_cache() {
  docker ps --format '{{ .ID }} {{ .Mounts }}' --no-trunc | grep '\(^\|,\)/tmp/kind-cache/' \
    | tr -s ' ' | cut -d ' ' -f 1 | xargs -r -I % docker rm -fv %
}

run() {
  "$@"
}

run_on_host() {
  docker run --privileged --pid=host -i --rm alpine nsenter -t 1 -m -u -n -i -- "$@"
}

create_or_update_shared_cache() {
  reset_others_shared_caches
  if shared_cache_requires_reset_or_update
  then
    if shared_cache_requires_reset
    then
      reset_shared_cache
    fi
    pre_update_shared_cache
    update_shared_cache
    post_update_shared_cache
    echo "$CI_PIPELINE_ID" > /tmp/kind-cache-reset-pipeline
  fi
  pre_cache
  if cache_requires_reset
  then
    reset_cache
  fi
  mount_cache
}
