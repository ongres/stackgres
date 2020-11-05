#!/bin/sh

mkdir -p "$PG_EXTENSIONS_PATH"

chmod 700 "$PG_EXTENSIONS_PATH"

mkdir -p "$PG_EXTENSIONS_BIN_PATH"

mkdir -p "$PG_EXTENSIONS_LIB_PATH"

mkdir -p "$PG_EXTENSIONS_EXTENSION_PATH"

mkdir -p "$PG_EXTENSIONS_LIB64_PATH"

mkdir -p "$PG_RELOCATED_PATH"

for RELOCATE_PATH in "$PG_BIN_PATH:$PG_RELOCATED_BIN_PATH" \
  "$PG_LIB_PATH:$PG_RELOCATED_LIB_PATH" \
  "$PG_SHARE_PATH:$PG_RELOCATED_SHARE_PATH" \
  "$PG_LIB64_PATH:$PG_RELOCATED_LIB64_PATH"
do
  if [ ! -f "${RELOCATE_PATH#*:}/.done" ]
  then
    echo "Relocating ${RELOCATE_PATH%:*} to ${RELOCATE_PATH#*:}..."
    mkdir -p "${RELOCATE_PATH#*:}"
    rm -fr "${RELOCATE_PATH#*:}"
    cp -a "${RELOCATE_PATH%:*}" "${RELOCATE_PATH#*:}"
    chmod 700 "${RELOCATE_PATH#*:}" -R
    touch "${RELOCATE_PATH#*:}/.done"
    echo "done."
  else
    echo "${RELOCATE_PATH%:*} already relocated to ${RELOCATE_PATH#*:}, skipping"
  fi
done
