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
mkdir -p target/public/js/components/forms/help
"$SHELL" $SHELL_XTRACE ../../ci/utils/crds2description_json.sh ../../install/helm/stackgres-operator/crds target/public/js/components/forms/help

mkdir -p target/public/info
# Export SG version to show on the UI
grep '<artifactId>stackgres-parent</artifactId>' "../pom.xml" -A 2 -B 2 \
 | grep -oP '(?<=<version>).*?(?=</version>)' \
 | xargs -I % echo '{"version":"%"}' > target/public/info/sg-info.json
