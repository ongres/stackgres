#!/bin/sh

set -e

USER_NAME=postgres
USER_ID="$(id "$USER_NAME" -u)"
GROUP_ID="$(id "$USER_NAME" -g)"
USER_SHELL=/bin/sh

POD_CGROUP_IO_MAX="$(find "$HOST_CGROUP_PATH" -maxdepth 5 -name io.max -path "*pod$(echo "$POD_UID" | tr - _).slice/io.max")"
if ! [ -f "$POD_CGROUP_IO_MAX" ]
then
  echo "Can not find cgroup for Pod uid $POD_UID under path $HOST_CGROUP_PATH" >&2
  exit 1
fi
echo "Setting user owner $USER_NAME uid:$USER_ID gid:$GROUP_ID for $POD_CGROUP_IO_MAX"
chown "$USER_ID:$GROUP_ID" "$POD_CGROUP_IO_MAX"
