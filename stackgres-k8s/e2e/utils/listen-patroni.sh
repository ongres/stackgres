CLUSTER=stackgres
INSTANCE=0
CLUSTER_NAMESPACE=$(kubectl get pods --all-namespaces | grep $CLUSTER-$INSTANCE | cut -d ' ' -f 1)

while getopts ":c:n:i:" opt; do
  case $opt in
    c) CLUSTER="$OPTARG"
    ;;
    n) CLUSTER_NAMESPACE="$OPTARG"
    ;;
    i) INSTANCE="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

new_state(){

  clear
  kubectl logs -n $CLUSTER_NAMESPACE -f $CLUSTER-$INSTANCE patroni
  echo ""
}

while true;do
  new_state
  sleep 2
done
