set -ex

to_json_string() {
  sed ':a;N;$!ba;s/\n/\\n/g' | sed 's/\(["\\\t]\)/\\\1/g' | tr '\t' 't'
}

try_lock() {
  kubectl get cronjob.batch -n "$CLUSTER_NAMESPACE" "$CRONJOB_NAME" --template '
  LOCK_POD={{ if .metadata.annotations.lockPod }}{{ .metadata.annotations.lockPod }}{{ else }}{{ end }}
  LOCK_TIMESTAMP={{ if .metadata.annotations.lockTimestamp }}{{ .metadata.annotations.lockTimestamp }}{{ else }}0{{ end }}
  RESOURCE_VERSION={{ .metadata.resourceVersion }}
  ' > /tmp/current-backup-job
  source /tmp/current-backup-job
  CURRENT_TIMESTAMP="$(date +%s)"
  if [ "$POD_NAME" != "$LOCK_POD" ] && [ "$((CURRENT_TIMESTAMP-LOCK_TIMESTAMP))" -lt 15 ]
  then
    echo "Locked already by $LOCK_POD at $(date -d @"$LOCK_TIMESTAMP" --iso-8601 --utc)"
    exit 1
  fi
  kubectl annotate cronjob.batch -n "$CLUSTER_NAMESPACE" "$CRONJOB_NAME" \
    --resource-version "$RESOURCE_VERSION" --overwrite "lockPod=$POD_NAME" "lockTimestamp=$CURRENT_TIMESTAMP"
}

if [ "$IS_CRONJOB" = true ]
then
  BACKUP_NAME="${CLUSTER_NAME}-${POD_UID}"
fi

if ! kubectl get "$BACKUP_CRD_NAME" "$BACKUP_NAME" -o name >/dev/null 2>&1
then
  cat << EOF | kubectl create -f -
apiVersion: $BACKUP_CRD_APIVERSION
kind: $BACKUP_CRD_KIND
metadata:
  namespace: "$CLUSTER_NAMESPACE"
  name: "$BACKUP_NAME"
  ownerReferences:
$(kubectl get cronjob -n "$CLUSTER_NAMESPACE" "$CRONJOB_NAME" \
  --template '  - apiVersion: {{ .apiVersion }}{{ printf "\n" }}    kind: {{ .kind }}{{ printf "\n" }}    name: {{ .metadata.name }}{{ printf "\n" }}    uid: {{ .metadata.uid }}{{ printf "\n" }}')
spec:
  clusterName: "$CLUSTER_NAME"
  isPermanent: false
status:
  phase: "$BACKUP_PHASE_PENDING"
  pod: "$POD_NAME"
  backupConfig: "$BACKUP_CONFIG"
EOF
else
  if ! kubectl get "$BACKUP_CRD_NAME" "$BACKUP_NAME" --template "{{ .status.phase }}" \
    | grep -q "^$BACKUP_PHASE_COMPLETED$"
  then
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_PENDING"'"},
      {"op":"replace","path":"/status/pod","value":"'"$POD_NAME"'"},
      {"op":"replace","path":"/status/backupConfig","value":"'"$BACKUP_CONFIG"'"}
      ]'
  fi
fi

try_lock
(
while true
do
  sleep 5
  try_lock
done
) &
try_lock_pid=$!

(
kubectl get pod -n "$CLUSTER_NAMESPACE" -l "${CLUSTER_LABELS},${PATRONI_ROLE_KEY}=${PATRONI_PRIMARY_ROLE}" -o name > /tmp/current-primary
kubectl get pod -n "$CLUSTER_NAMESPACE" -l "${CLUSTER_LABELS},${PATRONI_ROLE_KEY}=${PATRONI_REPLICA_ROLE}" -o name | head -n 1 > /tmp/current-replica-or-primary
if [ ! -s /tmp/current-primary ]
then
  kubectl get pod -n "$CLUSTER_NAMESPACE" -l "${CLUSTER_LABELS}" >&2
  echo >&2
  echo "Unable to find primary, backup aborted" >&2
  exit 1
fi
if [ ! -s /tmp/current-replica-or-primary ]
then
  cat /tmp/current-primary > /tmp/current-replica-or-primary
fi
set +x
cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-primary)" -c patroni \
  -- sh -l -e > /tmp/backup-push 2>&1
wal-g backup-push /var/lib/postgresql/data/ -f $([ "$BACKUP_IS_PERMANENT" = true ] && echo '-p' || true)
EOF
set -x
cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-replica-or-primary)" -c patroni \
  -- sh -l -ex
wal-g delete retain FULL "$RETAIN" --confirm || true
EOF
) &
pid=$!

