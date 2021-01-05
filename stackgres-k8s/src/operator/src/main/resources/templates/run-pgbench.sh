#!/bin/sh

run_op() {
  set -e
  
  (
  export PGHOST="$PRIMARY_PGHOST"
  
  until [ "$(psql -t -A \
    -c "SELECT EXISTS (SELECT * FROM pg_database WHERE datname = 'pgbench')")" = 'f' ]
  do
    psql -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'pgbench' AND pid != pg_backend_pid()" \
      -c "DROP DATABASE pgbench" || true
  done
  
  psql -c "CREATE DATABASE pgbench"
  
  pgbench -s "$SCALE" -i pgbench
  )
  
  if "$READ_WRITE"
  then
    pgbench -M "$PROTOCOL" -s "$SCALE" -T "$DURATION" -c "$CLIENTS" -j "$JOBS" -r -P 1 pgbench
  else
    PGBENCH_ACCOUNTS_COUNT="$(PGHOST="$PRIMARY_PGHOST" psql -t -A -d pgbench \
      -c "SELECT COUNT(*) FROM pgbench_accounts")"
  
    until [ "$(psql -t -A -d pgbench \
      -c "SELECT COUNT(*) FROM pgbench_accounts")" = "$PGBENCH_ACCOUNTS_COUNT" ]
    do
      sleep 1
    done
  
    pgbench -b "select-only" -M "$PROTOCOL" -s "$SCALE" -T "$DURATION" -c "$CLIENTS" -j "$JOBS" -r -P 1 pgbench
  fi
}