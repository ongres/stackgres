cat << EOF > /wal-g-restore-wrapper/wal-g
#/bin/sh
$(env | sed 's/^/export /')
exec wal-g "\$@"
EOF
chmod a+x /wal-g-restore-wrapper/wal-g
