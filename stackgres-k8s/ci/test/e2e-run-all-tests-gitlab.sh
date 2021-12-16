#!/bin/sh
# shellcheck disable=SC2039

# shellcheck disable=SC1090
. "$(dirname "$0")/e2e-functions.sh"

set -e

# shellcheck disable=SC2015
{ [ "$IS_WEB" = true ] || [ "$IS_WEB" = false ]; } \
  && { [ "$IS_NATIVE" = true ] || [ "$IS_NATIVE" = false ]; } \
  && [ -n "$E2E_SUFFIX" ] && [ -n "$E2E_RUN_ONLY" ] \
  && [ -n "$CI_JOB_ID" ] && [ -n "$CI_PROJECT_ID" ] && [ -d "$CI_PROJECT_DIR" ] \
  && [ -n "$CI_COMMIT_SHORT_SHA" ] && [ -n "$CI_PROJECT_PATH" ] \
  && [ -n "$CI_REGISTRY" ] && [ -n "$CI_REGISTRY_USER" ] && [ -n "$CI_REGISTRY_PASSWORD" ] \
  && true || false

export E2E_SHELL="${E2E_SHELL:-sh}"
export E2E_ENV="${E2E_ENV:-kind}"
export E2E_PARALLELISM="${E2E_PARALLELISM:-32}"
export K8S_VERSION="${K8S_VERSION:-1.16.15}"
export K8S_FROM_DIND=true
export K8S_REUSE="${K8S_REUSE:-false}"
# shellcheck disable=SC2155
export K8S_DELETE="$([ "$K8S_REUSE" = true ] && echo false || echo true)"
E2E_FAILURE_RETRY="${E2E_FAILURE_RETRY:-4}"
E2E_STORE_RESULTS_RETRY="${E2E_STORE_RESULTS_RETRY:-4}"

SUFFIX="$(echo "-$E2E_SUFFIX-$E2E_RUN_ONLY" | tr -d '\n' | tr -c 'a-z0-9' '-' | sed 's/\(-[0-9]\+\)-[0-9]\+$/\1/')"

export IMAGE_TAG="${CI_COMMIT_TAG:-"$CI_COMMIT_SHORT_SHA"}$IMAGE_TAG_SUFFIX"
export KIND_NAME="kind$SUFFIX"
export K8S_REUSE="${K8S_REUSE:-true}"
export K8S_FROM_DIND=true
export E2E_BUILD_IMAGES=false
export E2E_WAIT_OPERATOR=false
export E2E_PULLED_IMAGES_PATH="/tmp/pulled-images$SUFFIX"
export E2E_OPERATOR_REGISTRY=$CI_REGISTRY
export E2E_OPERATOR_REGISTRY_PATH=/$CI_PROJECT_PATH/
export E2E_FORCE_IMAGE_PULL=true
export K8S_USE_INTERNAL_REPOSITORY=true
export KIND_LOCK_PATH="/tmp/kind-lock$SUFFIX"
export E2E_LOCK_PATH="/tmp/e2e-lock$SUFFIX"
export E2E_DISABLE_CACHE=true
export E2E_DISABLE_LOGS=true
export KIND_LOG=true
export KIND_LOG_PATH="/tmp/kind-log$SUFFIX"
export KIND_LOG_RESOURCES=true
export KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache$SUFFIX"
export TEMP_DIR="/tmp/$CI_PROJECT_ID"

copy_project_to_temp_dir() {
  echo "Copying project files ..."

  mkdir -p "$TEMP_DIR"

  docker run --rm -i -u 0 -v "$TEMP_DIR:$TEMP_DIR" alpine rm -rf "$TEMP_DIR/stackgres-build-$CI_JOB_ID"
  cp -r . "$TEMP_DIR/stackgres-build-$CI_JOB_ID"

  echo "done"
}

clean_up_project_temp_dir() {
  echo "Cleaning up ..."

  docker run --rm -u 0 -v "$TEMP_DIR:$TEMP_DIR" alpine rm -rf "$TEMP_DIR/stackgres-build-$CI_JOB_ID"

  echo "done"
}

