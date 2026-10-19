#!/bin/sh
# Publish bundles whose image Red Hat already released in the file-based catalog
# of a Red Hat catalog repository, in semver position in every channel they
# belong to. Only the latest version of each minor is kept, so a bundle replaces the latest
# version of the previous minor and takes the place of (skipping) the lower
# versions of its own minor, which are removed from the catalog.
#
# STACKGRES_VERSION is either a version or a range <from>..<to> of the versions
# published in the repository. For each minor of the range only the latest
# version is added, skipping the others: e.g. 1.17.0..1.17.4 adds 1.17.4,
# skipping 1.17.0 to 1.17.3, so that a cluster on any of them is first upgraded
# to 1.17.4 and then to the next minor. A version lower than one of its minor
# already in the catalog is only added to its skips.
#
# Needed when a version can not be auto-released by Red Hat's FBC pipeline,
# which only ever appends an entry to a channel: a version on a minor older than
# the latest one in the catalog (e.g. 1.18.9 released when 1.19 is out) belongs
# in the middle of the update graph, so appending it would leave the channel with
# a second head. deploy.sh detects that case, skips release-config.yaml and
# points here.
#
# Run it only after the bundle PR has been merged and Red Hat has published the
# bundle image: the catalog references bundles by digest, and the digest does not
# exist before then.

set -e

PROJECT_PATH=../../../../

cd "$(dirname "$0")"

. ./common.sh

require_env
require_version publish

setup_fork_repositories

# Print the bundle image of version $2 for the catalog template $1, pinned by
# digest. The catalog pins bundles by digest, and every catalog has its own
# bundle repository, so derive it from the template instead of hardcoding a
# registry.
bundle_image_pinned() {
  BUNDLE_IMAGE_REPOSITORY="$(bundle_image_repository "$1")"
  if [ -z "$BUNDLE_IMAGE_REPOSITORY" ]
  then
    >&2 echo "Could not determine the bundle image repository from $1"
    return 1
  fi
  BUNDLE_IMAGE="$BUNDLE_IMAGE_REPOSITORY:$2"
  BUNDLE_IMAGE_DIGEST="$(docker buildx imagetools inspect "$BUNDLE_IMAGE" \
    | grep '^Digest:' | tr -d ' ' | cut -d : -f 2-)"
  if [ -z "$BUNDLE_IMAGE_DIGEST" ]
  then
    >&2 echo "Digest not found for image $BUNDLE_IMAGE."
    LATEST_PUBLISHED_VERSION="$(published_versions "$BUNDLE_IMAGE_REPOSITORY" | tail -n 1)"
    if [ -n "$LATEST_PUBLISHED_VERSION" ]
    then
      >&2 echo "The latest version published in $BUNDLE_IMAGE_REPOSITORY is $LATEST_PUBLISHED_VERSION."
    fi
    >&2 echo "Merging the bundle PR is not enough: Red Hat publishes the bundle image separately, and the"
    >&2 echo "catalog can only reference it by digest. Check the pipeline of the merged PR, and open a"
    >&2 echo "support case for the certification project if the image never appears."
    >&2 echo "You will have to wait before publishing the version in the catalog :("
    return 1
  fi
  printf '%s@%s' "$BUNDLE_IMAGE_REPOSITORY" "$BUNDLE_IMAGE_DIGEST"
}

FROM_VERSION="${STACKGRES_VERSION%%..*}"
TO_VERSION="${STACKGRES_VERSION##*..}"
if [ "$(printf '%s\n%s\n' "$FROM_VERSION" "$TO_VERSION" | sort_versions | head -n 1)" != "$FROM_VERSION" ]
then
  >&2 echo "Version $FROM_VERSION is greater than version $TO_VERSION"
  exit 1
fi
RANGE_VERSIONS="$(range_versions "$FROM_VERSION" "$TO_VERSION")"
if ! printf '%s\n' "$RANGE_VERSIONS" | grep -qxF "$TO_VERSION"
then
  >&2 echo "Version $TO_VERSION is not a bundle of operators/$PROJECT_NAME in $UPSTREAM_NAME"
  exit 1
fi
echo "Publishing versions: $(echo $RANGE_VERSIONS)"

