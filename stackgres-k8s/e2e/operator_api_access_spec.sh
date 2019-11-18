#!/bin/bash
CLUSTER_NAMESPACE=api-test
TEMP_DIRECTORY=$(mktemp -d)
CLUSTER_NAME=api-cluster
source testlib.sh

bash deploy_curl_box.sh

remove_cluster_if_exists $CLUSTER_NAME
helm install --name $CLUSTER_NAME --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ > cluster.log

sleep 2

wait-all-pods-ready.sh


function check_cluster_in_list(){

  sleep 10 #Wait for cache to be refreshed

  CLUSTER_IN_RESPONSE=$(run_curl.sh -r "stackgres/cluster" | jq -c ".[] | select ( .metadata.namespace == \"$CLUSTER_NAMESPACE\") | select ( .metadata.name == \"$CLUSTER_NAME\")" | jq '.metadata.name' -r)
    
  if [ "$CLUSTER_IN_RESPONSE" == "$CLUSTER_NAME" ]
  then
    echo "Cluster $CLUSTER_NAME included in json response"
    exit 0
  else
    echo "Cluster $CLUSTER_NAME not included in json response"
    exit 1
  fi
}


function get_cluster_http_status(){

  HTTP_STATUS=$(run_curl.sh -r "stackgres/cluster/$CLUSTER_NAMESPACE/$CLUSTER_NAME" -e "-LI -o /dev/null -w %{http_code}")
  echo $HTTP_STATUS

}

function get_cluster_stats(){

  HTTP_STATUS=$(run_curl.sh -r "stackgres/cluster/status/$CLUSTER_NAMESPACE/$CLUSTER_NAME" -e "-LI -o /dev/null -w %{http_code}")
  echo $HTTP_STATUS

}

function check_cluster_directly(){

  HTTP_STATUS=$(get_cluster_http_status)

  if [ $HTTP_STATUS -eq 200 ]
  then
    echo "Cluster $CLUSTER_NAME was found bye the api"
    exit 0
  else
    echo "Cluster $CLUSTER_NAME was not found bye the api"
    exit 1
  fi

}

function test_cluster_stats_are_loaded(){
  
  HTTP_STATUS=$(get_cluster_stats)

  if [ $HTTP_STATUS -eq 200 ]
  then
    echo "Cluster $CLUSTER_NAME status was found by the api"
    exit 0
  else
    echo "Cluster $CLUSTER_NAME stats was not found by the api"
    exit 1
  fi

  
}

run_test "Check that a created cluster can be accessed directly through the API" check_cluster_directly

run_test "Check that a created cluster is included in the response" check_cluster_in_list

run_test "Check that the status endpoint are returning the pod stats" test_cluster_stats_are_loaded

function check_cluster_removed_from_list(){
  
  remove_cluster_if_exists $CLUSTER_NAME

  sleep 10 #Wait for cache to be refreshed

  CLUSTER_IN_RESPONSE=$(run_curl.sh -r "stackgres/cluster" | jq -c ".[] | select ( .metadata.namespace == \"$CLUSTER_NAMESPACE\") | select ( .metadata.name == \"$CLUSTER_NAME\")" | jq '.metadata.name' -r)

  if [ "$CLUSTER_IN_RESPONSE" == "$CLUSTER_NAME" ]
  then
    echo "Cluster $CLUSTER_NAME wasn't removed from cache";
    exit 1
  else 
    echo "Cluster $CLUSTER_NAME was removed from cache";
    exit 0
  fi

}

function check_cluster_deletion_directly(){

  remove_cluster_if_exists $CLUSTER_NAME

  HTTP_STATUS=$(run_curl.sh -r "stackgres/cluster/$CLUSTER_NAMESPACE/$CLUSTER_NAME" -e "-LI -o /dev/null -w %{http_code}")

  if [ $HTTP_STATUS -eq 404 ]
  then
    echo "Cluster $CLUSTER_NAME was not found by the api"
    exit 0
  else    
    echo "Cluster $CLUSTER_NAME was found by the api"
    exit 1
  fi

}

run_test "Check that a cluster was removed from cache after its deletion" check_cluster_removed_from_list
run_test "Check that a cluster was removed from direct access after its deletion" check_cluster_deletion_directly

function create_cluster_with_api(){

  create_or_replace_cluster $CLUSTER_NAME $CLUSTER_NAMESPACE 1

  wait-all-pods-ready.sh

  sleep 10

  kubectl get sgclusters.stackgres.io -n $CLUSTER_NAMESPACE $CLUSTER_NAME  -o json \
   | jq 'del(.metadata.creationTimestamp) | del(.metadata.generation) | del(.metadata.resourceVersion) | del(.metadata.selfLink) | del(.metadata.uid)'\
    > stackgres-cluster.json

  remove_cluster_if_exists $CLUSTER_NAME    

  helm install --name $CLUSTER_NAME --namespace $CLUSTER_NAMESPACE $STACKGRES_PATH/install/helm/stackgres-cluster/ --set cluster.create=false
  
  HTTP_STATUS=$(run_curl.sh -r "stackgres/cluster" -d stackgres-cluster.json -e '-H "Content-Type: application/json" -X POST -w %{http_code} -o /dev/null')

  if [ "$HTTP_STATUS" == "200" ] || [ "$HTTP_STATUS" == "202" ] || [ "$HTTP_STATUS" == "204" ]
  then
    echo "request acknowledged by the operator"
  else
    ERROR_RESPONSE=$(run_curl.sh -r "stackgres/cluster" -d stackgres-cluster.json -e '-H "Content-Type: application/json" -X POST')
    rm stackgres-cluster.json
    echo "Invalid response status $HTTP_STATUS. response: $ERROR_RESPONSE"
    exit 1
  fi

  sleep 10

  wait-all-pods-ready.sh

}

