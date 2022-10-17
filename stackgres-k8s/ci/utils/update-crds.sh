#!/bin/sh

if [ -z "$1" ]
then
  echo "You must specifiy the kubernetes version (e.g. 1.25)"
  exit 1
fi

K8S_VERSION="$1"

. "$(dirname "$0")/utils"

if [ ! -s "$SWAGGER_FILE" ] || ! jq . "$SWAGGER_FILE" > /dev/null
then
  wget "https://github.com/kubernetes/kubernetes/blob/release-$K8S_VERSION/api/openapi-spec/swagger.json?raw=true" -O "$SWAGGER_FILE"
fi

if [ ! -s "$REMOVED_PATHS_SWAGGER_FILE" ] || ! jq . "$REMOVED_PATHS_SWAGGER_FILE" > /dev/null
then
  jq --argjson debug "$DEBUG" -f remove-paths.jq "$SWAGGER_FILE" > "$REMOVED_PATHS_SWAGGER_FILE"
fi

if [ ! -s "$MERGED_DEFINITIONS_SWAGGER_FILE" ] || ! jq . "$MERGED_DEFINITIONS_SWAGGER_FILE" > /dev/null
then
  jq --argjson debug "$DEBUG" -f merge-definitions.jq "$REMOVED_PATHS_SWAGGER_FILE" > "$MERGED_DEFINITIONS_SWAGGER_FILE"
fi

