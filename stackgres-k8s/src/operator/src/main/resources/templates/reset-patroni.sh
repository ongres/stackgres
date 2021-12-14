#!/bin/sh

set -e

if [ "$PRIMARY_INSTANCE" = "$POD_NAME" ]
then
  PATRONI_INITIALIZATION="$(kubectl get endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
    --template='
      {{- if and .metadata.annotations }}
        {{- if .metadata.annotations.initialize }}true{{ end }}
      {{- end }}')"
  PATRONI_HISTORY="$(kubectl get endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
    --template='
      {{- if and .metadata.annotations }}
        {{- if .metadata.annotations.history }}true{{ end }}
      {{- end }}')"
  if [ "$PATRONI_INITIALIZATION" = "true" ]
  then
    echo "Resetting patroni"
    kubectl patch endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
      --type json -p '[{"op":"remove","path":"/metadata/annotations/initialize"}]'
    if [ "$PATRONI_HISTORY" = "true" ]
    then
      echo "Resetting patroni hitory too"
      kubectl patch endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
        --type json -p '[{"op":"replace","path":"/metadata/annotations/history","value":[]}]'
    fi
  else
    echo "Patroni already resetted"
  fi
else
  echo "Skip resetting patroni"
fi
