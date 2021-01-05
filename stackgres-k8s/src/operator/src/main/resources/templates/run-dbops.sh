#!/bin/sh

LOCK_RESOURCE="$CLUSTER_CRD_NAME"
LOCK_RESOURCE_NAME="$CLUSTER_NAME"

. "$LOCAL_BIN_SHELL_UTILS_PATH"

run() {
  set +e

  touch "$SHARED_PATH/$KEBAB_OP_NAME.out"
  touch "$SHARED_PATH/$KEBAB_OP_NAME.err"

  set -e
  acquire_lock > /tmp/try-lock
  echo "Lock acquired"
  maintain_lock 2>&1 >> /tmp/try-lock &
  TRY_LOCK_PID=$!
  set +e

  tail -q -f "$SHARED_PATH/$KEBAB_OP_NAME.out" "$SHARED_PATH/$KEBAB_OP_NAME.err" &

  TAIL_PID="$!"

  sleep "$TIMEOUT" &

  TIMEOUT_PID="$!"

  run_op >> "$SHARED_PATH/$KEBAB_OP_NAME.out" 2>> "$SHARED_PATH/$KEBAB_OP_NAME.err" &

  PID="$!"

  (
  set +x
  while (kill -0 "$PID" && kill -0 "$TIMEOUT_PID" && kill -0 "$TRY_LOCK_PID") 2>/dev/null
  do
    true
  done
  )

  if kill -0 "$PID" 2>/dev/null
  then
    kill_with_childs "$PID"
    if ! kill -0 "$TIMEOUT_PID" 2>/dev/null
    then
      kill_with_childs "$TRY_LOCK_PID"
      echo "LOCK_LOST=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "TIMED_OUT=true" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "EXIT_CODE=1" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    elif ! kill -0 "$TRY_LOCK_PID" 2>/dev/null
    then
      kill_with_childs "$TIMEOUT_PID"
      echo "LOCK_LOST=true" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "TIMED_OUT=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "EXIT_CODE=1" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    else
      kill_with_childs "$TRY_LOCK_PID"
      kill_with_childs "$TIMEOUT_PID"
      echo "LOCK_LOST=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "TIMED_OUT=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "EXIT_CODE=1" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    fi
  else
    kill_with_childs "$TIMEOUT_PID"
    kill_with_childs "$TRY_LOCK_PID"
    wait "$PID"
    EXIT_CODE="$?"
    echo "LOCK_LOST=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    echo "TIMED_OUT=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    echo "EXIT_CODE=$EXIT_CODE" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
  fi

  kill_with_childs "$TAIL_PID"

  return 0
}

if [ -n "$RUN_SCRIPT_PATH" ]
then
  . "$RUN_SCRIPT_PATH"
fi

run
