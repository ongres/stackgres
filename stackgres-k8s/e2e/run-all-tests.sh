#!/bin/sh

. "$(dirname "$0")/e2e"

E2E_PARALLELISM="${E2E_PARALLELISM:-8}"
E2E_RETRY="${E2E_RETRY:-2}"

echo "Preparing environment"

setup_k8s

echo "Functional tests results" > "$TARGET_PATH/logs/results.log"

SPECS="$(find "$SPEC_PATH" -type f | grep '^.*/[^\.]\+$')"

export K8S_REUSE=true
export E2E_BUILD_OPERATOR=false
export REUSE_OPERATOR=true

START="$(date +%s)"
COUNT=0
SPECS_TO_RUN=""
SH_OPTS=$(! echo $- | grep -q x || echo "-x")
OVERALL_RESULT=true
CLEANUP=false
find "$TARGET_PATH" -maxdepth 1 -type f -name '*.retries' -delete
while true
do
  SPEC_COUNT="$(echo "$SPECS" | tr ' ' '\n' | wc -l)"
  if [ "$COUNT" -ge "$SPEC_COUNT" ]
  then
    break
  fi
  COUNT="$((COUNT+1))"
  SPEC="$(echo "$SPECS" | tr ' ' '\n' | tail -n+"$COUNT" | head -n 1)"
  SPEC_NAME="$(basename "$SPEC")"
  SPECS_TO_RUN="$SPECS_TO_RUN $SPEC"
  if [ "$((COUNT%E2E_PARALLELISM))" -eq 0 -o "$COUNT" -eq "$SPEC_COUNT" ]
  then
    if "$CLEANUP"
    then
      CLEANUP=false
      setup_k8s
    fi
    if ! echo "$SPECS_TO_RUN" | tr ' ' '\n' | tail -n +2 \
      | xargs -r -n 1 -I % -P 0 "$SHELL" $SH_OPTS "$(dirname "$0")/e2e" spec "%"
    then
      if [ "$((COUNT%E2E_PARALLELISM))" -ne 0 ]
      then
        PAD_COUNT="$((E2E_PARALLELISM - COUNT%E2E_PARALLELISM))"
        SPECS="$SPECS $(seq 1 "$PAD_COUNT")"
        COUNT="$((COUNT+PAD_COUNT))"
      fi
      for FAILED in $(find "$TARGET_PATH" -maxdepth 1 -type f -name '*.failed')
      do
        if [ "$(stat -c %Y "$FAILED")" -lt "$START" ]
        then
          continue
        fi
        SPEC_NAME="$(basename "$FAILED"|sed 's/\.failed$//')"
        RETRIES="$([ -f "$TARGET_PATH/$SPEC_NAME.retries" ] && cat "$TARGET_PATH/$SPEC_NAME.retries" || echo 0)"
        RETRIES="$((RETRIES + 1))"
        if [ "$RETRIES" -lt "$E2E_RETRY" ]
        then
          echo "$RETRIES" > "$TARGET_PATH/$SPEC_NAME.retries"
          rm "$FAILED"
          SPECS="$SPECS $SPEC_PATH/$SPEC_NAME"
        else
          OVERALL_RESULT=false
          break
        fi
        CLEANUP=true
      done
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
