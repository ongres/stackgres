#!/bin/sh

run_op() {
  set -e

  START_TIMESTAMP="$(date +%s)"

  if [ "$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
    --template="{{ if .status.dbOps }}{{ if .status.dbOps.$OP_NAME }}true{{ end }}{{ end }}")" != "true" ]
  then
    INITIAL_PODS="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name)"
    INITIAL_INSTANCES="$(printf '%s' "$INITIAL_PODS" | cut -d / -f 2 | sort)"
    PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
    PRIMARY_INSTANCE="$(printf '%s' "$PRIMARY_POD" | cut -d / -f 2)"

    echo "Signaling $NORMALIZED_OP_NAME started to cluster"
    echo

    DB_OPS_PATCH="$(cat << EOF
      {
        "dbOps": {
          "$OP_NAME":{
            "initialInstances": [$(
              FIRST=true
              for INSTANCE in $INITIAL_INSTANCES
              do
                if "$FIRST"
                then
                  printf '%s' "\"$INSTANCE\""
                  FIRST=false
                else
                  printf '%s' ",\"$INSTANCE\""
                fi
              done
              )],
            "primaryInstance": "$PRIMARY_INSTANCE"
          }
        }
      }
EOF
    )"
    until (
      DBOPS="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json)"
      DBOPS="$(printf '%s' "$DBOPS" | jq '.status |= . + '"$DB_OPS_PATCH")"
      printf '%s' "$DBOPS" | kubectl replace --raw /apis/"$CRD_GROUP"/v1/namespaces/"$CLUSTER_NAMESPACE"/"$CLUSTER_CRD_NAME"/"$CLUSTER_NAME" -f -
      )
    do
      (
      DBOPS="$(printf '%s' "$DBOPS" | kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json)"
      DBOPS="$(printf '%s' "$DBOPS" | jq 'del(.status.dbOps)')"
      printf '%s' "$DBOPS" | kubectl replace --raw /apis/"$CRD_GROUP"/v1/namespaces/"$CLUSTER_NAMESPACE"/"$CLUSTER_CRD_NAME"/"$CLUSTER_NAME" -f -
      )
    done

  else
    INITIAL_INSTANCES="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template="{{ .status.dbOps.$OP_NAME.initialInstances }}")"
    INITIAL_INSTANCES="$(printf '%s' "$INITIAL_INSTANCES" | tr -d '[]' | tr ' ' '\n')"
    PRIMARY_INSTANCE="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template="{{ .status.dbOps.$OP_NAME.primaryInstance }}")"

    CURRENT_PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
    CURRENT_PRIMARY_INSTANCE="$(printf '%s' "$CURRENT_PRIMARY_POD" | cut -d / -f 2)"
    if [ "$PRIMARY_INSTANCE" != "$CURRENT_PRIMARY_INSTANCE" ]
    then
      PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
      PRIMARY_INSTANCE="$(printf '%s' "$PRIMARY_POD" | cut -d / -f 2)"
    fi
  fi

  if ! kubectl get pod -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -o name > /dev/null
  then
    echo FAILURE="Primary instance not found!" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    return 1
  fi
  echo "Found primary instance $PRIMARY_INSTANCE"
  echo

  INITIAL_INSTANCES_COUNT="$(printf '%s' "$INITIAL_INSTANCES" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  echo "Initial instances:"
  if [ "$INITIAL_INSTANCES_COUNT" = 0 ]
  then
    echo '<none>'
  else
    echo "$INITIAL_INSTANCES" | sed 's/^/ - /'
  fi
  echo

  update_status

  if [ "$RESTART_PRIMARY_FIRST" = "true" ]
  then
    echo "Restarting primary inscante first..."

    kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" -- \
      patronictl restart "$CLUSTER_NAME" -r master --force \

    echo "Waiting primary instance $PRIMARY_INSTANCE to be ready..."

    wait_for_instance "$PRIMARY_INSTANCE"

    CURRENT_PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
    CURRENT_PRIMARY_INSTANCE="$(printf '%s' "$CURRENT_PRIMARY_POD" | cut -d / -f 2)"
    if [ "$PRIMARY_INSTANCE" != "$CURRENT_PRIMARY_INSTANCE" ]
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Please check pod $PRIMARY_INSTANCE logs for more info" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      return 1
    fi

    echo "done"
    echo

    update_status
  fi

  if [ "$REDUCED_IMPACT" = "true" ]
  then
    echo "Upscaling cluster to $((INITIAL_INSTANCES_COUNT + 1)) instances"
    echo

    kubectl patch "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --type=json \
      -p "$(cat << EOF
[
  {"op":"replace","path":"/spec/instances","value":$((INITIAL_INSTANCES_COUNT + 1))}
]
EOF
        )"

    INSTANCE="${PRIMARY_INSTANCE%-*}-$INITIAL_INSTANCES_COUNT"

    echo "Waiting instance $INSTANCE to be ready..."

    wait_for_instance "$INSTANCE"

    echo "done"
    echo

    update_status
  fi

  echo "Restarting replicas"
  echo

  while true
  do
    if [ "$PENDING_TO_RESTART_REPLICAS_COUNT" = 0 ]
    then
      break
    fi

    INSTANCE="$(printf '%s' "$PENDING_TO_RESTART_REPLICAS" | head -n 1)"

    echo "Restarting instance $INSTANCE..."

    kubectl delete pod -n "$CLUSTER_NAMESPACE" "$INSTANCE"

    echo "done"
    echo

    echo "Waiting instance $INSTANCE to be ready..."

    wait_for_instance "$INSTANCE"

    echo "done"
    echo

    update_status
  done

  if echo "$PENDING_TO_RESTART_INSTANCES" | grep -q '^'"$PRIMARY_INSTANCE"'$'
  then
    PREVIOUS_PRIMARY_INSTANCE="$PRIMARY_INSTANCE"
    TARGET_INSTANCE="$(echo "$RESTARTED_REPLICAS" | head -n 1)"
    if kubectl wait pod -n "$CLUSTER_NAMESPACE" "$TARGET_INSTANCE" --for condition=Ready --timeout 0 >/dev/null 2>&1
    then
      echo "Performing switchover from primary $PRIMARY_INSTANCE to replica $TARGET_INSTANCE..."

      kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" -- \
        patronictl switchover "$CLUSTER_NAME" --master "$PRIMARY_INSTANCE" --candidate "$TARGET_INSTANCE" --force \

      echo "done"
      echo

      PRIMARY_INSTANCE="$TARGET_INSTANCE"

      echo "Waiting primary instance $PRIMARY_INSTANCE to be ready..."

      wait_for_instance "$PRIMARY_INSTANCE"

      echo "done"
      echo

      RETRY=3
      while [ "$RETRY" != 0 ]
      do
        CURRENT_PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
        if [ -n "$CURRENT_PRIMARY_POD" ]
        then
          break
        fi
        RETRY="$((RETRY-1))"
        sleep 5
      done
      CURRENT_PRIMARY_INSTANCE="$(printf '%s' "$CURRENT_PRIMARY_POD" | cut -d / -f 2)"
      if [ "$PRIMARY_INSTANCE" != "$CURRENT_PRIMARY_INSTANCE" ]
      then
        echo "FAILURE=$NORMALIZED_OP_NAME failed. Please check pod $PRIMARY_INSTANCE logs for more info" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
        return 1
      fi
    fi

    if [ "$PREVIOUS_PRIMARY_INSTANCE" = "$PRIMARY_INSTANCE" ]
    then
      echo "Restarting instance $PREVIOUS_PRIMARY_INSTANCE..."
    else
      echo "Restarting primary instance $PREVIOUS_PRIMARY_INSTANCE..."
    fi

    kubectl delete pod -n "$CLUSTER_NAMESPACE" "$PREVIOUS_PRIMARY_INSTANCE"

    echo "done"
    echo

    if [ "$PREVIOUS_PRIMARY_INSTANCE" = "$PRIMARY_INSTANCE" ]
    then
      echo "Waiting instance $PREVIOUS_PRIMARY_INSTANCE to be ready..."
    else
      echo "Waiting primary instance $PREVIOUS_PRIMARY_INSTANCE to be ready..."
    fi

    wait_for_instance "$PREVIOUS_PRIMARY_INSTANCE"

    echo "done"
    echo

    update_status
  fi

  if [ "$REDUCED_IMPACT" = "true" ]
  then
    echo "Downscaling cluster to $INITIAL_INSTANCES_COUNT instances"
    echo

    kubectl patch "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --type=json \
      -p "$(cat << EOF
[
  {"op":"replace","path":"/spec/instances","value":$INITIAL_INSTANCES_COUNT}
]
EOF
        )"

    echo "Waiting cluster downscale..."

    until [ "$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name | cut -d / -f 2 | sort)" = "$INITIAL_INSTANCES" ]
    do
      sleep 1
    done

    echo "done"
    echo
  fi

  echo "Signaling $NORMALIZED_OP_NAME finished to cluster"
  echo

  until kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json | \
    jq 'del(.status.dbOps)' | \
    kubectl replace --raw /apis/"$CRD_GROUP"/v1/namespaces/"$CLUSTER_NAMESPACE"/"$CLUSTER_CRD_NAME"/"$CLUSTER_NAME" -f -
  do
    sleep 1
  done
}

