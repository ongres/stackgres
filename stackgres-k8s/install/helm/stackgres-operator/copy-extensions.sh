#!/bin/sh

set -e

PG_EXTENSIONS_PATH=/var/lib/postgresql/extensions
! [ -s "$PG_EXTENSIONS_PATH/index-${POSTGRES_VERSION%%.*}-build-${BUILD_VERSION}.json" ] || exit 0
[ -n "$POSTGRES_VERSION" ]
[ -n "$BUILD_VERSION" ]
mkdir -p "$PG_EXTENSIONS_PATH"
chmod -R 700 "$PG_EXTENSIONS_PATH"

for CONTROL_FILE in "/usr/share/postgresql/$POSTGRES_VERSION/extension"/*.control
do
  SO_FILE="$(ls "/usr/lib/postgresql/$POSTGRES_VERSION/lib/$(
    grep -F 'module_pathname' "$CONTROL_FILE" \
      | cut -d / -f 2 | tr -d \').so" 2>/dev/null || true)"
  echo "$(basename ${CONTROL_FILE%.*}):$SO_FILE"
done > "$PG_EXTENSIONS_PATH/extensions-simple"

sed -i "s#^timescaledb:[^:]*\(.*\)\$#timescaledb:$(
    ls /usr/lib/postgresql/$POSTGRES_VERSION/lib/ -1 | grep '^timescaledb' | tr '\n' ','
  ):\1#" \
  "$PG_EXTENSIONS_PATH/extensions-simple"

for SO_FILE in "/sbin"/* "/bin"/* "/usr/sbin"/* "/usr/bin"/{python3,du,pspg} /usr/lib64/python3.6/lib-dynload/*.so \
  "/usr/lib/postgresql/$POSTGRES_VERSION/lib"/*.so "/usr/lib/postgresql/$POSTGRES_VERSION/bin"/*
do
  if ! grep -q -F "$SO_FILE" "$PG_EXTENSIONS_PATH/extensions-simple" \
    && ! echo "$SO_FILE" | grep -q '\(pg_repack\|raster2pgsql\|shp2pgsql\|pgsql2shp\)'
  then
    ldd "$SO_FILE" | sed 's/^[^:]\+: \([^:]\+\):/\1 /' \
      | tr -d '\t' | cut -d ' ' -f 1 \
      | while read SO_DEP_FILE
        do
          if [ -f "/usr/lib64/${SO_DEP_FILE##*/}" ]
          then
            echo "/usr/lib64/${SO_DEP_FILE##*/}"
          fi
        done
  fi
done | sort | uniq > "$PG_EXTENSIONS_PATH/postgres"

for CONTROL_FILE in "/usr/share/postgresql/$POSTGRES_VERSION/extension"/*.control
do
  SO_FILE="$(ls "/usr/lib/postgresql/$POSTGRES_VERSION/lib/$(
    grep -F 'module_pathname' "$CONTROL_FILE" \
      | cut -d / -f 2 | tr -d \').so" 2>/dev/null || true)"
  echo "$(basename ${CONTROL_FILE%.*}):$SO_FILE:$(
    if [ -n "$SO_FILE" ]
    then ldd "$SO_FILE" | sed 's/^[^:]\+: \([^:]\+\):/\1 /' \
      | tr -d '\t' | cut -d ' ' -f 1 \
      | while read SO_DEP_FILE
        do
          if [ -f "/usr/lib64/${SO_DEP_FILE##*/}" ]
          then
            echo "/usr/lib64/${SO_DEP_FILE##*/}"
          fi
        done \
      | while read SO_DEP_FILE
        do
          if ! grep -q -F "$SO_DEP_FILE" "$PG_EXTENSIONS_PATH/postgres"
          then
            echo -n "$SO_DEP_FILE,"
          fi
        done
    fi | sed 's/,$//')"
done > "$PG_EXTENSIONS_PATH/extensions"

for BIN_FILE in "/usr/lib/postgresql/$POSTGRES_VERSION/bin"/{pg_repack,raster2pgsql,shp2pgsql,pgsql2shp}
do
  echo "$BIN_FILE:$(
    if [ -n "$BIN_FILE" ]
    then
      ldd "$BIN_FILE" | sed 's/^[^:]\+: \([^:]\+\):/\1 /' \
        | tr -d '\t' | cut -d ' ' -f 1 \
      | while read BIN_DEP_FILE
        do
          if [ -f "/usr/lib64/${BIN_DEP_FILE##*/}" ]
          then
            echo "/usr/lib64/${BIN_DEP_FILE##*/}"
          fi
        done \
        | while read BIN_DEP_FILE
        do
          if ! grep -q "$BIN_DEP_FILE" "$PG_EXTENSIONS_PATH/postgres"
          then
            echo -n "$BIN_DEP_FILE,"
          fi
        done | sed 's/,$//'
    fi)"
done > "$PG_EXTENSIONS_PATH/extensions-binaries"

