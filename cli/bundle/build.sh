#!/bin/bash
set -e
cd ${0%/*}/../../

# function definitions
info()
{
    echo '[INFO] ' "$@"
}
fatal()
{
    echo '[ERROR] ' "$@" >&2
    exit 1
}

detect_arch() {
    ARCH=$(uname -m)
    if [[ "$ARCH" == x86_64* ]]; then
      ARCH="amd64"
#    elif [[ "$ARCH" == "s390x" ]]; then
#      ARCH="s390x"
    else
      fatal "Arch $ARCH not supported"
#    elif [[ "$ARCH" == i*86 ]]; then
#    elif  [[ "$ARCH" == arm* ]]; then
    fi
}

# --- download from url into a path, $1: path, $2: url, e.g. download "/path/to/save" "<url>" ---
download() {
    [ $# -eq 2 ] || fatal 'download needs exactly 2 arguments'

    info "downloading $2"
    curl -o $1 -sfL $2

    [ $? -eq 0 ] || fatal 'Download failed'
}

# --- verify that the file exists ---
verify() {
    [ $# -eq 1 ] || fatal 'verify needs exactly 1 argument'
    [ -f $1 ] || fatal "expected file $1 doesn't exist"
}

build_matriarch() {
    pushd matriarch/
        info "building Matriarch: mvn clean package -Pnative"
        mvn clean package -Pnative
        verify target/matriarch
        upx target/matriarch
        ls -ahl target/
        info "finished build"
    popd
}

build_slony() {
    pushd slony/slony-linux/
        info "building Slony: mvn clean package -Pnative"
        mvn clean package -Pnative
        verify target/slony-linux
        upx target/slony-linux
        ls -ahl target/
        info "finished build"
    popd
}

build_cli() {
    pushd cli/
        info "building stackgres-cli: mvn clean package -Pnative"
        mvn clean package -Pnative
        verify target/cli
        upx target/cli
        ls -ahl target/
        info "finished build"
    popd
}

{
    detect_arch
    build_matriarch
    build_slony
    build_cli
}