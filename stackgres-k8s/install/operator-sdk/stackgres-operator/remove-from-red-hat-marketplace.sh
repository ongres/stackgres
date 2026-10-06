#!/bin/sh

UPSTREAM_NAME="Red Hat Marketplace"
UPSTREAM_GIT_URL="https://github.com/redhat-openshift-ecosystem/redhat-marketplace-operators"
FORK_GIT_URL="${FORK_GIT_URL:-$1}"
PROJECT_NAME="stackgres"

. "$(dirname "$0")/remove.sh"
