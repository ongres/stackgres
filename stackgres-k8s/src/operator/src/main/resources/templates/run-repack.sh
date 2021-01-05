#!/bin/sh

run_op() {
  set -e
  
  if [ -z "$DATABASES" ]
  then
    run_command -a
  else
    echo "$DATABASES" | while read CONFIG DATABASE
      do
        eval "$CONFIG"
        run_command -d "$DATABASE"
      done
  fi
}

run_command() {
  COMMAND='pg_repack'
  if "$NO_ORDER"
  then
    COMMAND="$COMMAND"' -n'
  fi
  if [ -n "$WAIT_TIMEOUT" ]
  then
    COMMAND="$COMMAND"" -T $WAIT_TIMEOUT"
  fi
  if "$NO_KILL_BACKEND"
  then
    COMMAND="$COMMAND"' -D'
  fi
  if "$NO_ANALYZE"
  then
    COMMAND="$COMMAND"' -Z'
  fi
  if "$EXCLUDE_EXTENSION"
  then
    COMMAND="$COMMAND"' -C'
  fi
  $COMMAND "$@"
}
