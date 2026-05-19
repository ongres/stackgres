#!/usr/bin/env bash
#
# Reproducibility harness: builds the given OCI image target twice from a
# fully-clean state and compares manifest digests. Same inputs => same
# digest. (An OCI image's manifest digest is sha256 of the manifest JSON;
# the manifest references config + layer blobs by content-address, so a
# stable manifest digest implies stable bits all the way down.)
#
# Usage:
#   ./repro_check.sh                       # check //agent:image
#   ./repro_check.sh //agent:image         # explicit
#   ./repro_check.sh <other-target>        # any oci_image target accepts
#
# Run from anywhere; cd's to the build root automatically.
#
# Wall-clock cost: ~10-15 min per target on a 16-core box (two cold cargo
# builds). Run before tagging a release, or in CI as a nightly check.

set -euo pipefail

cd "$(dirname "$0")/.."

extract_manifest_digest() {
    local img_dir="$1"
    if [[ ! -f "$img_dir/index.json" ]]; then
        echo "ERROR: no index.json at $img_dir" >&2
        return 1
    fi
    # The first sha256:... in index.json is the manifest digest.
    grep -oE 'sha256:[0-9a-f]{64}' "$img_dir/index.json" | head -1
}

cold_build_digest() {
    local target="$1"
    bazel clean --expunge >/dev/null 2>&1
    bazel build "$target" >&2
    extract_manifest_digest "$(bazel cquery --output=files "$target" 2>/dev/null)"
}

check_target() {
    local target="$1"
    echo "================ $target ================"
    echo "=== Build 1 (clean state) ==="
    local d1
    d1=$(cold_build_digest "$target")
    echo "  manifest digest: $d1"
    echo
    echo "=== Build 2 (clean state) ==="
    local d2
    d2=$(cold_build_digest "$target")
    echo "  manifest digest: $d2"
    echo
    if [[ "$d1" == "$d2" ]]; then
        echo "REPRO OK ($target): $d1"
        return 0
    fi
    echo "REPRO FAIL ($target)"
    echo "  Build 1: $d1"
    echo "  Build 2: $d2"
    echo
    echo "Investigate by diffing the two image directories or comparing"
    echo "individual blobs at bazel-out/k8-*/bin/<role>/image/blobs/sha256/."
    return 1
}

if [[ $# -gt 0 ]]; then
    targets=("$@")
else
    targets=(//agent:image)
fi

fails=0
for target in "${targets[@]}"; do
    check_target "$target" || fails=$((fails + 1))
    echo
done

if [[ $fails -gt 0 ]]; then
    echo "$fails of ${#targets[@]} target(s) failed reproducibility"
    exit 1
fi

echo "All ${#targets[@]} target(s) reproducible"
