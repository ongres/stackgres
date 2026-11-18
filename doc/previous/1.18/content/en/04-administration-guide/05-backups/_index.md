---
title: Backups
weight: 4
url: /administration/backups
aliases: [ /install/prerequisites/backups , /tutorial/complete-cluster/backup-configuration ]
description: Details about how to set up and configure backups.
showToc: true
---

StackGres supports manual and automated backups, based on Postgres [continuous archiving](https://www.postgresql.org/docs/current/continuous-archiving.html), that is base backups plus WAL (write ahead log) archiving, as well as backup lifecycle management.
To achieve maximum durability, backups are stored on cloud/object storage and/or [volume snapshots](https://kubernetes.io/docs/concepts/storage/volume-snapshots/).
S3, GCP, Azure Blob, and S3-compatible object storages are supported as on cloud/object storage.

## Cluster Backup Configuration

All the configuration options related to backups can be found at the [SGCluster backups section]({{% relref "06-crd-reference/01-sgcluster/#backups" %}}).
When backups are configured, Postgres WAL files will start being archived in the specified storage at the specified path.
Also, automatic backups can be scheduled and (in such case) a retention policy of backups is created.
You will have to find out a time window and retention policy that fit your needs.
When configuring cluster backups, you may also specify the compression algorithm and performance-related options, such as the maximum disk and network throughput, or the parallelism for uploading files.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
  # [...]
  configurations:
    backups:
    - sgObjectStorage: # name of the referenced SGObjectStorage
      path: # leave this empty for the operator to fill it with a default value
            # and only configure if you restore the same cluster from its own backup
      cronSchedule: '0 5 0 0 0'
      retention: 5
      compression: # <lz4|lzma|brotli>
      performance:
        maxDiskBandwidth: # unlimited if left unset
        maxNetworkBandwidth: # unlimited if left unset
        uploadDiskConcurrency: # 1 by default
```

For more information, have a look at the [SGCluster backups section]({{% relref "06-crd-reference/01-sgcluster/#backups" %}}).


## Backup Storage

StackGres supports backups with the following storage options:

* [AWS S3](https://aws.amazon.com/s3/)
* [Google Cloud Storage](https://cloud.google.com/storage)
* [Azure Blob Storage](https://azure.microsoft.com/en-us/products/storage/blobs)
* S3-Compatible Storages:
  * [Self-hosted MinIO](https://min.io/)
  * [Alibaba OSS](https://www.alibabacloud.com/en/product/object-storage-service)
  * [DigitalOcean Spaces](https://www.digitalocean.com/products/spaces)
  * [Cloudflare R2](https://developers.cloudflare.com/r2/)

The examples below are using the [MinIO](https://min.io/) service as an S3-Compatible service for a quick setup on local Kubernetes clusters.
Although StackGres definitely recommends to choose a Storage-as-a-Service for production setups.

See also specific sections for some of the listed technologies:

{{% children style="li" depth="1" %}}

All the storage-related configuration is defined in the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD.

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
 name: objectstorage
spec:
  # fill the preferred storage method with
  # specific credentials and configurations
  type: # <s3|s3Compatible|gcs|azureBlob>
  s3: {}
  s3Compatible: {}
  gcs: {}
  azureBlob: {}
```

StackGres supports also backup based on Volume Snapshot that, in general, are faster than object storage for big volumes of data. This feature requires the VolumeSnapshot CRDs and controller to be installed in the Kubernetes cluster and to use a StorageClass for disks that supports the volume snapshot functionality. A backup based on VolumeSnapshot still requires WAL files that will be stored in the object storage defined by the SGObjectStorage.

## Backup Timeouts and Retries

You can configure timeout and retry behavior for backup operations at the cluster level or on individual SGBackup resources.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `timeout` | integer | disabled | Timeout in seconds for the backup creation. If not set or set to `0`, the backup runs until it completes or fails. Set to a high value to allow for unexpected delays (slow network, low disk throughput). |
| `reconciliationTimeout` | integer | `300` (5 minutes) | Timeout in seconds for the reconciliation process that runs after a backup completes. Set to `0` to disable. Reconciliation failures do not fail the backup and will be retried on the next backup. |
| `maxRetries` | integer | `3` | Maximum number of retries after a backup failure. Set to `0` to disable retries. |

These fields can be set in the SGCluster backup configuration:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
  configurations:
    backups:
    - sgObjectStorage: my-storage
      cronSchedule: '0 5 * * *'
      retention: 5
      timeout: 7200
      reconciliationTimeout: 600
      maxRetries: 5
```

They can also be set on individual SGBackup resources:

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  name: manual-backup
spec:
  sgCluster: cluster
  managedLifecycle: false
  timeout: 3600
  maxRetries: 2
```

## Backups

Backups metadata are stored using [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}).
An SGBackup can be created automatically by the scheduled backup process, manually, or by copying an existing SGBackup in order to make it accessible in another namespace.
Removing an SGBackup also triggers the removal of the actual backup associated with it, that is the files on the object storage that represent the backup (if they are accessible by the backup configuration used by the SGCluster). An SGBackup may also be removed automatically if the physical backup associated to it is also removed. The process of reconciliation for backups is executed after a backup is correctly performed. This process of reconciliation is also responsible of removing SGBackups with managed lifecycle that are out of the retention window size specified in field `SGCluster.spec.configurations.backups[0].retention` that indicates the number of backups with managed lifecycle that must be retained. Failed SGBackups (even with managed lifecycle) are not removed by the reconciliation in order to maintain the full list of failures that a user may need to inspect.

### Scheduled backups

When field `SGCluster.spec.configurations.backups[0].cronSchedule` is set the operator will create a CronJob that will be scheduling backup Jobs based on the [cron expression](https://en.wikipedia.org/wiki/Cron) specified in such field. These backup Job will create an SGBackup with managed lifecycle and will perform the backup. When the SGBackup completes successfully it will set the field `SGBackup.status.process.status` to `Completed` and the backup will be available to be restored (see [Restoring from a Backup](#restoring-from-a-backup) section). If the SGBackup fails the field `SGBackup.status.process.status` will be set to `Failed` and the field `SGBackup.status.process.failure` will contain the failure message. The Job of a failed scheduled SGBackup is maintained (only for the latest 10 Jobs) in order for the user to inspect its content.

### Creating a Manual Backup

A manual backup has to reference the cluster and to specify whether it will have a managed lifecycle (i.e. it will be removed on rotation by the specified retention):

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  name: cluster-2024-11-16
spec:
  sgCluster: cluster # name of the referenced SGCluster
  managedLifecycle: false # <true|false>
```

When a SGBackup is created manually the operator will generate a Job that will perform the backup. When the SGBackup completes successfully it will set the field `SGBackup.status.process.status` to `Completed` and the backup will be available to be restored (see [Restoring from a Backup](#restoring-from-a-backup) section). If the SGBackup fails the field `SGBackup.status.process.status` will be set to `Failed` and the field `SGBackup.status.process.failure` will contain the failure message. The Job of a failed manually created SGBackup is not removed in order for the user to inspect its content.

### Copying an Existing Backup to Another Namespace

A backup is only accessible from the namespace in which it is located.
In order to use it in another namespace, you need to copy it by modifying the resource. In particular, apart from the obvious part of having to change the namespace, you will have to prepend the referenced cluster name with the source namespace and a dot (`.`).

The following shows how to copy an SGBackup from the `source` namespace to the `target` namespace using `kubectl` and [`jq`](https://stedolan.github.io/jq/):

```
kubectl get sgbackup -n source source -o json \
  | jq '.spec.sgCluster = .metadata.namespace + "." + .spec.sgCluster | .metadata.namespace = "target"' \
  | kubectl create -f -
```

The backup associated to the SGBackup created in this way will not be deleted by the reconciliation until all the copies and the original SGBackup have been removed.

## Restoring from a Backup

StackGres can restore a database from a StackGres backup by specifying the SGBackup resource name of the desired backup in the [restore section of the SGCluster]({{% relref "06-crd-reference/01-sgcluster/#restore" %}}).
Like this:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
spec:
  initialData:
    restore:
      fromBackup:
        name: # the backup name to restore
```

An SGBackup can be restored only on SGCluster creation and such section can not be modified.
Check the complete explanation about how to restore a backup in the [Restore a Backup Runbook]({{% relref "09-runbooks/03-restore-backup" %}}).