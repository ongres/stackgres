#!/bin/sh

set -e

if [ "$PRIMARY_INSTANCE" = "$POD_NAME" ]
then
  if [ -f "$PG_UPGRADE_PATH/.patroni-reset-for-upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION" ]
  then
    echo "Patroni reset already performed"
  else
    PATRONI_INITIALIZATION="$(kubectl get endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
      --template='
        {{- if and .metadata.annotations }}
          {{- if .metadata.annotations.initialize }}true{{ end }}
        {{- end }}')"
    if [ "$PATRONI_INITIALIZATION" = "true" ]
    then
      echo "Resetting patroni"
      kubectl patch endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
        --type json -p '[{"op":"remove","path":"/metadata/annotations/initialize"}]'
    else
      echo "Patroni is already resetted"
    fi
    mkdir -p "$PG_UPGRADE_PATH"
    touch "$PG_UPGRADE_PATH/.patroni-reset-for-upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION"
  fi

  if [ -f "$PG_UPGRADE_PATH/.patroni-reset-history-for-upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION" ]
  then
    echo "Patroni history reset already performed"
  else
    PATRONI_HISTORY="$(kubectl get endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
      --template='
        {{- if and .metadata.annotations }}
          {{- if .metadata.annotations.history }}
            {{- if not (eq .metadata.annotations.history "[]") }}true{{ end }}
          {{- end }}
        {{- end }}')"
    if [ "$PATRONI_INITIALIZATION" = "true" ]
    then
      echo "Resetting patroni history"
      kubectl patch endpoints -n "$CLUSTER_NAMESPACE" "$PATRONI_ENDPOINT_NAME" \
        --type json -p '[{"op":"replace","path":"/metadata/annotations/history","value":"[]"}]'
    else
      echo "Patroni history is already resetted"
    fi
    mkdir -p "$PG_UPGRADE_PATH"
    touch "$PG_UPGRADE_PATH/.patroni-reset-history-for-upgrade-from-$SOURCE_VERSION-to-$TARGET_VERSION"
  fi
else
  echo "Skip resetting patroni"
fi
