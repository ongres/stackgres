#!/bin/sh

export UPSTREAM_NAME="Red Hat Certified"
export UPSTREAM_GIT_URL="https://github.com/redhat-openshift-ecosystem/certified-operators"
export FORK_GIT_URL="${FORK_GIT_URL:-$1}"
export PROJECT_NAME="stackgres-certified"
export DO_PIN_IMAGES=true
export HELM_OPERATOR_BUNDLE_IMAGE_TAG_SUFFIX=-openshift
exec sh $(printf %s "$-" | grep -q x && printf %s -x) "$(dirname "$0")/deploy.sh"
