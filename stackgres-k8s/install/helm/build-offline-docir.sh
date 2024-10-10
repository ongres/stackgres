#!/bin/sh
#
# Build an image of the cache of the StackGres images repository (the docir REST API) with the
# whole catalog and the preloaded image resolutions stored in it, to be used in air-gapped
# systems: install the operator with `repository.cache.enabled=true`,
# `repository.cache.offline=true` and the StatefulSet http container image replaced by the
# generated image (see the `repository.cache` section of the Helm chart values).
#
# Usage: [IMAGE=<image name>] [DOCIR_REPOSITORY_URL=<url>] [DOCIR_USE_PUBLISHED_IMAGES=<bool>]
#   build-offline-docir.sh <operator major.minor version> <image request>...
#   build-offline-docir.sh <operator major.minor version> <file with one image request per line>
#
# The image requests use the syntax of the cache key of the operator (see the
# `repository.cache.preloadedImages` value of the Helm chart), for example:
#   'tshirt-size=full&base=debian&flavors=postgres@16.15&addons=patroni&addons=wal-g&addons=hdrhistogram'

cd "$(dirname "$0")"

if [ "x$1" = x ]
then
  >&2 echo "You must specify the <major>.<minor> version of the operator (like 1.20)"
  exit 1
fi

DOCIR_OPERATOR_VERSION="$1"
shift

if [ "x$1" = x ]
then
  >&2 echo "You must specify one or more image requests to preload" \
    "(like 'tshirt-size=full&base=debian&flavors=postgres@16.15&addons=patroni&addons=wal-g&addons=hdrhistogram')"
  exit 1
fi

if [ -f "$1" ]
then
  DOCIR_CACHE_PRELOADED_IMAGES="$(grep -v '^$' "$1" | jq -R . | jq -c -s .)"
else
  DOCIR_CACHE_PRELOADED_IMAGES="$(printf '%s\n' "$@" | jq -R . | jq -c -s .)"
fi

docker build \
  --build-arg DOCIR_OPERATOR_VERSION="$DOCIR_OPERATOR_VERSION" \
  --build-arg DOCIR_REPOSITORY_URL="${DOCIR_REPOSITORY_URL:-https://sgcr.dev}" \
  --build-arg DOCIR_USE_PUBLISHED_IMAGES="${DOCIR_USE_PUBLISHED_IMAGES:-true}" \
  --build-arg DOCIR_CACHE_PRELOADED_IMAGES="$DOCIR_CACHE_PRELOADED_IMAGES" \
  $(echo "$-" | grep -q x && printf %s '--build-arg DOCIR_CACHE_LOG_LEVEL=TRACE' || true) \
  -t "${IMAGE:-stackgres-offline-docir}" \
  -f Dockerfile.offline-docir .
