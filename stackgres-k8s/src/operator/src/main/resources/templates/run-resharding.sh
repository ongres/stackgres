#!/bin/sh

run_op() {
  set -e

  echo "Starting sharded dbops $NORMALIZED_OP_NAME"

  get_primary_pod

  resharding

  echo "Sharded DbOps $NORMALIZED_OP_NAME completed"
}

get_primary_pod() {
  kubectl get pod -n "$CLUSTER_NAMESPACE" -l "${COORDINATOR_CLUSTER_LABELS},${PATRONI_ROLE_KEY}=${PATRONI_PRIMARY_ROLE}" -o name > /tmp/current-primary
  if [ ! -s /tmp/current-primary ]
  then
    echo "FAILURE=$NORMALIZED_OP_NAME failed. Unable to find primary, resharding aborted" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    exit 1
  fi

  echo "Primary is $(cat /tmp/current-primary)"
}

resharding() {
  cat << EOF | { set +e; kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-primary)" -c "$PATRONI_CONTAINER_NAME" \
      -- sh -e $SHELL_XTRACE 2>&1; printf %s "$?" > /tmp/resharding-exit-code; } | tee /tmp/resharding
psql -d "$SHARDED_CLUSTER_DATABASE" -v ON_ERROR_STOP=1 \
  -c "SELECT * FROM get_ rebalance_table_shards_plan()" \
  -c "SELECT rebalance_table_shards(relation => NULL, threshold => ${THRESHOLD:-NULL}, drain_only => ${DRAIN_ONLY:-NULL}, rebalance_strategy => ${REBALANCE_STRATEGY:-NULL})"
EOF
  if [ "$(cat /tmp/resharding-exit-code)" != 0 ]
  then
    echo "FAILURE=$NORMALIZED_OP_NAME failed. Unable to perform resharding: $(cat /tmp/resharding)" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    exit 1
  fi
}

