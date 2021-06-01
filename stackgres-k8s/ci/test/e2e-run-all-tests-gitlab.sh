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

  echo "Preparing kind shared cache cache..."
  export E2E_LOCK_PATH="/tmp/e2e-lock$SUFFIX"
  export KIND_LOCK_PATH="/tmp/kind-lock$SUFFIX"
  if docker manifest inspect \
    "$CI_REGISTRY/$CI_PROJECT_PATH/stackgres/operator:$IMAGE_TAG_BASE" >/dev/null 2>&1
  then
    IMAGE_TAGS="$IMAGE_TAG_BASE-jvm $IMAGE_TAG_BASE"
  else
    IMAGE_TAGS="$IMAGE_TAG_BASE-jvm"
  fi
  if sh stackgres-k8s/ci/test/e2e-shared-cache-requires-reset.sh
  then
    flock /tmp/e2e-create-kind-cache-base \
      sh stackgres-k8s/ci/test/e2e-create-kind-cache-base.sh
  else
    flock "$E2E_LOCK_PATH" \
      sh stackgres-k8s/ci/test/e2e-create-kind-cache-base.sh
  fi
  export KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache/$KIND_NAME"

  echo "Retrieving jobs cache..."
  if "$IS_WEB"
  then
    E2E_TEST=ui
  fi
  if [ -n "$E2E_TEST" ]
  then
    export E2E_ONLY_INCLUDES="$E2E_TEST"
  fi
  export IS_WEB
  export IS_NATIVE
  E2E_EXCLUDES_BY_HASH="$(flock /tmp/e2e-retrieve-pipeline-info.lock \
    timeout -s KILL 300 \
    sh stackgres-k8s/ci/test/e2e-already-passed-gitlab.sh || true)"
  if echo "$E2E_EXCLUDES_BY_HASH" | grep -q '[^ ]'
  then
    echo "Excluding following tests since already passed:"
    echo
    printf '%s' "$E2E_EXCLUDES_BY_HASH" | tr ' ' '\n' | grep -v '^$' \
      | while read E2E_EXCLUDED_TEST
        do
          printf ' - %s\n' "$E2E_EXCLUDED_TEST"
        done
    echo
  fi
  E2E_EXCLUDES="$(echo "$("$IS_WEB" || echo "ui ")$E2E_EXCLUDES $E2E_EXCLUDES_BY_HASH" | tr ' ' '\n' | sort | uniq | tr '\n' ' ')"
  export E2E_EXCLUDES
  flock /tmp/e2e-retrieve-pipeline-info.lock timeout -s KILL 300 sh stackgres-k8s/ci/test/e2e-variables.sh || true

  unset DEBUG

  flock -s /tmp/stackgres-build-operator-native-executable \
    flock -s /tmp/stackgres-build-restapi-native-executable \
    flock -s /tmp/stackgres-build-jobs-native-executable \
    flock -s /tmp/stackgres-build-distributedlogs-controller-native-executable \
    flock "/tmp/stackgres-integration-test$SUFFIX" \
    flock "$E2E_LOCK_PATH" \
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