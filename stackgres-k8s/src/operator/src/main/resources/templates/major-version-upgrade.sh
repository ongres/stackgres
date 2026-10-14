#!/bin/sh

set -e

# pg_upgrade writes the output of the old and of the new server, and the reason why it failed, under
# the data folder of the new cluster, in a folder named after the time it was started. The whole
# folder is therefore looked up recursively: a glob per depth would also have to be written with a
# brace expansion to match both extensions, and the POSIX shell that runs this script does not
# implement it, silently reporting nothing at all.
print_pg_upgrade_logs() {
  find . -type f \( -name '*.txt' -o -name '*.log' \) -exec grep -H . {} + 2>/dev/null | cat >&2
}

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
  chmod -R 700 "$PG_DATA_PATH"
  # When the data folder holds a postmaster.pid, left behind by a postgres that was killed instead
  # of being shut down, pg_upgrade finds out whether it is stale by starting the old server once.
  # That probe runs before pg_upgrade applies its own --socketdir, so the server only gets the
  # socket directory that is compiled into postgres, /var/run/postgresql, which is a volume mounted
  # in the containers that run a server and does not exist in this one. The probe then fails with
  # "could not create lock file", pg_upgrade concludes that a postmaster is servicing the old
  # cluster and aborts, leaving a data folder that only a rollback can recover. The socket
  # directory is passed in the options of the old server too, which the probe does read, so that
  # it starts in the same directory --socketdir gives to every other start.
  PG_UPGRADE_SOCKET_OPTION="-c 'unix_socket_directories=$PG_UPGRADE_PATH/$TARGET_VERSION'"
  if [ "$CHECK" = true ]
  then
    echo "Checking major version upgrade"
    if ! pg_upgrade -c -r \
      -b "/usr/lib/postgresql/$SOURCE_VERSION/bin" \
      -B "/usr/lib/postgresql/$TARGET_VERSION/bin" \
      -d "$PG_DATA_PATH" \
      -D "$PG_UPGRADE_PATH/$TARGET_VERSION/data" \
      -s "$PG_UPGRADE_PATH/$TARGET_VERSION" \
      -o "-c 'dynamic_library_path=$SOURCE_PG_LIB_PATH:$SOURCE_PG_EXTRA_LIB_PATH' $PG_UPGRADE_SOCKET_OPTION" \
      -O "-c 'dynamic_library_path=$TARGET_PG_LIB_PATH:$TARGET_PG_EXTRA_LIB_PATH'" \
      $("$LINK" && printf %s "-k" || true) \
      $("$CLONE" && printf %s "--clone" || true)
    then
      echo "Major version upgrade check failed"
    fi
    
    echo
    print_pg_upgrade_logs
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
    -o "-c 'dynamic_library_path=$SOURCE_PG_LIB_PATH:$SOURCE_PG_EXTRA_LIB_PATH' $PG_UPGRADE_SOCKET_OPTION" \
    -O "-c 'dynamic_library_path=$TARGET_PG_LIB_PATH:$TARGET_PG_EXTRA_LIB_PATH'" \
    $("$LINK" && printf %s "-k" || true) \
    $("$CLONE" && printf %s "--clone" || true)
  then
    echo
    print_pg_upgrade_logs
    echo
    exit 1
  fi
  echo
  print_pg_upgrade_logs
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
