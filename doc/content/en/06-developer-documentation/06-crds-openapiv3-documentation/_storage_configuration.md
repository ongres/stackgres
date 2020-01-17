# Storage Configuration

| Property | Type | Description |
|-----------|------|-------------|
| type | string  | Type of storage: <br>- volume: A kubernetes volume source of type nfs, cefhfs or glusterfs <br>- s3: Amazon Web Services S3 <br>- gcs: Google Clooud Storage <br>- azureblob: Azure Blob Storage  |
| [volume](#volume)  | object  | A kubernetes volume source of type nfs, cefhfs or glusterfs |


### Volume

| Property | Type | Description |
|-----------|------|-------------|
| size | string  | The size of the persistent volume claim for backups |
| writeManyStorageClass  | string  | The storage class of the persistent volume claim for backups. It must allow ReadWriteMany access mode  |


### S3 - Amazon Web Services S3 configuration

| Property | Type | Description |
|-----------|------|-------------|
| prefix | string  | The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder) |
| [credentials](#s3-credentials)  | object  | The credentials to access AWS S3 for writing and reading  |
| region  | string  | The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this property  |
| endpoint  | string  | Overrides the default hostname to connect to an S3-compatible service. i.e, http://s3-like-service:9000  |
| forcePathStyle  | boolean  | To enable path-style addressing(i.e., http://s3.amazonaws.com/BUCKET/KEY) when connecting to an S3-compatible service that lack of support for sub-domain style bucket URLs (i.e., http://BUCKET.s3.amazonaws.com/KEY). Defaults to false  |
| storageClass  | string  | By default, the \"STANDARD\" storage class is used. Other supported values include \"STANDARD_IA\" for Infrequent Access and \"REDUCED_REDUNDANCY\" for Reduced Redundancy  |
| sse  | string  | To enable S3 server-side encryption, set to the algorithm to use when storing the objects in S3 (i.e., AES256, aws:kms)  |
| sseKmsId  | string  | If using S3 server-side encryption with aws:kms, the KMS Key ID to use for object encryption  |
| cseKmsId  | string  | To configure AWS KMS key for client side encryption and decryption. By default, no encryption is used. (region or cseKmsRegion required to be set when using AWS KMS key client side encryption)  |
| cseKmsRegion  | string  | To configure AWS KMS key region for client side encryption and decryption (i.e., eu-west-1)  |

#### S3 Credentials

| Property | Type | Description |
|-----------|------|-------------|
| [accessKey]() | object  | The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder) |
| [secretKey]()  | object  | AWS Secret Access Key  |


**accessKey**

| Property | Type | Description |
|-----------|------|-------------|
| key | string  | The key of the secret to select from. Must be a valid secret key |
| name  | string  | Name of the referent. [More info](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names)  |


**secretKey**

| Property | Type | Description |
|-----------|------|-------------|
| key | string  | The key of the secret to select from. Must be a valid secret key |
| name  | string  | Name of the referent. [More info](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names)  |



### GSC - Google Cloud Storage configuration


| Property | Type | Description |
|-----------|------|-------------|
| prefix | string  | Specify where to store backups (eg. gs://x4m-test-bucket/walg-folder) |
| [credentials](#gcp-credentials)  | object  | The credentials to access GCS for writing and reading |


#### GCP Credentials

**serviceAccountJsonKey**

| Property | Type | Description |
|-----------|------|-------------|
| key | string  | The key of the secret to select from. Must be a valid secret key |
| name  | string  | Name of the referent. [More info](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names)  |


### AZURE - Azure Blob Storage configuration

| Property | Type | Description |
|-----------|------|-------------|
| prefix | string  | Specify where to store backups in Azure storage (eg. azure://test-container/walg-folder) |
| [credentials](#azure-credentials)  | object  | AWS Secret Access Key  |
| bufferSize  | integer  | Overrides the default upload buffer size of 67108864 bytes (64 MB). Note that the size of the buffer must be specified in bytes. Therefore, to use 32 MB sized buffers, this variable should be set to 33554432 bytes  |
| maxBuffers  | integer  | Overrides the default maximum number of upload buffers. By default, at most 3 buffers are used concurrently  |

#### Azure Credentials


| Property | Type | Description |
|-----------|------|-------------|
| account | object  | The name of the storage account |
| accessKey  | string  | Name of the referent. [More info](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names)  |


**account**

| Property | Type | Description |
|-----------|------|-------------|
| key | string  | The key of the secret to select from. Must be a valid secret key |
| name  | string  | Name of the referent. [More info](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names)  |


