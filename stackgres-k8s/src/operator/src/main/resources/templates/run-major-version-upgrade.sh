#!/bin/sh

set +e

touch "$SHARED_PATH/major-version-upgrade.out"
touch "$SHARED_PATH/major-version-upgrade.err"

try_lock() {
  local WAIT="$1"
  local TEMPLATE='
  LOCK_POD={{ if .metadata.annotations.lockPod }}{{ .metadata.annotations.lockPod }}{{ else }}{{ end }}
  LOCK_TIMESTAMP={{ if .metadata.annotations.lockTimestamp }}{{ .metadata.annotations.lockTimestamp }}{{ else }}0{{ end }}
  RESOURCE_VERSION={{ .metadata.resourceVersion }}
  '
  kubectl get "$CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --template "$TEMPLATE" > /tmp/sgcluster
  . /tmp/sgcluster
  CURRENT_TIMESTAMP="$(date +%s)"
  if [ "$POD_NAME" != "$LOCK_POD" ] && [ "$((CURRENT_TIMESTAMP-LOCK_TIMESTAMP))" -lt 15 ]
  then
    echo "Locked already by $LOCK_POD at $(date -d @"$LOCK_TIMESTAMP" --iso-8601=seconds --utc)"
    if "$WAIT"
    then
      sleep 20
      try_lock true
    else
      return 1
    fi
  fi
  if ! kubectl annotate "$CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
    --resource-version "$RESOURCE_VERSION" --overwrite "lockPod=$POD_NAME" "lockTimestamp=$CURRENT_TIMESTAMP"
  then
    kubectl get "$CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" --template "$TEMPLATE" > /tmp/sgcluster
    . /tmp/sgcluster
    if [ "$POD_NAME" = "$LOCK_POD" ]
    then
      try_lock "$WAIT"
      return 0
    fi
    echo "Locked by $LOCK_POD at $(date -d @"$LOCK_TIMESTAMP" --iso-8601=seconds --utc)"
    if "$WAIT"
    then
      sleep 20
      try_lock true
    else
      return 1
    fi
  fi
}

dbops_update_status() {
  PENDING_TO_RESTART_INSTANCES="$(echo "$INITIAL_INSTANCES" \
    | while read INSTANCE
      do
        if ! kubectl get -n "$CLUSTER_NAMESPACE" pods -l "$CLUSTER_POD_LABELS" -o name | cut -d / -f 2 | grep -q "^$INSTANCE$"
        then
          echo "$INSTANCE"
        fi
      done)"
  RESTARTED_PODS="$(kubectl get -n "$CLUSTER_NAMESPACE" pods -l "$CLUSTER_POD_LABELS" -o name)"
  RESTARTED_INSTANCES="$(echo "$RESTARTED_PODS" | cut -d / -f 2 | sort)"
  while true
  do
    OPERATION="$(kubectl get -n "$CLUSTER_NAMESPACE" "$DB_OPS_CRD_NAME" "$DB_OPS_NAME" \
      --template '{{ if .status.majorVersionUpgrade }}replace{{ else }}add{{ end }}')"
    kubectl patch -n "$CLUSTER_NAMESPACE" "$DB_OPS_CRD_NAME" "$DB_OPS_NAME" --type=json \
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
            echo -n "\"$INSTANCE\""
            FIRST=false
          else
            echo -n ",\"$INSTANCE\""
          fi
        done
        )],
      "pendingToRestartInstances": [$(
        FIRST=true
        for INSTANCE in $PENDING_TO_RESTART_INSTANCES
        do
          if "$FIRST"
          then
            echo -n "\"$INSTANCE\""
            FIRST=false
          else
            echo -n ",\"$INSTANCE\""
          fi
        done
        )],
      "restartedInstances": [$(
        FIRST=true
        for INSTANCE in $PENDING_TO_RESTART_INSTANCES
        do
          if "$FIRST"
          then
            echo -n "\"$INSTANCE\""
            FIRST=false
          else
            echo -n ",\"$INSTANCE\""
          fi
        done
        )]
    }
  }
]
EOF
      )"
    break
  done
}

