#!/bin/sh

set -e

cd "$(dirname "$0")"

for POM_FILE in $(find ../../src -name pom.xml -type f | grep -v /target/)
do
  POM_FOLDER="${POM_FILE%/pom.xml}"
  POM_FOLDER="${POM_FOLDER#\.\./\.\./src}"
  POM_FOLDER="${POM_FOLDER#/}"
  echo "Updating stackgres-k8s/src/$POM_FOLDER/pom.xml.build"
  sh redact-version.sh "$POM_FOLDER" \
    | sed 's#<module>\([^<]\+\)</module>#<module>\1/pom.xml.build</module>#g' \
    > "$POM_FILE.build"
done
