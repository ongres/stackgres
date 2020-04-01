---
title: Installation via Helm
weight: 3
---

StackGres operator and clusters can be installed using [helm](https://helm.sh/) version >= `3.1.1`.

## Install Operator

Create stackgres namespace if doesn't exists already

``` shell
kubectl create namespace stackgres
```

Install the operator with the following command:

```shell
helm install --namespace stackgres --name stackgres-operator \
  --values my-operator-values.yml \
  https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm-operator.tgz
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
* `prometheus-operator.create`: Create a prometheus operator and embed his grafana with StackGres
 by setting this to true. Default false.
* `service.loadBalancer.enabled`: Create a load balancer (if supported by the kubernetes cluster)
 to allow connect from Internet to the UI. Note that enabling this feature will probably incurr in
 some fee that depend on the host of the kubernetes cluster (for example this is true for EKS, GKE
 and AKS). Default false.
* `service.loadBalancer.loadBalancerIP`: LoadBalancer will get created with the IP specified in
 this field. This feature depends on whether the underlying cloud-provider supports specifying
 the loadBalancerIP when a load balancer is created. This field will be ignored if the
 cloud-provider does not support the feature.
* `service.loadBalancer.loadBalancerSourceRanges`: If specified and supported by the platform,
 this will restrict traffic through the cloud-provider load-balancer will be restricted to the
 specified client IPs. This field will be ignored if the cloud-provider does not support the
 feature.
 More info: https://kubernetes.io/docs/tasks/access-application-cluster/configure-cloud-provider-firewall/
* `authentication.user`: Username that will be required to access the UI. Default admin.
* `authentication.password`: Password that will be required to access the UI. Default st4ckgr3s.
* `grafana.autoEmbed`: Embed an existing grafana by setting grafana.autoEmbed to true
* `grafana.schema`: the schema to access grafana. By default http. (used to embed manually and
 automatically grafana)
* `grafana.webHost`: the service host name to access grafana (used to embed manually and
 automatically grafana)
* `grafana.user`: the username to access grafana. By default admin. (used to embed automatically
 grafana)
* `grafana.password`: the password to access grafana. By default prom-operator (the default in for
 stable/prometheus-operator helm chart). (used to embed automatically grafana)
* `grafana.secretNamespace`: the namespace of secret with credentials to access grafana. (used to
 embed automatically grafana, alternative to use `grafana.user` and `grafana.password`)
* `grafana.secretName`: the name of secret with credentials to access grafana. (used to embed
 automatically grafana, alternative to use `grafana.user` and `grafana.password`)
* `grafana.secretUserKey`: the key of secret with username used to access grafana. (used to embed
 automatically grafana, alternative to use `grafana.user` and `grafana.password`)
* `grafana.secretPasswordKey`: the key of secret with password used to access grafana. (used to
 embed automatically grafana, alternative to use `grafana.user` and `grafana.password`)
* `grafana.datasourceName`: the datasource name used by dashboard that will be created in grafana.
 By default Prometheus. (used to embed automatically grafana)
* `grafana.dashboardId`: the dashboard id that will be create in grafana
 (see https://grafana.com/grafana/dashboards). By default 9628. (used to embed automatically
 grafana)
* `grafana.url`: the URL of the PostgreSQL dashboard created in grafana (used to embed manually
 grafana)
* `grafana.token`: the grafana API token to access the PostgreSQL dashboard created
 in grafana (used to embed manually grafana)

## Create a Cluster

To install the operator use the following command:

```shell
helm install --namespace my-namespace --name my-cluster \
  --values my-cluster-values.yml \
  https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm-cluster.tgz
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
* `cluster.restore`: The cluster restore options . Is not enabled by default, so if you want to
 create a cluster from a existent backup, please see
 [the restore configuration options](####restore-configuration)
* `cluster.backupconfig`: The backup configuration CR name.
* `cluster.size`: The size set in the persistent volume claim of PostgreSQL data.
* `cluster.storageclass`: The storage class used for the persisitent volume claim of PostgreSQL
 data. If defined, storageClassName: <storageClass>. If set to "-", storageClassName: "", which
 disables dynamic provisioning. If undefined (the default) or set to null, no storageClassName
 spec is set, choosing the default provisioner. (gp2 on AWS, standard on GKE, AWS & OpenStack).
 
#### Backups

By default the chart create a storage class backed by an MinIO server. To avoid the creation of the
 MinIO server set `config.backup.minio.create` to `false` and fill any of the `config.backup.s3`,
  `config.backup.gcs` or `config.backup.azureblob` sections.
 
* `config.backup.create`: If true create and set the backup configuration for the cluster.
* `config.backup.retention`: Retains specified number of full backups. Default is 5.
* `config.backup.fullSchedule`: Specify when to perform full backups using cron syntax:
 <minute: 0 to 59, or *> <hour: 0 to 23, or * for any value. All times UTC> <day of the month: 1 to
 31, or *> <month: 1 to 12, or *> <day of the week: 0 to 7 (0 and 7 both represent Sunday), or *>.
 If not specified full backups will be performed each 2 minutes.
* `config.backup.fullWindow`: Specify the time window in minutes where a full backup will start
 happening after the point in time specified by fullSchedule. If for some reason the system is not
 capable to start the full backup it will be skipped. If not specified the window will be of 1 hour.
* `config.backup.compressionMethod`: To configure compression method used for backups. Possible
 options are: lz4, lzma, brotli. Default method is lz4. LZ4 is the fastest method, but compression
 ratio is bad. LZMA is way much slower, however it compresses backups about 6 times better than LZ4.
 Brotli is a good trade-off between speed and compression ratio which is about 3 times better than
 LZ4.
* `config.backup.uploadDiskConcurrency`: To configure how many concurrency streams are reading
 disk during uploads. By default 1 stream.
* `config.backup.tarSizeThreshold`: To configure the size of one backup bundle (in bytes). Smaller
 size causes granularity and more optimal, faster recovering. It also increases the number of
 storage requests, so it can costs you much money. Default size is 1 GB (1 << 30 - 1 bytes).
* `config.backup.networkRateLimit`: To configure the network upload rate limit during uploads in
 bytes per second.
* `config.backup.diskRateLimit`: To configure disk read rate limit during uploads in bytes per
 second.
* `config.backup.minio.create`: If true create a MinIO server that will be used to store backups.

##### Amazon Web Services S3

* `config.backup.s3.bucket`: The AWS S3 bucket (eg. bucket).
* `config.backup.s3.path`: The AWS S3 bucket path (eg. /path/to/folder).
* `config.backup.s3.accessKey.name`: The name of secret with the access key credentials to access
 AWS S3 for writing and reading.
* `config.backup.s3.accessKey.key`: The key in the secret with the access key credentials to access
 AWS S3 for writing and reading.
* `config.backup.s3.secretKey.name`: The name of secret with the secret key credentials to access
 AWS S3 for writing and reading.
* `config.backup.s3.secretKey.key`: The key in the secret with the secret key credentials to access
 AWS S3 for writing and reading.
* `config.backup.s3.region`: The AWS S3 region. Region can be detected using s3:GetBucketLocation,
 but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this
 property.
* `config.backup.s3.storageClass`: By default, the "STANDARD" storage class is used. Other
 supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced
 Redundancy.

##### Amazon Web Services S3 Compatible

* `config.backup.s3Compatible.bucket`: The AWS S3 bucket (eg. bucket).
* `config.backup.s3Compatible.path`: The AWS S3 bucket path (eg. /path/to/folder).
* `config.backup.s3Compatible.accessKey.name`: The name of secret with the access key credentials to access
 AWS S3 for writing and reading.
* `config.backup.s3Compatible.accessKey.key`: The key in the secret with the access key credentials to access
 AWS S3 for writing and reading.
* `config.backup.s3Compatible.secretKey.name`: The name of secret with the secret key credentials to access
 AWS S3 for writing and reading.
* `config.backup.s3Compatible.secretKey.key`: The key in the secret with the secret key credentials to access
 AWS S3 for writing and reading.
* `config.backup.s3Compatible.region`: The AWS S3 region. Region can be detected using s3:GetBucketLocation,
 but if you wish to avoid this API call or forbid it from the applicable IAM policy, specify this
 property.
* `config.backup.s3Compatible.endpoint`: Overrides the default hostname to connect to an S3-compatible
 service. i.e, http://s3-like-service:9000.
* `config.backup.s3Compatible.forcePathStyle`: To enable path-style addressing(i.e.,
 http://s3.amazonaws.com/BUCKET/KEY) when connecting to an S3-compatible service that lack of
 support for sub-domain style bucket URLs (i.e., http://BUCKET.s3.amazonaws.com/KEY). Defaults to
 false.
* `config.backup.s3Compatible.storageClass`: By default, the "STANDARD" storage class is used. Other
 supported values include "STANDARD_IA" for Infrequent Access and "REDUCED_REDUNDANCY" for Reduced
 Redundancy.

##### Google Cloud Storage

* `config.backup.gcs.bucket`: Specify bucket where to store backups (eg. x4m-test-bucket).
* `config.backup.gcs.path`: Specify bucket path where to store backups (eg. /walg-folder).
* `config.backup.gcs.serviceAccountJsonKey.name`: The name of secret with service account json key
 to access GCS for writing and reading.
* `config.backup.gcs.serviceAccountJsonKey.key`: The key in the secret with service account json key
 to access GCS for writing and reading.

##### Azure Blob Storage

* `config.backup.azureblob.bucket`: Specify bucket where to store backups in Azure storage (eg.
 test-container).
* `config.backup.azureblob.path`: Specify bucket path where to store backups in Azure storage (eg.
 /walg-folder).
* `config.backup.azureblob.account.name`: The name of secret with storage account name to access
 Azure Blob Storage for writing and reading.
* `config.backup.azureblob.account.key`: The key in the secret with storage account name to access
 Azure Blob Storage for writing and reading.
* `config.backup.azureblob.accessKey.name`: The name of secret with the primary or secondary access
 key for the storage account to access Azure Blob Storage for writing and reading.
* `config.backup.azureblob.accessKey.key`: The key in the secret with the primary or secondary
 access key for the storage account to access Azure Blob Storage for writing and reading.

#### Restore configuration

By default, stackgres it's creates as an empty database. To create a cluster with data from an
 existent backup, we have the restore options. It works, by simply indicating the backup CR Uid
 that we want to restore. 

* `cluster.restore.fromBackup`: The backup CR UID to restore the cluster data
* `config.restore.downloadDiskConcurrency`: By default 1. How many concurrent stream will create
 while downloading the backup.

#### Sidecars

* `sidecar.pooling`: If true enables connection pooling sidecar.
* `sidecar.util`: If true enables util sidecar.
* `sidecar.prometheus.create`: If true enables prometheus exporter sidecar.
* `sidecar.prometheus.allowAutobind`: If true allow autobind prometheus exporter to the available
 prometheus installed using the [prometheus-operator](https://github.com/coreos/prometheus-operator)
 by creating required `ServiceMonitor` custom resources.
