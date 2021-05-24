#!/bin/sh

if [ -f "$PG_UPGRADE_PATH/.upgraded-from-$SOURCE_VERSION-to-$TARGET_VERSION" ]
then
  echo "Major version upgrade already performed"
  exit 0
fi

if [ "$PRIMARY_INSTANCE" != "$POD_NAME" ]
then
  echo "Removing data of non primary instance"
  rm -rf "$PG_DATA_PATH"
  mkdir -p "$PG_UPGRADE_PATH"
  touch "$PG_UPGRADE_PATH/.upgraded-from-$SOURCE_VERSION-to-$TARGET_VERSION"
  echo "Major version upgrade not needed for non primary instance"
  exit 0
fi

if [ ! -f "$PG_DATA_PATH/.upgraded-from-$SOURCE_VERSION-to-$TARGET_VERSION" ]
then
  echo "Creating new database"
  rm -rf "$PG_UPGRADE_PATH/$TARGET_VERSION/data"
  mkdir -p "$PG_UPGRADE_PATH/$TARGET_VERSION/data"
  initdb \
    -D "$PG_UPGRADE_PATH/$TARGET_VERSION/data" \
    -E "$ENCODING" \
    --locale "$LOCALE" \
    $("$DATA_CHECKSUM" && echo "-k" || true)
  (
  cd "$PG_UPGRADE_PATH/$TARGET_VERSION"
  if [ "$CHECK" ]
  then
    echo "Checking major version upgrade"
    if ! pg_upgrade -c \
      -b "/usr/lib/postgresql/$SOURCE_VERSION/bin" \
      -B "/usr/lib/postgresql/$TARGET_VERSION/bin" \
      -d "$PG_DATA_PATH" \
      -D "$PG_UPGRADE_PATH/$TARGET_VERSION/data" \
      -o "dynamic_library_path=\$libdir:$SOURCE_PG_EXTENSIONS_MOUNTED_LIB64_PATH" \
      -O "dynamic_library_path=\$libdir:$TARGET_PG_EXTENSIONS_MOUNTED_LIB64_PATH" \
      $("$LINK" && echo "-k" || true) \
      $("$CLONE" && echo "--clone" || true)
    then
      grep '^.*' *.txt *.log 2>/dev/null | cat >&2
      exit 1
    fi
  fi
  echo "Performing major version upgrade"
  if ! pg_upgrade \
    -b "/usr/lib/postgresql/$SOURCE_VERSION/bin" \
    -B "/usr/lib/postgresql/$TARGET_VERSION/bin" \
    -d "$PG_DATA_PATH" \
    -D "$PG_UPGRADE_PATH/$TARGET_VERSION/data" \
    -o "dynamic_library_path=\$libdir:$SOURCE_PG_EXTENSIONS_MOUNTED_LIB64_PATH" \
    -O "dynamic_library_path=\$libdir:$TARGET_PG_EXTENSIONS_MOUNTED_LIB64_PATH" \
    $("$LINK" && echo "-k" || true) \
    $("$CLONE" && echo "--clone" || true)
  then
    grep '^.*' *.txt *.log 2>/dev/null | cat >&2
    exit 1
  fi
  )
fi

if [ ! -d "$PG_UPGRADE_PATH/$SOURCE_VERSION/data" ]
then
  mkdir -p "$PG_UPGRADE_PATH/$SOURCE_VERSION"
  mv "$PG_DATA_PATH" "$PG_UPGRADE_PATH/$SOURCE_VERSION/data"
fi
if [ ! -d "$PG_DATA_PATH" ]
then
  if [ ! -d "$PG_UPGRADE_PATH/$TARGET_VERSION/data" ]
  then
    echo "Upgraded data not found!"
    exit 1
  fi
  mv "$PG_UPGRADE_PATH/$TARGET_VERSION/data" "$PG_DATA_PATH"
fi
rm -rf "$PG_UPGRADE_PATH/$SOURCE_VERSION/data"
touch "$PG_UPGRADE_PATH/.upgraded-from-$SOURCE_VERSION-to-$TARGET_VERSION"
echo "Major version upgrade performed"
