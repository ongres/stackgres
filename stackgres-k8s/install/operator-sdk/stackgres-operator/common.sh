#!/bin/sh
# Shared helpers for the operator bundle scripts. Sourced by deploy.sh,
# insert.sh and remove.sh, each of which is in turn sourced by a per-catalog
# entrypoint (deploy-to-*.sh, insert-to-*.sh, remove-from-*.sh) that sets
# UPSTREAM_NAME, UPSTREAM_GIT_URL, FORK_GIT_URL, PROJECT_NAME and friends.

require_env() {
  for REQUIRED_ENV_VAR in UPSTREAM_NAME UPSTREAM_GIT_URL FORK_GIT_URL PROJECT_NAME
  do
    if [ -z "$(eval "printf %s \"\$$REQUIRED_ENV_VAR\"")" ]
    then
      >&2 echo "Must set $REQUIRED_ENV_VAR env var"
      exit 1
    fi
  done
}

# The version to operate on is never inferred from the checked out tree: a
# release always targets a version that is known up-front, and guessing it
# silently operates on the wrong one.
require_version() {
  if [ -z "$STACKGRES_VERSION" ]
  then
    >&2 echo "Must set STACKGRES_VERSION env var to the version to $1, e.g.:"
    >&2 echo
    >&2 echo "  STACKGRES_VERSION=1.19.2 sh $(basename "$0")"
    exit 1
  fi
}

# Clone (or reset) the upstream catalog repository and our fork of it, leaving
# both at upstream/main, and export the paths the callers work with.
setup_fork_repositories() {
  mkdir -p target
  UPSTREAM_SUFFIX="$(printf %s "$UPSTREAM_NAME" | tr '[A-Z] ' '[a-z]-' | tr -dc '[a-z0-9]-')"
  UPSTREAM_GIT_PATH=target/"upstream-$UPSTREAM_SUFFIX"
  FORK_GIT_PATH=target/"fork-$UPSTREAM_SUFFIX"

  if ! [ -d "$UPSTREAM_GIT_PATH" ] || ! git -C "$UPSTREAM_GIT_PATH" remote -v | tr -s '[:blank:]' ' ' | grep -qF "origin $UPSTREAM_GIT_URL "
  then
    echo "Cloning Upstream $UPSTREAM_NAME from $UPSTREAM_GIT_URL"
    rm -rf "$UPSTREAM_GIT_PATH"
    git clone "$UPSTREAM_GIT_URL" "$UPSTREAM_GIT_PATH"
  fi

  echo "Resetting Upstream $UPSTREAM_NAME from $UPSTREAM_GIT_URL"
  git -C "$UPSTREAM_GIT_PATH" fetch
  git -C "$UPSTREAM_GIT_PATH" reset --hard HEAD
  git -C "$UPSTREAM_GIT_PATH" checkout main
  git -C "$UPSTREAM_GIT_PATH" reset --hard origin/main
  git -C "$UPSTREAM_GIT_PATH" stash save --keep-index --include-untracked
  git -C "$UPSTREAM_GIT_PATH" stash drop || true

  if ! [ -d "$FORK_GIT_PATH" ] || ! git -C "$FORK_GIT_PATH" remote -v | tr -s '[:blank:]' ' ' | grep -qF "origin $FORK_GIT_URL "
  then
    echo "Cloning OperatorHub fork for StackGres from $FORK_GIT_URL"
    rm -rf "$FORK_GIT_PATH"
    git clone "$FORK_GIT_URL" "$FORK_GIT_PATH"
  fi

  echo "Resetting OperatorHub fork for StackGres from $FORK_GIT_URL"
  if ! git -C "$FORK_GIT_PATH" remote -v | tr -s '[:blank:]' ' ' | grep -qF "upstream $UPSTREAM_GIT_URL "
  then
    git -C "$FORK_GIT_PATH" remote add upstream "$UPSTREAM_GIT_URL"
  fi
  git -C "$FORK_GIT_PATH" fetch upstream
  git -C "$FORK_GIT_PATH" reset --hard HEAD
  git -C "$FORK_GIT_PATH" checkout main
  git -C "$FORK_GIT_PATH" reset --hard upstream/main
  git -C "$FORK_GIT_PATH" stash save --keep-index --include-untracked
  git -C "$FORK_GIT_PATH" stash drop || true

  if [ "$(git -C "$FORK_GIT_PATH" rev-list --max-parents=0 HEAD)" != "$(git -C "$UPSTREAM_GIT_PATH" rev-list --max-parents=0 HEAD)" ]
  then
    >&2 echo "Git repository $FORK_GIT_URL seems not a fork of $UPSTREAM_GIT_URL"
    exit 1
  fi
}

