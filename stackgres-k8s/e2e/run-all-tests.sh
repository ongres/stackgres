#!/bin/sh

. "${0%/*}/e2e"

echo "Utils loaded:"
e2e_list_utils | while read -r UTIL_PATH
  do
    printf '%s\n' " - $UTIL_PATH"
  done
echo

echo "Setup versions"
setup_versions
echo "Setup spec"
setup_spec

E2E_RETRY="${E2E_RETRY:-2}"
if [ -z "$E2E_ONLY_INCLUDES" ]
then
  E2E_ONLY_INCLUDES="$*"
fi
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
  if printf '%s' "$E2E_RUN_ONLY" | grep -q ":"
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
  E2E_ONLY_INCLUDES="$("$BATCH_LIST_TEST_FUNCTION" | to_e2e_test_batch "$BATCH_INDEX" "$BATCH_COUNT")"
fi

if [ "$E2E_USE_TEST_CACHE" = true ]
then
  echo "Retrieving cache..."
  E2E_EXCLUDES_BY_HASH="$(get_already_passed_tests)"
  echo 'done'

  if [ "$E2E_REPEAT_TESTS" != true ]
  then
    if echo "$E2E_EXCLUDES_BY_HASH" | grep -q '[^ ]'
    then
      echo "Excluding following tests since already passed:"
      echo
      printf '%s' "$E2E_EXCLUDES_BY_HASH" | tr ' ' '\n' | grep -v '^$' \
        | while read -r E2E_EXCLUDED_TEST
          do
            printf ' - %s\n' "$E2E_EXCLUDED_TEST"
          done
      echo
    fi
    E2E_EXCLUDES="$(echo "$E2E_EXCLUDES $E2E_EXCLUDES_BY_HASH" | tr ' ' '\n' | sort | uniq | tr '\n' ' ')"
  fi
fi

if [ -z "$E2E_ONLY_INCLUDES" ]
then
  SPECS="$(printf "%s\n%s" "$(get_all_specs)" "$(get_all_env_specs)")"
else
  SPECS="$(printf '%s' "$E2E_ONLY_INCLUDES" | tr ' ' '\n')"
fi

printf '%s\n' "$SPECS" | \
  while IFS="$(printf '\n')" read -r LINE
  do
    printf '%s\n' "${LINE##*spec/}"
  done > "$TARGET_PATH/all-tests"

if [ -n "$E2E_EXCLUDES" ]
then
  SPECS="$(
    printf '%s\n' "$SPECS" | \
      while IFS="$(printf '\n')" read -r LINE
      do
        printf '%s\n' "$SPEC_PATH/${LINE##*spec/}"
      done
    )"

  E2E_EXCLUDES="$(
    printf '%s\n' "$E2E_EXCLUDES" | tr ' ' '\n' | grep -v '^$' \
      | while IFS="$(printf '\n')" read -r LINE
        do
          printf '%s\n' "$SPECS" | grep "/${LINE##*spec/}$" || true
        done
    )"

  SPECS="$(printf '%s\n' "$SPECS" \
    | while IFS="$(printf '\n')" read -r LINE
      do
        if ! printf '%s\n' "$E2E_EXCLUDES" | grep -q "/${LINE##*spec/}$"
        then
          printf '%s\n' "$SPEC_PATH/${LINE##*spec/}"
        fi
      done
    )"
else
  SPECS="$(
    printf '%s\n' "$SPECS" | \
      while IFS="$(printf '\n')" read -r LINE
      do
        printf '%s\n' "$SPEC_PATH/${LINE##*spec/}"
      done
    )"
fi

SPECS_NO_STATS="$(
  printf '%s\n' "$SPECS" \
    | while IFS="$(printf '\n')" read -r LINE
      do
        if ! [ -f "$E2E_PATH/test.stats" ] \
          || ! cat "$E2E_PATH/test.stats" | cut -d : -f 1 | grep -qxF "${LINE##*spec/}"
        then
          printf '%s\n' "${LINE##*spec/}"
        fi
      done
  )"

if [ -n "$SPECS_NO_STATS" ]
then
  printf '%s' "Tests without stats:

$(printf '%s\n' "$SPECS_NO_STATS" \
  | sort | uniq \
  | while IFS="$(printf '\n')" read -r LINE
    do
      printf '%s\n' " - ${LINE##*spec/}"
    done)

Please, to improve performance add a duration estimation in seconds for each test in file

$E2E_PATH/test.stats

Format of each line is: <test path>:<duration>

"
fi

if [ -n "$E2E_EXCLUDES" ]
then
  printf '%s' "Excluded tests:

$(
    printf '%s\n' "$E2E_EXCLUDES" | tr ' ' '\n' | grep -v '^$' | \
      while IFS="$(printf '\n')" read -r LINE
      do
        printf '%s\n' " - ${LINE##*spec/}"
      done
  )
