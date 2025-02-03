export SHELL_XTRACE=$(! echo $- | grep -q x || printf %s -x)
export HOME="$PG_BASE_PATH"
export PATRONI_POSTGRESQL_LISTEN="$(eval "echo $PATRONI_POSTGRESQL_LISTEN")"
export PATRONI_POSTGRESQL_CONNECT_ADDRESS="$(eval "echo $PATRONI_POSTGRESQL_CONNECT_ADDRESS")"
export PATRONI_RESTAPI_CONNECT_ADDRESS="$(eval "echo $PATRONI_RESTAPI_CONNECT_ADDRESS")"

echo "Setting up scripts..."
sh -e $SHELL_XTRACE "$TEMPLATES_PATH/setup-scripts.sh"

echo "Waiting for the controller to setup things..."
(
  START="$(date +%s)"
  set +x
  until [ "$(( $(date +%s -r "$PATRONI_START_FILE_PATH" 2>/dev/null || printf 0) - START ))" -ge 0 ]
  do
    sleep 1
  done
)

## Restore
if [ -n "$RECOVERY_FROM_BACKUP" ]
then
	echo "Creating script for restoring from backup"
  cat << 'RECOVERY_FROM_BACKUP_EOF' > "$PATRONI_CONFIG_PATH/recovery-from-backup.sh"
#!/bin/sh

set -e

if [ "x$SHELL_XTRACE" = x-x ]
then
  set -x
fi

if [ -n "$RESTORE_BACKUP_ERROR" ]
then
  echo "$RESTORE_BACKUP_ERROR" >&2
  exit 1
fi

if [ "$RESTORE_VOLUME_SNAPSHOT" = true ]
then
  if [ -d "$PG_DATA_PATH".backup ]
  then
    mv "$PG_DATA_PATH".backup "$PG_DATA_PATH"
  fi
  if ! [ -d "$PG_DATA_PATH" ]
  then
    echo "Path $PG_DATA_PATH was not found, can not restore from the volume snapshot"
    exit 1
  fi
  printf %s "$RESTORE_BACKUP_LABEL" | base64 -d > "$PG_DATA_PATH"/backup_label
  chmod 600 "$PG_DATA_PATH"/backup_label
  if [ "x$RESTORE_TABLESPACE_MAP" != x ]
  then
    printf %s "$RESTORE_TABLESPACE_MAP" | base64 -d > "$PG_DATA_PATH"/tablespace_map
    chmod 600 "$PG_DATA_PATH"/tablespace_map
  fi
  POD_DATA_PV_NAME_ENV_VAR="POD_$(printf %s "$POD_NAME" | tr '-' '_')_DATA_PV_NAME"
  touch "$PG_DATA_PATH/.already_restored_from_volume_snapshot_$(eval "printf %s \"\$$POD_DATA_PV_NAME_ENV_VAR\"")"
else
  wal-g backup-fetch "$PG_DATA_PATH" "$RESTORE_BACKUP_NAME"
fi
RECOVERY_FROM_BACKUP_EOF
  chmod 700 "$PATRONI_CONFIG_PATH/recovery-from-backup.sh"
fi

## Replication initialization from backup
if [ -n "$REPLICATION_INITIALIZATION_FROM_BACKUP" ]
then
  echo "Creating script for replication initialization from backup"
  cat << 'REPLICATION_INITIALIZATION_FROM_BACKUP_EOF' > "$PATRONI_CONFIG_PATH/replication-initialization-from-backup.sh"
#!/bin/sh

set -e

if [ "x$SHELL_XTRACE" = x-x ]
then
  set -x
fi

if [ -z "$REPLICATION_INITIALIZATION_BACKUP" ]
then
  echo "Skipping replication initialization from backup since no backup was found or all failed"
  exit 1
fi

if [ "$REPLICATION_INITIALIZATION_VOLUME_SNAPSHOT" = true ]
then
  if [ -d "$PG_DATA_PATH".backup ]
  then
    mv "$PG_DATA_PATH".backup "$PG_DATA_PATH"
  fi
  printf %s "$REPLICATION_INITIALIZATION_BACKUP_LABEL" | base64 -d > "$PG_DATA_PATH"/backup_label
  chmod 600 "$PG_DATA_PATH"/backup_label
  if [ "x$REPLICATION_INITIALIZATION_TABLESPACE_MAP" != x ]
  then
    printf %s "$REPLICATION_INITIALIZATION_TABLESPACE_MAP" | base64 -d > "$PG_DATA_PATH"/tablespace_map
    chmod 600 "$PG_DATA_PATH"/tablespace_map
  fi
  POD_DATA_PV_NAME_ENV_VAR="POD_$(printf %s "$POD_NAME" | tr '-' '_')_DATA_PV_NAME"
  touch "$PG_DATA_PATH/.already_restored_from_volume_snapshot_$(eval "printf %s \"\$$POD_DATA_PV_NAME_ENV_VAR\"")"
