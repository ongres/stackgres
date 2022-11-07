#!/bin/sh

. "$TEMPLATES_PATH/${LOCAL_BIN_SHELL_UTILS_PATH##*/}"

run_postgres_exporter() {
  set -x
  exec /usr/local/bin/postgres_exporter --log.level="$PG_EXPORTER_LOG_LEVEL"
}

set +x
PID=
while true
do
  if ! is_child "$PID" \
    && [ -S "$PG_RUN_PATH/.s.PGSQL.$PG_PORT" ]
  then
    if [ -n "$PID" ] && is_child "$PID"
    then
      kill "$PID" || true
      wait "$PID" || true
    fi
    run_postgres_exporter &
    PID="$!"
  fi
  sleep 5
done