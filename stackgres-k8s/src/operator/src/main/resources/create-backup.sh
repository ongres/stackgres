set -e

to_json_string() {
  sed ':a;N;$!ba;s/\n/\\n/g' | sed 's/\(["\\\t]\)/\\\1/g' | tr '\t' 't'
}

try_lock() {
  kubectl get cronjob.batch -n "$CLUSTER_NAMESPACE" "$CRONJOB_NAME" --template '
  LOCK_POD={{ if .metadata.annotations.lockPod }}{{ .metadata.annotations.lockPod }}{{ else }}{{ end }}
  LOCK_TIMESTAMP={{ if .metadata.annotations.lockTimestamp }}{{ .metadata.annotations.lockTimestamp }}{{ else }}0{{ end }}
  RESOURCE_VERSION={{ .metadata.resourceVersion }}
  ' > /tmp/current-backup-job
  . /tmp/current-backup-job
  CURRENT_TIMESTAMP="$(date +%s)"
  if [ "$POD_NAME" != "$LOCK_POD" ] && [ "$((CURRENT_TIMESTAMP-LOCK_TIMESTAMP))" -lt 15 ]
  then
    echo "Locked already by $LOCK_POD at $(date -d @"$LOCK_TIMESTAMP" --iso-8601 --utc)"
    exit 1
  fi
  kubectl annotate cronjob.batch -n "$CLUSTER_NAMESPACE" "$CRONJOB_NAME" \
    --resource-version "$RESOURCE_VERSION" --overwrite "lockPod=$POD_NAME" "lockTimestamp=$CURRENT_TIMESTAMP"
}

try_lock > /tmp/try-lock
echo "Lock acquired"
(
while true
do
  sleep 5
  try_lock > /tmp/try-lock
done
) &
try_lock_pid=$!

backup_cr_template="{{ range .items }}"
backup_cr_template="${backup_cr_template}{{ .spec.cluster }}"
backup_cr_template="${backup_cr_template}:{{ .metadata.name }}"
backup_cr_template="${backup_cr_template}:{{ .status.phase }}"
backup_cr_template="${backup_cr_template}:{{ with .status.name }}{{ . }}{{ end }}"
backup_cr_template="${backup_cr_template}:{{ with .status.pod }}{{ . }}{{ end }}"
backup_cr_template="${backup_cr_template}:{{ with .metadata.ownerReferences }}{{ with index . 0 }}{{ .kind }}{{ end }}{{ end }}"
backup_cr_template="${backup_cr_template}:{{ if .spec.isPermanent }}true{{ else }}false{{ end }}"
backup_cr_template="${backup_cr_template}{{ printf "'"\n"'" }}{{ end }}"
kubectl get "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" \
  --template "$backup_cr_template" > /tmp/all-backups
grep "^$CLUSTER_NAME:" /tmp/all-backups > /tmp/backups || true

if [ "$IS_CRONJOB" = true ]
then
  BACKUP_NAME="${CLUSTER_NAME}-${POD_UID}"
fi

BACKUP_CONFIG_RESOURCE_VERSION="$(kubectl get sgbackupconfig -n "$CLUSTER_NAMESPACE" "$BACKUP_CONFIG" --template '{{ .metadata.resourceVersion }}')"

if ! kubectl get "$BACKUP_CRD_NAME" "$BACKUP_NAME" -o name >/dev/null 2>&1
then
  echo "Creating backup CR"
  cat << EOF | kubectl create -f -
apiVersion: $BACKUP_CRD_APIVERSION
kind: $BACKUP_CRD_KIND
metadata:
  namespace: "$CLUSTER_NAMESPACE"
  name: "$BACKUP_NAME"
  ownerReferences:
