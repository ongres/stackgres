# Build the manager binary
ARG BASE_IMAGE
FROM $BASE_IMAGE

ENV HOME=/opt/helm
COPY stackgres-k8s/install/operator-sdk/stackgres-operator/watches.yaml ${HOME}/watches.yaml
COPY stackgres-k8s/install/operator-sdk/stackgres-operator/helm-charts/stackgres-operator  ${HOME}/helm-charts/stackgres-operator
WORKDIR ${HOME}