"
fi

printf '%s' "Running tests:

$(printf '%s\n' "$SPECS" | grep -v '^$' \
    | while IFS="$(printf '\n')" read -r LINE
    do
      printf '%s\n' " - ${LINE##*spec/}"
    done)

"

rm -f "$TARGET_PATH/runned-tests"
touch "$TARGET_PATH/runned-tests"
rm -f "$TARGET_PATH/passed-tests"
touch "$TARGET_PATH/passed-tests"
rm -f "$TARGET_PATH/e2e-tests-junit-report.results.xml"

if [ "$(printf '%s\n' "$SPECS" | tr '\n' ' ' | wc -w)" = 0 ]
then
  echo "Nothing to test!"
  cat << EOF > "$TARGET_PATH/e2e-tests-junit-report.xml"
<?xml version="1.0" encoding="UTF-8"?>
<testsuites time="0">
  <testsuite name="e2e tests" tests="0" time="0">
  </testsuite>
</testsuites>
EOF
  exit
fi

echo "Preparing environment"

echo "Setup k8s"
setup_k8s
echo "Setup images"
setup_images
echo "Setup repository cache"
setup_repository_cache
echo "Setup helm"
setup_helm
echo "Setup logs"
setup_logs
echo "Setup operator"
setup_operator

if [ "$E2E_USE_TEST_HASHES" = true ]
then
  echo "Calculating spec hashes"
  SPEC_HASHES="$(calculate_spec_hashes)"
else
  SPEC_HASHES=""
fi

echo "Functional tests results" > "$TARGET_PATH/logs/results.log"

export K8S_REUSE=true
export E2E_REUSE_OPERATOR_PODS=true
export E2E_SKIP_SETUP=false

START="$(date +%s)"
find "$TARGET_PATH" -maxdepth 1 -type f -name '*.retries' -delete
rm -f "$TARGET_PATH/specs_to_run.pipe"
mkfifo "$TARGET_PATH/specs_to_run.pipe"

rm -f "$TARGET_PATH/specs_to_run.result"
rm -f "$TARGET_PATH/specs_to_run.tail.pid"

