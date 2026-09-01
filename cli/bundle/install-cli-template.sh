#!/bin/sh
set -e
set -o noglob

# Usage:
#   curl ... | ENV_VAR=... sh -
#       or
#   ENV_VAR=... ./stackgres-cli.sh
#
# Example:
#   Installing the CLI pointed at a Matriarch:
#     curl ... | OTT=xxx STACKGRES_ENDPOINT_URL=server-url:6443 sh -
#
# Environment variables:
#   - STACKGRES_*
#     Environment variables which begin with STACKGRES_ will be preserved in the
#     .env file for the CLI to use (e.g. STACKGRES_TOKEN, STACKGRES_ENDPOINT_URL).
#
#   - INSTALL_STACKGRES_NAME
#     The name of the StackGres installation, from which the default directories
#     and binaries are derived. Defaults to "stackgres".
#
#   - INSTALL_STACKGRES_VERSION
#     Version of the StackGres CLI to download.
#
#   - INSTALL_STACKGRES_DIR
#     Directory to install the StackGres CLI binary and .env config to.
#     /var/lib/stackgres as the default.
#
#   - INSTALL_STACKGRES_BIN_DIR
#     Directory to install the stackgres wrapper script to.
#     /usr/local/bin as the default.

DOWNLOADER=

# --- default Matriarch URL, override with STACKGRES_ENDPOINT_URL env var ---
export STACKGRES_ENDPOINT_URL="${STACKGRES_ENDPOINT_URL:-dev-cc.stackgres.best}"

# --- accept OTT as user-facing alias for STACKGRES_TOKEN ---
[ -n "$OTT" ] && export STACKGRES_TOKEN="$OTT"

# --- helper functions for logs ---
info()
{
    echo "$@"
}
warn()
{
    echo 'WARNING: ' "$@" >&2
}
fatal()
{
    echo 'ERROR: ' "$@" >&2
    exit 1
}

# --- add quotes to command arguments ---
quote() {
    for arg in "$@"; do
        printf '%s\n' "$arg" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/'/"
    done
}

# --- escape most punctuation characters, except quotes, forward slash, and space ---
escape() {
    printf '%s' "$@" | sed -e 's/\([][!#$%&()*;<=>?\_`{|}]\)/\\\1/g;'
}

# --- check for invalid characters in system name ---
validate_identifier() {
    valid_chars=$(printf '%s' "$1" | sed -e 's/[][!#$%&()*;<=>?\_`{|}/[:space:]]/^/g;' )
    if [ "$1" != "${valid_chars}"  ]; then
        invalid_chars=$(printf '%s' "${valid_chars}" | sed -e 's/[^^]/ /g')
        fatal "Invalid characters for name:
            $1
            ${invalid_chars}"
    fi
}

# --- define needed environment variables ---
setup_env() {
    NAME=${INSTALL_STACKGRES_NAME:-stackgres}

    validate_identifier ${NAME}

    SUDO=sudo
    if [ $(id -u) -eq 0 ]; then
        SUDO=
    fi

    if [ -n "${INSTALL_STACKGRES_DIR}" ]; then
        STACKGRES_DIR=${INSTALL_STACKGRES_DIR}
    else
        STACKGRES_DIR=/var/lib/${NAME}
    fi
    FILE_STACKGRES_ENV=${STACKGRES_DIR}/.env

    if [ -n "${INSTALL_STACKGRES_BIN_DIR}" ]; then
        BIN_DIR=${INSTALL_STACKGRES_BIN_DIR}
    else
        BIN_DIR=/usr/local/bin
        if ! $SUDO sh -c "touch ${BIN_DIR}/${NAME}-ro-test && rm -rf ${BIN_DIR}/${NAME}-ro-test"; then
            if [ -d /opt/bin ]; then
                BIN_DIR=/opt/bin
            fi
        fi
    fi
}

# --- set arch and suffix, fatal if architecture not supported ---
setup_verify_arch() {
    if [ -z "$ARCH" ]; then
        ARCH=$(uname -m)
    fi
    case $ARCH in
        amd64)
            ARCH=amd64
            ;;
        x86_64)
            ARCH=amd64
            ;;
        arm64)
            ARCH=arm64
            ;;
        s390x)
            ARCH=s390x
            ;;
        aarch64)
            ARCH=arm64
            ;;
        arm*)
            ARCH=arm
            ;;
        *)
            fatal "Unsupported architecture $ARCH"
    esac
}

# --- verify existence of network downloader executable ---
verify_downloader() {
    [ -x "$(command -v $1)" ] || return 1

    DOWNLOADER=$1
    return 0
}

