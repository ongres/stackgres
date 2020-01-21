---
title: Postgres clusters
weight: 1
---

StackGres PostgreSQL cluster can be created using a cluster Custom Resource (CR) in Kubernetes.

___

**Kind:** StackGresCluster
**listKind:** StackGresClusterList
**plural:** sgclusters
**singular:** sgcluster
___

**Properties**

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| instances | x | integer  | Number of instances to be created (for example 1) |
| pgVersion | x | string  | PostgreSQL version for the new cluster (for example 11.6) |
| volumeSize | x | string  | Storage volume size (for example 5Gi) |
| storageClass | x | string  | Storage class name to be used for the cluster (if not specified means default storage class wiil be used) |
| [pgConfig](#postgresql-configuration) | x | string  | PostgreSQL configuration to apply |
| [connectionPoolingConfig](#connection-pooling-configuration) | x | string  | Pooling configuration to apply |
| [resourceProfile](#resource-profile-configuration) | x | string  | Resource profile size to apply |
| prometheusAutobind |   | boolean | If enabled a ServiceMonitor will be created for each Prometheus instance found in order to collect metrics |
| [sidecars](#sidecar-containers) |   | array  | List of sidecars to include in the cluster |
| [backupConfig](#backup-configuration) |   | string | Backup config to apply |
| [nonProduction](#non-production-options) |   | array  | Additional parameters for non production environments |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
spec:
  instances: 1
  pgVersion: '11.6'
  volumeSize: '5Gi'
  pgConfig: 'postgresconf'
  connectionPoolingConfig: 'pgbouncerconf'
  resourceProfile: 'size-xs'
  backupConfig: 'backupconf'
```

## Sidecar containers

A sidecar container is a container that adds functionality to PostgreSQL or to the cluster
 infrastructure. Currently StackGres implement following sidecar containers:

* `envoy`: this container is always present even if not specified in the configuration. It serve as
 a edge proxy from client to PostgreSQL instances or between PostgreSQL instances. It enables
 network metrics collection to provide connection statistics.
* `pgbouncer`: a container with pgbouncer as the connection pooling for the PostgreSQL instances.
* `prometheus-postgres-exporter`: a container with postgres exporter at the metrics exporter for
 the PostgreSQL instances.
* `postgres-util`: a container with psql and all PostgreSQL common tools in order to connect to the
 database directly as root to perform any administration tasks.

Following sinnept enables all sidecars but `postgres-util`:

```yaml
  sidecars:
    - envoy
    - pgbouncer
    - prometheus-postgres-exporter
```

## Non Production options

Following options should be enabled only when NOT working in a production environment.

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| disableClusterPodAntiAffinity |   | boolean | Disable the pod Anti-Affinity rule |

# PostgreSQL configuration

A PostgreSQL configuration CR represent the configuration of a specific PostgreSQL major
 version.

___

**Kind:** StackGresPostgresConfig
**listKind:** StackGresPostgresConfigList
**plural:** sgpgconfigs
**singular:** sgpgconfig
___

**Properties**

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| pgVersion | x | string  | PostgreSQL configuration version (for example 11) |
| postgresql.conf |   | object  | List of PostgreSQL configuration parameters with their values  |


Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresPostgresConfig
metadata:
  name: postgresconf
spec:
  pgVersion: "11"
  postgresql.conf:
    password_encryption: 'scram-sha-256'
    random_page_cost: '1.5'
    shared_buffers: '256MB'
    wal_compression: 'on'
```

# Connection pooling configuration

The connection pooling CR represent the configuration of PgBouncer.

___

**Kind:** StackGresConnectionPoolingConfig
**listKind:** StackGresConnectionPoolingConfigList
**plural:** sgconnectionpoolingconfigs
**singular:** sgconnectionpoolingconfig
___

**Properties**

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| pgbouncer.ini |   | object  | Section [pgbouncer] of pgbouncer.ini configuration |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresConnectionPoolingConfig
metadata:
  name: pgbouncerconf
spec:
  pgbouncer.ini:
    default_pool_size: '200'
    max_client_conn: '200'
    pool_mode: 'transaction'
```

# Resource profile configuration

The Resource profile CR represent the CPU and memory resources assigned to each Pod of the cluster.

___

**Kind:** StackGresProfile
**listKind:** StackGresProfileList
**plural:** sgprofiles
**singular:** sgprofile
___

**Properties**

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| cpu | x | string  | CPU amount to be used  |
| memory | x | string  | Memory size to be used  |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
  kind: StackGresProfile
  metadata:
    name: size-l
  spec:
    cpu: "4"
    memory: 8Gi
```

# Backup configuration

The backup configuration CR represent the backups configuration of the cluster.

___

**Kind:** StackGresBackupConfig
**listKind:** StackGresBackupConfigList
**plural:** sgbackupconfigs
**singular:** sgbackupconfig
___

**Properties**


| Property | Required | Type | Description |
|-----------|------|------|-------------|
| retention | x | integer  | Retains specified number of full backups. Default is 5  |
| fullSchedule | x | string  | Specify when to perform full backups using cron syntax:<br><minute: 0 to 59, or *> <hour: 0 to 23, or * for any value. All times UTC> <day of the month: 1 to 31, or *> <month: 1 to 12, or *> <day of the week: 0 to 7 (0 and 7 both represent Sunday), or *>. <br>If not specified full backups will be performed each day at 05:00 UTC  |
| fullWindow | x | integer  | Specify the time window in minutes where a full backup will start happening after the point in time specified by fullSchedule. If for some reason the system is not capable to start the full backup it will be skipped. If not specified the window will be of 1 hour  |
| compressionMethod | x | string  | To configure compression method used for backups. Possible options are: lz4, lzma, brotli. Default method is lz4. LZ4 is the fastest method, but compression ratio is bad. LZMA is way much slower, however it compresses backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio which is about 3 times better than LZ4  |
| networkRateLimit | x | integer  | To configure disk read rate limit during uploads in bytes per second  |
| uploadDiskConcurrency | x | integer  | To configure how many concurrency streams are reading disk during uploads. By default 1 stream  |
| [pgpConfiguration](#pgp-configuration) |   | string  | The OpenPGP configuration for encryption and decryption backups with the following properties: - key: PGP private key  |
| tarSizeThreshold |   | integer  | To configure the size of one backup bundle (in bytes). Smaller size causes granularity and more optimal, faster recovering. It also increases the number of storage requests, so it can costs you much money. Default size is 1 GB (1 << 30 - 1 bytes)  |
| [storage](#storage-configuration) | x | object  | Backup storage configuration  |

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

# PGP configuration

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| [key](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | x | object  | PGP private key  |

# Storage Configuration

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| type | x | string  | Type of storage: <br>- s3: Amazon Web Services S3 <br>- gcs: Google Clooud Storage <br>- azureblob: Azure Blob Storage  |
| [s3](#s3--amazon-web-services-s3-configuration) | ? | object  | Amazon Web Services S3 configuration |
| [gcs](#gsc--google-cloud-storage-configuration) | ? | object  | Google Cloud Storage configuration |
| [azureblob](#azure--azure-blob-storage-configuration) | ? | object  | Google Cloud Storage configuration |

## S3 - Amazon Web Services S3 configuration

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| prefix | x | string  | The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder) |
| [credentials](#s3-credentials) | x | object  | The credentials to access AWS S3 for writing and reading  |
| region |   | string  | The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this property  |
| endpoint |   | string  | Overrides the default hostname to connect to an S3-compatible service. i.e, http://s3-like-service:9000  |
| forcePathStyle |   | boolean  | To enable path-style addressing(i.e., http://s3.amazonaws.com/BUCKET/KEY) when connecting to an S3-compatible service that lack of support for sub-domain style bucket URLs (i.e., http://BUCKET.s3.amazonaws.com/KEY). Defaults to false  |
| storageClass |   | string  | By default, the "STANDARD" storage class is used. Other supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced Redundancy  |
| sse |   | string  | To enable S3 server-side encryption, set to the algorithm to use when storing the objects in S3 (i.e., AES256, aws:kms)  |
| sseKmsId |   | string  | If using S3 server-side encryption with aws:kms, the KMS Key ID to use for object encryption  |
| cseKmsId |   | string  | To configure AWS KMS key for client side encryption and decryption. By default, no encryption is used. (region or cseKmsRegion required to be set when using AWS KMS key client side encryption)  |
| cseKmsRegion |   | string  | To configure AWS KMS key region for client side encryption and decryption (i.e., eu-west-1)  |

### S3 Credentials

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | x | object  | The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder) |
| [secretKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | x | object  | AWS Secret Access Key  |

## GSC - Google Cloud Storage configuration

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| prefix | x | string  | Specify where to store backups (eg. gs://x4m-test-bucket/walg-folder) |
| [credentials](#gcp-credentials) | x | object  | The credentials to access GCS for writing and reading |

### GCP Credentials

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| [serviceAccountJsonKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | x | object  | The key of the secret to select from. Must be a valid secret key |


## AZURE - Azure Blob Storage configuration

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| prefix | x | string  | Specify where to store backups in Azure storage (eg. azure://test-container/walg-folder) |
| [credentials](#azure-credentials) | x | object  | AWS Secret Access Key  |
| bufferSize |   | integer  | Overrides the default upload buffer size of 67108864 bytes (64 MB). Note that the size of the buffer must be specified in bytes. Therefore, to use 32 MB sized buffers, this variable should be set to 33554432 bytes  |
| maxBuffers |   | integer  | Overrides the default maximum number of upload buffers. By default, at most 3 buffers are used concurrently  |

### Azure Credentials

| Property | Required | Type | Description |
|-----------|------|------|-------------|
| [account](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | x | object  | The name of the storage account |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | x | object  | The primary or secondary access key for the storage account. |