else
  wal-g backup-fetch "$PG_DATA_PATH" "$REPLICATION_INITIALIZATION_BACKUP_NAME"
fi
REPLICATION_INITIALIZATION_FROM_BACKUP_EOF
  chmod 700 "$PATRONI_CONFIG_PATH/replication-initialization-from-backup.sh"

  cat << 'REPLICATION_INITIALIZATION_FROM_BACKUP_FAILOVER_EOF' > "$PATRONI_CONFIG_PATH/replication-initialization-from-backup-failover.sh"
#!/bin/sh

set -e

if [ "x$SHELL_XTRACE" = x-x ]
then
  set -x
fi

if [ -z "$REPLICATION_INITIALIZATION_BACKUP" ]
then
  echo "Skipping replication initialization from backup failover since no backup was found or all failed"
  exit 1
fi

if ! [ -f "$PG_REPLICATION_INITIALIZATION_FAILED_BACKUP_PATH" ]
then
  mkdir -p "$PG_REPLICATION_BASE_PATH"
fi
printf %s "$REPLICATION_INITIALIZATION_BACKUP" \
  > "$PG_REPLICATION_INITIALIZATION_FAILED_BACKUP_PATH"
while true
do
  echo "Waiting for the cluster controller to restart the Pod"
  sleep 300
done
REPLICATION_INITIALIZATION_FROM_BACKUP_FAILOVER_EOF
  chmod 700 "$PATRONI_CONFIG_PATH/replication-initialization-from-backup-failover.sh"
fi

## Replication initialization from replica
if [ -n "$REPLICATION_INITIALIZATION_FROM_REPLICA" ]
then
  echo "Creating script for replication initialization from replica"
  cat << 'REPLICATION_INITIALIZATION_FROM_REPLICA_EOF' > "$PATRONI_CONFIG_PATH/replication-initialization-from-replica.sh"
#!/bin/sh

set -e

if [ "x$SHELL_XTRACE" = x-x ]
then
  set -x
fi

printf %s:%s: \
  "${PATRONI_READ_ONLY_SERVICE_NAME}" \
  "${REPLICATION_SERVICE_PORT}" \
  > "$PG_BASE_PATH/pgpass-replicas"
cat "$PG_BASE_PATH/pgpass" \
  | cut -d : -f 3- \
  >> "$PG_BASE_PATH/pgpass-replicas"
chmod 600 "$PG_BASE_PATH/pgpass-replicas"

PGPASSFILE="$PG_BASE_PATH/pgpass-replicas" \
  pg_basebackup \
  --pgdata "$PG_DATA_PATH" \
  -X stream \
  --dbname postgres://${PATRONI_REPLICATION_USERNAME}@${PATRONI_READ_ONLY_SERVICE_NAME}:${REPLICATION_SERVICE_PORT}/postgres \
  $([ "$PATRONI_LOG_LEVEL" != DEVEL ] || printf %s --verbose) \
  --checkpoint='fast'
REPLICATION_INITIALIZATION_FROM_REPLICA_EOF
  chmod 700 "$PATRONI_CONFIG_PATH/replication-initialization-from-replica.sh"
fi

## Patroni configuration 
echo "Creating patroni configuration"
cat << 'PATRONI_CONFIG_EOF' | exec-with-env "${PATRONI_ENV}" -- sh -e $SHELL_XTRACE
cat << EOF > "$PATRONI_CONFIG_FILE_PATH".tmp

# Custom initial config
$(
cat << PATRONI_INITIAL_CONFIG | eval "$(cat)"
cat << INNER_PATRONI_INITIAL_CONFIG
$PATRONI_INITIAL_CONFIG
INNER_PATRONI_INITIAL_CONFIG
PATRONI_INITIAL_CONFIG
)

#Reset ignored sections
ctl: null

name: ${PATRONI_NAME}

bootstrap:
  dcs:
$(printf %s "$PATRONI_DCS_CONFIG" | sed 's/^/    /')
  post_init: '${LOCAL_BIN_PATH}/post-init.sh'
