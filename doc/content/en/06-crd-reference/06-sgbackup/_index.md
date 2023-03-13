---
title: SGBackup
weight: 7
url: reference/crd/sgbackup
description: Details about SGBackup
showToc: true
---

The `SGBackup` custom resource represents a backup of the Postgres cluster.
Backups are created automatically by a cron job configured using the settings in the [backup configuration](#configuration) or manually by creating a backup custom resource.

___

**Kind:** SGBackup

**listKind:** SGBackupList

**plural:** sgbackups

**singular:** sgbackup
___

**Spec**

| <div style="width:10rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div> | Default | Description |
|:----------------------------------------|----------|-----------|:-----------------------------------|:--------|:------------|
| sgCluster                               | ✓        |           | string                             |         | {{< crd-field-description SGBackup.spec.sgCluster >}} |
| managedLifecycle                        |          | ✓         | booolean                           | false   | {{< crd-field-description SGBackup.spec.managedLifecycle >}} |


Example:

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

**Status**

| Property                                   | Type    | Description |
|:-------------------------------------------|:--------|:------------|
| internalName                               | string  | {{< crd-field-description SGBackup.status.internalName >}} |
| [process](#backup-process)                 | object  | {{< crd-field-description SGBackup.status.process >}} |
| [backupInformation](#backup-information)   | object  | {{< crd-field-description SGBackup.status.backupInformation >}} |
| [sgBackupConfig](#backup-configuration)           | object  | {{< crd-field-description SGBackup.status.sgBackupConfig >}} |

### Backup Process

| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| status                           | string  | {{< crd-field-description SGBackup.status.process.status >}} |
| jobPod                           | string  | {{< crd-field-description SGBackup.status.process.jobPod >}} |
| failure                          | string  | {{< crd-field-description SGBackup.status.process.failure >}} |
| managedLifecycle         | boolean | {{< crd-field-description SGBackup.status.process.managedLifecycle >}} |
| [timing](#backup-timing)         | object  | {{< crd-field-description SGBackup.status.process.timing >}} |

#### Backup Timing
| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| start                            | string  | {{< crd-field-description SGBackup.status.process.timing.start >}} |
| end                              | string  | {{< crd-field-description SGBackup.status.process.timing.end >}} |
| stored                           | string  | {{< crd-field-description SGBackup.status.process.timing.stored >}} |

### Backup Information
| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| hostname (deprecated)            | string  | {{< crd-field-description SGBackup.status.backupInformation.hostname >}} |
| sourcePod                        | string  | {{< crd-field-description SGBackup.status.backupInformation.sourcePod >}} |
| systemIdentifier                 | string  | {{< crd-field-description SGBackup.status.backupInformation.systemIdentifier >}} |
| postgresVersion                  | string  | {{< crd-field-description SGBackup.status.backupInformation.postgresVersion >}} |
| pgData                           | string  | {{< crd-field-description SGBackup.status.backupInformation.pgData >}} |
| [size](#backup-size)             | object  | {{< crd-field-description SGBackup.status.backupInformation.size >}} |
| [lsn](#backup-lsn)               | object  | {{< crd-field-description SGBackup.status.backupInformation.lsn >}} |
| startWalFile                     | string  | {{< crd-field-description SGBackup.status.backupInformation.startWalFile >}} |
| controlData                      | object  | {{< crd-field-description SGBackup.status.backupInformation.controlData >}} |

#### Backup Size

| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| compressed                       | integer | {{< crd-field-description SGBackup.status.backupInformation.size.compressed >}} |
| uncompressed                     | integer | {{< crd-field-description SGBackup.status.backupInformation.size.uncompressed >}} |

#### Backup LSN

| Property                      | Type    | Description |
|:------------------------------|:--------|:------------|
| start                         | string  | {{< crd-field-description SGBackup.status.backupInformation.lsn.start >}} |
| end                           | string  | {{< crd-field-description SGBackup.status.backupInformation.lsn.end >}} |

## Backup Configuration

| <div style="width:8rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default   | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:----------|:------------|
| compression                            |          | ✓         | string                             | lz4       | {{< crd-field-description SGBackup.status.sgBackupConfig.compression >}} |
| [storage](#storage-configuration)      |          | ✓         | object                             |           | {{< crd-field-description SGBackup.status.sgBackupConfig.storage >}} |

## Storage Configuration

| Property                                                             | Required               | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------|------------------------|-----------|:-------|:--------|:------------|
| type                                                                 | ✓                      | ✓         | string |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.type >}} |
| [s3](#s3---amazon-web-services-s3-configuration)                      | if type = s3           | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3 >}} |
| [s3Compatible](#s3---amazon-web-services-s3-compatible-configuration) | if type = s3Compatible | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible >}} |
| [gcs](#gsc---google-cloud-storage-configuration)                      | if type = gcs          | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.gcs >}} |
| [azureBlob](#azure---azure-blob-storage-configuration)                | if type = azureblob    | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.azureBlob >}} |

## S3

### S3 - Amazon Web Services S3 configuration

| <div style="width:8rem">Property</div>             | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                                             | ✓        | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3.bucket >}} |
| path                                               |          | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3.path >}} |
| [awsCredentials](#amazon-web-services-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3.awsCredentials >}} |
| region                                             |          | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3.region >}} |
| storageClass                                       |          | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3.storageClass >}} |

### S3 - Amazon Web Services S3 Compatible configuration

| <div style="width:14rem">Property</div>            | Required | Updatable | <div style="width:5rem">Type</div> | Default | Description |
|:---------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                                             | ✓        | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.bucket >}} |
| path                                               |          | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.path >}} |
| [awsCredentials](#amazon-web-services-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.awsCredentials >}} |
| region                                             |          | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.region >}} |
| storageClass                                       |          | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.storageClass >}} |
| endpoint                                           |          | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.endpoint >}} |
| enablePathStyleAddressing                          |          | ✓         | boolean |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.enablePathStyleAddressing >}} |

