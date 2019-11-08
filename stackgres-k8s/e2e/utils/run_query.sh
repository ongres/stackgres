CLUSTER=stackgres
INSTANCE=0
CLUSTER_NAMESPACE=$(kubectl get pods --all-namespaces | grep $CLUSTER-$INSTANCE | cut -d ' ' -f 1)
QUERY="'SELECT 1'"
PORT=5432
HOST=127.0.0.1
DATABASE=postgres

while getopts ":c:n:i:q:p:h:d:" opt; do
  case $opt in
    c) CLUSTER="$OPTARG"
    ;;
    n) CLUSTER_NAMESPACE="$OPTARG"
    ;;
    i) INSTANCE="$OPTARG"
    ;;
    q) QUERY="$OPTARG"
    ;;
    p) PORT="$OPTARG"
    ;;
    h) HOST="$OPTARG"
    ;;
    d) DATABASE="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

kubectl exec -t -n $CLUSTER_NAMESPACE $CLUSTER-$INSTANCE -c postgres-util -- sh -c "PGPASSWORD=$(kubectl -n $CLUSTER_NAMESPACE get secrets $CLUSTER -o jsonpath='{.data.superuser-password}' | base64 --decode) PGCONNECT_TIMEOUT=5 psql -t -A -U postgres -d $DATABASE -p $PORT -c $QUERY -h $HOST"

