#!/bin/sh

# Seconds
BACKUP_TIMEOUT="${BACKUP_TIMEOUT:-}"
RECONCILIATION_TIMEOUT="${RECONCILIATION_TIMEOUT:-300}"

. "$LOCAL_BIN_SHELL_UTILS_PATH"

run() {
  set -e

  echo "Acquiring lock..."
  acquire_lock
  echo "Lock acquired"
  maintain_lock >> /tmp/try-lock 2>&1 &
  TRY_LOCK_PID=$!

  true > /tmp/backup-push
  reconcile_backups &
  PID=$!

  set +e
  (
  set +x
  while (kill -0 "$PID" && kill -0 "$TRY_LOCK_PID") 2>/dev/null
  do
    true
  done
  )

  if kill -0 "$PID" 2>/dev/null
  then
    tail -n 100 /tmp/try-lock
    echo "Lock lost"
    kill_with_childs "$PID"
    retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Lock lost:\n'; cat /tmp/try-lock; } | to_json_string)"'}
      ]'
    return 1
  else
    kill_with_childs "$TRY_LOCK_PID"
    release_lock >> /tmp/try-lock 2>&1
    echo "Lock released"
    wait "$PID"
    EXIT_CODE="$?"
    if [ "$EXIT_CODE" != 0 ]
    then
      echo "Backup failed"
      return 1
    fi
  fi
}

reconcile_backups() {
  set -e

  if [ -n "$SCHEDULED_BACKUP_KEY" ]
  then
    BACKUP_NAME="${CLUSTER_NAME}-$(date +%Y-%m-%d-%H-%M-%S --utc)"
  fi

  BACKUP_CONFIG_RESOURCE_VERSION="$(retry kubectl get "$BACKUP_CONFIG_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_CONFIG" --template='{{ .metadata.resourceVersion }}')"
  CLUSTER_BACKUP_PATH="$(retry kubectl get "$CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
    --template="{{ if .spec.configurations.backupPath }}{{ .spec.configurations.backupPath }}{{ else }}{{ (index .spec.configurations.backups 0).path }}{{ end }}")"
  BACKUP_ALREADY_COMPLETED=false
  create_or_update_backup_cr
  if [ "$BACKUP_ALREADY_COMPLETED" = "true" ]
  then
    echo "Already completed backup. Nothing to do!"
    return
  fi

  CURRENT_BACKUP_CONFIG="$(retry kubectl get "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" \
    --template="{{ .status.sgBackupConfig.storage }}")"

  set +e
  BACKUP_START_TIMESTAMP="$(date +%s)"
  echo "Retrieving primary and replica"
  get_primary_and_replica_pods

  echo "Performing backup"

  do_backup

  echo "Extracting pg_controldata"
  extract_controldata
  if [ "$?" != 0 ]
  then
    echo "WARNING: Failed extracting pg_controldata"
  fi

  echo "Listing existing backups"
  list_backups
  if [ "$?" != 0 ]
  then
    cat /tmp/backup-list
    echo "Backups can not be listed after creation"
    retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/process/failure","value":"Backups can not be listed after creation"}
      ]'
    return 1
  fi
  if [ "$BACKUP_CONFIG_RESOURCE_VERSION" != "$(retry kubectl get "$BACKUP_CONFIG_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_CONFIG" --template='{{ .metadata.resourceVersion }}')" ]
  then
    cat /tmp/backup-list
    echo "Backup configuration '$BACKUP_CONFIG' changed during backup"
    retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/process/failure","value":'"$(printf 'Backup configuration %s changed during backup' "$BACKUP_CONFIG" | to_json_string)"'}
      ]'
    return 1
  elif [ "$CLUSTER_BACKUP_PATH" != "$(retry kubectl get "$CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME" \
      --template="{{ if .spec.configurations.backupPath }}{{ .spec.configurations.backupPath }}{{ else }}{{ (index .spec.configurations.backups 0).path }}{{ end }}")" ]
  then
    cat /tmp/backup-list
    echo "Backup path '$CLUSTER_BACKUP_PATH' changed during backup"
    retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/process/failure","value":'"$(printf 'Backup path %s changed during backup' "$CLUSTER_BACKUP_PATH" | to_json_string)"'}
      ]'
    return 1
  fi
  cat /tmp/backup-list | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
    | grep '"backup_name"' \
    > /tmp/existing-backups
  cat /tmp/backup-list | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
    | grep '"backup_name":"'"$CURRENT_BACKUP_NAME"'"' | tr -d '{}"' | tr ',' '\n' > /tmp/current-backup
  if ! grep -q "^backup_name:${CURRENT_BACKUP_NAME}$" /tmp/current-backup
  then
    cat /tmp/backup-list
    echo "Backup '$CURRENT_BACKUP_NAME' was not found after creation can not reconcile backups CRs"
    retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/process/failure","value":'"$(printf "Backup '%s' was not found after creation can not reconcile backups CRs" "$CURRENT_BACKUP_NAME" | to_json_string)"'}
      ]'
    return 1
  fi

  echo "Updating backup CR as completed"
  set_backup_completed
  if [ "$?" = 0 ]
  then
    echo "Backup CR updated as completed"
  else
    echo "Unable to update backup CR as completed"
    return 1
  fi
  echo "Backup completed"

  RECONCILIATION_START_TIMESTAMP="$(date +%s)"
  echo "Starting backups reconciliation"
  echo "Retain backups"
  get_backup_crs

  retain_backups
  if [ "$?" = 0 ]
  then
    echo "Retain of backups completed"
  else
    echo "Retain of backups failed"
  fi

  echo "Listing existing backups after retain"
  list_backups_for_reconciliation
  if [ "$?" != 0 ]
  then
    cat /tmp/backup-list
    echo "Backups can not be listed before retention"
    return
  fi
  cat /tmp/backup-list | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
    | grep '"backup_name"' \
    > /tmp/existing-backups
  cat /tmp/backup-list | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
    | grep '"backup_name":"'"$CURRENT_BACKUP_NAME"'"' | tr -d '{}"' | tr ',' '\n' > /tmp/current-backup
  if ! grep -q "^backup_name:${CURRENT_BACKUP_NAME}$" /tmp/current-backup
  then
    cat /tmp/backup-list
    echo "Backup '$CURRENT_BACKUP_NAME' was not found after creation can not reconcile backups CRs"
    return
  fi

  echo "Reconcile backup CRs"
  reconcile_backup_crs
  echo "Reconciliation of backups completed"
}

