cat << EOF > /tmp/env
export PATRONI_POSTGRESQL_LISTEN=$PATRONI_POSTGRESQL_LISTEN
export PATRONI_POSTGRESQL_CONNECT_ADDRESS=$PATRONI_POSTGRESQL_CONNECT_ADDRESS
EOF
source /tmp/env
if [ -f /etc/patroni/restore/entrypoint ]
then
  exec /etc/patroni/restore/entrypoint
else
  exec /entrypoint
fi