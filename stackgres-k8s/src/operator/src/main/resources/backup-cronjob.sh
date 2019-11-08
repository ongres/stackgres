try_lock() {
  kubectl get cronjob.batch -n "${CLUSTER_NAMESPACE}" "${CLUSTER_NAME}"-backup --template '
  LOCK_POD={{ if .metadata.annotations.lockPod }}{{ .metadata.annotations.lockPod }}{{ else }}{{ end }}
  LOCK_TIMESTAMP={{ if .metadata.annotations.lockTimestamp }}{{ .metadata.annotations.lockTimestamp }}{{ else }}0{{ end }}
  RESOURCE_VERSION={{ .metadata.resourceVersion }}
  ' > /tmp/current-backup-cronjob
  source /tmp/current-backup-cronjob
  CURRENT_TIMESTAMP="$(date +%s)"
  if [ "$POD_NAME" != "$LOCK_POD" ] && [ "$((CURRENT_TIMESTAMP-LOCK_TIMESTAMP))" -lt 15 ]
  then
    echo "Locked already by $LOCK_POD at $(date -d @"$LOCK_TIMESTAMP" --iso-8601 --utc)"
    exit 1
  fi
  kubectl annotate cronjob.batch -n "${CLUSTER_NAMESPACE}" "${CLUSTER_NAME}"-backup \
    --resource-version "$RESOURCE_VERSION" --overwrite "lockPod=$POD_NAME" "lockTimestamp=$CURRENT_TIMESTAMP"
}

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
kubectl get pod -n "${CLUSTER_NAMESPACE}" -l "${CLUSTER_LABELS},role=master" -o name > /tmp/current-master
if [ ! -s /tmp/current-master ]
then
  kubectl get pod -n "${CLUSTER_NAMESPACE}" -l "${CLUSTER_LABELS}" >&2
  echo >&2
  echo "Unable to find master, backup aborted" >&2
  exit 1
fi
cat << EOF | kubectl exec -i -n "${CLUSTER_NAMESPACE}" "$(cat /tmp/current-master)" -c patroni \
  -- sh -l -ex
wal-g backup-push /var/lib/postgresql/data/ -f
wal-g delete retain FULL "$RETAIN" --confirm || true
EOF
) &
pid=$!

wait -n "$pid" "$try_lock_pid"
if kill -0 "$pid" 2>/dev/null
then
  kill "$pid"
else
  kill "$try_lock_pid"
fi