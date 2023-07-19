---
title: SGBackupConfig
weight: 5
url: /reference/crd/sgbackupconfig
description: Details about SGBackupConfig
showToc: true
---

___

**Kind:** SGBackupConfig

**listKind:** SGBackupConfigList

**plural:** sgbackupconfigs

**singular:** sgbackupconfig

**shortNames** sgbac
___

> **WARNING**: This CRD has been deprecated and is replaced by the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD that have to be
>  specified by the new section `.spec.configurations.backups` in the [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) CRD.

Backup configuration allows to specify when and how backups are performed.
By default, this is done at 5am UTC in a window of 1 hour.
You may change this value in order to perform backups for another time zone and period of time.
The `SGBackupConfig` custom resource represents the backup configuration of a Postgres cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGBackupConfig
metadata:
  name: backupconf
spec:
  baseBackups:
    retention: 5
    cronSchedule: 0 5 * * *
    compression: lz4
    performance:
      maxDiskBandwitdh: 26214400 #25 MB per seceod
      maxNetworkBandwitdh: 52428800 #50 MB per second
      uploadDiskConcurrency: 2
  storage:
    type: s3Compatible
    s3Compatible:
      bucket: stackgres
      region: k8s
      enablePathStyleAddressing: true
      endpoint: http://my-cluster-minio:9000
      awsCredentials:
        secretKeySelectors:
          accessKeyId:
            key: accesskey
            name: my-cluster-minio
          secretAccessKey:
            key: secretkey
            name: my-cluster-minio
```

{{% include "generated/SGBackupConfig.md" %}}
