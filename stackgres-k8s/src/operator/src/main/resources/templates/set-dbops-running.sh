#!/bin/sh

. "$LOCAL_BIN_SHELL_UTILS_PATH"

create_event_queue

eval_in_place() {
eval "cat << EVAL_IN_PLACE_EOF
$*
EVAL_IN_PLACE_EOF
"
}

LAST_TRANSITION_TIME="$(date_iso8601)"
STARTED="$LAST_TRANSITION_TIME"

kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=merge \
  -p "$(cat << EOF
{
  "status": {
    "conditions":[
        $(eval_in_place "$CONDITION_DB_OPS_RUNNING"),
        $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
        $(eval_in_place "$CONDITION_DB_OPS_FALSE_FAILED")
    ],
    "opRetries": $CURRENT_RETRY,
    "opStarted": "$STARTED"
  }
}
EOF
    )"

create_event "DbOpStarted" "Normal" "Database operation $OP_NAME started"
