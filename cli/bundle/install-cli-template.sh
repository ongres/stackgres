#!/bin/sh
set -e
set -o noglob

# Usage:
#   curl ... | OTT=<one-time-token> sh -
#       or
#   OTT=<one-time-token> ./stackgres-cli.sh
#
# Installs the single, self-contained `stackgres` binary into a bin directory and configures a context
# in the INVOKING user's ~/.stackgres/. When an OTT (one-time token from the web console) is supplied it
# runs `stackgres login` (exchanges it for a JWT and saves the session); a full STACKGRES_TOKEN JWT or a
# bare endpoint are stored via `stackgres context set`. There is no /var/lib install, no wrapper, and no
# .env: the CLI reads its endpoint/token/environment straight from ~/.stackgres/config.yaml.
#
# Environment variables:
#   - OTT
#     One-time token from the web console; exchanged for a JWT via `stackgres login`.
#
#   - STACKGRES_ENDPOINT_URL
#     Cloud/matriarch endpoint. Defaults to dev-cc.stackgres.best.
#
#   - STACKGRES_TOKEN
#     A full JWT, if you already have one (stored as-is instead of exchanging an OTT).
#
#   - INSTALL_STACKGRES_NAME
#     The installed command name (default: stackgres).
#
#   - INSTALL_STACKGRES_VERSION
#     Version of the StackGres CLI to download.
#
#   - INSTALL_STACKGRES_BIN_DIR
#     Directory to install the stackgres binary to. /usr/local/bin as the default
#     (falls back to /opt/bin when /usr/local/bin is not writable).

DOWNLOADER=

# --- default Matriarch/cloud URL, override with STACKGRES_ENDPOINT_URL env var ---
export STACKGRES_ENDPOINT_URL="${STACKGRES_ENDPOINT_URL:-dev-cc.stackgres.best}"

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

# --- install the single stackgres binary into BIN_DIR (the package is just ./bin/stackgres) ---
install_stackgres() {
    info "Installing StackGres CLI to ${BIN_DIR}/${NAME}"
    tar -xf ${TMP_GZ} -C ${TMP_DIR}
    [ -f "${TMP_DIR}/bin/stackgres" ] || fatal "unexpected package layout: bin/stackgres not found"
    $SUDO install -m 0755 "${TMP_DIR}/bin/stackgres" "${BIN_DIR}/${NAME}"
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
}

# --- run a command as the INVOKING user, so ~/.stackgres lands in their home even under `sudo sh` ---
run_as_user() {
    if [ "$(id -u)" -eq 0 ] && [ -n "${SUDO_USER:-}" ] && [ "${SUDO_USER}" != "root" ]; then
        sudo -u "${SUDO_USER}" -H "$@"
    else
        "$@"
    fi
}

# --- seed ~/.stackgres for the invoking user: exchange an OTT (login), store a JWT, or set endpoint ---
configure_context() {
    CLI="${BIN_DIR}/${NAME}"
    if [ -n "${OTT}" ]; then
        info "Configuring ~/.stackgres (exchanging the one-time token)"
        run_as_user "${CLI}" login "${OTT}" --endpoint "${STACKGRES_ENDPOINT_URL}" \
            || warn "automatic login failed — finish it with: ${NAME} login <OTT> --endpoint ${STACKGRES_ENDPOINT_URL}"
    elif [ -n "${STACKGRES_TOKEN}" ]; then
        info "Configuring ~/.stackgres (using the provided token)"
        run_as_user "${CLI}" context set default --endpoint "${STACKGRES_ENDPOINT_URL}" --token "${STACKGRES_TOKEN}" \
            || warn "could not write the context — set it with: ${NAME} context set default --endpoint ${STACKGRES_ENDPOINT_URL} --token <jwt>"
    else
        info "Configuring ~/.stackgres (endpoint only)"
        run_as_user "${CLI}" context set default --endpoint "${STACKGRES_ENDPOINT_URL}" \
            || warn "could not write the context — set it with: ${NAME} context set default --endpoint ${STACKGRES_ENDPOINT_URL}"
        info "Authenticate later with: ${NAME} login <OTT>"
    fi
}

# --- print getting started messages ---
print_getting_started() {
    info "StackGres CLI ${STACKGRES_VERSION} installed to ${BIN_DIR}/${NAME}"
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
    info "  ${SUDO} rm -f ${BIN_DIR}/${NAME}          # the stackgres binary"
    info '  rm -rf ~/.stackgres                       # your contexts/config (optional)'
    info ''
}

# --- run the install process --
{
    setup_env "$@"
    download_and_verify
    configure_context
    print_getting_started
}
