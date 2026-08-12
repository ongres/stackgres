#!/bin/sh
set -e
set -o noglob

# Usage:
#   curl ... | ENV_VAR=... sh -
#       or
#   ENV_VAR=... ./install.sh
#
# Example:
#   Installing a server without traefik:
#     curl ... | INSTALL_STACKGRES_EXEC="--disable=traefik" sh -
#   Installing an agent to point at a server:
#     curl ... | OTT=xxx STACKGRES_URL=https://server-url:6443 sh -
#
# Environment variables:
#   - STACKGRES_*
#     Environment variables which begin with STACKGRES_ will be preserved for the
#     systemd service to use. Setting STACKGRES_URL without explicitly setting
#     a systemd exec command will default the command to "agent", and we
#     enforce that STACKGRES_TOKEN is also set.
#
#   - INSTALL_STACKGRES_NAME
#     The name of the StackGres installation, from will the default directories,
#     service names, and binaries are derived. This changes all following options
#     that contain the default StackGres name.
#     stackgres as the default
#
#   - INSTALL_STACKGRES_SKIP_ENABLE
#     If set to true will not enable or start StackGres service.
#
#   - INSTALL_STACKGRES_SKIP_START
#     If set to true will not start StackGres services.
#
#   - INSTALL_STACKGRES_CREATE_DEFAULT_CLUSTER
#     If set to true will create and start a default Postgres cluster.
#
#   - INSTALL_STACKGRES_VERSION
#     Version of StackGres to download.
#
#   - INSTALL_STACKGRES_DIR
#     Directory to install StackGres, config files, and required dependencies
#     /var/lib/stackgres as the default
#
#   - INSTALL_STACKGRES_BIN_DIR
#     Directory to install StackGres binary, links, and uninstall script to, or use
#     /usr/local/bin as the default
#
#   - INSTALL_STACKGRES_RUN_DIR
#     Directory to install StackGres run files
#     /var/run/stackgres as the default
#
#   - INSTALL_STACKGRES_SYSTEMD_DIR
#     Directory to install systemd service files to
#     /etc/systemd/system as the default
#
#   - INSTALL_STACKGRES_SYSTEMD_NAME
#     Name of systemd service to create, will default from the StackGres name
#     if not specified.

DOWNLOADER=

# --- default Matriarch URL, override with STACKGRES_MATRIARCH_URL env var ---
export STACKGRES_MATRIARCH_URL="${STACKGRES_MATRIARCH_URL:-dev.cc.stackgres.best}"

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

# --- fatal if no systemd ---
verify_system() {
    if [ -x /bin/systemctl ] || type systemctl > /dev/null 2>&1; then
        HAS_SYSTEMD=true
        return
    fi
    fatal 'Can not find systemd to use as a process supervisor'
}

# --- generates a UUID ---
generate_uuid()
{
  if command -v uuidgen >/dev/null; then
    uuidgen
  elif [ -r /proc/sys/kernel/random/uuid ]; then
    cat /proc/sys/kernel/random/uuid
  else
    fatal 'No UUID generator available, please install `uuidgen`'
  fi
}

# --- add quotes to command arguments ---
quote() {
    for arg in "$@"; do
        printf '%s\n' "$arg" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/'/"
    done
}

# --- add indentation and trailing slash to quoted args ---
quote_indent() {
    printf ' \\\n'
    for arg in "$@"; do
        printf '\t%s \\\n' "$(quote "$arg")"
    done
}

# --- escape most punctuation characters, except quotes, forward slash, and space ---
escape() {
    printf '%s' "$@" | sed -e 's/\([][!#$%&()*;<=>?\_`{|}]\)/\\\1/g;'
}

# --- escape double quotes ---
escape_dq() {
    printf '%s' "$@" | sed -e 's/"/\\"/g'
}

