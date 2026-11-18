---
title: Strict Synchronous Replication Mode
weight: 3
url: /administration/replication/modes/strict-sync
description: This section describes the involved steps and concepts under the Strict Sync option.
---

The `replication.mode` *strict-sync* option instructs to create one or more cluster members as synchronous replicas. As indicated in the [CRD reference]({{% relref "06-crd-reference/01-sgcluster/#sgclusterspecreplication" %}}) the difference with the simple synchronous replication mode is that this option sets the Patroni `synchronous_mode_strict` and prevents from switching off the synchronous replication on the primary when no synchronous standby candidates are available. As a downside, the Leader is not enabled for writes (unless the Postgres transaction explicitly turns off synchronous_mode), blocking all client write requests until at least one synchronous replica comes up.

## Setting up a Cluster with strict-sync replica

Move forward to the next item, creating a synchronous replication cluster:

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
    mode: strict-sync
    role: ha-read
    syncInstances: 2
```

```sh
$ kubectl -n failover exec -it sync-cluster-0 -c patroni -- patronictl list 
+ Cluster: sync-cluster (7373750354182599290) -----+-----------+----+-----------+
| Member         | Host             | Role         | State     | TL | Lag in MB |
+----------------+------------------+--------------+-----------+----+-----------+
| sync-cluster-0 | 10.244.0.11:7433 | Leader       | running   |  2 |           |
| sync-cluster-1 | 10.244.0.10:7433 | Sync Standby | streaming |  2 |         0 |
| sync-cluster-2 | 10.244.0.4:7433  | Sync Standby | streaming |  2 |         0 |
| sync-cluster-3 | 10.244.0.8:7433  | Replica      | streaming |  2 |         0 |
+----------------+------------------+--------------+-----------+----+-----------+
```

