#!/bin/bash
echo "setting up kind environment"

reset(){
  reset-kind.sh

  LOCAL_PATH=`pwd`
  echo $LOCAL_PATH
  cd $STACKGRES_PATH/src
  load-operator-kind.sh

  cd $LOCAL_PATH
  helm delete --purge stackgres-operator
  helm template --name stackgres-operator --namespace stackgres $STACKGRES_PATH/install/helm/stackgres-operator | kubectl delete --ignore-not-found -f -
  kubectl get pvc -A -o yaml|kubectl delete -f -
  helm install --name stackgres-operator --namespace stackgres $STACKGRES_PATH/install/helm/stackgres-operator

  wait-all-pods-ready.sh
}

reset
export -f reset

