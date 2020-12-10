#!/bin/sh

set +e

sleep "$TIMEOUT" &

TIMEOUT_PID="$!"

(
set -e

until [ "$(psql -t -A \
  -c "SELECT EXISTS (SELECT * FROM pg_database WHERE datname = 'pgbench')")" = 'f' ]
do
  psql -c "DROP DATABASE pgbench" || true
done

psql -c "CREATE DATABASE pgbench"

pgbench -s "$SCALE" -i pgbench

pgbench -s "$SCALE" -T "$DURATION" -c "$CLIENTS" -j "$JOBS" -r pgbench
) &

PID="$!"

wait -n "$PID" "$TIMEOUT_PID"
EXIT_CODE="$?"

if kill -0 "$PID" 2>/dev/null
then
  kill "$PID"
  echo "TIMED_OUT=true"
  echo "EXIT_CODE=1"
else
  kill "$TIMEOUT_PID"
  echo "TIMED_OUT=false"
  echo "EXIT_CODE=$EXIT_CODE"
fi