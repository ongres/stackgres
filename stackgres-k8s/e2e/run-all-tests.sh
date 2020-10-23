#!/bin/sh

. "$(dirname "$0")/e2e"

E2E_PARALLELISM="${E2E_PARALLELISM:-8}"
E2E_RETRY="${E2E_RETRY:-2}"
E2E_ONLY_INCLUDES="${E2E_ONLY_INCLUDES}"
E2E_EXCLUDES="${E2E_EXCLUDES}"
SPECS_EXCLUDED=""
SPECS_NO_STATS=""

if [ -n "$E2E_RUN_ONLY" ] && [ -z "$E2E_ONLY_INCLUDES" ]
then
  BATCH_LIST_TEST_FUNCTION="get_all_${E2E_RUN_ONLY%:*}_specs"
  if [ "$BATCH_LIST_TEST_FUNCTION" != "get_all_exclusive_specs" ] \
    && [ "$BATCH_LIST_TEST_FUNCTION" != "get_all_non_exclusive_specs" ]
  then
    echo 'E2E_RUN_ONLY must follow pattern `(exclusive|non_exclusive)[:<batch index>/<batch count>]`, but it was '"$E2E_RUN_ONLY"
    exit 1
  fi
  if echo "$E2E_RUN_ONLY" | grep -q ":"
  then
    BATCH_CONFIG="${E2E_RUN_ONLY##*:}"
    BATCH_INDEX="${BATCH_CONFIG%/*}"
    BATCH_COUNT="${BATCH_CONFIG#*/}"
  else
    BATCH_INDEX="1"
    BATCH_COUNT="1"
  fi
  if ! [ "$BATCH_INDEX" -ge 1 ] && [ "$BATCH_INDEX" -le "$BATCH_COUNT" ]
  then
    echo 'Batch index start from 1 and must be less or equal than batch count, but it was '"$E2E_RUN_ONLY"
    exit 1
  fi
  if ! [ "$BATCH_COUNT" -ge 1 ]
  then
    echo 'Batch count must be greather or equal to 1, but it was '"$E2E_RUN_ONLY"
    exit 1
  fi
  BATCH_TESTS="$("$BATCH_LIST_TEST_FUNCTION")"
  if [ -n "$E2E_EXCLUDES" ]
  then
    BATCH_TESTS="$(echo "$BATCH_TESTS" | \
      while IFS="$(printf '\n')" read LINE
      do
        IS_EXCLUDED=false
        for EXCLUDED in $E2E_EXCLUDES
        do
          if echo "${LINE##*spec/}" | grep -qxF "${EXCLUDED##*spec/}"
          then
            SPECS_EXCLUDED="$SPECS_EXCLUDED ${EXCLUDED##*spec/}"
            IS_EXCLUDED=true
            break
          fi
        done
        if ! "$IS_EXCLUDED"
        then
          echo "${LINE##*spec/}"
        fi
      done)"
  fi
  E2E_ONLY_INCLUDES="$(echo "$BATCH_TESTS"  \
    | while IFS="$(printf '\n')" read LINE
      do
        if [ -f "$E2E_PATH/test.stats" ] \
          && cat "$E2E_PATH/test.stats" | cut -d : -f 1 | grep -qxF "${LINE##*spec/}"
        then
          INDEX="$(cat "$E2E_PATH/test.stats" | cut -d : -f 1 \
            | grep -nxF "${LINE##*spec/}" | cut -d : -f 1)"
          tail -n "+$INDEX" "$E2E_PATH/test.stats" | head -n 1
        else
          SPECS_NO_STATS="$SPECS_NO_STATS ${LINE##*spec/}"
          echo "${LINE##*spec/}:3600"
        fi
      done \
    | sort | sort -t : -k 2 -n | grep -n '.' \
    | while IFS="$(printf '\n')" read LINE
      do
        if [ "$(( (${LINE%%:*} - 1) % BATCH_COUNT))" = "$((BATCH_INDEX - 1))" ]
        then
          echo "$LINE" | cut -d : -f 2
        fi
      done)"
