#!/bin/sh
# Generate VERSIONS-<repository>.md with a mermaid graph of the update graph of
# every channel published in a catalog repository.
#
# Sourced by a per-repository entrypoint (versions-of-*.sh) that sets
# UPSTREAM_NAME, UPSTREAM_GIT_URL and PROJECT_NAME, e.g.:
#
#   sh versions-of-red-hat-certified.sh
#
# The upstream repository is cloned (or pulled) first, so the graphs always
# reflect what is currently published there:
# * Repositories onboarded to file-based catalogs are read from the catalog
#   templates. The graph of a channel merges those of every OCP catalog, and
#   any version or edge that is not in all of them is labeled with the OCP
#   catalogs it is in.
# * Other repositories are read from the bundles, following the updateGraph
#   mode declared in the operator ci.yaml.

set -e

cd "$(dirname "$0")"

. ./common.sh

require_env UPSTREAM_NAME UPSTREAM_GIT_URL PROJECT_NAME

# Version of a bundle name, e.g. 1.19.0-rc1 for stackgres-community.v1.19.0-rc1
JQ_DEFS='
def version: sub("^[^.]*\\.v"; "");
def version_key:
  version
  | capture("^(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)(-(?<pre>[a-z]+)(?<pre_number>[0-9.]*))?")
  | [(.major | tonumber), (.minor | tonumber), (.patch | tonumber),
    (if .pre == null then 1 else 0 end), (.pre // ""),
    ((.pre_number // "") | split(".") | map(select(. != "") | tonumber))];
'

# Read from stdin the channels of a repository as a list of
# {catalog, name, entries: [{name, replaces, skips}]} (one item per channel of
# every catalog, catalog being "" when there is only one) and print the
# markdown section of each channel with its mermaid graph.
render_channels() {
  jq -r "$JQ_DEFS"'
    def node_id: "v" + (version | gsub("[^0-9A-Za-z]"; "_"));
    def catalog_key: ltrimstr("v") | split(".") | map(tonumber? // 0);
    # Compact a list of catalogs in ranges of consecutive ones, e.g. v4.12-v4.16, v4.18
    def catalog_ranges($all):
      map(. as $c | $all | index($c)) | sort
      | reduce .[] as $i ([];
          if length > 0 and .[-1][1] == $i - 1 then .[-1][1] = $i else . + [[$i, $i]] end)
      | map(if .[0] == .[1] then $all[.[0]] else $all[.[0]] + "-" + $all[.[1]] end)
      | join(", ");
    # Label suffix for something that is only in some of the catalogs of the channel
    def only_in($all):
      if (. | length) == ($all | length) then "" else catalog_ranges($all) end;

    group_by(.name)
    | sort_by(.[0].name | [(["stable", "candidate", "fast"] | index(.)) // 99, .])[]
    | . as $channels
    | ($channels | map(.catalog) | unique | sort_by(catalog_key)) as $all
    | ([$channels[] | .catalog as $catalog | .entries[] | {name, catalog: $catalog}]
      | group_by(.name) | map({name: .[0].name, catalogs: map(.catalog)})) as $nodes
    | ([$channels[] | .catalog as $catalog | .entries[]
        | .name as $to
        | (if .replaces then {from: .replaces, to: $to, kind: "replaces", catalog: $catalog} else empty end),
          ((.skips // [])[] | {from: ., to: $to, kind: "skips", catalog: $catalog})]
      | group_by([.from, .to, .kind])
      | map(.[0] + {catalogs: map(.catalog)} | del(.catalog))) as $edges
    | ([$channels[] | .entries
        | (map(.name) - (map(.replaces // empty) + (map(.skips // []) | add // [])))[]]
      | unique) as $heads
    | ($nodes | map(.name)) as $present
    | ([$edges[].from] | unique - $present) as $removed
    | "## Channel `" + $channels[0].name + "`",
      "",
      "Head: " + ($heads | map("`" + version + "`") | join(", ")),
      "",
      "```mermaid",
      "flowchart TD",
      (($nodes + ($removed | map({name: ., catalogs: $all})))
        | sort_by(.name | version_key)[]
        | (.catalogs | only_in($all)) as $only
        | "  " + (.name | node_id) + "[\"" + (.name | version)
          + (if $only == "" then "" else "<br/>" + $only end) + "\"]"
          + (if (.name | IN($heads[])) then ":::head"
            elif (.name | IN($removed[])) then ":::removed"
            else "" end)),
      ($edges
        | sort_by((.to | version_key), (.from | version_key))[]
        | (.catalogs | only_in($all)) as $only
        | "  " + (.from | node_id)
          + (if .kind == "replaces" then " --> " else " -.-> " end)
          + (if $only == "" then "" else "|\"" + $only + "\"| " end)
          + (.to | node_id)),
      "  classDef head stroke-width:3px",
      "  classDef removed stroke-dasharray:4 4,color:#888",
      "```",
      ""
    '
}

# Channels from the catalog templates of a file-based catalog.
fbc_channels() {
  for TEMPLATE_FILE in "$1"/catalog-templates/*.yaml
  do
    yq -c --arg catalog "$(basename "$TEMPLATE_FILE" .yaml)" \
      '.entries[] | select(.schema == "olm.channel") | {catalog: $catalog, name, entries}' \
      "$TEMPLATE_FILE"
  done | jq -s .
}

# Channels from the bundles. In semver-mode the channels are ordered by version
# and every bundle replaces the previous one, ignoring the CSV replaces and skips.
bundle_channels() {
  UPDATE_GRAPH="$(yq -r '.updateGraph // "replaces-mode"' "$1/ci.yaml" 2>/dev/null || echo replaces-mode)"
  for BUNDLE_PATH in "$1"/*/manifests
  do
    BUNDLE_PATH="${BUNDLE_PATH%/manifests}"
    yq -c '{name: .metadata.name, replaces: .spec.replaces, skips: .spec.skips}' \
      "$BUNDLE_PATH"/manifests/*.clusterserviceversion.yaml \
      | jq -c --arg channels "$(yq -r '.annotations["operators.operatorframework.io.bundle.channels.v1"]' \
          "$BUNDLE_PATH/metadata/annotations.yaml")" \
        '. + {channels: ($channels | split(","))}'
  done \
    | jq -s --arg update_graph "$UPDATE_GRAPH" "$JQ_DEFS"'
      [.[] | . as $bundle | .channels[] | {name: ., bundle: ($bundle | del(.channels))}]
      | group_by(.name)
      | map({catalog: "", name: .[0].name, entries: (
          map(.bundle) | sort_by(.name | version_key)
          | if $update_graph == "semver-mode"
            then . as $entries
              | [range(length) as $i | {name: $entries[$i].name}
                + (if $i > 0 then {replaces: $entries[$i - 1].name} else {} end)]
            else map(with_entries(select(.value != null)))
            end)})
      '
}

setup_upstream_repository
OPERATOR_PATH="$UPSTREAM_GIT_PATH/operators/$PROJECT_NAME"
VERSIONS_FILE="VERSIONS-$UPSTREAM_SUFFIX.md"

if [ -d "$OPERATOR_PATH/catalog-templates" ]
then
  IS_FBC=true
  SOURCE="the file-based catalog templates in \`operators/$PROJECT_NAME/catalog-templates\`"
  DEFAULT_CHANNEL="$(yq -r 'first(.entries[] | select(.schema == "olm.package") | .defaultChannel)' \
    "$(ls -1 "$OPERATOR_PATH"/catalog-templates/*.yaml | head -n 1)")"
  CHANNELS="$(fbc_channels "$OPERATOR_PATH")"
else
  SOURCE="the bundles in \`operators/$PROJECT_NAME\` (update graph \`$(yq -r '.updateGraph // "replaces-mode"' "$OPERATOR_PATH/ci.yaml")\`)"
  DEFAULT_CHANNEL="$(yq -r '.annotations["operators.operatorframework.io.bundle.channel.default.v1"]' \
    "$OPERATOR_PATH/$(ls -1d "$OPERATOR_PATH"/*/manifests | cut -d / -f 5 | sort_versions | tail -n 1)/metadata/annotations.yaml")"
  CHANNELS="$(bundle_channels "$OPERATOR_PATH")"
fi

echo "Generating $VERSIONS_FILE"
{
  echo "# $UPSTREAM_NAME versions"
  echo
  echo "Update graph of each channel of operator \`$PROJECT_NAME\` in [$UPSTREAM_NAME]($UPSTREAM_GIT_URL),"
  echo "generated by \`versions-of-$UPSTREAM_SUFFIX.sh\` from $SOURCE at commit"
  echo "\`$(git -C "$UPSTREAM_GIT_PATH" log -1 --format='%h (%cs)')\`."
  echo
  echo "Default channel: \`$DEFAULT_CHANNEL\`"
  echo
  echo "A solid arrow goes from a version to the one that replaces it, a dotted arrow from a"
  echo "version to one that skips it. The heads of a channel have a thick border, and versions"
  echo "that are only skipped, since they are no longer in the catalog, a dashed one."
  if [ "$IS_FBC" = true ]
  then
    echo "A version or an arrow that is not in every OCP catalog is labeled with the catalogs it is in."
  fi
  echo
  printf '%s\n' "$CHANNELS" | render_channels
} > "$VERSIONS_FILE"
