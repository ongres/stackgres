# Shared bundling library, sourced by bundle.sh / bundle-cloud.sh / bundle-cli.sh.
# Drivers must:
#   - cd to the bundle directory (cd ${0%/*}) before sourcing this file
#   - set VARIANT (one of: full, cloud, cli) before calling the bundling steps

# --- versions ---
#STACKGRES_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
STACKGRES_VERSION="${STACKGRES_VERSION:-0.1}"
CONTAINERD_VERSION='1.7.3'
RUNC_VERSION='1.1.14'
CRICTL_VERSION='1.27.1'

info()
{
    echo '[INFO] ' "$@"
}
fatal()
{
    echo '[ERROR] ' "$@" >&2
    exit 1
}

# --- per-variant name mapping ---
variant_tarball_prefix() {
    case "$VARIANT" in
        full)  echo "stackgres" ;;
        cloud) echo "stackgres-cloud" ;;
        cli)   echo "stackgres-cli" ;;
        *)     fatal "unknown VARIANT '$VARIANT'" ;;
    esac
}

variant_install_template() {
    case "$VARIANT" in
        full)  echo "install-template.sh" ;;
        cloud) echo "install-cloud-template.sh" ;;
        cli)   echo "install-cli-template.sh" ;;
        *)     fatal "unknown VARIANT '$VARIANT'" ;;
    esac
}

variant_install_output() {
    case "$VARIANT" in
        full)  echo "stackgres.sh" ;;
        cloud) echo "stackgres-cloud.sh" ;;
        cli)   echo "stackgres-cli.sh" ;;
        *)     fatal "unknown VARIANT '$VARIANT'" ;;
    esac
}

# --- detect architecture ---
detect_arch() {
    ARCH=$(uname -m)
    if [[ "$ARCH" == x86_64* ]]; then
      ARCH="amd64"
    else
      fatal "Arch $ARCH not supported"
    fi
}

# --- create temporary directory and cleanup when done ---
setup_tmp() {
    info "bundling StackGres ${VARIANT} ${STACKGRES_VERSION}"
    TMP_DIR=$(mktemp -d -t stackgres-bundle.XXXXXXXXXX)
    info "setting up in ${TMP_DIR}"
    mkdir -p ${TMP_DIR}/bin
    cleanup() {
        code=$?
        set +e
        trap - EXIT
        rm -rf ${TMP_DIR}
        exit $code
    }
    trap cleanup INT EXIT
}

# --- bundle the Matriarch binary ---
bundle_matriarch() {
    if [ -f ../../matriarch/target/matriarch ]; then
        cp ../../matriarch/target/matriarch ${TMP_DIR}/bin/matriarch
    else
        fatal "expected file matriarch/target/matriarch don't exist"
    fi
}

# --- bundle the Slony binary ---
bundle_slony() {
    if [ -f ../../slony/slony-linux/target/slony-linux ]; then
        cp ../../slony/slony-linux/target/slony-linux ${TMP_DIR}/bin/slony
    else
        fatal "expected file slony/slony-linux/target/slony-linux don't exist"
    fi
}

# --- bundle the CLI binary ---
bundle_cli() {
    if [ -f ../target/cli ]; then
        cp ../target/cli ${TMP_DIR}/bin/stackgres
    else
        fatal "expected file cli/target/cli don't exist"
    fi
}

# --- bundle the containerd files ---
bundle_containerd() {
    mkdir -p ${TMP_DIR}/containerd/
    download ${TMP_DIR}/containerd/containerd.tar.gz "https://github.com/containerd/containerd/releases/download/v${CONTAINERD_VERSION}/containerd-${CONTAINERD_VERSION}-linux-${ARCH}.tar.gz"
    extract_remove ${TMP_DIR}/containerd/ containerd.tar.gz
}

# --- bundle the runc files ---
bundle_runc() {
    download ${TMP_DIR}/containerd/bin/runc "https://github.com/opencontainers/runc/releases/download/v${RUNC_VERSION}/runc.${ARCH}"
    chmod 755 ${TMP_DIR}/containerd/bin/runc
}

