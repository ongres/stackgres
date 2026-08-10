#!/bin/bash

PROJECT_PATH=../../../../
TARGET_PATH=target

cd "$(dirname "$0")"

# Container engine used to build and push the catalog image. Used as a command
# prefix, so values with arguments like `podman --remote` are supported.
: "${CONTAINER_ENGINE:=docker}"

mkdir -p "$TARGET_PATH"

STACKGRES_ALL_VERSIONS="$($CONTAINER_ENGINE run --rm docker.io/regclient/regctl tag ls quay.io/stackgres/operator-bundle)"
STACKGRES_VERSIONS="$({ printf '%s\n' "$STACKGRES_ALL_VERSIONS"; } 2>/dev/null \
  | grep '^1\.[0-9]\+\.[0-9]\+$' | sort -V)"
STACKGRES_RC_VERSIONS="$({ printf '%s\n' "$STACKGRES_ALL_VERSIONS"; } 2>/dev/null \
  | grep '^1\.[0-9]\+\.[0-9]\+-rc[0-9]\+$' | sort -V)"
STACKGRES_WITH_RC_VERSIONS="$({ printf '%s\n' "$STACKGRES_VERSIONS"; } 2>/dev/null \
  | {
    PREVIOUS_BUNDLE_VERSION=
    while read BUNDLE_VERSION
    do
      if [ "${PREVIOUS_BUNDLE_VERSION%.*}" != "${BUNDLE_VERSION%.*}" ]
      then
        { printf '%s\n' "$STACKGRES_RC_VERSIONS"; } 2>/dev/null | grep "^${BUNDLE_VERSION%.*}\.[0-9]\+-rc"
      fi
      echo "$BUNDLE_VERSION"
      PREVIOUS_BUNDLE_VERSION="$BUNDLE_VERSION"
    done
    # Append release candidates for an upcoming minor that has no stable
    # release yet (e.g. 1.19.0-rc1 before 1.19.0 exists), so the candidate
    # channel can still offer the latest pre-release.
    LATEST_STABLE_MINOR="${PREVIOUS_BUNDLE_VERSION%.*}"
    { printf '%s\n' "$STACKGRES_RC_VERSIONS"; } 2>/dev/null \
      | while read RC_VERSION
        do
          if [ "${RC_VERSION%.*}" != "$LATEST_STABLE_MINOR" ] \
            && [ "$(printf '%s\n%s\n' "${RC_VERSION%.*}" "$LATEST_STABLE_MINOR" \
              | sort -V | tail -n 1)" = "${RC_VERSION%.*}" ]
          then
            echo "$RC_VERSION"
          fi
        done
    })"
LATEST_STACKGRES_VERSION="$({ printf '%s\n' "$STACKGRES_VERSIONS"; } 2>/dev/null | tail -n 1)"
LATEST_STACKGRES_RC_VERSION="$({ printf '%s\n' "$STACKGRES_RC_VERSIONS"; } 2>/dev/null | tail -n 1)"
if [ "$(printf '%s\n%s\n' "$LATEST_STACKGRES_VERSION" "${LATEST_STACKGRES_RC_VERSION%-rc*}" | sort -V | tail -n 1)" = "${LATEST_STACKGRES_RC_VERSION%-rc*}" ]
then
  LATEST_STACKGRES_VERSION="$LATEST_STACKGRES_RC_VERSION"
fi

