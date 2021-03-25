---
title: SGDistributedLogs
weight: 8
url: reference/crd/sgdistributedlogs
description: Details about SGDistributedLogs configurations
---

## Creating a distributed logs cluster

The distributed logs CR represent a distributed logs cluster. When a cluster is configured to use a
 distributed logs cluster it will forward all logs from different sources to the distributed logs cluster.
Under the hood, distributed log cluster use a SGCluster, therefore the distributed log cluster could be receive SQL queries in a *postgres-util* fashion but not with it.

For more information about distributed log usage please review the [Distributed Log Cluster Administration Guide](/administration/cluster/distributedlogs/)
___

**Kind:** SGDistributedLogs

**listKind:** SGDistributedLogsList

**plural:** sgdistributedlogs

**singular:** sgdistributedlogs
___

**Spec**

| Property                                        | Required | Updatable | Type     | Default | Description |
|:------------------------------------------------|----------|-----------|:---------|:--------|:------------|
| [persistentVolume](#persistent-volume)          | ✓        |           | string   |         | {{< crd-field-description SGDistributedLogs.spec.persistentVolume >}} |
| [scheduling](#scheduling)                       |          | ✓         | object   |         | {{< crd-field-description SGDistributedLogs.spec.scheduling >}} |
| [metadata](#metadata)                           |          | ✓         | object   |         | {{< crd-field-description SGDistributedLogs.spec.metadata >}} |
| [nonProductionOptions](#non-production-options) |          | ✓         | array    |         | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions >}} |

## Persistent volume

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| size                                       | ✓        |           | string   |                              | {{< crd-field-description SGDistributedLogs.spec.persistentVolume.size >}} |
| storageClass                               | ✓        |           | string   | default storage class        | {{< crd-field-description SGDistributedLogs.spec.persistentVolume.storageClass >}} |

### Scheduling

Holds scheduling configuration for StackGres pods to have.

| Property                    | Required | Updatable | Type     | Default        | Description |
|:----------------------------|----------|-----------|:---------|:---------------|:------------|
| nodeSelector                |          | ✓         | object   |                | {{< crd-field-description SGDistributedLogs.spec.scheduling.nodeSelector >}} |
| [tolerations](#tolerations) |          | ✓         | array    |                | {{< crd-field-description SGDistributedLogs.spec.scheduling.tolerations >}} |

#### Tolerations

Holds scheduling configuration for StackGres pods to have.

| Property  | Required | Updatable | Type     | Default                 | Description |
|:----------|----------|-----------|:---------|:------------------------|:------------|
| key       |          | ✓         | string   |                         | {{< crd-field-description SGDistributedLogs.spec.scheduling.nodeSelector >}} |
| operator  |          | ✓         | string   | Equal                   | {{< crd-field-description SGDistributedLogs.spec.scheduling.tolerations >}} |
| value     |          | ✓         | string   |                         | {{< crd-field-description SGDistributedLogs.spec.scheduling.tolerations >}} |
| effect    |          | ✓         | string   | match all taint effects | {{< crd-field-description SGDistributedLogs.spec.scheduling.tolerations >}} |

### Metadata

Holds custom metadata information for StackGres generated resources to have.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| [annotations](#annotations)   |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations >}} |

### Annotations

Holds custom annotations for StackGres generated resources to have.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| allResources                  |          | ✓         | object   |                | {{< crd-field-description SGDistributedLogs.spec.metadata.annotations.allResources >}} |
| pods                          |          | ✓         | object   |                | {{< crd-field-description SGDistributedLogs.spec.metadata.annotations.pods >}} |
| services                      |          | ✓         | object   |                | {{< crd-field-description SGDistributedLogs.spec.metadata.annotations.services >}} |

```yaml
apiVersion: stackgres.io/v1beta1
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

| Property                      | Required | Updatable | Type     | Default | Description |
|:------------------------------|----------|-----------|:---------|:--------|:------------|
| disableClusterPodAntiAffinity |          | ✓         | boolean  | false   | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions.disableClusterPodAntiAffinity >}} |

Example:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGDistributedLogs
metadata:
  name: distributedlogs
spec:
  persistentVolume:
    size: 10Gi
```
