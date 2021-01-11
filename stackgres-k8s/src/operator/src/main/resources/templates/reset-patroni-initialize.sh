#!/bin/sh

if [ "$PRIMARY_INSTANCE" = "$POD_NAME" ]
then
  if [ "$(kubectl get endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
    --template '{{ if .metadata.annotations.initialize }}true{{ end }}')" = "true" ]
  then
    echo "Resetting patroni initialize"
    kubectl patch endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
      --type json -p '[{"op":"remove","path":"/metadata/annotations/initialize"}]'
  else
    echo "Patroni initialize already resetted"
  fi
else
  echo "Skip resetting patroni initialize"
fi
