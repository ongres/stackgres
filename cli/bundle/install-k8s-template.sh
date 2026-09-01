#!/bin/sh
set -e
set -o noglob

# Installs the development StackGres Kubernetes operator into the cluster your kubectl points at,
# wired to the cloud. It fetches ONLY the operator helm chart (a GitLab subdir archive, into /tmp)
# and runs `helm upgrade --install` with the dev operator image from the GitLab registry.
#
# Usage:
#   curl -sfL https://pga.ongres.dev/stackgres-k8s.sh | OTT=<ott> sh -
#
# Environment variables:
#   - OTT / STACKGRES_TOKEN   one-time install token from the cloud UI (exchanged for a JWT)
#   - STACKGRES_ENDPOINT_URL  cloud endpoint the operator dials out to (default dev-cc.stackgres.best)
#   - STACKGRES_IMAGE_TAG     operator image tag (default: the dev build baked in at upload time)
#   - STACKGRES_K8S_BRANCH    branch to pull the helm chart from (default wip-matriarch)
#   - NAMESPACE               namespace to install into (default stackgres)

# --- config / defaults ---
export STACKGRES_ENDPOINT_URL="${STACKGRES_ENDPOINT_URL:-dev-cc.stackgres.best}"
[ -n "$OTT" ] && export STACKGRES_TOKEN="$OTT"
STACKGRES_IMAGE_NAME="registry.gitlab.com/ongresinc/stackgres/stackgres/operator"
STACKGRES_IMAGE_TAG="${STACKGRES_IMAGE_TAG:-%STACKGRES_IMAGE_TAG%}"
STACKGRES_K8S_BRANCH="${STACKGRES_K8S_BRANCH:-wip-matriarch}"
NAMESPACE="${NAMESPACE:-stackgres}"
CHART_SUBPATH="stackgres-k8s/install/helm/stackgres-operator"

# --- helper functions for logs ---
info()  { echo "$@"; }
warn()  { echo 'WARNING: ' "$@" >&2; }
fatal() { echo 'ERROR: ' "$@" >&2; exit 1; }

# --- verify required tooling and a reachable cluster (no git needed) ---
verify_prereqs() {
    for bin in kubectl helm curl tar; do
        command -v "$bin" >/dev/null 2>&1 || fatal "'$bin' is required but was not found in PATH"
    done
    kubectl cluster-info >/dev/null 2>&1 \
        || fatal 'kubectl cannot reach a cluster - check your kubeconfig / current-context'
    info "Installing into cluster '$(kubectl config current-context 2>/dev/null || echo unknown)' (namespace: ${NAMESPACE})"
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
    [ -n "$STACKGRES_TOKEN" ] || fatal 'Could not parse exchange response from the cloud'
    export STACKGRES_TOKEN
}

# --- download ONLY the operator helm chart subdir into /tmp (GitLab subdir archive; no clone) ---
fetch_chart() {
    TMP_DIR=$(mktemp -d -t stackgres-k8s.XXXXXXXXXX)
    cleanup() { code=$?; rm -rf "$TMP_DIR"; exit $code; }
    trap cleanup INT EXIT
    chart_url="https://gitlab.com/ongresinc/stackgres/-/archive/${STACKGRES_K8S_BRANCH}/stackgres.tar.gz?path=${CHART_SUBPATH}"
    info "Fetching operator helm chart (${STACKGRES_K8S_BRANCH}) ..."
    curl -sfL "$chart_url" -o "${TMP_DIR}/chart.tar.gz" \
        || fatal "Failed to download the chart from ${chart_url}"
    tar -xzf "${TMP_DIR}/chart.tar.gz" -C "$TMP_DIR" --strip-components=1 \
        || fatal 'Failed to extract the chart archive'
    rm -f "${TMP_DIR}/chart.tar.gz"
    CHART_DIR="${TMP_DIR}/${CHART_SUBPATH}"
    [ -f "${CHART_DIR}/Chart.yaml" ] || fatal "chart not found at ${CHART_DIR} (branch ${STACKGRES_K8S_BRANCH})"
}

# --- install/upgrade the operator, wired to the cloud ---
install_operator() {
    [ -n "$STACKGRES_TOKEN" ] || fatal 'No install token - pass OTT=<token> (from the cloud UI)'
    info "Installing StackGres operator (${STACKGRES_IMAGE_NAME}:${STACKGRES_IMAGE_TAG}) ..."
    helm upgrade --install stackgres-operator "$CHART_DIR" \
        --create-namespace -n "$NAMESPACE" \
        --set-string operator.image.name="$STACKGRES_IMAGE_NAME" \
        --set-string operator.image.tag="$STACKGRES_IMAGE_TAG" \
        --set-string 'developer.extraOpts[0]=-Dstackgres.cloud.enabled=true' \
        --set-string "developer.extraOpts[1]=-DSTACKGRES_ENDPOINT_URL=${STACKGRES_ENDPOINT_URL}" \
        --set-string 'developer.extraOpts[2]=-Dstackgres.cloud.plaintext=false' \
        --set-string "developer.extraOpts[3]=-DSTACKGRES_TOKEN=${STACKGRES_TOKEN}" \
        --set-string "developer.extraEnv.SG_IMAGE_CLUSTER_CONTROLLER=registry.gitlab.com/ongresinc/stackgres/stackgres/cluster-controller:${STACKGRES_IMAGE_TAG}"
}

print_getting_started() {
    info ''
    info 'StackGres operator install submitted.'
    info "  watch:  kubectl -n ${NAMESPACE} rollout status deploy/stackgres-operator"
    info "  uplink: kubectl -n ${NAMESPACE} logs deploy/stackgres-operator | grep 'cloud uplink'"
}

# --- run the install process ---
{
    verify_prereqs
    maybe_exchange_install_token
    fetch_chart
    install_operator
    print_getting_started
}
