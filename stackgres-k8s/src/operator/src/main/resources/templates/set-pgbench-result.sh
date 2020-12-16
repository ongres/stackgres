#!/bin/sh

eval_in_place() {
eval "cat << EVAL_IN_PLACE_EOF
$*
EVAL_IN_PLACE_EOF
"
}

EXIT_CODE="$(grep '^EXIT_CODE=' "$SHARED_PATH/pgbench.out" | cut -d = -f 2)"
TIMED_OUT="$(grep '^TIMED_OUT=' "$SHARED_PATH/pgbench.out" | cut -d = -f 2)"
LAST_TRANSITION_TIME="$(date -Iseconds -u)"

if [ "$EXIT_CODE" = 0 ]
then
  SCALE_FACTOR="$(grep '^\s*scaling factor: ' "$SHARED_PATH/pgbench.out" | sed 's/\s\+//g' | cut -d : -f 2 \
    | grep -v '^$' || echo null)"
  TRANSACTION_PROCESSED="$(grep '^\s*number of transactions actually processed: ' "$SHARED_PATH/pgbench.out" \
    | sed 's/\s\+//g' | cut -d : -f 2 | cut -d / -f 1 | grep -v '^$' || echo null)"
  LATENCY_AVERAGE="$(grep '^\s*latency average = ' "$SHARED_PATH/pgbench.out" \
    | sed 's/\s\+//g' | cut -d = -f 2 | sed 's/[^0-9.]\+$//' | grep -v '^$' || echo null)"
  LATENCY_STDDEV="$(grep '^\s*latency stddev = ' "$SHARED_PATH/pgbench.out" \
    | sed 's/\s\+//g' | cut -d = -f 2 | sed 's/[^0-9.]\+$//' | grep -v '^$' || echo null)"
  TPS_INCLUDING_CONNECTIONS_ESTABLISHING="$(grep '^\s*tps = ' "$SHARED_PATH/pgbench.out" \
    | grep '(including connections establishing)$' | sed 's/\s\+//g' | cut -d = -f 2 | cut -d '(' -f 1 \
    | grep -v '^$' || echo null)"
  TPS_EXCLUDING_CONNECTIONS_ESTABLISHING="$(grep '^\s*tps = ' "$SHARED_PATH/pgbench.out" \
    | grep '(excluding connections establishing)$' | sed 's/\s\+//g' | cut -d = -f 2 | cut -d '(' -f 1 \
    | grep -v '^$' || echo null)"
  kubectl patch -n "$CLUSTER_NAMESPACE" "$DB_OPS_CRD_NAME" "$DB_OPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"replace","path":"/status/conditions","value":[
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DB_OPS_COMPLETED"),
      $(eval_in_place "$CONDITION_DB_OPS_FALSE_FAILED")
    ]
  },
  {"op":"replace","path":"/status/benchmark","value":{
      "pgbench": {
        "scaleFactor": $SCALE_FACTOR,
        "transactionsProcessed": $TRANSACTION_PROCESSED,
        "latencyAverage": $LATENCY_AVERAGE,
        "latencyStddev": $LATENCY_STDDEV,
        "tpsIncludingConnectionsEstablishing": $TPS_INCLUDING_CONNECTIONS_ESTABLISHING,
        "tpsExcludingConnectionsEstablishing": $TPS_EXCLUDING_CONNECTIONS_ESTABLISHING
      }
    }
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
