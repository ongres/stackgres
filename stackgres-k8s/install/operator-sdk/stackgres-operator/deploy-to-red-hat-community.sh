#!/bin/sh

UPSTREAM_NAME="Red Hat Community"
UPSTREAM_GIT_URL="https://github.com/redhat-openshift-ecosystem/community-operators-prod"
FORK_GIT_URL="${FORK_GIT_URL:-$1}"
PROJECT_NAME="stackgres-community"
RENAME_CSV=true
DO_PIN_IMAGES=true
OPERATOR_BUNDLE_IMAGE_TAG_SUFFIX=-openshift

. "$(dirname "$0")/deploy.sh"
