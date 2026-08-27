#!/bin/sh

cd "$(dirname "$0")"

POM_FILE=../../src/pom.xml
if [ -n "$1" ]
then
  POM_FILE="../../src/$1/pom.xml"
  # repo-root Matriarch modules (proto, matriarch) live outside stackgres-k8s/src
  [ -f "$POM_FILE" ] || POM_FILE="../../../$1/pom.xml"
fi

sed -f redact-version.sed "$POM_FILE"
