#!/bin/sh

. "$(dirname "$0")/e2e"

echo "Preparing environment"

setup_k8s

echo "Functional tests results" > "$TARGET_PATH/logs/results.log"

SPECS="$(find "$(realpath "$(dirname "$0")")/spec" -type f | grep '^.*/[^\.]\+$')"

export REUSE_K8S=true
export BUILD_OPERATOR=false
export REUSE_OPERATOR=true

PARALLELISM="${PARALLELISM:-8}"
COUNT=0
SPECS_TO_RUN=""
SH_OPTS=$(! echo $- | grep -q x || echo "-x")
OVERALL_RESULT=true
SPEC_COUNT="$(echo "$SPECS" | tr ' ' '\n' | wc -l)"
for SPEC in $(echo $SPECS)
do
  COUNT="$((COUNT+1))"
  SPECS_TO_RUN="$SPECS_TO_RUN $SPEC"
  if [ "$((COUNT%PARALLELISM))" -eq 0 -o "$COUNT" -eq "$SPEC_COUNT" ]
  then
    setup_k8s
    if ! echo "$SPECS_TO_RUN" | tr ' ' '\n' | tail -n +2 \
      | xargs -r -n 1 -I % -P 0 "$SHELL" $SH_OPTS "$(dirname "$0")/e2e" spec "%"
    then
      OVERALL_RESULT=false
      break;
    fi
    SPECS_TO_RUN=""
  fi
done

if $OVERALL_RESULT
then
  cat "$TARGET_PATH/logs/results.log"
else
  cat "$TARGET_PATH/logs/results.log"
  exit 1
fi
