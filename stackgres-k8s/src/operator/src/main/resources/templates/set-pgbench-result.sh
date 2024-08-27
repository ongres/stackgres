#!/bin/sh

FORMAT_LOCALE="$(locale -a | grep -i '^C\.UTF' | head -n 1)"

set_completed() {
  if ! locale -a | grep -qxF "$FORMAT_LOCALE"
  then
    FAILURE="Required locale $FORMAT_LOCALE is not installed"
    EXIT_CODE=1
    set_failed
    return
  fi
  SCALE_FACTOR="$(grep '^\s*scaling factor: ' "$SHARED_PATH/$KEBAB_OP_NAME.out" | sed 's/\s\+//g' | cut -d : -f 2 \
    | grep -v '^$' || echo null)"
  TRANSACTION_PROCESSED="$(grep '^\s*number of transactions actually processed: ' "$SHARED_PATH/$KEBAB_OP_NAME.out" \
    | sed 's/\s\+//g' | cut -d : -f 2 | cut -d / -f 1 | grep -v '^$' || echo null)"
  LATENCY_AVERAGE="$(grep '^\s*latency average = ' "$SHARED_PATH/$KEBAB_OP_NAME.out" \
    | sed 's/\s\+//g' | cut -d = -f 2 | sed 's/[^0-9.]\+$//' | grep -v '^$' | format_measure || echo null)"
  LATENCY_STDDEV="$(grep '^\s*latency stddev = ' "$SHARED_PATH/$KEBAB_OP_NAME.out" \
    | sed 's/\s\+//g' | cut -d = -f 2 | sed 's/[^0-9.]\+$//' | grep -v '^$' | format_measure || echo null)"
  if [ "$POSTGRES_MAJOR_VERSION" -gt 13 ]
  then
    TPS_INCLUDING_CONNECTIONS_ESTABLISHING='null'
    TPS_EXCLUDING_CONNECTIONS_ESTABLISHING="$(grep '^\s*tps = ' "$SHARED_PATH/$KEBAB_OP_NAME.out" \
      | grep '(without initial connection time)$' | sed 's/\s\+//g' | cut -d = -f 2 | cut -d '(' -f 1 \
      | grep -v '^$' | format_measure || echo null)"
  else
    TPS_INCLUDING_CONNECTIONS_ESTABLISHING="$(grep '^\s*tps = ' "$SHARED_PATH/$KEBAB_OP_NAME.out" \
      | grep '(including connections establishing)$' | sed 's/\s\+//g' | cut -d = -f 2 | cut -d '(' -f 1 \
      | grep -v '^$' | format_measure || echo null)"
    TPS_EXCLUDING_CONNECTIONS_ESTABLISHING="$(grep '^\s*tps = ' "$SHARED_PATH/$KEBAB_OP_NAME.out" \
      | grep '(excluding connections establishing)$' | sed 's/\s\+//g' | cut -d = -f 2 | cut -d '(' -f 1 \
      | grep -v '^$' | format_measure || echo null)"
  fi
  if [ "${SCRIPTS%,*}" = "$SCRIPTS" ]
  then
    STATEMENTS_LATENCY_START="$(grep -n '^statement latencies .*:$' "$SHARED_PATH/$KEBAB_OP_NAME.out" | cut -d : -f 1)"
    STATEMENTS="$(tail -n +"$((STATEMENTS_LATENCY_START + 1))" "$SHARED_PATH/$KEBAB_OP_NAME.out" \
      | grep '\s\+[0-9.]\+\s\+[0-9]\+\s\+.*' \
      | while read -r LATENCY FAILURES COMMAND
        do
          echo true | jq -c \
            --arg LATENCY "$LATENCY" \
            --arg SCRIPT "0" \
            --arg COMMAND "$COMMAND" \
            '{latency: ($LATENCY | tonumber), script: ($SCRIPT | tonumber), command: $COMMAND, unit: "ms"}'
        done \
      | jq -c -s .)"
  else
    STATEMENTS_LATENCY_STARTS="$(grep -n '^ - statement latencies .*:$' "$SHARED_PATH/$KEBAB_OP_NAME.out" | cut -d : -f 1)"
    STATEMENTS_LATENCY_END="$(grep -c . "$SHARED_PATH/$KEBAB_OP_NAME.out")"
    STATEMENTS='[]'
    SCRIPT=0
    PREVIOUS_STATEMENTS_LATENCY_START=
    for STATEMENTS_LATENCY_START in $STATEMENTS_LATENCY_STARTS "$STATEMENTS_LATENCY_END"
    do
      if [ -z "$PREVIOUS_STATEMENTS_LATENCY_START" ]
      then
        PREVIOUS_STATEMENTS_LATENCY_START="$STATEMENTS_LATENCY_START"
        continue
      fi
      STATEMENTS="$(tail -n +"$((PREVIOUS_STATEMENTS_LATENCY_START + 1))" "$SHARED_PATH/$KEBAB_OP_NAME.out" \
        | head -n "$((STATEMENTS_LATENCY_START - PREVIOUS_STATEMENTS_LATENCY_START))" \
        | grep '\s\+[0-9.]\+\s\+[0-9]\+\s\+.*' \
        | while read -r LATENCY FAILURE COMMAND
          do
            echo true | jq -c \
              --arg LATENCY "$LATENCY" \
              --arg SCRIPT "$SCRIPT" \
              --arg COMMAND "$COMMAND" \
              '{latency: ($LATENCY | tonumber), script: ($SCRIPT | tonumber), command: $COMMAND, unit: "ms"}'
          done \
        | jq -c -s --arg statements "$STATEMENTS" '($statements | fromjson) + .')"
      PREVIOUS_STATEMENTS_LATENCY_START="$STATEMENTS_LATENCY_START"
      SCRIPT="$((SCRIPT + 1))"
    done
  fi
  TPS_INTERVAL_DURATION="$((DURATION))"
  if [ "$TPS_INTERVAL_DURATION" -lt 1000 ]
  then
    TPS_INTERVAL_DURATION=1000
  fi
  TPS_OVER_TIME_VALUES="$(cat "$SHARED_PATH"/pgbench_log.* \
    | jq -R '[splits(" +")]' \
    | jq -c -s --arg interval_duration "$TPS_INTERVAL_DURATION" '
      ($interval_duration | tonumber) as $interval_duration
        | map(map(tonumber))
        | sort_by(.[4])
        | . + [null]
        | reduce .[] as $current (
          {
            values: [],
            begin: null,
            previous: null,
            transactions: 0,
            previousTps: null
          };
          if $current != null and (.begin == null or ((($current[4] - .begin[4]) * 1000 + ($current[5] - .begin[5]) / 1000) <= $interval_duration))
          then {
            values: .values,
            begin: (if .begin == null then $current else .begin end),
            previous: $current,
            transactions: (.transactions + 1),
            previousTps: .previousTps
            }
          else (
            ((.transactions * 100000.0 / $interval_duration | round) / 100.0) as $tps
              | {
                values: (.values + [(if .previousTps == null then $tps else ($tps - .previousTps) end)]),
                begin: $current,
                previous: $current,
                transactions: 1,
                previousTps: $tps
                }
             )
          end)
        | .values
      ')"
  HDRHISTOGRAM="$(grep '^HDRHISTOGRAM: ' "$SHARED_PATH/$KEBAB_OP_NAME.out" | cut -d ' ' -f 2 \
    | printf '"%s"' "$(cat)" | grep -v '^""$' || echo null)"
  kubectl patch "$DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DBOPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"replace","path":"/status/conditions","value":[
      $(eval_in_place "$CONDITION_DBOPS_FALSE_RUNNING"),
      $(eval_in_place "$CONDITION_DBOPS_COMPLETED"),
      $(eval_in_place "$CONDITION_DBOPS_FALSE_FAILED")
    ]
  },
  {"op":"replace","path":"/status/benchmark","value":{
      "pgbench": {
        "scaleFactor": ${SCALE_FACTOR},
        "transactionsProcessed": ${TRANSACTION_PROCESSED},
        "latency": {
          "average": {
            "value": ${LATENCY_AVERAGE},
            "unit": "ms"
          },
          "standardDeviation": {
            "value": ${LATENCY_STDDEV},
            "unit": "ms"
          }
        },
        "transactionsPerSecond": {
          "includingConnectionsEstablishing": {
            "value": ${TPS_INCLUDING_CONNECTIONS_ESTABLISHING},
            "unit": "tps"
          },
          "excludingConnectionsEstablishing": {
            "value": ${TPS_EXCLUDING_CONNECTIONS_ESTABLISHING},
            "unit": "tps"
          },
          "overTime": {
            "intervalDuration": ${TPS_INTERVAL_DURATION},
            "intervalDurationUnit": "ms",
            "values": ${TPS_OVER_TIME_VALUES},
            "valuesUnit": "tps"
          }
        },
        "statements": ${STATEMENTS},
        "hdrHistogram": ${HDRHISTOGRAM}
      }
    }
  }
]
EOF
    )"
  create_event "DbOpCompleted" "Normal" "Database operation $OP_NAME completed"
}

format_measure() {
  MEASURE="$(cat)"
  [ "x$MEASURE" != x ] && LC_NUMERIC="$FORMAT_LOCALE" printf '%.2f' "$MEASURE"
}
