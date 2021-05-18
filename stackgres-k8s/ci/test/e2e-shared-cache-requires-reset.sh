#!/bin/sh

. "$(dirname "$0")/e2e-shared-cache-functions.sh"

set -e

reset_others_shared_caches
shared_cache_requires_reset_or_update
