---
title: SGCluster
weight: 1
url: reference/crd/sgcluster
description: Details about SGCluster configurations
showToc: true
---

StackGres PostgreSQL cluster can be created using a cluster Custom Resource (CR) in Kubernetes.

___

**Kind:** SGCluster

**listKind:** SGClusterList

**plural:** sgclusters

**singular:** sgcluster
___

**Spec**

| Property                                                                                   | Required | Updatable | Type     | Default                             | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:---------|:------------------------------------|:-------------------------------------------------------------------|
| instances                                                                                  | ✓        | ✓         | integer  |                                     | {{< crd-field-description SGCluster.spec.instances >}}             |
| [postgres](#postgres)                                                                      |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.postgres >}}              |
| [replication](#replication)                                                                |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.replication >}}           |
| [replicateFrom](#replicate-from)                                                           |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.replication >}}           |
| [sgInstanceProfile]({{% relref "/06-crd-reference/02-sginstanceprofile" %}})               |          | ✓         | string   | will be generated                   | {{< crd-field-description SGCluster.spec.sgInstanceProfile >}}     |
| [metadata](#metadata)                                                                      |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.metadata >}}              |
| [postgresServices](#postgres-services)                                                     |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.postgresServices >}}      |
| [pods](#pods)                                                                              | ✓        | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.pods >}}                  |
| [configurations](#configurations)                                                          |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.configurations >}}        |
| prometheusAutobind                                                                         |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.prometheusAutobind >}}    |
| [initialData](#initial-data)                                                               |          |           | object   |                                     | {{< crd-field-description SGCluster.spec.initialData >}}           |
| [managedSql](#managed-sql)                                                                 |          |           | object   |                                     | {{< crd-field-description SGCluster.spec.managedSql >}}            |
| [distributedLogs](#distributed-logs)                                                       |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.distributedLogs >}}       |
| [nonProductionOptions](#non-production-options)                                            |          | ✓         | array    |                                     | {{< crd-field-description SGCluster.spec.nonProductionOptions >}}  |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  instances: 1
  postgres:
    version: 'latest'
  pods:
    persistentVolume:
      size: '5Gi'
  sgInstanceProfile: 'size-xs'
```

## Postgres

| Property                            | Required | Updatable | Type     | Default  | Description |
|:------------------------------------|----------|-----------|:---------|:---------|:------------|
| version                             | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.version >}}       |
| flavor                              |          |           | string   |          | {{< crd-field-description SGCluster.spec.postgres.flavor >}}       |
| [extensions](#postgres-extensions)  |          | ✓         | array    |          | {{< crd-field-description SGCluster.spec.postgres.extensions >}}    |
| [ssl](#postgres-ssl)                |          | ✓         | object   |          | {{< crd-field-description SGCluster.spec.postgres.ssl >}} |

## Postgres extensions

Extensions to be installed in the cluster.

| Property         | Required | Updatable | Type     | Default           | Description |
|:-----------------|----------|-----------|:---------|:------------------|:------------|
| name             | ✓        | ✓         | string   |                   | {{< crd-field-description SGCluster.spec.postgres.extensions.items.name >}} |
| version          |          | ✓         | string   | stable            | {{< crd-field-description SGCluster.spec.postgres.extensions.items.version >}} |
| publisher        |          | ✓         | string   | com.ongres        | {{< crd-field-description SGCluster.spec.postgres.extensions.items.publisher >}} |
| repository       |          | ✓         | string   |                   | {{< crd-field-description SGCluster.spec.postgres.extensions.items.repository >}} |

Example:

``` yaml

apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  postgres:
    extensions:
      - {name: 'timescaledb', version: '2.3.1'}
```

### Postgres SSL

By default, support for SSL connections to Postgres is disabled, to enable it configure this section. SSL connections will
 be handled by Envoy using [Postgres filter's SSL termination](https://github.com/envoyproxy/envoy/issues/10942).

| Property                   | Required | Updatable | Type     | Default  | Description |
|:---------------------------|----------|-----------|:---------|:---------|:------------|
| enabled                    |          |           | string   | false    | {{< crd-field-description SGCluster.spec.postgres.ssl.enabled >}} |
| [certificateSecretKeySelector](#ssl-certificate-secret) |          |           | object   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.certificateSecretKeySelector >}} |
| [privateKeySecretKeySelector](#ssl-private-key-secret)  |          |           | object   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.privateKeySecretKeySelector >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  postgres:
    ssl:
      enabled: true
      certificateSecretKeySelector:
        name: stackgres-secrets
        key: cert
      secretKeyRef:
        name: stackgres-secrets
        key: key
```

#### SSL Certificate Secret

| Property  | Required | Updatable | Type     | Default  | Description |
|:----------|----------|-----------|:---------|:---------|:------------|
| name      | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.certificateSecretKeySelector.name >}} |
| key       | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.certificateSecretKeySelector.key >}} |

#### SSL Private Key Secret

| Property  | Required | Updatable | Type     | Default  | Description |
|:----------|----------|-----------|:---------|:---------|:------------|
| name      | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.privateKeySecretKeySelector.name >}} |
| key       | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.privateKeySecretKeySelector.key >}} |

## Replication

| Property                            | Required | Updatable | Type     | Default  | Description |
|:------------------------------------|----------|-----------|:---------|:---------|:------------|
| mode                                |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replication.mode >}}       |
| role                                |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replication.role >}}       |
| syncInstances                       |          | ✓         | integer  |          | {{< crd-field-description SGCluster.spec.replication.syncInstances >}} |
| [groups](#replication-group)        |          | ✓         | array    |          | {{< crd-field-description SGCluster.spec.replication.groups >}}     |

## Replication group

| Property                            | Required | Updatable | Type     | Default  | Description |
|:------------------------------------|----------|-----------|:---------|:---------|:------------|
| name                                |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replication.groups.items.name >}}       |
| role                                |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replication.groups.items.role >}}       |
| instances                           |          | ✓         | integer  |          | {{< crd-field-description SGCluster.spec.replication.groups.items.instances >}}  |

