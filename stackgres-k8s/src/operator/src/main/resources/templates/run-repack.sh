#!/bin/sh

run_op() {
  set -e

  if [ -z "$DATABASES" ]
  then
    run_command -a
  else
    echo "$DATABASES" | while read CONFIG DATABASE
      do
        eval "$CONFIG"
        run_command -d "$DATABASE"
      done
  fi
}

run_command() {
  if "$NO_ORDER"
  then
    COMMAND="$COMMAND"' -n'
  fi
  if [ -n "$WAIT_TIMEOUT" ]
  then
    COMMAND="$COMMAND"" -T $WAIT_TIMEOUT"
  fi
  if "$NO_KILL_BACKEND"
  then
    COMMAND="$COMMAND"' -D'
  fi
  if "$NO_ANALYZE"
  then
    COMMAND="$COMMAND"' -Z'
  fi
  if "$EXCLUDE_EXTENSION"
  then
    COMMAND="$COMMAND"' -C'
  fi
  get_primary_instance
  kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" -- pg_repack $ARGS "$@"
}

get_primary_instance() {
  PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
  PRIMARY_INSTANCE="$(printf '%s' "$PRIMARY_POD" | cut -d / -f 2)"
  until kubectl wait pod -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" --for condition=Ready --timeout 0 >/dev/null 2>&1
  do
    sleep 1
    PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
    PRIMARY_INSTANCE="$(printf '%s' "$PRIMARY_POD" | cut -d / -f 2)"
  done
}
