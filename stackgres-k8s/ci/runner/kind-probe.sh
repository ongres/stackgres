#!/bin/sh

# Decides whether a pod can run the kind environment of the e2e tests with
# podman. Every step prints what it found and why it matters, so that a failure
# tells which of the paths described in the README the runner needs. It is meant
# to be run inside a pod created from kind-probe.yaml:
#
#   kubectl exec -i kind-probe -- sh -s < stackgres-k8s/ci/runner/kind-probe.sh
#
# It leaves nothing behind: the cluster it creates is deleted at the end.

KIND="${KIND:-kind-0.32.0}"
KIND_NAME="${KIND_NAME:-gonogo}"
export KIND_EXPERIMENTAL_PROVIDER=podman
export CONTAINERS_CONF="${CONTAINERS_CONF:-/etc/containers/containers-kind.conf}"

FAILED=false

# When podman talks to a service the containers run on its side, so nothing this
# pod has answers whether a kind node can be created: the devices, the cgroups
# and the paths that decide are the ones of the service. Only the checks that go
# through podman itself, and creating the cluster, mean anything then. This is
# the same distinction container_engine_is_remote makes in stackgres-k8s/e2e.
IS_REMOTE="$(podman info --format '{{ .Host.ServiceIsRemote }}' 2>/dev/null || printf false)"

check() {
  printf '\n== %s\n' "$1"
  shift
  if "$@"
  then
    printf '   OK\n'
  else
    printf '   FAILED\n'
    FAILED=true
  fi
}

# A uid_map mapping the id 0 of the pod to the id 0 of the node means the pod
# shares the user namespace of the node and its root is the root of the node,
# restricted by the capabilities. Any other value means the pod has its own user
# namespace, which is what path 1 of the README relies on.
# shellcheck disable=SC2317 # called through check
has_own_user_namespace() {
  [ "$(tr -s ' ' < /proc/self/uid_map | cut -d ' ' -f 3)" != 0 ]
}

# shellcheck disable=SC2317 # called through check
cgroups_are_v2() {
  [ "$(stat -f -c %T /sys/fs/cgroup)" = cgroup2fs ]
}

# shellcheck disable=SC2317 # called through check
cgroups_are_delegated() {
  mkdir /sys/fs/cgroup/probe.slice && rmdir /sys/fs/cgroup/probe.slice
}

printf '== identity\n'
id
printf 'uid_map:%s\n' "$(tr -s ' ' < /proc/self/uid_map | tr '\n' ';')"
printf 'podman service is remote: %s\n' "$IS_REMOTE"

if [ "$IS_REMOTE" = true ]
then
  printf '\n   The engine is a service, so what this pod has does not decide.\n'
  printf '   Skipping the checks on the user namespace, the cgroups and\n'
  printf '   /dev/net/tun of this container.\n'
else
  check "the pod has its own user namespace" has_own_user_namespace
  check "the cgroups are v2" cgroups_are_v2
  check "the cgroups of the pod are writable, so a node can be given one" \
    cgroups_are_delegated

  printf '\n== cgroup controllers podman reports\n'
  podman info --format '{{ .Host.CgroupControllers }}' 2>&1
  # kind refuses to create a cluster with a rootless podman unless this contains
  # cpu, memory and pids, and it is podman it asks. See
  # check_kind_podman_cgroup_delegation in stackgres-k8s/e2e/envs/kind.

  check "/dev/net/tun is present" test -c /dev/net/tun
fi

printf '\n== podman\n'
podman info --format \
  'rootless={{ .Host.Security.Rootless }} driver={{ .Store.GraphDriverName }} arch={{ .Version.OsArch }}' 2>&1
# The driver must be overlay. vfs means the container storage got no volume and
# the e2e tests would be unusably slow.

check "podman can give a container a network of its own" \
  podman run --rm --network bridge docker.io/library/alpine ip -o -4 addr show
check "podman can give a container cgroups of its own" \
  podman run --rm --cgroups=enabled docker.io/library/alpine \
    cat /sys/fs/cgroup/cgroup.controllers

if "$FAILED"
then
  printf '\nA requirement is missing, creating a cluster would only produce a\n'
  printf 'less readable error. Fix the above and run this again.\n'
  exit 1
fi

printf '\n== kind\n'
EXIT_CODE=0
"$KIND" create cluster --name "$KIND_NAME" --wait 120s || EXIT_CODE="$?"
if [ "$EXIT_CODE" = 0 ]
then
  kubectl --context "kind-$KIND_NAME" get nodes || EXIT_CODE="$?"
fi
"$KIND" delete cluster --name "$KIND_NAME" || true
podman network rm kind || true

exit "$EXIT_CODE"
