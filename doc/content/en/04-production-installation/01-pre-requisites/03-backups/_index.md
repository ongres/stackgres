---
title: Backups
weight: 3
url: install/prerequisites/backups
description: Details about how to setup and configure the backups. 
showToc: true
---

In the next section, you'll be able to see how to manage backups and related operations with more explicit examples.

## Cluster Configuration

All the configuration for this matter can be found at the [backups section of SGCluster]({{% relref "06-crd-reference/01-sgcluster/#backups" %}}).
 When backups are configured Postgres WAL files will start to be archived in the specified storage at the specified
 path. Also, automatic backups will be scheduled and a retention policy of backups whom lifecycle is managed will be
 removed on rotation after a new backup is created. By default, automatic backups will be scheduled daily at
 `05:00 UTC` and with a retention policy of 5 backups. You will have to find out the correct time window and retention
 policy that fit your needs. When configuring cluster's backups you may also specify the compression algorithm to use
 and some performence options like the maximum throughput for disk and network or the parallelism for uploading files.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
spec:
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

## Storage

StackGres support Backups with the following storage options:

* AWS S3
* Google CLoud Storage
* Azure Blob Storage
* S3-Compatible Storages:
  * DigitalOcean Spaces
  * Self-hosted MinIO

> Examples are using [MinIO](https://min.io/) service as a S3 compatible service for
> quick setups on local Kubernetes Cluster. Although, for production setups, StackGres Team recommends
> emphatically to pick a Storage as a Service for this purpose.

All the related configuration for the storage, is in your [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}).

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
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

Backups are materialized using [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}). An SGBackup can be created
 automatically, by the scheduled backup process, manually, to trigger the creation of a backup at any atime, or by
 copying an existing SGBackup to make it accesible into another namespace. Removing an SGBackup also trigger the removal
 of the real backup associated with it (if it is accesible by the backup configuration used by the SGCluster).

### Create a manual backup

A manual backup have to reference the cluster and specify if it will have a managed lifecycle (i.e. it will be removed
 on rotation by the specified retention):

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
spec:
  sgCluster: # name of the referenced SGCluster
  managedLifecycle: # <true|false>
```

### Copy an existing backup to make it accesible into another namespace

A backup is only accesible from the namespace where it belongs. In order to make it accesible to another namespace you
 may copy it by modifying its content slightly. In particular, apart from the obvious part of having to change the
 namespace, you will have to prepend the referenced cluster name with the source namespace and a dot (`.`). Above is
 an example on how to copy an SGBackup from `source` namespace to `target` namespace using the commands `kubectl`
 and [`jq`](https://stedolan.github.io/jq/):

```shell
kubectl get sgbackup -n source source -o json \
  | jq '.spec.sgCluster = .metadata.namespace + "." + .spec.sgCluster | .metadata.namespace = "target"' \
  | kubectl create -f -
```

A backup created in this way will prevent its deletion until all the copy and the original SGBackup have been removed.

## Restore

StackGres can perform a database restoration from a StackGres backup by just setting the UID of
 the backup CR that represents the backup that we want to restore in the
 [restore section of the SGCluster]({{% relref "06-crd-reference/01-sgcluster/#restore" %}}). Like this:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
spec:
  initialData:
    restore:
      fromBackup:
        name: # the backup name to restore
```

