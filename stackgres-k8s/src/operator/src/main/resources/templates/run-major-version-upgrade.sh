#!/bin/sh

run_op() {
  set -e

  if [ "$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
    --template='{{ if .status.dbOps }}{{ if .status.dbOps.majorVersionUpgrade }}true{{ end }}{{ end }}')" != "true" ]
  then
    INITIAL_PODS="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name)"
    INITIAL_INSTANCES="$(printf '%s' "$INITIAL_PODS" | cut -d / -f 2 | sort)"
    PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
    PRIMARY_INSTANCE="$(printf '%s' "$PRIMARY_POD" | cut -d / -f 2)"
    if ! kubectl get pod -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -o name > /dev/null
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Primary instance not found!" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      return 1
    fi
    echo "Found primary instance $PRIMARY_INSTANCE"
    echo
    SOURCE_IMAGE="$(kubectl get pod -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" \
      --template="{{ range .spec.containers }}{{ if eq .name \"$PATRONI_CONTAINER_NAME\" }}{{ .image }}{{ end }}{{ end }}")"
    SOURCE_VERSION="$(printf '%s' "$SOURCE_IMAGE" | sed 's/^.*-pg\([0-9]\+\.[0-9]\+\)-.*$/\1/')"
    TARGET_VERSION="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template='{{ .spec.postgresVersion }}')"
    LOCALE="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c patroni \
      -- psql -t -A -c "SHOW lc_collate")"
    ENCODING="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" \
      -- psql -t -A -c "SHOW server_encoding")"
    DATA_CHECKSUM="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" \
      -- psql -t -A -c "SELECT CASE WHEN current_setting('data_checksums')::bool THEN 'true' ELSE 'false' END")"

    if ! [ "${SOURCE_VERSION%%.*}" -lt "${TARGET_VERSION%%.*}" ]
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Can not perform major version upgrade from version $SOURCE_VERSION to version $TARGET_VERSION" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      exit 1
    fi

    echo "Signaling major version upgrade started to cluster"
    echo
    DB_OPS_PATCH="$(cat << EOF
      {
        "dbOps": {
          "majorVersionUpgrade":{
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
            "primaryInstance": "$PRIMARY_INSTANCE",
            "sourcePostgresVersion": "$SOURCE_VERSION",
            "targetPostgresVersion": "$TARGET_VERSION",
            "locale": "$LOCALE",
            "encoding": "$ENCODING",
            "dataChecksum": $DATA_CHECKSUM,
            "link": $LINK,
            "clone": $CLONE,
            "check": $CHECK
          }
        }
      }