$(
if [ -n "$RECOVERY_FROM_BACKUP" ]
then
  cat << RECOVERY_EOF
  method: recovery
  recovery:
    command: 'exec-with-env "${RESTORE_ENV}" -- ${PATRONI_CONFIG_PATH}/recovery-from-backup.sh'
    keep_existing_recovery_conf: False
    keep_data: true
    recovery_conf:
      restore_command: 'exec-with-env "${RESTORE_ENV}" -- wal-g wal-fetch %f %p'
$(
  [ -z "$RECOVERY_TARGET" ] || cat << RECOVERY_TARGET_EOF
      recovery_target: '$RECOVERY_TARGET'
RECOVERY_TARGET_EOF
)
$(
  [ -z "$RECOVERY_TARGET_TIMELINE" ] || cat << RECOVERY_TARGET_TIMELINE_EOF
      recovery_target_timeline: '$RECOVERY_TARGET_TIMELINE'
RECOVERY_TARGET_TIMELINE_EOF
)
$(
  [ -z "$RECOVERY_TARGET_INCUSIVE" ] || cat << RECOVERY_TARGET_INCUSIVE_EOF
      recovery_target_incusive: '$RECOVERY_TARGET_INCUSIVE'
RECOVERY_TARGET_INCUSIVE_EOF
)
$(
  [ -z "$RECOVERY_TARGET_NAME" ] || cat << RECOVERY_TARGET_NAME_EOF
      recovery_target_name: '$RECOVERY_TARGET_NAME'
RECOVERY_TARGET_NAME_EOF
)
$(
  [ -z "$RECOVERY_TARGET_XID" ] || cat << RECOVERY_TARGET_XID_EOF
      recovery_target_xid: '$RECOVERY_TARGET_XID'
RECOVERY_TARGET_XID_EOF
)
$(
  [ -z "$RECOVERY_TARGET_LSN" ] || cat << RECOVERY_TARGET_LSN_EOF
      recovery_target_lsn: '$RECOVERY_TARGET_LSN'
RECOVERY_TARGET_LSN_EOF
)
$(
  [ -z "$RECOVERY_TARGET_TIME" ] || cat << RECOVERY_TARGET_TIME_EOF
      recovery_target_time: '$RECOVERY_TARGET_TIME'
RECOVERY_TARGET_TIME_EOF
)
      recovery_target_action: 'promote'
RECOVERY_EOF
fi
)
  initdb:
  - auth-host: ${INITDB_AUTH_HOST:-scram-sha-256}
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
  use_slots: true
  use_pg_rewind: true
  remove_data_directory_on_rewind_failure: true
  use_unix_socket: true
  use_unix_socket_repl: true
  connect_address: '${PATRONI_KUBERNETES_POD_IP}:5432'
  listen: 0.0.0.0:5432
  pg_ctl_timeout: $PATRONI_PG_CTL_TIMEOUT
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
$(
  [ -z "$REPLICATION_INITIALIZATION_FROM_BACKUP" ] || cat << REPLICATION_INITIALIZATION_EOF
  - backup
  - backup_failover
REPLICATION_INITIALIZATION_EOF
)
$(
  [ -z "$REPLICATION_INITIALIZATION_FROM_REPLICA" ] || cat << REPLICATION_INITIALIZATION_EOF
  - replica_basebackup
REPLICATION_INITIALIZATION_EOF
)
  - basebackup
  basebackup:
$([ "$PATRONI_LOG_LEVEL" != DEVEL ] || printf %s '- verbose')
    - checkpoint: 'fast'
