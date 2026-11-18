---
title: Asynchronous Replication Mode
weight: 1
url: /administration/replication/modes/async
description: This section describes the involved steps and concepts of the async replication mode.
---

As indicated in the [CRD reference]({{% relref "06-crd-reference/01-sgcluster/#sgclusterspecreplication" %}}) the `replication.mode` *async* option is the default and enables the asynchronous Postgres replication mode. This is the most common way of creating a replica in a Postgres cluster.

## Setting up a Cluster with Asynchronous replicas

Setting up the StackGres Cluster with asynchronous replica members is quite straightforward. In the [Cluster Creation]({{% relref "04-administration-guide/02-cluster-creation" %}})  section, the example used the default way, async.

Nevertheless, the next box highlight the SGCluster CRD again:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: failover
  name: cluster
spec:
  postgres:
    version: '16.1'
  instances: 3
  sgInstanceProfile: 'size-s'
  pods:
    persistentVolume:
      size: '10Gi'
  configurations:
    sgPostgresConfig: 'pgconfig1'
    sgPoolingConfig: 'poolconfig1'
    observability:
      prometheusAutobind: true
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
  replication:
    mode: async
    role: ha-read
    syncInstances: 1
```

The result will be the next:

```sh
$ kubectl -n failover exec -it cluster-0 -c patroni -- bash - patronictl list
+ Cluster: cluster (7369933339677233777) +-----------+----+-----------+
| Member    | Host             | Role    | State     | TL | Lag in MB |
+-----------+------------------+---------+-----------+----+-----------+
| cluster-0 | 10.244.0.8:7433  | Leader  | running   |  1 |           |
| cluster-1 | 10.244.0.10:7433 | Replica | streaming |  1 |         0 |
| cluster-2 | 10.244.0.12:7433 | Replica | streaming |  1 |         0 |
+-----------+------------------+---------+-----------+----+-----------+
```

Maybe the variable `syncInstances` caught your attention. As shown above the cluster is composed by 1 Leader and 2 replicas using asynchronous replication. Therefore, we could think that the variable should be set to `0` instances, but the documentation confirms that the variable will take effect only if we enable synchronous replication. Please, be aware of this if there are plans to update the replication mode from async to sync.

Nevertheless, an example is included to demonstrate that updating the variable is harmless if sync mode is not enabled:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: failover
  name: async-cluster
spec:
  postgres:
    version: '16.1'
  instances: 3
  sgInstanceProfile: 'size-s'
  pods:
    persistentVolume:
      size: '10Gi'
  configurations:
    sgPostgresConfig: 'pgconfig1'
    sgPoolingConfig: 'poolconfig1'
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
  replication:
    mode: async
    role: ha-read
    syncInstances: 3
```

```sh
kubectl -n failover exec -it async-cluster-0 -c patroni -- bash - patronictl list
+ Cluster: async-cluster (7369943621678699243) +-----------+----+-----------+
| Member          | Host             | Role    | State     | TL | Lag in MB |
+-----------------+------------------+---------+-----------+----+-----------+
| async-cluster-0 | 10.244.0.14:7433 | Leader  | running   |  1 |           |
| async-cluster-1 | 10.244.0.16:7433 | Replica | streaming |  1 |         0 |
| async-cluster-2 | 10.244.0.18:7433 | Replica | streaming |  1 |         0 |
+-----------------+------------------+---------+-----------+----+-----------+
```
