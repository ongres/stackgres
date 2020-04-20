---
title: Postgres clusters
weight: 1
---

StackGres PostgreSQL cluster can be created using a cluster Custom Resource (CR) in Kubernetes.

___

**Kind:** SGCluster

**listKind:** SGClusterList

**plural:** sgclusters

**singular:** sgcluster
___

**Spec**

| Property                                                                                   | Required | Updatable | Type     | Default                             | Description |
|:-------------------------------------------------------------------------------------------|----------|-----------|:---------|:------------------------------------|:------------|
| postgresVersion                                                                            | ✓        | ✓         | string   |                                     | {{< crd-field-description SGCluster.spec.postgresVersion >}} |
| instances                                                                                  | ✓        | ✓         | integer  |                                     | {{< crd-field-description SGCluster.spec.instances >}} |
| [sgInstanceProfile]({{% relref "/04-postgres-cluster-management/03-resource-profiles" %}}) |          |           | string   | will be generated                   | {{< crd-field-description SGCluster.spec.sgInstanceProfile >}} |
| [pods](#pods)                                                                              | ✓        | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.pods >}} |
| [configurations](#configurations)                                                          |          |           | object   |                                     | {{< crd-field-description SGCluster.spec.configurations >}} |
| prometheusAutobind                                                                         |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.prometheusAutobind >}} |
| [initialData](#initial-data-configuration)                                                 |          |           | object   |                                     | {{< crd-field-description SGCluster.spec.initialData >}} |
| [nonProductionOptions](#non-production-options)                                            |          | ✓         | array    |                                     | {{< crd-field-description SGCluster.spec.nonProductionOptions >}} |

Example:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  name: stackgres
spec:
  instances: 1
  postgresVersion: 'latest'
  pods:
    persistentVolume:
      size: '5Gi'
  sgInstanceProfile: 'size-xs'
```

## Pods

Cluster's pod configuration

| Property                               | Required | Updatable | Type     | Default                             | Description |
|:---------------------------------------|----------|-----------|:---------|:------------------------------------|:------------|
| [persistentVolume](#persistent-volume) | ✓        |           | object   |                                     | {{< crd-field-description SGCluster.spec.pods.persistentVolume >}} |
| disableConnectionPooling               |          |           | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disableConnectionPooling >}} |
| disableMetricsExporter                 |          |           | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disableMetricsExporter >}} |
| disablePostgresUtil                    |          |           | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disablePostgresUtil >}} |

### Sidecar containers

A sidecar container is a container that adds functionality to PostgreSQL or to the cluster
 infrastructure. Currently StackGres implement following sidecar containers:

* `envoy`: this container is always present, and is not possible to disable it. It serve as
 a edge proxy from client to PostgreSQL instances or between PostgreSQL instances. It enables
 network metrics collection to provide connection statistics.
* `pgbouncer`: a container with pgbouncer as the connection pooling for the PostgreSQL instances.
* `prometheus-postgres-exporter`: a container with postgres exporter at the metrics exporter for
 the PostgreSQL instances.
* `postgres-util`: a container with psql and all PostgreSQL common tools in order to connect to the
 database directly as root to perform any administration tasks.

The following example, disable all optional sidecars:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    disableConnectionPooling: false
    disableMetricsExporter: false
    disablePostgresUtil: false
```

## Persistent Volume

Holds the configurations of the persistent volume that the cluster pods are going to use

| Property     | Required | Updatable | Type     | Default                             | Description |
|:-------------|----------|-----------|:---------|:------------------------------------|:------------|
| size         | ✓        | ✓         | string   |                                     | {{< crd-field-description SGCluster.spec.pods.persistentVolume.size >}} |
| storageClass |          |           | string   | default storage class               | {{< crd-field-description SGCluster.spec.pods.persistentVolume.storageClass >}} |

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    persistentVolume:
      size: '5Gi'
      storageClass: default
```

## Configurations

Custom configurations to be applied to the cluster.

| Property                                                                                                                        | Required | Updatable | Type     | Default           | Description |
|:--------------------------------------------------------------------------------------------------------------------------------|----------|-----------|:---------|:------------------|:------------|
| [sgPostgresConfig]({{% relref "/04-postgres-cluster-management/02-configuration-tuning/02-postgres-configuration" %}})          |          | ✓         | string   | will be generated | {{< crd-field-description SGCluster.spec.configurations.sgPostgresConfig >}} |
| [sgPoolingConfig]({{% relref "/04-postgres-cluster-management/02-configuration-tuning/03-connection-pooling-configuration" %}}) |          | ✓         | string   | will be generated | {{< crd-field-description SGCluster.spec.configurations.sgPoolingConfig >}} |
| [sgBackupConfig]({{% relref "/04-postgres-cluster-management/04-backups/_index.md#configuration" %}})                           |          | ✓         | string   |                   | {{< crd-field-description SGCluster.spec.configurations.sgBackupConfig >}} |

Example: 

``` yaml

apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  name: stackgres
spec:
  configurations:
    sgPostgresConfig: 'postgresconf'
    sgPoolingConfig: 'pgbouncerconf'
    sgBackupConfig: 'backupconf'

```

## Initial Data Configuration
Specifies the cluster initialization data configurations

| Property                          | Required | Updatable | Type     | Default | Description |
|:----------------------------------|----------|-----------|:---------|:--------|:------------|
| [restore](#restore-configuration) |          |           | object   |         | {{< crd-field-description SGCluster.spec.initialData.restore >}} |


## Restore configuration

By default, stackgres it's creates as an empty database. To create a cluster with data 
 from an existent backup, we have the restore options. It works, by simply indicating the 
 backup CR UUI that we want to restore. 

| Property                | Required | Updatable | Type     | Default | Description |
|:------------------------|----------|-----------|:---------|:--------|:------------|
| fromBackup              | ✓        |           | string   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup >}} |
| downloadDiskConcurrency |          |           | integer  | 1       | {{< crd-field-description SGCluster.spec.initialData.restore.downloadDiskConcurrency >}} |

Example:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData: 
    restore:
      fromBackup: d7e660a9-377c-11ea-b04b-0242ac110004
      downloadDiskConcurrency: 1
```

## Non Production options

Following options should be enabled only when NOT working in a production environment.

| Property                      | Required | Updatable | Type     | Default | Description |
|:------------------------------|----------|-----------|:---------|:--------|:------------|
| disableClusterPodAntiAffinity |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.disableClusterPodAntiAffinity >}} |
