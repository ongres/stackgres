#!/bin/bash
reset(){
  reset-kind.sh

  LOCAL_PATH=`pwd`
  echo $LOCAL_PATH
  cd $STACKGRES_PATH/src
  load-operator-kind.sh

  cd $LOCAL_PATH
  helm list -a
  helm list -a \
    | tail -n +2 \
    | sed 's/\s\+/ /g' \
    | cut -d ' ' -f 1 \
    | xargs -r -n 1 -I % helm delete --purge %
  helm template --name stackgres-operator --namespace stackgres $STACKGRES_PATH/install/helm/stackgres-operator \
    | kubectl delete -f - --ignore-not-found
  kubectl get namespace -o name \
    | grep -v "\(default\|kube-system\|kube-public\)" \
    | xargs -r -n 1 -I % -P 0 kubectl delete %
  kubectl get validatingwebhookconfigurations.admissionregistration.k8s.io -o name \
    | xargs -r -n 1 -I % kubectl delete %
  helm install --name stackgres-operator --namespace stackgres $STACKGRES_PATH/install/helm/stackgres-operator

  wait-all-pods-ready.sh
}

reset
export -f reset

