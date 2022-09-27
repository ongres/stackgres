#!/bin/sh

if [ -z "$1" ]
then
  echo "You must specifiy the kubernetes version (e.g. 1.25)"
  exit 1
fi

DEBUG="${DEBUG:-false}"
K8S_VERSION="$1"

cd "$(basedir "$0")"
mkdir -p target

SWAGGER_FILE="target/swagger-$K8S_VERSION.json"
MERGED_SWAGGER_FILE="target/meged-swagger-$K8S_VERSION.json"
if [ ! -f "$SWAGGER_FILE" ]
then
  wget "https://github.com/kubernetes/kubernetes/blob/release-$K8S_VERSION/api/openapi-spec/swagger.json?raw=true" -O "$SWAGGER_FILE"
fi
jq --argjson debug "$DEBUG" "$(cat << 'EOF'
  . as $in | [paths | select(.[0] == "definitions" and (. | length) == 2)] as $definitions
    | reduce $definitions[] as $definition ($in;
      . as $accumulator | (if $debug then [ "Expanded Definition", $definition ] | debug else . end)
        | $accumulator | [paths(. == "#/definitions/" + $definition[-1]) | select(.[0] == "definitions")] as $refs
        | reduce $refs[] as $ref ($accumulator;
          . as $ref_accumulator | (if $debug then [ "Expanded $ref", $ref[0:-1] ] | debug else . end) | $ref_accumulator
            | setpath($ref[0:-1]; ($ref_accumulator|getpath($definition)))
          )
      )
EOF
  )" "$SWAGGER_FILE" > "$MERGED_SWAGGER_FILE"


