CURRENT_TIMESTAMP="$(date +%s)"
kubectl get cronjob.batch -n "${CLUSTER_NAMESPACE}" "${CLUSTER_NAME}"-backup --template $'
BACKUP_STATUS={{ if .metadata.annotations.backupStatus }}{{ .metadata.annotations.backupStatus }}{{ else }}completed{{ end }}\n
BACKUP_TIMESTAMP={{ if .metadata.annotations.backupTimestamp }}{{ .metadata.annotations.backupTimestamp }}{{ else }}0{{ end }}\n
RESOURCE_VERSION={{ .metadata.resourceVersion }}\n
LAST_SCHEDULE_TIME="$(date -d "{{ .status.lastScheduleTime }}" +%s)"\n
' > /tmp/current-backup-cronjob
source /tmp/current-backup-cronjob
if [ "$BACKUP_STATUS" == "completed" ] \
  || [ "$(kubectl get pod -n "${CLUSTER_NAMESPACE}" \
    -l "${CLUSTER_LABELS},role=backup" -o name | grep -v "${POD_NAME}" | wc -l)" -eq 0 ]
then
  kubectl annotate cronjob.batch -n "${CLUSTER_NAMESPACE}" "${CLUSTER_NAME}"-backup \
    --resource-version "$RESOURCE_VERSION" "backupStatus=started,backupTimestamp=$CURRENT_TIMESTAMP"
  kubectl get pod -n "${CLUSTER_NAMESPACE}" -l "${CLUSTER_LABELS},role=maste" -o name \
    | xargs -I % kubectl exec -n "${CLUSTER_NAMESPACE}" % -c patroni \
    -- sh -l -c "wal-g backup-push /var/lib/postgresql/data/ -f; wal-g delete retain FULL $RETAIN" || true
  kubectl annotate cronjob.batch -n "${CLUSTER_NAMESPACE}" "${CLUSTER_NAME}"-backup \
    --resource-version "$RESOURCE_VERSION" "backupStatus=completed,backupTimestamp=$CURRENT_TIMESTAMP"
fi