spec_controller() {
  echo 'Run started!'
  COUNT=1
  OVERALL_RESULT=true
  SPECS_TO_COMPLETE=""
  COMPLETED_COUNT=0
  SPEC_COUNT="$(printf '%s\n' "$SPECS" | tr '\n' ' ' | wc -w)"
  while true
  do
    if [ "$COMPLETED_COUNT" -ge "$SPEC_COUNT" ]
    then
      if [ "$E2E_DISABLE_LOGS_SNAPSHOTS" != true ]
      then
        if [ "$E2E_DISABLE_LOGS" = "true" ] || [ "$E2E_DISABLE_RESOURCE_LOGS" = "true" ]
        then
          echo "Snapshotting resources"
          resource_watch --log-in-files >> "$LOG_PATH/all_resources.log" 2>/dev/null || true
        fi
        if [ "$E2E_DISABLE_LOGS" = "true" ] || [ "$E2E_DISABLE_POD_LOGS" = "true" ]
        then
          echo "Snapshotting pods logs"
          pod_logs --log-in-files >> "$LOG_PATH/all_pods.log" 2>/dev/null || true
        fi
        if [ "$E2E_DISABLE_LOGS" = "true" ] || [ "$E2E_DISABLE_EVENT_LOGS" = "true" ]
        then
          echo "Snapshotting events"
          event_watch -o wide >> "$TARGET_PATH/events.log" 2>/dev/null || true
        fi
      fi
      echo 'Run completed!'
      break
    fi
    SPEC_TO_RUN="$(printf '%s\n' "$SPECS" | tr ' ' '\n' | grep -vxF "" | tail -n+"$COUNT" | head -n 1)"
    if [ -n "$SPEC_TO_RUN" ]
    then
      COUNT="$((COUNT+1))"
      SPEC_NAME="${SPEC_TO_RUN##*/}"
      rm -f "$TARGET_PATH/$SPEC_NAME.completed"
      rm -f "$TARGET_PATH/$SPEC_NAME.failed"
      if ! [ -f "$SPEC_TO_RUN" ]
      then
        echo "Spec file $SPEC_TO_RUN not found! Aborting..."
        OVERALL_RESULT=false
        break
      fi
      printf '%s\n' "$SPEC_TO_RUN" >> "$TARGET_PATH/specs_to_run.pipe"
      printf '%s\n' "$SPEC_NAME" >> "$TARGET_PATH/runned-tests"
      SPECS_TO_COMPLETE="$SPECS_TO_COMPLETE $SPEC_TO_RUN"
    else
      sleep 2
    fi
    for SPEC_TO_COMPLETE in $SPECS_TO_COMPLETE
    do
      SPEC_NAME="${SPEC_TO_COMPLETE##*/}"
      if [ ! -f "$TARGET_PATH/$SPEC_NAME.completed" ]
      then
        continue
      fi
      SPEC_FAILED=false
      if [ -f "$TARGET_PATH/$SPEC_NAME.failed" ]
      then
        SPEC_FAILED=true
      fi
      echo "Spec $SPEC_NAME ($([ "$SPEC_FAILED" != true ] && printf 'succeded' || printf 'failed'))"
      SPECS_TO_COMPLETE="$(printf %s "$SPECS_TO_COMPLETE" | tr ' ' '\n' | grep -vxF "$SPEC_TO_COMPLETE" | tr '\n' ' ')"
      if printf '%s\n' "$SPEC_HASHES" | grep -q "^$SPEC_TO_COMPLETE:"
      then
        SPEC_HASH="$(printf '%s\n' "$SPEC_HASHES" | grep "^$SPEC:" | cut -d : -f 2)"
      else
        SPEC_HASH="$SPEC_NAME"
      fi
      if [ "$SPEC_FAILED" = true ]
      then
        RETRIES="$([ -f "$TARGET_PATH/$SPEC_NAME.retries" ] && cat "$TARGET_PATH/$SPEC_NAME.retries" || echo 0)"
        RETRIES="$((RETRIES + 1))"
        if [ "$RETRIES" -lt "$E2E_RETRY" ]
        then
          SPECS="$SPECS $SPEC_PATH/$SPEC_NAME"
          rm "$TARGET_PATH/$SPEC_NAME.completed"
          rm "$TARGET_PATH/$SPEC_NAME.failed"
          echo "$RETRIES" > "$TARGET_PATH/$SPEC_NAME.retries"
        else
          cat << EOF >> "$TARGET_PATH/e2e-tests-junit-report.results.xml"
    <testcase classname="$SPEC_NAME" name="$SPEC_HASH" time="$(cat "$TARGET_PATH/$SPEC_NAME.duration")">
      <failure message="$SPEC_NAME failed" type="ERROR">
      <![CDATA[
      $(show_logs "$SPEC_NAME")
      ]]>
      </failure>
    </testcase>
EOF
          COMPLETED_COUNT="$((COMPLETED_COUNT + 1))"
          OVERALL_RESULT=false
        fi
      else
        printf '%s\n' "$SPEC_NAME" >> "$TARGET_PATH/passed-tests"
        cat << EOF >> "$TARGET_PATH/e2e-tests-junit-report.results.xml"
    <testcase classname="$SPEC_NAME" name="$SPEC_HASH" time="$(cat "$TARGET_PATH/$SPEC_NAME.duration")" />
EOF
          COMPLETED_COUNT="$((COMPLETED_COUNT + 1))"
      fi
    done
  done
  until [ -f "$TARGET_PATH/specs_to_run.tail.pid" ]
  do
    sleep 1
  done
  kill "$(cat "$TARGET_PATH/specs_to_run.tail.pid")"
  if [ "$OVERALL_RESULT" = false ]
  then
    return 1
  else
    return 0
  fi
}

(
  notrace_function spec_controller
) &
SPEC_EMITTER_PID="$!"
trap_kill "$SPEC_EMITTER_PID"

set +e
OVERALL_RESULT=true
shell -c '
  echo $$ > "'"$TARGET_PATH/specs_to_run.tail.pid"'"
  exec tail -f "'"$TARGET_PATH/specs_to_run.pipe"'"
  ' | xargs_parallel_shell % -c "'$SHELL' $SHELL_XTRACE '$E2E_PATH/e2e' spec '%'"
EXIT_CODE="$?"
if [ "$EXIT_CODE" != 0 ]
then
  OVERALL_RESULT=false
fi
set -e

if ! wait "$SPEC_EMITTER_PID"
then
  if [ "$OVERALL_RESULT" = true ]
  then
    echo "No test failed but overall process failed...this is weird!"
    OVERALL_RESULT=false
  fi
fi

cat << EOF > "$TARGET_PATH/e2e-tests-junit-report.xml"
<?xml version="1.0" encoding="UTF-8"?>
<testsuites time="$(($(date +%s) - START))">
  <testsuite name="e2e tests" tests="$(printf '%s\n' "$SPECS" | tr '\n' ' ' | wc -w)" time="$(($(date +%s) - START))">
$(cat "$TARGET_PATH/e2e-tests-junit-report.results.xml")
  </testsuite>
</testsuites>
EOF

if [ "$E2E_USE_TEST_CACHE" = true ]
then
  store_test_results
fi

if [ "$K8S_DELETE" = true ]
then
  delete_k8s || true
fi

if [ "$OVERALL_RESULT" = true ]
then
  cat "$TARGET_PATH/logs/results.log" | sort
else
  cat "$TARGET_PATH/logs/results.log" | sort
  exit 1
fi
