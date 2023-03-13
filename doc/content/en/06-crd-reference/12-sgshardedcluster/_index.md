---
title: SGShardedCluster
weight: 12
url: reference/crd/sgshardedcluster
description: Details about SGShardedCluster
showToc: true
---

StackGres PostgreSQL sharded clusters are created using the `SGShardedCluster` custom resource.

___

**Kind:** SGShardedCluster

**listKind:** SGShardedClusterList

**plural:** sgshardedclusters

**singular:** sgshardedcluster
___

**Spec**

| <div style="width:12rem">Property</div>                                                    | Required | Updatable | <div style="width:5rem">Type</div> | Default           | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:-------------------------------------------------------------------|
| type                                                                                       | ✓        | ✓         | string                             |                   | {{< crd-field-description SGShardedCluster.spec.type >}}    |
| database                                                                                   | ✓        | ✓         | string                             |                   | {{< crd-field-description SGShardedCluster.spec.database >}}    |
| [postgres](#postgres)                                                                      |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.postgres >}}              |
| [coordinator](#coordinator)                                                                | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator >}}              |
| [shards](#shards)                                                                          | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards >}}              |
| prometheusAutobind                                                                         |          | ✓         | boolean                            | false             | {{< crd-field-description SGShardedCluster.spec.prometheusAutobind >}}    |
| [nonProductionOptions](#non-production-options)                                            |          | ✓         | array                              |                   | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions >}}  |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  postgres:
    version: 'latest'
  coordinator:
    instances: 1
    pods:
      persistentVolume:
        size: '5Gi'
  shards:
    clusters: 2
    instancesPerCluster: 1
    pods:
      persistentVolume:
        size: '5Gi'
```

## Postgres

| <div style="width:6rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default  | Description                                                      |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------|:-----------------------------------------------------------------|
| version                                | ✓        | ✓         | string                             |          | {{< crd-field-description SGShardedCluster.spec.postgres.version >}}    |

## Coordinator

| <div style="width:12rem">Property</div>                                                    | Required | Updatable | <div style="width:5rem">Type</div> | Default           | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:-------------------------------------------------------------------|
| instances                                                                                  | ✓        | ✓         | integer                            |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.instances >}}             |
| [sgInstanceProfile]({{% relref "/06-crd-reference/02-sginstanceprofile" %}})               |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.coordinator.sgInstanceProfile >}}     |
| [pods](#pods)                                                                              | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.pods >}}                  |
| [configurations](#configurations)                                                          |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.configurations >}}        |

## Coordinator Pods

Cluster's pod configuration

| <div style="width:14rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div> | <div style="width:4rem">Default</div> | Description |
|:----------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:------------|
| [persistentVolume](#shards-persistent-volume)  | ✓        | ✓         | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.persistentVolume >}} |

### SCoordinatorhards Persistent Volume

Configuration of the persistent volumes that the cluster pods are going to use.

| <div style="width:7rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:11rem">Default</div> | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------------------------------------|:------------|
| size                                   | ✓        | ✓         | string                             |                                        | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.persistentVolume.size >}} |
| storageClass                           |          | ✓         | string                             | default storage class                  | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.persistentVolume.storageClass >}} |

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  pods:
    persistentVolume:
      size: '5Gi'
      storageClass: default
```

## Coordinator Configurations

Custom Postgres configuration.

| <div style="width:9rem">Property</div>                                    | Required | Updatable | <div style="width:4rem">Type</div> | Default           | Description |
|:--------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:------------|
| [sgPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.coordinator.configurations.sgPostgresConfig >}} |
| [sgPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}})   |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.coordinator.configurations.sgPoolingConfig >}}  |

## Shards

| <div style="width:12rem">Property</div>                                                    | Required | Updatable | <div style="width:5rem">Type</div> | Default           | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:-------------------------------------------------------------------|
| clusters                                                                                   | ✓        | ✓         | integer                            |                   | {{< crd-field-description SGShardedCluster.spec.shards.clusters >}}             |
| instancesPerCluster                                                                        | ✓        | ✓         | integer                            |                   | {{< crd-field-description SGShardedCluster.spec.shards.instancesPerCluster >}}             |
| [sgInstanceProfile]({{% relref "/06-crd-reference/02-sginstanceprofile" %}})               |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.shards.sgInstanceProfile >}}     |
| [pods](#shards-pods)                                                                              | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.pods >}}                  |
| [configurations](#shards-configurations)                                                          |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.configurations >}}        |

## Shards Pods

Cluster's pod configuration

| <div style="width:14rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div> | <div style="width:4rem">Default</div> | Description |
|:----------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:------------|
| [persistentVolume](#shards-persistent-volume)  | ✓        | ✓         | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.shards.pods.persistentVolume >}} |

### Shards Persistent Volume

Configuration of the persistent volumes that the cluster pods are going to use.

| <div style="width:7rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:11rem">Default</div> | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------------------------------------|:------------|
| size                                   | ✓        | ✓         | string                             |                                        | {{< crd-field-description SGShardedCluster.spec.shards.pods.persistentVolume.size >}} |
| storageClass                           |          | ✓         | string                             | default storage class                  | {{< crd-field-description SGShardedCluster.spec.shards.pods.persistentVolume.storageClass >}} |

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  pods:
    persistentVolume:
      size: '5Gi'
      storageClass: default
```

## Shards Configurations

Custom Postgres configuration.

| <div style="width:9rem">Property</div>                                    | Required | Updatable | <div style="width:4rem">Type</div> | Default           | Description |
|:--------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:------------|
| [sgPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.shards.configurations.sgPostgresConfig >}} |
| [sgPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}})   |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.shards.configurations.sgPoolingConfig >}}  |

## Non Production Options

The following options should NOT be enabled in a production environment.

| <div style="width:19rem">Property</div> | Re&shy;quired | Up&shy;datable | <div style="width:5rem">Type</div> | <div style="width:4rem">Default</div> | Description |
|:----------------------------------------|---------------|----------------|:-----------------------------------|:--------------------------------------|:------------|
| disableClusterPodAntiAffinity           |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.disableClusterPodAntiAffinity >}} |
| disablePatroniResourceRequirements      |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.disablePatroniResourceRequirements >}} |
| disableClusterResourceRequirements      |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.disableClusterResourceRequirements >}} |
| enableSetPatroniCpuRequests             |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enableSetPatroniCpuRequests >}} |
| enableSetClusterCpuRequests             |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enableSetClusterCpuRequests >}} |
| enableSetPatroniMemoryRequests          |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enableSetPatroniMemoryRequests >}} |
| enableSetClusterMemoryRequests          |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enableSetClusterMemoryRequests >}} |
| enabledFeatureGates                     |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enabledFeatureGates >}} |
