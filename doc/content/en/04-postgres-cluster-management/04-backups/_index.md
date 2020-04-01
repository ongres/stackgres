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
| sgCluster                | ✓        |           | string   |         | The name of the cluster where the backup will or has been taken. |
| subjectToRetentionPolicy |          | ✓         | booolean | false   | Indicate if this backup is permanent and should not be removed by retention process. |

**Status**

| Property                                   | Type    | Description |
|:-------------------------------------------|:--------|:------------|
| internalName                               | string  | The name of the backup |
| [process](#backup-process)                 | object  | backup lifecycle information |
| [backupInformation](#backup-information)   | object  | General backup information |
| [sgBackupConfig](#configuration)           | object  | The backup configuration to restore this backup. |

## Backup Process

| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| status                           | string  | The phase of the backup (Pending, Created, Failed). |
| jobPod                           | string  | The name of pod assigned to this backup. |
| failure                          | string  | If the phase is failed this field will contain a message with the failure reason. |
| subjectToRetentionPolicy         | boolean | Indicate if this backup is permanent and should not be removed by retention process. |
| [timing](#backup-timing)         | object  | Backup timing information |

### Backup Timing
| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| start                            | string  | The start time of backup. |
| end                              | string  | The finish time of backup. |
| stored                           | string  | The date of the backup. | 

## Backup Intormation
| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| hostname                         | string  | The hostname of instance where the backup is taken. |
| systemIdentifier                 | string  | The internal system identifier of this backup. |
| postgresVersion                  | string  | The PostgreSQL version of the server where backup is taken. |
| pgData                           | string  | The data directory where the backup is taken. |
| [size](#backup-size)             | object  | backup size information |
| [lsn](#backup-lsn)               | object  | The LSN backup information |
| startWalFile                     | string  | WAL file name when backup was started. |
| controlData                      | object  | An object containing data from the output of pg_controldata on the backup. |

### Backup LSN
| Property                      | Type    | Description |
|:------------------------------|:--------|:------------|
| start                         | string  | The LSN of when backup started. |
| finish                        | string  | The LSN of when backup finished. |

### Backup Size
| Property                         | Type    | Description |
|:---------------------------------|:--------|:------------|
| compressed                       | integer | The start time of backup. |
| uncompressed                     | integer | The finish time of backup. |



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
      s3compatible:
        awsCredentials:
          secretKeySelectors:
            accessKeyId:
              key: accesskey
              name: minio
            accessKeyId:
              key: secretkey
              name: minio
        endpoint: http://minio:9000
        forcePathStyle: true
        bucket: stackgres
        region: k8s
      type: s3compatible
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
      finish: "234881272"
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
| [baseBackups](#base-backups     )      |          | ✓         | object  |           | Base backup configuration     |
| [storage](#storage-configuration)      |          | ✓         | object  |           | Backup storage configuration  |

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
    type: s3compatible
    s3compatible:
      credentials:
        accessKey:
          key: accesskey
          name: my-cluster-minio
        secretKey:
          key: secretkey
          name: my-cluster-minio
      endpoint: http://my-cluster-minio:9000
      forcePathStyle: true
      bucket: stackgres
      region: k8s
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
| retention                                |          | ✓         | integer | 5         | Retains specified number of full backups. Default is 5 |
| cronSchedule                             |          | ✓         | string  | 05:00 UTC | Specify when to perform full backups using cron syntax:<br><minute: 0 to 59, or *> <hour: 0 to 23, or * for any value. All times UTC> <day of the month: 1 to 31, or *> <month: 1 to 12, or *> <day of the week: 0 to 7 (0 and 7 both represent Sunday), or *>. <br>If not specified full backups will be performed each day at 05:00 UTC  |
| compressionMethod                        |          | ✓         | string  | lz4       | To configure compression method used for backups. Possible options are: lz4, lzma, brotli. Default method is lz4. LZ4 is the fastest method, but compression ratio is bad. LZMA is way much slower, however it compresses backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio which is about 3 times better than LZ4  |
| [performance](#base-backup-performance)  |          | ✓         | object  |           | To set limits on the resource consumtion of the backup process |


# Base Backup Performance
| Property                               | Required | Updatable |Type     | Default   | Description |
|:---------------------------------------|----------|-----------|:--------|:----------|:------------|
| maxDiskBandwitdh                       |          | ✓         | integer | unlimited | To configure disk read rate limit during uploads in bytes per second  |
| maxNetworkBandwitdh                    |          | ✓         | integer | unlimited | To configure network read rate limit during uploads in bytes per second  |
| uploadDiskConcurrency                  |          | ✓         | integer | 1         | To configure how many concurrency streams are reading disk during uploads. By default 1 stream  |


# Storage Configuration

| Property                                                             | Required               | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------|------------------------|-----------|:-------|:--------|:------------|
| type                                                                 | ✓                      | ✓         | string |         | Type of storage: <br>- s3: Amazon Web Services S3 <br>- s3compatible: Amazon Web Services S3 Compatible <br>- gcs: Google Clooud Storage <br>- azureblob: Azure Blob Storage  |
| [s3](#s3--amazon-web-services-s3-configuration)                      | if type = s3           | ✓         | object |         | Amazon Web Services S3 configuration |
| [s3compatible](#s3--amazon-web-services-s3-compatible-configuration) | if type = s3compatible | ✓         | object |         | Amazon Web Services S3 configuration |
| [gcs](#gsc--google-cloud-storage-configuration)                      | if type = gcs          | ✓         | object |         | Google Cloud Storage configuration |
| [azureBlob](#azure--azure-blob-storage-configuration)                | if type = azureblob    | ✓         | object |         | Google Cloud Storage configuration |

## S3 - Amazon Web Services S3 configuration

| Property                          | Required | Updatable | Type    | Default | Description |
|:----------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                            | ✓        | ✓         | string  |         | The AWS S3 bucket (eg. bucket) |
| path                              |          | ✓         | string  |         | The AWS S3 bucket path (eg. /path/to/folder) |
| [awsCredentials](#s3-credentials) | ✓        | ✓         | object  |         | The credentials to access AWS S3 for writing and reading  |
| region                            |          | ✓         | string  |         | The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this property  |
| storageClass                      |          | ✓         | string  |         | By default, the "STANDARD" storage class is used. Other supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced Redundancy  |

## S3 - Amazon Web Services S3 Compatible configuration

| Property                          | Required | Updatable | Type    | Default | Description |
|:----------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                            | ✓        | ✓         | string  |         | The AWS S3 bucket (eg. bucket) |
| path                              |          | ✓         | string  |         | The AWS S3 bucket path (eg. /path/to/folder) |
| [awsCredentials](#s3-credentials) | ✓        | ✓         | object  |         | The credentials to access AWS S3 for writing and reading  |
| region                            |          | ✓         | string  |         | The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this property  |
| endpoint                          |          | ✓         | string  |         | Overrides the default hostname to connect to an S3-compatible service. i.e, http://s3-like-service:9000  |
| forcePathStyle                    |          | ✓         | boolean |         | To enable path-style addressing(i.e., http://s3.amazonaws.com/BUCKET/KEY) when connecting to an S3-compatible service that lack of support for sub-domain style bucket URLs (i.e., http://BUCKET.s3.amazonaws.com/KEY). Defaults to false  |
| storageClass                      |          | ✓         | string  |         | By default, the "STANDARD" storage class is used. Other supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced Redundancy  |

### S3 Credentials

| Property                                      | Required | Updatable | Type   | Default | Description |
|:----------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#s3-secret-key-selector) | ✓        | ✓         | object |         | The S3 credential configuration by using secret keys selectors |

#### S3 Secret Key Selector

| Property                                                                                                          | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [accessKeyId](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core)     | ✓        | ✓         | object |         | AWS Access Key ID |
| [secretAccessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | AWS Secret Access Key  |

## GSC - Google Cloud Storage configuration

| Property                           | Required | Updatable | Type   | Default | Description |
|:-----------------------------------|----------|-----------|:-------|:--------|:------------|
| bucket                             | ✓        | ✓         | string |         | Specify bucket where to store backups (eg. x4m-test-bucket) |
| path                               |          | ✓         | string |         | Specify bueckt path where to store backups (eg. /walg-folder) |
| [gcpCredentials](#gcp-credentials) | ✓        | ✓         | object |         | The credentials to access GCS for writing and reading |

### GCP Credentials

| Property                                      | Required | Updatable | Type   | Default | Description |
|:----------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#gcp-secret-key-selector) | ✓        | ✓         | object |         | The GCP credential configuration using secret keys selectors |

#### GCP Secret Key Selector

| Property                                                                                                             | Required | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------------------------------------------------------|----------|:----------|:-------|:--------|:------------|
| [serviceAccountJSON](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | The key of the secret to select from. Must be a valid GCP account key |


## AZURE - Azure Blob Storage configuration

| Property                               | Required | Updatable | Type    | Default | Description |
|:---------------------------------------|----------|-----------|:--------|:--------|:-------------|
| bucket                                 | ✓        | ✓         | string  |         | Specify bucket where to store backups in Azure storage (eg. test-container) |  
| path                                   |          | ✓         | string  |         | Specify bucket path where to store backups in Azure storage (eg. /walg-folder) |  
| [azureCredentials](#azure-credentials) | ✓        | ✓         | object  |         | AWS Secret Access Key  |

### Azure Credentials

| Property                                         | Required | Updatable | Type   | Default | Description |
|:-------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#azure-secret-key-selector) | ✓        | ✓         | object |         | The Azure credential configuration using secret keys selectors |

### Azure Secret Key Selector

| Property                                                                                                           | Required | Updatable | Type   | Default | Description |
|:-------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:-------------|
| [storageAccount](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core)   | ✓        | ✓         | object |         | The name of the storage account |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core)        | ✓        | ✓         | object |         | The primary or secondary access key for the storage account. |
