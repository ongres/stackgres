cat <<EOF >/restore/postgres.yml
scope: ${PATRONI_SCOPE}
name: ${PATRONI_NAME}

bootstrap:
  post_init: '/etc/patroni/post-init.sh'
  dcs:
    postgresql:
      use_pg_rewind: true
  method: wal_g
  wal_g:
    command: '/etc/patroni/restore/bootstrap'
    keep_existing_recovery_conf: False
    recovery_conf:
      restore_command: '/bin/sh /wal-g-restore-wrapper/wal-g wal-fetch %f %p'
      recovery_target_timeline: 'latest'
      recovery_target_action: 'promote'
  initdb:
  - auth-host: md5
  - auth-local: trust
  - encoding: UTF8
  - locale: C.UTF-8
  - data-checksums
  pg_hba:
  - host all all 0.0.0.0/0 md5
  - host replication ${PATRONI_REPLICATION_USERNAME} 0.0.0.0/0 md5
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
cat <<'EOF' >/restore/bootstrap
#!/bin/sh
SCOPE="$PATRONI_SCOPE"
DATADIR=/var/lib/postgresql/data
BACKUP="$RESTORE_BACKUP_ID"
while getopts d:s-: OPT; do
  if [ "$OPT" = "-" ]; then   # long option: reformulate OPT and OPTARG
    OPT="${OPTARG%%=*}"       # extract long option name
    OPTARG="${OPTARG#$OPT}"   # extract long option argument (may be empty)
    OPTARG="${OPTARG#=}"      # if long option argument, remove assigning `=`
  fi
  case "$OPT" in
    d | datadir ) DATADIR="$OPTARG" ;;
    s | scope )   SCOPE="$OPTARG" ;;
  esac
done
exec-with-env /etc/env/restore /etc/secret/restore \\
  -- wal-g backup-fetch "$DATADIR" "$BACKUP"
EOF
cat <<EOF >/restore/entrypoint
export LC_ALL=C.UTF-8

unset PATRONI_SUPERUSER_PASSWORD PATRONI_REPLICATION_PASSWORD

/usr/bin/patroni /etc/patroni/restore/postgres.yml
EOF
chmod a+x /restore/entrypoint
chmod a+x /restore/bootstrap
