listenoperator() {

  PODID=$(kubectl get pods --all-namespaces | grep stackgres-operator | grep -v stackgres-operator-init | grep -P '\w+-\w+-\w+-(\w+)' -o)
  STACKGRES_NAMESPACE=$(kubectl get pods --all-namespaces | grep stackgres-operator | grep -v stackgres-operator-init | cut -d ' ' -f 1)
  clear
  kubectl logs -f $PODID -n $STACKGRES_NAMESPACE stackgres-operator

}

until listenoperator
do
  sleep 2
done
