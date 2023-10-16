#!/bin/sh

RESOURCE_CRD_NAME="$SHARDED_DBOPS_CRD_NAME"
RESOURCE_NAME="$SHARDED_DBOPS_NAME"

. "$LOCAL_BIN_SHELL_UTILS_PATH"

eval_in_place() {
eval "cat << EVAL_IN_PLACE_EOF
$*
EVAL_IN_PLACE_EOF
"
}

LAST_TRANSITION_TIME="$(date_iso8601)"
STARTED="$LAST_TRANSITION_TIME"

kubectl patch "$SHARDED_DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_DBOPS_NAME" --type=merge \
  -p "$(cat << EOF
{
  "status": {
    "conditions":[
        $(eval_in_place "$CONDITION_DBOPS_RUNNING"),
        $(eval_in_place "$CONDITION_DBOPS_FALSE_COMPLETED"),
        $(eval_in_place "$CONDITION_DBOPS_FALSE_FAILED")
    ],
    "opStarted": "$STARTED"
  }
}
EOF
    )"

create_event_queue

create_event "ShardedDbOpStarted" "Normal" "Database operation $OP_NAME started"
