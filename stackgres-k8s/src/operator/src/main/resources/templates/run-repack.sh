#!/bin/sh

set +e

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

touch "$SHARED_PATH/repack.out"
touch "$SHARED_PATH/repack.err"

tail -q -f "$SHARED_PATH/repack.out" "$SHARED_PATH/repack.err" &

TAIL_PID="$!"

sleep "$TIMEOUT" &

TIMEOUT_PID="$!"

(
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
) >> "$SHARED_PATH/repack.out" 2>> "$SHARED_PATH/repack.err" &

PID="$!"

wait -n "$PID" "$TIMEOUT_PID"
EXIT_CODE="$?"

if kill -0 "$PID" 2>/dev/null
then
  kill "$PID"
  echo "TIMED_OUT=true" >> "$SHARED_PATH/repack.out"
  echo "EXIT_CODE=1" >> "$SHARED_PATH/repack.out"
else
  kill "$TIMEOUT_PID"
  echo "TIMED_OUT=false" >> "$SHARED_PATH/repack.out"
  echo "EXIT_CODE=$EXIT_CODE" >> "$SHARED_PATH/repack.out"
fi

kill "$TAIL_PID"

true