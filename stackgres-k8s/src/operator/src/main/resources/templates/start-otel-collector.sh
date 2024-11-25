#!/bin/sh

OTEL_COLLECTOR_LAST_CONFIG_PATH=/tmp/otel-collector
CONFIG_PATH="${COLLECTOR_CONFIG_PATH%/*}"

update_config() {
  rm -Rf "$OTEL_COLLECTOR_LAST_CONFIG_PATH"
  cp -Lr "$CONFIG_PATH" "$OTEL_COLLECTOR_LAST_CONFIG_PATH"
}

has_config_changed() {
  for file in $(ls -1 "$CONFIG_PATH")
  do
    [ "$(cat "$CONFIG_PATH/$file" | md5sum)" \
      != "$(cat "$OTEL_COLLECTOR_LAST_CONFIG_PATH/$file" | md5sum)" ] \
      && return || true
  done
  return 1
}

run_otel_collector() {
  set -x
  exec otelcol-contrib --config "$COLLECTOR_CONFIG_PATH"
}

is_child() {
  local PID="$1"
  grep -q "^PPid:[[:space:]]$$" "/proc/$PID/status" 2>/dev/null
}

set +x
PID=
while true
do
  if ! is_child "$PID" || has_config_changed
  then
    if [ -n "$PID" ] && is_child "$PID"
    then
      >&2 echo "Reloading otelcol-contrib config"
      kill -s HUP "$PID" || true
    else
      >&2 echo "otelcol-contrib process not found, starting it"
      run_otel_collector &
      PID="$!"
    fi
    update_config
  fi
  sleep 5
done

