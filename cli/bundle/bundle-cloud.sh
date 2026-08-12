#!/bin/bash
set -e
cd ${0%/*}

source ./bundle-common.sh

VARIANT=cloud

{
    detect_arch
    setup_tmp
    bundle_slony
    bundle_cli
    bundle_containerd
    bundle_runc
    bundle_crictl
    remove_not_required_files
    verify_files
    package_tar
    hash_artifacts
    bundle_install
}
