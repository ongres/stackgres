#!/bin/sh

kubectl apply -f sgcluster.yaml

cat << EOF > pgpass
test-primary.test.svc:5432:pgbench:pguser:pguser
test-replicas.test.svc:5432:pgbench:pguser:pguser
EOF

cat << EOF > pg_service.conf
[pgbench]
host=test-primary.test.svc
port=5432
dbname=pgbench
[pgbenchreplica]
host=test-replicas.test.svc
port=5432
dbname=pgbench
EOF

accessKeyId=$(kubectl get secret -n minio minio-minio -o jsonpath='{.data.accesskey}' | base64 -d)
secretAccessKey=$(kubectl get secret -n minio minio-minio -o jsonpath='{.data.secretkey}' | base64 -d)

kubectl --namespace "test" create secret generic minio-backup-bucket-secret \
  --from-literal="accessKeyId=$accessKeyId" \
  --from-literal="secretAccessKey=$secretAccessKey"

  cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1
kind: SGBackupConfig
metadata:
  namespace: test
  name: backupconfig
spec:
  baseBackups:
    cronSchedule: '*/5 * * * *'
    retention: 3
  storage:
    type: 's3Compatible'
    s3Compatible:
      bucket: stackgres
      endpoint: http://minio.minio.svc:9000
      enablePathStyleAddressing: true
      region: k8s
      awsCredentials:
        secretKeySelectors:
          accessKeyId: {name: 'minio-backup-bucket-secret', key: 'accessKeyId'}
          secretAccessKey: {name: 'minio-backup-bucket-secret', key: 'secretAccessKey'}
EOF

kubectl create secret generic pgbench --from-file=.pgpass=pgpass --from-file=.pg_service.conf=pg_service.conf -n "test"

kubectl apply -f pgbench.yaml

kubectl exec -it pgbench -c pgbench -n "test" -- pgbench -i -U pguser pgbench

/usr/lib/postgresql/13.5/bin/postgres -D /var/lib/postgresql/data --config-file=/var/lib/postgresql/data/postgresql.conf --listen_addresses=127.0.0.1 --port=5432 --cluster_name=restore --wal_level=logical --hot_standby=off --max_connections=100 --max_wal_senders=20 --max_prepared_transactions=32 --max_locks_per_transaction=128 --track_commit_timestamp=off --max_replication_slots=20 --max_worker_processes=8 --wal_log_hints=on