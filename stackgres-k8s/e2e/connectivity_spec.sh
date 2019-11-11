#!/bin/bash
CLUSTER_NAMESPACE=connectivity
TEMP_DIRECTORY=$(mktemp -d)
CLUSTER_NAME=stackgres
source testlib.sh

helm install --name $CLUSTER_NAMESPACE --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ --set cluster.instances=2 > cluster.log

sleep 2

wait-all-pods-ready.sh

echo "INFO: Starting test"

function ports_check(){

  RESPONSE_5432=$(run_query.sh -i 0 -p 5432 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
  RESPONSE_5433=$(run_query.sh -i 0 -p 5433 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
  RESPONSE_5434=$(run_query.sh -i 0 -p 5434 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
  RESPONSE_5435=$(run_query.sh -i 0 -p 5435 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)

  if [ "$RESPONSE_5432" == "1" ] && [ "$RESPONSE_5433" == "1" ] && [ "$RESPONSE_5434" == "1" ] && [ "$RESPONSE_5435" == "1" ]
  then
    
    RESPONSE_5432=$(run_query.sh -i 1 -p 5432 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    RESPONSE_5433=$(run_query.sh -i 1 -p 5433 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    RESPONSE_5434=$(run_query.sh -i 1 -p 5434 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    RESPONSE_5435=$(run_query.sh -i 1 -p 5435 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    
    if [ "$RESPONSE_5432" == "1" ] && [ "$RESPONSE_5433" == "1" ] && [ "$RESPONSE_5434" == "1" ] && [ "$RESPONSE_5435" == "1" ]
    then
      exit 0
    else 
      echo "FAIL: Not all 4 ports of the replica node are working"
      exit 1
    fi
    
  else 
      echo "FAIL: Not all 4 ports of the primary node are working"
      exit 1
  fi

}

run_test "Checking that all 4 ports (5432, 5433, 5434, 5435) in the patroni pods are openned and listening for queries" ports_check

function service_check(){
  RESPONSE_PRIMARY=$(run_query.sh -h $CLUSTER_NAME-primary -i 1 -p 5432 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)

  if [ "$RESPONSE_PRIMARY" == "1" ]
  then

    RESPONSE_REPLICA=$(run_query.sh -h $CLUSTER_NAME-replica -i 0 -p 5432 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    if [ "$RESPONSE_REPLICA" == "1" ]
    then
      echo "SUCCESS: Connections are possible using services"
      exit 0
    else
      echo "FAIL: Cannot connect to replica db using a kubernetes service"
      exit 1
    fi  

  else
    echo "FAIL: Cannot connect to primary db using a kubernetes service"
    exit 1
  fi
}

run_test "Checking that is possible to connect using services is working" service_check