get_backup_crs() {
  BACKUP_CR_TEMPLATE="{{ range .items }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}{{ .spec.sgCluster }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}:{{ .metadata.namespace }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}:{{ .metadata.name }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}:{{ with .status.process.status }}{{ . }}{{ end }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}:{{ with .status.internalName }}{{ . }}{{ end }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}:{{ with .status.process.jobPod }}{{ . }}{{ end }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}:{{ with .metadata.labels }}{{ with index . \"$SCHEDULED_BACKUP_KEY\" }}{{ . }}{{ end }}{{ end }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}:{{ if .spec.managedLifecycle }}true{{ else }}false{{ end }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}:{{ if .status.process.managedLifecycle }}true{{ else }}false{{ end }}"
  BACKUP_CR_TEMPLATE="${BACKUP_CR_TEMPLATE}{{ printf "'"\n"'" }}{{ end }}"
  retry kubectl get "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" \
    --template="$BACKUP_CR_TEMPLATE" > /tmp/all-backups-in-namespace
  grep "^$CLUSTER_NAME:" /tmp/all-backups-in-namespace > /tmp/backups-in-namespace
  true > /tmp/all-backups
  local CLUSTER_BACKUP_NAMESPACE
  for CLUSTER_BACKUP_NAMESPACE in $CLUSTER_BACKUP_NAMESPACES
  do
    retry kubectl get "$BACKUP_CRD_NAME" -n "$CLUSTER_BACKUP_NAMESPACE" \
      --template="$BACKUP_CR_TEMPLATE" >> /tmp/all-backups
  done
  grep "^$CLUSTER_NAMESPACE.$CLUSTER_NAME:" /tmp/all-backups > /tmp/backups-out-of-namespace
  cat /tmp/backups-in-namespace /tmp/backups-out-of-namespace > /tmp/backups
}

create_or_update_backup_cr() {
  BACKUP_CONFIG_JSON="$(retry kubectl get "$BACKUP_CONFIG_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_CONFIG" -o json)"
BACKUP_STATUS_YAML="$(cat << BACKUP_STATUS_YAML_EOF
status:
  backupPath: "$CLUSTER_BACKUP_PATH"
  process:
    jobPod: "$POD_NAME"
  sgBackupConfig:
    baseBackups:
      compression: "$COMPRESSION"
    storage: $(printf %s "$BACKUP_CONFIG_JSON" | jq .spec)
BACKUP_STATUS_YAML_EOF
  )"

  if ! kubectl get "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" -o name >/dev/null 2>&1
  then
    echo "Creating backup CR"
    cat << EOF > /tmp/backup-to-create
apiVersion: $BACKUP_CRD_APIVERSION
kind: $BACKUP_CRD_KIND
metadata:
  namespace: "$CLUSTER_NAMESPACE"
  name: "$BACKUP_NAME"
  annotations:
    $SCHEDULED_BACKUP_KEY: "$RIGHT_VALUE"
  labels:
    $SCHEDULED_BACKUP_KEY: "$RIGHT_VALUE"
    $SCHEDULED_BACKUP_JOB_NAME_KEY: "$SCHEDULED_BACKUP_JOB_NAME"
spec:
  sgCluster: "$CLUSTER_NAME"
  managedLifecycle: true
$BACKUP_STATUS_YAML
EOF
    if ! retry kubectl create -f /tmp/backup-to-create -o json > /tmp/backup-create
    then
      cat /tmp/backup-create
      echo
      exit 1
    fi
    cat /tmp/backup-create
    echo
    BACKUP_UID="$(jq -r .metadata.uid /tmp/backup-create)"
  else
    if ! retry kubectl get "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --template="{{ .status.process.status }}" \
      | grep -q "^$BACKUP_PHASE_COMPLETED$"
    then
      DRY_RUN_CLIENT=$(kubectl version --client=true -o json | jq -r 'if (.clientVersion.minor | tonumber) < 18 then "true" else "client" end')
      echo "Updating backup CR"
      retry kubectl get "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" -o yaml > /tmp/backup-found
      {
        cat /tmp/backup-found
        echo "$BACKUP_STATUS_YAML"
      } | kubectl create --dry-run="$DRY_RUN_CLIENT" -f - -o json > /tmp/backup-to-patch
      if ! retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" -o json \
        --type merge --patch-file /tmp/backup-to-patch | tee /tmp/backup-update 2>&1
      then
        cat /tmp/backup-update
        echo
        exit 1
      fi
      cat /tmp/backup-update
      echo
    else
      BACKUP_ALREADY_COMPLETED=true
    fi
  fi
}