## Replicate From

| Property                             | Required | Updatable | Type     | Default  | Description |
|:-------------------------------------|----------|-----------|:---------|:---------|:------------|
| [instance](#replicate-from-instance) |          | ✓         | object   |          | {{< crd-field-description SGCluster.spec.replicateFrom.instance >}}       |
| [storage](#replicate-from-storage)   |          | ✓         | object   |          | {{< crd-field-description SGCluster.spec.replicateFrom.storage >}}       |
| [users](#replicate-from-users)       |          | ✓         | object   |          | {{< crd-field-description SGCluster.spec.replicateFrom.users >}}       |

### Replicate From Instance

| Property                             | Required | Updatable | Type     | Default  | Description |
|:-------------------------------------|----------|-----------|:---------|:---------|:------------|
| sgCluster                            |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replicateFrom.instance.sgCluster >}}       |
| [external](#replicate-from-external) |          | ✓         | object   |          | {{< crd-field-description SGCluster.spec.replicateFrom.instance.external >}}       |

### Replicate From External

| Property                            | Required | Updatable | Type     | Default  | Description |
|:------------------------------------|----------|-----------|:---------|:---------|:------------|
| host                                | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replicateFrom.instance.external.host >}}       |
| port                                | ✓        | ✓         | integer  |          | {{< crd-field-description SGCluster.spec.replicateFrom.instance.external.port >}}       |

### Replicate From Storage

