if [ ! -z "$REUSE_KIND" ]
then
  exit 0
fi

echo "Setting up kind environment"

kind delete cluster --name "$KIND_NAME" || true
cat << EOF > kind-config.yaml
kind: Cluster
apiVersion: kind.sigs.k8s.io/v1alpha3
nodes:
- role: control-plane
- role: worker
- role: worker
EOF

kind create cluster --name "$KIND_NAME" --config kind-config.yaml --image kindest/node:v${KUBERNETES_VERSION}


set-up-tiller.sh
