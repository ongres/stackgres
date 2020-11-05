if [ -f "$PG_RELOCATED_PATH/.mock-completed" ]
then
  exit 0
fi

if [ ! -f "$PG_RELOCATED_PATH/.mock-metadata-completed" ]
then
  for CONTROL_FILE in "$PG_RELOCATED_EXTENSION_PATH"/*.control
  do
    SO_FILE="$(ls "$PG_RELOCATED_LIB_PATH/$(
      grep -F 'module_pathname' "$CONTROL_FILE" \
        | cut -d / -f 2 | tr -d \').so" 2>/dev/null || true)"
    echo "$(basename ${CONTROL_FILE%.*}):$SO_FILE"
  done > "$PG_RELOCATED_PATH/extensions-simple"

  sed -i "s#^timescaledb:[^:]*\(.*\)\$#timescaledb:$(
      ls "$PG_RELOCATED_LIB_PATH" -1 | grep '^timescaledb' | tr '\n' ','
    ):\1#" \
    "$PG_RELOCATED_PATH/extensions-simple"

  for SO_FILE in "/sbin"/* "/bin"/* "/usr/sbin"/* "/usr/bin"/{python3,du,pspg} "$PG_RELOCATED_LIB64_PATH"/python3.6/lib-dynload/*.so \
    "$PG_RELOCATED_LIB_PATH"/*.so "$PG_RELOCATED_LIB_PATH"/*
  do
    if ! grep -q -F "$SO_FILE" "$PG_RELOCATED_PATH/extensions-simple" && ! echo "$SO_FILE" \
     | grep -q '\(pg_repack\|raster2pgsql\|shp2pgsql\|pgsql2shp\)'
    then
      ldd "$SO_FILE" | sed 's/^[^:]\+: \([^:]\+\):/\1 /' \
        | tr -d '\t' | cut -d ' ' -f 1 \
        | while read SO_DEP_FILE
          do
            if [ -f "$PG_RELOCATED_LIB64_PATH/${SO_DEP_FILE##*/}" ]
            then
              echo "$PG_RELOCATED_LIB64_PATH/${SO_DEP_FILE##*/}"
            fi
          done
    fi
  done | sort | uniq > "$PG_RELOCATED_PATH/postgres"

  for CONTROL_FILE in "$PG_RELOCATED_EXTENSION_PATH"/*.control
  do
    SO_FILE="$(ls "$PG_RELOCATED_LIB_PATH/$(
      grep -F 'module_pathname' "$CONTROL_FILE" \
        | cut -d / -f 2 | tr -d \').so" 2>/dev/null || true)"
    echo "$(basename ${CONTROL_FILE%.*}):$SO_FILE:$(
      if [ -n "$SO_FILE" ]
      then ldd "$SO_FILE" | sed 's/^[^:]\+: \([^:]\+\):/\1 /' \
        | tr -d '\t' | cut -d ' ' -f 1 \
        | while read SO_DEP_FILE
          do
            if [ -f "$PG_RELOCATED_LIB64_PATH/${SO_DEP_FILE##*/}" ]
            then
              echo "$PG_RELOCATED_LIB64_PATH/${SO_DEP_FILE##*/}"
            fi
          done \
        | while read SO_DEP_FILE
          do
            if ! grep -q -F "$SO_DEP_FILE" "$PG_RELOCATED_PATH/postgres"
            then
              echo -n "$SO_DEP_FILE,"
            fi
          done
      fi | sed 's/,$//')"
  done > "$PG_RELOCATED_PATH/extensions"

  for BIN_FILE in "$PG_RELOCATED_BIN_PATH"/{pg_repack,raster2pgsql,shp2pgsql,pgsql2shp}
  do
    echo "$BIN_FILE:$(
      if [ -n "$BIN_FILE" ]
      then
        ldd "$BIN_FILE" | sed 's/^[^:]\+: \([^:]\+\):/\1 /' \
          | tr -d '\t' | cut -d ' ' -f 1 \
        | while read BIN_DEP_FILE
          do
            if [ -f "$PG_RELOCATED_LIB64_PATH/${BIN_DEP_FILE##*/}" ]
            then
              echo "$PG_RELOCATED_LIB64_PATH/${BIN_DEP_FILE##*/}"
            fi
          done \
          | while read BIN_DEP_FILE
          do
            if ! grep -q "$BIN_DEP_FILE" "$PG_RELOCATED_PATH/postgres"
            then
              echo -n "$BIN_DEP_FILE,"
            fi
          done | sed 's/,$//'
      fi)"
  done > "$PG_RELOCATED_PATH/extensions-binaries"

  touch "$PG_RELOCATED_PATH/.mock-metadata-completed"
fi

cat "$PG_RELOCATED_PATH/"extensions \
  | cut -d : -f 2 | tr ',' '\n' | grep -v '^$' | sort | uniq \
  | while read SO_FILE
    do
      rm -f "$PG_RELOCATED_LIB_PATH/${SO_FILE##*/}"
    done

cat "$PG_RELOCATED_PATH/"extensions-binaries \
  | cut -d : -f 1 \
  | while read BIN_FILE
    do
      rm -f "$PG_RELOCATED_BIN_PATH/${BIN_FILE##*/}"
    done

(cat "$PG_RELOCATED_PATH/"extensions | cut -d : -f 3
cat "$PG_RELOCATED_PATH/"extensions-binaries | cut -d : -f 2) \
  | tr ',' '\n' | grep -v '^$' | sort | uniq \
  | while read SO_FILE
    do
      rm -f "$PG_RELOCATED_LIB64_PATH/${SO_FILE##*/}"
    done

touch "$PG_RELOCATED_PATH/.mock-completed"
