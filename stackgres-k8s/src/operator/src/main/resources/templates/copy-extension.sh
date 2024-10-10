#!/bin/sh

"$LD_LINUX_PATH" "$PG_RELOCATED_USR_BIN_PATH/coreutils" \
  --coreutils-prog-shebang=mkdir mkdir \
  -p "$PG_EXTENSIONS_PATH/usr"
"$LD_LINUX_PATH" "$PG_RELOCATED_USR_BIN_PATH/coreutils" \
  --coreutils-prog-shebang=cp cp \
  -a -u -n -x -v /usr/. "$PG_EXTENSIONS_PATH/usr/."
# The extensions of the StackGres registry are installed under /postgres/<version>
if [ -d /postgres ]
then
  "$LD_LINUX_PATH" "$PG_RELOCATED_USR_BIN_PATH/coreutils" \
    --coreutils-prog-shebang=mkdir mkdir \
    -p "$PG_EXTENSIONS_PATH/postgres"
  "$LD_LINUX_PATH" "$PG_RELOCATED_USR_BIN_PATH/coreutils" \
    --coreutils-prog-shebang=cp cp \
    -a -u -n -x -v /postgres/. "$PG_EXTENSIONS_PATH/postgres/."
fi
