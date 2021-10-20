#!/bin/sh

if [ "$E2E_SKIP_TEST_CACHE" = true ]
then
  exit
fi

. "$(dirname "$0")/e2e-gitlab-functions.sh"

[ -n "$CI_PIPELINE_ID" ]
[ -n "$CI_JOB_NAME" ]
[ "$IS_WEB" = true ] || [ "$IS_WEB" = false ]
[ "$IS_NATIVE" = true ] || [ "$IS_NATIVE" = false ]

RETRY=3
while true
do
  curl -f -s --header "PRIVATE-TOKEN: $READ_API_TOKEN" \
    "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines?per_page=100" \
    > stackgres-k8s/ci/test/target/pipelines.json
  curl -f -s --header "PRIVATE-TOKEN: $READ_API_TOKEN" \
    "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines/$CI_PIPELINE_ID/jobs?per_page=100&scope[]=failed&scope[]=success&include_retried=true" \
    > stackgres-k8s/ci/test/target/jobs.json
  if ! jq null stackgres-k8s/ci/test/target/pipelines.json stackgres-k8s/ci/test/target/jobs.json >/dev/null
  then
    rm -f stackgres-k8s/ci/test/target/pipelines.json stackgres-k8s/ci/test/target/jobs.json
  else
    break
  fi
  if [ "$RETRY" -lt 0 ]
  then
    exit 1
  fi
  RETRY="$((RETRY-1))"
  sleep 5
