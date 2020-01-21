---
title: Backup settings
weight: 5
---

The backup configuration CR represent the backups configuration of the cluster.

___

**Kind:** StackGresBackupConfig

**listKind:** StackGresBackupConfigList

**plural:** sgbackupconfigs

**singular:** sgbackupconfig
___

**Properties**


| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| retention | ✓ | integer  | Retains specified number of full backups. Default is 5 | 5 |
| fullSchedule | ✓ | string  | Specify when to perform full backups using cron syntax:<br><minute: 0 to 59, or *> <hour: 0 to 23, or * for any value. All times UTC> <day of the month: 1 to 31, or *> <month: 1 to 12, or *> <day of the week: 0 to 7 (0 and 7 both represent Sunday), or *>. <br>If not specified full backups will be performed each day at 05:00 UTC  | 05:00 UTC |
| fullWindow | ✓ | integer  | Specify the time window in minutes where a full backup will start happening after the point in time specified by fullSchedule. If for some reason the system is not capable to start the full backup it will be skipped. If not specified the window will be of 1 hour  | 1 hour |
| compressionMethod | ✓ | string  | To configure compression method used for backups. Possible options are: lz4, lzma, brotli. Default method is lz4. LZ4 is the fastest method, but compression ratio is bad. LZMA is way much slower, however it compresses backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio which is about 3 times better than LZ4  | LZ4 |
| networkRateLimit | ✓ | integer  | To configure disk read rate limit during uploads in bytes per second  |   |
| uploadDiskConcurrency | ✓ | integer  | To configure how many concurrency streams are reading disk during uploads. By default 1 stream  | 1 |
| [pgpConfiguration](#pgp-configuration) |   | string  | The OpenPGP configuration for encryption and decryption backups with the following properties: - key: PGP private key  |   |
| tarSizeThreshold |   | integer  | To configure the size of one backup bundle (in bytes). Smaller size causes granularity and more optimal, faster recovering. It also increases the number of storage requests, so it can costs you much money. Default size is 1 GB (1 << 30 - 1 bytes)  | 1 GB |
| [storage](#storage-configuration) | ✓ | object  | Backup storage configuration  |   |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresBackupConfig
metadata:
  name: backupconf
spec:
  retention: 5
  fullSchedule: 0 5 * * *
  fullWindow: 60
  storage:
    type: s3
    s3:
      credentials:
        accessKey:
          key: accesskey
          name: my-cluster-minio
        secretKey:
          key: secretkey
          name: my-cluster-minio
      endpoint: http://my-cluster-minio:9000
      forcePathStyle: true
      prefix: s3://stackgres
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

# PGP configuration

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| [key](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | PGP private key  |   |

# Storage Configuration

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| type | ✓ | string  | Type of storage: <br>- s3: Amazon Web Services S3 <br>- gcs: Google Clooud Storage <br>- azureblob: Azure Blob Storage  |   |
| [s3](#s3--amazon-web-services-s3-configuration) | ? | object  | Amazon Web Services S3 configuration |   |
| [gcs](#gsc--google-cloud-storage-configuration) | ? | object  | Google Cloud Storage configuration |   |
| [azureblob](#azure--azure-blob-storage-configuration) | ? | object  | Google Cloud Storage configuration |   |

## S3 - Amazon Web Services S3 configuration

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| prefix | ✓ | string  | The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder) | s3://stackgres |
| [credentials](#s3-credentials) | ✓ | object  | The credentials to access AWS S3 for writing and reading  |   |
| region |   | string  | The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this property  | k8s |
| endpoint |   | string  | Overrides the default hostname to connect to an S3-compatible service. i.e, http://s3-like-service:9000  | http://minio.stackgres.svc:9000 |
| forcePathStyle |   | boolean  | To enable path-style addressing(i.e., http://s3.amazonaws.com/BUCKET/KEY) when connecting to an S3-compatible service that lack of support for sub-domain style bucket URLs (i.e., http://BUCKET.s3.amazonaws.com/KEY). Defaults to false  | true |
| storageClass |   | string  | By default, the "STANDARD" storage class is used. Other supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced Redundancy  |   |
| sse |   | string  | To enable S3 server-side encryption, set to the algorithm to use when storing the objects in S3 (i.e., AES256, aws:kms)  |   |
| sseKmsId |   | string  | If using S3 server-side encryption with aws:kms, the KMS Key ID to use for object encryption  |   |
| cseKmsId |   | string  | To configure AWS KMS key for client side encryption and decryption. By default, no encryption is used. (region or cseKmsRegion required to be set when using AWS KMS key client side encryption)  |   |
| cseKmsRegion |   | string  | To configure AWS KMS key region for client side encryption and decryption (i.e., eu-west-1)  |   |

### S3 Credentials

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder) | minio / accesskey |
| [secretKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | AWS Secret Access Key  | minio / secretkey |

## GSC - Google Cloud Storage configuration

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| prefix | ✓ | string  | Specify where to store backups (eg. gs://x4m-test-bucket/walg-folder) |   |
| [credentials](#gcp-credentials) | ✓ | object  | The credentials to access GCS for writing and reading |   |

### GCP Credentials

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| [serviceAccountJsonKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | The key of the secret to select from. Must be a valid secret key |   |


## AZURE - Azure Blob Storage configuration

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| prefix | ✓ | string  | Specify where to store backups in Azure storage (eg. azure://test-container/walg-folder) |   |
| [credentials](#azure-credentials) | ✓ | object  | AWS Secret Access Key  |   |
| bufferSize |   | integer  | Overrides the default upload buffer size of 67108864 bytes (64 MB). Note that the size of the buffer must be specified in bytes. Therefore, to use 32 MB sized buffers, this variable should be set to 33554432 bytes  | 64 MB |
| maxBuffers |   | integer  | Overrides the default maximum number of upload buffers. By default, at most 3 buffers are used concurrently  | 3 |

### Azure Credentials

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| [account](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | The name of the storage account |   |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | The primary or secondary access key for the storage account. |   |
