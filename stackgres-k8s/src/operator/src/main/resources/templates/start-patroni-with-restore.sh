export HOME="$PG_BASE_PATH"
export PATRONI_POSTGRESQL_LISTEN="$(eval "echo $PATRONI_POSTGRESQL_LISTEN")"
export PATRONI_POSTGRESQL_CONNECT_ADDRESS="$(eval "echo $PATRONI_POSTGRESQL_CONNECT_ADDRESS")"
export PATRONI_RESTAPI_CONNECT_ADDRESS="$(eval "echo $PATRONI_RESTAPI_CONNECT_ADDRESS")"

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

cat << EOF > "$PATRONI_CONFIG_PATH/postgres.yml"
scope: ${PATRONI_SCOPE}
name: ${PATRONI_NAME}

bootstrap:
  post_init: '${LOCAL_BIN_PATH}/post-init.sh'
  method: wal_g
  wal_g:
    command: '${PATRONI_CONFIG_PATH}/bootstrap'
    keep_existing_recovery_conf: False
    recovery_conf:
      restore_command: 'exec-with-env "${RESTORE_ENV}" -- wal-g wal-fetch %f %p'
$(
  if [ -n "$RECOVERY_TARGET_TIME" ]
  then
    cat << SUB_EOF
      recovery_target_time: '$RECOVERY_TARGET_TIME'
SUB_EOF
  fi
)
      recovery_target_timeline: 'latest'
      recovery_target_action: 'promote'
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
      password: '${PATRONI_SUPERUSER_PASSWORD}'
    replication:
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

cat << EOF > "$PATRONI_CONFIG_PATH/bootstrap"
#!/bin/sh

exec-with-env "$RESTORE_ENV" \\
  -- sh -ec 'if test -n "$RESTORE_BACKUP_ERROR"; then echo "$RESTORE_BACKUP_ERROR" >&2; exit 1; fi'

exec-with-env "$RESTORE_ENV" \\
  -- sh -ec 'wal-g backup-fetch "\$PG_DATA_PATH" "\$RESTORE_BACKUP_ID"'
EOF
chmod a+x "$PATRONI_CONFIG_PATH/bootstrap"

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

export LC_ALL=C.UTF-8

unset PATRONI_SUPERUSER_PASSWORD PATRONI_REPLICATION_PASSWORD

PATRONI_POSTGRESQL_BIN_DIR="${LOCAL_BIN_PATH}" exec /usr/bin/patroni "$PATRONI_CONFIG_PATH/postgres.yml"
