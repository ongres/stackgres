#!/bin/sh

set -e

if [ "$ROLLBACK" = true ]
then
  if [ -f "$PG_UPGRADE_PATH/$SOURCE_VERSION/data/PG_VERSION" ]
  then
    rm -rf "$PG_DATA_PATH"
    mv "$PG_UPGRADE_PATH/$SOURCE_VERSION/data" "$PG_DATA_PATH"
  fi
  if [ -f "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64" ]
  then
    cat "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64" \
      | cut -d ' ' -f 3 | tr -d "'" \
      | while read FILE
        do
	  if [ -f "$FILE" ]
          then
            chmod a+rw "$FILE"
            rm -rfv "$FILE"
	  fi
        done
  fi
  if [ -d "$PG_UPGRADE_PATH/$TARGET_VERSION" ]
  then
    rm -rf "$PG_UPGRADE_PATH/$TARGET_VERSION"
  fi
  rm -f "$PG_UPGRADE_PATH/.upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION.done"
  if [ -d "$PG_RELOCATED_BASE_PATH/$TARGET_VERSION" ]
  then
    chmod -R a+rw "$PG_RELOCATED_BASE_PATH/$TARGET_VERSION"
    rm -rf "$PG_RELOCATED_BASE_PATH/$TARGET_VERSION"
  fi
  if [ -d "$PG_EXTENSIONS_BASE_PATH/${TARGET_VERSION%.*}" ]
  then
    chmod -R a+rw "$PG_EXTENSIONS_BASE_PATH/${TARGET_VERSION%.*}"
    rm -rf "$PG_EXTENSIONS_BASE_PATH/${TARGET_VERSION%.*}"
  fi
  exit 0
fi

if [ -f "$PG_UPGRADE_PATH/.upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION.done" ]
then
  echo "Major version upgrade already performed"
  exit 0
fi

if [ "$POSTGRES_VERSION" != "$TARGET_VERSION" ]
then
  echo "Can not perform major version upgrade, postgres version has not been updated"
  exit 1
fi

if [ "$PRIMARY_INSTANCE" != "$POD_NAME" ]
then
  echo "Removing data of non primary instance"
  rm -rf "$PG_DATA_PATH"
  mkdir -p "$PG_UPGRADE_PATH"
  touch "$PG_UPGRADE_PATH/.upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION.done"
  echo "Major version upgrade not needed for non primary instance"
  exit 0
fi

if [ ! -f "$PG_UPGRADE_PATH/$TARGET_VERSION/data/.pg_upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION.done" ]
then
  echo "Creating new database"
  rm -rf "$PG_UPGRADE_PATH/$TARGET_VERSION/data"
  mkdir -p "$PG_UPGRADE_PATH/$TARGET_VERSION/data"
  initdb \
    -D "$PG_UPGRADE_PATH/$TARGET_VERSION/data" \
    -E "$ENCODING" \
    --locale "$LOCALE" \
    $("$DATA_CHECKSUM" && echo "-k" || true)
  mv "$PG_UPGRADE_PATH/$TARGET_VERSION/data/postgresql.conf" \
    "$PG_UPGRADE_PATH/$TARGET_VERSION/data/postgresql.init.conf"
  {
    cat "$PG_UPGRADE_PATH/$TARGET_VERSION/data/postgresql.init.conf"
    echo
    cat "$ETC_POSTGRES_PATH/postgresql.conf"
  } > "$PG_UPGRADE_PATH/$TARGET_VERSION/data/postgresql.conf"
  (
  cd "$PG_UPGRADE_PATH/$TARGET_VERSION"
  if [ ! -f "$PG_UPGRADE_PATH/$TARGET_VERSION/.copy-missing-lib64.done" ]
  then
    cp -aunv "$SOURCE_PG_LIB64_PATH" "${TARGET_PG_LIB64_PATH%/*}" > "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64"
    if [ -s "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64" ]
    then
      echo "Following files where copied from $SOURCE_PG_LIB64_PATH to $TARGET_PG_LIB64_PATH"
      echo
      cat "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64"
      echo
    fi
    touch "$PG_UPGRADE_PATH/$TARGET_VERSION/.copy-missing-lib64.done"
  fi
  chmod 700 "$PG_DATA_PATH"
  if [ "$CHECK" = true ]
  then
    echo "Checking major version upgrade"
    if ! pg_upgrade -c -r \
      -b "/usr/lib/postgresql/$SOURCE_VERSION/bin" \
      -B "/usr/lib/postgresql/$TARGET_VERSION/bin" \
      -d "$PG_DATA_PATH" \
      -D "$PG_UPGRADE_PATH/$TARGET_VERSION/data" \
      -s "$PG_UPGRADE_PATH/$TARGET_VERSION" \
      -o "-c 'dynamic_library_path=$SOURCE_PG_LIB_PATH:$SOURCE_PG_EXTRA_LIB_PATH'" \
      -O "-c 'dynamic_library_path=$TARGET_PG_LIB_PATH:$TARGET_PG_EXTRA_LIB_PATH'" \
      $("$LINK" && printf %s "-k" || true) \
      $("$CLONE" && printf %s "--clone" || true)
    then
      echo "Major version upgrade check failed"
    fi
    
    echo
    grep . *.txt *.log 2>/dev/null | cat >&2
    echo
    
    echo "Major version upgrade check performed"
    
    echo -n "Wait for the major version upgrade rollback to happen"
    while true
    do
      printf .
      sleep 30
    done
    exit 0
  fi
  echo "Performing major version upgrade"
  if ! pg_upgrade -r \
    -b "/usr/lib/postgresql/$SOURCE_VERSION/bin" \
    -B "/usr/lib/postgresql/$TARGET_VERSION/bin" \
    -d "$PG_DATA_PATH" \
    -D "$PG_UPGRADE_PATH/$TARGET_VERSION/data" \
    -s "$PG_UPGRADE_PATH/$TARGET_VERSION" \
    -o "-c 'dynamic_library_path=$SOURCE_PG_LIB_PATH:$SOURCE_PG_EXTRA_LIB_PATH'" \
    -O "-c 'dynamic_library_path=$TARGET_PG_LIB_PATH:$TARGET_PG_EXTRA_LIB_PATH'" \
    $("$LINK" && printf %s "-k" || true) \
    $("$CLONE" && printf %s "--clone" || true)
  then
    echo
    grep . *.txt *.log 2>/dev/null | cat >&2
    echo
    exit 1
  fi
  echo
  grep . *.txt *.log 2>/dev/null | cat >&2
  echo
  touch "$PG_UPGRADE_PATH/$TARGET_VERSION/data/.pg_upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION.done"
  )
fi

if [ ! -d "$PG_UPGRADE_PATH/$SOURCE_VERSION/data" ] \
  && [ -d "$PG_UPGRADE_PATH/$TARGET_VERSION/data" ]
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
cat "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64" \
  | cut -d ' ' -f 3 | tr -d "'" \
  | while read FILE
    do
      if [ -f "$FILE" ]
      then
        chmod a+rw "$FILE"
        rm -rfv "$FILE"
     fi
    done
touch "$PG_UPGRADE_PATH/.upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION.done"
echo "Major version upgrade performed"