$(kubectl get cronjob -n "$CLUSTER_NAMESPACE" "$CRONJOB_NAME" \
  --template '  - apiVersion: {{ .apiVersion }}
    kind: {{ .kind }}
    name: {{ .metadata.name }}
    uid: {{ .metadata.uid }}
')
spec:
  cluster: "$CLUSTER_NAME"
  isPermanent: false
status:
  phase: "$BACKUP_PHASE_PENDING"
  pod: "$POD_NAME"
  backupConfig:
$(kubectl get sgbackupconfig -n "$CLUSTER_NAMESPACE" "$BACKUP_CONFIG" \
  --template '    compressionMethod: "{{ .spec.compressionMethod }}"
    {{- with .spec.pgpConfiguration }}
    pgpConfiguration:
      key:
        key: "{{ .spec.pgpConfiguration.key.key }}"
        name: "{{ .spec.pgpConfiguration.key.name }}"
    {{- end }}
    storage:
      type: "{{ .spec.storage.type }}"
      {{- with .spec.storage.s3 }}
      s3:
        prefix: "{{ .prefix }}"
        credentials:
          accessKey:
            key: "{{ .credentials.accessKey.key }}"
            name: "{{ .credentials.accessKey.name }}"
          secretKey:
            key: "{{ .credentials.secretKey.key }}"
            name: "{{ .credentials.secretKey.name }}"
        {{ with .region }}region: "{{ . }}"{{ end }}
        {{ with .endpoint }}endpoint: "{{ . }}"{{ end }}
        {{ with .forcePathStyle }}forcePathStyle: {{ . }}{{ end }}
        {{ with .storageClass }}storageClass: "{{ . }}"{{ end }}
        {{ with .sse }}sse: "{{ . }}"{{ end }}
        {{ with .sseKmsId }}sseKmsId: "{{ . }}"{{ end }}
        {{ with .cseKmsId }}cseKmsId: "{{ . }}"{{ end }}
        {{ with .cseKmsRegion }}cseKmsRegion: "{{ . }}"{{ end }}
      {{- end }}
      {{- with .spec.storage.gcs }}
      gcs:
        prefix: "{{ .prefix }}"
        credentials:
          serviceAccountJsonKey:
            key: "{{ .credentials.serviceAccountJsonKey.key }}"
            name: "{{ .credentials.serviceAccountJsonKey.name }}"
      {{- end }}
      {{- with .spec.storage.azureblob }}
      azureblob:
        prefix: "{{ .prefix }}"
        credentials:
          account:
            key: "{{ .credentials.account.key }}"
            name: "{{ .credentials.account.name }}"
          accessKey:
            key: "{{ .credentials.accessKey.key }}"
            name: "{{ .credentials.accessKey.name }}"
        {{ with .bufferSize }}bufferSize: {{ . }}{{ end }}
        {{ with .maxBuffers }}maxBuffers: {{ . }}{{ end }}
      {{- end }}
')
EOF
else
  if ! kubectl get "$BACKUP_CRD_NAME" "$BACKUP_NAME" --template "{{ .status.phase }}" \
    | grep -q "^$BACKUP_PHASE_COMPLETED$"
  then
    echo "Updating backup CR"
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status","value":{
        "phase":"'"$BACKUP_PHASE_PENDING"'",
        "pod":"'"$POD_NAME"'",
        "backupConfig":{'"$(kubectl get sgbackupconfig -n "$CLUSTER_NAMESPACE" "$BACKUP_CONFIG" \
  --template '    "compressionMethod": "{{ .spec.compressionMethod }}",
    {{- with .spec.pgpConfiguration }}
    "pgpConfiguration": {
      "key": {
        "key": "{{ .spec.pgpConfiguration.key.key }}",
        "name": "{{ .spec.pgpConfiguration.key.name }}"
      }
    },
    {{- end }}
    "storage": {
      "type": "{{ .spec.storage.type }}",
      {{- with .spec.storage.s3 }}
      "s3": {
        "prefix": "{{ .prefix }}",
        "credentials": {
          "accessKey": {
            "key": "{{ .credentials.accessKey.key }}",
            "name": "{{ .credentials.accessKey.name }}"
          },
          "secretKey": {
            "key": "{{ .credentials.secretKey.key }}",
            "name": "{{ .credentials.secretKey.name }}"
          }
        }
        {{ with .region }},"region": "{{ . }}"{{ end }}
        {{ with .endpoint }},"endpoint": "{{ . }}"{{ end }}
        {{ with .forcePathStyle }},"forcePathStyle": {{ . }}{{ end }}
        {{ with .storageClass }},"storageClass": "{{ . }}"{{ end }}
        {{ with .sse }},"sse": "{{ . }}"{{ end }}
        {{ with .sseKmsId }},"sseKmsId": "{{ . }}"{{ end }}
        {{ with .cseKmsId }},"cseKmsId": "{{ . }}"{{ end }}
        {{ with .cseKmsRegion }},"cseKmsRegion": "{{ . }}"{{ end }}
      }
      {{- end }}
      {{- with .spec.storage.gcs }}
      "gcs": {
        "prefix": "{{ .prefix }}",
        "credentials": {
          "serviceAccountJsonKey": {
            "key": "{{ .credentials.serviceAccountJsonKey.key }}",
            "name": "{{ .credentials.serviceAccountJsonKey.name }}"
          }
        }
      {{- end }}
      {{- with .spec.storage.azureblob }}
      "azureblob": {
        "prefix": "{{ .prefix }}",
        "credentials": {
          "account": {
            "key": "{{ .credentials.account.key }}",
            "name": "{{ .credentials.account.name }}"
          },
          "accessKey": {
            "key": "{{ .credentials.accessKey.key }}",
            "name": "{{ .credentials.accessKey.name }}"
          }
        }
        {{ with .bufferSize }},"bufferSize": {{ . }}{{ end }}
        {{ with .maxBuffers }},"maxBuffers": {{ . }}{{ end }}
      }
      {{- end }}
    }
')"'}}}
      ]'
  else
    echo "Already completed backup. Nothing to do!"
    exit
  fi
