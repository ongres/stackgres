#!/bin/sh
# Leave only the latest version of each minor in every channel of the
# file-based catalog of a Red Hat catalog repository: every other patch, rc,
# beta or alpha of that minor is removed from the catalog and skipped by the
# kept one, and the kept versions are chained by replaces in semver order (see
# prune_channel in common.sh).
#
# insert.sh and remove.sh already leave the catalog that way, but Red Hat's FBC
# auto-release, that adds a version deployed with deploy.sh, can only append an
# entry: run this once its PR is merged to remove the versions the new one
# skips. It also brings to that shape a catalog that predates the policy.

set -e

PROJECT_PATH=../../../../

cd "$(dirname "$0")"

. ./common.sh

require_env

setup_fork_repositories

if ! [ -d "$FORK_GIT_PATH/operators/$PROJECT_NAME/catalog-templates" ]
then
  >&2 echo "Operator $PROJECT_NAME of $UPSTREAM_NAME has no file-based catalog templates"
  exit 1
fi

prune_catalog_templates

if git -C "$FORK_GIT_PATH" diff --quiet -- "operators/$PROJECT_NAME/catalog-templates"
then
  echo "Every channel of $PROJECT_NAME already has only the latest version of each minor, nothing to do"
  exit 0
fi
git -C "$FORK_GIT_PATH" diff -- "operators/$PROJECT_NAME/catalog-templates" | cat

assert_single_channel_head
render_catalogs

git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/ci.yaml"
git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/catalog-templates"
git -C "$FORK_GIT_PATH" add "$PWD/$FORK_GIT_PATH/catalogs"/*/"$PROJECT_NAME"
git -C "$FORK_GIT_PATH" status
git -C "$FORK_GIT_PATH" commit -s -m "Keep only the latest patch of each minor of $PROJECT_NAME in FBC"
git -C "$FORK_GIT_PATH" reset --hard HEAD
show_push_and_pr_instructions