| Property                                           | Required | Updatable | Type     | Default  | Description |
|:---------------------------------------------------|----------|-----------|:---------|:---------|:------------|
| path                                               | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replicateFrom.storage.path >}}       |
| sgObjectStorage                                    | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replicateFrom.storage.sgObjectStorage >}}       |
| [performance](#replicate-from-storage-performance) |          | ✓         | object   |          | {{< crd-field-description SGCluster.spec.replicateFrom.storage.performance >}}       |

### Replicate From Storage Performance

| Property            | Required | Updatable | Type     | Default  | Description |
|:--------------------|----------|-----------|:---------|:---------|:------------|
| downloadConcurrency |          | ✓         | integer  |          | {{< crd-field-description SGCluster.spec.replicateFrom.storage.performance.downloadConcurrency >}}       |
| maxDiskBandwidth    |          | ✓         | integer  |          | {{< crd-field-description SGCluster.spec.replicateFrom.storage.performance.maxDiskBandwidth >}}       |
| maxNetworkBandwidth |          | ✓         | integer  |          | {{< crd-field-description SGCluster.spec.replicateFrom.storage.performance.maxNetworkBandwidth >}}       |

### Replicate From Users

| Property      | Required | Updatable | Type     | Default  | Description |
|:--------------|----------|-----------|:---------|:---------|:------------|
| superuser     | ✓        | ✓         | object   |          | {{< crd-field-description SGCluster.spec.replicateFrom.users.superuser >}}       |
| replication   | ✓        | ✓         | object   |          | {{< crd-field-description SGCluster.spec.replicateFrom.users.replication >}}       |
| authenticator | ✓        | ✓         | object   |          | {{< crd-field-description SGCluster.spec.replicateFrom.users.authenticator >}}       |

### Metadata

Holds custom metadata information for StackGres generated resources to have.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| [annotations](#annotations)   |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations >}} |
| [labels](#labels)             |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.labels >}} |

### Annotations

Holds custom annotations for StackGres generated resources to have.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| allResources                  |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.allResources >}} |
| clusterPods                   |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.clusterPods >}} |
| services                      |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.services >}} |
| primaryService               |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.primaryService >}} |
| replicasService              |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.replicasService >}} |

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    annotations:
      clusterPods:
        customAnnotations: customAnnotationValue
      primaryService:
        customAnnotations: customAnnotationValue
      replicasService:
        customAnnotations: customAnnotationValue
```

### Labels

Holds custom labels for StackGres generated resources to have.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| clusterPods                   |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.labels.clusterPods >}} |

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    labels:
      clusterPods:
        customLabel: customLabelValue
```

## Postgres Services

Specifies the service configuration for the cluster:

| Property                            | Required | Updatable | Type     | Default                              | Description                                                            |
|:------------------------------------|----------|-----------|:---------|:-------------------------------------|:-----------------------------------------------------------------------|
| [Primary](#primary-service-type)    |          | ✓         | object   | [primary](#primary-service-type)   | {{< crd-field-description SGCluster.spec.postgresServices.primary >}}  |
| [Replicas](#replicas-service-type)  |          | ✓         | object   | [replicas](#replicas-service-type) | {{< crd-field-description SGCluster.spec.postgresServices.replicas >}} |

### Primary service type

| Property                        | Required | Updatable | Type     | Default   | Description                                                                 |
|:--------------------------------|----------|-----------|:---------|:----------|:----------------------------------------------------------------------------|
| enabled                         |          | ✓         | boolean  | true      | {{< crd-field-description SGCluster.spec.postgresServices.primary.enabled >}}  |
| type                            |          | ✓         | string   | ClusterIP | {{< crd-field-description SGCluster.spec.postgresServices.primary.type >}}  |
| externalIPs                     |          | ✓         | array    |           | {{< crd-field-description SGCluster.spec.postgresServices.primary.externalIPs >}}  |
| loadBalancerIP                  |          | ✓         | string   |           | {{< crd-field-description SGCluster.spec.postgresServices.primary.loadBalancerIP >}}  |
| customPorts                     |          | ✓         | array    |           | {{< crd-field-description SGCluster.spec.postgresServices.primary.customPorts >}}  |

### Replicas service type

| Property                        | Required | Updatable | Type     | Default   | Description                                                                 |
|:--------------------------------|----------|-----------|:---------|:----------|:----------------------------------------------------------------------------|
| enabled                         |          | ✓         | boolean  | true      | {{< crd-field-description SGCluster.spec.postgresServices.replicas.enabled >}}  |
| type                            |          | ✓         | string   | ClusterIP | {{< crd-field-description SGCluster.spec.postgresServices.replicas.type >}}  |
| externalIPs                     |          | ✓         | array    |           | {{< crd-field-description SGCluster.spec.postgresServices.replicas.externalIPs >}}  |
| loadBalancerIP                  |          | ✓         | string   |           | {{< crd-field-description SGCluster.spec.postgresServices.replicas.loadBalancerIP >}}  |
| customPorts                     |          | ✓         | array    |           | {{< crd-field-description SGCluster.spec.postgresServices.replicas.customPorts >}}  |

## Pods

Cluster's pod configuration

| Property                               | Required | Updatable | Type     | Default                             | Description |
|:---------------------------------------|----------|-----------|:---------|:------------------------------------|:------------|
| [persistentVolume](#persistent-volume) | ✓        | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.pods.persistentVolume >}} |
| disableConnectionPooling               |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disableConnectionPooling >}} |
| disableMetricsExporter                 |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disableMetricsExporter >}} |
| disablePostgresUtil                    |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disablePostgresUtil >}} |
| [scheduling](#scheduling)              |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.pods.scheduling >}} |
| managementPolicy                       |          | ✓         | string   | OrderedReady                        | {{< crd-field-description SGCluster.spec.pods.managementPolicy >}} |
| customVolumes                          |          | ✓         | array    |                                     | {{< crd-field-description SGCluster.spec.pods.customVolumes >}}  |
| customInitContainers                   |          | ✓         | array    |                                     | {{< crd-field-description SGCluster.spec.pods.customInitContainers >}}  |
| customContainers                       |          | ✓         | array    |                                     | {{< crd-field-description SGCluster.spec.pods.customContainers >}}  |

### Sidecar containers

A sidecar container is a container that adds functionality to PostgreSQL or to the cluster
 infrastructure. Currently StackGres implement following sidecar containers:

* `cluster-controller`: this container is always present, and it is not possible to disable it. It serve to reconcile
 local configurations, collect Pod status and perform local actions (like extensions installation, execution of SGScript entries and so on).
* `envoy`: this container is always present, and it is not possible to disable it. It serve as
 a edge proxy from client to PostgreSQL instances or between PostgreSQL instances. It enables
 network metrics collection to provide connection statistics.
* `pgbouncer`: a container with pgbouncer as the connection pooling for the PostgreSQL instances.
* `prometheus-postgres-exporter`: a container with postgres exporter that exports metrics for
 the PostgreSQL instances.
* `fluent-bit`: a container with fluent-bit that send logs to a distributed logs cluster.
* `postgres-util`: a container with psql and all PostgreSQL common tools in order to connect to the
 database directly as root to perform any administration tasks.

The following example, disable all optional sidecars:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    disableConnectionPooling: false
    disableMetricsExporter: false
    disablePostgresUtil: false
```

### Persistent Volume

Holds the configurations of the persistent volume that the cluster pods are going to use.

| Property     | Required | Updatable | Type     | Default                             | Description |
|:-------------|----------|-----------|:---------|:------------------------------------|:------------|
| size         | ✓        | ✓         | string   |                                     | {{< crd-field-description SGCluster.spec.pods.persistentVolume.size >}} |
| storageClass |          | ✓         | string   | default storage class               | {{< crd-field-description SGCluster.spec.pods.persistentVolume.storageClass >}} |

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    persistentVolume:
      size: '5Gi'
      storageClass: default
```

### Scheduling

Holds scheduling configuration for StackGres pods to have.

| Property                    | Required | Updatable | Type     | Default        | Description |
|:----------------------------|----------|-----------|:---------|:---------------|:------------|
| nodeSelector                |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.pods.scheduling.nodeSelector >}} |
| tolerations                 |          | ✓         | array    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.tolerations >}} |
| nodeAffinity                |          | ✓         | object    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.nodeAffinity >}} |
| podAffinity                 |          | ✓         | object    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.podAffinity >}} |
| podAntiAffinity             |          | ✓         | object    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.podAntiAffinity >}} |
| topologySpreadConstraints   |          | ✓         | array    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.podAntiAffinity >}} |
| [backup](#backup)           |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup >}} |

#### Backup

Holds scheduling configuration for StackGres Backups pods to have.

| Property                    | Required | Updatable | Type     | Default        | Description |
|:----------------------------|----------|-----------|:---------|:---------------|:------------|
| nodeSelector                |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup.nodeSelector >}} |
| tolerations                 |          | ✓         | array    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup.tolerations >}} |
| nodeAffinity                |          | ✓         | object    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup.nodeAffinity >}} |
| podAffinity                 |          | ✓         | object    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup.podAffinity >}} |
| podAntiAffinity             |          | ✓         | object    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup.podAntiAffinity >}} |