fi

echo "Performing backup"
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
cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-primary)" -c patroni \
  -- sh -l -e > /tmp/backup-push 2>&1
wal-g backup-push /var/lib/postgresql/data/ -f $([ "$BACKUP_IS_PERMANENT" = true ] && echo '-p' || true)
EOF
echo "Backup completed"
set +e
echo "Cleaning up old backups"
cat << EOF | kubectl exec -i -n "$CLUSTER_NAMESPACE" "$(cat /tmp/current-replica-or-primary)" -c patroni \
  -- sh -e -l $(! echo $- | grep -q x || echo " -x")
echo '$(cat /tmp/backups)' \
  | grep '^[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:true' \
  | cut -d : -f 4 \
  | grep -v '^$' \
  | while read backup
    do
      if wal-g backup-list --detail --json \
        | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
        | grep '"backup_name"' \
        | grep "\"backup_name\":\"\$backup\"" \
        | grep -q "\"is_permanent\":false"
      then
        wal-g backup-mark "\$backup"
      fi
    done
echo '$(cat /tmp/backups)' \
  | grep -v '^[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:true' \
  | cut -d : -f 4 \
  | grep -v '^$' \
  | while read backup
    do
      if wal-g backup-list --detail --json \
        | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
        | grep '"backup_name"' \
        | grep "\"backup_name\":\"\$backup\"" \
        | grep -q "\"is_permanent\":true"
      then
        wal-g backup-mark -i "\$backup"
      fi
    done
wal-g backup-list --detail --json \
  | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
  | grep '"backup_name"' \
  | grep -q "\"is_permanent\":true" \
  | tr -d '{}"' | tr ',' '\n' | grep 'backup_name' | cut -d : -f 2- \
  | while read backup
    do
      if ! echo '$(cat /tmp/backups)' \
        | cut -d : -f 4 \
        | grep -q '^\$backup$'
      then
        wal-g backup-mark -i "\$backup"
      fi
    done
PERMANENT="$(wal-g backup-list --detail --json \
  | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
  | grep '"backup_name"' \
  | grep "\"is_permanent\":true" \
  | wc -l)"
wal-g delete retain FULL "$((RETAIN+PERMANENT))" --confirm
EOF
if [ "$?" = 0 ]
then
  echo "Cleanup completed"
else
  echo "Cleanup failed"
fi
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
    {"op":"replace","path":"/status/failureReason","value":"Lock lost:\n'"$(cat /tmp/try-lock | to_json_string)"'"}
    ]'
  cat /tmp/try-lock
  echo "Lock lost"
  exit 1
else
  kill "$try_lock_pid"
  if [ "$RESULT" != 0 ]
  then
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
      {"op":"replace","path":"/status/failureReason","value":"Backup failed: '"$(cat /tmp/backup-push | to_json_string)"'"}
      ]'
    cat /tmp/backup-push
    echo "Backup failed"
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
    -- sh -l -e > /tmp/backup-list 2>&1
