PODID=$(kubectl get pods --all-namespaces | grep stackgres-operator-init | grep -P '\w+-\w+-\w+-(\w+)' -o)
STACKGRES_NAMESPACE=$(kubectl get pods --all-namespaces | grep stackgres-operator-init | cut -d ' ' -f 1)
kubectl logs -f $PODID -n $STACKGRES_NAMESPACE
