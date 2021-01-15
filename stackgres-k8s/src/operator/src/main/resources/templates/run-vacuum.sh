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
