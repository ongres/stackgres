cat << EOF > /wal-g-restore-wrapper/wal-g
#/bin/sh
$(env | grep "RESTORE_" | sed 's/RESTORE_//' | sed 's/^/export /')
exec wal-g "\$@"
EOF
chmod a+x /wal-g-restore-wrapper/wal-g