set +e
wait -n "$pid" "$try_lock_pid"
RESULT=$?
if kill -0 "$pid" 2>/dev/null
then
  kill "$pid"
  kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
    {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
    {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
    {"op":"replace","path":"/status/failureReason","value":"'"$(cat /tmp/backup-push | to_json_string)"'"}
    ]'
  exit 1
else
  kill "$try_lock_pid"
  if [ "$RESULT" != 0 ]
  then
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
      {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
      {"op":"replace","path":"/status/failureReason","value":"'"$(cat /tmp/backup-push | to_json_string)"'"}
      ]'
    exit 1
  fi
fi

if grep -q " Wrote backup with name " /tmp/backup-push
then
  WAL_G_BACKUP_NAME="$(grep " Wrote backup with name " /tmp/backup-push | sed 's/.* \([^ ]\+\)$/\1/')"
fi

if [ ! -z "$WAL_G_BACKUP_NAME" ]
then
  set +x
  cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-replica-or-primary)" -c patroni \
    -- sh -l -ex > /tmp/backup-list 2>&1
wal-g backup-list --detail --json
EOF
  RESULT=$?
  set -x
  if [ "$RESULT" != 0 ]
  then
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
      {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
      {"op":"replace","path":"/status/failureReason","value":"Backups can not be listed after creation '"$(cat /tmp/backup-list | to_json_string)"'"}
      ]'
    exit 1
  fi
  cat /tmp/backup-list | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
    | grep '"backup_name":"'"$WAL_G_BACKUP_NAME"'"' | tr -d '{}"' | tr ',' '\n' > /tmp/current-backup
  if ! grep -q "^backup_name:${WAL_G_BACKUP_NAME}$" /tmp/current-backup
  then
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
      {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
      {"op":"replace","path":"/status/failureReason","value":"Backup '"$WAL_G_BACKUP_NAME"' was not found after creation"}
      ]'
    exit 1
  else
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_COMPLETED"'"},
      {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
      {"op":"replace","path":"/status/failureReason","value":""},
      {"op":"replace","path":"/status/time","value":"'"$(cat /tmp/current-backup | grep "^time:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/walFileName","value":"'"$(cat /tmp/current-backup | grep "^wal_file_name:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/startTime","value":"'"$(cat /tmp/current-backup | grep "^start_time:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/finishTime","value":"'"$(cat /tmp/current-backup | grep "^finish_time:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/hostname","value":"'"$(cat /tmp/current-backup | grep "^hostname:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/dataDir","value":"'"$(cat /tmp/current-backup | grep "^data_dir:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/pgVersion","value":"'"$(cat /tmp/current-backup | grep "^pg_version:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/startLsn","value":"'"$(cat /tmp/current-backup | grep "^start_lsn:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/finishLsn","value":"'"$(cat /tmp/current-backup | grep "^finish_lsn:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/isPermanent","value":'"$(cat /tmp/current-backup | grep "^is_permanent:" | cut -d : -f 2-)"'},
      {"op":"replace","path":"/status/systemIdentifier","value":"'"$(cat /tmp/current-backup | grep "^system_identifier:" | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/uncompressedSize","value":'"$(cat /tmp/current-backup | grep "^uncompressed_size:" | cut -d : -f 2-)"'},
      {"op":"replace","path":"/status/compressedSize","value":'"$(cat /tmp/current-backup | grep "^compressed_size:" | cut -d : -f 2-)"'}
      ]'
  fi
  kubectl get "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" \
    --template "{{ range .items }}{{ .spec.clusterName }}:{{ .metadata.name }}:{{ .status.phase }}:{{ with .status.name }}{{ . }}{{ end }}{{ printf "'"\n"'" }}{{ end }}" \
    | grep "^$CLUSTER_NAME:" \
    > /tmp/backups
  cat /tmp/backup-list | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
    | grep '"backup_name"' \
    > /tmp/existing-backups
  for existing_backup in $(cat /tmp/existing-backups)
  do
    existing_backup_name="$(echo "$existing_backup" | tr -d '{}"' | tr ',' '\n' | grep 'backup_name' | cut -d : -f 2-)"
    if ! cat /tmp/backups | cut -d : -f 4 | grep -q "^$existing_backup_name$"
    then
     cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-replica-or-primary)" -c patroni \
       -- sh -l -ex
wal-g delete before "$existing_backup_name" --confirm
EOF
    fi 
  done
  for backup in $(cat /tmp/backups)
  do
    backup_name="$(echo "$backup" | cut -d : -f 4)"
    backup_cr_name="$(echo "$backup" | cut -d : -f 2)"
    backup_phase="$(echo "$backup" | cut -d : -f 3)"
    if [ ! -z "$backup_name" ] && [ "$backup_phase" = "$BACKUP_PHASE_COMPLETED" ] \
      && ! grep -q "\"backup_name\":\"$backup_name\"" /tmp/existing-backups
    then
      kubectl delete "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$backup_cr_name"
    fi
  done
else
  kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
    {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
    {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
    {"op":"replace","path":"/status/failureReason","value":"Backup name not found in backup-push log:\n'"$(cat /tmp/backup-push | to_json_string)"'"}
    ]'
  exit 1
fi