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

mkdir -p target/public/info
cp ../restapi/target/swagger-merged.json target/public/info/sg-tooltips.json

mkdir -p target/public/info
# Export SG version to show on the UI
sh ../../ci/build/version.sh \
  | echo "{\"version\":\"$(cat)\"}" > target/public/info/sg-info.json

mkdir -p public/info
cp -a target/public/info/. public/info/.

cp -a dist/. target/public/.