done
mkdir -p "$TEMP_DIR/jobs"
PIPELINE_SAME_JOB_IDS="$(jq -r ".[] | select(.name == \"$CI_JOB_NAME\").id" stackgres-k8s/ci/test/target/jobs.json)"
printf '%s' "$PIPELINE_SAME_JOB_IDS" \
  | xargs -I '~' -P 16 sh -ec "
    RETRY=3
    while true
    do
      $(
      get_or_default_script "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/jobs/~/artifacts" \
        'file-not-found' "$TEMP_DIR/jobs/artifacts.zip.~"
      )
      if grep -qxF 'file-not-found' "$TEMP_DIR/jobs/artifacts.zip.~"
      then
        echo '[{}, []]' > '$TEMP_DIR/jobs/job_test_report.~'
        break
      fi
      if ! unzip -p '$TEMP_DIR/jobs/artifacts.zip.~' >/dev/null
      then
        rm -f '$TEMP_DIR/jobs/artifacts.zip.~' '$TEMP_DIR/jobs/job_test_report.~'
      else
        [ -f '$TEMP_DIR/jobs/job_test_report.~' ] \
          || (unzip -p '$TEMP_DIR/jobs/artifacts.zip.~' stackgres-k8s/e2e/target/e2e-tests-junit-report.xml 2>/dev/null \
              || echo '<empty />') > '$TEMP_DIR/jobs/e2e-tests-junit-report.xml.~'
        if ! xq null '$TEMP_DIR/jobs/e2e-tests-junit-report.xml.~' >/dev/null
        then
          rm -f '$TEMP_DIR/jobs/artifacts.zip.~' '$TEMP_DIR/jobs/job_test_report.~'
        else
          xq 'select(has(\"empty\")|not) | .testsuites.testsuite.test_cases = (
              (if (.testsuites.testsuite.testcase | type) == \"object\"
                then [.testsuites.testsuite.testcase] else .testsuites.testsuite.testcase end)
              | map(.status = if has(\"failure\") then \"failure\" else \"success\" end
                | del(.failure)
                | .execution_time = .[\"@time\"]
                | del(.[\"@time\"]))
                | group_by(.[\"@classname\"])
                | map(sort_by(if .status == \"failure\" then 0 else 1 end) | .[0]))
            | del(.testsuites.testsuite.testcase)
            | .testsuites.testsuite.total_time = .testsuites.testsuite[\"@time\"]
            | del(.testsuites.testsuite[\"@time\"])
            | .testsuites.testsuite.total_count = .testsuites.testsuite[\"@tests\"]
            | del(.testsuites.testsuite[\"@tests\"])
            | .testsuites.testsuite[\"@name\"] = '\"\$(
                jq '.[] | select(.id == ~).name' stackgres-k8s/ci/test/target/jobs.json
              )\"'
            | .test_suites = [ .testsuites.testsuite ]
            | del(.testsuites)' \
              '$TEMP_DIR/jobs/e2e-tests-junit-report.xml.~' \
            | tr -d '@' \
               > '$TEMP_DIR/jobs/job_test_report.~'
          break
        fi
      fi
      if [ \"\$RETRY\" -lt 0 ]
      then
        exit 1
      fi
      RETRY=\"\$((RETRY-1))\"
      sleep 5
    done
    "
jq -r '.[] | .status + " " + .updated_at + " " + (.id | tostring)' stackgres-k8s/ci/test/target/pipelines.json \
  | while read STATUS UPDATED_AT PIPELINE_ID
    do
      UPDATED_AT="$(date -d "$UPDATED_AT" +%s)"
      META_UPDATED_AT="$(stat -c %Y "$TEMP_DIR/test_report_with_variables.$PIPELINE_ID" 2>/dev/null || echo 0)"
      # Re-download if pipeline updated after meta has been updated
      if [ "$((UPDATED_AT + 300))" -gt "$META_UPDATED_AT" ]
      then
        rm -f "$TEMP_DIR/test_report.$PIPELINE_ID" \
          "$TEMP_DIR/variables.$PIPELINE_ID" \
          "$TEMP_DIR/test_report_with_variables.$PIPELINE_ID"
      fi
    done
if ! jq null "$TEMP_DIR"/test_report.* "$TEMP_DIR"/variables.* >/dev/null
then
  {
    echo "$TEMP_DIR"/test_report.*
    echo "$TEMP_DIR"/variables.*
  } | tr ' ' '\n' \
    | while read -r JSON_FILE
      do
        if ! jq null "$JSON_FILE" >/dev/null
        then
          rm -f "$JSON_FILE"
        fi
      done
fi
jq -r '.[].id' stackgres-k8s/ci/test/target/pipelines.json \
  | xargs -I @ -P 16 sh -ec "
    RETRY=3
    while true
    do
      $(
      get_or_default_script "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines/@/test_report" \
        '{}' "$TEMP_DIR/test_report.@"
      )
      if ! jq null '$TEMP_DIR/test_report.@' > /dev/null
      then
        rm '$TEMP_DIR/test_report.@'
      else
        break
      fi
      if [ \"\$RETRY\" -lt 0 ]
      then
        exit 1
      fi
      RETRY=\"\$((RETRY-1))\"
      sleep 5
    done
    "
jq -r '.[].id' stackgres-k8s/ci/test/target/pipelines.json \
  | xargs -I @ -P 16 sh -ec "
    RETRY=3
    while true
    do
      $(
      get_or_default_script "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines/@/variables" \
        '[]' "$TEMP_DIR/variables.@"
      )
      if ! jq null '$TEMP_DIR/variables.@' > /dev/null
      then
        rm '$TEMP_DIR/variables.@'
      else
        break
      fi
      if [ \"\$RETRY\" -lt 0 ]
      then
        exit 1
      fi
      RETRY=\"\$((RETRY-1))\"
      sleep 5
    done
    "
jq -r '.[].id' stackgres-k8s/ci/test/target/pipelines.json | xargs -I @ -P 16 sh -ec "
    [ -f '$TEMP_DIR/test_report_with_variables.@' ] || jq -s '.' \
      '$TEMP_DIR/test_report.@' '$TEMP_DIR/variables.@' > '$TEMP_DIR/test_report_with_variables.@'"

if [ "$E2E_DO_ALL_TESTS" = true ]
then
  exit
fi

RESOURCE_MODULE_HASH="$(jq -r -s "$(cat << EOF
  .[] | select(.test_suites != null)
    | .test_suites[]
    | select(.name == "build").test_cases[]
    | select(.classname == "module type resource").name
EOF
      )" "$TEMP_DIR/test_report.$CI_PIPELINE_ID" \
    | tr -d '\n')"
JAVA_MODULE_HASH="$(jq -r -s "$(cat << EOF
  .[] | select(.test_suites != null)
    | .test_suites[]
    | select(.name == "build").test_cases[]
    | select(.classname == "module type jvm-image").name
EOF
      )" "$TEMP_DIR/test_report.$CI_PIPELINE_ID" \
    | tr -d '\n')"
WEB_MODULE_HASH="$(jq -r -s "$(cat << EOF
  .[] | select(.test_suites != null)
    | .test_suites[]
    | select(.name == "build").test_cases[]
    | select(.classname == "module admin-ui-image").name
EOF
      )" "$TEMP_DIR/test_report.$CI_PIPELINE_ID" \
    | tr -d '\n')"
NATIVE_MODULE_HASH="$(jq -r -s "$(cat << EOF
  .[] | select(.test_suites != null)
    | .test_suites[]
    | select(.name == "build").test_cases[]
    | select(.classname == "module type native-image").name
EOF
      )" "$TEMP_DIR/test_report.$CI_PIPELINE_ID" \
    | tr -d '\n')"
VARIABLE_PREFIXES='["E2E_", "K8S_", "KIND_", "EXTENSIONS_"]'
VARIABLES="$(jq -c -s "$(cat << EOF
  .[]
    | map(select(.key as \$key | $VARIABLE_PREFIXES
      | map(. as \$prefix | \$key | startswith(\$prefix)) | any))
    | sort_by(.key)
EOF
      )" "$TEMP_DIR/variables.$CI_PIPELINE_ID")"
TEST_HASHES="$(sh stackgres-k8s/e2e/e2e calculate_spec_hashes | sed 's#^.*/\([^/]\+\)$#\1#' \
  | jq -R . | jq -s 'map(.|split(":")|{ key: .[0], value: .[1] })|from_entries')"
[ -n "$JAVA_MODULE_HASH" -a -n "$WEB_MODULE_HASH" -a -n "$NATIVE_MODULE_HASH" ]
(
jq -r -s "$(cat << EOF
  . as \$in | $TEST_HASHES as \$test_hashes
    | \$in[] | select(.[0].test_suites != null)
    | select(.[0].test_suites[] | select(.name == "build").test_cases
      | map(.classname == "module type resource" and .name == "$RESOURCE_MODULE_HASH") | any)
    | select(.[0].test_suites[] | select(.name == "build").test_cases
      | map(($IS_NATIVE) or .classname == "module type jvm-image" and .name == "$JAVA_MODULE_HASH") | any)
    | select(.[0].test_suites[] | select(.name == "build").test_cases
      | map(($IS_WEB | not) or (.classname == "module admin-ui-image" and .name == "$WEB_MODULE_HASH")) | any)
    | select(.[0].test_suites[] | select(.name == "build").test_cases
      | map(($IS_NATIVE | not) or (.classname == "module type native-image" and .name == "$NATIVE_MODULE_HASH")) | any)
    | select((.[1]
      | map(select(.key as \$key | $VARIABLE_PREFIXES
        | map(. as \$prefix | \$key | startswith(\$prefix)) | any))
      | sort_by(.key)) == $VARIABLES)
    | .[0].test_suites[]
    | select(.name == "$CI_JOB_NAME").test_cases[]
    | select(\$test_hashes[.classname] == .name and .status == "success")
EOF
  )" $(echo "$TEMP_DIR"/test_report_with_variables.* \
  | sed "s/\(^\|\)[^ ]\+\.${CI_PIPELINE_ID}\($\| \)/\1/g")
if [ "x$PIPELINE_SAME_JOB_IDS" != x ]
then
  jq -r -s "$(cat << EOF
    .[] | .test_suites[]
      | select(.name == "$CI_JOB_NAME").test_cases[]
      | select(.status == "success")
EOF
    )" $(printf '%s' "$PIPELINE_SAME_JOB_IDS" \
      | xargs -I @ echo "$TEMP_DIR/jobs/job_test_report.@")
fi
) > stackgres-k8s/ci/test/target/already_passed_tests.json
jq -r -s '.[] | .classname' stackgres-k8s/ci/test/target/already_passed_tests.json | sort | uniq | tr '\n' ' '

jq -r -s 'unique_by(.classname)[] | "<testcase classname=\"" + .classname + "\" name=\"" + .name + "\" time=\"" + (.execution_time|tostring) + "\" />"' \
  stackgres-k8s/ci/test/target/already_passed_tests.json > stackgres-k8s/ci/test/target/already-passed-e2e-tests-junit-report.results.xml

E2E_ALREADY_PASSED_COUNT="$(jq -r -s 'unique_by(.classname) | length' stackgres-k8s/ci/test/target/already_passed_tests.json)"
E2E_ALREADY_PASSED_TIME="$(jq -r -s 'unique_by(.classname) | map(.execution_time) | add' stackgres-k8s/ci/test/target/already_passed_tests.json)"

cat << EOF > stackgres-k8s/ci/test/target/already-passed-e2e-tests-junit-report.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites time="$E2E_ALREADY_PASSED_TIME">
  <testsuite name="e2e tests already passed" tests="$E2E_ALREADY_PASSED_COUNT" time="$E2E_ALREADY_PASSED_TIME">
    $(cat stackgres-k8s/ci/test/target/already-passed-e2e-tests-junit-report.results.xml)
  </testsuite>
</testsuites>
EOF