$(
  [ -z "$REPLICATION_INITIALIZATION_FROM_REPLICA" ] || cat << REPLICATION_INITIALIZATION_EOF
  replica_basebackup:
    command: 'exec-with-env "${PATRONI_ENV}" -- ${PATRONI_CONFIG_PATH}/replication-initialization-from-replica.sh'
REPLICATION_INITIALIZATION_EOF
)
$(
  [ -z "$REPLICATION_INITIALIZATION_FROM_BACKUP" ] || cat << REPLICATION_INITIALIZATION_EOF
  backup:
    command: 'exec-with-env "${REPLICATION_INITIALIZATION_ENV}" -- ${PATRONI_CONFIG_PATH}/replication-initialization-from-backup.sh'
    keep_data: true
    recovery_conf:
      restore_command: 'exec-with-env "${REPLICATION_INITIALIZATION_ENV}" -- wal-g wal-fetch %f %p'
      recovery_target_action: 'promote'
  backup_failover:
    command: 'exec-with-env "${REPLICATION_INITIALIZATION_ENV}" -- ${PATRONI_CONFIG_PATH}/replication-initialization-from-backup-failover.sh'
    recovery_conf:
      restore_command: 'exec-with-env "${REPLICATION_INITIALIZATION_ENV}" -- wal-g wal-fetch %f %p'
      recovery_target_action: 'promote'
REPLICATION_INITIALIZATION_EOF
)
$(
if [ -n "$RECOVERY_FROM_BACKUP" ] \
  && [ -n "$REPLICATE_FROM_BACKUP" ]
then
  cat << RECOVERY_EOF
  replicate:
    command: 'exec-with-env "${RESTORE_ENV}" -- ${PATRONI_CONFIG_PATH}/recovery-from-backup.sh'
    keep_data: true
    keep_existing_recovery_conf: False
    recovery_conf:
      restore_command: 'exec-with-env "${RESTORE_ENV}" -- wal-g wal-fetch %f %p'
$(
  [ -z "$RECOVERY_TARGET" ] || cat << RECOVERY_TARGET_EOF
      recovery_target: '$RECOVERY_TARGET'
RECOVERY_TARGET_EOF
)
$(
  [ -z "$RECOVERY_TARGET_TIMELINE" ] || cat << RECOVERY_TARGET_TIMELINE_EOF
      recovery_target_timeline: '$RECOVERY_TARGET_TIMELINE'
RECOVERY_TARGET_TIMELINE_EOF
)
$(
  [ -z "$RECOVERY_TARGET_INCUSIVE" ] || cat << RECOVERY_TARGET_INCUSIVE_EOF
      recovery_target_incusive: '$RECOVERY_TARGET_INCUSIVE'
RECOVERY_TARGET_INCUSIVE_EOF
)
$(
  [ -z "$RECOVERY_TARGET_NAME" ] || cat << RECOVERY_TARGET_NAME_EOF
      recovery_target_name: '$RECOVERY_TARGET_NAME'
RECOVERY_TARGET_NAME_EOF
)
$(
  [ -z "$RECOVERY_TARGET_XID" ] || cat << RECOVERY_TARGET_XID_EOF
      recovery_target_xid: '$RECOVERY_TARGET_XID'
RECOVERY_TARGET_XID_EOF
)
$(
  [ -z "$RECOVERY_TARGET_LSN" ] || cat << RECOVERY_TARGET_LSN_EOF
      recovery_target_lsn: '$RECOVERY_TARGET_LSN'
RECOVERY_TARGET_LSN_EOF
)
$(
  [ -z "$RECOVERY_TARGET_TIME" ] || cat << RECOVERY_TARGET_TIME_EOF
      recovery_target_time: '$RECOVERY_TARGET_TIME'
RECOVERY_TARGET_TIME_EOF
)
      recovery_target_action: 'shutdown'
RECOVERY_EOF
fi
)
watchdog:
  mode: off
tags: {}
EOF
PATRONI_CONFIG_EOF
chmod 600 "$PATRONI_CONFIG_FILE_PATH".tmp
mv "$PATRONI_CONFIG_FILE_PATH".tmp "$PATRONI_CONFIG_FILE_PATH"

cat << EOF > "${LOCAL_BIN_PATH}/postgres"
#!/bin/sh
chmod 700 "$PG_DATA_PATH"
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

if [ ! -f "$PG_BASE_PATH/.psqlrc" ]
then
  cat << EOF > "$PG_BASE_PATH/.psqlrc"
\pset pager off
EOF
fi

if [ -n "$RECOVERY_FROM_BACKUP" ]
then
  cat << 'PREPARE_RECOVERY_FROM_BACKUP_EOF' > "$PATRONI_CONFIG_PATH/prepare-recovery-from-backup.sh"
#!/bin/sh

set -e

if [ "x$SHELL_XTRACE" = x-x ]
then
  set -x
fi
echo "Running script for restoring from backup $([ "$RESTORE_VOLUME_SNAPSHOT" = true ] && printf '(snapshot)' || printf '(object storage)')"

