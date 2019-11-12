#!/bin/bash
CLUSTER_NAMESPACE=replication
TEMP_DIRECTORY=$(mktemp -d)
CLUSTER_NAME=stackgres
source testlib.sh

helm delete --purge "$CLUSTER_NAMESPACE" || true
helm template --name $CLUSTER_NAMESPACE --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ | kubectl delete --namespace $CLUSTER_NAMESPACE --ignore-not-found -f -
helm install --name replication --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ --set cluster.instances=2 > cluster.log

sleep 2

wait-all-pods-ready.sh

echo "INFO: Starting test"
run_query.sh -p 5432 -h "$CLUSTER_NAME-primary" -i 1 -n "$CLUSTER_NAMESPACE" -q "'CREATE DATABASE test;'"

function create_database_replica(){

  run_query.sh -p 5432 -h "$CLUSTER_NAME-replica" -i 1 -n "$CLUSTER_NAMESPACE" -q "'CREATE TABLE fibonacci(num integer);'" -d test &> ${TEMP_DIRECTORY}/test1.log

  if [ $? -eq 0 ]
  then
    echo "FAIL: Its possible to create tables in the replica node"
    exit 1
  else
    echo "SUCCESS: Good it should not be able to create tables in the replica db" 
    exit 0
  fi

}
run_test "Checking that replication is working" create_database_replica

function insert_data(){
  run_query.sh -p 5432 -h "$CLUSTER_NAME-primary" -n "$CLUSTER_NAMESPACE" -i 1 -q "'CREATE TABLE fibonacci(num integer);'" -d test
  run_query.sh -p 5432 -h "$CLUSTER_NAME-primary" -n "$CLUSTER_NAMESPACE" -i 1 -q "'INSERT INTO fibonacci(num) VALUES (1);'" -d test
  run_query.sh -p 5432 -h "$CLUSTER_NAME-primary" -n "$CLUSTER_NAMESPACE" -i 1 -q "'INSERT INTO fibonacci(num) VALUES (2);'" -d test
  run_query.sh -p 5432 -h "$CLUSTER_NAME-primary" -n "$CLUSTER_NAMESPACE" -i 1 -q "'INSERT INTO fibonacci(num) VALUES (3);'" -d test

  PRIMARY_RESPONSE=$(run_query.sh -p 5432 -n "$CLUSTER_NAMESPACE" -h "$CLUSTER_NAME-primary" -i 1 -q "'SELECT num FROM fibonacci;'" -d "test" | paste -sd "" -)
  REPLICA_RESPONSE=$(run_query.sh -p 5432 -n "$CLUSTER_NAMESPACE" -h "$CLUSTER_NAME-replica" -i 1 -q "'SELECT num FROM fibonacci;'" -d "test" | paste -sd "" -)

  if [ "$PRIMARY_RESPONSE" = "123" ]
  then
    if [ "$PRIMARY_RESPONSE" = "$REPLICA_RESPONSE" ]
    then
      echo "SUCCESS: replication is working"
      exit 0
    else
      echo "FAIL: replication is not working. The records don't match in the fibonacci table"
      exit 1
    fi
  else
    echo "FAIL: inserts on the primary db where not sucessful."  
    exit 1
  fi
}

run_test "Data replication" insert_data
