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
  process:
    sgBackups:
    - backup-coord
    - backup-shard0
    - backup-shard1
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



A manual or automatically generated sharded backup of an SGCluster configured with an SGBackupConfig.

When a SGBackup is created a Job will perform a full sharded backup of the database and update the status of the SGBackup
 with the all the information required to restore it and some stats (or a failure message in case something unexpected
 happened).
After an SGBackup is created the same Job performs a reconciliation of the sharded backups by applying the retention window
 that has been configured in the SGBackupConfig and removing the sharded backups with managed lifecycle and the WAL files older
 than the ones that fit in the retention window. The reconciliation also removes sharded backups (excluding WAL files) that do
 not belongs to any SGBackup. If the target storage of the SGBackupConfig is changed deletion of an SGBackup sharded backups
 with managed lifecycle and the WAL files older than the ones that fit in the retention window and of sharded backups that do
 not belongs to any SGBackup will not be performed anymore on the previous storage, only on the new target storage.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr><tr>
      <td><b>apiVersion</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>stackgres.io/v1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGShardedBackup</td>
      </tr>
      <tr>
      <td><b><a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta">metadata</a></b></td>
      <td>✓</td>
      <td>✓</td>
      <td></td>
      <td>object</td>
      <td>Refer to the Kubernetes API documentation for the fields of the `metadata` field.</td>
      </tr>
      <tr>
        <td><b><a href="#sgshardedbackupspec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardedbackupstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

## SGShardedBackup.spec
<sup><sup>[↩ Parent](#sgshardedbackup)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>managedLifecycle</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Indicate if this sharded backup is permanent and should not be removed by the automated
 retention policy. Default is `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The maximum number of retries the backup operation is allowed to do after a failure.

A value of `0` (zero) means no retries are made. Defaults to: `3`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>reconciliationTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Allow to set a timeout for the reconciliation process that take place after the backup.

If not set defaults to 300 (5 minutes). If set to 0 it will disable timeout.

Failure of reconciliation will not make the backup fail and will be re-tried the next time a SGBackup
 or shecduled backup Job take place.
<br/>
          <br/>
            <i>Default</i>: 300<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgShardedCluster</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the `SGShardedCluster` from which this sharded backup is/will be taken.

If this is a copy of an existing completed sharded backup in a different namespace
 the value must be prefixed with the namespace of the source backup and a
 dot `.` (e.g. `<sharded cluster namespace>.<sharded cluster name>`) or have the same value
 if the source sharded backup is also a copy.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>timeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Allow to set a timeout for the backup creation.

If not set it will be disabled and the backup operation will continue until the backup completes or fail. If set to 0 is the same as not being set.

Make sure to set a reasonable high value in order to allow for any unexpected delays during backup creation (network low bandwidth, disk low throughput and so forth).
<br/>
        </td>
      </tr></tbody>
</table>

## SGShardedBackup.status
<sup><sup>[↩ Parent](#sgshardedbackup)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardedbackupstatusbackupinformation">backupInformation</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardedbackupstatusprocess">process</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b>sgBackups</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The list of SGBackups that compose the SGShardedBackup used to restore the sharded cluster.
<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedBackup.status.backupInformation
<sup><sup>[↩ Parent](#sgshardedbackupstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>postgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Postgres version of the server where the sharded backup is taken from.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardedbackupstatusbackupinformationsize">size</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

#### SGShardedBackup.status.backupInformation.size
<sup><sup>[↩ Parent](#sgshardedbackupstatusbackupinformation)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>compressed</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Size (in bytes) of the compressed sharded backup.
<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr>
      <tr>
        <td><b>uncompressed</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Size (in bytes) of the uncompressed sharded backup.
<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedBackup.status.process
<sup><sup>[↩ Parent](#sgshardedbackupstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If the status is `failed` this field will contain a message indicating the failure reason.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>jobPod</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the pod assigned to the sharded backup. StackGres utilizes internally a locking mechanism based on the pod name of the job that creates the sharded backup.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>status</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Status of the sharded backup.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardedbackupstatusprocesstiming">timing</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

#### SGShardedBackup.status.process.timing
<sup><sup>[↩ Parent](#sgshardedbackupstatusprocess)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>end</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          End time of sharded backup.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>start</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Start time of sharded backup.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>stored</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Time at which the sharded backup is safely stored in the object storage.
<br/>
        </td>
      </tr></tbody>
</table>
