#!/bin/sh

DATABASE_NAME="${DATABASE:-pgbench_$(printf '%x' "$(date +%s)")}"

run_op() {
  set +e

  (
  set -e

  run_pgbench
  )

  EXIT_CODE="$?"

  if [ "x$DATABASE" = x ]
  then
    try_drop_pgbench_database
  fi

  return "$EXIT_CODE"
}

run_pgbench() {
  cd "$SHARED_PATH"
  MODE_PARAMS="-b $MODE"
  if [ -z "$MODE" ]
  then
    if "$READ_WRITE"
    then
      MODE_PARAMS='-b tpcb-like'
    else
      MODE_PARAMS='-b select-only'
    fi
  fi

  if [ "$MODE" = custom ]
  then
    printf %s "$INIT_SCRIPT" > init-script.sql
    MODE_PARAMS="$(printf '%s\n' "$SCRIPTS" | tr ',' '\n' \
      | {
        INDEX=0
        while read BUILTIN WEIGHT
        do
          if [ "$BUILTIN" != custom ]
          then
            printf '%s %s@%s ' -b "$BUILTIN" "$WEIGHT"
          else
            printf '%s %s@%s ' -f "script-${INDEX}.sql" "$WEIGHT"
            eval "printf %s \"\${SCRIPT_${INDEX}}\"" > "script-${INDEX}.sql"
          fi
          INDEX="$((INDEX + 1))"
        done
        }
      )"
  fi

  (
  export PGHOST="$PRIMARY_PGHOST"

  if [ "x$DATABASE" = x ]
  then
    DATABASE_EXISTS="$(psql -t -A \
      -c "SELECT EXISTS (SELECT * FROM pg_database WHERE datname = '$DATABASE_NAME')")"
    if [ "$DATABASE_EXISTS" != 'f' ]
    then
      try_drop_pgbench_database
    fi

    try_function_with_message psql -c "CREATE DATABASE $DATABASE_NAME"
    if [ "$(cat exit_code)" = 0 ]
    then
      create_event_service "DatabaseCreated" "Normal" "Database $DATABASE_NAME created"
    else
      create_event_service "CreateDatabaseFailed" "Warning" "Can not create database $DATABASE_NAME: $(cat message)"
      return 1
    fi
  fi

  create_event_service "BenchmarkInitializationStarted" "Normal" "Benchamrk initialization started"
  if [ -s init-script.sql ]
  then
    try_function_with_message psql -t -A -d "$DATABASE_NAME" -f init-script.sql
    if [ "$(cat exit_code)" = 0 ]
    then
      create_event_service "BenchmarkInitialized" "Normal" "Benchamrk initialized"
    else
      create_event_service "BenchmarkInitializationFailed" "Warning" "Can not initialize benchmark: $(cat message)"
      return 1
    fi
  else
    try_function_with_message pgbench \
      -s "$SCALE" \
      -i \
      $([ -z "$INIT_STEPS" ] || printf %s=%s --init-steps "$INIT_STEPS") \
      $([ -z "$FILLFACTOR" ] || printf %s=%s --fillfactor "$FILLFACTOR") \
      $([ "$NO_VACUUM" != true ] || printf %s --no-vacuum) \
      "$DATABASE_NAME"
    if [ "$(cat exit_code)" = 0 ]
    then
      create_event_service "BenchmarkInitialized" "Normal" "Benchamrk initialized"
    else
      create_event_service "BenchmarkInitializationFailed" "Warning" "Can not initialize benchmark: $(cat message)"
      return 1
    fi
  fi
  )

  if ! "$READ_WRITE"
  then
    create_event_service "BenchmarkPostInitializationStarted" "Normal" "Benchamrk post initialization started"
    until [ "$(PGHOST="$PRIMARY_PGHOST" psql -t -A -d "$DATABASE_NAME" \
      -c "SELECT NOT EXISTS (SELECT * FROM pg_stat_replication WHERE replay_lsn != pg_current_wal_lsn())")" = "t" ]
    do
      sleep 1
    done
    create_event_service "BenchmarkPostInitializationCompleted" "Normal" "Benchamrk post initialization completed"
  fi

  create_event_service "BenchmarkStarted" "Normal" "Benchamrk started"
  try_function_with_message pgbench $MODE_PARAMS \
    -M "$PROTOCOL" \
    -s "$SCALE" \
    -T "$DURATION" \
    -c "$CLIENTS" \
    -j "$JOBS" \
    -r \
    -P 1 \
    -l \
    --sampling-rate "$SAMPLING_RATE" \
    $([ "$FOREIGN_KEYS" != true ] || printf %s --foreign-keys) \
    $([ "$UNLOGGED_TABLES" != true ] || printf %s --unlogged-tables) \
    $([ -z "$PARTITION_METHOD" ] || printf %s=%s --partition-method "$PARTITION_METHOD") \
    $([ -z "$PARTITIONS" ] || printf %s=%s --partitions "$PARTITIONS") \
    "$DATABASE_NAME"
  if [ "$(cat exit_code)" = 0 ]
  then
    create_event_service "BenchmarkCompleted" "Normal" "Benchmark completed"
  else
    create_event_service "BenchmarkFailed" "Warning" "Can not complete benchmark: $(cat message)"
    return 1
  fi
}

try_drop_pgbench_database() {
  (
  set +e
  DROP_RETRY=3
  while [ "$DROP_RETRY" -ge 0 ]
  do
    try_function_with_message psql \
      -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DATABASE_NAME' AND pid != pg_backend_pid()" \
      -c "DROP DATABASE $DATABASE_NAME"
    if [ "$(cat exit_code)" = 0 ]
    then
      break
    fi
    create_event_service "DropDatabaseFailed" "Warning" "Can not drop $DATABASE_NAME database: $MESSAGE"
    DROP_RETRY="$((DROP_RETRY - 1))"
    sleep 3
  done
  )
}

try_function_with_message() {
  (
    set +e
    (
    { { { "$@" 2>&1; echo $? >&3; } | tee message >&4; }  3>&1 | { read EXIT_CODE; exit "$EXIT_CODE"; }; } 4>&1
    )
    printf %s "$?" > exit_code
  )
}
