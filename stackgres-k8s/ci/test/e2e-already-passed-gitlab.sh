#!/bin/sh

. "$(dirname "$0")/e2e-gitlab-functions.sh"

[ "$IS_WEB" = true ] || [ "$IS_WEB" = false ]
[ "$IS_NATIVE" = true ] || [ "$IS_NATIVE" = false ]

curl -f -s --header "PRIVATE-TOKEN: $READ_API_TOKEN" \
  "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines?per_page=100" \
  > stackgres-k8s/ci/test/target/pipelines.json
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
jq -r '.[].id' stackgres-k8s/ci/test/target/pipelines.json \
  | xargs -I @ -P 16 sh -ec "$(
    get_or_default_script "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines/@/test_report" \
      '{}' "$TEMP_DIR/test_report.@"
    )"
jq -r '.[].id' stackgres-k8s/ci/test/target/pipelines.json \
  | xargs -I @ -P 16 sh -ec "$(
    get_or_default_script "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines/@/variables" \
      '[]' "$TEMP_DIR/variables.@"
    )"
jq -r '.[].id' stackgres-k8s/ci/test/target/pipelines.json | xargs -I @ -P 16 sh -ec "
    [ -f '$TEMP_DIR/test_report_with_variables.@' ] || jq -s '.' \
      '$TEMP_DIR/test_report.@' '$TEMP_DIR/variables.@' > '$TEMP_DIR/test_report_with_variables.@'"

if [ "$E2E_DO_ALL_TESTS" = true ]
then
  exit
fi

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
jq -r -s "$(cat << EOF
  . as \$in | $TEST_HASHES as \$test_hashes
    | \$in[] | select(.[0].test_suites != null)
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
    | select(
        (($IS_NATIVE | not) and (.name | test("^e2e ([^ ]+ )?tests jvm( |$)")))
        or (($IS_NATIVE) and (.name | test("^e2e ([^ ]+ )?tests native( |$)")))
      ).test_cases[]
    | select(\$test_hashes[.classname] == .name and .status == "success").classname
EOF
  )" "$TEMP_DIR"/test_report_with_variables.* \
    | sort | uniq | tr '\n' ' '
