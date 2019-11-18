if [ ! -z "$REUSE_KIND" ]
then
  exit 0
fi

KIND_NAME="kind"

while getopts ":n:" opt; do
  case $opt in
    n) KIND_NAME="$OPTARG"
    ;;    
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

if kind get clusters | grep $KIND_NAME
then
  kind delete cluster --name $KIND_NAME || true
fi

KUBERNETES_VERSION="${KUBERNETES_VERSION:-1.12.10}"
cat << EOF > kind-config.yaml
kind: Cluster
apiVersion: kind.sigs.k8s.io/v1alpha3
nodes:
- role: control-plane
- role: worker
- role: worker
EOF

kind create cluster --name $KIND_NAME --config kind-config.yaml --image kindest/node:v${KUBERNETES_VERSION}

export KUBECONFIG="$(kind get kubeconfig-path --name="$KIND_NAME")"
set-up-tiller.sh
