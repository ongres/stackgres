export HOME="$PG_BASE_PATH"
export PATRONI_POSTGRESQL_LISTEN="$(eval "echo $PATRONI_POSTGRESQL_LISTEN")"
export PATRONI_POSTGRESQL_CONNECT_ADDRESS="$(eval "echo $PATRONI_POSTGRESQL_CONNECT_ADDRESS")"
export PATRONI_RESTAPI_CONNECT_ADDRESS="$(eval "echo $PATRONI_RESTAPI_CONNECT_ADDRESS")"

if [ -n "$RECOVERY_FROM_BACKUP" ]
then
  cat << 'EOF' | exec-with-env "${RESTORE_ENV}" -- sh -ex
if [ -n "$ENDPOINT_HOSTNAME" ] && [ -n "$ENDPOINT_PORT" ]
then
  if cat < /dev/null > "/dev/tcp/$ENDPOINT_HOSTNAME/$ENDPOINT_PORT"
  then
    echo "Host $ENDPOINT_HOSTNAME:$ENDPOINT_PORT reachable"
  else
    echo "ERROR: Host $ENDPOINT_HOSTNAME:$ENDPOINT_PORT not reachable"
    exit 1
  fi
fi
EOF
fi

exec-with-env "${PATRONI_ENV}" -- sh -exc "$(cat << 'SH_EOF'
cat << EOF > "$PATRONI_CONFIG_PATH/postgres.yml"
scope: ${PATRONI_SCOPE}
name: ${PATRONI_NAME}

bootstrap:
  post_init: '${LOCAL_BIN_PATH}/post-init.sh'
$(
if [ -n "$RECOVERY_FROM_BACKUP" ]
then
  cat << RECOVERY_EOF
  method: wal_g
  wal_g:
    command: '${PATRONI_CONFIG_PATH}/bootstrap'
    keep_existing_recovery_conf: False
    recovery_conf:
      restore_command: 'exec-with-env "${RESTORE_ENV}" -- wal-g wal-fetch %f %p'
$(
  if [ -n "$RECOVERY_TARGET_TIME" ]
  then
    cat << RECOVERY_TARGET_TIME_EOF
      recovery_target_time: '$RECOVERY_TARGET_TIME'
RECOVERY_TARGET_TIME_EOF
  fi
)
      recovery_target_timeline: 'latest'
      recovery_target_action: 'promote'
RECOVERY_EOF
fi
)
  initdb:
  - auth-host: md5
  - auth-local: trust
  - encoding: UTF8
  - locale: C.UTF-8
  - data-checksums
  pg_hba:
  - 'host all all 0.0.0.0/0 md5'
  - 'host replication ${PATRONI_REPLICATION_USERNAME} 0.0.0.0/0 md5'
restapi:
  connect_address: '${PATRONI_KUBERNETES_POD_IP}:8008'
  listen: 0.0.0.0:8008
postgresql:
  use_pg_rewind: true
  remove_data_directory_on_rewind_failure: true
  use_unix_socket: true
  connect_address: '${PATRONI_KUBERNETES_POD_IP}:5432'
  listen: 0.0.0.0:5432
  authentication:
    superuser:
      username: '${PATRONI_SUPERUSER_USERNAME}'
      password: '${PATRONI_SUPERUSER_PASSWORD}'
    replication:
      username: '${PATRONI_REPLICATION_USERNAME}'
      password: '${PATRONI_REPLICATION_PASSWORD}'
  parameters:
    unix_socket_directories: '${PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY}'
    dynamic_library_path: '${PG_LIB_PATH}:${PG_EXTRA_LIB_PATH}'
  basebackup:
    checkpoint: 'fast'
watchdog:
  mode: off
tags: {}
EOF
SH_EOF
)"

if [ -n "$RECOVERY_FROM_BACKUP" ]
then
  cat << EOF > "$PATRONI_CONFIG_PATH/bootstrap"
#!/bin/sh

exec-with-env "${RESTORE_ENV}" \\
  -- sh -ec 'if test -n "$RESTORE_BACKUP_ERROR"; then echo "$RESTORE_BACKUP_ERROR" >&2; exit 1; fi'

exec-with-env "${RESTORE_ENV}" \\
  -- sh -ec 'wal-g backup-fetch "\$PG_DATA_PATH" "\$RESTORE_BACKUP_ID"'
EOF
  chmod a+x "$PATRONI_CONFIG_PATH/bootstrap"
fi

export LC_ALL=C.UTF-8

exec exec-with-env "${PATRONI_ENV}" -- /usr/bin/patroni "$PATRONI_CONFIG_PATH/postgres.yml"
