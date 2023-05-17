---
title: Backups
weight: 5
url: administration/backups
aliases: [ /install/prerequisites/backups , /tutorial/complete-cluster/backup-configuration ]
description: Details about how to set up and configure backups.
showToc: true
---

StackGres supports automated backups, based on Postgres [continuous archiving](https://www.postgresql.org/docs/current/continuous-archiving.html), that is base backups plus WAL (write ahead log) archiving, as well as backup lifecycle management.
To achieve maximum durability, backups are stored on cloud/object storage.
S3, GCP, Azure Blob, and S3-compatible object storages are supported.

## Cluster Backup Configuration

All the configuration for this matter can be found at the [SGCluster backups section]({{% relref "06-crd-reference/01-sgcluster/#backups" %}}).
When backups are configured, Postgres WAL files will start being archived in the specified storage at the specified path.
Also, automatic backups will be scheduled and a retention policy of backups is created.
By default, automatic backups will be scheduled daily at `05:00 UTC`, with a retention policy of 5 backups.
You will have to find out a time window and retention policy that fit your needs.
When configuring cluster backups, you may also specify the compression algorithm and performance-related options, such as the maximum disk and network throughput, or the parallelism for uploading files.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
# [...]
spec:
  # [...]
  configurations:
    backups:
    - sgObjectStorage: # name of the referenced SGObjectStorage
      path: # may be customized by the user or left with
            # a default value based on cluster namespace,
            # name and postgres version
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

StackGres support backups with the following storage options:

* AWS S3
* Google CLoud Storage
* Azure Blob Storage
* S3-Compatible Storages:
  * DigitalOcean Spaces
  * Self-hosted MinIO

> The examples are using the [MinIO](https://min.io/) service as a S3 compatible service for a quick setup on local Kubernetes clusters.
> Although StackGres definitely recommends to choose a Storage-as-a-Service for production setups.

All the storage-related configuration is defined in the [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRD.

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
# [...]
spec:
  # fill the preferred storage method with
  # specific credentials and configurations
  type: # <s3|s3Compatible|gcs|azureBlob>
  storage:
    s3: {}
    s3Compatible: {}
    gcs: {}
    azureBlob: {}
```

## Backups

Backups are materialized using [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}).
An SGBackup can be created automatically by the scheduled backup process, manually, or by copying an existing SGBackup in order to make it accessible in another namespace.
Removing an SGBackup also triggers the removal of the actual backup associated with it, that is the files on the object storage that represent the backup (if they are accessible by the backup configuration used by the SGCluster).

### Creating a Manual Backup

A manual backup has to reference the cluster and to specify whether it will have a managed lifecycle (i.e. it will be removed on rotation by the specified retention):

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
# [...]
spec:
  sgCluster: # name of the referenced SGCluster
  managedLifecycle: # <true|false>
```

### Copying an Existing Backup to Another Namespace

A backup is only accessible from the namespace in which it is located.
In order to use it in another namespace, you need to copy it by modifying the resource content.
In particular, apart from the obvious part of having to change the namespace, you will have to prepend the referenced cluster name with the source namespace and a dot (`.`).

The following is shows how to copy an SGBackup from the `source` namespace to the `target` namespace using `kubectl` and [`jq`](https://stedolan.github.io/jq/):

```
kubectl get sgbackup -n source source -o json \
  | jq '.spec.sgCluster = .metadata.namespace + "." + .spec.sgCluster | .metadata.namespace = "target"' \
  | kubectl create -f -
```

A backup created in this way will not be deleted until the copying action has finished and the original SGBackup has been removed.

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

Check the complete explanation about restoring a backup in the [Restore a Backup Runbook]({{% relref "09-runbooks/03-restore-backup" %}}).