# Print the olm.template.basic for the operator (package + channels + bundles).
# This is the single source of truth for the version graph; both the test
# catalog (build_catalog) and the submitted catalog (deploy.sh DO_ADD_FBC)
# render from it. Customizable via the environment:
#   PACKAGE_NAME       olm.package name           (default: stackgres)
#   BUNDLE_NAME_PREFIX CSV name prefix in channels (default: stackgres)
#   DEFAULT_CHANNEL    package default channel     (default: stable)
#   ICON               JSON icon object, optional  (e.g. yq -c '.spec.icon[0]' csv)
build_template() {
  BUNDLE_TYPE="${1:-}"
  PACKAGE_NAME="${PACKAGE_NAME:-stackgres}"
  BUNDLE_NAME_PREFIX="${BUNDLE_NAME_PREFIX:-stackgres}"
  DEFAULT_CHANNEL="${DEFAULT_CHANNEL:-stable}"
  TEMPLATE_PATH="$TARGET_PATH/operator-template-$PACKAGE_NAME"
  rm -rf "$TEMPLATE_PATH"
  mkdir -p "$TEMPLATE_PATH"
  get_channels | yq -s -c . > "$TEMPLATE_PATH/channels.yaml"
  get_bundles | yq -c . > "$TEMPLATE_PATH/bundles.yaml"
  {
    printf 'schema: olm.template.basic\n'
    printf 'entries:\n'
    printf '  - schema: olm.package\n'
    printf '    name: %s\n' "$PACKAGE_NAME"
    printf '    defaultChannel: %s\n' "$DEFAULT_CHANNEL"
    [ -z "${ICON:-}" ] || printf '    icon: %s\n' "$ICON"
  } > "$TEMPLATE_PATH/header.yaml"
  yq -s -y '.[1] as $channels | .[2] as $bundles | .[0] | .entries = .entries + $channels + $bundles' \
    "$TEMPLATE_PATH/header.yaml" \
    "$TEMPLATE_PATH/channels.yaml" \
    "$TEMPLATE_PATH/bundles.yaml"
}

build_catalog() {
  BUNDLE_TYPE="${1:-}"
  CATALOG_IMAGE_NAME="quay.io/stackgres/operator-catalog:$LATEST_STACKGRES_VERSION$BUNDLE_TYPE"
  CATALOG_PATH="$TARGET_PATH/operator-catalog-$PACKAGE_NAME"
  TEMPLATE="$(build_template "$BUNDLE_TYPE")"
  rm -rf "$CATALOG_PATH"
  mkdir -p "$CATALOG_PATH/operator-catalog"
  opm generate dockerfile "$CATALOG_PATH/operator-catalog"
  printf '%s\n' "$TEMPLATE" > "$CATALOG_PATH/operator-catalog.template.yaml"
  opm alpha render-template basic "$CATALOG_PATH/operator-catalog.template.yaml" \
    --output=yaml > "$CATALOG_PATH/operator-catalog/operator.yaml"
  opm validate "$CATALOG_PATH/operator-catalog"
  (
  cd "$CATALOG_PATH"
  $CONTAINER_ENGINE build . \
    -f "operator-catalog.Dockerfile" \
    -t "$CATALOG_IMAGE_NAME"
  )
}

get_channels() {
  CATALOG_PATH="$TARGET_PATH/operator-catalog"
  rm -rf "$CATALOG_PATH"
  mkdir -p "$CATALOG_PATH/operator-catalog"
  STACKGRES_ENTRIES="$(get_stable_entries)"
  STACKGRES_WITH_RC_ENTRIES="$(get_candidate_entries)"
  cat << EOF
---
$(get_channel stable "$STACKGRES_ENTRIES")
---
$(get_channel candidate "$STACKGRES_WITH_RC_ENTRIES")
EOF
}

get_bundles() {
  BUNDLE_TYPE="${BUNDLE_TYPE:-}"
  CATALOG_PATH="$TARGET_PATH/operator-catalog"
  rm -rf "$CATALOG_PATH"
  mkdir -p "$CATALOG_PATH/operator-catalog"
  STACKGRES_WITH_RC_ENTRIES="$(get_candidate_entries)"
  { printf '%s\n' "$STACKGRES_WITH_RC_ENTRIES"; } 2>/dev/null > "$CATALOG_PATH/all-entries"
  while read BUNDLE_VERSION BUNDLE_REPLACES BUNDLE_SKIPS
  do
    BUNDLE_IMAGE_NAME="quay.io/stackgres/operator-bundle:$BUNDLE_VERSION$BUNDLE_TYPE"
    cat << EOF
- image: ${BUNDLE_IMAGE_NAME}
  schema: olm.bundle
EOF
  done < "$CATALOG_PATH/all-entries"
}

get_stable_entries() {
  get_entries "$STACKGRES_VERSIONS"
}

get_candidate_entries() {
  get_entries "$STACKGRES_WITH_RC_VERSIONS"
}

