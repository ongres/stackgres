---
title: Strict Synchronous All Replication Mode
weight: 5
url: /administration/replication/modes/strict-sync-all
description: This section describes the involved steps and concepts of the strict sync all replication mode.
---

The `replication.mode` *strict-sync-all* option allow to create or convert all cluster members as synchronous replicas and enables at the same time the Patroni `synchronous_mode_strict`. This is a combination of the `strict-sync` and `sync-all` replication modes and the cluster works with the highest HA possible in Postgres.

## Setting up a Cluster with Strict-sync-all replica

Setting this replication mode is quite simple. Here is an example.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: failover
  name: sync-cluster
spec:
  postgres:
    version: '16.1'
  instances: 6
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
    mode: strict-sync-all
    role: ha-read
    syncInstances: 1
```

```sh
$ kubectl -n failover exec -it sync-cluster-0 -c patroni -- patronictl list 
+ Cluster: sync-cluster (7373750354182599290) -----+-----------+----+-----------+
| Member         | Host             | Role         | State     | TL | Lag in MB |
+----------------+------------------+--------------+-----------+----+-----------+
| sync-cluster-0 | 10.244.0.11:7433 | Leader       | running   |  2 |           |
| sync-cluster-1 | 10.244.0.10:7433 | Sync Standby | streaming |  2 |         0 |
| sync-cluster-2 | 10.244.0.4:7433  | Sync Standby | streaming |  2 |         0 |
| sync-cluster-3 | 10.244.0.8:7433  | Sync Standby | streaming |  2 |         0 |
| sync-cluster-4 | 10.244.0.13:7433 | Sync Standby | streaming |  2 |         0 |
| sync-cluster-5 | 10.244.0.14:7433 | Sync Standby | streaming |  2 |         0 |
+----------------+------------------+--------------+-----------+----+-----------+
```
