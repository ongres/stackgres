#!/bin/sh

until grep -q '^EXIT_CODE=' "$SHARED_PATH/repack.out" 2>/dev/null
do
  sleep 1
done

eval_in_place() {
eval "cat << EVAL_IN_PLACE_EOF
$*
EVAL_IN_PLACE_EOF
"
}

EXIT_CODE="$(grep '^EXIT_CODE=' "$SHARED_PATH/repack.out" | cut -d = -f 2)"
TIMED_OUT="$(grep '^TIMED_OUT=' "$SHARED_PATH/repack.out" | cut -d = -f 2)"
LAST_TRANSITION_TIME="$(date -Iseconds -u)"

if [ "$EXIT_CODE" = 0 ]
then
  kubectl patch -n "$CLUSTER_NAMESPACE" "$DB_OPS_CRD_NAME" "$DB_OPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"replace","path":"/status/conditions","value":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_FAILED")
    ]
  }
]
EOF
    )"
elif [ "$TIMED_OUT" = "false" ]
then
  kubectl patch -n "$CLUSTER_NAMESPACE" "$DB_OPS_CRD_NAME" "$DB_OPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"replace","path":"/status/conditions","value":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_FAILED")
    ]
  }
]
EOF
    )"
else
  kubectl patch -n "$CLUSTER_NAMESPACE" "$DB_OPS_CRD_NAME" "$DB_OPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"replace","path":"/status/conditions","value":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_TIMED_OUT")
    ]
  }
]
EOF
    )"
fi

exit "$EXIT_CODE"