get_sensible_variables() {
  # shellcheck disable=SC2039
  local E2E_SENSIBLE_VARIABLES='
    E2E_ENV
    E2E_COMPONENTS_REGISTRY
    E2E_COMPONENTS_REGISTRY_PATH
    E2E_EXTENSIONS_REGISTRY_PATH
    E2E_IMAGE_MAP
    E2E_MAJOR_SOURCE_POSTGRES_VERSION
    E2E_MAJOR_TARGET_POSTGRES_VERSION
    E2E_MINOR_SOURCE_POSTGRES_VERSION
    E2E_MINOR_TARGET_POSTGRES_VERSION
    E2E_OPERATOR_OPTS
    E2E_OPERATOR_REGISTRY
    E2E_OPERATOR_REGISTRY_PATH
    E2E_SET_MAX_LENGTH_NAMES
    E2E_SET_MAX_LENGTH_NAMES_PLUS_ONE
    E2E_SKIP_UPGRADE_FROM_PREVIOUS_OPERATOR
    E2E_STORAGE_CLASS_REFLINK_ENABLED
    '
  E2E_SENSIBLE_VARIABLES=" $(echo "$E2E_SENSIBLE_VARIABLES" | tr '\n' ' ' | tr -s ' ') "

  env | grep '^\(E2E_.*\|K8s_.*\|EXTENSIONS_.*\|STACKGRES_.*\)$' \
    | cut -d = -f 1 | sort \
    | while read -r NAME
      do
        if [ "${NAME%%_*}" != E2E ] || echo "$E2E_SENSIBLE_VARIABLES" | grep -qF " $NAME "
        then
          eval "printf '%s=%s\n' \"$NAME\" \"\$$NAME\""
        fi
      done
}

get_already_passed_tests() {
  sh stackgres-k8s/ci/build/build-functions.sh generate_image_hashes

  JVM_IMAGE_MODULE_HASH="$(
    grep '^jvm-image=' stackgres-k8s/ci/build/target/image-type-hashes \
      | cut -d = -f 2)"
  NATIVE_IMAGE_MODULE_HASH="$(
    grep '^native-image=' stackgres-k8s/ci/build/target/image-type-hashes \
      | cut -d = -f 2)"
  UI_IMAGE_MODULE_HASH="$(
    grep '^ui-image=' stackgres-k8s/ci/build/target/image-type-hashes \
      | cut -d = -f 2)"
  VARIABLES="$(get_sensible_variables)"
  SPEC_PLATFORM="$(sh stackgres-k8s/ci/build/build-functions.sh get_platform)"
  # shellcheck disable=SC2015
  [ -n "$JVM_IMAGE_MODULE_HASH" ] \
    && [ -n "$UI_IMAGE_MODULE_HASH" ] \
    && [ -n "$NATIVE_IMAGE_MODULE_HASH" ] \
    && true || false
  sh stackgres-k8s/e2e/e2e calculate_spec_hashes > stackgres-k8s/ci/test/target/test-hashes
  while read -r SPEC_HASH
  do
    SPEC_HASH="${SPEC_HASH##*/}"
    SPEC_NAME="${SPEC_HASH%:*}"
    SPEC_HASH="${SPEC_HASH#*:}"
    SPEC_RESULT_HASH="$(
      {
        printf '%s\n' "$SPEC_NAME"
        printf '%s\n' "$SPEC_HASH"
        if "$IS_NATIVE"
        then
          printf '%s\n' "$NATIVE_IMAGE_MODULE_HASH"
        else
          printf '%s\n' "$JVM_IMAGE_MODULE_HASH"
        fi
        if [ "$SPEC_NAME" = ui ]
        then
          printf '%s\n' "$UI_IMAGE_MODULE_HASH"
        fi
        printf '%s\n' "$VARIABLES"
      } | md5sum | cut -d ' ' -f 1)"
    printf '%s=%s/%s/e2e-test-result-%s:%s-%s\n' \
      "$SPEC_NAME" "$CI_REGISTRY" "$CI_PROJECT_PATH" "$SPEC_NAME" "$SPEC_RESULT_HASH" "$SPEC_PLATFORM"
  done < stackgres-k8s/ci/test/target/test-hashes \
    > stackgres-k8s/ci/test/target/test-result-images

  cut -d = -f 2- stackgres-k8s/ci/test/target/test-result-images \
    > stackgres-k8s/ci/test/target/all-test-result-images

  sh stackgres-k8s/ci/build/build-functions.sh find_image_digests \
    stackgres-k8s/ci/test/target/all-test-result-images \
    > stackgres-k8s/ci/test/target/test-result-image-digests

  rm -f stackgres-k8s/ci/test/target/already-passed-tests
  touch stackgres-k8s/ci/test/target/already-passed-tests
  if [ "$E2E_DO_TESTS" != true ]
  then
    while IFS='=' read -r TEST_NAME IMAGE_NAME
    do
      if grep -q "^$IMAGE_NAME=" stackgres-k8s/ci/test/target/test-result-image-digests
      then
        printf '%s\n' "$TEST_NAME" >> stackgres-k8s/ci/test/target/already-passed-tests
      fi
    done < stackgres-k8s/ci/test/target/test-result-images
  fi

  tr '\n' ' ' < stackgres-k8s/ci/test/target/already-passed-tests
}

