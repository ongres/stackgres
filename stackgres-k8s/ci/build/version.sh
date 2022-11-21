#!/bin/sh

cd "$(dirname "$0")"

POM_FILE=../../src/pom.xml
if [ -n "$1" ]
then
  POM_FILE="../../src/$1/pom.xml"
fi

sed -n -f extract-version.sed "$POM_FILE"