initialize() {
  if [ "$RESTORE_VOLUME_SNAPSHOT" != true ]
  then
    return
  fi

  POD_DATA_PV_NAME_ENV_VAR="POD_$(printf %s "$POD_NAME" | tr '-' '_')_DATA_PV_NAME"
  while eval "test -z \"\$$POD_DATA_PV_NAME_ENV_VAR\""
  do
    echo "Waiting for $POD_DATA_PV_NAME_ENV_VAR env var to be set"
    sleep 5
  done

  if [ -f "$PG_DATA_PATH/.already_restored_from_volume_snapshot_$(eval "printf %s \"\$$POD_DATA_PV_NAME_ENV_VAR\"")" ]
  then
    return
  fi

  echo "File $PG_DATA_PATH/.already_restored_from_volume_snapshot_$(eval "printf %s \"\$$POD_DATA_PV_NAME_ENV_VAR\"") was not found, preparing restore of snapshot based backup"
  
  if [ -d "$PG_DATA_PATH".failed ] \
    && ! [ -d "$PG_DATA_PATH" ]
  then
    mv "$PG_DATA_PATH".failed "$PG_DATA_PATH"
  fi

  if [ -d "$PG_DATA_PATH" ] \
    && ! [ -d "$PG_DATA_PATH".backup ]
  then
    mv "$PG_DATA_PATH" "$PG_DATA_PATH".backup
  fi

  echo
  echo "WARNING: In case you upgraded from 1.14.2 or older StackGres release and the primary Pod do not start"
  echo " run the following commands on the patroni container of your primary Pod:"
  echo
  echo " mv $PG_DATA_PATH.backup $PG_DATA_PATH"
  echo " touch $PG_DATA_PATH/.already_restored_from_volume_snapshot_$(eval "printf %s \"\$$POD_DATA_PV_NAME_ENV_VAR\"")"
  echo
}

initialize

if [ -d "$PG_DATA_PATH" ] \
  && [ -d "$PG_DATA_PATH".backup ]
then
  echo "Warning: both ${PG_DATA_PATH} and ${PG_DATA_PATH}.backup folder already exists"
fi

PREPARE_RECOVERY_FROM_BACKUP_EOF
  chmod 700 "$PATRONI_CONFIG_PATH/prepare-recovery-from-backup.sh"

  exec-with-env "${RESTORE_ENV}" -- "$PATRONI_CONFIG_PATH/prepare-recovery-from-backup.sh"
fi

if [ -n "$REPLICATION_INITIALIZATION_FROM_BACKUP" ]
then
  cat << 'PREPARE_REPLICATION_INITIALIZATION_FROM_BACKUP_EOF' > "$PATRONI_CONFIG_PATH/prepare-replication-initialization-from-backup.sh"
#!/bin/sh

set -e

if [ "x$SHELL_XTRACE" = x-x ]
then
  set -x
fi
echo "Running script for restoring from backup $([ "$REPLICATION_INITIALIZATION_VOLUME_SNAPSHOT" = true ] && printf '(snapshot)' || printf '(object storage)')"

initialize() {
  if [ "$REPLICATION_INITIALIZATION_VOLUME_SNAPSHOT" != true ]
  then
    return;
  fi

  POD_DATA_PV_NAME_ENV_VAR="POD_$(printf %s "$POD_NAME" | tr '-' '_')_DATA_PV_NAME"
  while eval "test -z \"\$$POD_DATA_PV_NAME_ENV_VAR\""
  do
    echo "Waiting for $POD_DATA_PV_NAME_ENV_VAR env var to be set"
    sleep 5
  done

  if [ -f "$PG_DATA_PATH/.already_restored_from_volume_snapshot_$(eval "printf %s \"\$$POD_DATA_PV_NAME_ENV_VAR\"")" ]
  then
    return
  fi

  echo "File $PG_DATA_PATH/.already_restored_from_volume_snapshot_$(eval "printf %s \"\$$POD_DATA_PV_NAME_ENV_VAR\"") was not found, preparing restore of snapshot based backup"

  if [ -d "$PG_DATA_PATH".failed ] \
    && ! [ -d "$PG_DATA_PATH" ]
  then
    mv "$PG_DATA_PATH".failed "$PG_DATA_PATH"
  fi

  if [ -d "$PG_DATA_PATH" ] \
    && ! [ -d "$PG_DATA_PATH".backup ]
  then
    mv "$PG_DATA_PATH" "$PG_DATA_PATH".backup
  fi
}

initialize

if [ -d "$PG_DATA_PATH" ] \
  && [ -d "$PG_DATA_PATH".backup ]
then
  echo "Warning: both ${PG_DATA_PATH} and ${PG_DATA_PATH}.backup folder already exists"
fi

PREPARE_REPLICATION_INITIALIZATION_FROM_BACKUP_EOF
  chmod 700 "$PATRONI_CONFIG_PATH/prepare-replication-initialization-from-backup.sh"

  exec-with-env "${REPLICATION_INITIALIZATION_ENV}" -- "$PATRONI_CONFIG_PATH/prepare-replication-initialization-from-backup.sh"
fi

PATRONI_POSTGRESQL_BIN_DIR="${LOCAL_BIN_PATH}" exec exec-with-env "${PATRONI_ENV}" -- /usr/bin/patroni "$PATRONI_CONFIG_FILE_PATH"
