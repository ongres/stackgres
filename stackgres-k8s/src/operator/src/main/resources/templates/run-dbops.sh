#!/bin/sh

LOCK_RESOURCE="$CLUSTER_CRD_NAME"
LOCK_RESOURCE_NAME="$CLUSTER_NAME"

RESOURCE_CRD_NAME="$DBOPS_CRD_NAME"
RESOURCE_NAME="$DBOPS_NAME"

. "$LOCAL_BIN_SHELL_UTILS_PATH"

run() {
  set +e

  touch "$SHARED_PATH/$KEBAB_OP_NAME.out"
  touch "$SHARED_PATH/$KEBAB_OP_NAME.err"

  sleep "$TIMEOUT" &

  TIMEOUT_PID="$!"

  tail -q -f "$SHARED_PATH/$KEBAB_OP_NAME.out" "$SHARED_PATH/$KEBAB_OP_NAME.err" &

  TAIL_PID="$!"

  if [ "$EXCLUSIVE_OP" = true ]
  then
    (set -e; acquire_lock) > "$SHARED_PATH/try-lock" 2>&1 &
    ACQUIRE_LOCK_PID=$!
    (
    while { kill -0 "$TIMEOUT_PID" \
      && kill -0 "$ACQUIRE_LOCK_PID"; } 2>/dev/null
    do
      { sleep 1; } 2>/dev/null
    done
    )
    if ! kill -0 "$TIMEOUT_PID" 2>/dev/null
    then
      kill_with_childs "$ACQUIRE_LOCK_PID"
      release_lock >> "$SHARED_PATH/try-lock" 2>&1
      echo "Lock released"
      echo "LOCK_LOST=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "TIMED_OUT=true" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "EXIT_CODE=1" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      kill_with_childs "$TAIL_PID"
      return 0
    else
      wait "$ACQUIRE_LOCK_PID"
      LOCK_ACQUIRED="$?"
      if [ "$LOCK_ACQUIRED" = 0 ]
      then
        echo "Lock acquired"
        (set -e; maintain_lock) >> "$SHARED_PATH/try-lock" 2>&1 &
        TRY_LOCK_PID=$!
      else
        kill_with_childs "$TIMEOUT_PID"
        echo "Can not acquire lock"
        cat "$SHARED_PATH/try-lock"
        echo "LOCK_LOST=true" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
        echo "TIMED_OUT=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
        echo "EXIT_CODE=1" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
        kill_with_childs "$TAIL_PID"
        return 0
      fi
    fi
  fi

  run_op >> "$SHARED_PATH/$KEBAB_OP_NAME.out" 2>> "$SHARED_PATH/$KEBAB_OP_NAME.err" &

  PID="$!"

  (
  while { kill -0 "$PID" && kill -0 "$TIMEOUT_PID" \
    && { [ "$EXCLUSIVE_OP" != true ] || kill -0 "$TRY_LOCK_PID"; }; } 2>/dev/null
  do
    { sleep 1; } 2>/dev/null
  done
  )

  if kill -0 "$PID" 2>/dev/null
  then
    kill_with_childs "$PID"
    if ! kill -0 "$TIMEOUT_PID" 2>/dev/null
    then
      if [ "$EXCLUSIVE_OP" = true ]
      then
        kill_with_childs "$TRY_LOCK_PID"
        release_lock >> "$SHARED_PATH/try-lock" 2>&1
        echo "Lock released"
      fi
      echo "LOCK_LOST=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "TIMED_OUT=true" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "EXIT_CODE=1" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    elif [ "$EXCLUSIVE_OP" = true ] && ! kill -0 "$TRY_LOCK_PID" 2>/dev/null
    then
      kill_with_childs "$TIMEOUT_PID"
      echo "LOCK_LOST=true" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "TIMED_OUT=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "EXIT_CODE=1" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    else
      if [ "$EXCLUSIVE_OP" = true ]
      then
        kill_with_childs "$TRY_LOCK_PID"
        release_lock >> "$SHARED_PATH/try-lock" 2>&1
        echo "Lock released"
      fi
      kill_with_childs "$TIMEOUT_PID"
      echo "LOCK_LOST=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "TIMED_OUT=false" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      echo "EXIT_CODE=1" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    fi
  else
    kill_with_childs "$TIMEOUT_PID"
    if [ "$EXCLUSIVE_OP" = true ]
    then
      kill_with_childs "$TRY_LOCK_PID"
      release_lock >> "$SHARED_PATH/try-lock" 2>&1
      echo "Lock released"
    fi
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
