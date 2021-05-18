#!/bin/sh

. "$(dirname "$0")/e2e-gitlab-functions.sh"

sh -ec "$(
  get_or_default_script "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/pipelines/$CI_PIPELINE/variables" '[]' "$TEMP_DIR/variables.@")"

echo "Pipeline variables:"
echo
jq -r '.[] | " - " + .key + "=" + .value' "/tmp/$CI_PROJECT_ID/variables.$CI_PIPELINE_ID"
echo