get_primary_and_replica_pods() {
  retry kubectl get pod -n "$CLUSTER_NAMESPACE" -l "${CLUSTER_LABELS},${PATRONI_ROLE_KEY}=${PATRONI_PRIMARY_ROLE}" \
    --template '{{ range .items }}{{ printf "%s\n" .metadata.name }}{{ end }}' > /tmp/current-primary
  retry kubectl get pod -n "$CLUSTER_NAMESPACE" -l "${CLUSTER_LABELS},${PATRONI_ROLE_KEY}=${PATRONI_REPLICA_ROLE}" \
    --template '{{ range .items }}{{ printf "%s\n" .metadata.name }}{{ end }}' \
    | head -n 1 > /tmp/current-replica-or-primary
  if [ ! -s /tmp/current-primary ]
  then
    retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/process/failure","value":"Unable to find primary, backup aborted"}
      ]'
    kubectl get pod -n "$CLUSTER_NAMESPACE" -l "${CLUSTER_LABELS}" >&2 || true
    echo "Unable to find primary, backup aborted"
    exit 1
  fi
  retry kubectl get pod -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-primary)" \
    --template "{{ range .spec.volumes }}{{ if eq .name \"${CLUSTER_NAME}-data\" }}{{ .persistentVolumeClaim.claimName }}{{ end }}{{ end }}" > /tmp/current-primary-pvc

  if [ ! -s /tmp/current-replica-or-primary ]
  then
    cat /tmp/current-primary > /tmp/current-replica-or-primary
    echo "Primary is $(cat /tmp/current-primary)"
    echo "Replica not found, primary will be used for cleanups"
  else
    echo "Primary is $(cat /tmp/current-primary)"
    echo "Replica is $(cat /tmp/current-replica-or-primary)"
  fi
}

do_backup() {
  if [ -f /tmp/backup-create ]
  then
    BACKUP_IS_PERMANENT=false
  else
    BACKUP_IS_PERMANENT="$(jq '.spec | if (has("managedLifecycle") | not) or .managedLifecycle then false else true end' /tmp/backup-update)"
  fi

  if [ "$USE_VOLUME_SNAPSHOT" = true ]
  then
    echo "Performing full backup stored on VolumeSnapshot"
    BACKUP_START_TIME="$(date_iso8601)"
    rm -f /tmp/backup-psql
    mkfifo /tmp/backup-psql
    sh -c 'echo $$ > /tmp/backup-tail-pid; exec tail -f /tmp/backup-psql' \
      | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-primary)" -c "$PATRONI_CONTAINER_NAME" \
        -- psql -v ON_ERROR_STOP=1 -t -A > /tmp/backup-psql-out 2>&1 &
    echo $! > /tmp/backup-psql-pid

    echo "Starting backup"
    cat << EOF >> /tmp/backup-psql
SELECT 'Backup bootstrap';
EOF
    until grep -qxF 'Backup bootstrap' /tmp/backup-psql-out \
      || ! kill -0 "$(cat /tmp/backup-psql-pid)"
    do
      sleep 1
    done
    cat /tmp/backup-psql-out
    if ! kill -0 "$(cat /tmp/backup-psql-pid)"
    then
      echo 'Backup failed while connecting to primary'
      retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
        {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while connecting to primary:\n'; cat /tmp/backup-psql-out; } | to_json_string)"'}
        ]'
      kill "$(cat /tmp/backup-tail-pid)" || true
      exit 1
    fi
    truncate -s 0 /tmp/backup-psql-out

    cat << EOF >> /tmp/backup-psql
SET client_min_messages TO WARNING;
SELECT
  (SELECT system_identifier FROM pg_control_system()),
  current_setting('server_version_num')::int,
  pg_is_in_recovery(),
  CASE WHEN pg_is_in_recovery() THEN upper(
    lpad(to_hex((SELECT timeline_id FROM pg_control_checkpoint()))::text, 8, '0')
    || lpad(to_hex(lsn_high)::text, 8, '0')
    || lpad(to_hex(lsn_low - 1
      / (SELECT bytes_per_wal_segment::bigint FROM pg_control_init()))::text, 8, '0'))
  ELSE (pg_walfile_name_offset(lsn)).file_name END,
  (lsn_high << 32) | lsn_low
FROM (SELECT 
  lsn,
  ('x' || lower(lpad(substring(lsn::text from '^[^/]+'), 8, '0')))::bit(32)::bigint AS lsn_high,
  ('x' || lower(lpad(substring(lsn::text from '[^/]+$'), 8, '0')))::bit(32)::bigint AS lsn_low
  FROM pg_backup_start(label => '$BACKUP_NAME', fast => $FAST_VOLUME_SNAPSHOT) AS lsn) AS lsn;
SELECT 'Backup started';
EOF
    until grep -qxF 'Backup started' /tmp/backup-psql-out \
      || ! kill -0 "$(cat /tmp/backup-psql-pid)"
    do
      sleep 1
    done
    cat /tmp/backup-psql-out
    if ! kill -0 "$(cat /tmp/backup-psql-pid)"
    then
      echo 'Backup failed while running pg_backup_start'
      retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
        {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while running pg_backup_start:\n'; cat /tmp/backup-psql-out; } | to_json_string)"'}
        ]'
      kill "$(cat /tmp/backup-tail-pid)" || true
      exit 1
    fi
    head -n 2 /tmp/backup-psql-out | tail -n 1 > /tmp/backup-start
    truncate -s 0 /tmp/backup-psql-out

    cat << EOF > /tmp/snapshot-to-create 
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  namespace: $CLUSTER_NAMESPACE
  name: $BACKUP_NAME
  ownerReferences:
  - apiVersion: $BACKUP_CRD_APIVERSION
    kind: $BACKUP_CRD_KIND
    name: $BACKUP_NAME
    uid: $BACKUP_UID
spec:
  volumeSnapshotClassName: $([ "x$VOLUME_SNAPSHOT_STORAGE_CLASS" = x ] && printf null || printf %s "$VOLUME_SNAPSHOT_STORAGE_CLASS")
  source:
    persistentVolumeClaimName: $(cat /tmp/current-primary-pvc)
