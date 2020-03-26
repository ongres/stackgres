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

| Property    | Required | Updatable | Type     | Default | Description |
|:------------|----------|-----------|:---------|:--------|:------------|
| cluster     | ✓        |           | string   |         | The name of the cluster where the backup will or has been taken. |
| isPermanent |          | ✓         | booolean | false   | Indicate if this backup is permanent and should not be removed by retention process. |

**Status**

| Property                       | Type    | Description |
|:-------------------------------|:--------|:------------|
| phase                          | string  | The phase of the backup (Pending, Created, Failed). |
| pod                            | string  | The name of pod assigned to this backup. |
| failureReason                  | string  | If the phase is failed this field will contain a message with the failure reason. |
| [backupConfig](#configuration) | object  | The backup configuration to restore this backup. |
| name                           | string  | The name of the backup. |
| time                           | string  | The date of the backup. |
| walFileName                    | string  | The WAL file name when backup was started. |
| startTime                      | string  | The start time of backup. |
| finishTime                     | string  | The finish time of backup. |
| hostname                       | string  | The hostname of instance where the backup is taken. |
| dataDir                        | string  | The data directory where the backup is taken. |
| pgVersion                      | string  | The PostgreSQL version of the server where backup is taken. |
| startLsn                       | string  | The LSN of when backup started. |
| finishLsn                      | string  | The LSN of when backup finished. |
| isPermanent                    | boolean | Indicate internally if this backup is permanent and should not be removed by retention process. |
| systemIdentifier               | string  | The internal system identifier of this backup. |
| uncompressedSize               | integer | The size in bytes of the uncompressed backup. |
| compressedSize                 | integer | The size in bytes of the compressed backup. |
| controlData                    | object  | An object containing data from the output of pg_controldata on the backup. |
| tested                         | boolean | true if the backup has been tested. |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGBackup
metadata:
  name: backup
spec:
  cluster: stackgres
  isPermanent: true
status:
  backupConfig:
    compressionMethod: lz4
    storage:
      s3compatible:
        credentials:
          accessKey:
            key: accesskey
            name: minio
          secretKey:
            key: secretkey
            name: minio
        endpoint: http://minio:9000
        forcePathStyle: true
        bucket: stackgres
        region: k8s
      type: s3compatible
  compressedSize: 6691164
  dataDir: /var/lib/postgresql/data
  failureReason: ""
  finishLsn: "234881272"
  finishTime: "2020-01-22T10:17:27.165204Z"
  hostname: stackgres-1
  isPermanent: true
  name: base_00000002000000000000000E
  pgVersion: "110006"
  phase: Completed
  pod: backup-backup-q79zq
  startLsn: "234881064"
  startTime: "2020-01-22T10:17:24.983902Z"
  systemIdentifier: "6784708504968245298"
  time: "2020-01-22T10:17:27.183Z"
  uncompressedSize: 24037844
  walFileName: 00000002000000000000000E
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
| retention                              |          | ✓         | integer | 5         | Retains specified number of full backups. Default is 5 |
| fullSchedule                           |          | ✓         | string  | 05:00 UTC | Specify when to perform full backups using cron syntax:<br><minute: 0 to 59, or *> <hour: 0 to 23, or * for any value. All times UTC> <day of the month: 1 to 31, or *> <month: 1 to 12, or *> <day of the week: 0 to 7 (0 and 7 both represent Sunday), or *>. <br>If not specified full backups will be performed each day at 05:00 UTC  |
| fullWindow                             |          | ✓         | integer | 1 hour    | Specify the time window in minutes where a full backup will start happening after the point in time specified by fullSchedule. If for some reason the system is not capable to start the full backup it will be skipped. If not specified the window will be of 1 hour  |
| compressionMethod                      |          | ✓         | string  | lz4       | To configure compression method used for backups. Possible options are: lz4, lzma, brotli. Default method is lz4. LZ4 is the fastest method, but compression ratio is bad. LZMA is way much slower, however it compresses backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio which is about 3 times better than LZ4  |
| diskRateLimit                          |          | ✓         | integer | unlimited | To configure disk read rate limit during uploads in bytes per second  |
| networkRateLimit                       |          | ✓         | integer | unlimited | To configure network read rate limit during uploads in bytes per second  |
| uploadDiskConcurrency                  |          | ✓         | integer | 1         | To configure how many concurrency streams are reading disk during uploads. By default 1 stream  |
| tarSizeThreshold                       |          | ✓         | integer | 1 GB      | To configure the size of one backup bundle (in bytes). Smaller size causes granularity and more optimal, faster recovering. It also increases the number of storage requests, so it can costs you much money. Default size is 1 GB (1 << 30 - 1 bytes)  |
| [storage](#storage-configuration)      |          | ✓         | object  |           | Backup storage configuration  |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGBackupConfig
metadata:
  name: backupconf
spec:
  retention: 5
  fullSchedule: 0 5 * * *
  fullWindow: 60
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

# Storage Configuration

| Property                                                             | Required               | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------|------------------------|-----------|:-------|:--------|:------------|
| type                                                                 | ✓                      | ✓         | string |         | Type of storage: <br>- s3: Amazon Web Services S3 <br>- s3compatible: Amazon Web Services S3 Compatible <br>- gcs: Google Clooud Storage <br>- azureblob: Azure Blob Storage  |
| [s3](#s3--amazon-web-services-s3-configuration)                      | if type = s3           | ✓         | object |         | Amazon Web Services S3 configuration |
| [s3compatible](#s3--amazon-web-services-s3-compatible-configuration) | if type = s3compatible | ✓         | object |         | Amazon Web Services S3 configuration |
| [gcs](#gsc--google-cloud-storage-configuration)                      | if type = gcs          | ✓         | object |         | Google Cloud Storage configuration |
| [azureblob](#azure--azure-blob-storage-configuration)                | if type = azureblob    | ✓         | object |         | Google Cloud Storage configuration |

## S3 - Amazon Web Services S3 configuration

| Property                       | Required | Updatable | Type    | Default | Description |
|:-------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                         | ✓        | ✓         | string  |         | The AWS S3 bucket (eg. bucket) |
| path                           |          | ✓         | string  |         | The AWS S3 bucket path (eg. /path/to/folder) |
| [credentials](#s3-credentials) | ✓        | ✓         | object  |         | The credentials to access AWS S3 for writing and reading  |
| region                         |          | ✓         | string  |         | The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this property  |
| storageClass                   |          | ✓         | string  |         | By default, the "STANDARD" storage class is used. Other supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced Redundancy  |

## S3 - Amazon Web Services S3 Compatible configuration

| Property                       | Required | Updatable | Type    | Default | Description |
|:-------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                         | ✓        | ✓         | string  |         | The AWS S3 bucket (eg. bucket) |
| path                           |          | ✓         | string  |         | The AWS S3 bucket path (eg. /path/to/folder) |
| [credentials](#s3-credentials) | ✓        | ✓         | object  |         | The credentials to access AWS S3 for writing and reading  |
| region                         |          | ✓         | string  |         | The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this property  |
| endpoint                       |          | ✓         | string  |         |Overrides the default hostname to connect to an S3-compatible service. i.e, http://s3-like-service:9000  |
| forcePathStyle                 |          | ✓         | boolean |         | To enable path-style addressing(i.e., http://s3.amazonaws.com/BUCKET/KEY) when connecting to an S3-compatible service that lack of support for sub-domain style bucket URLs (i.e., http://BUCKET.s3.amazonaws.com/KEY). Defaults to false  |
| storageClass                   |          | ✓         | string  |         | By default, the "STANDARD" storage class is used. Other supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced Redundancy  |

### S3 Credentials

| Property                                                                                                    | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | AWS Access Key ID |
| [secretKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | AWS Secret Access Key  |

## GSC - Google Cloud Storage configuration

| Property                        | Required | Updatable | Type   | Default | Description |
|:--------------------------------|----------|-----------|:-------|:--------|:------------|
| bucket                          | ✓        | ✓         | string |         | Specify bucket where to store backups (eg. x4m-test-bucket) |
| path                            |          | ✓         | string |         | Specify bueckt path where to store backups (eg. /walg-folder) |
| [credentials](#gcp-credentials) | ✓        | ✓         | object |         | The credentials to access GCS for writing and reading |

### GCP Credentials

| Property                                                                                                                | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------------------|----------|:----------|:-------|:--------|:------------|
| [serviceAccountJsonKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | The key of the secret to select from. Must be a valid secret key |


## AZURE - Azure Blob Storage configuration

| Property                          | Required | Updatable | Type    | Default | Description |
|:----------------------------------|----------|-----------|:--------|:--------|:-------------|
| bucket                            | ✓        | ✓         | string  |         | Specify bucket where to store backups in Azure storage (eg. test-container) |  
| path                              |          | ✓         | string  |         | Specify bucket path where to store backups in Azure storage (eg. /walg-folder) |  
| [credentials](#azure-credentials) | ✓        | ✓         | object  |         | AWS Secret Access Key  |

### Azure Credentials

| Property                                                                                                    | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:-------------|
| [account](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core)   | ✓        | ✓         | object |         | The name of the storage account |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | The primary or secondary access key for the storage account. |
