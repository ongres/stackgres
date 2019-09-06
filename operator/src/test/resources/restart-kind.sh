#!/bin/bash
set -e
CONTAINER_NAME="$(docker inspect -f '{{.Name}}' "$(hostname)"|cut -d '/' -f 2)"
#echo "Installing kubectl"
#wget -q -L -O /bin/kubectl https://storage.googleapis.com/kubernetes-release/release/v1.12.10/bin/linux/amd64/kubectl
#chmod a+x /bin/kubectl
#echo "Installing kind"
#wget -q -L -O /bin/kind  https://github.com/kubernetes-sigs/kind/releases/download/v0.5.1/kind-$(uname)-amd64
#chmod a+x /bin/kind
#echo "Installing helm"
#wget -q -L https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz -O -|tar xz --strip-components=1 -C /bin -f - linux-amd64/helm
kind delete cluster --name "$CONTAINER_NAME" || true
kind create cluster --name "$CONTAINER_NAME" --image kindest/node:v1.12.10
sed -i 's#^    server:.*$#    server: 'https://"$(docker inspect -f '{{ .NetworkSettings.IPAddress }}' kind-control-plane)"':6443#' "$(kind get kubeconfig-path --name="$CONTAINER_NAME")"
export KUBECONFIG="$(kind get kubeconfig-path --name="$CONTAINER_NAME")"
echo "export KUBECONFIG='$(kind get kubeconfig-path --name="$CONTAINER_NAME")'" > /root/.bashrc
cat << 'EOF' | kubectl apply -f -
kind: ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1beta1
metadata:
  name: tiller-clusterrolebinding
subjects:
- kind: ServiceAccount
  name: default
  namespace: kube-system
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: ""
EOF
helm init --history-max 20
while ! helm version > /dev/null 2>&1; do sleep 0.5; done
echo "Kind started k8s cluster"