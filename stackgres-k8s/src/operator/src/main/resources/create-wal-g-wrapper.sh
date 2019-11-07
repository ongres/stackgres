cat << EOF > /usr/bin/wal-g-wrapper
#/bin/sh
$(set)
exec wal-g wal-push "\$1"
EOF
chmod a+x /usr/bin/wal-g-wrapper
