#!/bin/sh

set -e

SHELL="$(readlink /proc/$$/exe)"
if [ "$(basename "$SHELL")" = "busybox" ]; then
  SHELL=sh
fi
SHELL_XTRACE="$(! echo $- | grep -q x || echo "-x")"

CURRENT_DIR="$(dirname "$(realpath "$0")")"
cd "$CURRENT_DIR"

mkdir -p $CURRENT_DIR/target
rm -rf $CURRENT_DIR/target/public
cp -av $CURRENT_DIR/dist $CURRENT_DIR/target/public

if [ ! -f "${CURRENT_DIR}/../api-web/target/swagger-merged.json" ]
then
  echo "Please build Stackgres operator and swagger first:"
  echo
  echo "cd stackgres-k8s/src"
  echo "./mvnw clean package -DskipTests"
  echo "sh api-web/src/main/swagger/build.sh"
  echo
  exit 1
fi

mkdir -p $CURRENT_DIR/target/public/info
cp -v $CURRENT_DIR/../api-web/target/swagger-merged.json $CURRENT_DIR/target/public/info/sg-tooltips.json

# Export SG version to show on the UI
mkdir -p $CURRENT_DIR/target/public/info

grep '<artifactId>stackgres-parent</artifactId>' "../pom.xml" -C 2 --color=never \
 | grep --color=never -oP '(?<=<version>).*?(?=</version>)' \
 | xargs -I % echo '{"version":"%"}' > target/public/info/sg-info.json
