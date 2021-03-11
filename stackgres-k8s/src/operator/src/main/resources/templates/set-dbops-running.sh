#!/bin/sh

. "$LOCAL_BIN_SHELL_UTILS_PATH"

eval_in_place() {
eval "cat << EVAL_IN_PLACE_EOF
$*
EVAL_IN_PLACE_EOF
"
}

LAST_TRANSITION_TIME="$(date_iso8601)"
STARTED="$LAST_TRANSITION_TIME"

if [ "$(kubectl get "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" \
  --template='{{ if .status }}true{{ else }}false{{ end }}')" = "false" ]
then
  kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"add","path":"/status","value":{
      "conditions": [
        $(eval_in_place "$CONDITION_DB_OPS_RUNNING"),
        $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
        $(eval_in_place "$CONDITION_DB_OPS_FALSE_FAILED")
      ],
      "opRetries": $CURRENT_RETRY,
      "opStarted": "$STARTED"
    }
  }
]
EOF
    )"
else
  kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"replace","path":"/status","value":{
      "conditions": [
        $(eval_in_place "$CONDITION_DB_OPS_RUNNING"),
        $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
        $(eval_in_place "$CONDITION_DB_OPS_FALSE_FAILED")
      ],
      "opRetries": $CURRENT_RETRY,
      "opStarted": "$STARTED"
    }
  }
]
EOF
    )"
fi
