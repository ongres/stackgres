#!/bin/sh

set -eu

mkdir -p "$PG_EXTENSIONS_PATH"

chmod 700 "$PG_EXTENSIONS_PATH"

mkdir -p "$PG_EXTENSIONS_BIN_PATH"

mkdir -p "$PG_EXTENSIONS_LIB_PATH"

mkdir -p "$PG_EXTENSIONS_EXTENSION_PATH"

mkdir -p "$PG_EXTENSIONS_LIB64_PATH"

# The images of the StackGres registry (Debian) keep the system libraries in the multiarch
# directory (/usr/lib64 only holds the dynamic loader): relocate it too so that the relocated
# bash and coreutils can run from the ephemeral containers that install the extensions.
if [ -n "${PG_SYSTEM_LIB_PATH:-}" ]
then
  mkdir -p "$PG_EXTENSIONS_SYSTEM_LIB_PATH"
fi

mkdir -p "$PG_RELOCATED_PATH"

# Record the architecture of the relocated binaries (the data volume may be
# reused on a node of a different architecture).
RELOCATED_ARCH="$(uname -m)"
if [ -f "$PG_RELOCATED_PATH/.arch" ] \
  && [ "x$(cat "$PG_RELOCATED_PATH/.arch")" != "x$RELOCATED_ARCH" ]
then
  echo "Relocated binaries architecture ($(cat "$PG_RELOCATED_PATH/.arch" 2>/dev/null || echo unknown))" \
    "differs from current architecture ($RELOCATED_ARCH), forcing relocation"
fi

for RELOCATE_PATH in "$PG_BIN_PATH:$PG_RELOCATED_BIN_PATH" \
  "$PG_LIB_PATH:$PG_RELOCATED_LIB_PATH" \
  "$PG_SHARE_PATH:$PG_RELOCATED_SHARE_PATH" \
  "$USR_BIN_PATH:$PG_RELOCATED_USR_BIN_PATH" \
  "$PG_LIB64_PATH:$PG_RELOCATED_LIB64_PATH" \
  $([ -z "${PG_SYSTEM_LIB_PATH:-}" ] || printf '%s' "$PG_SYSTEM_LIB_PATH:$PG_RELOCATED_SYSTEM_LIB_PATH")
do
  echo "Relocating ${RELOCATE_PATH%:*} to ${RELOCATE_PATH#*:} ..."
  mkdir -p "${RELOCATE_PATH#*:}"
  find "${RELOCATE_PATH%:*}" -printf '%p %s\n' | sed "s|^${RELOCATE_PATH%:*}/\(.* [0-9]\+\)$|${RELOCATE_PATH#*:}/\1|" | sort > "$PG_RELOCATED_PATH/.source"
  find "${RELOCATE_PATH#*:}" -printf '%p %s\n' | sort > "$PG_RELOCATED_PATH/.target"
  comm -23 "$PG_RELOCATED_PATH/.source" "$PG_RELOCATED_PATH/.target" \
    | sed "s|^\(.*\) [0-9]\+$|\1|" | sort > "$PG_RELOCATED_PATH/.source-diff"
  comm -13 "$PG_RELOCATED_PATH/.source" "$PG_RELOCATED_PATH/.target" \
    | sed "s|^\(.*\) [0-9]\+$|\1|" | sort > "$PG_RELOCATED_PATH/.target-diff"
  comm -12 "$PG_RELOCATED_PATH/.source-diff" "$PG_RELOCATED_PATH/.target-diff" \
    | xargs rm -f || true
  cp -a -u "${RELOCATE_PATH%:*}/." "${RELOCATE_PATH#*:}"
  chmod -R 700 "${RELOCATE_PATH#*:}"
  echo "done."
done

for EXTENSION_CONTROL_FILE in "$PG_EXTENSION_PATH"/*.control
do
  if ! [ -f "$EXTENSION_CONTROL_FILE" ]
  then
    continue
  fi
  EXTENSION_NAME="${EXTENSION_CONTROL_FILE%.*}"
  EXTENSION_NAME="${EXTENSION_NAME##*/}"
  echo "Relocating $EXTENSION_CONTROL_FILE (and $EXTENSION_NAME--*.sql) to $PG_EXTENSIONS_EXTENSION_PATH/. ..."
  {
    find "${EXTENSION_CONTROL_FILE%/*}" -maxdepth 1 -name "$EXTENSION_NAME.control" -printf '%p %s\n'
    find "${EXTENSION_CONTROL_FILE%/*}" -maxdepth 1 -name "$EXTENSION_NAME--*.sql" -printf '%p %s\n'
  }\
    | sed "s|^${EXTENSION_CONTROL_FILE%/*}/\(.* [0-9]\+\)$|${PG_EXTENSIONS_EXTENSION_PATH}/\1|" | sort > "$PG_RELOCATED_PATH/.source"
  find "$PG_EXTENSIONS_EXTENSION_PATH" -printf '%p %s\n' | sort > "$PG_RELOCATED_PATH/.target"
  comm -23 "$PG_RELOCATED_PATH/.source" "$PG_RELOCATED_PATH/.target" \
    | sed "s|^\(.*\) [0-9]\+$|\1|" | sort > "$PG_RELOCATED_PATH/.source-diff"
  comm -13 "$PG_RELOCATED_PATH/.source" "$PG_RELOCATED_PATH/.target" \
    | sed "s|^\(.*\) [0-9]\+$|\1|" | sort > "$PG_RELOCATED_PATH/.target-diff"
  comm -12 "$PG_RELOCATED_PATH/.source-diff" "$PG_RELOCATED_PATH/.target-diff" \
    | xargs rm -f || true
  cp -a -u "$EXTENSION_CONTROL_FILE" "${EXTENSION_CONTROL_FILE%/*}/$EXTENSION_NAME"--*.sql \
    "$PG_EXTENSIONS_EXTENSION_PATH/."
  echo "done."
done

for EXTRA_MOUNT in $EXTRA_MOUNTS
do
  echo "Creating extra mount folder $PG_EXTENSIONS_EXTENSION_PATH/$EXTRA_MOUNT"
  mkdir -p "$PG_EXTENSIONS_EXTENSION_PATH/$EXTRA_MOUNT"
done

printf '%s' "$RELOCATED_ARCH" > "$PG_RELOCATED_PATH/.arch"

rm -f "$PG_RELOCATED_PATH/.source" "$PG_RELOCATED_PATH/.target" \
  "$PG_RELOCATED_PATH/.source-diff" "$PG_RELOCATED_PATH/.target-diff"
