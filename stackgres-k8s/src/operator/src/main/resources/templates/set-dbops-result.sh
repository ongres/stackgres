#!/bin/sh

. "$LOCAL_BIN_SHELL_UTILS_PATH"

eval_in_place() {
eval "cat << EVAL_IN_PLACE_EOF
$*
EVAL_IN_PLACE_EOF
"
}

set_completed() {
  kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=merge \
    -p "$(cat << EOF
{
  "status": {
    "conditions":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_FAILED")
    ]
  }
}
EOF
    )"
  create_event "DbOpCompleted" "Normal" "Database operation $OP_NAME completed"
}

set_timed_out() {
  kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=merge \
    -p "$(cat << EOF
{
  "status": {
    "conditions":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_TIMED_OUT")
    ]
  }
}
EOF
    )"

  create_event "DbOpTimeOut" "Warning" "Database operation $OP_NAME timed out"
}

set_lock_lost() {
  kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=merge \
    -p "$(cat << EOF
{
  "status": {
    "conditions":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_LOCK_LOST")
    ]
  }
}
EOF
    )"
}

set_failed() {
  if [ -z "$FAILURE" ]
  then
    kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=merge \
      -p "$(cat << EOF
{
  "status": {
    "conditions":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_FAILED")
    ]
  }
}
EOF
      )"

    create_event "DbOpFailed" "Warning" "Database operation $OP_NAME failed"
  else
    kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=merge \
      -p "$(cat << EOF
{
  "status": {
    "conditions":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_FAILED")
    ],
    "$OP_NAME": {
      "failure": $FAILURE
    }
  }
}
EOF
      )"

    create_event "DbOpFailed" "Warning" "Database operation $OP_NAME failed: $FAILURE"
  fi
}

set_result() {
  read_events_service_loop &
  READ_EVENTS_SERVICE_PID="$!"
  until grep -q '^EXIT_CODE=' "$SHARED_PATH/$KEBAB_OP_NAME.out" 2>/dev/null
  do
    sleep 1
  done

  kill -SIGINT "$READ_EVENTS_SERVICE_PID" || true

  EXIT_CODE="$(grep '^EXIT_CODE=' "$SHARED_PATH/$KEBAB_OP_NAME.out" | cut -d = -f 2)"
  TIMED_OUT="$(grep '^TIMED_OUT=' "$SHARED_PATH/$KEBAB_OP_NAME.out" | cut -d = -f 2)"
  LOCK_LOST="$(grep '^LOCK_LOST=' "$SHARED_PATH/$KEBAB_OP_NAME.out" | cut -d = -f 2)"
  FAILURE="$(grep '^FAILURE=' "$SHARED_PATH/$KEBAB_OP_NAME.out" | cut -d = -f 2 | sed 's/^\(.*\)$/"\1"/')"
  LAST_TRANSITION_TIME="$(date_iso8601)"

  if [ "$EXIT_CODE" = 0 ]
  then
    set_completed
  elif [ "$TIMED_OUT" = "true" ]
  then
    set_timed_out
  elif [ "$LOCK_LOST" = "true" ]
  then
    set_lock_lost
  else
    set_failed
  fi

  return "$EXIT_CODE"
}

if [ -n "$SET_RESULT_SCRIPT_PATH" ]
then
  . "$SET_RESULT_SCRIPT_PATH"
fi

set_result