get_entries() {
  local VERSIONS="$1"
  PREVIOUS_BUNDLE_VERSION=
  for BUNDLE_VERSION in $VERSIONS
  do
    if [ "${PREVIOUS_BUNDLE_VERSION%.*}" != "${BUNDLE_VERSION%.*}" ]
    then
      SKIPS_BUNDLE_VERSIONS=
      PREVIOUS_MINOR_BUNDLE_VERSION="$PREVIOUS_BUNDLE_VERSION"
      LAST_BUNDLE_VERSION="$({ printf '%s\n' "$VERSIONS"; } 2>/dev/null | tr ' ' '\n' | grep "^${BUNDLE_VERSION%.*}\." | tail -n 1)"
    else
      SKIPS_BUNDLE_VERSIONS="$SKIPS_BUNDLE_VERSIONS $PREVIOUS_BUNDLE_VERSION"
    fi
    if [ "$BUNDLE_VERSION" != "$LAST_BUNDLE_VERSION" ]
    then
      PREVIOUS_BUNDLE_VERSION="$BUNDLE_VERSION"
      continue
    fi
    if [ -z "$PREVIOUS_BUNDLE_VERSION" ]
    then
      echo "$BUNDLE_VERSION"
    else
      REPLACES_BUNDLE_VERSION="$PREVIOUS_MINOR_BUNDLE_VERSION"
      echo "$BUNDLE_VERSION $REPLACES_BUNDLE_VERSION $SKIPS_BUNDLE_VERSIONS"
    fi
    PREVIOUS_BUNDLE_VERSION="$BUNDLE_VERSION"
  done
}

get_channel() {
  local NAME="$1"
  local ENTRIES="$2"
  { printf '%s\n' "$ENTRIES"; } 2>/dev/null > "$CATALOG_PATH/$NAME-entries"
  cat << EOF
schema: olm.channel
package: ${PACKAGE_NAME:-stackgres}
name: $NAME
entries:
$(
  while read BUNDLE_ENTRY
  do
    cat << INNER_EOF
$(get_channel_entry $BUNDLE_ENTRY)
INNER_EOF
  done < "$CATALOG_PATH/$NAME-entries"
)
EOF
}

get_channel_entry() {
  local BUNDLE_VERSION="$1"
  if [ "$#" -gt 1 ]
  then
    local BUNDLE_REPLACES_VERSION="$2"
    shift 2
    local BUNDLE_SKIPS_VERSIONS="$*"
  else
    local BUNDLE_REPLACES_VERSION=
    local BUNDLE_SKIPS_VERSIONS=
  fi
  cat << EOF
  - name: ${BUNDLE_NAME_PREFIX:-stackgres}.v$BUNDLE_VERSION
EOF
  if [ -n "$BUNDLE_REPLACES_VERSION" ]
  then
    cat << EOF
    replaces: ${BUNDLE_NAME_PREFIX:-stackgres}.v$BUNDLE_REPLACES_VERSION
    skips:
$(
    for SKIPS_BUNDLE_VERSION in $BUNDLE_SKIPS_VERSIONS
    do
      cat << INNER_EOF
      - ${BUNDLE_NAME_PREFIX:-stackgres}.v$SKIPS_BUNDLE_VERSION
INNER_EOF
    done
)
EOF
  fi
}

push_catalog() {
  BUNDLE_TYPE="${1:-}"
  CATALOG_IMAGE_NAME="quay.io/stackgres/operator-catalog:$LATEST_STACKGRES_VERSION$BUNDLE_TYPE"
  sh "$PROJECT_PATH"/stackgres-k8s/ci/build/build-functions.sh docker_push "$CATALOG_IMAGE_NAME"
}

create_catalog_source() {
  BUNDLE_TYPE="${1:-}"
  CATALOG_IMAGE_NAME="quay.io/stackgres/operator-catalog:$LATEST_STACKGRES_VERSION$BUNDLE_TYPE"
  cat << EOF | kubectl create -f -
  apiVersion: operators.coreos.com/v1alpha1
  kind: CatalogSource
  metadata:
    name: stackgres-catalog
    namespace: openshift-marketplace
  spec:
    sourceType: grpc
    image: ${CATALOG_IMAGE_NAME}
    displayName: StackGres v$LATEST_STACKGRES_VERSION Catalog
    publisher: OnGres
EOF
}

"$@"