function test_cluster_create_with_api(){

  create_cluster_with_api

  CLUSTER_STATUS=$(kubectl get pod -n $CLUSTER_NAMESPACE $CLUSTER_NAME-0 -o jsonpath='{.status.phase}')

  if [ $CLUSTER_STATUS == "Running" ]
  then
    echo "Cluster created with the API"
  else
    echo "Cluster not created or in invalid status: $CLUSTER_STATUS"  
    exit 1
  fi
  
}

run_test "Cluster creation through the API" test_cluster_create_with_api

function api_created_cluster_visible(){

  create_cluster_with_api

  check_cluster_directly

}

run_test "Check that a cluster created with the API is visible" api_created_cluster_visible

function increate_cluster_instances_with_api(){

  kubectl get sgclusters.stackgres.io -n $CLUSTER_NAMESPACE $CLUSTER_NAME -o json | jq 'del(.metadata.creationTimestamp) | del(.metadata.generation) | del(.metadata.resourceVersion) | del(.metadata.selfLink) | del(.metadata.uid)' > stackgres-cluster.json

  TMP_JSON=$(mktemp)

  jq ".spec.instances = 2" stackgres-cluster.json > "$TMP_JSON" && mv "$TMP_JSON" stackgres-cluster.json
  
  HTTP_STATUS=$(run_curl.sh -r "stackgres/cluster" -d stackgres-cluster.json -e '-H "Content-Type: application/json" -X PUT -w %{http_code} -o /dev/null')

  if [ "$HTTP_STATUS" == "200" ] || [ "$HTTP_STATUS" == "202" ] || [ "$HTTP_STATUS" == "204" ]
  then
    echo "request acknowledged by the operator"
  else
    ERROR_RESPONSE=$(run_curl.sh -r "stackgres/cluster" -d stackgres-cluster.json -e '-H "Content-Type: application/json" -X PUT')
    rm stackgres-cluster.json
    echo "Invalid response status $HTTP_STATUS. response: $ERROR_RESPONSE"
    exit 1
  fi

  sleep 10

  wait-all-pods-ready.sh

}

function test_update_cluster_with_api(){
  
  create_or_replace_cluster $CLUSTER_NAME $CLUSTER_NAMESPACE 1
  
  increate_cluster_instances_with_api

  CLUSTER_INSTANCES=$(kubectl get sgclusters.stackgres.io -n $CLUSTER_NAMESPACE $CLUSTER_NAME -o jsonpath='{.spec.instances}')
  if [ "$CLUSTER_INSTANCES" == "2" ]
  then
    echo "Cluster instances were increased"
  else
    echo "Cluster instances weren't increase"
    exit 1
  fi
  
}

run_test "Cluster update with the api" test_update_cluster_with_api

function get_cluster_from_api(){
  echo $(run_curl.sh -r "stackgres/cluster/$CLUSTER_NAMESPACE/$CLUSTER_NAME")
}

function test_api_updated_cluster_is_visible(){

  create_or_replace_cluster $CLUSTER_NAME $CLUSTER_NAMESPACE 1

  sleep 10

  wait-all-pods-ready.sh

  increate_cluster_instances_with_api

  CLUSTER_INSTANCES=$(get_cluster_from_api | jq '.spec.instances' -r)

  if [ "$CLUSTER_INSTANCES" == "2" ]
  then
    echo "Cluster updates are being reflected in the api"
  else
    echo "Cluster updates aren't being reflected in the api"
    exit 1
  fi
  
}

run_test "Check that cluster changes are reflected in the api" test_api_updated_cluster_is_visible


function delete_cluster_with_api(){

  kubectl get sgclusters.stackgres.io -n $CLUSTER_NAMESPACE $CLUSTER_NAME -o json \
   | jq 'del(.metadata.creationTimestamp) | del(.metadata.generation) | del(.metadata.resourceVersion) | del(.metadata.selfLink) | del(.metadata.uid)' \
    > stackgres-cluster.json

  
  HTTP_STATUS=$(run_curl.sh -r "stackgres/cluster" -d stackgres-cluster.json -e '-H "Content-Type: application/json" -X DELETE -w %{http_code} -o /dev/null')

  if [ "$HTTP_STATUS" == "200" ] || [ "$HTTP_STATUS" == "202" ] || [ "$HTTP_STATUS" == "204" ]
  then
    echo "request acknowledged by the operator"
    rm stackgres-cluster.json
    sleep 10
    wait-all-pods-ready.sh
  else
    rm stackgres-cluster.json
    ERROR_RESPONSE=$(run_curl.sh -r "stackgres/cluster" -d stackgres-cluster.json -e '-H "Content-Type: application/json" -X PUT')
    echo "Invalid response status $HTTP_STATUS. response: $ERROR_RESPONSE"
    exit 1
  fi


}

function test_delete_cluster_with_api(){

  create_or_replace_cluster $CLUSTER_NAME $CLUSTER_NAMESPACE 

  delete_cluster_with_api

}

run_test "Check cluster deletion with api" test_delete_cluster_with_api

function test_api_delete_cluster_is_invible(){

  create_or_replace_cluster $CLUSTER_NAME $CLUSTER_NAMESPACE 

  delete_cluster_with_api

  HTTP_STATUS=$(get_cluster_http_status)

  if [ $HTTP_STATUS = "404" ]
  then
    echo "Cluster removed from the API"    
  else
    echo "Cluster wasn't removed from the API"
    exit 1
  fi

}

run_test "Check that cluster deletions with the API are reflected in the API" test_api_delete_cluster_is_invible