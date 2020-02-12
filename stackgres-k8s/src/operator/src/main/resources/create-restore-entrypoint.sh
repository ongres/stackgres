cat << EOF > "$RESTORE_ENTRYPOINT_PATH/postgres.yml"
scope: ${PATRONI_SCOPE}
name: ${PATRONI_NAME}

bootstrap:
  post_init: '/etc/patroni/post-init.sh'
  dcs:
    postgresql:
      use_pg_rewind: true
  method: wal_g
  wal_g:
    command: '${RESTORE_ENTRYPOINT_PATH}/bootstrap'
    keep_existing_recovery_conf: False
    recovery_conf:
      restore_command: 'exec-with-env "${RESTORE_ENV}" -- wal-g wal-fetch %f %p'
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
watchdog:
  mode: off
EOF

cat << EOF > "$RESTORE_ENTRYPOINT_PATH/bootstrap"
#!/bin/sh
exec-with-env "$RESTORE_ENV" \\
  -- wal-g backup-fetch "$PG_DATA_PATH" "$RESTORE_BACKUP_ID"
EOF

cat << EOF > "$RESTORE_ENTRYPOINT_PATH/entrypoint"
export LC_ALL=C.UTF-8

unset PATRONI_SUPERUSER_PASSWORD PATRONI_REPLICATION_PASSWORD

/usr/bin/patroni "${RESTORE_ENTRYPOINT_PATH}/postgres.yml"
EOF

chmod a+x "$RESTORE_ENTRYPOINT_PATH/entrypoint"
chmod a+x "$RESTORE_ENTRYPOINT_PATH/bootstrap"
