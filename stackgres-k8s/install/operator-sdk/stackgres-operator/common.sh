#!/bin/sh
# Shared helpers for the operator bundle scripts. Sourced by deploy.sh,
# publish.sh and remove.sh, each of which is in turn sourced by a per-catalog
# entrypoint (deploy-to-*.sh, publish-to-*.sh, remove-from-*.sh) that sets
# UPSTREAM_NAME, UPSTREAM_GIT_URL, FORK_GIT_URL, PROJECT_NAME and friends.

# jq definitions shared by the filters on catalog templates. Each one applies to
# a bundle name, e.g. stackgres-community.v1.19.0-rc1, or to a bare version.
#
# Every channel of a file-based catalog keeps only the latest version of each
# minor (prune_channel): every other patch, rc, beta or alpha of that minor is
# in the skips of the kept entry, and the kept entries are chained by replaces
# in semver order. A cluster running a version that is no longer in the catalog
# is still upgraded through those skips, and the catalogs stay small, which
# matters for the ones below OCP v4.17 that store each bundle inline.
JQ_DEFS='
def version: sub("^[^.]*\\.v"; "");
def version_key:
  version
  | capture("^(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)(-(?<pre>[a-z]+)(?<pre_number>[0-9.]*))?")
  | [(.major | tonumber), (.minor | tonumber), (.patch | tonumber),
    (if .pre == null then 1 else 0 end), (.pre // ""),
    ((.pre_number // "") | split(".") | map(select(. != "") | tonumber))];
def minor: version | split(".")[0:2] | join(".");
def prune_channel:
  [group_by(.name | minor)[]
    | sort_by(.name | version_key)
    | map(.name) as $names
    | .[-1] as $kept
    # The replaces of the group that leaves the minor, kept by the first entry
    # of the chain, which has no previous minor to replace
    | ([.[].replaces // empty | select(IN($names[]) | not)] | sort_by(version_key) | last) as $outside
    | ([.[:-1][] | .name, (.skips // [])[]] + ($kept.skips // []) | unique - [$kept.name]) as $skips
    | $kept | del(.replaces, .skips)
    | if $outside == null then . else .replaces = $outside end
    | if $skips == [] then . else .skips = ($skips | sort_by(version_key)) end]
  | sort_by(.name | version_key)
  | . as $kept
  | [range(length) as $i | $kept[$i] | if $i > 0 then .replaces = $kept[$i - 1].name else . end];
'

# Fail unless every given env var (by default the ones the scripts that push to
# a fork need) is set.
require_env() {
  [ "$#" -gt 0 ] || set -- UPSTREAM_NAME UPSTREAM_GIT_URL FORK_GIT_URL PROJECT_NAME
  for REQUIRED_ENV_VAR in "$@"
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

# Set the paths of the clones of the upstream catalog repository and of our fork
# of it.
set_repository_paths() {
  UPSTREAM_SUFFIX="$(printf %s "$UPSTREAM_NAME" | tr '[A-Z] ' '[a-z]-' | tr -dc '[a-z0-9]-')"
  UPSTREAM_GIT_PATH=target/"upstream-$UPSTREAM_SUFFIX"
  FORK_GIT_PATH=target/"fork-$UPSTREAM_SUFFIX"
}

# Clone (or reset) the upstream catalog repository, leaving it at origin/main,
# and export the paths the callers work with.
setup_upstream_repository() {
  mkdir -p target
  set_repository_paths

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
}

# Clone (or reset) the upstream catalog repository and our fork of it, leaving
# both at upstream/main.
setup_fork_repositories() {
  setup_upstream_repository

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

  restore_fbc_ci_yaml
}

# A bundle PR made with a deploy.sh that predates FBC support (e.g. from an older
# release branch) overwrites ci.yaml with the template, dropping the fbc
# catalog_mapping that onboarding and Red Hat's catalog promotions maintain.
# Without it 'make catalogs' renders nothing and fails, so put back the fbc
# section from the latest upstream revision of ci.yaml that still had it. The
# template may also set updateGraph, which the pipeline rejects when FBC is
# enabled ("FBC is enabled, updateGraph cannot be defined."), so drop it.
restore_fbc_ci_yaml() {
  CI_YAML_PATH="operators/$PROJECT_NAME/ci.yaml"
  [ -f "$FORK_GIT_PATH/$CI_YAML_PATH" ] || return 0
  ! yq -e '.fbc' "$FORK_GIT_PATH/$CI_YAML_PATH" > /dev/null || return 0
  for CI_YAML_COMMIT in $(git -C "$FORK_GIT_PATH" log --format=%H HEAD -- "$CI_YAML_PATH")
  do
    git -C "$FORK_GIT_PATH" show "$CI_YAML_COMMIT:$CI_YAML_PATH" > target/ci-with-fbc.yaml 2>/dev/null || continue
    yq -e '.fbc' target/ci-with-fbc.yaml > /dev/null || continue
    {
      echo ---
      yq -y -s '.[0] + {fbc: .[1].fbc} | del(.updateGraph)' "$FORK_GIT_PATH/$CI_YAML_PATH" target/ci-with-fbc.yaml
    } > "$FORK_GIT_PATH/$CI_YAML_PATH.new"
    mv "$FORK_GIT_PATH/$CI_YAML_PATH.new" "$FORK_GIT_PATH/$CI_YAML_PATH"
    echo "Restored the fbc section of $CI_YAML_PATH from commit $CI_YAML_COMMIT"
    return 0
  done
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

# Whether the bundle of version $1 in the fork repository supports the OCP
# catalog $2 (e.g. v4.16), by the OCP versions its annotations declare: a range
# "v4.9-v4.13", an exact "=v4.13" or a minimum "v4.13". A bundle declaring none
# supports every catalog.
bundle_supports_catalog() {
  yq -e --arg catalog "$2" '
    def ocp: ltrimstr("=") | ltrimstr("v") | split(".") | map(tonumber);
    (.annotations["com.redhat.openshift.versions"] // "") as $versions
    | ($catalog | ocp) as $c
    | if $versions == "" then true
      elif ($versions | test("-")) then ($versions | split("-") | map(ocp)) as $r | $c >= $r[0] and $c <= $r[1]
      elif ($versions | startswith("=")) then $c == ($versions | ocp)
      else $c >= ($versions | ocp) end
    ' "$FORK_GIT_PATH/operators/$PROJECT_NAME/$1/metadata/annotations.yaml" > /dev/null
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
# template $2: the greatest version of an older minor in that channel, since the
# lower versions of its own minor are skipped instead. Empty when there is none.
channel_predecessor() {
  yq -r --arg ch "$1" --arg version "$3" "$JQ_DEFS"'
    [.entries[] | select(.schema == "olm.channel" and .name == $ch) | .entries[].name
      | select(minor != ($version | minor) and version_key < ($version | version_key))]
    | sort_by(version_key) | last // empty | version
    ' "$2"
}

# Print the bundles a bundle of version $3 skips in channel $1 of a catalog
# template $2: the lower versions of its own minor in that channel, and whatever
# they skip. Empty when there is none.
channel_skips() {
  yq -r --arg ch "$1" --arg version "$3" "$JQ_DEFS"'
    [.entries[] | select(.schema == "olm.channel" and .name == $ch) | .entries[]
      | select((.name | minor) == ($version | minor) and (.name | version_key) < ($version | version_key))
      | .name, (.skips // [])[]]
    | unique | sort_by(version_key)[]
    ' "$2"
}

# Keep only the latest version of each minor in every channel of every catalog
# template (see prune_channel), and drop the bundle images that are then in no
# channel. Templates list bundles by image only, so the name of each image is
# taken from the catalog rendered from the template: a bundle that was just
# added, and is not rendered yet, is never dropped.
prune_catalog_templates() {
  for TEMPLATE_FILE in "$FORK_GIT_PATH/operators/$PROJECT_NAME"/catalog-templates/*.yaml
  do
    [ -f "$TEMPLATE_FILE" ] || continue
    RENDERED_BUNDLES="$(yq -c 'select(.schema == "olm.bundle") | {name, image}'       "$FORK_GIT_PATH/catalogs/$(basename "$TEMPLATE_FILE" .yaml)/$PROJECT_NAME/catalog.yaml" 2>/dev/null       | jq -s .)"
    yq -y --argjson rendered "${RENDERED_BUNDLES:-[]}" "$JQ_DEFS"'
      (.entries[] | select(.schema == "olm.channel") | .entries) |= prune_channel
      | ([.entries[] | select(.schema == "olm.channel") | .entries[].name] | unique) as $names
      | ($rendered | map(select(.name | IN($names[]) | not) | .image)) as $dropped
      | .entries |= map(select(.schema != "olm.bundle" or (.image | IN($dropped[]) | not)))
      ' "$TEMPLATE_FILE" > "$TEMPLATE_FILE.new"
    # Rewriting the template reformats it, so leave it alone when nothing changed
    if [ "$(yq -S -c . "$TEMPLATE_FILE.new")" = "$(yq -S -c . "$TEMPLATE_FILE")" ]
    then
      rm "$TEMPLATE_FILE.new"
    else
      mv "$TEMPLATE_FILE.new" "$TEMPLATE_FILE"
    fi
  done
}

# Print the bundle versions of channel $1 that are in the fork repository but not
# yet in the catalog template $2: those above the channel head and below version
# $3. Empty when the channel is absent.
pending_versions() {
  PENDING_HEAD_VERSION="$(channel_head "$1" "$2" | sed 's/^.*\.v//')"
  [ -n "$PENDING_HEAD_VERSION" ] || return 0
  ls -1d "$FORK_GIT_PATH/operators/$PROJECT_NAME"/*/manifests \
    | cut -d / -f 5 | { cat; printf '%s\n%s\n' "$PENDING_HEAD_VERSION" "$3"; } \
    | sort_versions | uniq \
    | sed -n "/^$(printf %s "$PENDING_HEAD_VERSION" | sed 's/\./\\./g')\$/,/^$(printf %s "$3" | sed 's/\./\\./g')\$/p" \
    | grep -vxF -e "$PENDING_HEAD_VERSION" -e "$3" \
    | while read -r PENDING_VERSION
      do
        if version_channels "$PENDING_VERSION" | tr ' ' '\n' | grep -qxF "$1"
        then
          printf '%s\n' "$PENDING_VERSION"
        fi
      done
}

# Print the bundle versions in the fork repository from version $1 to version $2,
# both included, in ascending semver order.
range_versions() {
  PUBLISHED_VERSIONS="$(ls -1d "$FORK_GIT_PATH/operators/$PROJECT_NAME"/*/manifests \
    | sed 's#^.*/\([^/]*\)/manifests$#\1#')"
  { printf '%s\n%s\n%s\n' "$PUBLISHED_VERSIONS" "$1" "$2"; } \
    | sort_versions | uniq \
    | sed -n "/^$(printf %s "$1" | sed 's/\./\\./g')\$/,/^$(printf %s "$2" | sed 's/\./\\./g')\$/p" \
    | grep -xF "$PUBLISHED_VERSIONS" || true
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
# Upstream render_catalogs.sh exits with the status of its last
# '[[ ${#catalogs_with_migrate[@]} -gt 0 ]] && ...', so it fails after rendering
# everything when the last catalog_mapping of ci.yaml has no catalog >= v4.17
# (e.g. v4.16 appended last by Red Hat's catalog promotions). So when it fails,
# whether it really did is told by its error output and the rendered catalogs.
render_catalogs() {
  RENDER_CATALOGS_EXIT_CODE=0
  make -C "$FORK_GIT_PATH/operators/$PROJECT_NAME" catalogs > target/render-catalogs.log 2>&1 \
    || RENDER_CATALOGS_EXIT_CODE="$?"
  cat target/render-catalogs.log
  if [ "$RENDER_CATALOGS_EXIT_CODE" != 0 ]
  then
    if grep -q '❌\|Unknown type' target/render-catalogs.log
    then
      >&2 echo "Catalog rendering failed"
      exit 1
    fi
    for CATALOG_NAME in $(yq -r '.fbc.catalog_mapping[].catalog_names[]' \
      "$FORK_GIT_PATH/operators/$PROJECT_NAME/ci.yaml")
    do
      if ! [ -s "$FORK_GIT_PATH/catalogs/$CATALOG_NAME/$PROJECT_NAME/catalog.yaml" ]
      then
        >&2 echo "Catalog $CATALOG_NAME was not rendered"
        exit 1
      fi
    done
  fi
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
