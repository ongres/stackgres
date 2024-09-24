#!/bin/sh

while true
do
  CONFIG_HASH="$(md5sum "$COLLECTOR_CONFIG_PATH")"
  if [ "x$PREVIOUS_CONFIG_HASH" != "x$CONFIG_HASH" ]
  then
    kill -SIGHUP "$(ls -1d /proc/[0-9]* \
      | while read FILE
        do
          echo "${FILE##*/} -- $(cat "$FILE/cmdline" 2>/dev/null | tr '\0' ' ')"
        done \
      | grep ' -- [/]otelcol-contrib ' \
      | cut -d ' ' -f 1)"
    PREVIOUS_CONFIG_HASH="$CONFIG_HASH"
  fi 
  sleep 10
done
