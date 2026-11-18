#!/bin/sh

# Copy the documentation of a released version into doc/previous/<minor version>
# so that it can be published without building it.
#
# The given git reference (the release tag by default) is checked out in a
# temporary worktree where the swagger is built and doc/build.sh generates the
# documentation files. The documentation is then copied replacing symbolic
# links with the files they point to. Only the latest PREVIOUS_VERSIONS_TO_KEEP
# (6 by default) minor versions are kept.

set -e

die() {
  >&2 printf '%s\n' "$1"
  exit 1
}

usage() {
  die "Usage: $0 <version> [<git reference>]"
}

[ "$#" -ge 1 ] || usage

VERSION="$1"
REF="${2:-$1}"
PREVIOUS_VERSIONS_TO_KEEP="${PREVIOUS_VERSIONS_TO_KEEP:-6}"

printf %s "$VERSION" | grep -q '^[0-9]\+\.[0-9]\+\.[0-9]\+$' \
  || die "Version $VERSION is not a released version (<major>.<minor>.<patch>)"
MINOR_VERSION="$(printf %s "$VERSION" | cut -d . -f 1-2)"

cd "$(dirname "$0")/.."

git rev-parse --verify -q "$REF^{commit}" > /dev/null \
  || die "Git reference $REF not found"

TARGET_PATH="doc/previous/$MINOR_VERSION"
TEMP_PATH="$(mktemp -d)"
SOURCE_PATH="$TEMP_PATH/stackgres"
trap 'git worktree remove --force "$SOURCE_PATH" > /dev/null 2>&1 || true; rm -rf "$TEMP_PATH"' EXIT

git worktree add --detach "$SOURCE_PATH" "$REF"
(
  cd "$SOURCE_PATH"
  # Reuse the build images when available, building locally what is missing
  # and everything when the chain of build images can not be reconstructed.
  # The build images are never pushed.
  SKIP_PUSH=true SKIP_REMOTE_MANIFEST=true sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build.sh \
    parent-java operator-framework-java test-util-java common-java restapi-java \
    || DO_BUILD=true SKIP_PUSH=true SKIP_REMOTE_MANIFEST=true sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build.sh \
      parent-java operator-framework-java test-util-java common-java restapi-java
  # The swagger is only extracted when the build image was not built locally
  [ -f stackgres-k8s/src/restapi/target/swagger-merged.yaml ] \
    || sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build-functions.sh \
      extract restapi-java stackgres-k8s/src/restapi/target/swagger-merged.yaml \
    || SKIP_REMOTE_MANIFEST=true sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build-functions.sh \
      extract restapi-java stackgres-k8s/src/restapi/target/swagger-merged.yaml
  STACKGRES_VERSION="$VERSION" sh stackgres-k8s/ci/build/ciw sh doc/build.sh
)

rm -rf "$TARGET_PATH"
mkdir -p "$TARGET_PATH"
for FILE in config.toml content themes data
do
  cp -RL "$SOURCE_PATH/doc/$FILE" "$TARGET_PATH/$FILE"
done
# The .gitignore files of the copied version would exclude its generated files
find "$TARGET_PATH" -name .gitignore -type f -exec rm {} +
IGNORED_FILES="$(git ls-files --others --ignored --exclude-standard -- "$TARGET_PATH")"
[ -z "$IGNORED_FILES" ] \
  || die "The following copied files are ignored by git and would not be committed:
$IGNORED_FILES"
sed "s/^current_version: .*$/current_version: \"$VERSION\"/" \
  "$TARGET_PATH/data/versions.yml" > "$TEMP_PATH/versions.yml"
mv "$TEMP_PATH/versions.yml" "$TARGET_PATH/data/versions.yml"
echo "Copied the documentation of version $VERSION from $REF into $TARGET_PATH"

PREVIOUS_VERSIONS="$(ls -1 doc/previous | sort -V)"
PREVIOUS_VERSIONS_COUNT="$(printf '%s\n' "$PREVIOUS_VERSIONS" | wc -l)"
if [ "$PREVIOUS_VERSIONS_COUNT" -gt "$PREVIOUS_VERSIONS_TO_KEEP" ]
then
  printf '%s\n' "$PREVIOUS_VERSIONS" \
    | head -n "$((PREVIOUS_VERSIONS_COUNT - PREVIOUS_VERSIONS_TO_KEEP))" \
    | while read -r PREVIOUS_VERSION
      do
        rm -rf "doc/previous/$PREVIOUS_VERSION"
        echo "Removed doc/previous/$PREVIOUS_VERSION to keep only the latest $PREVIOUS_VERSIONS_TO_KEEP minor versions"
      done
fi
