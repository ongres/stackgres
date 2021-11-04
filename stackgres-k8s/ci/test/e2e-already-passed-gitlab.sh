#!/bin/sh

if [ "$E2E_SKIP_TEST_CACHE" = true ]
then
  exit
fi

. "$(dirname "$0")/e2e-gitlab-functions.sh"

{ [ "$IS_WEB" = true ] || [ "$IS_WEB" = false ]; } \
  && { [ "$IS_NATIVE" = true ] || [ "$IS_NATIVE" = false ]; } \
  && [ -n "$CI_REGISTRY" ] && [ -n "$CI_PROJECT_PATH" ] \
  && true || false

if [ "$E2E_DO_ALL_TESTS" = true ]
then
  exit
fi

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
VARIABLES="$(sh stackgres-k8s/ci/test/e2e-variables.sh)"
[ -n "$JVM_IMAGE_MODULE_HASH" ] \
  && [ -n "$UI_IMAGE_MODULE_HASH" ] \
  && [ -n "$NATIVE_IMAGE_MODULE_HASH" ] \
  && true || false
sh stackgres-k8s/e2e/e2e calculate_spec_hashes \
  | while read -r SPEC_HASH
    do
      SPEC_HASH="${SPEC_HASH##*/}"
      SPEC_NAME="${SPEC_HASH#*:}"
      SPEC_HASH="${SPEC_HASH%:*}"
      SPEC_RESULT_HASH="$(
        {
          echo "$SPEC_NAME"
          echo "$SPEC_HASH"
          if "$IS_NATIVE"
          then
            echo "$NATIVE_IMAGE_MODULE_HASH"
          else
            echo "$JVM_IMAGE_MODULE_HASH"
          fi
          if [ "$SPEC_NAME" = ui ]
          then
            echo "$UI_IMAGE_MODULE_HASH"
          fi
        } | md5sum | cut -d ' ' -f 1)"
      printf '%s=%s/%s/e2e-test-result-%s:%s\n' \
        "$SPEC_NAME" "$CI_REGISTRY" "$CI_PROJECT_PATH" "$SPEC_NAME" "$SPEC_RESULT_HASH"
    done > stackgres-k8s/ci/test/target/test-result-images

cut -d = -f 2- stackgres-k8s/ci/test/target/test-result-images \
  > stackgres-k8s/ci/test/target/all-test-result-images

sh stackgres-k8s/ci/build/build-functions.sh retrieve_image_digests stackgres-k8s/ci/test/target/all-test-result-images \
  > stackgres-k8s/ci/test/target/test-result-image-digests

rm -f stackgres-k8s/ci/test/target/already-passed-tests
touch stackgres-k8s/ci/test/target/already-passed-tests
cat stackgres-k8s/ci/test/target/test-result-images \
  | while IFS='=' read -r TEST_NAME IMAGE_NAME
    do
      if grep -q "^$IMAGE_NAME=" stackgres-k8s/ci/test/target/test-result-image-digests
      then
        printf '%s\n' "$TEST_NAME" >> stackgres-k8s/ci/test/target/already-passed-tests
      fi
    done

E2E_ALREADY_PASSED_COUNT="$(wc -l stackgres-k8s/ci/test/target/already-passed-tests | cut -d ' ' -f 1)"

cat << EOF > stackgres-k8s/ci/test/target/already-passed-e2e-tests-junit-report.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites time="0">
  <testsuite name="e2e tests already passed" tests="$E2E_ALREADY_PASSED_COUNT" time="0">
    $(cat stackgres-k8s/ci/test/target/already-passed-e2e-tests-junit-report.results.xml)
  </testsuite>
</testsuites>
EOF

cat stackgres-k8s/ci/test/target/already-passed-tests | tr '\n' ' '