# --- define needed environment variables ---
setup_env() {
    # --- set StackGres name
    NAME=${INSTALL_STACKGRES_NAME:-stackgres}

    # --- use systemd name if defined or create default ---
    if [ -n "${INSTALL_STACKGRES_SYSTEMD_NAME}" ]; then
        SYSTEM_NAME=${INSTALL_STACKGRES_SYSTEMD_NAME}
    else
        SYSTEM_NAME=${NAME}
    fi

    validate_identifier ${NAME}
    validate_identifier ${SYSTEM_NAME}

    # --- use sudo if we are not already root ---
    SUDO=sudo
    if [ $(id -u) -eq 0 ]; then
        SUDO=
    fi

    # --- use install directory if defined or create default ---
    if [ -n "${INSTALL_STACKGRES_DIR}" ]; then
        STACKGRES_DIR=${INSTALL_STACKGRES_DIR}
    else
        STACKGRES_DIR=/var/lib/${NAME}
    fi
    FILE_STACKGRES_ENV=${STACKGRES_DIR}/.env

    # --- use binary install directory if defined or create default ---
    if [ -n "${INSTALL_STACKGRES_BIN_DIR}" ]; then
        BIN_DIR=${INSTALL_STACKGRES_BIN_DIR}
    else
        # --- use /usr/local/bin if root can write to it, otherwise use /opt/bin if it exists
        BIN_DIR=/usr/local/bin
        if ! $SUDO sh -c "touch ${BIN_DIR}/${NAME}-ro-test && rm -rf ${BIN_DIR}/${NAME}-ro-test"; then
            if [ -d /opt/bin ]; then
                BIN_DIR=/opt/bin
            fi
        fi
    fi

    # --- use run directory if defined or create default ---
    if [ -n "${INSTALL_STACKGRES_RUN_DIR}" ]; then
        STACKGRES_RUN_DIR=${INSTALL_STACKGRES_RUN_DIR}
    else
        STACKGRES_RUN_DIR=/var/run/${NAME}
    fi

    # --- use systemd directory if defined or create default ---
    if [ -n "${INSTALL_STACKGRES_SYSTEMD_DIR}" ]; then
        SYSTEMD_DIR="${INSTALL_STACKGRES_SYSTEMD_DIR}"
    else
        SYSTEMD_DIR=/etc/systemd/system
    fi

    # --- set related files from system name ---
    SERVICE_SLONY=${SYSTEM_NAME}-slony.service
    SERVICE_CONTAINERD=${SYSTEM_NAME}-containerd.service
    UNINSTALL_STACKGRES_SH=${UNINSTALL_STACKGRES_SH:-${BIN_DIR}/${NAME}-uninstall.sh}
    KILLALL_STACKGRES_SH=${KILLALL_STACKGRES_SH:-${BIN_DIR}/${NAME}-killall.sh}

    FILE_SLONY_SERVICE=${SYSTEMD_DIR}/${SERVICE_SLONY}
    FILE_CONTAINERD_SERVICE=${SYSTEMD_DIR}/${SERVICE_CONTAINERD}

    # --- StackGres daemon envs that are exported to .env config ---
    export STACKGRES_CRI_SOCKET_PATH="${STACKGRES_RUN_DIR}/containerd/containerd.sock"
    export STACKGRES_PATH="${STACKGRES_DIR}"
    export STACKGRES_SLONY_ID=$(generate_uuid)
    [ -n "${STACKGRES_MATRIARCH_URL}" ] && export STACKGRES_MATRIARCH_URL="${STACKGRES_MATRIARCH_URL}"
    if [ -z "${STACKGRES_SLONY_CLUSTERS_PATH}" ]; then
        export STACKGRES_SLONY_CLUSTERS_PATH="${STACKGRES_DIR}/clusters"
    fi
    if [ -z "${STACKGRES_SLONY_CLUSTERS_LOG_DIR}" ]; then
        export STACKGRES_SLONY_CLUSTERS_LOG_DIR="/var/log/${NAME}"
    fi
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
    # Return failure if it doesn't exist or is no executable
    [ -x "$(command -v $1)" ] || return 1

    # Set verified executable as our downloader program and return success
    DOWNLOADER=$1
    return 0
}

# --- create temporary directory and cleanup when done ---
setup_tmp() {
    TMP_DIR=$(mktemp -d -t stackgres-install.XXXXXXXXXX)
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

# --- use desired StackGres version if defined or find version from channel ---
get_release_version() {
    if [ -n "${INSTALL_STACKGRES_VERSION}" ]; then
        STACKGRES_VERSION=${INSTALL_STACKGRES_VERSION}
    else
        STACKGRES_VERSION='%STACKGRES_VERSION%'
    fi
    info "Installing StackGres ${STACKGRES_VERSION}"
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
The requested StackGres Cloud version '${STACKGRES_VERSION}' does not exist on the server.
Check INSTALL_STACKGRES_VERSION (or unset it to use the bundled default)."
            ;;
        *)
            fatal "Download failed: $2 returned HTTP $1"
            ;;
    esac
}