wait_for_instance() {
  local INSTANCE="$1"
  until kubectl wait -n "$CLUSTER_NAMESPACE" pod "$INSTANCE" --for condition=Ready --timeout 0
  do
    PHASE="$(kubectl get -n "$CLUSTER_NAMESPACE" pod "$INSTANCE" --template '{{ .status.phase }}')"
    if [ "$PHASE" = "Failed" ] || [ "$PHASE" = "Unknown" ]
    then
      echo "FAILURE=Major version upgrade failed. Please check pod $INSTANCE logs for more info" >> "$SHARED_PATH/major-version-upgrade.out"
      exit 1
    fi
    sleep 1
  done
}

try_lock true > /tmp/try-lock
echo "Lock acquired"
(
while true
do
  sleep 5
  try_lock false > /tmp/try-lock
done
) &
TRY_LOCK_PID=$!

tail -q -f "$SHARED_PATH/major-version-upgrade.out" "$SHARED_PATH/major-version-upgrade.err" &

TAIL_PID="$!"

sleep "$TIMEOUT" &

TIMEOUT_PID="$!"

(
set -e

if [ "$(kubectl get -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" \
  --template='{{ if .status.dbOps }}{{ if .status.dbOps.majorVersionUpgrade }}true{{ end }}{{ end }}')" != "true" ]
then
  INITIAL_PODS="$(kubectl get -n "$CLUSTER_NAMESPACE" pods -l "$CLUSTER_POD_LABELS" -o name)"
  INITIAL_INSTANCES="$(echo "$INITIAL_PODS" | cut -d / -f 2 | sort)"
  PRIMARY_POD="$(kubectl get -n "$CLUSTER_NAMESPACE" pods -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
  PRIMARY_INSTANCE="$(echo "$PRIMARY_POD" | cut -d / -f 2)"
  if ! kubectl get -n "$CLUSTER_NAMESPACE" pod "$PRIMARY_INSTANCE" -o name > /dev/null
  then
    echo FAILURE="Primary instance not found!" >> "$SHARED_PATH/major-version-upgrade.out"
    exit 1
  fi
  echo "Found primary instance $PRIMARY_INSTANCE"
  echo
  SOURCE_IMAGE="$(kubectl get -n "$CLUSTER_NAMESPACE" pod "$PRIMARY_INSTANCE" \
    --template "{{ range .spec.containers }}{{ if eq .name \"$PATRONI_CONTAINER_NAME\" }}{{ .image }}{{ end }}{{ end }}")"
  SOURCE_VERSION="$(echo "$SOURCE_IMAGE" | sed 's/^.*-pg\([0-9]\+\.[0-9]\+\)-.*$/\1/')"
  TARGET_VERSION="$(kubectl get -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" \
    --template '{{ .spec.postgresVersion }}')"
  LOCALE="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c patroni \
    -- psql -t -A -c "SHOW lc_collate")"
  ENCODING="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c patroni \
    -- psql -t -A -c "SHOW server_encoding")"
  DATA_CHECKSUM="$(kubectl exec -n "$CLUSTER_NAMESPACE" "$PRIMARY_INSTANCE" -c patroni \
    -- psql -t -A -c "SELECT CASE WHEN current_setting('data_checksums')::bool THEN 'true' ELSE 'false' END")"

  echo "Signaling major version upgrade started to cluster"
  echo

  until kubectl patch -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" --type=json \
      -p "$(cat << EOF
[
  {"op":"add","path":"/status/dbOps","value": {
      "majorVersionUpgrade":{
        "initialInstances": "$(echo "$INITIAL_INSTANCES" | tr '\n' ',' | sed 's/,$//')",
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
]
EOF
      )"
  do
    kubectl patch -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" --type=json \
      -p "$(cat << EOF
[
  {"op":"remove","path":"/status/dbOps"}
]
EOF
      )"
  done
else
  INITIAL_INSTANCES="$(kubectl get -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" \
    --template='{{ .status.dbOps.majorVersionUpgrade.initialInstances }}')"
  INITIAL_INSTANCES="$(echo "$INITIAL_INSTANCES" | tr ',' '\n')"
  PRIMARY_INSTANCE="$(kubectl get -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" \
    --template='{{ .status.dbOps.majorVersionUpgrade.primaryInstance }}')"
fi

while true
do
  IS_STATEFULSET_UPDATED="$(kubectl get -n "$CLUSTER_NAMESPACE" statefulset "$CLUSTER_NAME" \
    --template "{{ range .spec.template.spec.initContainers }}{{ if eq .name \"$MAJOR_VERSION_UPGRADE_CONTAINER_NAME\" }}true{{ end }}{{ end }}")"
  if [ "$IS_STATEFULSET_UPDATED" = "true" ]
  then
    break
  fi
  sleep 1
done

INITIAL_INSTANCES_COUNT="$(echo "$INITIAL_INSTANCES" | wc -l)"
echo "Initial instances:"
echo "$INITIAL_INSTANCES" | sed 's/^/ - /'
echo

dbops_update_status

if [ "$INITIAL_INSTANCES_COUNT" -gt 1 ]
then
  echo "Downscaling cluster to 1 instance"
  echo

  kubectl patch -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"replace","path":"/spec/instances","value":1}
]
EOF
      )"

  echo "Waiting cluster downscale..."

  until [ "$(kubectl get -n "$CLUSTER_NAMESPACE" pods -l "$CLUSTER_POD_LABELS" -o name | cut -d / -f 2)" = "$PRIMARY_INSTANCE" ]
  do
    sleep 1
  done

  echo "done"
  echo
