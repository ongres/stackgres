---
title: Restoration
weight: 5
---

The restoration configuration CR stores configuration parameters needed to
 perform a restoration

 ___
**Kind:** StackgresRestoreConfig

**listKind:** StackgresRestoreConfigList

**plural:** sgrestoreconfigs

**singular:** sgrestoreconfig
___

**Spec**

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| source | ✓ | object  | restoration source configuration. For detail see the restoration source [section](#restore-source).  | |
| compressionMethod |  | string  | Compression method that was used during the backup, could be:  lz4, lzma or brotli  | lz4 |
| downloadDiskConcurrency |  | integer | How many concurrent downloads will attempts during the restoration   | 1 |
| pgpConfiguration | | object | The OpenPGP configuration for encryption and decryption backups with the following properties: - key: PGP private key | |

Example: 

``` yaml
apiVersion: stackgres.io/v1alpha1
kind: StackgresRestoreConfig
metadata:
  name: restoreconf
spec:
  source:
    fromBackup: 70f915a5-11ab-485a-a991-3ecfb7bbb8f0
  compressionMethod: lz4  
  downloadDiskConcurrency: 1
```

The restore default settings are stored in the same namespaces of the stackgres operator,
 with the name `defaultrestoreconfig`

Therefore, iven a stackgres operator installed in the `stackgres` namespace we can see the backup default values with de command:

``` sh
kubectl get sgrestoreconfigs.stackgres.io -n stackgres defaultrestoreconfig
```

# Restore source

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| fromBackup | | string  | The UID of the backup CR to restore. If configured, it will ignore the fromStorage and backupName options. | |
| autoCopySecrets |  | boolean  | If the backup is in another namespace, the secrets that holds the storage might be needed to copy in the new cluster namespaces. If is set to true, it will copy the required secrets  | false |
| fromStorage |  | object | Storage configuration in where is located the backup. If set, the backupName is required. For datails see the restore storage [section](#restore-storate)   | |
| backupName | | string | Name of the backup to restore. If 'LATEST' is used, it will restore the most recent backup found in the storage | LATEST |

# Restore storage

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
| prefix | ✓ | string  | Specify where to restore the backups (eg. gs://x4m-test-bucket/walg-folder) |   |
| [credentials](#gcp-credentials) | ✓ | object  | The credentials to access GCS for writing and reading |   |

### GCP Credentials

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| [serviceAccountJsonKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | The key of the secret to select from. Must be a valid secret key |   |


## AZURE - Azure Blob Storage configuration

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| prefix | ✓ | string  | Specify where to restore the backups in Azure storage (eg. azure://test-container/walg-folder) |   |
| [credentials](#azure-credentials) | ✓ | object  | AWS Secret Access Key  |   |


### Azure Credentials

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| [account](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | The name of the storage account |   |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.12/#secretkeyselector-v1-core) | ✓ | object  | The primary or secondary access key for the storage account. |   |
