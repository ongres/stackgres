#!/bin/sh
# Remove a bundle from the file-based catalog of a Red Hat catalog repository.
#
# Used to prune versions that no longer need to be installable, which is what
# keeps the catalogs small: catalogs for OCP below v4.17 must store each bundle
# as "olm.bundle.object", with the CSV and every CRD inline (a few MB per
# bundle), so they grow past GitHub's file size limits over time.
#
# The upgrade graph is kept intact: whatever replaced the removed version now
# replaces its predecessor, and the removed version (plus anything it skipped)
# is added to the skips of its successor, so a cluster running a removed version
# is still offered an upgrade. Skips pointing at bundles that are no longer in
# the catalog are valid and used upstream as well.

set -e

PROJECT_PATH=../../../../

cd "$(dirname "$0")"

. ./common.sh

require_env
require_version remove

setup_fork_repositories

BUNDLE_NAME="$(bundle_name "$STACKGRES_VERSION")"
echo "Removing $BUNDLE_NAME"

REMOVED_FROM_ANY_TEMPLATE=false
for CATALOG_NAME in $(catalog_names)
do
  TEMPLATE_FILE="$(catalog_template "$CATALOG_NAME")" || continue

  if ! yq -e --arg name "$BUNDLE_NAME" \
    'any(.entries[]; .schema == "olm.channel" and any(.entries[]; .name == $name))' \
    "$TEMPLATE_FILE" > /dev/null
  then
    echo "$CATALOG_NAME: $BUNDLE_NAME is not present, skipping"
    continue
  fi

  # Refuse to leave a channel empty: a channel with no entries has no head and
  # the catalog would not validate.
  if yq -e --arg name "$BUNDLE_NAME" \
    'any(.entries[]; .schema == "olm.channel" and ([.entries[].name] == [$name]))' \
    "$TEMPLATE_FILE" > /dev/null
  then
    >&2 echo "$CATALOG_NAME: $BUNDLE_NAME is the only entry of a channel, removing it would leave the channel empty"
    exit 1
  fi

  yq -y --arg name "$BUNDLE_NAME" '
    (.entries[] | select(.schema == "olm.channel") | .entries) |=
      (
        (map(select(.name == $name)) | first) as $removed
        | if $removed == null then .
          else
            # Reconnect the chain: the successor takes over the removed entry'"'"'s
            # own replaces, and inherits its skips plus the removed version, so
            # nothing that could upgrade before is left stranded.
            map(
              if .replaces == $name then
                (if $removed.replaces == null then del(.replaces)
                 else .replaces = $removed.replaces end)
                | .skips = (((.skips // []) + ($removed.skips // []) + [$name]) | unique)
              else . end
            )
            | map(select(.name != $name))
          end
      )
    ' "$TEMPLATE_FILE" > "$TEMPLATE_FILE.new"
  mv "$TEMPLATE_FILE.new" "$TEMPLATE_FILE"

  # Drop the bundle image reference. The image is matched by its tag digest, so
  # resolve it from the catalog rendered from this template, which records both
  # the bundle name and its image.
  BUNDLE_IMAGE="$(yq -r --arg name "$BUNDLE_NAME" \
    'select(.schema == "olm.bundle" and .name == $name) | .image' \
    "$FORK_GIT_PATH/catalogs/$CATALOG_NAME/$PROJECT_NAME/catalog.yaml" 2>/dev/null | head -n 1)"
  if [ -n "$BUNDLE_IMAGE" ] && [ "$BUNDLE_IMAGE" != null ]
  then
    yq -y --arg image "$BUNDLE_IMAGE" \
      '.entries |= map(select((.schema != "olm.bundle") or (.image != $image)))' \
      "$TEMPLATE_FILE" > "$TEMPLATE_FILE.new"
    mv "$TEMPLATE_FILE.new" "$TEMPLATE_FILE"
    echo "$CATALOG_NAME: removed $BUNDLE_NAME and bundle image $BUNDLE_IMAGE"
  else
    >&2 echo "$CATALOG_NAME: could not resolve the bundle image of $BUNDLE_NAME from the rendered catalog"
    exit 1
  fi
  REMOVED_FROM_ANY_TEMPLATE=true
done

if [ "$REMOVED_FROM_ANY_TEMPLATE" != true ]
then
  >&2 echo "Version $STACKGRES_VERSION was not removed from any catalog template"
  exit 1
fi

assert_single_channel_head
render_catalogs

git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/catalog-templates"
git -C "$FORK_GIT_PATH" add "$PWD/$FORK_GIT_PATH/catalogs"/*/"$PROJECT_NAME"
git -C "$FORK_GIT_PATH" status
git -C "$FORK_GIT_PATH" commit -s -m "Remove $PROJECT_NAME $STACKGRES_VERSION from FBC"
git -C "$FORK_GIT_PATH" reset --hard HEAD
show_push_and_pr_instructions
