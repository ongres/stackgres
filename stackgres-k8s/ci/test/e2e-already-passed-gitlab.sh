#!/bin/sh

. "$(dirname "$0")/functions.sh"

curl -f -s --header "PRIVATE-TOKEN: $READ_API_TOKEN" \
  "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines" > stackgres-k8s/ci/test/target/pipelines.json
jq '.[].id' stackgres-k8s/ci/test/target/pipelines.json | xargs -I @ -P 16 sh -ec "$(
  get_or_default_script "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines/@/test_report" '{}' "$TEMP_DIR/test_report.@")"
jq '.[].id' stackgres-k8s/ci/test/target/pipelines.json | xargs -I @ -P 16 sh -ec "$(
  get_or_default_script "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines/@/variables" '[]' "$TEMP_DIR/variables.@")"
jq '.[].id' stackgres-k8s/ci/test/target/pipelines.json | xargs -I @ -P 16 sh -ec "
    [ -f '$TEMP_DIR/test_report_with_variables.@' ] || jq -s '.' '$TEMP_DIR/test_report.@' '$TEMP_DIR/variables.@' > '$TEMP_DIR/test_report_with_variables.@'"

if [ "$E2E_DO_ALL_TESTS" = true ]
then
  exit
fi

JAVA_MODULE_HASH="$(jq -r -s "$(cat << EOF
  .[] | select(.test_suites != null)
    | .test_suites[]
    | select(.name == "build").test_cases[]
    | select(.classname == "module type java").name
EOF
      )" "$TEMP_DIR/test_report.$CI_PIPELINE_ID" \
    | tr -d '\n')"
WEB_MODULE_HASH="$(jq -r -s "$(cat << EOF
  .[] | select(.test_suites != null)
    | .test_suites[]
    | select(.name == "build").test_cases[]
    | select(.classname == "module type web").name
EOF
      )" "$TEMP_DIR/test_report.$CI_PIPELINE_ID" \
    | tr -d '\n')"
NATIVE_MODULE_HASH="$(jq -r -s "$(cat << EOF
  .[] | select(.test_suites != null)
    | .test_suites[]
    | select(.name == "build").test_cases[]
    | select(.classname == "module type native").name
EOF
      )" "$TEMP_DIR/test_report.$CI_PIPELINE_ID" \
    | tr -d '\n')"
VARIABLE_PREFIXES='["E2E_", "K8S_", "KIND_", "EXTENSIONS_"]'
VARIABLES="$(jq -c -s "$(cat << EOF
  .[]
    | map(select(.key as \$key | $VARIABLE_PREFIXES | map(\$key | startswith(.)) | any))
    | sort_by(.variable_type + "." + .key)
EOF
      )" "$TEMP_DIR/variables.$CI_PIPELINE_ID")"
[ -n "$JAVA_MODULE_HASH" -a -n "$WEB_MODULE_HASH" -a -n "$NATIVE_MODULE_HASH" ]
jq -r -s "$(cat << EOF
  .[] | select(.[0].test_suites != null)
    | select(.[0].test_suites[] | select(.name == "build").test_cases
      | map(.classname == "module type java" and .name == "$JAVA_MODULE_HASH") | any)
    | select(.[0].test_suites[] | select(.name == "build").test_cases
      | map(($IS_WEB | not) or (.classname == "module type web" and .name == "$WEB_MODULE_HASH")) | any)
    | select(.[0].test_suites[] | select(.name == "build").test_cases
      | map(($IS_NATIVE | not) or (.classname == "module type native" and .name == "$NATIVE_MODULE_HASH")) | any)
    | select((.[1]
      | map(select(.key as \$key | $VARIABLE_PREFIXES | map(\$key | startswith(.)) | any))
      | sort_by(.variable_type + "." + .key)) == $VARIABLES)
    | .[0].test_suites[]
    | select(
        (($IS_NATIVE | not) and ((.name | startswith("e2e tests jvm ")) or (.name | startswith("e2e ex tests jvm "))))
        or (($IS_NATIVE) and ((.name | startswith("e2e tests native ")) or (.name | startswith("e2e ex tests native "))))
      ).test_cases[]
    | select(.status == "success").name
EOF
  )" "$TEMP_DIR"/test_report_with_variables.* \
    | sort | uniq | tr '\n' ' '
