POD=busybox-curl
NAMESPACE=stackgres
SERVICE=stackgres-operator
RESOURCE_PATH="stackgres/cluster"
EXTRA_PARAMETERS=""
DATA_FILENAME=""
while getopts ":p:n:s:r:e:d:" opt; do
  case $opt in
    p) POD="$OPTARG"
    ;;
    n) NAMESPACE="$OPTARG"
    ;;
    s) SERVICE="$OPTARG"
    ;;
    r) RESOURCE_PATH="$OPTARG"
    ;;
    e) EXTRA_PARAMETERS="$OPTARG"
    ;;
    d) DATA_FILENAME="$OPTARG";
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

if [ -n "${DATA_FILENAME}" ]
then
  tar cf - $DATA_FILENAME | kubectl exec -i busybox-curl -- tar xf - -C /tmp
  kubectl exec -t busybox-curl -- sh -c "curl -k https://$SERVICE.$NAMESPACE.svc.cluster.local/$RESOURCE_PATH  --data @/tmp/$DATA_FILENAME -s $EXTRA_PARAMETERS"
else
  kubectl exec -t busybox-curl -- sh -c "curl -k https://$SERVICE.$NAMESPACE.svc.cluster.local/$RESOURCE_PATH -s $EXTRA_PARAMETERS"  
fi

