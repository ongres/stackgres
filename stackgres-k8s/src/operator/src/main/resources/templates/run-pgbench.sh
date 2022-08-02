#!/bin/sh

DATABASE_NAME="pgbench_$(printf '%x' "$(date +%s)")"

run_op() {
  set +e

  (
  set -e

  run_pgbench
  )

  EXIT_CODE="$?"

  try_drop_pgbench_database

  return "$EXIT_CODE"
}

run_pgbench() {
  (
  export PGHOST="$PRIMARY_PGHOST"

  DATABASE_EXISTS="$(psql -t -A \
    -c "SELECT EXISTS (SELECT * FROM pg_database WHERE datname = '$DATABASE_NAME')")"
  if [ "$DATABASE_EXISTS" != 'f' ]
  then
    try_drop_pgbench_database
  fi

  if MESSAGE="$(psql -c "CREATE DATABASE $DATABASE_NAME" 2>&1)"
  then
    echo "$MESSAGE"
    create_event_service "DatabaseCreated" "Normal" "Database $DATABASE_NAME created"
  else
    echo "$MESSAGE"
    create_event_service "CreateDatabaseFailed" "Warning" "Can not create database $DATABASE_NAME: $MESSAGE"
    return 1
  fi

  create_event_service "BenchmarkInitializationStarted" "Normal" "Benchamrk initialization started"
  if MESSAGE="$(pgbench -s "$SCALE" -i "$DATABASE_NAME" 2>&1)"
  then
    echo "$MESSAGE"
    create_event_service "BenchmarkInitialized" "Normal" "Benchamrk initialized"
  else
    echo "$MESSAGE"
    create_event_service "BenchmarkInitializationFailed" "Warning" "Can not initialize benchmark: $MESSAGE"
    return 1
  fi
  )

  if "$READ_WRITE"
  then
    create_event_service "BenchmarkStarted" "Normal" "Benchamrk started"
    if MESSAGE="$(pgbench -M "$PROTOCOL" -s "$SCALE" -T "$DURATION" -c "$CLIENTS" -j "$JOBS" -r -P 1 "$DATABASE_NAME" 2>&1)"
    then
      echo "$MESSAGE"
      create_event_service "BenchmarkCompleted" "Normal" "Benchmark completed"
    else
      echo "$MESSAGE"
      create_event_service "BenchmarkFailed" "Warning" "Can not complete benchmark: $MESSAGE"
      return 1
    fi
  else
    create_event_service "BenchmarkPostInitializationStarted" "Normal" "Benchamrk post initialization started"
    PGBENCH_ACCOUNTS_COUNT="$(PGHOST="$PRIMARY_PGHOST" psql -t -A -d "$DATABASE_NAME" \
      -c "SELECT COUNT(*) FROM pgbench_accounts")"

    until [ "$(psql -t -A -d "$DATABASE_NAME" \
      -c "SELECT COUNT(*) FROM pgbench_accounts")" = "$PGBENCH_ACCOUNTS_COUNT" ]
    do
      sleep 1
    done
    create_event_service "BenchmarkPostInitializationCompleted" "Normal" "Benchamrk post initialization completed"

    create_event_service "BenchmarkStarted" "Normal" "Benchamrk started"
    if MESSAGE="$(pgbench -b "select-only" -M "$PROTOCOL" -s "$SCALE" -T "$DURATION" -c "$CLIENTS" -j "$JOBS" -r -P 1 "$DATABASE_NAME" 2>&1)"
    then
      echo "$MESSAGE"
      create_event_service "BenchmarkCompleted" "Normal" "Benchmark completed"
    else
      echo "$MESSAGE"
      create_event_service "BenchmarkFailed" "Warning" "Can not complete benchmark: $MESSAGE"
      return 1
    fi
  fi
}

try_drop_pgbench_database() {
  (
  set +e
  DROP_RETRY=3
  while [ "$DROP_RETRY" -ge 0 ]
  do
      if MESSAGE="$(psql \
        -c "SELECT pg_cancel_backend(pid) FROM pg_stat_activity WHERE datname = '$DATABASE_NAME' AND pid != pg_backend_pid()" \
        -c "DROP DATABASE $DATABASE_NAME" 2>&1)"
    then
      break
    fi
    create_event_service "DropDatabaseFailed" "Warning" "Can not drop $DATABASE_NAME database: $MESSAGE"
    DROP_RETRY="$((DROP_RETRY - 1))"
    sleep 3
  done
  )
}

