---
title: SGBackupConfig
weight: 5
url: /reference/crd/sgbackupconfig
description: Details about SGBackupConfig
showToc: true
---

**WARNING**: This CRD has been deprecated and is replaced by the [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRD, specified by the new section `.spec.configurations.backups` in the [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) CRD.

## Configuration

Backup configuration allows to specify when and how backups are performed.
By default, this is done at 5am UTC in a window of 1 hour.
You may change this value in order to perform backups for another time zone and period of time.
The `SGBackupConfig` custom resource represents the backup configuration of a Postgres cluster.

___

**Kind:** SGBackupConfig

**listKind:** SGBackupConfigList

**plural:** sgbackupconfigs

**singular:** sgbackupconfig
___

**Spec**


| Property                               | Required | Updatable |Type     | Default   | Description |
|:---------------------------------------|----------|-----------|:--------|:----------|:------------|
| [baseBackups](#base-backups)           |          | ✓         | object  |           | {{< crd-field-description SGBackupConfig.spec.baseBackups >}} |
| [storage](#storage-configuration)      |          | ✓         | object  |           | {{< crd-field-description SGBackupConfig.spec.storage >}} |

Example:

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

## Base Backups

| <div style="width:7rem">Property</div>   | Required | Updatable |<div style="width:5rem">Type</div> | <div style="width:5rem">Default</div> | Description |
|:-----------------------------------------|----------|-----------|:----------------------------------|:--------------------------------------|:------------|
| retention                                |          | ✓         | integer                           | 5                                     | {{< crd-field-description SGBackupConfig.spec.baseBackups.retention >}} |
| cronSchedule                             |          | ✓         | string                            | 05:00 UTC                             | {{< crd-field-description SGBackupConfig.spec.baseBackups.cronSchedule >}} |
| compression                              |          | ✓         | string                            | lz4                                   | {{< crd-field-description SGBackupConfig.spec.baseBackups.compression >}} |
| [performance](#base-backup-performance)  |          | ✓         | object                            |                                       | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance >}} |

## Base Backup Performance

| <div style="width:12rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div> | <div style="width:5rem">Default</div> | Description |
|:----------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:------------|
| maxDiskBandwitdh                        |          | ✓         | integer                            | unlimited                             | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.maxDiskBandwitdh >}} |
| maxNetworkBandwitdh                     |          | ✓         | integer                            | unlimited                             | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.maxNetworkBandwitdh >}} |
| uploadDiskConcurrency                   |          | ✓         | integer                            | 1                                     | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.uploadDiskConcurrency >}} |

## Storage Configuration

| Property                                                             | Required               | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------|------------------------|-----------|:-------|:--------|:------------|
| type                                                                 | ✓                      | ✓         | string |         | {{< crd-field-description SGBackupConfig.spec.storage.type >}} |
| [s3](#s3---amazon-web-services-s3-configuration)                      | if type = s3           | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3 >}} |
| [s3Compatible](#s3---amazon-web-services-s3-configuration) | if type = s3Compatible | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible >}} |
| [gcs](#gsc---google-cloud-storage-configuration)                      | if type = gcs          | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs >}} |
| [azureBlob](#azure---azure-blob-storage-configuration)                | if type = azureblob    | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob >}} |

## S3

### S3 - Amazon Web Services S3 configuration

| <div style="width:8rem">Property</div>             | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                                             | ✓        | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.bucket >}} |
| path                                               |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.path >}} |
| [awsCredentials](#amazon-web-services-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.awsCredentials >}} |
| region                                             |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.region >}} |
| storageClass                                       |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.storageClass >}} |

### S3 - Amazon Web Services S3 Compatible configuration

| <div style="width:14rem">Property</div>            | Required | Updatable | <div style="width:5rem">Type</div> | Default | Description |
|:---------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                                             | ✓        | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.bucket >}} |
| path                                               |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.path >}} |
| [awsCredentials](#amazon-web-services-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials >}} |
| region                                             |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.region >}} |
| storageClass                                       |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.storageClass >}} |
| endpoint                                           |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.endpoint >}} |
| enablePathStyleAddressing                          |          | ✓         | boolean |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.enablePathStyleAddressing >}} |

### Amazon Web Services Credentials

| <div style="width:10rem">Property</div>                        | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#amazon-web-services-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors >}} |

#### Amazon Web Services Secret Key Selector

| Property                                                                                                          | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [accessKeyId](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)     | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId >}} |
| [secretAccessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey >}} |

## GSC - Google Cloud Storage configuration

| <div style="width:8rem">Property</div>| Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:--------------------------------------|----------|-----------|:-------|:--------|:------------|
| bucket                                | ✓        | ✓         | string |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.bucket >}} |
| path                                  |          | ✓         | string |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.path >}} |
| [gcpCredentials](#gcp-credentials)    | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials >}} |

### GCP Credentials

| Property                                       | Required | Updatable | Type   | Default | Description |
|:-----------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#gcp-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials.secretKeySelectors >}} |

#### GCP Secret Key Selector

| Property                                                                                                             | Required | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------------------------------------------------------|----------|:----------|:-------|:--------|:------------|
| [serviceAccountJSON](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON >}} |


## AZURE - Azure Blob Storage configuration

| <div style="width:9rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------|----------|-----------|:--------|:--------|:-------------|
| bucket                                 | ✓        | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.bucket >}} |
| path                                   |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.path >}} |
| [azureCredentials](#azure-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials >}} |

### Azure Credentials

| <div style="width:10rem">Property</div>          | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:-------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#azure-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors >}} |

### Azure Secret Key Selector

| Property                                                                                                           | Required | Updatable | Type   | Default | Description |
|:-------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:-------------|
| [storageAccount](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)   | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount >}} |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)        | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey >}} |
