#!/bin/sh

set -e

cp "$TEMPLATES_PATH/start-patroni.sh" "$LOCAL_BIN_PATH"
cp "$TEMPLATES_PATH/post-init.sh" "$LOCAL_BIN_PATH"
cp "$TEMPLATES_PATH/exec-with-env" "$LOCAL_BIN_PATH"
cp "$TEMPLATES_PATH/patronictl" "$LOCAL_BIN_PATH"
sed -i "s#\${POSTGRES_PORT}#${POSTGRES_PORT}#g" \
  "$LOCAL_BIN_PATH/post-init.sh"
sed -i "s#\${BASE_ENV_PATH}#${BASE_ENV_PATH}#g" \
  "$LOCAL_BIN_PATH/exec-with-env"
sed -i "s#\${BASE_SECRET_PATH}#${BASE_SECRET_PATH}#g" \
  "$LOCAL_BIN_PATH/exec-with-env"
chmod a+x "$LOCAL_BIN_PATH/start-patroni.sh"
chmod a+x "$LOCAL_BIN_PATH/post-init.sh"
chmod a+x "$LOCAL_BIN_PATH/exec-with-env"
chmod a+x "$LOCAL_BIN_PATH/patronictl"
