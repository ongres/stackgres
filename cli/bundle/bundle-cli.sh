#!/bin/bash
set -e
cd ${0%/*}

source ./bundle-common.sh

VARIANT=cli

{
    detect_arch
    setup_tmp
    bundle_cli
    verify_files
    package_tar
    hash_artifacts
    bundle_install
}