show_push_and_pr_instructions() {
  echo
  echo "To push use the following command"
  echo
  echo git -C "$PROJECT_PATH"/stackgres-k8s/install/operator-sdk/stackgres-operator/"$FORK_GIT_PATH" push -f
  echo
  if [ "$UPSTREAM_GIT_URL" != "${UPSTREAM_GIT_URL#https://github.com}" ]
  then
    if [ "$FORK_GIT_URL" != "${FORK_GIT_URL#https://github.com}" ]
    then
      echo "To create the PR go to: $UPSTREAM_GIT_URL/compare/main...$(printf %s "$FORK_GIT_URL" | cut -d / -f 4):$(printf %s "$FORK_GIT_URL" | cut -d / -f 5):main?expand=1"
    fi
    if [ "$FORK_GIT_URL" != "${FORK_GIT_URL#git@github.com}" ]
    then
      echo "To create the PR go to: $UPSTREAM_GIT_URL/compare/main...$(printf %s "$FORK_GIT_URL" | cut -d / -f 1 | cut -d : -f 2):$(printf %s "$FORK_GIT_URL" | cut -d / -f 2 | cut -d . -f 1):main?expand=1"
    fi
  fi
}

# The OCP catalogs the bundle declares support for, as "v4.12 v4.13 ...".
catalog_names() {
  yq -r \
    '.annotations["com.redhat.openshift.versions"] / "-" | map(sub("^v4\\.";"")|tonumber)|[range(.[0];.[1]+1)]|map("v4." + (.|tostring))|.[]' \
    openshift-operator-bundle/metadata/annotations.yaml
}

# Path of the catalog template for an OCP catalog name, empty when onboarding
# did not produce one for that OCP version.
catalog_template() {
  TEMPLATE_FILE="$FORK_GIT_PATH/operators/$PROJECT_NAME/catalog-templates/$1.yaml"
  [ -f "$TEMPLATE_FILE" ] || return 1
  printf %s "$TEMPLATE_FILE"
}

# Channels a version belongs to, by the pre-release types each one admits:
#   stable    -> GA only
#   candidate -> GA + rc
#   fast      -> GA + rc + alpha + beta
# A version is added to every channel whose regexp it matches.
version_channels() {
  sh channels.sh "$1" \
    'stable:^[0-9]\+\.[0-9]\+\.[0-9]\+$' \
    'fast:^[0-9]\+\.[0-9]\+\.[0-9]\+\(-\(alpha\|beta\|rc\)[0-9.]*\)\?$' \
    'candidate:^[0-9]\+\.[0-9]\+\.[0-9]\+\(-rc[0-9.]*\)\?$' \
    | tr ',' ' '
}

# Prefix of the bundle names used in the channels of this catalog.
bundle_name_prefix() {
  [ "$RENAME_CSV" = true ] && printf %s "$PROJECT_NAME" || printf stackgres
}

bundle_name() {
  printf '%s.v%s' "$(bundle_name_prefix)" "$1"
}

# Repository the bundle images of a catalog template are pulled from, derived
# from the template itself so that no registry has to be hardcoded per catalog.
bundle_image_repository() {
  yq -r 'first(.entries[] | select(.schema == "olm.bundle") | .image)' "$1" \
    | cut -d @ -f 1
}

# Versions published in a bundle image repository, in ascending order. Red Hat
# tags each bundle both as "<version>" and as "<version>-<build id>", and both
# point at the same image, so only the plain tags are listed. Empty when skopeo
# is not available, since it is only used to improve an error message.
published_versions() {
  command -v skopeo > /dev/null 2>&1 || return 0
  skopeo list-tags "docker://$1" 2>/dev/null \
    | jq -r '.Tags[]?' \
    | grep -E '^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)[0-9.]*)?$' \
    | sort_versions
}

