#!/bin/bash
echo "setting up kind environment"

reset(){
  reset-kind.sh

  LOCAL_PATH=`pwd`
  echo $LOCAL_PATH
  cd $STACKGRES_PATH/src
  load-operator-kind.sh

  cd $LOCAL_PATH
  helm install --name stackgres-operator --namespace stackgres $STACKGRES_PATH/install/helm/stackgres-operator

  wait-all-pods-ready.sh
}

reset
export -f reset

