# Build the manager binary
FROM $BASE_IMAGE

ENV HOME=/opt/helm
COPY stackgres-k8s/install/operator-sdk/stackgres-operator/watches.yaml ${HOME}/watches.yaml
COPY stackgres-k8s/install/operator-sdk/stackgres-operator/helm-charts  ${HOME}/helm-charts
WORKDIR ${HOME}
