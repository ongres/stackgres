if [ ! -z "$REUSE_KIND" ]
then
  exit 0
fi
kind delete cluster || true
KUBERNETES_VERSION="${KUBERNETES_VERSION:-1.12.10}"
cat << EOF > kind-config.yaml
kind: Cluster
apiVersion: kind.sigs.k8s.io/v1alpha3
nodes:
- role: control-plane
- role: worker
- role: worker
EOF

kind create cluster --config kind-config.yaml --image kindest/node:v${KUBERNETES_VERSION}


set-up-tiller.sh
