#!/bin/sh

set -e

cd "$(dirname "$0")"
TARGET=target
mkdir -p "$TARGET"

IMAGE="$1"
test -n "$IMAGE"
REPOSITORY="${IMAGE%%:*}"
PROJECT_ID="$(jq -r ".[\"$REPOSITORY\"] | if . != null then . else error(\"Project not found for repository $REPOSITORY\") end" "${OPENSHIFT_CERTIFICATION_PROJECTS_JSON_PATH:-$HOME/.openshift-certification-projects.json}")"
. "${OPENSHIFT_CERTIFICATION_TOKENS_PATH:-$HOME/.openshift-tokens}"

REPOSITORY="${IMAGE%%/*}"
if [ "$REPOSITORY" = docker.io ]
then
  REPOSITORY=https://index.docker.io/v1/
fi

AUTH="$(jq -r '.auths|to_entries|.[]|select(.key == "'"$REPOSITORY"'").value.auth' "${OPENSHIFT_CERTIFICATION_PROJECTS_JSON_PATH:-$HOME/.openshift-certification-auths.json}")"
if [ -z "$AUTH" ]
then
  echo "No auth found for repository $REPOSITORY in auths.json"
  exit 1
fi

echo true | jq -c "{auths:{\"$REPOSITORY\":{auth:\"$AUTH\"}}}" > "$TARGET"/auth.json

rm -rf preflight.log artifacts/
preflight check container \
  --certification-project-id="$PROJECT_ID" \
  --submit \
  --docker-config "$TARGET"/auth.json \
  --pyxis-api-token "$PYXIS_API_TOKEN" \
  "$IMAGE"
