#!/bin/bash
CLUSTER_NAMESPACE=sgprofilerem
TEMP_DIRECTORY=$(mktemp -d)
source testlib.sh

helm install --name $CLUSTER_NAMESPACE --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ > cluster.log

sleep 2

wait-all-pods-ready.sh

echo "INFO: Starting test"

function delete_with_cluster(){
  kubectl get -n $CLUSTER_NAMESPACE sgprofiles.stackgres.io size-xs -o yaml | kubectl delete -f - &> ${TEMP_DIRECTORY}/test1.log

  if [ $? -eq 0 ]
  then
    echo "INFO: It should not be able to delete size-xs"
    exit 1
  else
    echo "SUCCESS: Good it should have failed"  
    exit 0
  fi

  cat $TEMP_DIRECTORY/test1.log | grep "Can't delete sgprofiles.stackgres.io size-xs" &> /dev/null
  if [ $? -eq 0 ]
  then
    echo "SUCCESS: Error message was the expected"
    exit 0
  else
    ERROR=$(cat $TEMP_DIRECTORY/test1.log)
    echo "FAIL: Error is not what it should be. ERROR ${ERROR}"
    exit 1
  fi
}

run_test "Trying to delete size-xs with cluster running" delete_with_cluster

function delete_whitout_cluster(){

  kubectl get -n $CLUSTER_NAMESPACE sgclusters.stackgres.io stackgres -o yaml | kubectl delete -f -

  sleep 10
  wait-all-pods-ready.sh

  kubectl get -n $CLUSTER_NAMESPACE sgprofiles.stackgres.io size-xs -o yaml | kubectl delete -f - &> ${TEMP_DIRECTORY}/test2.log

  if [ $? -eq 0 ]
  then
    echo "SUCCESS: Good. It should have deleted the configuration"  
    exit 0
  else
    ERROR=$(cat $TEMP_DIRECTORY/test2.log)
    echo "FAIL: It should be able to delete the size-xs. ERROR ${ERROR}"
    exit 1
  fi

}

run_test "Trying to delete size-xs without cluster running" delete_whitout_cluster