# --- bundle the CRICTL tool ---
bundle_crictl() {
    download ${TMP_DIR}/containerd/bin/crictl.tar.gz "https://github.com/kubernetes-sigs/cri-tools/releases/download/v${CRICTL_VERSION}/crictl-v${CRICTL_VERSION}-linux-${ARCH}.tar.gz"
    extract_remove ${TMP_DIR}/containerd/bin/ crictl.tar.gz
}

# --- remove the binaries that aren't required for Slony Linux ---
remove_not_required_files() {
    info "removing unnecessary binary files from package"
    rm ${TMP_DIR}/containerd/bin/containerd-shim
    rm ${TMP_DIR}/containerd/bin/containerd-shim-runc-v1
    rm ${TMP_DIR}/containerd/bin/containerd-stress
    rm ${TMP_DIR}/containerd/bin/ctr
}

# --- verify that the expected files have been bundled for this variant ---
verify_files() {
    case "$VARIANT" in
        full)
            verify ${TMP_DIR}/bin/matriarch
            verify ${TMP_DIR}/bin/slony
            verify ${TMP_DIR}/bin/stackgres
            verify ${TMP_DIR}/containerd/bin/containerd
            verify ${TMP_DIR}/containerd/bin/runc
            verify ${TMP_DIR}/containerd/bin/crictl
            ;;
        cloud)
            verify ${TMP_DIR}/bin/slony
            verify ${TMP_DIR}/bin/stackgres
            verify ${TMP_DIR}/containerd/bin/containerd
            verify ${TMP_DIR}/containerd/bin/runc
            verify ${TMP_DIR}/containerd/bin/crictl
            ;;
        cli)
            verify ${TMP_DIR}/bin/stackgres
            ;;
        *) fatal "unknown VARIANT '$VARIANT'" ;;
    esac
}

# --- package all files into the final tar gz ---
package_tar() {
    local out=$(variant_tarball_prefix)-${STACKGRES_VERSION}-linux-${ARCH}.tar.gz
    pushd ${TMP_DIR} > /dev/null
      tar --exclude=stackgres.tar.gz -cf stackgres.tar.gz .
    popd > /dev/null
    cp ${TMP_DIR}/stackgres.tar.gz ${out}
    info "packaged all files into cli/bundle/${out}"
}

# --- hash the final tar gz ---
hash_artifacts() {
    local tarball=$(variant_tarball_prefix)-${STACKGRES_VERSION}-linux-${ARCH}.tar.gz
    local hash_file
    case "$VARIANT" in
        full) hash_file=sha256sum-${STACKGRES_VERSION}.txt ;;
        *)    hash_file=sha256sum-${VARIANT}-${STACKGRES_VERSION}.txt ;;
    esac
    sha256sum ${tarball} > ${hash_file}
    info "added hashes into cli/bundle/${hash_file}"
}

# --- copy the install template with version specifics ---
bundle_install() {
    local template=$(variant_install_template)
    local output=$(variant_install_output)
    cp ${template} ${output}
    sed -i "s/STACKGRES_VERSION='%STACKGRES_VERSION%'/STACKGRES_VERSION='${STACKGRES_VERSION}'/" ${output}
    info "added install script into cli/bundle/${output}"
}

# --- download from url into a path, $1: path, $2: url ---
download() {
    [ $# -eq 2 ] || fatal 'download needs exactly 2 arguments'

    info "downloading $2"
    curl -o $1 -sfL $2

    [ $? -eq 0 ] || fatal 'Download failed'
}

# --- extract the tar gz file in a directory and remove the tar gz file, $1: directory, $2: tar.gz file name ---
extract_remove() {
    [ $# -eq 2 ] || fatal 'extract_remove needs exactly 2 arguments'

    info "extracting file $2"
    pushd $1 > /dev/null
      tar -xf $2
      rm $2
    popd > /dev/null
}

# --- verify that the file exists ---
verify() {
    [ $# -eq 1 ] || fatal 'verify needs exactly 1 argument'
    [ -f $1 ] || fatal "expected file $1 doesn't exist"
}