fi

echo "Killing primary instance $PRIMARY_INSTANCE..."

kubectl delete -n "$CLUSTER_NAMESPACE" pod "$PRIMARY_INSTANCE"

echo "done"
echo

echo "Waiting primary instance $PRIMARY_INSTANCE major upgrade..."

wait_for_instance "$PRIMARY_INSTANCE"

CURRENT_PRIMARY_POD="$(kubectl get -n "$CLUSTER_NAMESPACE" pods -l "$CLUSTER_PRIMARY_POD_LABELS" -o name)"
CURRENT_PRIMARY_INSTANCE="$(echo "$PRIMARY_POD" | cut -d / -f 2)"
if [ "$PRIMARY_INSTANCE" != "$CURRENT_PRIMARY_INSTANCE" ]
then
  echo "FAILURE=Major version upgrade failed. Please check pod $PRIMARY_INSTANCE logs for more info" >> "$SHARED_PATH/major-version-upgrade.out"
  exit 1
fi

echo "done"
echo

dbops_update_status

if [ "$INITIAL_INSTANCES_COUNT" -gt 1 ]
then
  echo "Upscaling cluster to $INITIAL_INSTANCES_COUNT instances"
  echo

  kubectl patch -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"replace","path":"/spec/instances","value":$INITIAL_INSTANCES_COUNT}
]
EOF
      )"

  echo "Waiting cluster upscale..."

  echo "$INITIAL_INSTANCES" | while read INSTANCE
    do
      wait_for_instance "$INSTANCE"
      dbops_update_status
    done

  echo "done"
  echo
fi

echo "Signaling major version upgrade finished to cluster"
echo

until kubectl patch -n "$CLUSTER_NAMESPACE" "$CLUSTER_CRD_NAME" "$CLUSTER_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"remove","path":"/status/dbOps"}
]
EOF
    )"
do
  sleep 1
done
) >> "$SHARED_PATH/major-version-upgrade.out" 2>> "$SHARED_PATH/major-version-upgrade.err" &

PID="$!"

wait -n "$PID" "$TIMEOUT_PID" "$TRY_LOCK_PID"
EXIT_CODE="$?"

if kill -0 "$PID" 2>/dev/null
then
  kill "$PID"
  if kill -0 "$TRY_LOCK_PID" 2>/dev/null
  then
    kill "$TRY_LOCK_PID"
    echo "LOCK_LOST=false" >> "$SHARED_PATH/major-version-upgrade.out"
    echo "TIMED_OUT=true" >> "$SHARED_PATH/major-version-upgrade.out"
    echo "EXIT_CODE=1" >> "$SHARED_PATH/major-version-upgrade.out"
  else
    kill "$TIMEOUT_PID"
    echo "LOCK_LOST=true" >> "$SHARED_PATH/major-version-upgrade.out"
    echo "TIMED_OUT=false" >> "$SHARED_PATH/major-version-upgrade.out"
    echo "EXIT_CODE=1" >> "$SHARED_PATH/major-version-upgrade.out"
  fi
else
  kill "$TIMEOUT_PID"
  kill "$TRY_LOCK_PID"
  echo "LOCK_LOST=false" >> "$SHARED_PATH/major-version-upgrade.out"
  echo "TIMED_OUT=false" >> "$SHARED_PATH/major-version-upgrade.out"
  echo "EXIT_CODE=$EXIT_CODE" >> "$SHARED_PATH/major-version-upgrade.out"
fi

kill "$TAIL_PID"

true