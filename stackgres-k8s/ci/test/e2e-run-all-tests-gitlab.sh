#!/bin/sh

. "$(dirname "$0")/e2e-gitlab-functions.sh"

E2E_FAILURE_RETRY="${E2E_FAILURE_RETRY:-4}"

set +e
while true
do
  (
  set -e
  SUFFIX="$(echo "-$E2E_SUFFIX-$E2E_RUN_ONLY" | tr -d '\n' | tr -c 'a-z0-9' '-' | sed 's/\(-[0-9]\+\)-[0-9]\+$/\1/')"

  export IMAGE_TAG="${CI_COMMIT_TAG:-"$CI_COMMIT_SHORT_SHA"}$IMAGE_TAG_SUFFIX"
  export KIND_NAME="kind$SUFFIX"
  export K8S_REUSE="${K8S_REUSE:-true}"
  export K8S_FROM_DIND=true
  export E2E_BUILD_IMAGES=false
  export E2E_WAIT_OPERATOR=false
  export E2E_PULLED_IMAGES_PATH="/tmp/pulled-images$SUFFIX"
  export KIND_LOCK_PATH="/tmp/kind-lock$SUFFIX"
  export KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache$SUFFIX"
  export E2E_OPERATOR_REGISTRY=$CI_REGISTRY
  export E2E_OPERATOR_REGISTRY_PATH=/$CI_PROJECT_PATH/
  export E2E_FORCE_IMAGE_PULL=true
  export K8S_USE_INTERNAL_REPOSITORY=true

  if [ -n "$E2E_TEST" ]
  then
    export E2E_ONLY_INCLUDES="$E2E_TEST"
  fi

  if [ "$E2E_CLEAN_IMAGE_CACHE" = "true" ]
  then
    rm -rf "$E2E_PULLED_IMAGES_PATH"
  fi

  docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"

  if [ "$K8S_REUSE" = false ]
  then
    export K8S_DELETE=true
    export E2E_SPEC_TRY_UNINSTALL_ON_FAILURE=true
  fi

  unset DEBUG

  flock -s /tmp/stackgres-build-operator-native-executable \
    flock -s /tmp/stackgres-build-restapi-native-executable \
    flock -s /tmp/stackgres-build-jobs-native-executable \
    flock -s /tmp/stackgres-build-distributedlogs-controller-native-executable \
    flock "/tmp/stackgres-integration-test$SUFFIX" \
    timeout -s KILL 3600 \
    "$E2E_SHELL" $SHELL_XTRACE stackgres-k8s/e2e/run-all-tests.sh
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
done

exit "$EXIT_CODE"