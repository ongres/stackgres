#!/bin/sh

set -e

rm -f "$PG_UPGRADE_PATH/.major-version-upgrade-fail" \
  "$PG_UPGRADE_PATH/.major-version-upgrade-continue"

if [ "$ROLLBACK" = true ]
then
  if [ -f "$PG_UPGRADE_PATH/$SOURCE_VERSION/data/PG_VERSION" ]
  then
    rm -rf "$PG_DATA_PATH"
    mv "$PG_UPGRADE_PATH/$SOURCE_VERSION/data" "$PG_DATA_PATH"
  fi
  if [ -n "${POSTGRES_WAL_PATH:-}" ]
  then
    rm -rf "$POSTGRES_WAL_PATH.upgrade-$TARGET_VERSION"
    if [ -d "$POSTGRES_WAL_PATH.old-$SOURCE_VERSION" ]
    then
      rm -rf "$POSTGRES_WAL_PATH"
      mv "$POSTGRES_WAL_PATH.old-$SOURCE_VERSION" "$POSTGRES_WAL_PATH"
    fi
    if [ -L "$PG_DATA_PATH/pg_wal" ]
    then
      ln -sfn "$POSTGRES_WAL_PATH" "$PG_DATA_PATH/pg_wal"
    fi
  fi
  if [ -f "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64" ]
  then
    cat "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64" \
      | cut -d ' ' -f 3 | tr -d "'" \
      | while read FILE
        do
	  if [ -f "$FILE" ]
          then
            chmod a+rw "$FILE" || true
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
    chmod -R a+rw "$PG_RELOCATED_BASE_PATH/$TARGET_VERSION" || true
    rm -rf "$PG_RELOCATED_BASE_PATH/$TARGET_VERSION"
  fi
  if [ -d "$PG_EXTENSIONS_BASE_PATH/${TARGET_VERSION%.*}" ]
  then
    chmod -R a+rw "$PG_EXTENSIONS_BASE_PATH/${TARGET_VERSION%.*}" || true
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
  if [ -n "${POSTGRES_WAL_PATH:-}" ] && [ -d "$POSTGRES_WAL_PATH" ]
  then
    echo "Removing existing content of $POSTGRES_WAL_PATH of non primary instance"
    (cd "$POSTGRES_WAL_PATH" && find . -mindepth 1 -maxdepth 1 -exec rm -rf {} +)
  fi
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
  if [ -n "${POSTGRES_WAL_PATH:-}" ]
  then
    rm -rf "$POSTGRES_WAL_PATH.upgrade-$TARGET_VERSION"
  fi
  initdb \
    -D "$PG_UPGRADE_PATH/$TARGET_VERSION/data" \
    $([ -z "${POSTGRES_WAL_PATH:-}" ] || printf %s "--waldir=$POSTGRES_WAL_PATH.upgrade-$TARGET_VERSION") \
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
  chmod -R 700 "$PG_DATA_PATH"
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
    grep . *.{txt,log} */*.{txt,log} */*/*.{txt,log} */*/*/*.{txt,log} */*/*/*/*.{txt,log} 2>/dev/null | cat >&2
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
    grep . *.{txt,log} */*.{txt,log} */*/*.{txt,log} */*/*/*.{txt,log} */*/*/*/*.{txt,log} 2>/dev/null | cat >&2
    echo
    if [ "$MANUAL_ROLLBACK" != true ]
    then
      echo "Major version upgrade failed"
      exit 1
    fi
    # When manual rollback is enabled do not exit immediately. Sleep and wait for the major version
    # upgrade Job to decide what to do by creating one of the sentinel files below. This keeps the
    # init container (and therefore the Pod) alive so the major version upgrade can be inspected
    # and, if desired, performed manually.
    echo "Major version upgrade failed, waiting for the major version upgrade rollback decision"
    while true
    do
      if [ -f "$PG_UPGRADE_PATH/.major-version-upgrade-fail" ]
      then
        echo "Major version upgrade failure confirmed"
        exit 1
      fi
      if [ -f "$PG_UPGRADE_PATH/.major-version-upgrade-continue" ]
      then
        echo "Major version upgrade continue requested, assuming the upgrade was performed manually"
        break
      fi
      sleep 1
    done
  fi
  echo
  grep . *.{txt,log} */*.{txt,log} */*/*.{txt,log} */*/*/*.{txt,log} */*/*/*/*.{txt,log} 2>/dev/null | cat >&2
  echo
  echo 'Copying pg_hba.conf from the original data folder to the upgraded data folder:'
  cp -v "$PG_DATA_PATH/pg_hba.conf" "$PG_UPGRADE_PATH/$TARGET_VERSION/data/pg_hba.conf"
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
if [ -n "${POSTGRES_WAL_PATH:-}" ] && [ -L "$PG_DATA_PATH/pg_wal" ] \
  && [ "$(readlink "$PG_DATA_PATH/pg_wal")" = "$POSTGRES_WAL_PATH.upgrade-$TARGET_VERSION" ]
then
  echo "Swapping WAL directory $POSTGRES_WAL_PATH.upgrade-$TARGET_VERSION with $POSTGRES_WAL_PATH"
  if [ -d "$POSTGRES_WAL_PATH" ]
  then
    rm -rf "$POSTGRES_WAL_PATH.old-$SOURCE_VERSION"
    mv "$POSTGRES_WAL_PATH" "$POSTGRES_WAL_PATH.old-$SOURCE_VERSION"
  fi
  if [ -d "$POSTGRES_WAL_PATH.upgrade-$TARGET_VERSION" ]
  then
    mv "$POSTGRES_WAL_PATH.upgrade-$TARGET_VERSION" "$POSTGRES_WAL_PATH"
  fi
  ln -sfn "$POSTGRES_WAL_PATH" "$PG_DATA_PATH/pg_wal"
  if [ -L "$PG_UPGRADE_PATH/$SOURCE_VERSION/data/pg_wal" ]
  then
    ln -sfn "$POSTGRES_WAL_PATH.old-$SOURCE_VERSION" "$PG_UPGRADE_PATH/$SOURCE_VERSION/data/pg_wal"
  fi
fi
cat "$PG_UPGRADE_PATH/$TARGET_VERSION/copied-missing-lib64" \
  | cut -d ' ' -f 3 | tr -d "'" \
  | while read FILE
    do
      if [ -f "$FILE" ]
      then
        chmod a+rw "$FILE" || true
        rm -rfv "$FILE"
     fi
    done
touch "$PG_UPGRADE_PATH/.upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION.done"
echo "Major version upgrade performed"
