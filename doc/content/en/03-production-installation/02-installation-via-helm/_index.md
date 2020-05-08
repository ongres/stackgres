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
helm install --namespace stackgres stackgres-operator \
  --values my-operator-values.yml \
  https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm-operator.tgz
```

### Parameters

You can specify following parameters values:

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `cert.autoapprove` | {{< description stackgres-operator.cert.autoapprove >}} | true |
| `service.loadBalancer.enabled` | {{< description stackgres-operator.service.loadBalancer.enabled >}} | true |
| `service.loadBalancer.loadBalancerIP` | {{< description stackgres-operator.service.loadBalancer.loadBalancerIP >}} |  |
| `service.loadBalancer.loadBalancerSourceRanges` | {{< description stackgres-operator.service.loadBalancer.loadBalancerSourceRanges >}} |  |
| `authentication.user` | {{< description stackgres-operator.authentication.user >}} | admin |
| `authentication.password` | {{< description stackgres-operator.authentication.password >}} | st4ckgr3s |
| `grafana.autoEmbed` | {{< description stackgres-operator.grafana.autoEmbed >}} | true |
| `grafana.schema` | {{< description stackgres-operator.grafana.schema >}} | http |
| `grafana.webHost` | {{< description stackgres-operator.grafana.webHost >}} |  |
| `grafana.user` | {{< description stackgres-operator.grafana.user >}} |  |
| `grafana.password` | {{< description stackgres-operator.grafana.password >}} |  |
| `grafana.secretNamespace` | {{< description stackgres-operator.grafana.secretNamespace >}} |  |
| `grafana.secretName` | {{< description stackgres-operator.grafana.secretName >}} |  |
| `grafana.secretUserKey` | {{< description stackgres-operator.grafana.secretUserKey >}} |  |
| `grafana.secretPasswordKey` | {{< description stackgres-operator.grafana.secretPasswordKey >}} |  |
| `grafana.datasourceName` | {{< description stackgres-operator.grafana.datasourceName >}} | Prometheus |
| `grafana.dashboardId` | {{< description stackgres-operator.grafana.dashboardId >}} | 9628 |
| `grafana.url` | {{< description stackgres-operator.grafana.url >}} |  |
| `grafana.token` | {{< description stackgres-operator.grafana.token >}} |  |
| `prometheus.allowAutobind` | {{< description stackgres-operator.prometheus.allowAutobind >}} | true |
| `prometheus-operator.create` | {{< description stackgres-operator.prometheus-operator.create >}} | false |

## Create a Cluster

To install the operator use the following command:

```shell
helm install --namespace my-namespace my-cluster \
  --values my-cluster-values.yml \
  https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm-cluster.tgz
```

### Parameters

You can specify following parameters values:

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `cluster.create` | {{< description stackgres-cluster.cluster.create >}} | true |
| `cluster.postgresVersion` | {{< crd-field-description SGCluster.spec.postgresVersion >}} | 12.2 |
| `cluster.instances` | {{< crd-field-description SGCluster.spec.instances >}} | 1 |
| `cluster.sgInstanceProfile` | {{< crd-field-description SGCluster.spec.sgInstanceProfile >}} | size-xs |
| `cluster.configurations.sgPostgresConfig` | {{< crd-field-description SGCluster.spec.configurations.sgPostgresConfig >}} | postgresconfig |
| `cluster.configurations.sgPoolingConfig` | {{< crd-field-description SGCluster.spec.configurations.sgPoolingConfig >}} | poolingconfig |
| `cluster.configurations.sgBackupConfig` | {{< crd-field-description SGCluster.spec.configurations.sgBackupConfig >}} | backupconfig |
| `cluster.prometheusAutobind` | {{< crd-field-description SGCluster.spec.prometheusAutobind >}} | true |
| `instanceProfiles` | {{< description stackgres-cluster.instanceProfiles >}} | See [instance profiles](#instance-profiles) |
| `configurations.create` | {{< description stackgres-cluster.configurations.create >}} | true |
| `configurations.postgresconfig` | {{< description stackgres-cluster.configurations.postgresconfig >}} | See [postgres configuration](#postgres-configuration) |
| `configurations.poolingconfig` | {{< description stackgres-cluster.configurations.poolingconfig >}} | See [connection pooling configuration](#connection-pooling-configuration) |
| `configurations.backupconfig` | {{< description stackgres-cluster.configurations.backupconfig.description >}} | See [backup configuration](#backup-configuration) |

#### Pods

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `cluster.pods.persistentVolume.size` | {{< crd-field-description SGCluster.spec.pods.persistentVolume.size >}} | 5Gi |
| `cluster.pods.persistentVolume.storageclass` | {{< crd-field-description SGCluster.spec.pods.persistentVolume.storageClass >}} |  |
| `cluster.pods.disableConnectionPooling` | {{< crd-field-description SGCluster.spec.pods.disableConnectionPooling >}} | false |
| `cluster.pods.disableMetricsExporter` | {{< crd-field-description SGCluster.spec.pods.disableMetricsExporter >}} | false |
| `cluster.pods.disablePostgresUtil` | {{< crd-field-description SGCluster.spec.pods.disablePostgresUtil >}} | false |
| `cluster.pods.metadata.annotations` | {{< crd-field-description SGCluster.spec.metadata.annotations >}} | false |
| `cluster.pods.metadata.labels` | {{< crd-field-description SGCluster.spec.metadata.labels >}} | false |

#### Instance profiles

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `instanceProfiles.<index>.name` | {{< crd-field-description SGInstanceProfile.metadata.name >}} | See below |
| `instanceProfiles.<index>.cpu` | {{< crd-field-description SGInstanceProfile.spec.cpu >}} | See below |
| `instanceProfiles.<index>.memory` | {{< crd-field-description SGInstanceProfile.spec.memory >}} | See below |

By default following profiles are created:

```yaml
instanceProfiles:
  - name: size-xs
    cpu: "500m"
    memory: "512Mi"
  - name: size-s
    cpu: "1"
    memory: "2Gi"
  - name: size-m
    cpu: "2"
    memory: "4Gi"
  - name: size-l
    cpu: "4"
    memory: "8Gi"
  - name: size-xl
    cpu: "6"
    memory: "16Gi"
  - name: size-xxl
    cpu: "8"
    memory: "32Gi"
