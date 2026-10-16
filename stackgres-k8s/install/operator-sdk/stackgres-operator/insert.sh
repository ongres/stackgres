#!/bin/sh
# Insert an already published bundle into the file-based catalog of a Red Hat
# catalog repository, in semver position in every channel it belongs to. Only
# the latest version of each minor is kept, so the bundle replaces the latest
# version of the previous minor and takes the place of (skipping) the lower
# versions of its own minor, which are removed from the catalog.
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
require_version insert

setup_fork_repositories

BUNDLE_NAME="$(bundle_name "$STACKGRES_VERSION")"
CHANNELS="$(version_channels "$STACKGRES_VERSION")"
echo "Inserting $BUNDLE_NAME in channels: $CHANNELS"

INSERTED_IN_ANY_TEMPLATE=false
for CATALOG_NAME in $(catalog_names)
do
  # Only touch templates that onboarding actually produced for this OCP version.
  TEMPLATE_FILE="$(catalog_template "$CATALOG_NAME")" || continue

  if yq -e --arg name "$BUNDLE_NAME" \
    'any(.entries[]; .schema == "olm.channel" and any(.entries[]; .name == $name))' \
    "$TEMPLATE_FILE" > /dev/null
  then
    echo "$CATALOG_NAME: $BUNDLE_NAME is already present, skipping"
    continue
  fi

  # The catalog pins bundles by digest, and every catalog has its own bundle
  # repository, so derive it from the template instead of hardcoding a registry.
  BUNDLE_IMAGE_REPOSITORY="$(bundle_image_repository "$TEMPLATE_FILE")"
  if [ -z "$BUNDLE_IMAGE_REPOSITORY" ]
  then
    >&2 echo "Could not determine the bundle image repository from $TEMPLATE_FILE"
    exit 1
  fi
  BUNDLE_IMAGE="$BUNDLE_IMAGE_REPOSITORY:$STACKGRES_VERSION"
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
    >&2 echo "You will have to wait before inserting the version in the catalog :("
    exit 1
  fi
  BUNDLE_IMAGE_PINNED="$BUNDLE_IMAGE_REPOSITORY@$BUNDLE_IMAGE_DIGEST"

  for CHANNEL in $CHANNELS
  do
    yq -e --arg ch "$CHANNEL" 'any(.entries[]; .schema == "olm.channel" and .name == $ch)' \
      "$TEMPLATE_FILE" > /dev/null || continue

    # Only the latest version of each minor is kept in a channel, so the bundle
    # must be the latest of its minor: anything lower is skipped by it once the
    # templates are pruned below.
    NEWER_VERSION="$(yq -r --arg ch "$CHANNEL" --arg version "$STACKGRES_VERSION" "$JQ_DEFS"'
      [.entries[] | select(.schema == "olm.channel" and .name == $ch) | .entries[].name
        | select(minor == ($version | minor) and version_key > ($version | version_key))]
      | sort_by(version_key) | last // empty | version
      ' "$TEMPLATE_FILE")"
    if [ -n "$NEWER_VERSION" ]
    then
      >&2 echo "$CATALOG_NAME/$CHANNEL: $NEWER_VERSION is already in the channel, so $STACKGRES_VERSION would be skipped by it"
      exit 1
    fi

    # Append the bundle: pruning the templates below places it in semver order,
    # makes it skip the lower versions of its minor and re-points its successor,
    # which is what keeps the channel to a single head.
    yq -y --arg ch "$CHANNEL" --arg name "$BUNDLE_NAME" '
      (.entries[] | select(.schema == "olm.channel" and .name == $ch) | .entries) += [{name: $name}]
      ' "$TEMPLATE_FILE" > "$TEMPLATE_FILE.new"
    mv "$TEMPLATE_FILE.new" "$TEMPLATE_FILE"
    echo "$CATALOG_NAME/$CHANNEL: added $BUNDLE_NAME"
  done

  yq -y --arg image "$BUNDLE_IMAGE_PINNED" '
    .entries |= (. + [{schema: "olm.bundle", image: $image}])
    ' "$TEMPLATE_FILE" > "$TEMPLATE_FILE.new"
  mv "$TEMPLATE_FILE.new" "$TEMPLATE_FILE"
  echo "$CATALOG_NAME: added bundle image $BUNDLE_IMAGE_PINNED"
  INSERTED_IN_ANY_TEMPLATE=true
done

if [ "$INSERTED_IN_ANY_TEMPLATE" != true ]
then
  >&2 echo "Version $STACKGRES_VERSION was not inserted in any catalog template"
  exit 1
fi

prune_catalog_templates
assert_single_channel_head
render_catalogs

git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/ci.yaml"
git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/catalog-templates"
git -C "$FORK_GIT_PATH" add "$PWD/$FORK_GIT_PATH/catalogs"/*/"$PROJECT_NAME"
git -C "$FORK_GIT_PATH" status
git -C "$FORK_GIT_PATH" commit -s -m "Add $PROJECT_NAME $STACKGRES_VERSION to FBC"
git -C "$FORK_GIT_PATH" reset --hard HEAD
show_push_and_pr_instructions