### Amazon Web Services Credentials

| Property                                                       | Required | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#amazon-web-services-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.awsCredentials.secretKeySelectors >}} |

#### Amazon Web Services Secret Key Selector

| Property                                                                                                          | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [accessKeyId](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)     | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId >}} |
| [secretAccessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey >}} |

## GSC - Google Cloud Storage configuration

| <div style="width:8rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------|----------|-----------|:-------|:--------|:------------|
| bucket                                 | ✓        | ✓         | string |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.gcs.bucket >}} |
| path                                   |          | ✓         | string |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.gcs.path >}} |
| [gcpCredentials](#gcp-credentials)     | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.gcs.gcpCredentials >}} |

### GCP Credentials

| Property                                       | Required | Updatable | Type   | Default | Description |
|:-----------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#gcp-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.gcs.gcpCredentials.secretKeySelectors >}} |

#### GCP Secret Key Selector

| Property                                                                                                             | Required | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------------------------------------------------------|----------|:----------|:-------|:--------|:------------|
| [serviceAccountJSON](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON >}} |


## AZURE - Azure Blob Storage configuration

| <div style="width:9rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------|----------|-----------|:--------|:--------|:-------------|
| bucket                                 | ✓        | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.azureBlob.bucket >}} |
| path                                   |          | ✓         | string  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.azureBlob.path >}} |
| [azureCredentials](#azure-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.azureBlob.azureCredentials >}} |

### Azure Credentials

| Property                                         | Required | Updatable | Type   | Default | Description |
|:-------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#azure-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.azureBlob.azureCredentials.secretKeySelectors >}} |

### Azure Secret Key Selector

| Property                                                                                                           | Required | Updatable | Type   | Default | Description |
|:-------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:-------------|
| [storageAccount](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)   | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount >}} |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)        | ✓        | ✓         | object |         | {{< crd-field-description SGBackup.status.sgBackupConfig.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey >}} |
