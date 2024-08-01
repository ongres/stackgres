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
  QUERIES_COUNT="$QUERIES"
  QUERIES="$(
    for INDEX in $(seq 0 "$((QUERIES_COUNT - 1))")
    do
      if ! [ -f "$SHARED_PATH/query-id-$INDEX" ] \
        || ! [ -f "$SHARED_PATH/timestamp-$INDEX" ] \
        || ! [ -f "$SHARED_PATH/query-$INDEX" ]
      then
        break
      fi
      {
        jq -R . "$SHARED_PATH/query-id-$INDEX"
        jq -R . "$SHARED_PATH/timestamp-$INDEX"
        jq -R . "$SHARED_PATH/query-$INDEX"
      } \
        | jq -s '{id: .[0], timestamp: .[1], query: .[2]}'
    done \
      | jq -s .)"
  if [ "$OMIT_TOP_QUERIES_IN_STATUS" != true ]
  then
    TOP_QUERIES="$(
      for INDEX in $(seq 0 "$((QUERIES_COUNT - 1))")
      do
        if ! [ -f "$SHARED_PATH/query-id-$INDEX" ]
        then
          break
        fi
        if ! [ -f "$SHARED_PATH/stats-$INDEX" ]
        then
          continue
        fi
        {
          jq -R . "$SHARED_PATH/query-id-$INDEX"
          jq -R '. | fromjson' "$SHARED_PATH/stats-$INDEX"
        } \
          | jq -s '{id: .[0], stats: .[1]}'
      done \
        | jq -s .)"
  else
    TOP_QUERIES=null
  fi
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
      "sampling": {
        "queries": ${QUERIES},
        "topQueries": ${TOP_QUERIES}
      }
    }
  }
]
EOF
    )"
  create_event "DbOpCompleted" "Normal" "Database operation $OP_NAME completed"
}
