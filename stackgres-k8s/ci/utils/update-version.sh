#!/bin/sh

set -e

message_and_exit() {
  echo "\n\t$1\n\n"
  exit $2
}

usage() {
  message_and_exit "Usage: $0 <version> [<development tag version>]" 1
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

command -v jq > /dev/null || message_and_exit 'The program `jq` is required to be in PATH' 8
command -v yq > /dev/null || message_and_exit 'The program `yq` (https://kislyuk.github.io/yq/) is required to be in PATH' 16
command -v yamllint > /dev/null || message_and_exit 'The program `yamllint` is required to be in PATH' 32

cd "$(dirname "$0")/../../.."

VERSION="$1"
DEVELOPMENT_IMAGE_TAG="${2:-development}"
IMAGE_TAG="${VERSION}-jvm"
if [ "${VERSION##*-}" = "SNAPSHOT" ]
then
  IMAGE_TAG="${DEVELOPMENT_IMAGE_TAG}-jvm"
fi
ADMINUI_IMAGE_TAG="${IMAGE_TAG%-jvm}"

(
cd stackgres-k8s/src
./mvnw -q versions:set -DgenerateBackupPoms=false -DnewVersion="$VERSION"
)

yq_update_file "^operator:$" "^    tag:.*$" "    tag: \"$IMAGE_TAG\"" stackgres-k8s/install/helm/stackgres-operator/values.yaml
yq_update_file "^restapi:$" "^    tag:.*$" "    tag: \"$IMAGE_TAG\"" stackgres-k8s/install/helm/stackgres-operator/values.yaml
yq_update_file "^adminui:$" "^    tag:.*$" "    tag: \"$ADMINUI_IMAGE_TAG\"" stackgres-k8s/install/helm/stackgres-operator/values.yaml
yq_update_file "^jobs:$" "^    tag:.*$" "    tag: \"$IMAGE_TAG\"" stackgres-k8s/install/helm/stackgres-operator/values.yaml

yq_update_file "^version: .*$" "version: \"$VERSION\"" stackgres-k8s/install/helm/stackgres-operator/Chart.yaml
yq_update_file "^appVersion: .*$" "appVersion: \"$VERSION\"" stackgres-k8s/install/helm/stackgres-operator/Chart.yaml
yq_update_file "^version: .*$" "version: \"$VERSION\"" stackgres-k8s/install/helm/stackgres-cluster/Chart.yaml
yq_update_file "^appVersion: .*$" "appVersion: \"$VERSION\"" stackgres-k8s/install/helm/stackgres-cluster/Chart.yaml