EOF
    echo "Creating VolumeSnapshot"
    if ! retry kubectl create -f /tmp/snapshot-to-create > /tmp/backup-snapshot 2>&1
    then
      cat /tmp/backup-snapshot
      echo 'Backup failed while creating VolumeSnapshot'
      retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
        {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while creating VolumeSnapshot:\n'; cat /tmp/backup-snapshot; } | to_json_string)"'}
        ]'
      kill "$(cat /tmp/backup-tail-pid)" || true
      exit 1
    fi
    cat /tmp/backup-snapshot
    
    echo "Waiting for VolumeSnapshot to be ready"
    while true
    do
      if ! retry kubectl get "$VOLUME_SNAPSHOT_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" -o json > /tmp/backup-volumesnapshot 2> /tmp/backup-volumesnapshot-error
      then
        cat /tmp/backup-volumesnapshot-error
        echo 'Backup failed while waiting VolumeSpanshot to be ready'
        retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
          {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while waiting VolumeSpanshot to be ready:\n'; cat /tmp/backup-volumesnapshot-error; } | to_json_string)"'}
          ]'
        kill "$(cat /tmp/backup-tail-pid)" || true
        exit 1
      fi
      if [ "$(jq -r .status.readyToUse /tmp/backup-volumesnapshot)" = true ]
      then
        echo "VolumeSnapshot ready"
        break
      fi
      if [ "x$(jq -r 'if .status.error != null then .status.error.message else "" end' /tmp/backup-volumesnapshot)" != x ] \
        && ! jq -r '.status.error.message' /tmp/backup-volumesnapshot \
          | grep -qF 'the object has been modified; please apply your changes to the latest version and try again'
      then
        cat /tmp/backup-volumesnapshot
        echo 'Backup failed due to error in VolumeSnapshot'
        retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
          {"op":"replace","path":"/status/process/failure","value":'"$(jq '"Backup failed due to error in VolumeSnapshot: " + .status.error.message' /tmp/backup-volumesnapshot)"'}
          ]'
        kill "$(cat /tmp/backup-tail-pid)" || true
        exit 1
      fi
      if is_timeout_expired BACKUP
      then
        echo "Backup failed due to timeout ($BACKUP_TIMEOUT) while waiting VolumeSpanshot to be ready"
        retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
          {"op":"replace","path":"/status/process/failure","value":'"Backup failed due to timeout ($BACKUP_TIMEOUT) while waiting VolumeSpanshot to be ready"'}
          ]'
        kill "$(cat /tmp/backup-tail-pid)" || true
        exit 1
      fi
      sleep 1
    done

    echo "Stopping backup"
    cat << EOF >> /tmp/backup-psql
SET client_min_messages TO WARNING;
SELECT
  (lsn_high << 32) | lsn_low,
  CASE WHEN labelfile IS NULL OR labelfile = '' THEN '' ELSE regexp_replace(encode(labelfile::bytea, 'base64'), '[\n\r]+', '', 'g' ) END,
  CASE WHEN spcmapfile IS NULL OR spcmapfile = '' THEN '' ELSE regexp_replace(encode(spcmapfile::bytea, 'base64'), '[\n\r]+', '', 'g' ) END
FROM (SELECT 
  lsn,
  ('x' || lower(lpad(substring(lsn::text from '^[^/]+'), 8, '0')))::bit(32)::bigint AS lsn_high,
  ('x' || lower(lpad(substring(lsn::text from '[^/]+$'), 8, '0')))::bit(32)::bigint AS lsn_low,
  labelfile,
  spcmapfile
  FROM pg_backup_stop(wait_for_archive => true)) AS lsn;
SELECT 'Backup stopped';
EOF
    until grep -qxF 'Backup stopped' /tmp/backup-psql-out \
      || ! kill -0 "$(cat /tmp/backup-psql-pid)"
    do
      sleep 1
    done
    cat /tmp/backup-psql-out
    if ! kill -0 "$(cat /tmp/backup-psql-pid)"
    then
      echo 'Backup failed while running pg_backup_stop'
      retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
        {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while running pg_backup_stop:\n'; cat /tmp/backup-stop; } | to_json_string)"'}
        ]'
      kill "$(cat /tmp/backup-tail-pid)" || true
      exit 1
    fi
    head -n 2 /tmp/backup-psql-out | tail -n 1 > /tmp/backup-stop
    truncate -s 0 /tmp/backup-psql-out

    kill "$(cat /tmp/backup-tail-pid)" || true
    BACKUP_END_TIME="$(date_iso8601)"

    echo "Creating backup entry for wal-g marked with \"volume-snapshot\" user data"

    CURRENT_BACKUP_NAME="base_$(cut -d '|' -f 4 /tmp/backup-start)"
    VOLUME_SNAPSHOT_RESTORE_SIZE="$(jq '.status.restoreSize | . as $v|[splits("[eEinumkKMGTP]+")][0]|. as $ta|tonumber as $a|$v[($ta|length):($v|length)] as $f
      | if $f == "" then $a
        elif $f == "Ki" then $a * 1024
        elif $f == "Mi" then $a * 1024 * 1024
        elif $f == "Gi" then $a * 1024 * 1024 * 1024
        elif $f == "Ti" then $a * 1024 * 1024 * 1024 * 1024
        elif $f == "Pi" then $a * 1024 * 1024 * 1024 * 1024 * 1024
        elif $f == "Ei" then $a * 1024 * 1024 * 1024 * 1024 * 1024 * 1024
        elif $f == "m" then $a / 1000
        else . end' /tmp/backup-volumesnapshot)"
    if ! echo true | jq -c '{
        LSN: $LSN,
        PgVersion: $PgVersion,
        FinishLSN: $FinishLSN,
        SystemIdentifier: $SystemIdentifier,
        UncompressedSize: $UncompressedSize,
        CompressedSize: $CompressedSize,
        Spec: null,
        Version: 2,
        StartTime: $StartTime,
        FinishTime: $FinishTime,
        DateFmt: "%Y-%m-%dT%H:%M:%S.%fZ",
        Hostname: $Hostname,
        DataDir: $DataDir,
        IsPermanent: $IsPermanent
      }' \
        --argjson LSN "$(cut -d '|' -f 5 /tmp/backup-start)" \
        --argjson PgVersion "$(cut -d '|' -f 2 /tmp/backup-start)" \
        --argjson FinishLSN "$(cut -d '|' -f 1 /tmp/backup-stop)" \
        --argjson SystemIdentifier "$(cut -d '|' -f 1 /tmp/backup-start)" \
        --argjson UncompressedSize "$VOLUME_SNAPSHOT_RESTORE_SIZE" \
        --argjson CompressedSize "$VOLUME_SNAPSHOT_RESTORE_SIZE" \
        --arg StartTime "$BACKUP_START_TIME" \
        --arg FinishTime "$BACKUP_END_TIME" \
        --arg Hostname "$(cat /tmp/current-primary)" \
        --arg DataDir "$PG_DATA_PATH" \
        --argjson IsPermanent "$BACKUP_IS_PERMANENT" > "/tmp/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json" 2>&1
    then
      cat "/tmp/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json"
      echo 'Backup failed while creating the sentinel JSON for wal-g'
      retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
        {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while creating the JSON for wal-g:\n'; cat "/tmp/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json"; } | to_json_string)"'}
        ]'
      exit 1
    fi
    cat "/tmp/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json"

    if ! echo true | jq -c '{
        start_time: $start_time,
        finish_time: $finish_time,
        date_fmt: "%Y-%m-%dT%H:%M:%S.%fZ",
        hostname: $hostname,
        data_dir: $data_dir,
        pg_version: $pg_version,
        start_lsn: $start_lsn,
        finish_lsn: $finish_lsn,
        is_permanent: $is_permanent,
        system_identifier: $system_identifier,
        uncompressed_size: $uncompressed_size,
        compressed_size: $compressed_size,
        user_data: "volume-snapshot"
      }' \
        --argjson start_lsn "$(cut -d '|' -f 5 /tmp/backup-start)" \
        --argjson pg_version "$(cut -d '|' -f 2 /tmp/backup-start)" \
        --argjson finish_lsn "$(cut -d '|' -f 1 /tmp/backup-stop)" \
        --argjson system_identifier "$(cut -d '|' -f 1 /tmp/backup-start)" \
        --argjson uncompressed_size "$VOLUME_SNAPSHOT_RESTORE_SIZE" \
        --argjson compressed_size "$VOLUME_SNAPSHOT_RESTORE_SIZE" \
        --arg start_time "$BACKUP_START_TIME" \
        --arg finish_time "$BACKUP_END_TIME" \
        --arg hostname "$(cat /tmp/current-primary)" \
        --arg data_dir "$PG_DATA_PATH" \
        --argjson is_permanent "$BACKUP_IS_PERMANENT" > "/tmp/${CURRENT_BACKUP_NAME}_metadata.json"  2>&1
    then
      cat "/tmp/${CURRENT_BACKUP_NAME}_metadata.json"
      echo 'Backup failed while creating the metadata JSON for wal-g'
      retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
        {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while creating the metadata JSON for wal-g:\n'; cat "/tmp/${CURRENT_BACKUP_NAME}_metadata.json"; } | to_json_string)"'}
        ]'
      exit 1
    fi
    cat "/tmp/${CURRENT_BACKUP_NAME}_metadata.json"

    if ! cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-primary)" -c "$PATRONI_CONTAINER_NAME" \
      -- sh -e $SHELL_XTRACE > /tmp/backup-push-volume-snapshot 2>&1
