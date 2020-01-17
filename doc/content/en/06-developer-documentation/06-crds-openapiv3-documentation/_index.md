---
title: CRDs OpenApiV3 Documentation
weight: 6
---

- [Cluster](#stackgrescluster)
- [Profile](#stackgresprofile)
- [Postgres Config](#stackgrespostgresconfig)
- [Pooling Config](#stackgresconnectionpoolingconfig)
- [Backups Config](#stackgresbackupconfig)
- [Backups](#stackgresbackup)

# StackGresCluster

The cluster CRD represent the main params to create a new StackGres cluster.

___

**Kind:** StackGresCluster

**listKind:** StackGresClusterList

**plural:** sgclusters

**singular:** sgcluster
___

**Properties**


| Property | Type | Description |
|-----------|------|-------------|
| instances | integer  | Number of instances to be created  |
| pgVersion | string  | PostgreSQL version for the new cluster  |
| pgConfig | string  | PostgreSQL configuration to apply  |
| connectionPoolingConfig | string  | Pooling configuration to apply  |
| resourceProfile | string  | Resource profile size to apply  |
| volumeSize | string  | Storage volume size  |
| storageClass | string  | Storage class name to be used for the cluster  |
| sidecars | array  | List of sidecars to include in the cluster  |
| [nonProduction](#non-production-options)  | array  | Additional parameter for non production environments  |

Example:

```
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
  labels:
    app: stackgres
    chart: stackgres-cluster-0.8-SNAPSHOT
    heritage: Tiller
    release: stackgres
spec:
  instances: 3
  pgVersion: '11.6'
  pgConfig: 'postgresconf'
  connectionPoolingConfig: 'pgbouncerconf'
  resourceProfile: 'size-xs'
  backupConfig: 'backupconf'
  volumeSize: '5Gi'
  prometheusAutobind: true
  sidecars:
  - connection-pooling
  - postgres-util
  - prometheus-postgres-exporter
  nonProduction:
    disableClusterPodAntiAffinity: true
```

## Non Production options
| Property | Type | Description |
|-----------|------|-------------|
| disableClusterPodAntiAffinity | boolean | Disable the pod Anti-Affinity rule |

# StackGresProfile

The Profile CRD represent the main params for instance resouces.

___

**Kind:** StackGresProfile

**listKind:** StackGresProfileList

**plural:** sgprofiles

**singular:** sgprofile
___

**Properties**


| Property | Type | Description |
|-----------|------|-------------|
| cpu | string  | CPU amount to be used  |
| memory | string  | Memory size to be used  |


Example:

```
apiVersion: stackgres.io/v1alpha1
  kind: StackGresProfile
  metadata:
    annotations:
      helm.sh/hook: pre-install
    labels:
      app: stackgres
      chart: stackgres-cluster-0.8
      heritage: Tiller
      release: stackgres
    name: size-l
    namespace: default
  spec:
    cpu: "4"
    memory: 8Gi
```

# StackGresPostgresConfig

The Profile CRD represent the PostgreSQL version and params to be configured.

___

**Kind:** StackGresPostgresConfig

**listKind:** StackGresPostgresConfigList

**plural:** sgpgconfigs

**singular:** sgpgconfig
___

**Properties**

| Property | Type | Description |
|-----------|------|-------------|
| pgVersion | string  | PostgreSQL configuration version  |
| postgresql.conf | object  | List of PostgreSQL configuration parameters with their values  |


Example:

```
apiVersion: stackgres.io/v1alpha1
kind: StackGresPostgresConfig
metadata:
  name: postgresconf
  labels:
    app: stackgres
    chart: stackgres-cluster-0.8
    heritage: Tiller
    release: stackgres
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  pgVersion: "11"
  postgresql.conf:
    password_encryption: 'scram-sha-256'
    random_page_cost: '1.5'
    shared_buffers: '256MB'
    wal_compression: 'on'
```

# StackGres Connection Pooling Config

The Profile CRD represent the PostgreSQL version and params to be configured.

___

**Kind:** StackGresConnectionPoolingConfig

**listKind:** StackGresConnectionPoolingConfigList

**plural:** sgconnectionpoolingconfigs

**singular:** sgconnectionpoolingconfig
___

**Properties**


| Property | Type | Description |
|-----------|------|-------------|
| pgbouncer.ini | object  | pgbouncer.ini configuration  |


Example:

```
apiVersion: stackgres.io/v1alpha1
kind: StackGresConnectionPoolingConfig
metadata:
  name: pgbouncerconf
  labels:
    app: stackgres
    chart: stackgres-cluster-0.8-SNAPSHOT
    heritage: Tiller
    release: stackgres
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  pgbouncer.ini:
    default_pool_size: '200'
    max_client_conn: '200'
    pool_mode: 'transaction'
```

# StackGresBackupConfig

The Profile CRD represent the backups configuration for the cluster.

___

**Kind:** StackGresBackupConfig

**listKind:** StackGresBackupConfigList

**plural:** sgbackupconfigs

**singular:** sgbackupconfig
___

**Properties**


| Property | Type | Description |
|-----------|------|-------------|
| retention | integer  | Retains specified number of full backups. Default is 5  |
| fullSchedule  | string  | Specify when to perform full backups using cron syntax:<br><minute: 0 to 59, or *> <hour: 0 to 23, or * for any value. All times UTC> <day of the month: 1 to 31, or *> <month: 1 to 12, or *> <day of the week: 0 to 7 (0 and 7 both represent Sunday), or *>. <br>If not specified full backups will be performed each day at 05:00 UTC  |
| fullWindow  | integer  | Specify the time window in minutes where a full backup will start happening after the point in time specified by fullSchedule. If for some reason the system is not capable to start the full backup it will be skipped. If not specified the window will be of 1 hour  |
| compressionMethod  | string  | To configure compression method used for backups. Possible options are: lz4, lzma, brotli. Default method is lz4. LZ4 is the fastest method, but compression ratio is bad. LZMA is way much slower, however it compresses backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio which is about 3 times better than LZ4  |
| networkRateLimit  | integer  | To configure disk read rate limit during uploads in bytes per second  |
| uploadDiskConcurrency  | integer  | To configure how many concurrency streams are reading disk during uploads. By default 1 stream  |
| [pgpConfiguration](#pgp-configuration) | string  | The OpenPGP configuration for encryption and decryption backups with the following properties: - key: PGP private key  |
| tarSizeThreshold  | integer  | To configure the size of one backup bundle (in bytes). Smaller size causes granularity and more optimal, faster recovering. It also increases the number of storage requests, so it can costs you much money. Default size is 1 GB (1 << 30 - 1 bytes)  |
| [storage](#storage-configuration)  | object  | Backup storage configuration  |

# PGP configuration

| Property | Type | Description |
|-----------|------|-------------|
| [key](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | object  | PGP private key  |

# Storage Configuration

| Property | Type | Description |
|-----------|------|-------------|
| type | string  | Type of storage: <br>- s3: Amazon Web Services S3 <br>- gcs: Google Clooud Storage <br>- azureblob: Azure Blob Storage  |
| [s3](#s3--amazon-web-services-s3-configuration)  | object  | Amazon Web Services S3 configuration |
| [gcs](#gsc--google-cloud-storage-configuration)  | object  | Google Cloud Storage configuration |
| [azureblob](#azure--azure-blob-storage-configuration)  | object  | Google Cloud Storage configuration |

## S3 - Amazon Web Services S3 configuration

| Property | Type | Description |
|-----------|------|-------------|
| prefix | string  | The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder) |
| [credentials](#s3-credentials)  | object  | The credentials to access AWS S3 for writing and reading  |
| region  | string  | The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this property  |
| endpoint  | string  | Overrides the default hostname to connect to an S3-compatible service. i.e, http://s3-like-service:9000  |
| forcePathStyle  | boolean  | To enable path-style addressing(i.e., http://s3.amazonaws.com/BUCKET/KEY) when connecting to an S3-compatible service that lack of support for sub-domain style bucket URLs (i.e., http://BUCKET.s3.amazonaws.com/KEY). Defaults to false  |
| storageClass  | string  | By default, the "STANDARD" storage class is used. Other supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced Redundancy  |
| sse  | string  | To enable S3 server-side encryption, set to the algorithm to use when storing the objects in S3 (i.e., AES256, aws:kms)  |
| sseKmsId  | string  | If using S3 server-side encryption with aws:kms, the KMS Key ID to use for object encryption  |
| cseKmsId  | string  | To configure AWS KMS key for client side encryption and decryption. By default, no encryption is used. (region or cseKmsRegion required to be set when using AWS KMS key client side encryption)  |
| cseKmsRegion  | string  | To configure AWS KMS key region for client side encryption and decryption (i.e., eu-west-1)  |

### S3 Credentials

| Property | Type | Description |
|-----------|------|-------------|
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | object  | The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder) |
| [secretKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core)  | object  | AWS Secret Access Key  |

## GSC - Google Cloud Storage configuration

| Property | Type | Description |
|-----------|------|-------------|
| prefix | string  | Specify where to store backups (eg. gs://x4m-test-bucket/walg-folder) |
| [credentials](#gcp-credentials)  | object  | The credentials to access GCS for writing and reading |

### GCP Credentials

| Property | Type | Description |
|-----------|------|-------------|
| [serviceAccountJsonKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | object  | The key of the secret to select from. Must be a valid secret key |


## AZURE - Azure Blob Storage configuration

| Property | Type | Description |
|-----------|------|-------------|
| prefix | string  | Specify where to store backups in Azure storage (eg. azure://test-container/walg-folder) |
| [credentials](#azure-credentials)  | object  | AWS Secret Access Key  |
| bufferSize  | integer  | Overrides the default upload buffer size of 67108864 bytes (64 MB). Note that the size of the buffer must be specified in bytes. Therefore, to use 32 MB sized buffers, this variable should be set to 33554432 bytes  |
| maxBuffers  | integer  | Overrides the default maximum number of upload buffers. By default, at most 3 buffers are used concurrently  |

### Azure Credentials

| Property | Type | Description |
|-----------|------|-------------|
| [account](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | object  | The name of the storage account |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | object  | The primary or secondary access key for the storage account. |
