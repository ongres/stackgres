#!/bin/sh
# shellcheck disable=SC2039

# shellcheck disable=SC1090
. "$(dirname "$0")/e2e-functions.sh"

set -e

# shellcheck disable=SC2015
{ [ "$IS_WEB" = true ] || [ "$IS_WEB" = false ]; } \
  && [ -n "$E2E_JOB" ] && [ -n "$E2E_RUN_ONLY" ] \
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

SUFFIX="${SUFFIX:-$(echo "-$E2E_JOB-$E2E_RUN_ONLY" | tr -d '\n' | tr -c 'a-z0-9' '-' | sed 's/\(-[0-9]\+\)-[0-9]\+$/\1/')}"

export E2E_SUFFIX="${E2E_SUFFIX:-$SUFFIX}"
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
export E2E_DISABLE_CACHE="${E2E_DISABLE_CACHE:-false}"
export E2E_DISABLE_LOGS="${E2E_DISABLE_LOGS:-true}"
export E2E_SPEC_TRY_UNINSTALL_ON_FAILURE="${E2E_SPEC_TRY_UNINSTALL_ON_FAILURE:-true}"
export E2E_SKIP_SPEC_UNINSTALL="${E2E_SKIP_SPEC_UNINSTALL:-true}"
export KIND_LOCK_PATH="/tmp/kind-lock$SUFFIX"
export KIND_LOG="${KIND_LOG:-true}"
export KIND_LOG_PATH="/tmp/kind-log$SUFFIX"
export KIND_LOG_RESOURCES="${KIND_LOG_RESOURCES:-false}"
export KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache$SUFFIX"
export EXTENSIONS_CACHE_HOST_PATH="/containerd-cache/extensions"
export TEMP_DIR="/tmp/$CI_PROJECT_ID"
export E2E_TEST_REGISTRY="$CI_REGISTRY"
export E2E_TEST_REGISTRY_PATH="$CI_PROJECT_PATH"

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

run_all_tests_loop() {
  docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"
  if [ -n "$EXTRA_REGISTRY_USER" ] && [ -n "$EXTRA_REGISTRY_PASSWORD" ] && [ -n "$EXTRA_REGISTRY" ]
  then
    docker login -u "$EXTRA_REGISTRY_USER" -p "$EXTRA_REGISTRY_PASSWORD" "$EXTRA_REGISTRY"
  fi

  echo "Variables:"
  echo
  sh stackgres-k8s/e2e/e2e get_variables_for_hash \
    | while read -r VARIABLE
      do
        printf ' - %s\n' "$VARIABLE"
      done
  echo

  if [ "$E2E_SKIP_TEST_CACHE" != true ]
  then
    echo "Retrieving cache..."
    export IS_WEB
    E2E_EXCLUDES_BY_HASH="$(sh stackgres-k8s/e2e/e2e get_already_passed_tests)"
    echo 'done'
  
    echo
  else
    echo "Skipping cache, all tests will be executed!"
    E2E_EXCLUDES_BY_HASH=""
  fi

  echo "Retrieved image digests:"
  sort stackgres-k8s/e2e/target/all-test-result-images | uniq \
    | while read -r IMAGE_NAME
      do
        printf ' - %s => %s\n' "$IMAGE_NAME" "$(
          { grep "^$IMAGE_NAME=" stackgres-k8s/e2e/target/test-result-image-digests || echo '=<not found>'; } \
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

  sh stackgres-k8s/ci/build/build-functions.sh extract helm-packages stackgres-k8s/install/helm/target/packages
  sh stackgres-k8s/ci/build/build-functions.sh extract helm-templates stackgres-k8s/install/helm/target/templates

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
    "$E2E_SHELL" "$0" run_with_e2e_lock \
    timeout -s KILL 3600 \
    "$E2E_SHELL" "$0" run_all_e2e
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
    echo "Something bad happened, will retry one more time in 10 seconds (retries left $E2E_FAILURE_RETRY)..."
    sleep 10
  done

  set +e
  while true
  do
    (
    set -e
    sh stackgres-k8s/e2e/e2e store_test_results
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

  sh stackgres-k8s/e2e/e2e add_already_passed_tests_to_report

  sh stackgres-k8s/e2e/e2e show_test_result_summary "$EXIT_CODE"

  exit "$EXIT_CODE"
}

run_with_e2e_lock() {
  set +e
  while true
  do
    for POSSIBLE_SUFFIX in \
      -jvm-image-exclusive-1        \
      -jvm-image-exclusive-2        \
      -jvm-image-exclusive-3        \
      -jvm-image-non-exclusive-1    \
      -jvm-image-non-exclusive-2    \
      -jvm-image-non-exclusive-3    \
      -jvm-image-ui-1               \
      -native-image-exclusive-1     \
      -native-image-exclusive-2     \
      -native-image-exclusive-3     \
      -native-image-non-exclusive-1 \
      -native-image-non-exclusive-2 \
      -native-image-non-exclusive-3 \
      -native-image-ui-1            
    do
      SUFFIX="$POSSIBLE_SUFFIX" flock -n "/tmp/stackgres-integration-test$POSSIBLE_SUFFIX" \
        "$E2E_SHELL" "$0" run_in_e2e_lock "$@"
      EXIT_CODE="$?"
      if [ -f "/tmp/stackgres-integration-test$POSSIBLE_SUFFIX-was-locked-by-$CI_JOB_ID" ]
      then
        rm -f "/tmp/stackgres-integration-test$SUFFIX-was-locked-by-$CI_JOB_ID"
        return "$EXIT_CODE"
      fi
    done
  done
  return 1
}

run_in_e2e_lock() {
  touch "/tmp/stackgres-integration-test$SUFFIX-was-locked-by-$CI_JOB_ID"
  echo "Locked kind with name kind$SUFFIX"
  "$@"
}

run_all_e2e() {
  set +e
  docker run --rm -u 0 -v "${KIND_LOG_PATH%/*}:/source" alpine \
    rm -rf "/source/${KIND_LOG_PATH##*/}"
  docker run --rm -u 0 -v "${KIND_LOG_PATH%/*}:/source" alpine \
    mkdir -p "/source/${KIND_LOG_PATH##*/}"
  "$E2E_SHELL" $([ "$E2E_DEBUG" != true ] || printf '%s' '-x') stackgres-k8s/e2e/run-all-tests.sh
  EXIT_CODE="$?"
  if [ "$KIND_LOG" = true ]
  then
    mkdir -p stackgres-k8s/e2e/target/kind-logs
    docker run --rm -u 0 -v "$KIND_LOG_PATH:/source/kind-logs" \
      -v "$(pwd)/stackgres-k8s/e2e/target:/target" alpine \
      cp -r "/source/kind-logs/." /target/kind-logs/.
    docker run --rm -u 0 -v "${KIND_LOG_PATH%/*}:/source" alpine \
      rm -rf "/source/${KIND_LOG_PATH##*/}"
    docker run --rm -u 0 \
      -v "$(pwd)/stackgres-k8s/e2e/target:/target" alpine \
      chown -R "$(id -u):$(id -g)" '/target/kind-logs'
    if [ -d stackgres-k8s/e2e/target/kind-logs/kubernetes ]
    then
      tar c stackgres-k8s/e2e/target/kind-logs/kubernetes \
        | xz -v -c > stackgres-k8s/e2e/target/kind-logs/kubernetes.tar.lzma
      rm -rf stackgres-k8s/e2e/target/kind-logs/kubernetes
    fi
  fi
  return "$EXIT_CODE"
}

if [ "$#" -gt 0 ]
then
  "$@"
else
  run_all_tests
fi