EOF
    )"

    until kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json | \
     jq '.status |= . + '"$DB_OPS_PATCH" |
     kubectl replace --raw /apis/"$CRD_GROUP"/v1/namespaces/"$CLUSTER_NAMESPACE"/"$CLUSTER_CRD_NAME"/"$CLUSTER_NAME"/status -f -
    do
      kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json | \
      jq 'del(.status.dbOps)' | \
      kubectl replace --raw /apis/"$CRD_GROUP"/v1/namespaces/"$CLUSTER_NAMESPACE"/"$CLUSTER_CRD_NAME"/"$CLUSTER_NAME"/status -f -
    done
  else
    INITIAL_INSTANCES="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template='{{ .status.dbOps.majorVersionUpgrade.initialInstances }}')"
    INITIAL_INSTANCES="$(printf '%s' "$INITIAL_INSTANCES" | tr ',' '\n')"
    PRIMARY_INSTANCE="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template='{{ .status.dbOps.majorVersionUpgrade.primaryInstance }}')"

    until kubectl patch "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --type=json \
        -p "$(cat << EOF
[
  {"op":"replace","path":"/status/dbOps/majorVersionUpgrade/link","value": $LINK},
  {"op":"replace","path":"/status/dbOps/majorVersionUpgrade/clone","value": $CLONE},
  {"op":"replace","path":"/status/dbOps/majorVersionUpgrade/check","value": $CHECK}
]
EOF
        )"
    do
      sleep 1
    done
  fi

  while true
  do
    IS_STATEFULSET_UPDATED="$(kubectl get sts -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template="{{ range .spec.template.spec.initContainers }}{{ if eq .name \"$MAJOR_VERSION_UPGRADE_CONTAINER_NAME\" }}true{{ end }}{{ end }}")"
    if [ "$IS_STATEFULSET_UPDATED" = "true" ]
    then
      break
    fi
    sleep 1
  done

  INITIAL_INSTANCES_COUNT="$(printf '%s' "$INITIAL_INSTANCES" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  echo "Initial instances:"
  echo "$INITIAL_INSTANCES" | sed 's/^/ - /'
  echo

  update_status init

  if [ "${PRIMARY_INSTANCE##*-}" != "0" ]
  then
    TARGET_INSTANCE="${PRIMARY_INSTANCE%-*}-0"
    echo "Primary is not $TARGET_INSTANCE, doing switchover..."
    echo

    if [ "$INITIAL_INSTANCES_COUNT" = 1 ]
    then
      echo "Upscaling cluster to 2 instances"
      echo

      kubectl patch "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --type=json \
        -p "$(cat << EOF
[
  {"op":"replace","path":"/spec/instances","value":2}
]
EOF
          )"

      echo "Waiting cluster upscale"
      echo

      echo "Waiting instance $INSTANCE to become ready..."

      wait_for_instance "$TARGET_INSTANCE"

      echo "done"
      echo

      echo "Cluster upscale done"
      echo
    fi
    PREVIOUS_PRIMARY_INSTANCE="$PRIMARY_INSTANCE"
    if ! kubectl wait pod -n "$CLUSTER_NAMESPACE" "$TARGET_INSTANCE" --for condition=Ready --timeout 0 >/dev/null 2>&1
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Primary instance not found!" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      exit 1
    fi

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
      exit 1
    fi
  fi

  if [ "$INITIAL_INSTANCES_COUNT" -gt 1 ]
  then
    echo "Downscaling cluster to 1 instance"
    echo

    kubectl patch "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --type=json \
      -p "$(cat << EOF
[
  {"op":"replace","path":"/spec/instances","value":1}
]
EOF
        )"

    echo "Waiting cluster downscale..."

    until [ "$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name | cut -d / -f 2)" = "$PRIMARY_INSTANCE" ]
    do
      sleep 1
    done

    echo "done"
    echo
  fi

  echo "Restarting primary instance $PRIMARY_INSTANCE..."

  kubectl delete pod -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE"

  echo "done"
  echo

  echo "Waiting primary instance $PRIMARY_INSTANCE major version upgrade..."

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

  if [ "$INITIAL_INSTANCES_COUNT" -gt 1 ]
  then
    echo "Upscaling cluster to $INITIAL_INSTANCES_COUNT instances"
    echo

    kubectl patch "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --type=json \
      -p "$(cat << EOF
[
  {"op":"replace","path":"/spec/instances","value":$INITIAL_INSTANCES_COUNT}
]
EOF
        )"

    echo "Waiting cluster upscale"
    echo

    for INSTANCE in $INITIAL_INSTANCES
    do
      if [ "$INSTANCE" = "$PRIMARY_INSTANCE" ]
      then
        continue
      fi

      echo "Waiting instance $INSTANCE to become ready..."

      wait_for_instance "$INSTANCE"

      echo "done"
      echo

      update_status
    done

    echo "Cluster upscale done"
    echo
  fi

  echo "Signaling major version upgrade finished to cluster"
  echo

  until kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json | \
      jq 'del(.status.dbOps)' | \
      kubectl replace --raw /apis/"$CRD_GROUP"/v1/namespaces/"$CLUSTER_NAMESPACE"/"$CLUSTER_CRD_NAME"/"$CLUSTER_NAME"/status -f -
  do
    sleep 1
  done
}

update_status() {
  STATEFULSET_UPDATE_REVISION="$(kubectl get sts -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
    --template='{{ .status.updateRevision }}')"
  if [ "$1" = "init" ]
  then
    PENDING_TO_RESTART_INSTANCES="$INITIAL_INSTANCES"
  else
    PENDING_TO_RESTART_INSTANCES="$(echo "$INITIAL_INSTANCES" \
      | while read INSTANCE
        do
          PODS="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name)"
          if ! printf '%s' "$PODS" | cut -d / -f 2 | grep -q "^$INSTANCE$"
          then
            echo "$INSTANCE"
            continue
          fi
          PATRONI_STATUS="$(kubectl get pod -n "$CLUSTER_NAMESPACE" "$INSTANCE" \
            --template='{{ .metadata.annotations.status }}')"
          POD_STATEFULSET_REVISION="$(kubectl get pod -n "$CLUSTER_NAMESPACE" "$INSTANCE" \
            --template='{{ index .metadata.labels "controller-revision-hash" }}')"
          if [ "$STATEFULSET_UPDATE_REVISION" != "$POD_STATEFULSET_REVISION" ] \
            || echo "$PATRONI_STATUS" | grep -q '"pending_restart":true'
          then
            echo "$INSTANCE"
          fi
        done)"
  fi
  PENDING_TO_RESTART_INSTANCES_COUNT="$(echo "$PENDING_TO_RESTART_INSTANCES" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  EXISTING_PODS="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name)"
  RESTARTED_INSTANCES="$(echo "$EXISTING_PODS" | cut -d / -f 2 | grep -v "^\($(
      echo "$PENDING_TO_RESTART_INSTANCES" | tr '\n' ' ' | sed '{s/ $//;s/ /\\|/g}'
    )\)$" | sort)"
  RESTARTED_INSTANCES_COUNT="$(echo "$RESTARTED_INSTANCES" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  echo "Pending to $NORMALIZED_OP_NAME instances:"
  if [ "$PENDING_TO_RESTART_INSTANCES_COUNT" = 0 ]
  then
    echo '<none>'
  else
    echo "$PENDING_TO_RESTART_INSTANCES" | sed 's/^/ - /'
  fi
  echo

  OPERATION="$(kubectl get "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" \
    --template='{{ if .status.majorVersionUpgrade }}replace{{ else }}add{{ end }}')"
  kubectl patch "$DB_OPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DB_OPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"$OPERATION","path":"/status/majorVersionUpgrade","value":{
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