update_status() {
  STATEFULSET_UPDATE_REVISION="$(kubectl get sts -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
    --template='{{ .status.updateRevision }}')"
  PENDING_TO_RESTART_INSTANCES="$(echo "$INITIAL_INSTANCES" \
    | while read -r INSTANCE
      do
        PODS="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name)"
        if ! printf '%s' "$PODS" | cut -d / -f 2 | grep -q "^$INSTANCE$"
        then
          echo "$INSTANCE"
          continue
        fi
        if [ "$ONLY_PENDING_RESTART" != true ]
        then
          POD_CREATION_TIMESTAMP="$(kubectl get pod -n "$CLUSTER_NAMESPACE" "$INSTANCE" \
            --template '{{ .metadata.creationTimestamp }}')"
          POD_CREATION_TIMESTAMP="$(from_date_iso8601_to_unix_timestamp "$POD_CREATION_TIMESTAMP")"
          if [ "$POD_CREATION_TIMESTAMP" -lt "$START_TIMESTAMP" ]
          then
            echo "$INSTANCE"
          fi
        else
          CLUSTER_POD_STATUS="$(kubectl get "$CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
            -o=jsonpath='{ .status.podStatuses[?(@.name == "'"$INSTANCE"'")].pendingRestart }')"
          PATRONI_STATUS="$(kubectl get pod -n "$CLUSTER_NAMESPACE" "$INSTANCE" \
            --template='{{ .metadata.annotations.status }}')"
          POD_STATEFULSET_REVISION="$(kubectl get pod -n "$CLUSTER_NAMESPACE" "$INSTANCE" \
            --template='{{ index .metadata.labels "controller-revision-hash" }}')"
          if [ "$CLUSTER_POD_STATUS" = true ] \
            || echo "$PATRONI_STATUS" | grep -q '"pending_restart":true' \
            || [ "$STATEFULSET_UPDATE_REVISION" != "$POD_STATEFULSET_REVISION" ]
          then
            echo "$INSTANCE"
          fi
        fi
      done)"
  PENDING_TO_RESTART_INSTANCES_COUNT="$(echo "$PENDING_TO_RESTART_INSTANCES" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  PENDING_TO_RESTART_REPLICAS="$(printf '%s' "$PENDING_TO_RESTART_INSTANCES" | grep -v '^'"$PRIMARY_INSTANCE"'$' || true)"
  PENDING_TO_RESTART_REPLICAS_COUNT="$(printf '%s' "$PENDING_TO_RESTART_REPLICAS" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  EXISTING_PODS="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name)"
  RESTARTED_INSTANCES="$(echo "$EXISTING_PODS" | cut -d / -f 2 | grep -v "^\($(
      echo "$PENDING_TO_RESTART_INSTANCES" | tr '\n' ' ' | sed '{s/ $//;s/ /\\|/g}'
    )\)$" | sort)"
  RESTARTED_INSTANCES_COUNT="$(echo "$RESTARTED_INSTANCES" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  RESTARTED_REPLICAS="$(echo "$RESTARTED_INSTANCES" | grep -v '^'"$PRIMARY_INSTANCE"'$' || true)"
  RESTARTED_REPLICAS_COUNT="$(echo "$RESTARTED_REPLICAS" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  echo "Pending to $NORMALIZED_OP_NAME instances:"
  if [ "$PENDING_TO_RESTART_INSTANCES_COUNT" = 0 ]
  then
    echo '<none>'
  else
    echo "$PENDING_TO_RESTART_INSTANCES" | sed 's/^/ - /'
  fi
  echo

  OPERATION="$(kubectl get "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" \
    --template="{{ if .status.$OP_NAME }}replace{{ else }}add{{ end }}")"
  kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"$OPERATION","path":"/status/$OP_NAME","value":{
      "primaryInstance": "$PRIMARY_INSTANCE",
      "initialInstances": [$(
        FIRST=true
        for INSTANCE in $INITIAL_INSTANCES
        do
          if "$FIRST"
          then
            printf '%s' "\"$INSTANCE\""
            FIRST=false
          else
            printf '%s' ",\"$INSTANCE\""
          fi
        done
        )],
      "pendingToRestartInstances": [$(
        FIRST=true
        for INSTANCE in $PENDING_TO_RESTART_INSTANCES
        do
          if "$FIRST"
          then
            printf '%s' "\"$INSTANCE\""
            FIRST=false
          else
            printf '%s' ",\"$INSTANCE\""
          fi
        done
        )],
      "restartedInstances": [$(
        FIRST=true
        for INSTANCE in $RESTARTED_INSTANCES
        do
          if "$FIRST"
          then
            printf '%s' "\"$INSTANCE\""
            FIRST=false
          else
            printf '%s' ",\"$INSTANCE\""
          fi
        done
        )]
    }
  }
]
EOF
    )"
}

wait_for_instance() {
  local INSTANCE="$1"
  until kubectl get pod -n "$CLUSTER_NAMESPACE" "$INSTANCE" -o name >/dev/null 2>&1
  do
    sleep 1
  done
  until kubectl wait pod -n "$CLUSTER_NAMESPACE" "$INSTANCE" --for condition=Ready --timeout 0 >/dev/null 2>&1
  do
    PHASE="$(kubectl get pod -n "$CLUSTER_NAMESPACE" "$INSTANCE" --template='{{ .status.phase }}')"
    if [ "$PHASE" = "Failed" ] || [ "$PHASE" = "Unknown" ]
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Please check pod $INSTANCE logs for more info" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      return 1
    fi
    sleep 1
  done
}
