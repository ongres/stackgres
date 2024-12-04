#!/bin/sh

. "$TEMPLATES_PATH/${LOCAL_BIN_SHELL_UTILS_PATH##*/}"

CONFIG_PATH=/etc/fluent-bit

update_config() {
  rm -Rf "$FLUENT_BIT_LAST_CONFIG_PATH"
  cp -Lr "$CONFIG_PATH" "$FLUENT_BIT_LAST_CONFIG_PATH"
}

has_config_changed() {
  for file in $(ls -1 "$CONFIG_PATH")
  do
    [ "$(cat "$CONFIG_PATH/$file" | md5sum)" \
      != "$(cat "$FLUENT_BIT_LAST_CONFIG_PATH/$file" | md5sum)" ] \
      && return || true
  done
  return 1
}

run_fluentbit() {
  set -x
  exec /usr/local/bin/fluent-bit \
    -c /etc/fluent-bit/fluentbit.conf
}

set +x
PID=
while true
do
  if ! is_child "$PID" || has_config_changed
  then
    if [ -n "$PID" ] && is_child "$PID"
    then
      >&2 echo "Restarting fluent-bit process"
      kill "$PID" || true
      wait "$PID" || true
    else
      >&2 echo "fluent-bit process not found, starting it"
    fi
    run_fluentbit &
    PID="$!"
    update_config
  fi
  sleep 5
done