cat "$PG_EXTENSIONS_PATH/extensions" \
  | cut -d : -f 1 \
  | while read EXTENSION
    do
      EXTENSION_VERSION="$(cat "/usr/share/postgresql/$POSTGRES_VERSION/extension/$EXTENSION.control" \
        | grep '^default_version' | tr -d " '" | cut -d = -f 2)"
      mkdir -p "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib/postgresql/${POSTGRES_VERSION%%.*}/lib"
      mkdir -p "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/share/postgresql/${POSTGRES_VERSION%%.*}/extension"
      mkdir -p "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib64"
      if ! [ -s "$PG_EXTENSIONS_PATH/index-${POSTGRES_VERSION%%.*}-build-${BUILD_VERSION}.json" ]
      then
        cat << EOF > "$PG_EXTENSIONS_PATH/index-${POSTGRES_VERSION%%.*}-build-${BUILD_VERSION}.json"
{
  "extensions": [
EOF
      else
        echo -n "," >> "$PG_EXTENSIONS_PATH/index-${POSTGRES_VERSION%%.*}-build-${BUILD_VERSION}.json"
      fi
      cat << EOF >> "$PG_EXTENSIONS_PATH/index-${POSTGRES_VERSION%%.*}-build-${BUILD_VERSION}.json"
      {
        "name": "$EXTENSION",
        "publisher": "com.ongres",
        "license": "postgres",
        "abstract": "$EXTENSION",
        "description": "$EXTENSION",
        "channels": {
          "stable": "$EXTENSION_VERSION"
        },
        "tags": [ "$EXTENSION", "contrib" ],
        "versions": [{
          "version": "$EXTENSION_VERSION",
          "availableFor": [{
$(    if [ "$(grep "^$EXTENSION:" "$PG_EXTENSIONS_PATH/extensions" \
        | cut -d : -f 2 | tr ',' '\n' | grep -v '^$' | sort | uniq | wc -l)" -gt 0 ]
      then
        cat << BUILD_EOF
            "build": "${BUILD_VERSION%%.*}",
BUILD_EOF
      fi
)
            "postgresVersion": "${POSTGRES_VERSION%%.*}"
          }]
        }],
        "url": "https://www.postgresql.org/docs/${POSTGRES_VERSION%%.*}/contrib.html",
        "source": "https://www.postgresql.org/docs/${POSTGRES_VERSION%%.*}/contrib.html"
      }
EOF
      cp -a "/usr/share/postgresql/$POSTGRES_VERSION/extension/$EXTENSION.control" "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/share/postgresql/${POSTGRES_VERSION%%.*}/extension/." || true
      cp -a "/usr/share/postgresql/$POSTGRES_VERSION/extension/$EXTENSION"--*.sql "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/share/postgresql/${POSTGRES_VERSION%%.*}/extension/." || true
      grep "^$EXTENSION:" "$PG_EXTENSIONS_PATH/extensions" \
        | cut -d : -f 2 | tr ',' '\n' | grep -v '^$' | sort | uniq \
        | while read SO_FILE
          do
            cp -a "$SO_FILE" "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib/postgresql/${POSTGRES_VERSION%%.*}/lib/." || true
          done
      grep "^$EXTENSION:" "$PG_EXTENSIONS_PATH/extensions" \
        | cut -d : -f 3 | tr ',' '\n' | grep -v '^$' | sort | uniq \
        | while read SO_FILE
          do
            cp -a "$SO_FILE" "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib64/." || true
          done
      if [ "$EXTENSION" = "pg_repack" ]
      then
        mkdir -p "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib/postgresql/${POSTGRES_VERSION%%.*}/bin"
        cp -a "/usr/lib/postgresql/$POSTGRES_VERSION/bin/"pg_repack \
          "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib/postgresql/${POSTGRES_VERSION%%.*}/bin/."
        grep "/\(pg_repack\):" "$PG_EXTENSIONS_PATH/extensions-binaries" \
          | cut -d : -f 2 | tr ',' '\n' | grep -v '^$' | sort | uniq \
          | while read SO_FILE
            do
              cp -a "$SO_FILE" "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib64/." || true
            done
      fi
      if [ "$EXTENSION" = "postgis" ]
      then
        mkdir -p "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib/postgresql/${POSTGRES_VERSION%%.*}/bin"
        cp -a "/usr/lib/postgresql/$POSTGRES_VERSION/bin/"{shp2pgsql,pgsql2shp} \
          "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib/postgresql/${POSTGRES_VERSION%%.*}/bin/."
        grep "/\(shp2pgsql\|pgsql2shp\):" "$PG_EXTENSIONS_PATH/extensions-binaries" \
          | cut -d : -f 2 | tr ',' '\n' | grep -v '^$' | sort | uniq \
          | while read SO_FILE
            do
              cp -a "$SO_FILE" "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib64/." || true
            done
      fi
      if [ "$EXTENSION" = "postgis_raster" ]
      then
        mkdir -p "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib/postgresql/${POSTGRES_VERSION%%.*}/bin"
        cp -a "/usr/lib/postgresql/$POSTGRES_VERSION/bin/"raster2pgsql \
          "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib/postgresql/${POSTGRES_VERSION%%.*}/bin/."
        grep "/\(raster2pgsql\):" "$PG_EXTENSIONS_PATH/extensions-binaries" \
          | cut -d : -f 2 | tr ',' '\n' | grep -v '^$' | sort | uniq \
          | while read SO_FILE
            do
              cp -a "$SO_FILE" "$PG_EXTENSIONS_PATH/${POSTGRES_VERSION%%.*}/${BUILD_VERSION%%.*}/$EXTENSION/usr/lib64/." || true
            done
      fi
    done
echo "  ]" >> "$PG_EXTENSIONS_PATH/index-${POSTGRES_VERSION%%.*}-build-${BUILD_VERSION}.json"
echo "}" >> "$PG_EXTENSIONS_PATH/index-${POSTGRES_VERSION%%.*}-build-${BUILD_VERSION}.json"