# --- download hash from github url ---
download_hash() {
    #if [ -n "${INSTALL_STACKGRES_COMMIT}" ]; then
    #    HASH_URL=${STORAGE_URL}/stackgres-${INSTALL_STACKGRES_COMMIT}.sha256sum
    #else
    #    HASH_URL=${GITHUB_URL}/download/${STACKGRES_VERSION}/sha256sum-${ARCH}.txt
    #fi
    HASH_URL="https://pga.ongres.dev/stackgres/sha256sum-cloud-${STACKGRES_VERSION}.txt"
    info "Fetching checksum ${HASH_URL}"
    download ${TMP_HASH} ${HASH_URL}
    HASH_EXPECTED=$(grep " stackgres-cloud-${STACKGRES_VERSION}-linux-${ARCH}.tar.gz$" ${TMP_HASH}) \
        || fatal "No checksum entry for stackgres-cloud-${STACKGRES_VERSION}-linux-${ARCH}.tar.gz in ${HASH_URL}.
StackGres Cloud ${STACKGRES_VERSION} may not be built for architecture '${ARCH}'."
    HASH_EXPECTED=${HASH_EXPECTED%%[[:blank:]]*}
}

# --- download StackGres distribution ---
download_stackgres() {
    # TODO
    #if [ -n "${INSTALL_STACKGRES_COMMIT}" ]; then
    #    STACKGRES_URL=...
    #else
    #    STACKGRES_URL=...
    #fi
    STACKGRES_URL="https://pga.ongres.dev/stackgres/stackgres-cloud-${STACKGRES_VERSION}-linux-${ARCH}.tar.gz"
    info "Downloading StackGres package ${STACKGRES_URL}"
    download_progress ${TMP_GZ} ${STACKGRES_URL}
}

# --- verify downloaded StackGres hash ---
verify_stackgres() {
    info "Verifying StackGres download with ${HASH_URL}"
    HASH_BIN=$(sha256sum ${TMP_GZ})
    HASH_BIN=${HASH_BIN%%[[:blank:]]*}
    if [ "${HASH_EXPECTED}" != "${HASH_BIN}" ]; then
        fatal "Download sha256 does not match ${HASH_EXPECTED}, got ${HASH_BIN}"
    fi
}

# --- install StackGres, extract to target directory ---
install_stackgres() {
    info "Installing StackGres to ${STACKGRES_DIR}"
    $SUDO tar -xf ${TMP_GZ} -C ${STACKGRES_DIR}
}

# --- setup the StackGres clusters directory
setup_clusters_dir() {
    $SUDO mkdir -p ${STACKGRES_SLONY_CLUSTERS_PATH}
}

# --- set the permissions of the install directory
set_install_dir_permissions() {
    $SUDO chmod 755 ${STACKGRES_DIR}
    $SUDO chmod 755 ${STACKGRES_DIR}/clusters
    $SUDO chown -R root:root ${STACKGRES_DIR}
}

# --- download and verify StackGres ---
download_and_verify() {
    setup_verify_arch
    verify_downloader curl || verify_downloader wget || fatal 'Can not find curl or wget for downloading files'
    setup_tmp
    get_release_version
    download_hash

    setup_clusters_dir
    download_stackgres
    verify_stackgres
    install_stackgres
    set_install_dir_permissions
}

# --- create scripts ---
create_scripts() {
    create_stackgres_cli
    create_killall
    create_uninstall
}

# --- create stackgres cli script ---
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

