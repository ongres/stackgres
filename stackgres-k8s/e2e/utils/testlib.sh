#!/bin/bash
function run_test(){
    TEST_NAME=$1
    LOG_FILE=${TEST_NAME//" "/"-"}.log
    echo "Running test $TEST_NAME"
    ($2) &> $LOG_FILE
    if [ $? -eq 0 ]
    then
      cat $LOG_FILE
      echo "$TEST_NAME. SUCCESS."
    else
      cat $LOG_FILE
      echo "$TEST_NAME. FAIL. See file $LOG_FILE.log for details"
      exit 1
    fi
}

function spec(){
    SPEC_FILE=$1
    SPEC_NAME=$(basename "$SPEC_FILE" .sh)
    echo "Running $SPEC_NAME tests"
    bash $SPEC_FILE > $SPEC_NAME.log
    if [ $? -eq 0 ]
    then
      echo "$SPEC_NAME. SUCCESS." >> results.log
    else
      echo "$SPEC_NAME. FAIL. See file $SPEC_NAME.log for details" >> results.log
    fi
}

function remove_cluster(){

  RELEASE=$1

  NAMESPACE=$(helm ls | grep $RELEASE | awk '{print $11}')

  echo "Deleting release $RELEASE"
  helm template --name $RELEASE --namespace $NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ | kubectl delete --namespace $NAMESPACE --ignore-not-found -f - &> /dev/null
  helm delete --purge $RELEASE

  if [ "$NAMESPACE" != "default" ] || [ "$NAMESPACE" != "kube-system" ] || [ "$NAMESPACE" != "kube-public" ]
  then

    if kubectl get namespaces $NAMESPACE &> /dev/null
    then
      echo "Deleting namespace $NAMESPACE"
      kubectl delete namespace $NAMESPACE
    fi

  fi

  wait-all-pods-ready.sh
}

function remove_cluster_if_exists(){

  RELEASE=$1

  if helm get $RELEASE &> /dev/null
  then
  
    remove_cluster $RELEASE

  fi
  
}

function create_or_replace_cluster(){

  RELEASE=$1
  NAMESPACE=$2
    
  if [ -z $3 ]
  then 
    INSTANCES=1
  else
    INSTANCES=$3
  fi
  
  if helm get $RELEASE &> /dev/null
  then
    INSTALLED_NAMESPACE=$(helm ls | grep $RELEASE | awk '{print $11}')
    
    if [ "$INSTALLED_NAMESPACE" = "$NAMESPACE" ]
    then

      helm upgrade $RELEASE $STACKGRES_PATH/install/helm/stackgres-cluster/ --set cluster.instances=$INSTANCES

    else

      remove_cluster $RELEASE
      helm install --name $RELEASE --namespace $NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ --set cluster.instances=$INSTANCES
    fi      
    
  else
    helm install --name $RELEASE --namespace $NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ --set cluster.instances=$INSTANCES
  fi   

  sleep 10
  wait-all-pods-ready.sh 

}