cat << 'INNER_EOF' > /tmp/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json
$(cat /tmp/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json)
INNER_EOF
cat << 'INNER_EOF' > /tmp/${CURRENT_BACKUP_NAME}_metadata.json
$(cat /tmp/${CURRENT_BACKUP_NAME}_metadata.json)
INNER_EOF
cat << 'INNER_EOF' > /tmp/${CURRENT_BACKUP_NAME}_files_metadata.json
{"Files":{}}
INNER_EOF
exec-with-env "$BACKUP_ENV" \
  -- $(get_timeout_command BACKUP) wal-g st put --no-compress "/tmp/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json" "basebackups_005/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json"
exec-with-env "$BACKUP_ENV" \
  -- $(get_timeout_command BACKUP) wal-g st put --no-compress "/tmp/${CURRENT_BACKUP_NAME}_metadata.json" "basebackups_005/${CURRENT_BACKUP_NAME}/metadata.json"
exec-with-env "$BACKUP_ENV" \
  -- $(get_timeout_command BACKUP) wal-g st put --no-compress "/tmp/${CURRENT_BACKUP_NAME}_files_metadata.json" "basebackups_005/${CURRENT_BACKUP_NAME}/files_metadata.json"
rm \
  "/tmp/${CURRENT_BACKUP_NAME}_backup_stop_sentinel.json" \
  "/tmp/${CURRENT_BACKUP_NAME}_metadata.json" \
  "/tmp/${CURRENT_BACKUP_NAME}_files_metadata.json"
EOF
    then
      cat /tmp/backup-push-volume-snapshot
      echo 'Backup failed while pushing the JSON files for wal-g'
      retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
        {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while pushing the JSON files for wal-g:\n'; cat /tmp/backup-push-volume-snapshot; } | to_json_string)"'}
        ]'
      exit 1
    fi
    cat /tmp/backup-push-volume-snapshot

    return
  fi

  echo "Performing full backup stored on SGStorageObject"

  if ! cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-primary)" -c "$PATRONI_CONTAINER_NAME" \
    -- sh -e $SHELL_XTRACE > /tmp/backup-push 2>&1
exec-with-env "$BACKUP_ENV" \\
  -- $(get_timeout_command BACKUP) wal-g backup-push "$PG_DATA_PATH" -f $([ "$BACKUP_IS_PERMANENT" = true ] && printf %s '-p' || true)
EOF
  then
    cat /tmp/backup-push
    echo 'Backup failed while pushing'
    retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup failed while perfroming backup-push:\n'; cat /tmp/backup-push; } | to_json_string)"'}
      ]'
    exit 1
  fi
  CURRENT_BACKUP_NAME=
  if grep -q " Wrote backup with name " /tmp/backup-push
  then
    CURRENT_BACKUP_NAME="$(grep " Wrote backup with name " /tmp/backup-push | sed -E 's/.*name ([^ ]+) *(to storage [^ ]+)?/\1/')"
  fi
  if [ -z "$CURRENT_BACKUP_NAME" ]
  then
    cat /tmp/backup-push
    echo "Backup name not found in backup-push log"
    retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/process/failure","value":'"$({ printf 'Backup name not found in backup-push log:\n'; cat /tmp/backup-push; } | to_json_string)"'}
      ]'
    exit 1
  fi
  cat /tmp/backup-push
}

extract_controldata() {
  if cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-primary)" -c "$PATRONI_CONTAINER_NAME" \
      -- sh -e $SHELL_XTRACE > /tmp/pg_controldata