# --- create killall script ---
create_killall() {
    info "Creating killall script ${KILLALL_STACKGRES_SH}"
    $SUDO tee ${KILLALL_STACKGRES_SH} >/dev/null << EOF
#!/bin/sh
[ \$(id -u) -eq 0 ] || exec sudo \$0 \$@

crictl_cmd="${STACKGRES_DIR}/containerd/bin/crictl --runtime-endpoint unix://${STACKGRES_CRI_SOCKET_PATH}"
pods=\$(\$crictl_cmd pods --label managed-by=stackgres -q)
if [ -n "\$pods" ]; then
    \$crictl_cmd stopp \$pods
    \$crictl_cmd rmp \$pods
fi

set -x

# systemd
[ "${HAS_SYSTEMD}" = true ] && [ -s "${SYSTEMD_DIR}/${SERVICE_CONTAINERD}" ] && systemctl stop ${SYSTEM_NAME}-containerd.service
[ "${HAS_SYSTEMD}" = true ] && [ -s "${SYSTEMD_DIR}/${SERVICE_SLONY}" ] && systemctl stop ${SYSTEM_NAME}-slony.service

pschildren() {
    ps -e -o ppid= -o pid= | \
    sed -e 's/^\s*//g; s/\s\s*/\t/g;' | \
    grep -w "^\$1" | \
    cut -f2
}

pstree() {
    for pid in \$@; do
        echo \$pid
        for child in \$(pschildren \$pid); do
            pstree \$child
        done
    done
}

killtree() {
    kill -9 \$(
        { set +x; } 2>/dev/null;
        pstree \$@;
        set -x;
    ) 2>/dev/null
}

getshims() {
    ps -e -o pid= -o args= | sed -e 's/^ *//; s/\s\s*/\t/;' | grep -w '${NAME}/data/[^/]*/bin/containerd-shim' | cut -f1
}

killtree \$({ set +x; } 2>/dev/null; getshims; set -x)

do_unmount_and_remove() {
    set +x
    while read -r _ path _; do
        case "\$path" in \$1*) echo "\$path" ;; esac
    done < /proc/self/mounts | sort -r | xargs -r -t -n 1 sh -c 'umount "\$0" && rm -rf "\$0"'
    set -x
}

# TODO
do_unmount_and_remove '${STACKGRES_RUN_DIR}'
EOF
    $SUDO chmod 755 ${KILLALL_STACKGRES_SH}
    $SUDO chown root:root ${KILLALL_STACKGRES_SH}
}

# --- create uninstall script ---
create_uninstall() {
    info "Creating uninstall script ${UNINSTALL_STACKGRES_SH}"
    $SUDO tee ${UNINSTALL_STACKGRES_SH} >/dev/null << EOF
#!/bin/sh
set -x
[ \$(id -u) -eq 0 ] || exec sudo \$0 \$@

${KILLALL_STACKGRES_SH}

if command -v systemctl; then
    systemctl disable ${SYSTEM_NAME}-containerd ${SYSTEM_NAME}-slony
    systemctl reset-failed ${SYSTEM_NAME}-containerd ${SYSTEM_NAME}-slony
    systemctl daemon-reload
fi

rm -f ${FILE_SLONY_SERVICE}
rm -f ${FILE_CONTAINERD_SERVICE}
rm -f ${FILE_STACKGRES_ENV}

remove_uninstall() {
    rm -f ${UNINSTALL_STACKGRES_SH}
}
trap remove_uninstall EXIT

rm -rf ${STACKGRES_RUN_DIR}
rm -rf ${STACKGRES_DIR}
rm -f ${BIN_DIR}/${NAME}
rm -f ${KILLALL_STACKGRES_SH}
EOF
    $SUDO chmod 755 ${UNINSTALL_STACKGRES_SH}
    $SUDO chown root:root ${UNINSTALL_STACKGRES_SH}
}

# --- disable current service if loaded --
systemd_disable() {
    $SUDO systemctl disable ${SYSTEM_NAME}-containerd ${SYSTEM_NAME}-slony >/dev/null 2>&1 || true
    $SUDO rm -f /etc/systemd/system/${SERVICE_CONTAINERD} /etc/systemd/system/${SERVICE_SLONY} || true
}

# --- create StackGres & containerd config ---
create_config_files() {
    create_containerd_config
    create_env_file
}

# --- create containerd config ---
create_containerd_config() {
    $SUDO tee ${STACKGRES_DIR}/containerd/containerd-config.toml >/dev/null << EOF
version = 2

root = "${STACKGRES_DIR}/containerd/root"
state = "${STACKGRES_RUN_DIR}/containerd/run"

[grpc]
  address = "${STACKGRES_CRI_SOCKET_PATH}"
  uid = 1000
  gid = 1000
EOF
}

# --- capture current env and create file containing STACKGRES_ variables ---
create_env_file() {
    $SUDO touch ${FILE_STACKGRES_ENV}
    $SUDO chmod 0644 ${FILE_STACKGRES_ENV}
    echo "PATH=${STACKGRES_DIR}/containerd/bin:$PATH" | $SUDO tee ${FILE_STACKGRES_ENV} >/dev/null
    sh -c export | while read x v; do echo $v; done | grep -E '^STACKGRES_' | $SUDO tee -a ${FILE_STACKGRES_ENV} >/dev/null
    sh -c export | while read x v; do echo $v; done | grep -Ei '^(NO|HTTP|HTTPS)_PROXY' | $SUDO tee -a ${FILE_STACKGRES_ENV} >/dev/null
}