# --- create temporary directory and cleanup when done ---
setup_tmp() {
    TMP_DIR=$(mktemp -d -t stackgres-cli-install.XXXXXXXXXX)
    TMP_HASH=${TMP_DIR}/stackgres.hash
    TMP_GZ=${TMP_DIR}/stackgres.tar.gz
    cleanup() {
        code=$?
        set +e
        trap - EXIT
        rm -rf ${TMP_DIR}
        exit $code
    }
    trap cleanup INT EXIT
}

# --- use desired StackGres version if defined or fall back to bundled version ---
get_release_version() {
    if [ -n "${INSTALL_STACKGRES_VERSION}" ]; then
        STACKGRES_VERSION=${INSTALL_STACKGRES_VERSION}
    else
        STACKGRES_VERSION='%STACKGRES_VERSION%'
    fi
    info "Installing StackGres CLI ${STACKGRES_VERSION}"
}

# --- download, $1: path, $2: url ---
download() {
    [ $# -eq 2 ] || fatal 'download needs exactly 2 arguments'
    _do_download "$1" "$2" ''
}

# --- download with progress bar, $1: path, $2: url ---
download_progress() {
    [ $# -eq 2 ] || fatal 'download needs exactly 2 arguments'
    _do_download "$1" "$2" 'progress'
}

# --- internal download helper, $1: path, $2: url, $3: 'progress' to show a progress bar ---
_do_download() {
    case $DOWNLOADER in
        curl)
            if [ "$3" = 'progress' ]; then
                _status=$(curl -o "$1" -SL --progress-bar -w '%{http_code}' "$2") \
                    || fatal "Failed to reach $2 - check your network connection or proxy settings"
            else
                _status=$(curl -o "$1" -sSL -w '%{http_code}' "$2") \
                    || fatal "Failed to reach $2 - check your network connection or proxy settings"
            fi
            _check_http_status "$_status" "$2"
            ;;
        wget)
            if [ "$3" = 'progress' ]; then
                wget -O "$1" --show-progress "$2" \
                    || fatal "Failed to download $2 - check your network connection, proxy settings, or that the requested version exists"
            else
                wget -O "$1" "$2" \
                    || fatal "Failed to download $2 - check your network connection, proxy settings, or that the requested version exists"
            fi
            ;;
        *)
            fatal "Incorrect executable '$DOWNLOADER'"
            ;;
    esac
}

# --- branch on HTTP status from curl, $1: status, $2: url ---
_check_http_status() {
    case "$1" in
        2??) ;;
        404)
            fatal "Not found: $2
The requested StackGres CLI version '${STACKGRES_VERSION}' does not exist on the server.
Check INSTALL_STACKGRES_VERSION (or unset it to use the bundled default)."
            ;;
        *)
            fatal "Download failed: $2 returned HTTP $1"
            ;;
    esac
}

# --- download sha256 hash file ---
download_hash() {
    HASH_URL="https://pga.ongres.dev/stackgres/sha256sum-cli-${STACKGRES_VERSION}.txt"
    info "Fetching checksum ${HASH_URL}"
    download ${TMP_HASH} ${HASH_URL}
    HASH_EXPECTED=$(grep " stackgres-cli-${STACKGRES_VERSION}-linux-${ARCH}.tar.gz$" ${TMP_HASH}) \
        || fatal "No checksum entry for stackgres-cli-${STACKGRES_VERSION}-linux-${ARCH}.tar.gz in ${HASH_URL}.
The StackGres CLI ${STACKGRES_VERSION} may not be built for architecture '${ARCH}'."
    HASH_EXPECTED=${HASH_EXPECTED%%[[:blank:]]*}
}

# --- download StackGres CLI distribution ---
download_stackgres() {
    STACKGRES_URL="https://pga.ongres.dev/stackgres/stackgres-cli-${STACKGRES_VERSION}-linux-${ARCH}.tar.gz"
    info "Downloading StackGres CLI package ${STACKGRES_URL}"
    download_progress ${TMP_GZ} ${STACKGRES_URL}
}

# --- verify downloaded StackGres CLI hash ---
verify_stackgres() {
    info "Verifying StackGres CLI download with ${HASH_URL}"
    HASH_BIN=$(sha256sum ${TMP_GZ})
    HASH_BIN=${HASH_BIN%%[[:blank:]]*}
    if [ "${HASH_EXPECTED}" != "${HASH_BIN}" ]; then
        fatal "Download sha256 does not match ${HASH_EXPECTED}, got ${HASH_BIN}"
    fi
}

