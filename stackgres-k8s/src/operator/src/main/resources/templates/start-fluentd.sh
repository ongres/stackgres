#!/bin/sh

. "$TEMPLATES_PATH/${LOCAL_BIN_SHELL_UTILS_PATH##*/}"

CONFIG_PATH=/etc/fluentd

update_config() {
  rm -Rf "$FLUENTD_LAST_CONFIG_PATH"
  cp -Lr "$CONFIG_PATH" "$FLUENTD_LAST_CONFIG_PATH"
}

has_config_changed() {
  for file in $(ls -1 "$CONFIG_PATH")
  do
    [ "$(cat "$CONFIG_PATH/$file" | md5sum)" \
      != "$(cat "$FLUENTD_LAST_CONFIG_PATH/$file" | md5sum)" ] \
      && return || true
  done
  return 1
}

has_config_workers_changed() {
  [ "$(grep '^[[:space:]]*workers[[:space:]]\+[0-9]\+$' "$CONFIG_PATH"/fluentd.conf)" != \
    "$(grep '^[[:space:]]*workers[[:space:]]\+[0-9]\+$' "$FLUENTD_LAST_CONFIG_PATH"/fluentd.conf)" ]
}

run_fluentd() {
  set -x
  exec /usr/local/bin/fluentd \
    -c /etc/fluentd/fluentd.conf
}

echo 'Wait for postgres to be up, running and initialized'
until curl -s localhost:8008/readiness --fail > /dev/null; do sleep 1; done
export TMPDIR=/tmp/fluentd

set +x
PID=
while true
do
  if ! is_child "$PID" || has_config_changed
  then
    if [ -z "$PID" ] || ! is_child "$PID" || has_config_workers_changed
    then
      if [ -n "$PID" ] && is_child "$PID"
      then
        >&2 echo "Restarting fluentd process"
        kill "$PID"
        wait "$PID" || true
      else
        >&2 echo "fluentd process not found, starting it"
      fi
      run_fluentd &
      PID="$!"
    else
      >&2 echo "Reloading fluentd configuration"
      kill -n USR2 "$PID"
    fi
    update_config
  fi
  sleep 5
done