## Configurations

Custom configurations to be applied to the cluster.

| Property                                                                  | Required | Updatable | Type     | Default           | Description |
|:--------------------------------------------------------------------------|----------|-----------|:---------|:------------------|:------------|
| [sgPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) |          | ✓         | string   | will be generated | {{< crd-field-description SGCluster.spec.configurations.sgPostgresConfig >}} |
| [sgPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}})   |          | ✓         | string   | will be generated | {{< crd-field-description SGCluster.spec.configurations.sgPoolingConfig >}}  |
| [sgBackupConfig]({{% relref "06-crd-reference/05-sgbackupconfig" %}})     |          | ✓         | string   |                   | {{< crd-field-description SGCluster.spec.configurations.sgBackupConfig >}}   |
| backupPath                                                                |          | ✓         | string   |                   | {{< crd-field-description SGCluster.spec.configurations.backupPath >}}   |
| [backups](#backups)                                                       |          | ✓         | array    |                   | {{< crd-field-description SGCluster.spec.configurations.backups >}}   |

Example:

``` yaml

apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  configurations:
    sgPostgresConfig: 'postgresconf'
    sgPoolingConfig: 'pgbouncerconf'
    backups:
    - sgObjectStorage: 'backupconf'
```

### Backups

| Property                                                                  | Required | Updatable | Type    | Default           | Description |
|:--------------------------------------------------------------------------|----------|-----------|:--------|:------------------|:------------|
| [sgObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}})   |          | ✓         | string  |           | {{< crd-field-description SGCluster.spec.configurations.backups.items.sgObjectStorage >}} |
| path                                                                      |          | ✓         | string  |           | {{< crd-field-description SGCluster.spec.configurations.backups.items.path >}}   |
| retention                                                                 |          | ✓         | integer | 5         | {{< crd-field-description SGCluster.spec.configurations.backups.items.retention >}} |
| cronSchedule                                                              |          | ✓         | string  | 05:00 UTC | {{< crd-field-description SGCluster.spec.configurations.backups.items.cronSchedule >}} |
| compression                                                               |          | ✓         | string  | lz4       | {{< crd-field-description SGCluster.spec.configurations.backups.items.compression >}} |
| [performance](#backups-performance)                                       |          | ✓         | object  |           | {{< crd-field-description SGCluster.spec.configurations.backups.items.performance >}} |

#### Backups Performance

| Property                               | Required | Updatable |Type     | Default                           | Description |
|:---------------------------------------|----------|-----------|:--------|:----------------------------------|:------------|
| maxDiskBandwidth                       |          | ✓         | integer | unlimited                         | {{< crd-field-description SGCluster.spec.configurations.backups.items.performance.maxDiskBandwidth >}} |
| maxNetworkBandwidth                    |          | ✓         | integer | unlimited                         | {{< crd-field-description SGCluster.spec.configurations.backups.items.performance.maxNetworkBandwidth >}} |
| uploadDiskConcurrency                  |          | ✓         | integer | 1                                 | {{< crd-field-description SGCluster.spec.configurations.backups.items.performance.uploadDiskConcurrency >}} |
| uploadConcurrency                      |          | ✓         | integer | 16                                | {{< crd-field-description SGCluster.spec.configurations.backups.items.performance.uploadConcurrency >}} |
| downloadConcurrency                    |          | ✓         | integer | min(10, # of objects to download) | {{< crd-field-description SGCluster.spec.configurations.backups.items.performance.downloadConcurrency >}} |

## Initial Data

Specifies the cluster initialization data configurations

| Property                          | Required | Updatable | Type     | Default | Description |
|:----------------------------------|----------|-----------|:---------|:--------|:------------|
| [restore](#restore) |          |           | object   |         | {{< crd-field-description SGCluster.spec.initialData.restore >}} |

## Restore

By default, stackgres it's creates as an empty database. To create a cluster with data
 from an existent backup, we have the restore options. It works, by simply indicating the
 backup CR UUI that we want to restore.

| Property                                 | Required | Updatable | Type     | Default | Description |
|:-----------------------------------------|----------|-----------|:---------|:--------|:------------|
| [fromBackup](#restore-from-backup) | ✓        |           | object   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup >}} |
| downloadDiskConcurrency                  |          |           | integer  | 1       | {{< crd-field-description SGCluster.spec.initialData.restore.downloadDiskConcurrency >}} |

### Restore From Backup

| Property                                                 | Required | Updatable | Type     | Default | Description |
|:---------------------------------------------------------|----------|-----------|:---------|:--------|:------------|
| name                                                     | ✓        |           | string   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup.name >}} |
| [pointInTimeRecovery](#restore-from-backup-to-timestamp) |          |           | object   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup.pointInTimeRecovery >}} |

#### Restore From Backup To Timestamp

| Property           | Required | Updatable | Type     | Default | Description |
|:-------------------|----------|-----------|:---------|:--------|:------------|
| restoreToTimestamp |          |           | string   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    restore:
      fromBackup:
        name: stackgres-backup
      downloadDiskConcurrency: 1
```

## Managed SQL

By default, stackgres creates as an empty database. To execute some scripts, we have the managed SQL
 options where you can reference an SGScript contains the script to execute. When this configuration is
 changed scripts are executed ASAP.

| Property                       | Required | Updatable | Type     | Default  | Description |
|:-------------------------------|----------|-----------|:---------|:---------|:------------|
| continueOnSGScriptError        |          | ✓         | boolean  |          | {{< crd-field-description SGCluster.spec.managedSql.continueOnSGScriptError >}} |
| [scripts](managed-sql-scripts) |          | ✓         | boolean  |          | {{< crd-field-description SGCluster.spec.managedSql.scripts >}} |

## Managed SQL Scripts

| Property  | Required | Updatable | Type     | Default     | Description |
|:----------|----------|-----------|:---------|:------------|:------------|
| id        |          |           | integer  | auto-filled  | {{< crd-field-description SGCluster.spec.managedSql.scripts.items.id >}} |
| sgScript  |          | ✓         | string   |             | {{< crd-field-description SGCluster.spec.managedSql.scripts.items.sgScript >}} |

## Distributed logs
Specifies the distributed logs cluster to send logs to:

| Property                                                                     | Required | Updatable | Type     | Default | Description |
|:-----------------------------------------------------------------------------|----------|-----------|:---------|:--------|:------------|
| [sgDistributedLogs]({{% relref "/06-crd-reference/07-sgdistributedlogs" %}}) |          |           | string   |         | {{< crd-field-description SGCluster.spec.distributedLogs.sgDistributedLogs >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  distributedLogs:
    sgDistributedLogs: distributedlogs
```

## Non Production options

The following options should NOT be enabled in a production environment.

| Property                           | Required | Updatable | Type     | Default | Description |
|:-----------------------------------|----------|-----------|:---------|:--------|:------------|
| disableClusterPodAntiAffinity      |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.disableClusterPodAntiAffinity >}} |
| disablePatroniResourceRequirements |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.disablePatroniResourceRequirements >}} |
| disableClusterResourceRequirements |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.disableClusterResourceRequirements >}} |
| enableSetPatroniCpuRequests        |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enableSetPatroniCpuRequests >}} |
| enableSetClusterCpuRequests        |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enableSetClusterCpuRequests >}} |
| enableSetPatroniMemoryRequests     |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enableSetPatroniMemoryRequests >}} |
| enableSetClusterMemoryRequests     |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enableSetClusterMemoryRequests >}} |
| enabledFeatureGates                |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enabledFeatureGates >}} |
