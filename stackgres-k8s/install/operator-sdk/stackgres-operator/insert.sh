#!/bin/sh
# Insert an already published bundle into the file-based catalog of a Red Hat
# catalog repository, in semver position in every channel it belongs to.
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
    >&2 echo "This may mean the bundle PR has not been merged yet, or that Red Hat has not published the bundle image."
    >&2 echo "You will have to wait before inserting the version in the catalog :("
    exit 1
  fi
  BUNDLE_IMAGE_PINNED="$BUNDLE_IMAGE_REPOSITORY@$BUNDLE_IMAGE_DIGEST"

  for CHANNEL in $CHANNELS
  do
    yq -e --arg ch "$CHANNEL" 'any(.entries[]; .schema == "olm.channel" and .name == $ch)' \
      "$TEMPLATE_FILE" > /dev/null || continue

    # The entry the new bundle replaces is the greatest version below it in this
    # channel, and the entry that used to replace that one becomes its successor.
    # Placing it in semver order, rather than appending, is what keeps the channel
    # to a single head.
    CHANNEL_VERSIONS="$(yq -r --arg ch "$CHANNEL" \
      '.entries[] | select(.schema == "olm.channel" and .name == $ch) | .entries[] | .name' \
      "$TEMPLATE_FILE" | sed 's/^.*\.v//')"
    REPLACES_VERSION="$({ printf '%s\n%s\n' "$CHANNEL_VERSIONS" "$STACKGRES_VERSION"; } 2>/dev/null \
      | grep -v '^$' | sort_versions | grep -B 1 -xF "$STACKGRES_VERSION" | grep -vxF "$STACKGRES_VERSION" || true)"

    if [ -n "$REPLACES_VERSION" ]
    then
      REPLACES="$(bundle_name "$REPLACES_VERSION")"
    else
      REPLACES=
    fi

    yq -y --arg ch "$CHANNEL" --arg name "$BUNDLE_NAME" --arg replaces "$REPLACES" '
      (.entries[] | select(.schema == "olm.channel" and .name == $ch) | .entries) |=
        (
          ([{name: $name} + (if $replaces == "" then {} else {replaces: $replaces} end)]) as $new
          # Whatever replaced our predecessor now replaces us instead.
          | map(if .replaces == $replaces and $replaces != "" then .replaces = $name else . end)
          | . + $new
        )
      ' "$TEMPLATE_FILE" > "$TEMPLATE_FILE.new"
    mv "$TEMPLATE_FILE.new" "$TEMPLATE_FILE"
    echo "$CATALOG_NAME/$CHANNEL: added $BUNDLE_NAME${REPLACES:+ replacing $REPLACES}"
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

assert_single_channel_head
render_catalogs

git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/catalog-templates"
git -C "$FORK_GIT_PATH" add "$PWD/$FORK_GIT_PATH/catalogs"/*/"$PROJECT_NAME"
git -C "$FORK_GIT_PATH" status
git -C "$FORK_GIT_PATH" commit -s -m "Add $PROJECT_NAME $STACKGRES_VERSION to FBC"
git -C "$FORK_GIT_PATH" reset --hard HEAD
show_push_and_pr_instructions
