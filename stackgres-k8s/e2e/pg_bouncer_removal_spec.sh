#!/bin/bash
CLUSTER_NAMESPACE=pgbouncerem
TEMP_DIRECTORY=$(mktemp -d)
source testlib.sh

helm install --name $CLUSTER_NAMESPACE --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ > cluster.log
sleep 2
wait-all-pods-ready.sh

echo "INFO: Starting test"

function delete_with_cluster(){

  kubectl get -n $CLUSTER_NAMESPACE sgconnectionpoolingconfigs.stackgres.io pgbouncerconf -o yaml | kubectl delete -f - &> ${TEMP_DIRECTORY}/test1.log

  if [ $? -eq 0 ]
  then
    echo "ERROR: It should not be able to delete pgbouncerconf"
    exit 1  
  fi

  cat $TEMP_DIRECTORY/test1.log | grep "Can't delete sgconnectionpoolingconfigs.stackgres.io pgbouncerconf" &> /dev/null
  if [ $? -eq 0 ]
  then
    exit 0
  else
    ERROR=$(cat $TEMP_DIRECTORY/test1.log)
    echo "FAIL: Error is not what it should be. ERROR: ${ERROR}"
    exit 1
  fi
}

run_test "Trying to delete sgconnectionpoolingconfigs with cluster running" delete_with_cluster

function delete_whitout_cluster(){
  kubectl get -n $CLUSTER_NAMESPACE sgclusters.stackgres.io stackgres -o yaml | kubectl delete -f - &> /dev/null
  sleep 10
  wait-all-pods-ready.sh

  kubectl get -n $CLUSTER_NAMESPACE sgconnectionpoolingconfigs.stackgres.io pgbouncerconf -o yaml | kubectl delete -f - &> ${TEMP_DIRECTORY}/test2.log

  if [ $? -eq 0 ]
  then
    exit 0
  else  
    ERROR=$(cat $TEMP_DIRECTORY/test2.log)
    echo "FAIL: It should be able to delete the pgbouncerconf. ERROR $ERROR"
    exit 1
  fi
}

run_test "Trying to delete sgconnectionpoolingconfigs without a cluster running" delete_whitout_cluster

