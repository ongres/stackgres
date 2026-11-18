---
title: Synchronous All Replication Mode 
weight: 4
url: /administration/replication/modes/sync-all
description: This section describes the involved steps and concepts of the sync-all replication mode.
---

The `replication.mode` *sync-all* option allows creating or converting all cluster members as synchronous replicas. As indicated in the [CRD reference]({{% relref "06-crd-reference/01-sgcluster/#sgclusterspecreplication" %}}) the synchronous replicas are not tightly coupled to the leader since Patroni will turn off synchronous replication if no more replicas are available.

## Setting up a Cluster with sync-all replica

Setting up such an option is quite simple as all the provided options. Look that the number of `syncInstances` is set to 1 intentionally to demonstrate that `sync-all` overrides the `syncInstances` variable.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: failover
  name: sync-cluster
spec:
  postgres:
    version: '16.1'
  instances: 4
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
    mode: sync-all
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
+----------------+------------------+--------------+-----------+----+-----------+
```