# --- write systemd service files ---
create_systemd_service_files() {
    info "systemd: Creating service files ${SYSTEMD_DIR}/{${SERVICE_SLONY},${SERVICE_CONTAINERD}}"

    $SUDO tee ${FILE_SLONY_SERVICE} >/dev/null << EOF
[Unit]
Description=StackGres Slony
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
EnvironmentFile=-${FILE_STACKGRES_ENV}
KillMode=process
Delegate=yes
Restart=always
RestartSec=5s
ExecStart=${STACKGRES_DIR}/bin/slony
EOF

    $SUDO tee ${FILE_CONTAINERD_SERVICE} >/dev/null << EOF
[Unit]
Description=StackGres Slony containerd
Wants=network-online.target
After=network-online.target

[Install]
WantedBy=${SERVICE_SLONY}

[Service]
Type=simple
EnvironmentFile=-${FILE_STACKGRES_ENV}
ExecStart=${STACKGRES_DIR}/containerd/bin/containerd --config ${STACKGRES_DIR}/containerd/containerd-config.toml --log-level debug
Restart=always
RestartSec=5s
EOF
}

# --- write systemd service files ---
create_service_files() {
    [ "${HAS_SYSTEMD}" = true ] && create_systemd_service_files
    return 0
}

# --- enable and start systemd service ---
systemd_enable() {
    info "systemd: Enabling ${SYSTEM_NAME} unit"
    $SUDO systemctl daemon-reload &>/dev/null
    $SUDO systemctl enable ${SERVICE_CONTAINERD} ${SERVICE_SLONY} &>/dev/null
}

systemd_start() {
    info "systemd: Starting ${SYSTEM_NAME} unit"
    $SUDO systemctl start ${SERVICE_CONTAINERD} &>/dev/null
    $SUDO systemctl start ${SERVICE_SLONY} &>/dev/null
}

# --- startup systemd or openrc service ---
service_enable_and_start() {
    [ "${INSTALL_STACKGRES_SKIP_ENABLE}" = true ] && return

    [ "${HAS_SYSTEMD}" = true ] && systemd_enable

    [ "${INSTALL_STACKGRES_SKIP_START}" = true ] && return

    [ "${HAS_SYSTEMD}" = true ] && systemd_start

#    ${BIN_DIR}/${NAME} admin preload-images &> /dev/null

    if [ "${INSTALL_STACKGRES_CREATE_DEFAULT_CLUSTER}" = true ]; then
        info 'Creating default PostgreSQL cluster'
        ${BIN_DIR}/${NAME} cluster create
    fi

    return 0
}

# --- print getting started messages ---
print_getting_started() {
    info "StackGres ${STACKGRES_VERSION} installed successfully"
    info ''
    info 'Create your first Postgres cluster: stackgres cluster create --name postgres'
    #info 'See information about your StackGres installation: stackgres info'
    info 'Uninstall StackGres with stackgres-uninstall.sh'
}

# --- re-evaluate args to include env command ---
eval set -- $(escape "") $(quote "$@")

# --- if STACKGRES_TOKEN is a 16-char short install token, exchange it for the full JWT ---
maybe_exchange_install_token() {
    if [ -z "$STACKGRES_TOKEN" ] || [ ${#STACKGRES_TOKEN} -ne 16 ]; then
        return
    fi
    if [ -z "$STACKGRES_MATRIARCH_URL" ]; then
        fatal 'STACKGRES_MATRIARCH_URL is required to exchange the install token'
    fi
    resp_file=$(mktemp)
    http_status=$(curl -s -o "$resp_file" -w '%{http_code}' \
        -X POST "https://${STACKGRES_MATRIARCH_URL}/install/tokens/$STACKGRES_TOKEN/exchange") \
        || { rm -f "$resp_file"; fatal "Failed to reach https://${STACKGRES_MATRIARCH_URL} - reload the UI and copy the install command again"; }
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

# --- run the install process --
{
    verify_system
    maybe_exchange_install_token
    setup_env "$@"
    download_and_verify
    create_scripts
    systemd_disable
    create_config_files
    create_service_files
    service_enable_and_start
    print_getting_started
}