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

# --- verify that the file exists ---
verify() {
    [ $# -eq 1 ] || fatal 'verify needs exactly 1 argument'
    [ -f $1 ] || fatal "expected file $1 doesn't exist"
}

# Build all three native binaries in a single reactor pass. -am also-builds the shared
# modules (proto, matriarch, matriarch-model). The `native` profile resolves per module:
#   - matriarch-standalone: Quarkus native -> target/matriarch-runner (renamed below)
#   - slony / cli:          native-maven-plugin -> target/slony, target/cli
build_native() {
    info "building native binaries: matriarch-standalone, slony, cli (mvn -Pnative)"
    mvn clean package -Pnative -pl matriarch-standalone,slony,cli -am

    # Quarkus emits the native executable as <output-name>-runner (matriarch-runner).
    # Normalize to `matriarch` so bundle-common.sh finds it.
    if [ ! -f matriarch-standalone/target/matriarch ]; then
        runner=$(ls matriarch-standalone/target/*-runner 2>/dev/null | head -1)
        [ -n "$runner" ] || fatal "no matriarch native runner produced in matriarch-standalone/target/"
        mv "$runner" matriarch-standalone/target/matriarch
    fi

    verify matriarch-standalone/target/matriarch
    verify slony/target/slony
    verify cli/target/cli

    info "compressing binaries with upx"
    upx matriarch-standalone/target/matriarch slony/target/slony cli/target/cli

    ls -ahl matriarch-standalone/target/matriarch slony/target/slony cli/target/cli
    info "finished build"
}

{
    detect_arch
    build_native
}
