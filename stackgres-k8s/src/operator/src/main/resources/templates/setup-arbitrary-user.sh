#!/bin/sh

USER=postgres
UID="$(id -u)"
GID="$(id -g)"
SHELL=/bin/sh
cp "$TEMPLATES_PATH/passwd" /local/etc/.
cp "$TEMPLATES_PATH/group" /local/etc/.
cp "$TEMPLATES_PATH/shadow" /local/etc/.
cp "$TEMPLATES_PATH/gshadow" /local/etc/.
echo "$USER:x:$UID:$GID::$PG_BASE_PATH:$SHELL" >> /local/etc/passwd
chmod 644 /local/etc/passwd
echo "$USER:x:$GID:" >> /local/etc/group
chmod 644 /local/etc/group
echo "$USER"':!!:18179:0:99999:7:::' >> /local/etc/shadow
chmod 000 /local/etc/shadow
echo "$USER"':!::' >> /local/etc/gshadow
chmod 000 /local/etc/gshadow