pg_controldata --pgdata="$PG_DATA_PATH"
EOF
  then
    cat /tmp/pg_controldata
    cat /tmp/pg_controldata \
      | {
        FIRST=true
        printf '{\n'
        while read LINE
        do
          KEY="${LINE%%:*}"
          VALUE="${LINE#*:}"
          VALUE="$(printf %s "$VALUE" | tr -s ' ')"
          VALUE="${VALUE# }"
          if [ "$FIRST" = true ]
          then
            FIRST=false
          else
            printf ',\n'
          fi
          printf '"%s": "%s"' "$KEY" "$VALUE"
        done
        printf '\n}\n'
        } > /tmp/pg_controldata.json
  else
    cat /tmp/pg_controldata
    echo 'WARNING: Can not retrieve pg_controldata info'
    echo '{}' > /tmp/pg_controldata.json
    return 1
  fi
}

retain_backups() {
  cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-replica-or-primary)" -c "$PATRONI_CONTAINER_NAME" \
  -- sh -e $SHELL_XTRACE

# for each existing backup
exec-with-env "$BACKUP_ENV" \\
  -- $(get_timeout_command RECONCILIATION) wal-g backup-list --detail --json 2>/dev/null \\
  | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\\n' \\
  | grep '"backup_name"' \\
  | while read BACKUP
    do
      BACKUP_NAME="\$(echo "\$BACKUP" | tr -d '{}\\42' | tr ',' '\\n' \\
          | grep 'backup_name' | cut -d : -f 2-)"
      echo "Check if backup \$BACKUP_NAME has to be set permanent or impermanent"
      # if has a managed lifecycle and is marked as permanent, mark as impermanent
      if { [ "\$BACKUP_NAME" = "$CURRENT_BACKUP_NAME" ] && [ "$BACKUP_IS_PERMANENT" != true ]; } \\
        || { echo '$(cat /tmp/backups)' \\
          | grep '^[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:true' \\
          | cut -d : -f 5 \\
          | grep -v '^\$' \\
          | grep -q "^\$BACKUP_NAME\$" \\
        && echo "\$BACKUP" | grep -q "\\"is_permanent\\":true"; }
      then
        echo "Mark \$BACKUP_NAME as impermanent"
        exec-with-env "$BACKUP_ENV" \\
          -- $(get_timeout_command RECONCILIATION) wal-g backup-mark -i "\$BACKUP_NAME"
      # if has not a managed lifecycle and in not marked as permanent, mark as permanent
      elif echo '$(cat /tmp/backups)' \\
        | grep -v '^[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:true' \\
        | cut -d : -f 5 \\
        | grep -v '^\$' \\
        | grep -q "^\$BACKUP_NAME\$" \\
        && echo "\$BACKUP" | grep -q "\\"is_permanent\\":false"
      then
        echo "Mark \$BACKUP_NAME as permanent"
        exec-with-env "$BACKUP_ENV" \\
          -- $(get_timeout_command RECONCILIATION) wal-g backup-mark "\$BACKUP_NAME"
      fi
    done

# for each existing backup sorted by backup time ascending (this also mean sorted by creation date ascending)
exec-with-env "$BACKUP_ENV" \\
  -- $(get_timeout_command RECONCILIATION) wal-g backup-list --detail --json 2>/dev/null \\
  | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\\n' \\
  | grep '"backup_name"' \\
  | sort -r -t , -k 2 \\
  | (
    RETAIN="$RETAIN"
    TO_REMOVE_BACKUP_NAME=
    while read BACKUP
    do
      if [ -n "\$TO_REMOVE_BACKUP_NAME" ]
      then
        if [ "$RETAIN_WALS_FOR_UNMANAGED_LIFECYCLE" = true ] || [ "\$RETAIN" -ge 1 ]
        then
          echo "Deleting backup \$TO_REMOVE_BACKUP_NAME"
          exec-with-env "$BACKUP_ENV" \\
            -- $(get_timeout_command RECONCILIATION) wal-g delete target FIND_FULL "\$TO_REMOVE_BACKUP_NAME" --confirm
        else
          echo "Deleting backup \$TO_REMOVE_BACKUP_NAME and previous WAL files"
          exec-with-env "$BACKUP_ENV" \\
            -- $(get_timeout_command RECONCILIATION) wal-g delete before "\$TO_REMOVE_BACKUP_NAME" --confirm
          exec-with-env "$BACKUP_ENV" \\
            -- $(get_timeout_command RECONCILIATION) wal-g delete target FIND_FULL "\$TO_REMOVE_BACKUP_NAME" --confirm
        fi
      fi
      TO_REMOVE_BACKUP_NAME=
      BACKUP_NAME="\$(echo "\$BACKUP" | tr -d '{}\\42' | tr ',' '\\n' \\
          | grep 'backup_name' | cut -d : -f 2-)"
      echo "Check if backup \$BACKUP_NAME has to be retained and will retain \$RETAIN more backups"
      # if is not the created backup and is not in backup CR list, delete it
      if [ "\$BACKUP_NAME" != "$CURRENT_BACKUP_NAME" ] \\
        && ! echo '$(cat /tmp/backups \
          | cut -d : -f 5 \
          | grep -v '^\$')' \\
        | grep -q "^\$BACKUP_NAME\$"
      then
        echo "Deleting backup \$BACKUP_NAME since no associated SGBackup exists and will retain \$RETAIN backups"
        TO_REMOVE_BACKUP_NAME="\$BACKUP_NAME"
      # if is inside the retain window, retain it and decrease RETAIN counter
      elif [ "\$RETAIN" -ge 1 ] \\
        && echo '$(cat /tmp/backups \
            | grep '^[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:true' \
            | cut -d : -f 5 \
            | grep -v '^\$')' \\
          | grep -q "^\$BACKUP_NAME\$"
      then
        echo "Retaining backup \$BACKUP_NAME"
        RETAIN="\$((RETAIN-1))"
      # if is outside the retain window and has a managed lifecycle, delete it
      elif [ "\$RETAIN" -eq 0 ] \\
        && echo '$(cat /tmp/backups \
            | grep '^[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:true' \
            | cut -d : -f 5 \
            | grep -v '^\$')' \\
          | grep -q "^\$BACKUP_NAME\$"
      then
        echo "Deleting backup with managed lifecycle \$BACKUP_NAME"
        TO_REMOVE_BACKUP_NAME="\$BACKUP_NAME"
      # or retain it
      else
        echo "Retaining backup \$BACKUP_NAME with unmanaged lifecycle"
      fi
    done
    if [ -n "\$TO_REMOVE_BACKUP_NAME" ]
    then
      echo "Deleting latest backup \$TO_REMOVE_BACKUP_NAME and previous WAL files"
      exec-with-env "$BACKUP_ENV" \\
        -- $(get_timeout_command RECONCILIATION) wal-g delete before "\$TO_REMOVE_BACKUP_NAME" --confirm
      exec-with-env "$BACKUP_ENV" \\
        -- $(get_timeout_command RECONCILIATION) wal-g delete target FIND_FULL "\$TO_REMOVE_BACKUP_NAME" --confirm
    else
      echo "Deleting WAL files older than latest backup \$BACKUP_NAME"
      exec-with-env "$BACKUP_ENV" \\
        -- $(get_timeout_command RECONCILIATION) wal-g delete before "\$BACKUP_NAME" --confirm
    fi
    )

exec-with-env "$BACKUP_ENV" \\
  -- $(get_timeout_command RECONCILIATION) wal-g wal-verify integrity

exec-with-env "$BACKUP_ENV" \\
  -- $(get_timeout_command RECONCILIATION) wal-g wal-verify timeline
EOF
}

