#!/bin/sh

. "$(dirname "$0")/e2e"

echo "Preparing environment"

setup_k8s

echo "Functional tests results" > "$TARGET_PATH/logs/results.log"

if find "$(realpath "$(dirname "$0")")/spec" -type f \
  | grep '^.*/[^\.]\+$' \
  | xargs -r -n 1 -I % -P 0 sh $(! echo $- | grep -q x || echo "-x") "$(dirname "$0")/e2e" spec "%"
then
  cat "$TARGET_PATH/logs/results.log"
else
  cat "$TARGET_PATH/logs/results.log"
  exit 1
fi
