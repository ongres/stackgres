#!/bin/sh

set +e

run_command() {
  COMMAND='vacuumdb -v'
  if "$FULL"
  then
    COMMAND="$COMMAND"' -f'
  else
    if "$FREEZE"
    then
      COMMAND="$COMMAND"' -F'
    fi
    if "$DISABLE_PAGE_SKIPPING"
    then
      COMMAND="$COMMAND"' --disable-page-skipping'
    fi
  fi
  if "$ANALYZE"
  then
    COMMAND="$COMMAND"' -z'
  fi
  $COMMAND "$@"
}

touch "$SHARED_PATH/vacuum.out"
touch "$SHARED_PATH/vacuum.err"

tail -q -f "$SHARED_PATH/vacuum.out" "$SHARED_PATH/vacuum.err" &

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
) >> "$SHARED_PATH/vacuum.out" 2>> "$SHARED_PATH/vacuum.err" &

PID="$!"

wait -n "$PID" "$TIMEOUT_PID"
EXIT_CODE="$?"

if kill -0 "$PID" 2>/dev/null
then
  kill "$PID"
  echo "TIMED_OUT=true" >> "$SHARED_PATH/vacuum.out"
  echo "EXIT_CODE=1" >> "$SHARED_PATH/vacuum.out"
else
  kill "$TIMEOUT_PID"
  echo "TIMED_OUT=false" >> "$SHARED_PATH/vacuum.out"
  echo "EXIT_CODE=$EXIT_CODE" >> "$SHARED_PATH/vacuum.out"
fi

kill "$TAIL_PID"

true