---
title: SGShardedCluster
weight: 11
url: /reference/crd/sgshardedcluster
description: Details about SGShardedCluster
showToc: true
---

___

**Kind:** SGShardedCluster

**listKind:** SGShardedClusterList

**plural:** sgshardedclusters

**singular:** sgshardedcluster

**shortNames** sgscl
___

StackGres PostgreSQL sharded clusters are created using the `SGShardedCluster` Custom Resource.

**Example:**

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  postgres:
    version: 'latest'
  type: citus
  database: database
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

See also [Sharded Cluster Creation section]({{%  relref "04-administration-guide/14-sharded-cluster" %}}).

{{% include "generated/SGShardedCluster.md" %}}
