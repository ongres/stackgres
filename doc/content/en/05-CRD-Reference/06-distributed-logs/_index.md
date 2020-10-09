---
title: Distributed logs
weight: 6
url: reference/distributedlogs
---

## Creating a distributed logs cluster

The distributed logs CR represent a distributed logs cluster. When a cluster is configured to use a
 distributed logs cluster it will forward all logs from different sources to the distributed logs cluster.
Behind the hood, distributed log cluster use a SGCluster, therefore the distributed log cluster could be receive SQL queries in a *postgres-util* fashion but not with it.

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
| [nonProductionOptions](#non-production-options) |          | ✓         | array    |         | {{< crd-field-description SGDistributedLogs.spec.nonProductionOptions >}} |

## Persistent volume

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| size                                       | ✓        |           | string   |                              | {{< crd-field-description SGDistributedLogs.spec.persistentVolume.size >}} |
| storageClass                               | ✓        |           | string   | default storage class        | {{< crd-field-description SGDistributedLogs.spec.persistentVolume.storageClass >}} |

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
