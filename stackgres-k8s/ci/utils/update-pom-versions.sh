#!/bin/sh

cd "${0%/*}"/../../..

stackgres-k8s/src/mvnw \
  versions:update-properties \
  -f stackgres-k8s/src/pom.xml \
  -N \
  -Dmaven.version.ignore='(?i)(.*[-._](alpha|beta|rc|cr|dev|special|nf-execution|m|mr|preview)([-._]?\d+)?|([-._]|\d)+T([-._]|\d)+-([a-f]|\d)+-.*)' \
  -DgenerateBackupPoms=false
