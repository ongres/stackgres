#!/bin/bash
echo "setting up kind environment"
source testlib.sh
reset(){
  reset-kind.sh -n $1

  deploy_operator
}

deploy_operator(){
  LOCAL_PATH=`pwd`
  echo $LOCAL_PATH
  cd $STACKGRES_PATH/src
  load-operator-kind.sh

  cd $LOCAL_PATH

  cd $LOCAL_PATH
  helm delete --purge stackgres-operator &> /dev/null
  helm template --name stackgres-operator --namespace stackgres $STACKGRES_PATH/install/helm/stackgres-operator | kubectl delete --ignore-not-found -f -
  helm install --name stackgres-operator --namespace stackgres $STACKGRES_PATH/install/helm/stackgres-operator

  wait-all-pods-ready.sh
}



clean_k8s(){
  
  helm ls -a | awk '{if(NR>1)print}' | awk '{print $1}' | grep -v stackgres-operator | while read RELEASE; do
    NAMESPACE=$(helm ls | grep $RELEASE | awk '{print $11}')
    
    remove_cluster_if_exists $RELEASE $NAMESPACE

    helm delete --purge $RELEASE || true
    if [ "$NAMESPACE" != "default" ] || [ "$NAMESPACE" != "kube-system" ] || [ "$NAMESPACE" != "kube-public" ]
    then
      echo "Deleting namespace $NAMESPACE"
      kubectl delete namespace $NAMESPACE  
    fi
  done

  echo "Deploying operator"
  deploy_operator

}

prepare_environment(){
  KIND_NAME="kind"
  if [ -z ${1+x} ]; then KIND_NAME=$1; fi
  if kind get clusters | grep $KIND_NAME
  then
    RELEASES=$(helm ls -a | awk '{if(NR>1)print}' | wc -l)
    if [ $RELEASES -gt 4 ]
    then
      reset $KIND_NAME
    else 
      clean_k8s
    fi 
  else
    reset $KIND_NAME
  fi
}
#reset
export -f reset
export -f clean_cluster
export -f remove_cluster_if_exists
export -f prepare_environment
