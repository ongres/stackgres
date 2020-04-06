---
title: Backups
weight: 4
---

# Creating a backup

The backup CR represent a backup of the cluster. Backups are created automatically by the CronJob
 generated using the settings in [backup configuration](#configuration) or manually by creating a
 backup CR.

___

**Kind:** SGBackup

**listKind:** SGBackupList

**plural:** sgbackups

**singular:** sgbackup
___

**Spec**

| Property                 | Required | Updatable | Type     | Default | Description |
|:-------------------------|----------|-----------|:---------|:--------|:------------|
| sgCluster                | ✓        |           | string   |         | {{< crd-field-description SGBackup.spec.sgCluster >}} |
| subjectToRetentionPolicy |          | ✓         | booolean | false   | {{< crd-field-description SGBackup.spec.subjectToRetentionPolicy >}} |

**Status**

| Property                                   | Type    | Description |
|:-------------------------------------------|:--------|:------------|
| internalName                               | string  | {{< crd-field-description SGBackup.status.internalName >}} |
| [process](#backup-process)                 | object  | {{< crd-field-description SGBackup.status.process >}} |
| [backupInformation](#backup-information)   | object  | {{< crd-field-description SGBackup.status.backupInformation >}} |
| [sgBackupConfig](#configuration)           | object  | {{< crd-field-description SGBackup.status.sgBackupConfig >}} |

## Backup Process

| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| status                           | string  | {{< crd-field-description SGBackup.status.process.status >}} |
| jobPod                           | string  | {{< crd-field-description SGBackup.status.process.jobPod >}} |
| failure                          | string  | {{< crd-field-description SGBackup.status.process.failure >}} |
| subjectToRetentionPolicy         | boolean | {{< crd-field-description SGBackup.status.process.subjectToRetentionPolicy >}} |
| [timing](#backup-timing)         | object  | {{< crd-field-description SGBackup.status.process.timing >}} |

### Backup Timing
| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| start                            | string  | {{< crd-field-description SGBackup.status.process.timing.start >}} |
| end                              | string  | {{< crd-field-description SGBackup.status.process.timing.end >}} |
| stored                           | string  | {{< crd-field-description SGBackup.status.process.timing.stored >}} |

## Backup Intormation
| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| hostname                         | string  | {{< crd-field-description SGBackup.status.backupInformation.hostname >}} |
| systemIdentifier                 | string  | {{< crd-field-description SGBackup.status.backupInformation.systemIdentifier >}} |
| postgresVersion                  | string  | {{< crd-field-description SGBackup.status.backupInformation.postgresVersion >}} |
| pgData                           | string  | {{< crd-field-description SGBackup.status.backupInformation.pgData >}} |
| [size](#backup-size)             | object  | {{< crd-field-description SGBackup.status.backupInformation.size >}} |
| [lsn](#backup-lsn)               | object  | {{< crd-field-description SGBackup.status.backupInformation.lsn >}} |
| startWalFile                     | string  | {{< crd-field-description SGBackup.status.backupInformation.startWalFile >}} |
| controlData                      | object  | {{< crd-field-description SGBackup.status.backupInformation.controlData >}} |

### Backup Size

| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| compressed                       | integer | {{< crd-field-description SGBackup.status.backupInformation.size.compressed >}} |
| uncompressed                     | integer | {{< crd-field-description SGBackup.status.backupInformation.size.uncompressed >}} |

### Backup LSN

| Property                      | Type    | Description |
|:------------------------------|:--------|:------------|
| start                         | string  | {{< crd-field-description SGBackup.status.backupInformation.lsn.start >}} |
| end                           | string  | {{< crd-field-description SGBackup.status.backupInformation.lsn.end >}} |

Example:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGBackup
metadata:
  name: backup
spec:
  sgCluster: stackgres
  subjectToRetentionPolicy: true
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
    subjectToRetentionPolicy: true
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

# Configuration

Backup configuration allow to specify when and how backups are performed. By default this is done
 at 5am UTC in a window of 1 hour, you may change this value in order to perform backups for
 another time zone and period of time.
The backup configuration CR represent the backups configuration of the cluster.

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
apiVersion: stackgres.io/v1beta1
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
 
Default settings are stored in the same namespaces of the stackgres operator,
 with the name `defaultbackupconfig`

Given a stackgres operator installed in the `stackgres` namespace we can see the backup default values with de command:

``` sh
kubectl get sgbackupconfig -n stackgres defaultbackupconfig -o yaml
```

If a backup configuration is not specified in the cluster settings, a new one will be created with the default values. 

The default name of backup configuration CR is `defaultbackupconfig`

# Base Backups

| Property                                 | Required | Updatable |Type     | Default   | Description |
|:-----------------------------------------|----------|-----------|:--------|:----------|:------------|
| retention                                |          | ✓         | integer | 5         | {{< crd-field-description SGBackupConfig.spec.baseBackups.retention >}} |
| cronSchedule                             |          | ✓         | string  | 05:00 UTC | {{< crd-field-description SGBackupConfig.spec.baseBackups.cronSchedule >}} |
| compression                              |          | ✓         | string  | lz4       | {{< crd-field-description SGBackupConfig.spec.baseBackups.compression >}} |
| [performance](#base-backup-performance)  |          | ✓         | object  |           | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance >}} |

# Base Backup Performance

| Property                               | Required | Updatable |Type     | Default   | Description |
|:---------------------------------------|----------|-----------|:--------|:----------|:------------|
| maxDiskBandwitdh                       |          | ✓         | integer | unlimited | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.maxDiskBandwitdh >}} |
| maxNetworkBandwitdh                    |          | ✓         | integer | unlimited | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.maxNetworkBandwitdh >}} |
| uploadDiskConcurrency                  |          | ✓         | integer | 1         | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.uploadDiskConcurrency >}} |

# Storage Configuration

| Property                                                             | Required               | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------|------------------------|-----------|:-------|:--------|:------------|
| type                                                                 | ✓                      | ✓         | string |         | {{< crd-field-description SGBackupConfig.spec.storage.type >}} |
| [s3](#s3--amazon-web-services-s3-configuration)                      | if type = s3           | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3 >}} |
| [s3Compatible](#s3--amazon-web-services-s3-compatible-configuration) | if type = s3Compatible | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible >}} |
| [gcs](#gsc--google-cloud-storage-configuration)                      | if type = gcs          | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs >}} |
| [azureBlob](#azure--azure-blob-storage-configuration)                | if type = azureblob    | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob >}} |

## S3

### S3 - Amazon Web Services S3 configuration

| Property                                           | Required | Updatable | Type    | Default | Description |
|:---------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                                             | ✓        | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.bucket >}} |
| path                                               |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.path >}} |
| [awsCredentials](#amazon-web-services-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.awsCredentials >}} |
| region                                             |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.region >}} |
| storageClass                                       |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3.storageClass >}} |

