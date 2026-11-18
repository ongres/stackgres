---
title: SGBackup
weight: 6
url: /reference/crd/sgbackup
description: Details about SGBackup
showToc: true
---

___

**Kind:** SGBackup

**listKind:** SGBackupList

**plural:** sgbackups

**singular:** sgbackup

**shortNames** sgbkp
___

The `SGBackup` custom resource represents a backup of the Postgres cluster.
Backups are created automatically by a cron job configured using the settings in the [backup configuration]({{% relref "06-crd-reference/01-sgcluster" %}}#sgclusterspecconfigurationsbackupsindex) or manually by creating a `SGBackup`.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  name: backup
spec:
  sgCluster: stackgres
  managedLifecycle: true
status:
  internalName: base_00000002000000000000000E
  sgBackupConfig:
    baseBackups:
      compression: lz4
    storage:
      s3Compatible:
        awsCredentials:
          secretKeySelectors:
            accessKeyId:
              key: accesskey
              name: minio
            secretAccessKey:
              key: secretkey
              name: minio
        endpoint: http://minio:9000
        enablePathStyleAddressing: true
        bucket: stackgres
        region: k8s
      type: s3Compatible
  process:
    status: Completed
    jobPod: backup-backup-q79zq
    managedLifecycle: true
    timing:
      start: "2020-01-22T10:17:24.983902Z"
      stored: "2020-01-22T10:17:27.183Z"
      end: "2020-01-22T10:17:27.165204Z"
  backupInformation:
    hostname: stackgres-1
    systemIdentifier: "6784708504968245298"
    postgresVersion: "110006"
    pgData: /var/lib/postgresql/data
    size:
      compressed: 6691164
      uncompressed: 24037844
    lsn:
      start: "234881064"
      end: "234881272"
    startWalFile: 00000002000000000000000E
```

See also [Backups section]({{%  relref "04-administration-guide/05-backups#backups" %}}).

The SGBackup represents a manual or automatically generated backup of an SGCluster configured with an SGObjectStorage.

When a SGBackup is created a Job will perform a full backup of the database and update the status of the SGBackup
 with the all the information required to restore it and some stats (or a failure message in case something unexpected
 happened).
After an SGBackup is created the same Job performs a reconciliation of the backups by applying the retention window
 that has been configured in the SGObjectStorage and removing the backups with managed lifecycle and the WAL files older
 than the ones that fit in the retention window. The reconciliation also removes backups (excluding WAL files) that do
 not belongs to any SGBackup. If the target storage of the SGObjectStorage is changed deletion of an SGBackup backups
 with managed lifecycle and the WAL files older than the ones that fit in the retention window and of backups that do
 not belongs to any SGBackup will not be performed anymore on the previous storage, only on the new target storage.

{{% include "generated/SGBackup.md" %}}
