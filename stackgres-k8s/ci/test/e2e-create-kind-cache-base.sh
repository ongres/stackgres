#!/bin/sh

. "$(dirname "$0")/e2e-shared-cache-functions.sh"

set -e

reset_others_shared_caches
if shared_cache_requires_reset_or_update
then
  if shared_cache_requires_reset
  then
    reset_shared_cache
  fi
  update_shared_cache
  post_update_shared_cache
fi
if cache_requires_reset
then
  reset_cache
fi