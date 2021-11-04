#!/bin/sh

# shellcheck disable=SC1090
. "$(dirname "$0")/e2e-gitlab-functions.sh"

env | grep '^\(E2E_.*\|K8s_.*\|EXTENSIONS_.*\|STACKGRES_.*\)$' \
  | cut -d = -f 1 | sort | uniq \
  | while read -r NAME
    do
      eval "printf '%s=%s\n' \"$NAME\" \"\$$NAME\""
    done