WALG_LOG_LEVEL= wal-g backup-list --detail --json
EOF
  RESULT=$?
  set -x
  if [ "$RESULT" != 0 ]
  then
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
      {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
      {"op":"replace","path":"/status/failureReason","value":"Backup can not be listed after creation '"$(cat /tmp/backup-list | to_json_string)"'"}
      ]'
    cat /tmp/backup-list
    echo "Backups can not be listed after creation"
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
    cat /tmp/backup-list
    echo "Backup '$WAL_G_BACKUP_NAME' was not found after creation"
    exit 1
  elif [ "$BACKUP_CONFIG_RESOURCE_VERSION" != "$(kubectl get sgbackupconfig -n "$CLUSTER_NAMESPACE" "$BACKUP_CONFIG" --template '{{ .metadata.resourceVersion }}')" ]
  then
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
      {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
      {"op":"replace","path":"/status/failureReason","value":"Backup configuration '"$BACKUP_CONFIG"' changed during backup"}
      ]'
    cat /tmp/backup-list
    echo "Backup configuration '"$BACKUP_CONFIG"' changed during backup"
    exit 1
  else
    kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
      {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_COMPLETED"'"},
      {"op":"replace","path":"/status/name","value":"'"$WAL_G_BACKUP_NAME"'"},
      {"op":"replace","path":"/status/failureReason","value":""},
      {"op":"replace","path":"/status/time","value":"'"$(grep "^time:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/walFileName","value":"'"$(grep "^wal_file_name:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/startTime","value":"'"$(grep "^start_time:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/finishTime","value":"'"$(grep "^finish_time:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/hostname","value":"'"$(grep "^hostname:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/dataDir","value":"'"$(grep "^data_dir:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/pgVersion","value":"'"$(grep "^pg_version:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/startLsn","value":"'"$(grep "^start_lsn:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/finishLsn","value":"'"$(grep "^finish_lsn:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/isPermanent","value":'"$(grep "^is_permanent:" /tmp/current-backup | cut -d : -f 2-)"'},
      {"op":"replace","path":"/status/systemIdentifier","value":"'"$(grep "^system_identifier:" /tmp/current-backup | cut -d : -f 2-)"'"},
      {"op":"replace","path":"/status/uncompressedSize","value":'"$(grep "^uncompressed_size:" /tmp/current-backup | cut -d : -f 2-)"'},
      {"op":"replace","path":"/status/compressedSize","value":'"$(grep "^compressed_size:" /tmp/current-backup | cut -d : -f 2-)"'}
      ]'
  fi
  echo "Cleaning up backup CRs"
  cat /tmp/backup-list | tr -d '[]' | sed 's/},{/}|{/g' | tr '|' '\n' \
    | grep '"backup_name"' \
    > /tmp/existing-backups
  kubectl get pod -n "$CLUSTER_NAMESPACE" \
    --template "{{ range .items }}{{ .metadata.name }}{{ printf "'"\n"'" }}{{ end }}" \
    > /tmp/pods
  for backup in $(cat /tmp/backups)
  do
    backup_cr_name="$(echo "$backup" | cut -d : -f 2)"
    backup_phase="$(echo "$backup" | cut -d : -f 3)"
    backup_name="$(echo "$backup" | cut -d : -f 4)"
    backup_pod="$(echo "$backup" | cut -d : -f 5)"
    backup_owner_kind="$(echo "$backup" | cut -d : -f 6)"
    if [ ! -z "$backup_name" ] && [ "$backup_phase" = "$BACKUP_PHASE_COMPLETED" ] \
      && ! grep -q "\"backup_name\":\"$backup_name\"" /tmp/existing-backups
    then
      kubectl delete "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$backup_cr_name"
    fi
    if [ "$backup_owner_kind" = "CronJob" ] \
      && [ "$backup_phase" = "$BACKUP_PHASE_PENDING" ] \
      && ([ -z "$backup_pod" ] || ! grep -q "^$backup_pod$" /tmp/pods)
    then
      kubectl delete "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$backup_cr_name"
    fi
  done
else
  kubectl patch "$BACKUP_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$BACKUP_NAME" --type json --patch '[
    {"op":"replace","path":"/status/phase","value":"'"$BACKUP_PHASE_FAILED"'"},
    {"op":"replace","path":"/status/failureReason","value":"Backup name not found in backup-push log:\n'"$(cat /tmp/backup-push | to_json_string)"'"}
    ]'
  cat /tmp/backup-push
  echo "Backup name not found in backup-push log"
  exit 1
fi
