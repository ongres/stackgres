
#!/bin/bash
CLUSTER_NAMESPACE=pgconfigrem
TEMP_DIRECTORY=$(mktemp -d)
source testlib.sh

helm install --name $CLUSTER_NAMESPACE --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster > cluster.log

sleep 2

wait-all-pods-ready.sh

echo "INFO: Starting test"


function delete_with_cluster(){
  kubectl get -n $CLUSTER_NAMESPACE sgpgconfigs.stackgres.io postgresconf -o yaml | kubectl delete -f - &> ${TEMP_DIRECTORY}/test1.log

  if [ $? -eq 0 ]
  then
    echo "FAIL: It should not be able to delete sgspconfig"
    exit 1
  else
    echo "SUCCESS: Good it should have failed"  
  fi

  cat $TEMP_DIRECTORY/test1.log | grep "Can't delete sgpgconfigs.stackgres.io postgresconf" &> /dev/null
  if [ $? -eq 0 ]
  then
    exit 0

  else  
    ERROR=$(cat $TEMP_DIRECTORY/test1.log)
    echo "FAIL: Error is not what it should be. ERROR ${ERROR}"
    exit 1
  fi
}

run_test "Trying to delete pgconfig with cluster running" delete_with_cluster

function delete_whitout_cluster(){

  kubectl get -n $CLUSTER_NAMESPACE sgclusters.stackgres.io stackgres -o yaml | kubectl delete -f -

  sleep 10
  wait-all-pods-ready.sh

  kubectl get -n $CLUSTER_NAMESPACE sgpgconfigs.stackgres.io postgresconf -o yaml | kubectl delete -f - &> ${TEMP_DIRECTORY}/test2.log

  if [ $? -eq 0 ]
  then
    echo "SUCCESS: Good. It should have deleted the configuration"
    exit 0
  else
    ERROR=$(cat $TEMP_DIRECTORY/test2.log)
    echo "FAIL: It should be able to delete the posgresconf. ERROR ${ERROR}"
    exit 1
  fi
}

run_test "Trying to delete pgconfig without cluster running" delete_whitout_cluster


