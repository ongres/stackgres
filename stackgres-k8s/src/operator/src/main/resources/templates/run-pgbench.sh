#!/bin/sh

set +e

touch "$SHARED_PATH/pgbench.out"
touch "$SHARED_PATH/pgbench.err"

tail -q -f "$SHARED_PATH/pgbench.out" "$SHARED_PATH/pgbench.err" &

TAIL_PID="$!"

sleep "$TIMEOUT" &

TIMEOUT_PID="$!"

(
set -e

(
export PGHOST="$PRIMARY_PGHOST"

until [ "$(psql -t -A \
  -c "SELECT EXISTS (SELECT * FROM pg_database WHERE datname = 'pgbench')")" = 'f' ]
do
  psql -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'pgbench' AND pid != pg_backend_pid()" \
    -c "DROP DATABASE pgbench" || true
done

psql -c "CREATE DATABASE pgbench"

pgbench -s "$SCALE" -i pgbench
)

if "$READ_WRITE"
then
  pgbench -M "$PROTOCOL" -s "$SCALE" -T "$DURATION" -c "$CLIENTS" -j "$JOBS" -r -P 1 pgbench
else
  PGBENCH_ACCOUNTS_COUNT="$(PGHOST="$PRIMARY_PGHOST" psql -t -A -d pgbench \
    -c "SELECT COUNT(*) FROM pgbench_accounts")"

  until [ "$(psql -t -A -d pgbench \
    -c "SELECT COUNT(*) FROM pgbench_accounts")" = "$PGBENCH_ACCOUNTS_COUNT" ]
  do
    sleep 1
  done

  pgbench -b "select-only" -M "$PROTOCOL" -s "$SCALE" -T "$DURATION" -c "$CLIENTS" -j "$JOBS" -r -P 1 pgbench
fi
) >> "$SHARED_PATH/pgbench.out" 2>> "$SHARED_PATH/pgbench.err" &

PID="$!"

wait -n "$PID" "$TIMEOUT_PID"
EXIT_CODE="$?"

if kill -0 "$PID" 2>/dev/null
then
  kill "$PID"
  echo "TIMED_OUT=true" >> "$SHARED_PATH/pgbench.out"
  echo "EXIT_CODE=1" >> "$SHARED_PATH/pgbench.out"
else
  kill "$TIMEOUT_PID"
  echo "TIMED_OUT=false" >> "$SHARED_PATH/pgbench.out"
  echo "EXIT_CODE=$EXIT_CODE" >> "$SHARED_PATH/pgbench.out"
fi

kill "$TAIL_PID"

true