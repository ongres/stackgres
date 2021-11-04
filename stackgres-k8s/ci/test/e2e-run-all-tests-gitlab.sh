#!/bin/sh

. "$(dirname "$0")/e2e-gitlab-functions.sh"

export E2E_PATH="$(pwd)/stackgres-k8s/e2e"
E2E_FAILURE_RETRY="${E2E_FAILURE_RETRY:-4}"
{ [ "$IS_WEB" = true ] || [ "$IS_WEB" = false ]; } \
  && { [ "$IS_NATIVE" = true ] || [ "$IS_NATIVE" = false ]; } \
  && [ -n "$E2E_SUFFIX" ] && [ -n "$IMAGE_TAG_SUFFIX" ] && [ -n "$E2E_RUN_ONLY" ] \
  && [ -n "$CI_COMMIT_SHORT_SHA" ] && [ -n "$CI_PROJECT_PATH" ] \
  && [ -n "$CI_REGISTRY" ] && [ -n "$CI_REGISTRY_USER" ] && [ -n "$CI_REGISTRY_PASSWORD" ] \
  && true || false

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
  export E2E_OPERATOR_REGISTRY=$CI_REGISTRY
  export E2E_OPERATOR_REGISTRY_PATH=/$CI_PROJECT_PATH/
  export E2E_FORCE_IMAGE_PULL=true
  export K8S_USE_INTERNAL_REPOSITORY=true
  export KIND_LOCK_PATH="/tmp/kind-lock$SUFFIX"
  export E2E_LOCK_PATH="/tmp/e2e-lock$SUFFIX"
  export KIND_CONTAINERD_CACHE_PATH="/tmp/kind-cache$SUFFIX"

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

  docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"

  echo "Variables:"
  echo
  sh stackgres-k8s/ci/test/e2e-variables.sh \
    | while read -r VARIABLE
      do
        printf ' - %s' "$VARIABLE"
      done
  echo

  echo "Retrieving cache..."
  export IS_WEB
  export IS_NATIVE
  E2E_EXCLUDES_BY_HASH="$(sh stackgres-k8s/ci/test/e2e-already-passed-gitlab.sh)"
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

  sh stackgres-k8s/ci/build/build-gitlab.sh extract helm-packages stackgres-k8s/install/helm/template/packages
  sh stackgres-k8s/ci/build/build-gitlab.sh extract helm-templates stackgres-k8s/install/helm/template/templates

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

xq -r '
  select(.testsuites != null and .testsuites.testsuite != null and .testsuites.testsuite.testcase != null)
  | if (.testsuites.testsuite.testcase | type) == "object"
    then [.testsuites.testsuite.testcase][]
    else .testsuites.testsuite.testcase[]
    end
  | select((has("failure")|not))["@name"]' \
  stackgres-k8s/e2e/target/e2e-tests-junit-report.xml \
  > stackgres-k8s/ci/test/target/passed-tests

cat << EOF > stackgres-k8s/e2e/target/Dockerfile.e2e
FROM alpine:3.13.5
  COPY . /e2e
EOF

mkdir -p "$TEMP_DIR/stackgres-build-$CI_JOB_ID/stackgres-k8s/e2e/target"
rm -rf "$TEMP_DIR/stackgres-build-$CI_JOB_ID/stackgres-k8s/e2e/target"
cp -r  stackgres-k8s/e2e/target/. "$TEMP_DIR/stackgres-build-$CI_JOB_ID/stackgres-k8s/e2e/target/."
(
cd "$TEMP_DIR/stackgres-build-$CI_JOB_ID"
docker build -f stackgres-k8s/e2e/target/Dockerfile.e2e \
  $(
    cat stackgres-k8s/ci/test/target/passed-tests \
      | while read -r TEST_NAME
        do
          IMAGE_NAME="$(grep "^TEST_NAME=" stackgres-k8s/ci/test/target/test-result-images \
            | cut -d = -f 2-)"
          printf '%s %s ' '-t' "$IMAGE_NAME"
        done
  ) \
  stackgres-k8s/e2e/target
)
rm -rf "$TEMP_DIR/stackgres-build-$CI_JOB_ID"
cat stackgres-k8s/ci/test/target/passed-tests \
  | while read -r TEST_NAME
    do
      IMAGE_NAME="$(grep "^TEST_NAME=" stackgres-k8s/ci/test/target/test-result-images \
        | cut -d = -f 2-)"
      docker push "$IMAGE_NAME"
    done

exit "$EXIT_CODE"