for TEMPLATE_FILE in "$FORK_GIT_PATH/operators/$PROJECT_NAME"/catalog-templates/*.yaml
do
  CATALOG_NAME="$(basename "$TEMPLATE_FILE" .yaml)"

  for CHANNEL in $(yq -r '.entries[] | select(.schema == "olm.channel") | .name' "$TEMPLATE_FILE")
  do
    BUNDLE_NAMES="$(
      for VERSION in $RANGE_VERSIONS
      do
        # Each version goes only in the catalogs of the OCP versions its own
        # bundle supports, that may be fewer than those of the latest one.
        if version_channels "$VERSION" | tr ' ' '\n' | grep -qxF "$CHANNEL" \
          && bundle_supports_catalog "$VERSION" "$CATALOG_NAME"
        then
          bundle_name "$VERSION"
          echo
        fi
      done)"
    [ -n "$BUNDLE_NAMES" ] || continue

    # Append the bundles: pruning the templates below keeps only the latest
    # version of each minor, skipping the others, and chains the minors by
    # replaces in semver order. So a cluster on an older patch is first upgraded
    # to the latest patch of its minor, and from there to the next minor.
    yq -y --arg ch "$CHANNEL" --arg names "$BUNDLE_NAMES" '
      (.entries[] | select(.schema == "olm.channel" and .name == $ch) | .entries) |=
        (map(.name) as $present
          | . + ($names | split("\n") | map(select(. != "" and (IN($present[]) | not)) | {name: .})))
      ' "$TEMPLATE_FILE" > "$TEMPLATE_FILE.new"
    mv "$TEMPLATE_FILE.new" "$TEMPLATE_FILE"
  done
done

prune_catalog_templates

if git -C "$FORK_GIT_PATH" diff --quiet -- "operators/$PROJECT_NAME/catalog-templates"
then
  >&2 echo "Versions $STACKGRES_VERSION are already in the catalog or skipped by a version in it, nothing to publish"
  exit 1
fi

for TEMPLATE_FILE in "$FORK_GIT_PATH/operators/$PROJECT_NAME"/catalog-templates/*.yaml
do
  CATALOG_NAME="$(basename "$TEMPLATE_FILE" .yaml)"

  # Bundles of the range that are now an entry of a channel need their image,
  # while those only in skips do not. Bundles already in the catalog are known
  # to the catalog rendered from the template.
  RENDERED_NAMES="$(yq -r 'select(.schema == "olm.bundle") | .name' \
    "$FORK_GIT_PATH/catalogs/$CATALOG_NAME/$PROJECT_NAME/catalog.yaml" 2>/dev/null || true)"
  for BUNDLE_NAME in $(yq -r '[.entries[] | select(.schema == "olm.channel") | .entries[].name] | unique[]' "$TEMPLATE_FILE")
  do
    ! printf '%s\n' "$RENDERED_NAMES" | grep -qxF "$BUNDLE_NAME" || continue
    BUNDLE_IMAGE_PINNED="$(bundle_image_pinned "$TEMPLATE_FILE" "${BUNDLE_NAME#*.v}")"
    yq -y --arg image "$BUNDLE_IMAGE_PINNED" '
      .entries |= (. + [{schema: "olm.bundle", image: $image}])
      ' "$TEMPLATE_FILE" > "$TEMPLATE_FILE.new"
    mv "$TEMPLATE_FILE.new" "$TEMPLATE_FILE"
    echo "$CATALOG_NAME: added $BUNDLE_NAME with bundle image $BUNDLE_IMAGE_PINNED"
  done

  for BUNDLE_NAME in $(for VERSION in $RANGE_VERSIONS; do bundle_name "$VERSION"; echo; done)
  do
    yq -r --arg name "$BUNDLE_NAME" '
      .entries[] | select(.schema == "olm.channel")
      | .name as $ch | .entries[] | select(any((.skips // [])[]; . == $name))
      | "\($ch): \($name) is skipped by \(.name)"
      ' "$TEMPLATE_FILE" | sed "s#^#$CATALOG_NAME/#"
  done
done

assert_single_channel_head
render_catalogs

git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/ci.yaml"
git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/catalog-templates"
git -C "$FORK_GIT_PATH" add "$PWD/$FORK_GIT_PATH/catalogs"/*/"$PROJECT_NAME"
git -C "$FORK_GIT_PATH" status
git -C "$FORK_GIT_PATH" commit -s -m "Add $PROJECT_NAME $STACKGRES_VERSION to FBC"
git -C "$FORK_GIT_PATH" reset --hard HEAD
show_push_and_pr_instructions
