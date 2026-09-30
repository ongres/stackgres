#!/bin/sh

UPSTREAM_NAME="Red Hat Certified"
UPSTREAM_GIT_URL="https://github.com/redhat-openshift-ecosystem/certified-operators"
FORK_GIT_URL="${FORK_GIT_URL:-$1}"
PROJECT_NAME="stackgres-certified"

. "$(dirname "$0")/remove.sh"
