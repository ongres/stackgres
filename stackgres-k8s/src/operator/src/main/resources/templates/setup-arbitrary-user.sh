#!/bin/sh

set -e


USER_NAME=postgres
USER_ID="$(id -u)"
GROUP_ID="$(id -g)"
USER_SHELL=/bin/sh

echo "Setting arbitrary user $USER_NAME uid:$USER_ID gid:$GROUP_ID home:$PG_BASE_PATH shell:$USER_SHELL"

cp "$TEMPLATES_PATH/passwd" /local/etc/.
cp "$TEMPLATES_PATH/group" /local/etc/.
cp "$TEMPLATES_PATH/shadow" /local/etc/.
cp "$TEMPLATES_PATH/gshadow" /local/etc/.
chmod 644 /local/etc/passwd
echo "$USER_NAME:x:$USER_ID:$GROUP_ID::$PG_BASE_PATH:$USER_SHELL" >> /local/etc/passwd
chmod 644 /local/etc/group
echo "$USER_NAME:x:$GROUP_ID:" >> /local/etc/group
chmod 600 /local/etc/shadow
echo "$USER_NAME"':!!:18179:0:99999:7:::' >> /local/etc/shadow
chmod 600 /local/etc/gshadow
echo "$USER_NAME"':!::' >> /local/etc/gshadow
