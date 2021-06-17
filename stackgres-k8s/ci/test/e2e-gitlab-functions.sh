#!/bin/sh

[ "$DEBUG" != true ] || set -x
SHELL_XTRACE="$(! echo $- | grep -q x || echo "-x")"

set -e

cd "$(dirname "$0")/../../.."

mkdir -p stackgres-k8s/ci/test/target
TEMP_DIR="/tmp/$CI_PROJECT_ID"
mkdir -p "$TEMP_DIR"

[ -n "$READ_API_TOKEN" ]
[ -n "$CI_PROJECT_ID" ]
[ -n "$CI_PIPELINE_ID" ]

get_or_default_script() {
  [ "$#" -ge 3 ]
  local URL="$1"
  local DEFAULT="$2"
  local FILE="$3"
  cat << EOF
[ "$DEBUG" != true ] || set -x
if [ ! -f '$FILE' ]
then
  HTTP_CODE="\$(curl -w '%{http_code}' -s --location --header 'PRIVATE-TOKEN: $READ_API_TOKEN' '$URL' -o '$FILE' || true)"
  if [ "\${HTTP_CODE%??}" != 2 ]
  then
    if [ "\${HTTP_CODE}" = 404 -a -n '$DEFAULT' ]
    then
      printf '%s' '$DEFAULT' > '$FILE'
    else
      echo "Error '\$HTTP_CODE' retrieving $URL"
      exit 1
    fi
  fi
fi
EOF
}
