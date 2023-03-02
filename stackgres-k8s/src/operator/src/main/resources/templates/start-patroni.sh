export SHELL_XTRACE=$(! echo $- | grep -q x || echo " -x")
export HOME="$PG_BASE_PATH"
export PATRONI_POSTGRESQL_LISTEN="$(eval "echo $PATRONI_POSTGRESQL_LISTEN")"
export PATRONI_POSTGRESQL_CONNECT_ADDRESS="$(eval "echo $PATRONI_POSTGRESQL_CONNECT_ADDRESS")"
export PATRONI_RESTAPI_CONNECT_ADDRESS="$(eval "echo $PATRONI_RESTAPI_CONNECT_ADDRESS")"

if [ -n "$RECOVERY_FROM_BACKUP" ] \
  || [ -n "$REPLICATE_FROM_BACKUP" ]
then
  cat << 'RECOVERY_FROM_BACKUP_EOF' > "$PATRONI_CONFIG_PATH/recovery-from-backup"
#!/bin/sh

if [ "x$SHELL_XTRACE" = x-x ]
then
  set -x
fi

if [ -n "$RESTORE_BACKUP_ERROR" ]
then
  echo "$RESTORE_BACKUP_ERROR" >&2
  exit 1
fi

wal-g backup-fetch "$PG_DATA_PATH" "$RESTORE_BACKUP_ID"
RECOVERY_FROM_BACKUP_EOF
  chmod 700 "$PATRONI_CONFIG_PATH/recovery-from-backup"
fi

cat << 'PATRONI_CONFIG_EOF' | exec-with-env "${PATRONI_ENV}" -- sh -e $SHELL_XTRACE
cat << EOF > "$PATRONI_CONFIG_FILE_PATH"
scope: ${PATRONI_SCOPE}
name: ${PATRONI_NAME}

bootstrap:
  post_init: '${LOCAL_BIN_PATH}/post-init.sh'
$(
if [ -n "$RECOVERY_FROM_BACKUP" ]
then
  cat << RECOVERY_EOF
  method: recovery
  recovery:
    command: 'exec-with-env "${RESTORE_ENV}" -- ${PATRONI_CONFIG_PATH}/recovery-from-backup'
    keep_existing_recovery_conf: False
    recovery_conf:
      restore_command: 'exec-with-env "${RESTORE_ENV}" -- wal-g wal-fetch %f %p'
$(
  [ -z "$RECOVERY_TARGET_TIME" ] || cat << RECOVERY_TARGET_TIME_EOF
      recovery_target_time: '$RECOVERY_TARGET_TIME'
RECOVERY_TARGET_TIME_EOF
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
  create_replica_methods:
  - basebackup
  basebackup:
$(
  [ "$PATRONI_LOG_LEVEL" != DEVEL ] || cat << 'BASEBACKUP_VERBOSE_EOF'
    - verbose
BASEBACKUP_VERBOSE_EOF
)
    - checkpoint: 'fast'
$(
if [ -n "$RECOVERY_FROM_BACKUP" ] \
  && [ -n "$REPLICATE_FROM_BACKUP" ]
then
  cat << RECOVERY_EOF
  replicate:
    command: 'exec-with-env "${RESTORE_ENV}" -- ${PATRONI_CONFIG_PATH}/recovery-from-backup'
    keep_existing_recovery_conf: False
    recovery_conf:
      restore_command: 'exec-with-env "${RESTORE_ENV}" -- wal-g wal-fetch %f %p'
$(
  [ -z "$RECOVERY_TARGET_TIME" ] || cat << RECOVERY_TARGET_TIME_EOF
      recovery_target_time: '$RECOVERY_TARGET_TIME'
RECOVERY_TARGET_TIME_EOF
)
      recovery_target_timeline: 'latest'
      recovery_target_action: 'shutdown'
RECOVERY_EOF
fi
)
watchdog:
  mode: off
tags: {}
EOF
PATRONI_CONFIG_EOF
chmod 600 "$PATRONI_CONFIG_FILE_PATH"

cat << EOF > "${LOCAL_BIN_PATH}/postgres"
#!/bin/sh
chmod -R 700 "$PG_DATA_PATH"
exec "$PG_BIN_PATH/postgres" "\$@"
EOF
chmod 755 "${LOCAL_BIN_PATH}/postgres"

for POSTGRES_BIN_FILE in "${PG_BIN_PATH}"/*
do
  if [ ! -f "${LOCAL_BIN_PATH}/${POSTGRES_BIN_FILE##*/}" ]
  then
    ln -s "${POSTGRES_BIN_FILE}" "${LOCAL_BIN_PATH}/${POSTGRES_BIN_FILE##*/}"
  fi
done

PATRONI_POSTGRESQL_BIN_DIR="${LOCAL_BIN_PATH}" exec exec-with-env "${PATRONI_ENV}" -- /usr/bin/patroni "$PATRONI_CONFIG_FILE_PATH"
