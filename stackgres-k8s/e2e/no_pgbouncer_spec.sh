#!/bin/bash
CLUSTER_NAMESPACE=nopgbouncer
TEMP_DIRECTORY=$(mktemp -d)
CLUSTER_NAME=stackgres
source testlib.sh

helm delete --purge "$CLUSTER_NAMESPACE" || true
helm template --name $CLUSTER_NAMESPACE --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ | kubectl delete --namespace $CLUSTER_NAMESPACE --ignore-not-found -f -
helm install --name nopgbouncer --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ --set cluster.instances=2 --set sidecar.pooling=false  > cluster.log

sleep 2

wait-all-pods-ready.sh

echo "INFO: Starting test"

function ports_check(){
  RESPONSE_5432=$(run_query.sh -i 0 -p 5432 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
  RESPONSE_5433=$(run_query.sh -i 0 -p 5433 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
  RESPONSE_5434=$(run_query.sh -i 0 -p 5434 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)

  if [ "$RESPONSE_5432" == "1" ] && [ "$RESPONSE_5433" == "1" ] && [ "$RESPONSE_5434" == "1" ]
  then
    
    RESPONSE_5432=$(run_query.sh -i 1 -p 5432 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    RESPONSE_5433=$(run_query.sh -i 1 -p 5433 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    RESPONSE_5434=$(run_query.sh -i 1 -p 5434 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    
    if [ "$RESPONSE_5432" == "1" ] && [ "$RESPONSE_5433" == "1" ] && [ "$RESPONSE_5434" == "1" ]
    then
      echo "SUCCESS: All ports are ok"
    else 
      echo "FAIL: Not all 3 ports of the replica node are working"
      exit 1
    fi
    
  else 
      echo "FAIL: Not all 3 ports of the primary node are working"
      exit 1
  fi
}

run_test "Checking that all 3 ports (5432, 5433, 5434) in the patroni pods are openned and listeing for queries" ports_check

function service_check(){
  RESPONSE_PRIMARY=$(run_query.sh -h $CLUSTER_NAME-primary -i 1 -p 5432 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)

  if [ "$RESPONSE_PRIMARY" == "1" ]
  then

    RESPONSE_REPLICA=$(run_query.sh -h $CLUSTER_NAME-replica -i 0 -p 5432 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE)
    if [ "$RESPONSE_REPLICA" == "1" ]
    then
      echo "SUCCESS: Connections are possible using services"   
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

function check_pgbouncer(){
  run_query.sh -i 0 -p 5435 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE &> /dev/null

  if [ $? -eq 0 ]
  then
    echo "FAIL: pgbouncer port on primary server is open"
    exit 1
  else
    
    run_query.sh -i 1 -p 5435 -c $CLUSTER_NAME -n $CLUSTER_NAMESPACE &> /dev/null
    if [ $? -eq 0 ]
    then
      echo "FAIL: pgbouncer port on replica server is open"
      exit 1
    else
      echo "SUCCESS: pgbouncer connections are closed"
    fi
  fi
}

run_test "Checking that pgbouncer port is not open" check_pgbouncer