store_test_results() {
  cat << EOF > stackgres-k8s/ci/test/target/Dockerfile.e2e
FROM alpine:3.13.5
  COPY . /project
EOF

  # shellcheck disable=SC2046
  docker build -f stackgres-k8s/ci/test/target/Dockerfile.e2e \
    -t "$CI_REGISTRY/$CI_PROJECT_PATH/e2e-test-result$SUFFIX:$CI_COMMIT_SHORT_SHA" \
    $(
      while read -r TEST_NAME
      do
        IMAGE_NAME="$(grep "^$TEST_NAME=" stackgres-k8s/ci/test/target/test-result-images \
          | cut -d = -f 2-)"
        printf '%s %s ' '-t' "$IMAGE_NAME"
      done < stackgres-k8s/e2e/target/passed-tests
    ) \
    stackgres-k8s/e2e/target
  docker push "$CI_REGISTRY/$CI_PROJECT_PATH/e2e-test-result$SUFFIX:$CI_COMMIT_SHORT_SHA"
  while read -r TEST_NAME
  do
    IMAGE_NAME="$(grep "^$TEST_NAME=" stackgres-k8s/ci/test/target/test-result-images \
      | cut -d = -f 2-)"
    printf '%s\n' "$IMAGE_NAME"
  done < stackgres-k8s/e2e/target/passed-tests \
    | xargs -I % -P "$E2E_PARALLELISM" docker push %
}

add_already_passed_tests_to_report() {
  E2E_ALREADY_PASSED_COUNT="$(wc -l stackgres-k8s/ci/test/target/already-passed-tests | cut -d ' ' -f 1)"

  cat << EOF > stackgres-k8s/ci/test/target/already-passed-e2e-tests-junit-report.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites time="0">
  <testsuite name="e2e tests already passed" tests="$E2E_ALREADY_PASSED_COUNT" time="0">
    $(
      while read -r TEST_NAME
      do
        if ! grep -qxF "$TEST_NAME" stackgres-k8s/e2e/target/all-tests
        then
          continue
        fi
        TEST_HASH="$(grep "^$TEST_NAME:" stackgres-k8s/ci/test/target/test-hashes)"
        TEST_HASH="${TEST_HASH#*:}"
        cat << INNER_EOF
    <testcase classname="$TEST_NAME" name="$TEST_HASH" time="0" />
INNER_EOF
      done < stackgres-k8s/ci/test/target/already-passed-tests
      )
  </testsuite>
</testsuites>
EOF
}

