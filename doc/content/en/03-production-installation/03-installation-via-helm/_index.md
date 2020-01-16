---
title: Installation via Helm
weight: 3
---

# Installation via Helm

StackGres operator and clusters can be installed using [helm](https://helm.sh/) version >= `2.14`.

## Install Operator

```shell
helm install ongres/stackgres-operator
```

### Parameters

You can specify following parameters values:

* `cert.autoapprove`: if set to false disable automatic approve of certificate
 used by the operator. If disabled the operator installation will not complete
 until the certificate is approved by the kubernetes cluster administrator.
 Default is true.
* `prometheus.allowAutobind`: if set to false disable automatic bind to prometheus
 created using the [prometheus operator](https://github.com/coreos/prometheus-operator).
 If disabled the cluster will not be binded to prometheus when created until done
 manually by the kubernetes cluster administrator.
* `grafana.url`: the URL of the PostgreSQL dashboard created in grafana (required to
 enable the integration of the StackGres UI with grafana)
* `grafana.token`: the grafana API token to access the PostgreSQL dashboard created
 in grafana (required to enable the integration of the StackGres UI with grafana)
* `grafana.httpHost`: the service host name to access grafana (required to enable
 the integration of the StackGres UI with grafana)
* `grafana.schema`: the schema to access grafana. By default http.

## Create a Cluster

```shell
helm install --dep-up ongres/stackgres-cluster
```

### Parameters

You can specify following parameters values:

* `config.create`: If true create configuration CRs.
* `config.postgresql.version`: The PostgreSQL versio to use.
* `profiles.create`: If true creates the default profiles.
* `cluster.create`: If true create the cluster (useful to just create configurations).
* `cluster.instances`: The number of instances in the cluster.
* `cluster.pgconfig`: The PostgreSQL configuration CR name.
* `cluster.poolingconfig`: The PgBouncer configuration CR name.
* `cluster.profile`: The profile name used to create cluster's Pods.
* `cluster.volumeSize`: The size set in the persistent volume claim of PostgreSQL data.
* `cluster.storageclass`: The storage class used for the persisitent volume claim of PostgreSQL data.
 If defined, storageClassName: <storageClass>. If set to "-", storageClassName: "", which disables dynamic provisioning.
 If undefined (the default) or set to null, no storageClassName spec is set, choosing the default provisioner.
 (gp2 on AWS, standard on GKE, AWS & OpenStack).
 
#### Backups

By default the chart create a storage class backed by an NFS server and associate it to the `cluster.backup.volumeWriteManyStorageClass`
 parameter. To avoid the creation of the NFS storage class just specify the `cluster.backup.volumeWriteManyStorageClass` or fill any of
 the `cluster.backup.s3`, `cluster.backup.gcs` or `cluster.backup.azureblob` sections.
 
* `cluster.backup.create`: If true create and set the backup configuration for the cluster.
* `cluster.backup.name`: The backup configuration CR name.
* `cluster.backup.retention`: Retains specified number of full backups. Default is 5.
* `cluster.backup.fullSchedule`: Specify when to perform full backups using cron syntax:
 <minute: 0 to 59, or *> <hour: 0 to 23, or * for any value. All times UTC> <day of the month: 1 to 31, or *>
 <month: 1 to 12, or *> <day of the week: 0 to 7 (0 and 7 both represent Sunday), or *>.
 If not specified full backups will be performed each day at 05:00 UTC.
* `cluster.backup.fullWindow`: Specify the time window in minutes where a full backup will start happening after the point in
 time specified by fullSchedule. If for some reason the system is not capable to start the full backup it will be skipped.
 If not specified the window will be of 1 hour.
* `cluster.backup.compressionMethod`: To configure compression method used for backups. Possible options are: lz4, lzma, brotli.
 Default method is lz4. LZ4 is the fastest method, but compression ratio is bad. LZMA is way much slower, however it compresses
 backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio which is about 3 times
 better than LZ4.
* `cluster.backup.uploadDiskConcurrency`: To configure how many concurrency streams are reading disk during uploads. By default 1 stream.
* `cluster.backup.tarSizeThreshold`: To configure the size of one backup bundle (in bytes). Smaller size causes granularity and more optimal,
 faster recovering. It also increases the number of storage requests, so it can costs you much money. Default size is 1 GB (1 << 30 - 1 bytes).
* `cluster.backup.networkRateLimit`: To configure the network upload rate limit during uploads in bytes per second.
* `cluster.backup.diskRateLimit`: To configure disk read rate limit during uploads in bytes per second.
* `cluster.backup.pgpConfiguration.name`: The name of the secret with the private key of the OpenPGP configuration for encryption and decryption backups.
* `cluster.backup.pgpConfiguration.key`: The key in the secret with the private key of the OpenPGP configuration for encryption and decryption backups.
* `cluster.backup.nfs.create`: If true create a storage class backed by an NFS server that will be used to store backups.

##### Volume Source

* `cluster.backup.volumeSize`: Define the size of the volume used for backups.
* `cluster.backup.volumeWriteManyStorageClass`: Define the storage class name that will be used to store backups. Must support ReadWriteMany mode access mode.
 If defined, storageClassName: <volumeWriteManyStorageClass>. If set to "-", storageClassName: "", which disables dynamic provisioning.
 If undefined (the default) or set to null, no storageClassName spec is set, choosing the default provisioner.
 (gp2 on AWS, standard on GKE, AWS & OpenStack).

##### Amazon Web Services S3

* `cluster.backup.s3.prefix`: The AWS S3 bucket and prefix (eg. s3://bucket/path/to/folder).
* `cluster.backup.s3.accessKey.name`: The name of secret with the access key credentials to access AWS S3 for writing and reading.
* `cluster.backup.s3.accessKey.key`: The key in the secret with the access key credentials to access AWS S3 for writing and reading.
* `cluster.backup.s3.secretKey.name`: The name of secret with the secret key credentials to access AWS S3 for writing and reading.
* `cluster.backup.s3.secretKey.key`: The key in the secret with the secret key credentials to access AWS S3 for writing and reading.
* `cluster.backup.s3.region`: The AWS S3 region. Region can be detected using s3:GetBucketLocation, but if you wish to avoid this API call
 or forbid it from the applicable IAM policy, specify this property.
* `cluster.backup.s3.endpoint`: Overrides the default hostname to connect to an S3-compatible service. i.e, http://s3-like-service:9000.
* `cluster.backup.s3.forcePathStyle`: To enable path-style addressing(i.e., http://s3.amazonaws.com/BUCKET/KEY) when connecting to an S3-compatible
 service that lack of support for sub-domain style bucket URLs (i.e., http://BUCKET.s3.amazonaws.com/KEY). Defaults to false.
* `cluster.backup.s3.storageClass`: By default, the "STANDARD" storage class is used. Other supported values include "STANDARD_IA"
 for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced Redundancy.
* `cluster.backup.s3.sse`: To enable S3 server-side encryption, set to the algorithm to use when storing the objects in S3 (i.e., AES256, aws:kms).
* `cluster.backup.s3.sseKmsId`: If using S3 server-side encryption with aws:kms, the KMS Key ID to use for object encryption.
* `cluster.backup.s3.cseKmsId`: To configure AWS KMS key for client side encryption and decryption. By default, no encryption is used.
 (region or cseKmsRegion required to be set when using AWS KMS key client side encryption).
* `cluster.backup.s3.cseKmsRegion`: To configure AWS KMS key region for client side encryption and decryption (i.e., eu-west-1).

##### Google Cloud Storage

* `cluster.backup.gcs.prefix`: Specify where to store backups (eg. gs://x4m-test-bucket/walg-folder).
* `cluster.backup.gcs.serviceAccountJsonKey.name`: The name of secret with service account json key to access GCS for writing and reading.
* `cluster.backup.gcs.serviceAccountJsonKey.key`: The key in the secret with service account json key to access GCS for writing and reading.

##### Azure Blob Storage

* `cluster.backup.azureblob.prefix`: Specify where to store backups in Azure storage (eg. azure://test-container/walg-folder).
* `cluster.backup.azureblob.account.name`: The name of secret with storage account name to access Azure Blob Storage for writing and reading.
* `cluster.backup.azureblob.account.key`: The key in the secret with storage account name to access Azure Blob Storage for writing and reading.
* `cluster.backup.azureblob.accessKey.name`: The name of secret with the primary or secondary access key for the storage account
 to access Azure Blob Storage for writing and reading.
* `cluster.backup.azureblob.accessKey.key`: The key in the secret with the primary or secondary access key for the storage account
 to access Azure Blob Storage for writing and reading.
* `cluster.backup.azureblob.bufferSize`: Overrides the default upload buffer size of 67108864 bytes (64 MB). Note that the size of the buffer
 must be specified in bytes. Therefore, to use 32 MB sized buffers, this variable should be set to 33554432 bytes.
* `cluster.backup.azureblob.maxBuffers`: Overrides the default maximum number of upload buffers. By default, at most 3 buffers are used concurrently.

#### Sidecars

* `sidecar.pooling`: If true enables connection pooling sidecar.
* `sidecar.util`: If true enables util sidecar.
* `sidecar.prometheus.create`: If true enables prometheus exporter sidecar.
* `sidecar.prometheus.allowAutobind`: If true allow autobind prometheus exporter to the available prometheus
 installed using the [prometheus-operator](https://github.com/coreos/prometheus-operator) by creating required
 `ServiceMonitor` custom resources.
