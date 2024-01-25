#!/bin/sh

run_op() {
  set -e

  update_status init

  echo "Starting sharded dbops $NORMALIZED_OP_NAME"

  kubectl annotate "$SHARDED_CLUSTER_CRD_NAME" "$SHARDED_CLUSTER_NAME" \
    --overwrite "$VERSION_KEY=$OPERATOR_VERSION"

  rm -f /tmp/current-dbops
  local CLUSTER_NAME
  local DBOPS_NAME
  for CLUSTER_NAME in $CLUSTER_NAMES
  do
    DBOPS_NAME="${SHARDED_DBOPS_NAME}-${CLUSTER_NAME#${SHARDED_CLUSTER_NAME}-}"
    echo "Creating $DBOPS_CRD_KIND $DBOPS_NAME for $CLUSTER_CRD_KIND $CLUSTER_NAME"
    echo "$DBOPS_NAME" >> /tmp/current-dbops
    DBOPS_YAML="$(cat << EOF
apiVersion: $DBOPS_CRD_APIVERSION
kind: $DBOPS_CRD_KIND
metadata:
  namespace: $CLUSTER_NAMESPACE
  name: $DBOPS_NAME
  labels: $DBOPS_LABELS_JSON
  ownerReferences:
  - apiVersion: $SHARDED_DBOPS_CRD_APIVERSION
    kind: $SHARDED_DBOPS_CRD_KIND
    name: $SHARDED_DBOPS_NAME
    uid: $SHARDED_DBOPS_UID
spec:
  sgCluster: $CLUSTER_NAME
  op: securityUpgrade
  securityUpgrade:
    method: $METHOD
EOF
)"
    if ! printf %s "$DBOPS_YAML" | kubectl create -f - > /tmp/dbops-create-dbops 2>&1
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Can not create SGDbOps: $(cat /tmp/dbops-create-dbops)" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      exit 1
    fi
    cat /tmp/dbops-create-dbops
  done

  echo "Waiting for SGDbOps $(cat /tmp/current-dbops | tr '\n' ' ' | tr -s ' ') to complete"
  touch /tmp/completed-dbops
  while true
  do
    local COMPLETED=true
    for DBOPS_NAME in $(cat /tmp/current-dbops)
    do
      if ! grep -qxF "$DBOPS_NAME" /tmp/completed-dbops
      then
        DBOPS_STATUS="$(kubectl get "$DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DBOPS_NAME" \
          --template '{{ range .status.conditions }}{{ if eq .status "True" }}{{ .type }}{{ end }}{{ end }}')"
        if ! printf %s "$DBOPS_STATUS" | grep -q "^\($DBOPS_COMPLETED\|$DBOPS_FAILED\)$"
        then
          COMPLETED=false
          continue
        fi
        printf %s "$DBOPS_NAME" >> /tmp/completed-dbops
        update_status
        if printf %s "$DBOPS_STATUS" | grep -q "^$DBOPS_FAILED$"
        then
          echo "...$DBOPS_NAME failed"
          echo "FAILURE=$NORMALIZED_OP_NAME failed. SGDbOps $DBOPS_NAME failed" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
          exit 1
        fi
        echo "...$DBOPS_NAME completed"
      fi
    done
    if "$COMPLETED"
    then
      break
    fi
    sleep 2
  done

  echo "Sharded DbOps $NORMALIZED_OP_NAME completed"
}

update_status() {
  if [ "$1" = "init" ]
  then
    PENDING_TO_RESTART_CLUSTERS="$CLUSTER_NAMES"
    RESTARTED_CLUSTERS=""
  else
    DBOPS_STATUSES="$(kubectl get "$DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" -l "$DBOPS_LABELS" \
      --template '{{ range .items }}{{ .spec.sgCluster }}/{{ range .status.conditions }}{{ if eq .status "True" }}{{ .type }}{{ end }}{{ end }}{{ "\n" }}{{ end }}')"
    PENDING_TO_RESTART_CLUSTERS="$(echo "$CLUSTER_NAMES" | tr ' ' '\n' | grep -vxF '' \
      | while read CLUSTER
        do
          if ! printf '%s' "$DBOPS_STATUSES" | cut -d / -f 1 | grep -q "^$CLUSTER$" \
            || ! printf '%s' "$DBOPS_STATUSES" | grep -q "^$CLUSTER/$DBOPS_COMPLETED$"
          then
            echo "$CLUSTER"
          fi
        done)"
    RESTARTED_CLUSTERS="$(echo "$CLUSTER_NAMES" | tr ' ' '\n' \
      | while read CLUSTER
        do
          if printf '%s' "$DBOPS_STATUSES" | grep -q "^$CLUSTER/$DBOPS_COMPLETED$"
          then
            echo "$CLUSTER"
          fi
        done)"
  fi
  PENDING_TO_RESTART_CLUSTERS_COUNT="$(echo "$PENDING_TO_RESTART_CLUSTERS" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  echo "Pending to $NORMALIZED_OP_NAME clusters:"
  if [ "$PENDING_TO_RESTART_CLUSTERS_COUNT" = 0 ]
  then
    echo '<none>'
  else
    echo "$PENDING_TO_RESTART_CLUSTERS" | tr ' ' '\n' | grep -vxF '' | sed 's/^/ - /'
  fi
  echo

  OPERATION="$(kubectl get "$SHARDED_DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_DBOPS_NAME" \
    --template='{{ if .status.securityUpgrade }}replace{{ else }}add{{ end }}')"
  kubectl patch "$SHARDED_DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_DBOPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"$OPERATION","path":"/status/securityUpgrade","value":{
      "pendingToRestartSgClusters": [$(
        FIRST=true
        for CLUSTER in $PENDING_TO_RESTART_CLUSTERS
        do
          if "$FIRST"
          then
            printf '%s' "\"$CLUSTER\""
            FIRST=false
          else
            printf '%s' ",\"$CLUSTER\""
          fi
        done
        )],
      "restartedSgClusters": [$(
        FIRST=true
        for CLUSTER in $RESTARTED_CLUSTERS
        do
          if "$FIRST"
          then
            printf '%s' "\"$CLUSTER\""
            FIRST=false
          else
            printf '%s' ",\"$CLUSTER\""
          fi
        done
        )]
    }
  }
]
EOF
    )"
}