show_test_result_summary() {
  local EXIT_CODE="${1:-0}"

  local TEST_NAME
  local ANY_TEST_ALREADY_PASSED=false
  while read -r TEST_NAME
  do
    if grep -qxF "$TEST_NAME" stackgres-k8s/e2e/target/all-tests
    then
      ANY_TEST_ALREADY_PASSED=true
      break
    fi
  done < stackgres-k8s/ci/test/target/already-passed-tests
  if [ "$ANY_TEST_ALREADY_PASSED" = true ]
  then
    echo "Already passed tests:"
    echo
    while read -r TEST_NAME
    do
      if grep -qxF "$TEST_NAME" stackgres-k8s/e2e/target/all-tests
      then
        printf ' - %s\n' "$TEST_NAME"
      fi
    done < stackgres-k8s/ci/test/target/already-passed-tests
    echo
  else
    echo "No test where cached before for this build"
    echo
  fi

  if [ -s stackgres-k8s/e2e/target/passed-tests ]
  then
    echo "Passed tests:"
    echo
    while read -r TEST_NAME
    do
      printf ' - %s\n' "$TEST_NAME"
    done < stackgres-k8s/e2e/target/passed-tests
    echo
  elif [ "$({ wc -l stackgres-k8s/e2e/target/runned-tests 2>/dev/null || echo '0'; } | cut -d ' ' -f 1)" != \
    "$({ wc -l stackgres-k8s/e2e/target/passed-tests 2>/dev/null || echo '0'; } | cut -d ' ' -f 1)" ]
  then
    echo "Passed tests:"
    echo
    echo "None :("
    echo
  fi

  if [ "$({ wc -l stackgres-k8s/e2e/target/runned-tests 2>/dev/null || echo '0'; } | cut -d ' ' -f 1)" != \
    "$({ wc -l stackgres-k8s/e2e/target/passed-tests 2>/dev/null || echo '0'; } | cut -d ' ' -f 1)" ]
  then
    echo "Failed tests:"
    echo
    while read -r TEST_NAME
    do
      if ! grep -qxF "$TEST_NAME" stackgres-k8s/e2e/target/passed-tests
      then
        printf ' - %s\n' "$TEST_NAME"
      fi
    done < stackgres-k8s/e2e/target/all-tests
    echo
  else
    if [ "$EXIT_CODE" != 0 ]
    then
      echo "Something went bad?! :_("
    else
      echo "Everything went well! :)"
    fi
    echo
  fi
}

run_all_tests_loop() {
  docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"

  echo "Variables:"
  echo
  get_sensible_variables \
    | while read -r VARIABLE
      do
        printf ' - %s\n' "$VARIABLE"
      done
  echo

  echo "Retrieving cache..."
  export IS_WEB
  export IS_NATIVE
  E2E_EXCLUDES_BY_HASH="$(get_already_passed_tests)"
  echo 'done'

  echo

  echo "Retrieved image digests:"
  sort stackgres-k8s/ci/test/target/all-test-result-images | uniq \
    | while read -r IMAGE_NAME
      do
        printf ' - %s => %s\n' "$IMAGE_NAME" "$(
          { grep "^$IMAGE_NAME=" stackgres-k8s/ci/test/target/test-result-image-digests || echo '=<not found>'; } \
            | cut -d = -f 2-)"
      done
  echo "done"

  echo

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
  E2E_EXCLUDES="$(echo "$("$IS_WEB" || echo "ui ")$E2E_EXCLUDES $E2E_EXCLUDES_BY_HASH" | tr ' ' '\n' | sort | uniq | tr '\n' ' ')"
  export E2E_EXCLUDES

  echo

  echo "Extracting helm packages and templates..."

  sh stackgres-k8s/ci/build/build-functions.sh extract helm-packages stackgres-k8s/install/helm/template/packages
  sh stackgres-k8s/ci/build/build-functions.sh extract helm-templates stackgres-k8s/install/helm/template/templates

  echo 'done'

  echo

  unset DEBUG

  echo "Running e2e tests..."

  # shellcheck disable=SC2086
  # shellcheck disable=SC2046
  flock -s /tmp/stackgres-build-operator-native-executable \
    flock -s /tmp/stackgres-build-restapi-native-executable \
    flock -s /tmp/stackgres-build-jobs-native-executable \
    flock -s /tmp/stackgres-build-distributedlogs-controller-native-executable \
    flock "/tmp/stackgres-integration-test$SUFFIX" \
    flock "$E2E_LOCK_PATH" \
    timeout -s KILL 3600 \
    "$E2E_SHELL" -c $([ "$E2E_DEBUG" != true ] || printf '%s' '-x') \
      "
      docker run --rm -u 0 -v '${KIND_LOG_PATH%/*}:/source' alpine \
        rm -rf '/source/${KIND_LOG_PATH##*/}'
      docker run --rm -u 0 -v '${KIND_LOG_PATH%/*}:/source' alpine \
        mkdir -p '/source/${KIND_LOG_PATH##*/}'
      '$E2E_SHELL' $([ "$E2E_DEBUG" != true ] || printf '%s' '-x') stackgres-k8s/e2e/run-all-tests.sh
      EXIT_CODE=\"\$?\"
      mkdir -p stackgres-k8s/e2e/target/kind-logs
      docker run --rm -u 0 -v '$KIND_LOG_PATH:/source/kind-logs' \
        -v '$(pwd)/stackgres-k8s/e2e/target:/target' alpine \
        cp -r '/source/kind-logs/.' /target/kind-logs/.
      docker run --rm -u 0 -v '${KIND_LOG_PATH%/*}:/source' alpine \
        rm -rf '/source/${KIND_LOG_PATH##*/}'
      docker run --rm -u 0 \
        -v '$(pwd)/stackgres-k8s/e2e/target:/target' alpine \
        chown -R '$(id -u):$(id -g)' '/target/kind-logs'
      if [ -d stackgres-k8s/e2e/target/kind-logs/kubernetes ]
      then
        tar c stackgres-k8s/e2e/target/kind-logs/kubernetes \
          | xz -v -c > stackgres-k8s/e2e/target/kind-logs/kubernetes.tar.lzma
        rm -rf stackgres-k8s/e2e/target/kind-logs/kubernetes
      fi
      exit \"\$EXIT_CODE\"
      "
  echo 'done'

  echo
}