fi

if [ -z "$E2E_ONLY_INCLUDES" ]
then
  SPECS="$(printf "%s\n%s" "$(get_all_specs)" "$(get_all_env_specs)")"
else
  SPECS="$(echo_raw "$E2E_ONLY_INCLUDES" | tr ' ' '\n')"
fi

if [ -n "$E2E_EXCLUDES" ] && [ -z "$SPECS_EXCLUDED" ]
then
  SPECS="$(echo "$SPECS" | \
    while IFS="$(printf '\n')" read LINE
    do
      IS_EXCLUDED=false
      for EXCLUDED in $E2E_EXCLUDES
      do
        if echo "${LINE##*spec/}" | grep -qxF "${EXCLUDED##*spec/}"
        then
          SPECS_EXCLUDED="$SPECS_EXCLUDED ${EXCLUDED##*spec/}"
          IS_EXCLUDED=true
          break
        fi
      done
      if ! "$IS_EXCLUDED"
      then
        echo "$SPEC_PATH/${LINE##*spec/}"
      fi
    done)"
else
  SPECS="$(echo "$SPECS" | \
    while IFS="$(printf '\n')" read LINE
    do
      echo "$SPEC_PATH/${LINE##*spec/}"
    done)"
fi

SPECS="$(
  SPEC_COUNT="$(echo "$SPECS" | tr ' ' '\n' | wc -l)"
  BATCH_COUNT="$(( (SPEC_COUNT + E2E_PARALLELISM - 1) / E2E_PARALLELISM ))"
  for BATCH_INDEX in $(seq 1 $BATCH_COUNT)
  do
    echo "$SPECS"  \
      | while IFS="$(printf '\n')" read LINE
        do
          if [ -f "$E2E_PATH/test.stats" ] \
            && cat "$E2E_PATH/test.stats" | cut -d : -f 1 | grep -qxF "${LINE##*spec/}"
          then
            INDEX="$(cat "$E2E_PATH/test.stats" | cut -d : -f 1 \
              | grep -nxF "${LINE##*spec/}" | cut -d : -f 1)"
            tail -n "+$INDEX" "$E2E_PATH/test.stats" | head -n 1
          else
            SPECS_NO_STATS="$SPECS_NO_STATS ${LINE##*spec/}"
            echo "${LINE##*spec/}:3600"
          fi
        done \
      | sort | sort -t : -k 2 -n | grep -n '.' \
      | while IFS="$(printf '\n')" read LINE
        do
          if [ "$(( (${LINE%%:*} - 1) % BATCH_COUNT))" = "$((BATCH_INDEX - 1))" ]
          then
            echo "$SPEC_PATH/$(echo "$LINE" | cut -d : -f 2)"
          fi
        done
  done)"

if [ -n "$SPECS_NO_STATS" ]
then
  echo_raw "Tests without stats:

$(echo "$SPECS_NO_STATS" \
  | sort | uniq \
  | while IFS="$(printf '\n')" read LINE
    do
      echo "${LINE##*spec/}"
    done)

Please, to improve performance add a duration estimation in seconds for each test in file

$E2E_PATH/test.stats

Format of each line is: <test path>:<duration>

"
fi

if [ -n "$SPECS_EXCLUDED" ]
then
  echo_raw "Excluded tests:

$(echo "$SPECS_EXCLUDED" | \
    while IFS="$(printf '\n')" read LINE
    do
      echo "${LINE##*spec/}"
    done)
"
fi

echo_raw "Running tests:

$(echo "$SPECS" | \
    while IFS="$(printf '\n')" read LINE
    do
      echo "${LINE##*spec/}"
    done)
"

echo "Preparing environment"

setup_versions
setup_images
setup_k8s
setup_cache
setup_helm
setup_operator
setup_logs

