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

mkdir -p target/public
cp -a react/dist/. target/public/.

# The generated info files must win over the dev placeholders Vite copied
# from react/public.
mkdir -p target/public/info
cp ../restapi/target/swagger-merged.json target/public/info/sg-tooltips.json

# Export SG version to show on the UI
sh ../../ci/build/version.sh \
  | echo "{\"version\":\"$(cat)\"}" > target/public/info/sg-info.json

# Make the tooltips available to the Vite dev server (sg-info.json keeps its
# committed development placeholder there).
mkdir -p react/public/info
cp target/public/info/sg-tooltips.json react/public/info/sg-tooltips.json