# Print the head bundle of a channel in a catalog template (the entry that no
# other entry replaces or skips). Empty when the channel is absent.
channel_head() {
  yq -r --arg ch "$1" '
    .entries[]
    | select(.schema == "olm.channel" and .name == $ch)
    | .entries as $e
    | (($e | map(.replaces // empty)) + ($e | map(.skips // []) | add // [])) as $referenced
    | (($e | map(.name)) - $referenced)
    | .[0] // empty
    ' "$2"
}

# Print the version a bundle of version $3 replaces in channel $1 of a catalog
# template $2: the greatest version below it in that channel. Empty when there is
# none.
channel_predecessor() {
  CHANNEL_VERSIONS="$(yq -r --arg ch "$1" \
    '.entries[] | select(.schema == "olm.channel" and .name == $ch) | .entries[] | .name' \
    "$2" | sed 's/^.*\.v//')"
  { printf '%s\n%s\n' "$CHANNEL_VERSIONS" "$3"; } 2>/dev/null \
    | grep -v '^$' | sort_versions | grep -B 1 -xF "$3" | grep -vxF "$3" || true
}

# Sort versions read from stdin in ascending semver order. GNU sort -V orders
# 1.19.0 before 1.19.0-rc1, so map '-' to '~', which sorts before everything.
sort_versions() {
  sed 's/-/~/' | sort -V | sed 's/~/-/'
}

# Whether a catalog name is v4.17 or higher, which is where the compact
# "olm.csv.metadata" form replaces "olm.bundle.object". Mirrors
# requires_migrate_level() in the operator-pipelines render_catalogs.sh and
# is_catalog_v4_17_plus() in its add_bundle_to_fbc.py, so that what we commit is
# what those reproduce.
is_catalog_v4_17_plus() {
  CATALOG_MAJOR="$(printf %s "${1#v}" | cut -d . -f 1)"
  CATALOG_MINOR="$(printf %s "${1#v}" | cut -d . -f 2)"
  case "$CATALOG_MAJOR$CATALOG_MINOR" in *[!0-9]*|'') return 1 ;; esac
  [ "$CATALOG_MAJOR" -gt 4 ] || { [ "$CATALOG_MAJOR" -eq 4 ] && [ "$CATALOG_MINOR" -ge 17 ]; }
}

# Greatest minor version present in the catalog templates of this catalog.
latest_catalog_minor() {
  for CATALOG_NAME in $(catalog_names)
  do
    TEMPLATE_FILE="$(catalog_template "$CATALOG_NAME")" || continue
    yq -r '.entries[] | select(.schema == "olm.channel") | .entries[] | .name' "$TEMPLATE_FILE"
  done \
    | sed 's/^.*\.v//' | cut -d . -f 1-2 | sort -u | sort_versions | tail -n 1
}

# True when the version targets a minor older than the latest one already in the
# catalog, e.g. 1.18.9 released when 1.19 is out. Such a version does not become
# a channel head: it has to be inserted in the middle of the update graph, which
# Red Hat's FBC auto-release can not do (it only appends an entry, leaving the
# channel with a second head). A patch on the latest minor, or a new minor, does
# become the head and is fine to auto-release.
is_patch_of_older_minor() {
  VERSION_MINOR="$(printf %s "$1" | cut -d . -f 1-2)"
  LATEST_MINOR="$(latest_catalog_minor)"
  [ -n "$LATEST_MINOR" ] || return 1
  [ "$VERSION_MINOR" != "$LATEST_MINOR" ] \
    && [ "$(printf '%s\n%s\n' "$VERSION_MINOR" "$LATEST_MINOR" | sort_versions | tail -n 1)" = "$LATEST_MINOR" ]
}

# Render the catalogs from the templates and validate them, using the Makefile
# that FBC onboarding installed in the operator directory.
render_catalogs() {
  make -C "$FORK_GIT_PATH/operators/$PROJECT_NAME" catalogs
  make -C "$FORK_GIT_PATH/operators/$PROJECT_NAME" validate-catalogs 2>&1 | tee target/validate-catalogs.log
  if grep -q '❌' target/validate-catalogs.log
  then
    >&2 echo "Catalog validation failed"
    exit 1
  fi
}

# Every channel of every catalog template must have exactly one head, otherwise
# OLM can not decide what to upgrade to and opm rejects the catalog.
assert_single_channel_head() {
  for TEMPLATE_FILE in "$FORK_GIT_PATH/operators/$PROJECT_NAME"/catalog-templates/*.yaml
  do
    [ -f "$TEMPLATE_FILE" ] || continue
    HEADS="$(yq -r '
      .entries[]
      | select(.schema == "olm.channel")
      | .entries as $e
      | (($e | map(.replaces // empty)) + ($e | map(.skips // []) | add // [])) as $referenced
      | [.name, (($e | map(.name)) - $referenced | length | tostring)]
      | join(" ")
      ' "$TEMPLATE_FILE")"
    printf '%s\n' "$HEADS" \
      | while read -r CHANNEL_NAME HEAD_COUNT
        do
          [ "$HEAD_COUNT" = 1 ] && continue
          >&2 echo "Channel $CHANNEL_NAME of $TEMPLATE_FILE has $HEAD_COUNT heads, expected exactly 1"
          exit 1
        done || exit 1
  done
}
