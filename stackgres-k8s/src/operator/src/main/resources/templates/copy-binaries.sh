#!/bin/sh

mkdir -p "$PG_UPGRADE_PATH/$SOURCE_VERSION"

if [ "$PRIMARY_INSTANCE" != "$POD_NAME" ]
then
  echo "Copying binaries not needed for non primary instance"
  exit 0
fi

if [ -f "$PG_UPGRADE_PATH/$SOURCE_VERSION/.done" ]
then
  echo "Binaries already copied, skipping"
  exit 0
fi

echo "Copying binaries..."
rm -fr "$PG_UPGRADE_PATH/bin"
cp -a "/usr/lib/postgresql/$SOURCE_VERSION/bin" \
  "$PG_UPGRADE_PATH/$SOURCE_VERSION/bin"
rm -fr "$PG_UPGRADE_PATH/lib"
cp -a "/usr/lib/postgresql/$SOURCE_VERSION/lib" \
  "$PG_UPGRADE_PATH/$SOURCE_VERSION/lib"
rm -fr "$PG_UPGRADE_PATH/share"
cp -a "/usr/share/postgresql/$SOURCE_VERSION/" \
  "$PG_UPGRADE_PATH/$SOURCE_VERSION/share"
touch "$PG_UPGRADE_PATH/$SOURCE_VERSION/.done"
echo "done."