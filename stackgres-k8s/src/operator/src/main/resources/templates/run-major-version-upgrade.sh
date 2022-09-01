#!/bin/sh

run_op() {
  set -e

  if [ "$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
    --template='{{ if .status.dbOps }}{{ if .status.dbOps.majorVersionUpgrade }}true{{ end }}{{ end }}')" != "true" ]
  then
    set_first_statefulset_instance_as_primary

    INITIAL_PODS="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_POD_LABELS" -o name)"
    INITIAL_INSTANCES="$(printf '%s' "$INITIAL_PODS" | cut -d / -f 2 | sort)"
    PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
    PRIMARY_INSTANCE="$(printf '%s' "$PRIMARY_POD" | cut -d / -f 2)"
    if [ "x$PRIMARY_INSTANCE" = "x" ] \
      || ! kubectl get pod -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -o name > /dev/null
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Primary instance not found!" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      return 1
    fi
    echo "Found primary instance $PRIMARY_INSTANCE"
    echo
    SOURCE_VERSION="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template='{{ .spec.postgres.version }}')"
    LOCALE="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" \
      -- psql -t -A -c "SHOW lc_collate")"
    ENCODING="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" \
      -- psql -t -A -c "SHOW server_encoding")"
    DATA_CHECKSUM="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" \
      -- psql -t -A -c "SELECT CASE WHEN current_setting('data_checksums')::bool THEN 'true' ELSE 'false' END")"

    if [ -z "${TARGET_VERSION}" ] || [ "${SOURCE_VERSION%%.*}" -ge "${TARGET_VERSION%%.*}" ]
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Can not perform major version upgrade from version $SOURCE_VERSION to version $TARGET_VERSION" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      exit 1
    fi

    echo "Signaling major version upgrade started to cluster"
    echo
    DB_OPS_PATCH="$(cat << EOF
      {
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
EOF
    )"

    until (
      DBOPS="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json)"
      DBOPS="$(printf '%s' "$DBOPS" | jq '.status.dbOps = '"$DB_OPS_PATCH")"
      printf '%s' "$DBOPS" | kubectl replace --raw /apis/"$CRD_GROUP"/v1/namespaces/"$CLUSTER_NAMESPACE"/"$CLUSTER_CRD_NAME"/"$CLUSTER_NAME" -f -
      )
    do
      sleep 1
    done
  else
    SOURCE_VERSION="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template='{{ .status.dbOps.majorVersionUpgrade.sourcePostgresVersion }}')"
    INITIAL_INSTANCES="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template='{{ .status.dbOps.majorVersionUpgrade.initialInstances }}')"
    INITIAL_INSTANCES="$(printf '%s' "$INITIAL_INSTANCES" | tr -d '[]' | tr ' ' '\n')"
    PRIMARY_INSTANCE="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template='{{ .status.dbOps.majorVersionUpgrade.primaryInstance }}')"

    until kubectl patch "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --type=json \
        -p "$(cat << EOF
[
  {"op":"replace","path":"/status/dbOps/majorVersionUpgrade/targetPostgresVersion","value": $TARGET_VERSION},
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

  if [ -z "$TARGET_BACKUP_PATH" ]
  then
    echo "Setting postgres version to $TARGET_VERSION and postgres config to $TARGET_POSTGRES_CONFIG..."
  else
    echo "Setting postgres version to $TARGET_VERSION, postgres config to $TARGET_POSTGRES_CONFIG and backup path to $TARGET_BACKUP_PATH..."
  fi
  echo
  until (
    CLUSTER="$(kubectl get "$CLUSTER_CRD_NAME.$CRD_GROUP" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json)"
    CLUSTER="$(printf '%s' "$CLUSTER" | jq '.spec.postgres.version = "'"$TARGET_VERSION"'"')"
    CLUSTER="$(printf '%s' "$CLUSTER" | jq '.spec.configurations.sgPostgresConfig = "'"$TARGET_POSTGRES_CONFIG"'"')"
    if [ -n "$TARGET_BACKUP_PATH" ]
    then
      CLUSTER="$(printf '%s' "$CLUSTER" | jq '
          if .spec.configurations.sgBackupConfig != null
          then .spec.configurations.backupPath = "'"$TARGET_BACKUP_PATH"'"
          else
            if .spec.configurations.backups != null and (.spec.configurations.backups | length) > 0
            then .spec.configurations.backups[0].path = "'"$TARGET_BACKUP_PATH"'"
            else .
            end
          end')"
    fi
    printf '%s' "$CLUSTER" | kubectl replace --raw /apis/"$CRD_GROUP"/v1/namespaces/"$CLUSTER_NAMESPACE"/"$CLUSTER_CRD_NAME"/"$CLUSTER_NAME" -f -
    )
  do
    sleep 1
  done
  echo "done"
  echo

  echo "Waiting StatefulSet to be updated..."
  echo
  while true
  do
    IS_STATEFULSET_UPDATED="$(kubectl get sts -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" -o json \
      | jq "(.spec.template.spec.initContainers | any(.name == \"$MAJOR_VERSION_UPGRADE_CONTAINER_NAME\"))
        and (.spec.template.metadata.annotations[\"$POSTGRES_VERSION_KEY\"] == \"$TARGET_VERSION\")")"
    if [ "$IS_STATEFULSET_UPDATED" = "true" ]
    then
      break
    fi
    sleep 1
  done
  echo "done"
  echo

  INITIAL_INSTANCES_COUNT="$(printf '%s' "$INITIAL_INSTANCES" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  echo "Initial instances:"
  echo "$INITIAL_INSTANCES" | sed 's/^/ - /'
  echo

  update_status init

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
  if [ "${PRIMARY_INSTANCE##*-}" != "0" ] \
     || ( [ "x$CURRENT_PRIMARY_INSTANCE" != "x" ] \
        && [ "$PRIMARY_INSTANCE" != "$CURRENT_PRIMARY_INSTANCE" ] )
  then
    echo "FAILURE=$NORMALIZED_OP_NAME failed. Please check pod $PRIMARY_INSTANCE logs for more info" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    exit 1
  fi

  if [ "$INITIAL_INSTANCES_COUNT" -gt 1 ]
  then
    echo "Downscaling cluster to 1 instance"
    create_event "DecreasingInstances" "Normal" "Decreasing instances of $CLUSTER_CRD_NAME $CLUSTER_NAME"
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
    create_event "InstancesDecreased" "Normal" "Decreased instances of $CLUSTER_CRD_NAME $CLUSTER_NAME"
    echo
  fi

  echo "Restarting primary instance $PRIMARY_INSTANCE..."
  create_event "RestartingPod" "Normal" "Restarting pod $PRIMARY_INSTANCE"

  kubectl delete pod -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE"

  echo "done"
  echo

  echo "Waiting primary instance $PRIMARY_INSTANCE major version upgrade..."

  wait_for_instance "$PRIMARY_INSTANCE"
  create_event "PodRestarted" "Normal" "Pod $PRIMARY_INSTANCE successfully restarted"

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
    create_event "IncreasingInstances" "Normal" "Increasing instances of $CLUSTER_CRD_NAME $CLUSTER_NAME"
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
    create_event "InstancesIncreased" "Normal" "Increased instances of $CLUSTER_CRD_NAME $CLUSTER_NAME"
    echo
  fi

  echo "Signaling major version upgrade finished to cluster"
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
      "sourcePostgresVersion": "$SOURCE_VERSION",
      "targetPostgresVersion": "$TARGET_VERSION",
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

set_first_statefulset_instance_as_primary() {
  PRIMARY_POD="$(kubectl get pods -n "$CLUSTER_NAMESPACE" -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
  PRIMARY_INSTANCE="$(printf '%s' "$PRIMARY_POD" | cut -d / -f 2)"
  if [ "${PRIMARY_INSTANCE##*-}" != "0" ]
  then
    TARGET_INSTANCE="${PRIMARY_INSTANCE%-*}-0"
    echo "Primary is not $TARGET_INSTANCE, doing switchover..."
    echo

    if [ "$INITIAL_INSTANCES_COUNT" = 1 ]
    then
      echo "Upscaling cluster to 2 instances"
      echo

      create_event "IncreasingInstances" "Normal" "Increasing instances of $CLUSTER_CRD_NAME $CLUSTER_NAME"

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

      create_event "InstancesIncreased" "Normal" "Increased instances of $CLUSTER_CRD_NAME $CLUSTER_NAME"

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

    create_event "SwitchoverInitiated" "Normal" "Switchover of $CLUSTER_CRD_NAME $CLUSTER_NAME initiated"

    kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c "$PATRONI_CONTAINER_NAME" -- \
      patronictl switchover "$CLUSTER_NAME" --master "$PRIMARY_INSTANCE" --candidate "$TARGET_INSTANCE" --force \

    echo "done"

    create_event "SwitchoverFinalized" "Normal" "Switchover of $CLUSTER_CRD_NAME $CLUSTER_NAME completed"
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
