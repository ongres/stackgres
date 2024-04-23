#!/bin/sh

set -e

die() {
  >&2 echo "\n\t$1\n\n"
  exit 1
}

usage() {
  die "Usage: $0 <version>"
}

[ "$#" -ge 1 ] || usage

cd "$(dirname "$0")/../../.."

VERSION="$1"
MINOR_VERSION="$(printf %s "$VERSION" | cut -d . -f 1-2)"
NEXT_MINOR_VERSION="$(printf %s "$VERSION" | cut -d . -f 1).$(( $(printf %s "$VERSION" | cut -d . -f 2) + 1 ))"
PREVIOUS_MINOR_VERSION="$(printf %s "$VERSION" | cut -d . -f 1).$(( $(printf %s "$VERSION" | cut -d . -f 2) - 1 ))"
TO_REMOVE_MINOR_VERSION="$(printf %s "$VERSION" | cut -d . -f 1).$(( $(printf %s "$VERSION" | cut -d . -f 2) - 2 ))"
TO_REMOVE_FROM_SCHEDULED_MINOR_VERSION="$(printf %s "$VERSION" | cut -d . -f 1).$(( $(printf %s "$VERSION" | cut -d . -f 2) - 3 ))"
IS_NEW_MINOR_VERSION="${IS_NEW_MINOR_VERSION:-$(printf %s "$VERSION" | grep -q '\.0-rc1$' && printf true || printf false)}"
IS_UPGRADE_VERSION="$(printf %s "$VERSION" | grep -q '[-]\(alpha\|beta\)[0-9]\+$' && printf false || printf true)"
IS_GA_VERSION="$(printf %s "$VERSION" | grep -q '^[0-9]\+\.[0-9]\+\.[0-9]\+$' && printf true || printf false)"
if "$IS_GA_VERSION"
then
  NEXT_PATCH_VERSION="$(printf %s "$VERSION" | cut -d . -f 1-2).$(( $(printf %s "$VERSION" | cut -d - -f 1 | cut -d . -f 3) + 1 ))"
else
  NEXT_PATCH_VERSION="${MINOR_VERSION}.0"
fi

eval "cat << EOF
$(sed 's/`/\\`/g' .gitlab/Release.md.template)
EOF"