list_backups() {
  cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-replica-or-primary)" -c "$PATRONI_CONTAINER_NAME" \
    -- sh -e $SHELL_XTRACE > /tmp/backup-list
WALG_LOG_LEVEL= exec-with-env "$BACKUP_ENV" \\
  -- $(get_timeout_command BACKUP) wal-g backup-list --detail --json 2>/dev/null
EOF
}

list_backups_for_reconciliation() {
  cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-replica-or-primary)" -c "$PATRONI_CONTAINER_NAME" \
    -- sh -e $SHELL_XTRACE > /tmp/backup-list
WALG_LOG_LEVEL= exec-with-env "$BACKUP_ENV" \\
  -- $(get_timeout_command RECONCILIATION) wal-g backup-list --detail --json 2>/dev/null
EOF
}

set_backup_completed() {
  EXISTING_BACKUP_IS_PERMANENT="$(grep "^is_permanent:" /tmp/current-backup | cut -d : -f 2-)"
  IS_BACKUP_MANAGED_LIFECYCLE=""
  if [ "$EXISTING_BACKUP_IS_PERMANENT" = "true" ]
  then
    IS_BACKUP_MANAGED_LIFECYCLE="false"
  else
    IS_BACKUP_MANAGED_LIFECYCLE="true"
  fi

  TIMELINE="$(grep "^wal_file_name:" /tmp/current-backup)" # Read file name from current-backup
  TIMELINE="$(printf "$TIMELINE" | cut -d ':' -f 2 | tr -d '[:blank:]')" # Extract only the wal name value
  TIMELINE="$(expr substr "$TIMELINE" 1 8)" # Get the first 8 digits
  TIMELINE="$(printf "%d" "0x$TIMELINE")" # Convert hex to decimal

  BACKUP_PATCH="$(cat << EOF
[
  {"op":"replace","path":"/status/internalName","value":"$CURRENT_BACKUP_NAME"},
  {"op":"replace","path":"/status/process/failure","value":""},
  {"op":"replace","path":"/status/process/managedLifecycle","value":$IS_BACKUP_MANAGED_LIFECYCLE},
  {"op":"replace","path":"/status/process/timing","value":{
      "stored":"$(grep "^time:" /tmp/current-backup | cut -d : -f 2-)",
      "start":"$(grep "^start_time:" /tmp/current-backup | cut -d : -f 2-)",
      "end":"$(grep "^finish_time:" /tmp/current-backup | cut -d : -f 2-)"
    }
  },
  {"op":"replace","path":"/status/backupInformation","value":{
      "startWalFile":"$(grep "^wal_file_name:" /tmp/current-backup | cut -d : -f 2-)",
      "timeline":"$TIMELINE",
      "hostname":"$(grep "^hostname:" /tmp/current-backup | cut -d : -f 2-)",
      "sourcePod":"$(grep "^hostname:" /tmp/current-backup | cut -d : -f 2-)",
      "pgData":"$(grep "^data_dir:" /tmp/current-backup | cut -d : -f 2-)",
      "postgresVersion":"$(grep "^pg_version:" /tmp/current-backup | cut -d : -f 2-)",
      "systemIdentifier":"$(grep "^system_identifier:" /tmp/current-backup | cut -d : -f 2-)",
      "lsn":{
        "start":"$(grep "^start_lsn:" /tmp/current-backup | cut -d : -f 2-)",
        "end":"$(grep "^finish_lsn:" /tmp/current-backup | cut -d : -f 2-)"
      },
      "size":{
        "uncompressed":$(grep "^uncompressed_size:" /tmp/current-backup | cut -d : -f 2-),
        "compressed":$(grep "^compressed_size:" /tmp/current-backup | cut -d : -f 2-)
      },
      "controlData": $(cat /tmp/pg_controldata.json)
    }
  },
$(
if [ "$USE_VOLUME_SNAPSHOT" = true ]
then
  cat << INNER_EOF
  {"op":"replace","path":"/status/volumeSnapshot","value":{
      "name":"$BACKUP_NAME",
      "backupLabel":"$(cut -d '|' -f 2 /tmp/backup-stop)",
      "tablespaceMap":"$(cut -d '|' -f 3 /tmp/backup-stop)"
    }
  }
INNER_EOF
fi
)
]
EOF
    )"
  retry kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch "$BACKUP_PATCH"
}

