#!/bin/sh

set -e

die() {
  >&2 echo "\n\t$1\n\n"
  exit 1
}

usage() {
  die "Usage: $0 <version> [<main tag version>]" 1
}

yq_update_file() {
  [ "$#" -ge 2 ]
  local INDEX=0
  local OFFSET
  while [ "$#" -gt 2 ]
  do
    OFFSET="$(tail -n +"$((INDEX+1))" "$(eval "echo \$$#")" | grep -n "$1")"
    OFFSET="${OFFSET%%:*}"
    INDEX="$((INDEX+OFFSET))"
    shift
  done
  [ "$INDEX" -ge 0 ]
  [ "$INDEX" -gt 0 ] || INDEX=1
  cp "$2" "$2.bak"
  (
    head -n "$((INDEX-1))" "$2.bak"
    cat << EOF
$1
EOF
    tail -n "+$((INDEX+1))" "$2.bak"
  ) > "$2"
  rm "$2.bak"
}

[ "$#" -ge 1 ] || usage

command -v jq > /dev/null || die 'The program `jq` is required to be in PATH' 8
command -v yq > /dev/null || die 'The program `yq` (https://kislyuk.github.io/yq/) is required to be in PATH' 16
command -v yamllint > /dev/null || die 'The program `yamllint` is required to be in PATH' 32

cd "$(dirname "$0")/../../.."

VERSION="$1"
MAIN_IMAGE_TAG="${2:-main}"
IMAGE_TAG="${VERSION}"
if [ "${VERSION##*-}" = "SNAPSHOT" ]
then
  IMAGE_TAG="${MAIN_IMAGE_TAG}-jvm"
fi
ADMINUI_IMAGE_TAG="${IMAGE_TAG%-jvm}"

echo
echo "Setting Java moduve version to $VERSION"

(
cd stackgres-k8s/src
./mvnw -q versions:set -DgenerateBackupPoms=false -DnewVersion="$VERSION"
)

echo
echo "Setting helm charts images to tag $IMAGE_TAG (tag $ADMINUI_IMAGE_TAG for admin-ui)"

yq_update_file "^operator:$" "^    tag:.*$" "    tag: \"$IMAGE_TAG\"" stackgres-k8s/install/helm/stackgres-operator/values.yaml
yq_update_file "^restapi:$" "^    tag:.*$" "    tag: \"$IMAGE_TAG\"" stackgres-k8s/install/helm/stackgres-operator/values.yaml
yq_update_file "^adminui:$" "^    tag:.*$" "    tag: \"$ADMINUI_IMAGE_TAG\"" stackgres-k8s/install/helm/stackgres-operator/values.yaml
yq_update_file "^jobs:$" "^    tag:.*$" "    tag: \"$IMAGE_TAG\"" stackgres-k8s/install/helm/stackgres-operator/values.yaml

echo
echo "Setting helm charts version to $VERSION"

yq_update_file "^version: .*$" "version: \"$VERSION\"" stackgres-k8s/install/helm/stackgres-operator/Chart.yaml
yq_update_file "^appVersion: .*$" "appVersion: \"$VERSION\"" stackgres-k8s/install/helm/stackgres-operator/Chart.yaml
yq_update_file "^version: .*$" "version: \"$VERSION\"" stackgres-k8s/install/helm/stackgres-cluster/Chart.yaml
yq_update_file "^appVersion: .*$" "appVersion: \"$VERSION\"" stackgres-k8s/install/helm/stackgres-cluster/Chart.yaml

CURRENT_VERSION="$VERSION"
IS_GA="$(printf %s "$CURRENT_VERSION" | grep -q '^[0-9]\+\.[0-9]\+\.[0-9]\+$' && printf true || printf false)"
IS_NEW_MINOR_VERSION="$(printf %s "$CURRENT_VERSION" | grep -q '^[0-9]\+\.[0-9]\+\.0\(-.\+\)\?$' && printf true || printf false)"
IS_SNAPSHOT="$(printf %s "$CURRENT_VERSION" | grep -q '[-]SNAPSHOT$' && printf true || printf false)"
CURRENT_PATCH_VERSION="$("$IS_GA" && printf 0 || git tag | grep "^${NEXT_MINOR_PATCH_VERSION}\." | sort -V | tail -n 1)"
CURRENT_MAJOR_VERSION="$(printf %s "$CURRENT_VERSION" | cut -d . -f 1)"
CURRENT_MINOR_VERSION="$(printf %s "$CURRENT_VERSION" | cut -d . -f 2)"
CURRENT_PATCH_VERSION="$(printf %s "$CURRENT_VERSION" | cut -d - -f 1 | cut -d . -f 3)"

if ! "$IS_SNAPSHOT"
then
  if "$IS_GA"
  then
    VERSION="$CURRENT_MAJOR_VERSION.$CURRENT_MINOR_VERSION.$(( $CURRENT_PATCH_VERSION + 1 ))"

    echo
    echo "Generating new patch release template .gitlab/issue_templates/Patch Release.md for $VERSION"

    sh stackgres-k8s/ci/tools/generate-release-template.sh "$VERSION" > .gitlab/issue_templates/"Patch Release.md"
  fi

  if "$IS_NEW_MINOR_VERSION"
  then
    VERSION="$CURRENT_MAJOR_VERSION.$(( CURRENT_MINOR_VERSION + $("$IS_GA" && printf 1 || printf 0) )).0-beta1"

    echo
    echo "Generating new minor release template .gitlab/issue_templates/Beta Release.md for $VERSION"

    sh stackgres-k8s/ci/tools/generate-release-template.sh "$VERSION" > .gitlab/issue_templates/"Beta Release.md"

    VERSION="$CURRENT_MAJOR_VERSION.$(( CURRENT_MINOR_VERSION + $("$IS_GA" && printf 1 || printf 0) )).0-RC1"

    echo
    echo "Generating new minor release template .gitlab/issue_templates/RC Release.md for $VERSION"

    sh stackgres-k8s/ci/tools/generate-release-template.sh "$VERSION" > .gitlab/issue_templates/"RC Release.md"

    VERSION="$CURRENT_MAJOR_VERSION.$(( CURRENT_MINOR_VERSION + $("$IS_GA" && printf 1 || printf 0) )).0"

    echo
    echo "Generating new minor release template .gitlab/issue_templates/GA Release.md for $VERSION"

    sh stackgres-k8s/ci/tools/generate-release-template.sh "$VERSION" > .gitlab/issue_templates/"GA Release.md"
  fi
fi