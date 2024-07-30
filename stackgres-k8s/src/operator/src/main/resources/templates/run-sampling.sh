#!/bin/sh

DATABASE_NAME="${DATABASE:-sampling_$(printf '%x' "$(date +%s)")}"

run_op() {
  set +e

  (
  set -e

  run_sampling
  )

  EXIT_CODE="$?"

  if [ "x$DATABASE" = x ]
  then
    try_drop_sampling_database
  fi

  return "$EXIT_CODE"
}

run_sampling() {
  cd "$SHARED_PATH"
  TOP_QUERIES_ORDER="mean_exec_time DESC"
  case "$MODE" in
    (calls)
    TOP_QUERIES_ORDER="calls DESC"
    ;;
  esac

  (
  export PGHOST="$PRIMARY_PGHOST"

  if [ "x$DATABASE" = x ]
  then
    DATABASE_EXISTS="$(psql -t -A \
      -c "SELECT EXISTS (SELECT * FROM pg_database WHERE datname = '$DATABASE_NAME')")"
    if [ "$DATABASE_EXISTS" != 'f' ]
    then
      try_drop_sampling_database
    fi

    try_function_with_output psql -c "CREATE DATABASE $DATABASE_NAME"
    if [ "$(cat "$SHARED_PATH/exit_code")" = 0 ]
    then
      create_event_service "DatabaseCreated" "Normal" "Database $DATABASE_NAME created"
    else
      create_event_service "CreateDatabaseFailed" "Warning" "Can not create database $DATABASE_NAME: $(cat "$SHARED_PATH/output")"
      return 1
    fi
  fi

  create_event_service "BenchmarkInitializationStarted" "Normal" "Benchamrk initialization started"
  get_init_script > "$SHARED_PATH/init-script.sql"
  try_function_with_output psql -t -A -d "$DATABASE_NAME" -f "$SHARED_PATH/init-script.sql"
  if [ "$(cat "$SHARED_PATH/exit_code")" = 0 ]
  then
    create_event_service "BenchmarkInitialized" "Normal" "Benchamrk initialized"
  else
    create_event_service "BenchmarkInitializationFailed" "Warning" "Can not initialize benchmark: $(cat "$SHARED_PATH/output")"
    return 1
  fi

  create_event_service "BenchmarkStarted" "Normal" "Benchamrk started"
  echo "Waiting  $TOP_QUERIES_COLLECT_DURATION seconds for top queries stats to be collected..."
  sleep "$TOP_QUERIES_COLLECT_DURATION"
  try_function_with_output psql -t -A -d "$DATABASE_NAME" -c "CALL sampling.insert_topqueries()"
  if [ "$(cat "$SHARED_PATH/exit_code")" != 0 ]
  then
    create_event_service "BenchmarkFailed" "Warning" "Can not complete benchmark: $(cat "$SHARED_PATH/output")"
    return 1
  fi
  try_function_with_output psql -t -A -d "$DATABASE_NAME" -c "CALL sampling.insert_queries()"
  if [ "$(cat "$SHARED_PATH/exit_code")" != 0 ]
  then
    create_event_service "BenchmarkFailed" "Warning" "Can not complete benchmark: $(cat "$SHARED_PATH/output")"
    return 1
  fi
  try_function_with_output psql -t -A -d "$DATABASE_NAME" \
    -c "SELECT query_id, to_char(timestamp,'YYYY-MM-DD\"T\"HH:MM:SSZ') AS timestamp, query FROM sampling.queries"
  if [ "$(cat "$SHARED_PATH/exit_code")" != 0 ]
  then
    create_event_service "BenchmarkFailed" "Warning" "Can not complete benchmark: $(cat "$SHARED_PATH/output")"
    return 1
  fi
  sort -R < "$SHARED_PATH/output" \
    | head -n "${QUERIES}" \
    | tr '|' ' ' \
    | {
      INDEX=0
      while read -r QUERY_ID TIMESTAMP QUERY
      do
        printf %s "$QUERY_ID" > "$SHARED_PATH/query-id-$INDEX"
        printf %s "$TIMESTAMP" > "$SHARED_PATH/timestamp-$INDEX"
        printf %s "$QUERY" > "$SHARED_PATH/query-$INDEX"
        INDEX="$((INDEX + 1))"
      done
      }
  QUERY_IDS=""
  for INDEX in $(seq 0 "$((QUERIES - 1))")
  do
    if ! [ -f "$SHARED_PATH/query-id-$INDEX" ]
    then
      break
    fi
    QUERY_ID="$(cat "$SHARED_PATH/query-id-$INDEX")"
    if printf ' %s ' "$QUERY_IDS" | grep -qF " $QUERY_ID "
    then
      continue
    fi
    QUERY_IDS="$QUERY_IDS $QUERY_ID"
    try_function_with_output psql -t -A -d "$DATABASE_NAME" \
      -c "SELECT stats FROM sampling.topqueries WHERE query_id = $QUERY_ID"
    if [ "$(cat "$SHARED_PATH/exit_code")" != 0 ]
    then
      create_event_service "BenchmarkFailed" "Warning" "Can not complete benchmark: $(cat "$SHARED_PATH/output")"
      return 1
    fi
    cat "$SHARED_PATH/output" > "$SHARED_PATH/stats-$INDEX"
  done
  create_event_service "BenchmarkCompleted" "Normal" "Benchmark completed"
  )
}

