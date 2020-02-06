cat << EOF >> /wal-g-restore-wrapper/wal-g
#/bin/sh
$(env | grep "RESTORE_" | sed 's/RESTORE_//' | sed 's/^/export /')
exec wal-g "\$@"
EOF
chmod a+x /wal-g-restore-wrapper/wal-g
if [ -n "$RESTORE_ENDPOINT_HOSTNAME" ] && [ -n "$RESTORE_ENDPOINT_PORT" ]
then
  if nc -z "$RESTORE_ENDPOINT_HOSTNAME" "$RESTORE_ENDPOINT_PORT"
  then
    echo "Host $RESTORE_ENDPOINT_HOSTNAME:$RESTORE_ENDPOINT_PORT reachable"
  else
    echo "ERROR: Host $RESTORE_ENDPOINT_HOSTNAME:$RESTORE_ENDPOINT_PORT not reachable"
    exit 1
  fi
fi