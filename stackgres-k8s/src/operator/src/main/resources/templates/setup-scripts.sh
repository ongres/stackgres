#!/bin/sh

set -e

cp -f "$TEMPLATES_PATH/start-patroni.sh" "$LOCAL_BIN_PATH"
cp -f "$TEMPLATES_PATH/post-init.sh" "$LOCAL_BIN_PATH"
cp -f "$TEMPLATES_PATH/exec-with-env" "$LOCAL_BIN_PATH"
cp -f "$TEMPLATES_PATH/patronictl" "$LOCAL_BIN_PATH"
# A wrapper rather than a copy: the patroni of the images of the StackGres registry is a
# launcher that finds its runfiles next to itself.
printf '#!/bin/sh\nexec "%s" "$@"\n' "$PATRONI_BIN_PATH" > "$LOCAL_BIN_PATH/patroni"
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
chmod a+x "$LOCAL_BIN_PATH/patroni"