try_drop_sampling_database() {
  (
  set +e
  DROP_RETRY=3
  while [ "$DROP_RETRY" -ge 0 ]
  do
    try_function_with_output psql \
      -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DATABASE_NAME' AND pid != pg_backend_pid()" \
      -c "DROP DATABASE $DATABASE_NAME"
    if [ "$(cat "$SHARED_PATH/exit_code")" = 0 ]
    then
      break
    fi
    create_event_service "DropDatabaseFailed" "Warning" "Can not drop $DATABASE_NAME database: $MESSAGE"
    DROP_RETRY="$((DROP_RETRY - 1))"
    sleep 3
  done
  )
}

get_init_script() {
  cat << EOF
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

DROP SCHEMA IF EXISTS sampling CASCADE;

CREATE SCHEMA sampling;

CREATE TABLE sampling.topqueries (query_id bigint, stats json);

CREATE OR REPLACE PROCEDURE sampling.insert_topqueries() LANGUAGE plpgsql AS \$plpgsql\$
BEGIN
  INSERT INTO sampling.topqueries
    $(get_top_queries_query);
END\$plpgsql\$;

CREATE TABLE sampling.queries (query text, query_id bigint, timestamp timestamptz);

CREATE INDEX queries_query_idx ON sampling.queries (query);

CREATE OR REPLACE PROCEDURE sampling.insert_queries() LANGUAGE plpgsql AS \$plpgsql\$
DECLARE
  last_timestamp timestamptz := clock_timestamp() + interval '$SAMPLING_DURATION seconds';
BEGIN
  WHILE last_timestamp > clock_timestamp() LOOP
    INSERT INTO sampling.queries
      SELECT query, query_id, clock_timestamp() FROM pg_stat_activity
      WHERE query_id IN (SELECT query_id FROM sampling.topqueries)
      AND NOT EXISTS (SELECT * FROM sampling.queries WHERE queries.query = pg_stat_activity.query);
    COMMIT;
    PERFORM pg_sleep(0.01);
  END LOOP;
END\$plpgsql\$;

SELECT pg_stat_statements_reset();
EOF
}

get_top_queries_query() {
  if [ "$MODE" != custom ]
  then
    DEFAULT_TOP_QUERIES_FILTER='^ *(with|select) '
    cat << EOF
SELECT queryid, (
  SELECT json_object_agg(stats_each.key, stats_each.value)
  FROM json_each_text(
    (row_to_json(pg_stat_statements)::jsonb
      - 'query'
      - 'queryid')::json) AS stats_each) AS stats
FROM pg_stat_statements
JOIN pg_database ON (pg_database.oid = dbid)
WHERE datname = '$TARGET_DATABASE'
AND query ~* '${TOP_QUERIES_FILTER:-$DEFAULT_TOP_QUERIES_FILTER}'
ORDER BY ${TOP_QUERIES_ORDER}
LIMIT GREATEST(
  ${TOP_QUERIES_MIN},
  (SELECT count(1)
    FROM pg_stat_statements
    JOIN pg_database ON (pg_database.oid = dbid)
    WHERE datname = '$TARGET_DATABASE'
    AND query ~* '${TOP_QUERIES_FILTER:-$DEFAULT_TOP_QUERIES_FILTER}')
  * $((100 - TOP_QUERIES_PERCENTILE)) / 100)
EOF
  else
    printf %s "$CUSTOM_TOP_QUERIES_QUERY"
  fi
}