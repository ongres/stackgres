#!/bin/sh

cd "$(dirname "$0")"

if [ "x$1" = x ]
then
  >&2 echo "You must specify one or more expressions to preload extensions (like 'x86_64/linux/timescaledb-1\.7\.4-pg12')"
  exit 1
fi

if [ -f "$1" ]
then
  EXTENSIONS_CACHE_PRELOADED_EXTENSIONS="$(printf true | jq --slurpfile arg "$1" -c '$arg')"
else
  EXTENSIONS_CACHE_PRELOADED_EXTENSIONS="$(printf "$*" | tr ' ' '\n' | jq -R . | jq -c -s)"
fi

docker build \
  --build-arg EXTENSIONS_CACHE_PRELOADED_EXTENSIONS="$EXTENSIONS_CACHE_PRELOADED_EXTENSIONS" \
  $(echo "$-" | grep -q x && printf %s '--build-arg EXTENSIONS_CACHE_LOG_LEVEL=TRACE' || true) \
  -t "${IMAGE:-stackgres-offline-extensions}" \
  -f Dockerfile.offline-extensions .