# --- install StackGres CLI, extract to target directory ---
install_stackgres() {
    info "Installing StackGres CLI to ${STACKGRES_DIR}"
    $SUDO mkdir -p ${STACKGRES_DIR}
    $SUDO tar -xf ${TMP_GZ} -C ${STACKGRES_DIR}
}

# --- set the permissions of the install directory ---
set_install_dir_permissions() {
    $SUDO chmod 755 ${STACKGRES_DIR}
    $SUDO chown -R root:root ${STACKGRES_DIR}
}

# --- download and verify StackGres CLI ---
download_and_verify() {
    setup_verify_arch
    verify_downloader curl || verify_downloader wget || fatal 'Can not find curl or wget for downloading files'
    setup_tmp
    get_release_version
    download_hash

    download_stackgres
    verify_stackgres
    install_stackgres
    set_install_dir_permissions
}

# --- create stackgres cli wrapper script ---
create_stackgres_cli() {
    info "Creating CLI ${BIN_DIR}/${NAME}"
    $SUDO tee ${BIN_DIR}/${NAME} >/dev/null << EOF
#!/bin/bash
set -euo pipefail
set -o allexport
source ${FILE_STACKGRES_ENV}
set +o allexport

exec ${STACKGRES_DIR}/bin/stackgres \$@
EOF
    $SUDO chmod 755 ${BIN_DIR}/${NAME}
}

# --- capture current env and create file containing STACKGRES_ variables ---
create_env_file() {
    $SUDO touch ${FILE_STACKGRES_ENV}
    $SUDO chmod 0644 ${FILE_STACKGRES_ENV}
    echo "PATH=${STACKGRES_DIR}/bin:$PATH" | $SUDO tee ${FILE_STACKGRES_ENV} >/dev/null
    sh -c export | while read x v; do echo $v; done | grep -E '^STACKGRES_' | $SUDO tee -a ${FILE_STACKGRES_ENV} >/dev/null
    sh -c export | while read x v; do echo $v; done | grep -Ei '^(NO|HTTP|HTTPS)_PROXY' | $SUDO tee -a ${FILE_STACKGRES_ENV} >/dev/null
}

# --- if STACKGRES_TOKEN is a 16-char short install token, exchange it for the full JWT ---
maybe_exchange_install_token() {
    if [ -z "$STACKGRES_TOKEN" ] || [ ${#STACKGRES_TOKEN} -ne 16 ]; then
        return
    fi
    if [ -z "$STACKGRES_ENDPOINT_URL" ]; then
        fatal 'STACKGRES_ENDPOINT_URL is required to exchange the install token'
    fi
    resp_file=$(mktemp)
    http_status=$(curl -s -o "$resp_file" -w '%{http_code}' \
        -X POST "https://${STACKGRES_ENDPOINT_URL}/install/tokens/$STACKGRES_TOKEN/exchange") \
        || { rm -f "$resp_file"; fatal "Failed to reach https://${STACKGRES_ENDPOINT_URL} - reload the UI and copy the install command again"; }
    if [ "$http_status" != "200" ]; then
        rm -f "$resp_file"
        fatal 'Install token expired or already used - reload the UI and copy the install command again'
    fi
    STACKGRES_TOKEN=$(sed -n 's/.*"jwt":"\([^"]*\)".*/\1/p' "$resp_file")
    rm -f "$resp_file"
    if [ -z "$STACKGRES_TOKEN" ]; then
        fatal 'Could not parse exchange response from Matriarch'
    fi
    export STACKGRES_TOKEN
}

# --- print getting started messages ---
print_getting_started() {
    info "StackGres CLI ${STACKGRES_VERSION} installed successfully"
    info ''
    info 'Get started:'
    info '  stackgres status      # current endpoint, user and environments'
    info '  stackgres --help      # list all commands'
    info ''
    info 'Enable zsh autocompletion by adding this line to your ~/.zshrc and restarting your shell:'
    info '  source <(stackgres completion zsh)'
    info '  # needs compinit initialized (oh-my-zsh does this, or: autoload -Uz compinit && compinit)'
    info ''
    info 'To uninstall:'
    info "  ${SUDO} rm -f ${BIN_DIR}/${NAME}          # the stackgres command"
    info "  ${SUDO} rm -rf ${STACKGRES_DIR}           # the CLI binary and its .env"
    info '  rm -rf ~/.stackgres                       # your contexts/config (optional)'
    info ''
}

# --- re-evaluate args to include env command ---
eval set -- $(escape "") $(quote "$@")

# --- run the install process --
{
    maybe_exchange_install_token
    setup_env "$@"
    download_and_verify
    create_stackgres_cli
    create_env_file
    print_getting_started
}