reconcile_backup_crs() {
  retry kubectl get pod -n "$CLUSTER_NAMESPACE" \
    --template="{{ range .items }}{{ .metadata.name }}{{ printf "'"\n"'" }}{{ end }}" \
    > /tmp/pods
  for BACKUP in $(cat /tmp/backups)
  do
    BACKUP_CR_NAMESPACE="$(echo "$BACKUP" | cut -d : -f 2)"
    BACKUP_CR_NAME="$(echo "$BACKUP" | cut -d : -f 3)"
    BACKUP_PHASE="$(echo "$BACKUP" | cut -d : -f 4)"
    BACKUP_NAME="$(echo "$BACKUP" | cut -d : -f 5)"
    BACKUP_POD="$(echo "$BACKUP" | cut -d : -f 6)"
    BACKUP_SHEDULED_BACKUP="$(echo "$BACKUP" | cut -d : -f 7)"
    BACKUP_MANAGED_LIFECYCLE="$(echo "$BACKUP" | cut -d : -f 9)"
    BACKUP_IS_PERMANENT="$([ "$BACKUP_MANAGED_LIFECYCLE" = true ] && echo false || echo true)"
    BACKUP_CONFIG="$(retry kubectl get "$BACKUP_CRD_NAME" -n "$BACKUP_CR_NAMESPACE" "$BACKUP_CR_NAME" \
      --template="{{ .status.sgBackupConfig.storage }}")"
    BACKUP_PATH="$(retry kubectl get "$BACKUP_CRD_NAME" -n "$BACKUP_CR_NAMESPACE" "$BACKUP_CR_NAME" \
      --template="{{ .status.backupPath }}")"
    # if backup CR has backup internal name, uses the same current
    # backup config and backup path but is not found in the storage, delete it
    if [ -n "$BACKUP_NAME" ] \
      && [ "$BACKUP_CONFIG" = "$CURRENT_BACKUP_CONFIG" ] \
      && [ "$BACKUP_PATH" = "$CLUSTER_BACKUP_PATH" ] \
      && ! grep -q "\"backup_name\":\"$BACKUP_NAME\"" /tmp/existing-backups
    then
      echo "Deleting backup CR $BACKUP_CR_NAME since backup does not exists"
      retry kubectl delete "$BACKUP_CRD_NAME" -n "$BACKUP_CR_NAMESPACE" "$BACKUP_CR_NAME"
    # if backup CR is a scheduled backup, is marked as running, has no pod or pod
    # has been terminated, delete it
    elif [ "$BACKUP_SHEDULED_BACKUP" = "$RIGHT_VALUE" ] \
      && [ "$BACKUP_PHASE" = "$BACKUP_PHASE_RUNNING" ] \
      && ([ -z "$BACKUP_POD" ] || ! grep -q "^$BACKUP_POD$" /tmp/pods)
    then
      echo "Deleting backup CR $BACKUP_CR_NAME since backup is running but pod does not exists"
      retry kubectl delete "$BACKUP_CRD_NAME" -n "$BACKUP_CR_NAMESPACE" "$BACKUP_CR_NAME"
    # if backup CR has backup internal name, is marked as completed, and is marked as
    # stored as not managed lifecycle or managed lifecycle and is found as managed lifecycle or
    # not managed lifecycle respectively, then mark it as stored as managed lifecycle or
    # not managed lifecycle respectively
    elif [ -n "$BACKUP_NAME" ] && [ "$BACKUP_PHASE" = "$BACKUP_PHASE_COMPLETED" ] \
      && ! grep "\"backup_name\":\"$BACKUP_NAME\"" /tmp/existing-backups \
        | grep -q "\"is_permanent\":$BACKUP_IS_PERMANENT"
    then
      EXISTING_BACKUP_IS_PERMANENT="$(grep "\"backup_name\":\"$BACKUP_NAME\"" /tmp/existing-backups \
        | tr -d '{}"' | tr ',' '\n' | grep "^is_permanent:" | cut -d : -f 2-)"
      IS_BACKUP_MANAGED_LIFECYCLE=""
      if [ "$EXISTING_BACKUP_IS_PERMANENT" = "true" ]
      then
        IS_BACKUP_MANAGED_LIFECYCLE="false"
      else
        IS_BACKUP_MANAGED_LIFECYCLE="true"
      fi
      echo "Updating backup CR $BACKUP_CR_NAME .status.process.managedLifecycle to $IS_BACKUP_MANAGED_LIFECYCLE since was updated in the backup"
      retry kubectl patch "$BACKUP_CRD_NAME" -n "$BACKUP_CR_NAMESPACE" "$BACKUP_CR_NAME" --type json --patch '[
        {"op":"replace","path":"/status/process/managedLifecycle","value":'$IS_BACKUP_MANAGED_LIFECYCLE'}
        ]'
    fi
  done
}

get_timeout_command() {
  GLOBAL_TIMEOUT="$(eval "printf %s \"\$${1}_TIMEOUT\"")"
  START_TIMESTAMP="$(eval "printf %s \"\$${1}_START_TIMESTAMP\"")"
  if [ "x$GLOBAL_TIMEOUT" = x ] || [ "x$GLOBAL_TIMEOUT" = x0 ]
  then
    printf ''
    return
  fi
  printf 'timeout %s' "\"\$(TIMEOUT=\"\$(( $GLOBAL_TIMEOUT - \$(date +%s) + $START_TIMESTAMP ))\"; [ \"\$TIMEOUT\" -ge 60 ] && printf %s \"\$TIMEOUT\" || printf 60)\""
}

is_timeout_expired() {
  GLOBAL_TIMEOUT="$(eval "printf %s \"\$${1}_TIMEOUT\"")"
  START_TIMESTAMP="$(eval "printf %s \"\$${1}_START_TIMESTAMP\"")"
  if [ "x$GLOBAL_TIMEOUT" = x ] || [ "x$GLOBAL_TIMEOUT" = x0 ]
  then
    return 1
  fi
  if [ "$(( $GLOBAL_TIMEOUT - $(date +%s) + $START_TIMESTAMP ))" -le 0 ]
  then
    return
  fi
  return 1
}

run
