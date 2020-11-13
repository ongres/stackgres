#!/bin/sh

set -e

SHELL="$(readlink /proc/$$/exe)"
if [ "$(basename "$SHELL")" = busybox ]
then
  SHELL=sh
fi
SHELL_XTRACE="$(! echo $- | grep -q x || echo "-x")"

cd "$(dirname "$0")"
mkdir -p target
rm -rf target/public
cp -a public target/public

mkdir -p target/public/info
"$SHELL" $SHELL_XTRACE ../api-web/src/main/swagger/build.sh
cp ../api-web/target/swagger-merged.json target/public/info/sg-tooltips.json