rm -f "$TARGET_PATH/e2e-tests-junit-report.results.xml"

echo "Functional tests results" > "$TARGET_PATH/logs/results.log"

export K8S_REUSE=true
export E2E_REUSE_OPERATOR_PODS=true
export E2E_SKIP_SETUP=false

START="$(date +%s)"
COUNT=0
SPECS_TO_RUN=""
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
  SPECS_TO_RUN="${SPECS_TO_RUN# *}"
  if [ "$((COUNT%E2E_PARALLELISM))" -eq 0 ] || [ "$COUNT" -eq "$SPEC_COUNT" ]
  then
    if "$CLEANUP"
    then
      CLEANUP=false
      setup_k8s
    fi
    SPECS_FAILED=""
    if ! echo "$SPECS_TO_RUN" | tr ' ' '\n' \
      | xargs -r -n 1 -I % -P 0 "$SHELL" $SHELL_XTRACE -c "'$SHELL' $SHELL_XTRACE '$E2E_PATH/e2e' spec '%'"
    then
      if [ "$((COUNT%E2E_PARALLELISM))" -ne 0 ]
      then
        PAD_COUNT="$((E2E_PARALLELISM - COUNT%E2E_PARALLELISM))"
        SPECS="$SPECS $(seq 1 "$PAD_COUNT")"
        COUNT="$((COUNT+PAD_COUNT))"
      fi
      for FAILED in $(find "$TARGET_PATH" -maxdepth 1 -type f -name '*.failed')
      do
        if [ "$(stat -c %Y "$FAILED" || echo 0)" -lt "$START" ]
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
          SPECS_FAILED="$SPECS_FAILED $SPEC_NAME"
          SPECS_FAILED="${SPECS_FAILED# *}"
          OVERALL_RESULT=false
        fi
        CLEANUP=true
      done
    fi
    RUNNED_COUNT=0
    while [ "$RUNNED_COUNT" -lt "$(echo "$SPECS_TO_RUN" | tr ' ' '\n' | wc -l)" ]
    do
      RUNNED_COUNT="$((RUNNED_COUNT+1))"
      SPEC="$(echo "$SPECS_TO_RUN" | tr ' ' '\n' | tail -n+"$RUNNED_COUNT" | head -n 1)"
      SPEC_NAME="$(basename "$SPEC")"
      if echo " $SPECS_FAILED " | grep -q -F " $SPEC_NAME "
      then
        cat << EOF >> "$TARGET_PATH/e2e-tests-junit-report.results.xml"
    <testcase classname="$SPEC_NAME" name="$SPEC_NAME" time="$(cat "$TARGET_PATH/$SPEC_NAME.duration")">
      <failure message="$SPEC_NAME failed" type="ERROR">
      <![CDATA[
      $(show_logs "$SPEC_NAME")
      ]]>
      </failure>
    </testcase>
EOF
      else
        cat << EOF >> "$TARGET_PATH/e2e-tests-junit-report.results.xml"
    <testcase classname="$SPEC_NAME" name="$SPEC_NAME" time="$(cat "$TARGET_PATH/$SPEC_NAME.duration")" />
EOF
      fi
    done
    SPECS_TO_RUN=""
  fi
done

cat << EOF >> "$TARGET_PATH/e2e-tests-junit-report.xml"
<?xml version="1.0" encoding="UTF-8"?>
<testsuites time="$(($(date +%s) - START))">
  <testsuite name="e2e tests" tests="$(echo "$SPECS" | tr ' ' '\n' | wc -l)" time="$(($(date +%s) - START))">
$(cat "$TARGET_PATH/e2e-tests-junit-report.results.xml")
  </testsuite>
</testsuites>
EOF

if $OVERALL_RESULT
then
  cat "$TARGET_PATH/logs/results.log"
else
  cat "$TARGET_PATH/logs/results.log"
  exit 1
fi
