#!/bin/sh

while true
do
  CONFIG_HASH="$(md5sum "$COLLECTOR_CONFIG_PATH")"
  if [ "x$PREVIOUS_CONFIG_HASH" != "x$CONFIG_HASH" ]
  then
    ls -1d /proc/[0-9]* \
      | while read FILE
        do
          echo "${FILE##*/} -- $(cat "$FILE/cmdline" 2>/dev/null | tr '\0' ' ')"
        done \
      | grep ' -- [/]otelcol-contrib ' \
      | cut -d ' ' -f 1 \
      | while read PID
        do
          kill -SIGHUP "$PID" || true
        done
    PREVIOUS_CONFIG_HASH="$CONFIG_HASH"
    otelcol-contrib --config "$COLLECTOR_CONFIG_PATH"
  fi 
  sleep 10
done
