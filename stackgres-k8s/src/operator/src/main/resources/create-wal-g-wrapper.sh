cat << EOF > /wal-g-wrapper/wal-g
#/bin/sh
$(env | grep -v "^_" | sed 's/^/export /')
exec wal-g "\$@"
EOF
chmod a+x /wal-g-wrapper/wal-g
