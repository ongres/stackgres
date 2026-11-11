#!/bin/sh

. "$TEMPLATES_PATH/${LOCAL_BIN_SHELL_UTILS_PATH##*/}"

run_postgres_exporter() {
  set -x
  exec /usr/local/bin/postgres_exporter --log.level="$PG_EXPORTER_LOG_LEVEL"
}

set +x
PID=
MONITOR_USERNAME=
while true
do
  # The Postgres exporter reads the username of the monitor user only when it starts, so it is
  # restarted when the file it reads it from is updated with a new one
  if [ -n "$PID" ] && is_child "$PID" \
    && [ "$(cat "$DATA_SOURCE_USER_FILE")" != "$MONITOR_USERNAME" ]
  then
    echo "Restarting postgres exporter since the username of the monitor user changed"
    kill "$PID" || true
    wait "$PID" || true
  fi
  if ! is_child "$PID" \
    && [ -S "$PG_RUN_PATH/.s.PGSQL.$PG_PORT" ]
  then
    if [ -n "$PID" ] && is_child "$PID"
    then
      kill "$PID" || true
      wait "$PID" || true
    fi
    MONITOR_USERNAME="$(cat "$DATA_SOURCE_USER_FILE")"
    run_postgres_exporter &
    PID="$!"
  fi
  sleep 5
done