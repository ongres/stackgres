#!/bin/sh

set -e

rm -Rf "$(dirname "$0")/data/crds"
mkdir -p "$(dirname "$0")/data/crds"
find "$(dirname "$0")/../stackgres-k8s/install/helm/stackgres-operator/crds" -name '*.yaml' \
  | tr '\n' '\0' | xargs -0 -r -n 1 -I % cp % "$(dirname "$0")/data/crds"

grep '<artifactId>stackgres-parent</artifactId>' "$(dirname "$0")/../stackgres-k8s/src/pom.xml" -A 2 -B 2 \
 | grep -oP '(?<=<version>).*?(?=</version>)' \
 | xargs echo current_version: > "$(dirname "$0")/data/versions.yml"

cp "$(dirname "$0")/../stackgres-k8s/src/api-web/target/swagger-merged.yaml" "$(dirname "$0")/themes/sg-doc/static/sg-swagger.yaml"
