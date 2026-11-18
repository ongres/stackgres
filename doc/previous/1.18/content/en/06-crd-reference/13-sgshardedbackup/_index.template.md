---
title: SGShardedBackup
weight: 6
url: /reference/crd/sgshardedbackup
description: Details about SGShardedBackup
showToc: true
---

___

**Kind:** SGShardedBackup

**listKind:** SGShardedBackupList

**plural:** sgbackups

**singular:** sgbackup

**shortNames** sgbkp
___

The `SGShardedBackup` custom resource represents a backup of the sharded Postgres cluster.
Backups are created automatically by a cron job configured using the settings in the [backup configuration]({{% relref "06-crd-reference/01-sgcluster" %}}#sgshardedclusterspecconfigurationsbackupsindex) or manually by creating a `SGShardedBackup`.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedBackup
metadata:
  name: backup
spec:
  sgShardedCluster: stackgres
  managedLifecycle: true
status:
  sgBackups:
  - backup-coord
  - backup-shard0
  - backup-shard1
  process:
    status: Completed
    jobPod: backup-backup-q79zq
    timing:
      start: "2020-01-22T10:17:24.983902Z"
      stored: "2020-01-22T10:17:27.183Z"
      end: "2020-01-22T10:17:27.165204Z"
  backupInformation:
    postgresVersion: "11.6"
    size:
      compressed: 6691164
      uncompressed: 24037844
```

See also [Backups section]({{%  relref "04-administration-guide/05-backups#backups" %}}).

The SGShardedBackup represents a manual or automatically generated sharded backup of an SGShardedCluster configured with an SGObjectStorage.

When a SGShardedBackup is created a Job will perform a full backup for the coordinator and each shard that will be stored in a
 SGBackup and will update the status of the SGShardedBackup with the all the information required to restore it and some stats
 (or a failure message in case something unexpected happened).
After an SGShardedBackup is created the same Job performs a reconciliation of the sharded backups by applying the retention window
 that has been configured in the SGObjectStorage and removing the sharded backups with managed lifecycle and the WAL files older
 than the ones that fit in the retention window. The reconciliation also removes backups (excluding WAL files) that do
 not belongs to any SGShardedBackup. If the target storage of the SGObjectStorage is changed deletion of an SGShardedBackup backups
 with managed lifecycle and the WAL files older than the ones that fit in the retention window and of backups that do
 not belongs to any SGShardedBackup will not be performed anymore on the previous storage, only on the new target storage.

{{% include "generated/SGShardedBackup.md" %}}