run_all_tests() {
  if [ "$E2E_CLEAN_IMAGE_CACHE" = "true" ]
  then
    rm -rf "$E2E_PULLED_IMAGES_PATH"
  fi

  if "$IS_WEB"
  then
    E2E_TEST=ui
  fi

  if [ -n "$E2E_TEST" ]
  then
    export E2E_ONLY_INCLUDES="$E2E_TEST"
  fi

  copy_project_to_temp_dir

  set +e

  (
  cd "$TEMP_DIR/stackgres-build-$CI_JOB_ID"
  while true
  do
    (
    set -e

    run_all_tests_loop
    )
    EXIT_CODE="$?"
    if [ "$EXIT_CODE" = 0 ]
    then
      break
    elif [ -f "stackgres-k8s/e2e/target/e2e-tests-junit-report.xml" ]
    then
      break
    fi
    E2E_FAILURE_RETRY="$((E2E_FAILURE_RETRY - 1))"
    if [ "$E2E_FAILURE_RETRY" -le 0 ]
    then
      break
    fi
    echo "Something bad happened, will retry $E2E_FAILURE_RETRY times more in 10 seconds..."
    sleep 10
  done

  set +e
  while true
  do
    (
    set -e
    store_test_results
    )
    STORE_RESULTS_EXIT_CODE="$?"
    if [ "$STORE_RESULTS_EXIT_CODE" = 0 ]
    then
      break
    fi
    E2E_STORE_RESULTS_RETRY="$((E2E_STORE_RESULTS_RETRY - 1))"
    if [ "$E2E_STORE_RESULTS_RETRY" -le 0 ]
    then
      break
    fi
    sleep 10
  done

  mkdir -p "$CI_PROJECT_DIR/stackgres-k8s/ci/build/target"
  rm -rf stackgres-k8s/ci/build/target/.git
  cp -r stackgres-k8s/ci/build/target/. "$CI_PROJECT_DIR/stackgres-k8s/ci/build/target/."
  mkdir -p "$CI_PROJECT_DIR/stackgres-k8s/ci/test/target"
  cp -r stackgres-k8s/ci/test/target/. "$CI_PROJECT_DIR/stackgres-k8s/ci/test/target/."
  mkdir -p "$CI_PROJECT_DIR/stackgres-k8s/e2e/target"
  cp -r stackgres-k8s/e2e/target/. "$CI_PROJECT_DIR/stackgres-k8s/e2e/target/."

  exit "$EXIT_CODE"
  )
  EXIT_CODE="$?"

  clean_up_project_temp_dir

  add_already_passed_tests_to_report

  show_test_result_summary "$EXIT_CODE"

  exit "$EXIT_CODE"
}

if [ "$#" -gt 0 ]
then
  "$@"
else
  run_all_tests
fi