### S3 - Amazon Web Services S3 Compatible configuration

| Property                                           | Required | Updatable | Type    | Default | Description |
|:---------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                                             | ✓        | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.bucket >}} |
| path                                               |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.path >}} |
| [awsCredentials](#amazon-web-services-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials >}} |
| region                                             |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.region >}} |
| storageClass                                       |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.storageClass >}} |
| endpoint                                           |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.endpoint >}} |
| enablePathStyleAddressing                          |          | ✓         | boolean |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.enablePathStyleAddressing >}} |

### Amazon Web Services Credentials

| Property                                                       | Required | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#amazon-web-services-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors >}} |

#### Amazon Web Services Secret Key Selector

| Property                                                                                                          | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [accessKeyId](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core)     | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId >}} |
| [secretAccessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey >}} |

## GSC - Google Cloud Storage configuration

| Property                           | Required | Updatable | Type   | Default | Description |
|:-----------------------------------|----------|-----------|:-------|:--------|:------------|
| bucket                             | ✓        | ✓         | string |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.bucket >}} |
| path                               |          | ✓         | string |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.path >}} |
| [gcpCredentials](#gcp-credentials) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials >}} |

### GCP Credentials

| Property                                       | Required | Updatable | Type   | Default | Description |
|:-----------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#gcp-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials.secretKeySelectors >}} |

#### GCP Secret Key Selector

| Property                                                                                                             | Required | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------------------------------------------------------|----------|:----------|:-------|:--------|:------------|
| [serviceAccountJSON](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON >}} |


## AZURE - Azure Blob Storage configuration

| Property                               | Required | Updatable | Type    | Default | Description |
|:---------------------------------------|----------|-----------|:--------|:--------|:-------------|
| bucket                                 | ✓        | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.bucket >}} |
| path                                   |          | ✓         | string  |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.path >}} |
| [azureCredentials](#azure-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials >}} |

### Azure Credentials

| Property                                         | Required | Updatable | Type   | Default | Description |
|:-------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#azure-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors >}} |

### Azure Secret Key Selector

| Property                                                                                                           | Required | Updatable | Type   | Default | Description |
|:-------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:-------------|
| [storageAccount](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core)   | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount >}} |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core)        | ✓        | ✓         | object |         | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey >}} |
