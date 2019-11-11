#!/bin/bash
while true; do
  PODS_NOT_READY=$(kubectl get pods --all-namespaces | grep -v Running | grep -v Completed | wc -l)
  if [[ $PODS_NOT_READY == "1" ]]; then
    RUNNING_CONTAINERS=$(kubectl get pods --all-namespaces | grep "Running" | grep -P '\d+/\d+' -o | cut -d '/' -f 1)
    EXPECTED_CONTAINERS=$(kubectl get pods --all-namespaces | grep "Running" | grep -P '\d+/\d+' -o | cut -d '/' -f 2)
    if [[ $RUNNING_CONTAINERS == $EXPECTED_CONTAINERS ]]; then
      break
    fi
  fi
  sleep 1
done
echo "all pods ready"
