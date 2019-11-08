#!/bin/bash

while true; do

  if kubectl get pods --all-namespaces  | grep tiller | grep Running | grep 1/1; then
    echo "Tiller loaded"
    break
  fi

  sleep 1

done
