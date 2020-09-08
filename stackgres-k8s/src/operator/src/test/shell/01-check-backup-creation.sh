#!/bin/bash

shopt -s expand_aliases

CLUSTER_NAMESPACE=test
CLUSTER_NAME=test
CRONJOB_NAME=test
POD_NAME=test
BACKUP_NAME=test
BACKUP_CRD_NAME=sgbackups.stackgres.io
BACKUP_CONFIG_CRD_NAME=sgbackupconfigs.stackgres.io
BACKUP_PHASE_RUNNING=Running
BACKUP_PHASE_COMPLETED=Completed
BACKUP_PHASE_FAILED=Failed
SCHEDULED_BACKUP_KEY=scheduled-backup
RIGHT_VALUE=true
BACKUP_CONFIG=test
PATRONI_CLUSTER_LABELS=app=StackGresCluster,cluster-uid=test,cluster-namespace=test,cluster-name=test
PATRONI_ROLE_KEY=role
PATRONI_PRIMARY_ROLE=master
PATRONI_REPLICA_ROLE=replica
BACKUP_ENV=backup

alias kubectl=kubectl_mock

COUNT=0
kubectl_mock() {
  case "$*" in
    ("get cronjob.batch "*)
    echo '
      LOCK_POD=test
      LOCK_TIMESTAMP=$(date +%s)
      RESOURCE_VERSION=test
      '
    ;;
    ("annotate cronjob.batch "*)
    ;;
    ("get $BACKUP_CONFIG_CRD_NAME -n $CLUSTER_NAMESPACE $BACKUP_CONFIG --template {{ .metadata.resourceVersion }}")
    echo test
    ;;
    ("get $BACKUP_CRD_NAME -n $CLUSTER_NAMESPACE --template $BACKUP_CR_TEMPLATE")
    ;;
    ("get $BACKUP_CONFIG_CRD_NAME -n $CLUSTER_NAMESPACE $BACKUP_CONFIG --template $BACKUP_CONFIG_YAML")
    cat << EOF
    baseBackups:
      compression: "lz4"
    storage:
      type: "s3Compatible"
      s3Compatible:
        awsCredentials:
          secretKeySelectors:
            accessKeyId:
              key: "accesskey"
              name: "minio"
            secretAccessKey:
              key: "secretkey"
              name: "minio"
        endpoint: "http://minio.stackgres.svc:9000"
        enablePathStyleAddressing: true
        bucket: "stackgres"
        region: "k8s"
EOF
    ;;
    ("get $BACKUP_CRD_NAME -n $CLUSTER_NAMESPACE $BACKUP_NAME -o name")
    return 1
    ;;
    ("create -f - -o yaml")
    ;;
    ("get $BACKUP_CRD_NAME -n $CLUSTER_NAMESPACE $BACKUP_NAME --template {{ .status.sgBackupConfig.storage }}")
    echo test
    ;;
    ("get pod -n $CLUSTER_NAMESPACE -l ${PATRONI_CLUSTER_LABELS},${PATRONI_ROLE_KEY}=${PATRONI_PRIMARY_ROLE} -o name")
    echo pod/test-0
    ;;
    ("get pod -n $CLUSTER_NAMESPACE -l ${PATRONI_CLUSTER_LABELS},${PATRONI_ROLE_KEY}=${PATRONI_REPLICA_ROLE} -o name")
    echo pod/test-1
    ;;
    ("patch $BACKUP_CRD_NAME -n $CLUSTER_NAMESPACE $BACKUP_NAME --type json --patch "*)
    ;;
    ("get pod -n $CLUSTER_NAMESPACE --template "*)
    ;;
    ("exec -i -n $CLUSTER_NAMESPACE pod/test-0 -c patroni -- sh -e"*)
    STDIN="$(cat)"
    case "$STDIN" in
      (*" wal-g backup-push "*)
      cat << EOF
 Wrote backup with name test
EOF
      ;;
      ("pg_controldata "*)
      cat "$(dirname "$0")/pg_controldata"
      ;;
    esac
    ;;
    ("exec -i -n $CLUSTER_NAMESPACE pod/test-1 -c patroni -- sh -e"*)
    STDIN="$(cat)"
    case "$STDIN" in
      (*" for each existing backup"*)
      ;;
      (*" wal-g backup-list "*)
      cat "$(dirname "$0")/wal-g-backup-list"
      ;;
    esac
    ;;
    *)
    >&2 echo "$*"
    >&2 echo "Mock missing!"
    exit 1
    ;;
  esac
}

. "$1"