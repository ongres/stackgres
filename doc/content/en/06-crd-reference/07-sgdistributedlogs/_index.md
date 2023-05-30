---
title: SGDistributedLogs
weight: 8
url: reference/crd/sgdistributedlogs
description: Details about SGDistributedLogs
showToc: true
---

The `SGDistributedLogs` custom resource represents a distributed logs cluster.
When a Postgres cluster is configured to use distributed logs, all logs from different sources will be forwarded to the distributed logs cluster.
Under the hood, distributed log cluster use an `SGCluster`.
Therefore, the distributed log cluster can be queried using SQL as well, for example using *postgres-util*.

For more information about distributed log usage, please check out the [Distributed Logs Manual]({{% relref "04-administration-guide/09-distributed-logs" %}}).
___

**Kind:** SGDistributedLogs

**listKind:** SGDistributedLogsList

**plural:** sgdistributedlogs

**singular:** sgdistributedlogs
___

**Spec**

| <div style="width:12rem">Property</div>         | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:6rem">Default</div> | Description |
|:------------------------------------------------|----------|-----------|:---------|:--------|:------------|
| [persistentVolume](#persistent-volume)          | ✓        |           | string   |         | {{< crd-field-description SGDistributedLogs.spec.persistentVolume >}} |
| [postgresServices](#postgres-services)          |          | ✓         | object   |         | {{< crd-field-description SGDistributedLogs.spec.postgresServices >}} |
| [scheduling](#scheduling)                       |          | ✓         | object   |         | {{< crd-field-description SGDistributedLogs.spec.scheduling >}} |
| sgInstanceProfile                               | ✓        | ✓         | string   | generated | {{< crd-field-description SGDistributedLogs.spec.sgInstanceProfile >}} |
| [configurations](#configurations)               | ✓        | ✓         | object   | generated | {{< crd-field-description SGDistributedLogs.spec.configurations >}} |
| [metadata](#metadata)                           |          | ✓         | object   |         | {{< crd-field-description SGDistributedLogs.spec.metadata >}} |
| [nonProductionOptions](#non-production-options) |          | ✓         | array    |         | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  name: distributedlogs
spec:
  persistentVolume:
    size: 10Gi
```

## Persistent volume

| <div style="width:7rem">Property</div>     | Required | Updatable | <div style="width:4rem">Type</div> | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| size                                       | ✓        |           | string   |                              | {{< crd-field-description SGDistributedLogs.spec.persistentVolume.size >}} |
| storageClass                               |          |           | string   | default storage class        | {{< crd-field-description SGDistributedLogs.spec.persistentVolume.storageClass >}} |

## Postgres Services

| Property                            | Required | Updatable | Type     | Default                              | Description                                                            |
|:------------------------------------|----------|-----------|:---------|:-------------------------------------|:-----------------------------------------------------------------------|
| [primary](#primary-service-type)    |          | ✓         | object   | [primary](#primary-service-type)   | {{< crd-field-description SGDistributedLogs.spec.postgresServices.primary >}}  |
| [replicas](#replicas-service-type)  |          | ✓         | object   | [replicas](#replicas-service-type) | {{< crd-field-description SGDistributedLogs.spec.postgresServices.replicas >}} |

### Primary service type

| Property                        | Required | Updatable | Type     | Default   | Description                                                                 |
|:--------------------------------|----------|-----------|:---------|:----------|:----------------------------------------------------------------------------|
| type                            |          | ✓         | string   | ClusterIP | {{< crd-field-description SGDistributedLogs.spec.postgresServices.primary.type >}}  |
| loadBalancerIP                  |          | ✓         | string   |  | {{< crd-field-description SGDistributedLogs.spec.postgresServices.primary.loadBalancerIP >}}  |
| annotations                     |          | ✓         | object   |           | {{< crd-field-description SGDistributedLogs.spec.postgresServices.primary.annotations >}}  |

### Replicas service type

| Property                        | Required | Updatable | Type     | Default   | Description                                                                 |
|:--------------------------------|----------|-----------|:---------|:----------|:----------------------------------------------------------------------------|
| enabled                         |          | ✓         | boolean  | true      | {{< crd-field-description SGDistributedLogs.spec.postgresServices.replicas.enabled >}}  |
| type                            |          | ✓         | string   | ClusterIP | {{< crd-field-description SGDistributedLogs.spec.postgresServices.replicas.type >}}  |
| loadBalancerIP                  |          | ✓         | string   |  | {{< crd-field-description SGDistributedLogs.spec.postgresServices.replicas.loadBalancerIP >}}  |
| annotations                     |          | ✓         | object   |           | {{< crd-field-description SGDistributedLogs.spec.postgresServices.replicas.annotations >}}  |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  name: stackgres
spec:
  postgresServices:
    primary:
      type: ClusterIP
    replicas:
      enabled: true
      type: ClusterIP
```

### Scheduling

Defines scheduling configuration for StackGres pods.

| <div style="width:8rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:--------|:------------|
| nodeSelector                           |          | ✓         | object                             |         | {{< crd-field-description SGDistributedLogs.spec.scheduling.nodeSelector >}} |
| tolerations                            |          | ✓         | array                              |         | {{< crd-field-description SGDistributedLogs.spec.scheduling.tolerations >}} |
| nodeAffinity                           |          | ✓         | object                             |         | {{< crd-field-description SGDistributedLogs.spec.scheduling.nodeAffinity >}} |
| priorityClassName                      |          | ✓         | string                             |         | {{< crd-field-description SGDistributedLogs.spec.scheduling.priorityClassName >}} |
| podAffinity                            |          | ✓         | object                             |         | {{< crd-field-description SGDistributedLogs.spec.scheduling.podAffinity >}} |
| podAntiAffinity                        |          | ✓         | object                             |         | {{< crd-field-description SGDistributedLogs.spec.scheduling.podAntiAffinity >}} |

### Configurations

| <div style="width:9rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default        | Description |
|:---------------------------------------|----------|-----------|:---------|:---------------|:------------|
| sgPostgresConfig                       | ✓        | ✓         | string   |                | {{< crd-field-description SGDistributedLogs.spec.configurations.sgPostgresConfig >}} |

### Metadata

Defines custom metadata for StackGres generated Kubernetes resources.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| [annotations](#annotations)   |          | ✓         | object   |                | {{< crd-field-description SGDistributedLogs.spec.metadata.annotations >}} |

### Annotations

Defines custom annotations for StackGres generated Kubernetes resources.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| allResources                  |          | ✓         | object   |                | {{< crd-field-description SGDistributedLogs.spec.metadata.annotations.allResources >}} |
| pods                          |          | ✓         | object   |                | {{< crd-field-description SGDistributedLogs.spec.metadata.annotations.pods >}} |
| services                      |          | ✓         | object   |                | {{< crd-field-description SGDistributedLogs.spec.metadata.annotations.services >}} |

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  name: stackgres
spec:
  pods:
    metadata:
      annotations:
        allResources:
          customAnnotations: customAnnotationValue
```

## Non Production options

The following options should NOT be enabled in a production environment.

| <div style="width:19rem">Property</div> | Re&shy;quired | Up&shy;datable | <div style="width:5rem">Type</div> | <div style="width:4rem">Default</div> | Description |
|:----------------------------------------|---------------|----------------|:---------|:--------|:------------|
| disableClusterPodAntiAffinity           |               | ✓              | boolean  | false   | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions.disableClusterPodAntiAffinity >}} |
| disablePatroniResourceRequirements      |               | ✓              | boolean  | false   | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions.disablePatroniResourceRequirements >}} |
| disableClusterResourceRequirements      |               | ✓              | boolean  | false   | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions.disableClusterResourceRequirements >}} |
| enableSetPatroniCpuRequests             |               | ✓              | boolean  | false   | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions.enableSetPatroniCpuRequests >}} |
| enableSetClusterCpuRequests             |               | ✓              | boolean  | false   | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions.enableSetClusterCpuRequests >}} |
| enableSetPatroniMemoryRequests          |               | ✓              | boolean  | false   | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions.enableSetPatroniMemoryRequests >}} |
| enableSetClusterMemoryRequests          |               | ✓              | boolean  | false   | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions.enableSetClusterMemoryRequests >}} |