```

#### Postgres configuration

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `configurations.postgresconfig.postgresql\.conf` | {{< crd-field-description "SGPostgresConfig.spec.postgresql\.conf" >}} | See below |

By default following parameters are specified:

```yaml
configurations:
  postgresconfig:
    postgresql.conf:
      shared_buffers: '256MB'
      random_page_cost: '1.5'
      password_encryption: 'scram-sha-256'
      wal_compression: 'on'
      checkpoint_timeout: '30'
```

#### Connection pooling configuration

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `configurations.poolingconfig.pgBouncer.pgbouncer\.ini` | {{< crd-field-description "SGPoolingConfig.spec.pgBouncer.pgbouncer\.ini" >}} | See below |

By default following parameters are specified:

```yaml
configurations:
  poolingconfig:
    pgBouncer:
      pgbouncer.ini:
        pool_mode: transaction
        max_client_conn: '200'
        default_pool_size: '200'
```

#### Backup configuration

By default the chart create a storage class backed by an MinIO server. To avoid the creation of the
 MinIO server set `nonProductionOptions.createMinio` to `false` and fill any of the `configurations.backupconfig.storage.s3`,
  `configurations.backupconfig.storage.gcs` or `configurations.backupconfig.storage.azureBlob` sections.
 
| Parameter | Description | Default |
|:----------|:------------|:--------|
| `configurations.backupconfig.create` | {{< description stackgres-cluster.configurations.backupconfig.create >}} | true |
| `configurations.backupconfig.baseBackups.retention` | {{< crd-field-description SGBackupConfig.spec.baseBackups.retention >}} | 5 |
| `configurations.backupconfig.baseBackups.cronSchedule` | {{< crd-field-description SGBackupConfig.spec.baseBackups.cronSchedule >}} | Each 2 minutes |
| `configurations.backupconfig.baseBackups.compression` | {{< crd-field-description SGBackupConfig.spec.baseBackups.compression >}} | lz4 |
| `configurations.backupconfig.baseBackups.performance.uploadDiskConcurrency` | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.uploadDiskConcurrency >}} | 1 |
| `configurations.backupconfig.baseBackups.performance.maxNetworkBandwitdh` | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.maxDiskBandwitdh >}} | unlimited |
| `configurations.backupconfig.baseBackups.performance.maxDiskBandwitdh` | {{< crd-field-description SGBackupConfig.spec.baseBackups.performance.maxNetworkBandwitdh >}} | unlimited |

##### Amazon Web Services S3

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `configurations.backupconfig.storage.s3.bucket` | {{< crd-field-description SGBackupConfig.spec.storage.s3.bucket >}} |  |
| `configurations.backupconfig.storage.s3.path` | {{< crd-field-description SGBackupConfig.spec.storage.s3.path >}} |  |
| `configurations.backupconfig.storage.s3.awsCredentials.secretKeySelectors.accessKeyId` | {{< crd-field-description SGBackupConfig.spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId >}} |  |
| `configurations.backupconfig.storage.s3.awsCredentials.secretKeySelectors.accessKeyId.name` | {{< crd-field-description SGBackupConfig.spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId.name >}} |  |
| `configurations.backupconfig.storage.s3.awsCredentials.secretKeySelectors.accessKeyId.key` | {{< crd-field-description SGBackupConfig.spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId.key >}} |  |
| `configurations.backupconfig.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey` | {{< crd-field-description SGBackupConfig.spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey >}} |  |
| `configurations.backupconfig.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey.name` | {{< crd-field-description SGBackupConfig.spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey.name >}} |  |
| `configurations.backupconfig.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey.key` | {{< crd-field-description SGBackupConfig.spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey.key >}} |  |
| `configurations.backupconfig.storage.s3.region` | {{< crd-field-description SGBackupConfig.spec.storage.s3.region >}} |  |
| `configurations.backupconfig.storage.s3.storageClass` | {{< crd-field-description SGBackupConfig.spec.storage.s3.storageClass >}} |  |

##### Amazon Web Services S3 Compatible

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `configurations.backupconfig.storage.s3Compatible.bucket` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.bucket >}} |  |
| `configurations.backupconfig.storage.s3Compatible.path` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.path >}} |  |
| `configurations.backupconfig.storage.s3Compatible.bucket` | The AWS S3 bucket (eg. bucket).
| `configurations.backupconfig.storage.s3Compatible.path` | The AWS S3 bucket path (eg. /path/to/folder).
| `configurations.backupconfig.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId >}} |  |
| `configurations.backupconfig.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.name` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.name >}} |  |
| `configurations.backupconfig.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.key` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.key >}} |  |
| `configurations.backupconfig.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey >}} |  |
| `configurations.backupconfig.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.name` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.name >}} |  |
| `configurations.backupconfig.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.key` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.key >}} |  |
| `configurations.backupconfig.storage.s3Compatible.region` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.region >}} |  |
| `configurations.backupconfig.storage.s3Compatible.storageClass` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.storageClass >}} |  |
| `configurations.backupconfig.storage.s3Compatible.endpoint` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.endpoint >}} |  |
| `configurations.backupconfig.storage.s3Compatible.enablePathStyleAddressing` | {{< crd-field-description SGBackupConfig.spec.storage.s3Compatible.enablePathStyleAddressing >}} |  |

##### Google Cloud Storage

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `configurations.backupconfig.storage.gcs.bucket` | {{< crd-field-description SGBackupConfig.spec.storage.gcs.bucket >}} |  |
| `configurations.backupconfig.storage.gcs.path` | {{< crd-field-description SGBackupConfig.spec.storage.gcs.path >}} |  |
| `configurations.backupconfig.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON` | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON >}} |  |
| `configurations.backupconfig.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON.name` | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON.name >}} |  |
| `configurations.backupconfig.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON.key` | {{< crd-field-description SGBackupConfig.spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON.key >}} |  |

##### Azure Blob Storage

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `configurations.backupconfig.storage.azureBlob.bucket` | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.bucket >}} |  |
| `configurations.backupconfig.storage.azureBlob.path` | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.path >}} |  |
| `configurations.backupconfig.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount` | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount >}} |  |
| `configurations.backupconfig.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount.name` | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount.name >}} |  |
| `configurations.backupconfig.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount.key` | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount.key >}} |  |
| `configurations.backupconfig.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey` | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey >}} |  |
| `configurations.backupconfig.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey.name` | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey.name >}} |  |
| `configurations.backupconfig.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey.key` | {{< crd-field-description SGBackupConfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey.key >}} |  |

#### Restore configuration

By default, stackgres creates as an empty database. To create a cluster with data from an
 existent backup, we have the restore options. It works, by simply indicating the backup CR Uid
 that we want to restore. 

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `cluster.initialData.restore.fromBackup` | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup >}} |  |
| `cluster.initialData.restore.downloadDiskConcurrency` | {{< crd-field-description SGCluster.spec.initialData.restore.downloadDiskConcurrency >}} |  |

#### Distributed logs

By default, stackgres send logs to container stdout. To send logs to a distributed logs create a
 distributed logs cluster and configure the cluster to use it by setting `distributedLogs.enabled`
 to `true`.

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `cluster.distributedLogs.sgDistributedLogs` | {{< crd-field-description SGCluster.spec.distributedLogs.sgDistributedLogs >}} | distributedlogs |
| `distributedLogs.enabled` | {{< description stackgres-cluster.distributedLogs.enabled >}} | false |
| `distributedLogs.create` | {{< description stackgres-cluster.distributedLogs.create >}} | true |
| `distributedLogs.persistentVolume.size` | {{< crd-field-description SGDistributedLogs.spec.persistentVolume.size >}} | 5Gi |
| `distributedLogs.persistentVolume.storageClass` | {{< crd-field-description SGDistributedLogs.spec.persistentVolume.storageClass >}} |  |

#### Non production options

The following options should NOT be enabled in a production environment.

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `nonProductionOptions.disableClusterPodAntiAffinity` | {{< crd-field-description SGCluster.spec.nonProductionOptions.disableClusterPodAntiAffinity >}} | true |
| `nonProductionOptions.createMinio` | {{< description stackgres-cluster.nonProductionOptions.createMinio >}} | true |
