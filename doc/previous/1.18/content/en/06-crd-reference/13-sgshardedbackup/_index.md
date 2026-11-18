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



A manual or automatically generated sharded backup of an SGShardedCluster configured with an SGObjectStorage.

When an SGShardedBackup is created a Job will perform a full sharded backup of the database and update the status of the SGShardedBackup
 with all the information required to restore it and some stats (or a failure message in case something unexpected
 happened).
After an SGShardedBackup is created the same Job performs a reconciliation of the sharded backups by applying the retention window
 that has been configured in the SGObjectStorage and removing the sharded backups with managed lifecycle and the WAL files older
 than the ones that fit in the retention window. The reconciliation also removes sharded backups (excluding WAL files) that do
 not belong to any SGBackup. If the target storage of the SGObjectStorage is changed deletion of an SGBackup sharded backups
 with managed lifecycle and the WAL files older than the ones that fit in the retention window and of sharded backups that do
 not belong to any SGBackup will not be performed anymore on the previous storage, only on the new target storage.


{{< crd-properties >}}
[
  {
    "description": "stackgres.io/v1",
    "immutable": true,
    "name": "apiVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "SGShardedBackup",
    "immutable": true,
    "name": "kind",
    "required": true,
    "type": "string"
  },
  {
    "description": "Refer to the Kubernetes API documentation for the fields of the `metadata` field.",
    "link": "https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta",
    "name": "metadata",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedbackupspec",
    "description": "",
    "name": "spec",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedbackupstatus",
    "description": "",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGShardedBackup.spec
<sup><sup>[↩ Parent](#sgshardedbackup)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Indicate if this sharded backup is permanent and should not be removed by the automated\n retention policy. Default is `false`.",
    "name": "managedLifecycle",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The maximum number of retries the backup operation is allowed to do after a failure.\n\nA value of `0` (zero) means no retries are made. Defaults to: `3`.",
    "name": "maxRetries",
    "required": false,
    "type": "integer"
  },
  {
    "default": 300,
    "description": "Allow to set a timeout for the reconciliation process that takes place after the backup.\n\nIf not set defaults to 300 (5 minutes). If set to 0 it will disable timeout.\n\nFailure of reconciliation will not make the backup fail and will be re-tried the next time a SGBackup\n or scheduled backup Job takes place.",
    "name": "reconciliationTimeout",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The name of the `SGShardedCluster` from which this sharded backup is/will be taken.\n\nIf this is a copy of an existing completed sharded backup in a different namespace\n the value must be prefixed with the namespace of the source backup and a\n dot `.` (e.g. `\u003csharded cluster namespace\u003e.\u003csharded cluster name\u003e`) or have the same value\n if the source sharded backup is also a copy.",
    "name": "sgShardedCluster",
    "required": false,
    "type": "string"
  },
  {
    "description": "Allow to set a timeout for the backup creation.\n\nIf not set it will be disabled and the backup operation will continue until the backup completes or fails. If set to 0 is the same as not being set.\n\nMake sure to set a reasonably high value in order to allow for any unexpected delays during backup creation (network low bandwidth, disk low throughput and so forth).",
    "name": "timeout",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

## SGShardedBackup.status
<sup><sup>[↩ Parent](#sgshardedbackup)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedbackupstatusbackupinformation",
    "description": "",
    "name": "backupInformation",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedbackupstatusprocess",
    "description": "",
    "name": "process",
    "required": false,
    "type": "object"
  },
  {
    "description": "The list of SGBackups that compose the SGShardedBackup used to restore the sharded cluster.",
    "name": "sgBackups",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGShardedBackup.status.backupInformation
<sup><sup>[↩ Parent](#sgshardedbackupstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Postgres version of the server where the sharded backup is taken from.",
    "name": "postgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedbackupstatusbackupinformationsize",
    "description": "",
    "name": "size",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedBackup.status.backupInformation.size
<sup><sup>[↩ Parent](#sgshardedbackupstatusbackupinformation)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Size (in bytes) of the compressed sharded backup.",
    "format": "int64",
    "name": "compressed",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Size (in bytes) of the uncompressed sharded backup.",
    "format": "int64",
    "name": "uncompressed",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

### SGShardedBackup.status.process
<sup><sup>[↩ Parent](#sgshardedbackupstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "If the status is `failed` this field will contain a message indicating the failure reason.",
    "name": "failure",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the pod assigned to the sharded backup. StackGres utilizes internally a locking mechanism based on the pod name of the job that creates the sharded backup.",
    "name": "jobPod",
    "required": false,
    "type": "string"
  },
  {
    "description": "Status of the sharded backup.",
    "name": "status",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedbackupstatusprocesstiming",
    "description": "",
    "name": "timing",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedBackup.status.process.timing
<sup><sup>[↩ Parent](#sgshardedbackupstatusprocess)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "End time of sharded backup.",
    "name": "end",
    "required": false,
    "type": "string"
  },
  {
    "description": "Start time of sharded backup.",
    "name": "start",
    "required": false,
    "type": "string"
  },
  {
    "description": "Time at which the sharded backup is safely stored in the object storage.",
    "name": "stored",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}
