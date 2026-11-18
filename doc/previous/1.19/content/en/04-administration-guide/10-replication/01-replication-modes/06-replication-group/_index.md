---
title: Replication Group Mode
weight: 6
url: /administration/replication/modes/replication-group
description: This section describes the involved steps and concepts of the replication groups and roles.
---

The replication group is intended to manage Patroni tags for group members. In this way, it is possible to specify that a certain number of instances are tagged with the Patroni `nofailover` or `noloadbalance` creating groups of members within the cluster that are not taken into account to send reads or not to be promoted as the leader in a failure event. Hence, the role concept is explained at the same time.

Please, read the [CRD reference]({{% relref "06-crd-reference/01-sgcluster/#sgclusterspecreplicationgroupsindex" %}}) for details.

The next example will help to understand the feature by creating a cluster with 6 members, where 3 members are part of a group with `ha-read` role. The `ha-read` role is the default one, therefore it does not add any specific attribute to these members.

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
    mode: sync
    role: ha-read
    syncInstances: 1
    groups:
    - instances: 3
      role: ha-read
      name: group1-sync-cluster
```

```sh
$ kubectl -n failover exec -it sync-cluster-0 -c patroni -- patronictl list 
+ Cluster: sync-cluster (7373750354182599290) -----+-----------+----+-----------+
| Member         | Host             | Role         | State     | TL | Lag in MB |
+----------------+------------------+--------------+-----------+----+-----------+
| sync-cluster-0 | 10.244.0.8:7433  | Leader       | running   |  1 |           |
| sync-cluster-1 | 10.244.0.10:7433 | Sync Standby | streaming |  1 |         0 |
| sync-cluster-2 | 10.244.0.12:7433 | Replica      | streaming |  1 |         0 |
| sync-cluster-3 | 10.244.0.14:7433 | Replica      | streaming |  1 |         0 |
| sync-cluster-4 | 10.244.0.19:7433 | Replica      | streaming |  1 |         0 |
| sync-cluster-5 | 10.244.0.20:7433 | Replica      | streaming |  1 |         0 |
+----------------+------------------+--------------+-----------+----+-----------+
```

## Updating the `replication.groups.role` to `ha`

The next example include some changes to the cluster in order to show labels applied to a group with ha role:

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
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
  replication:
    mode: sync
    role: ha-read
    syncInstances: 1
    groups:
    - instances: 3
      role: ha 
      name: group1-sync-cluster
```

```sh
$ kubectl -n failover exec -it sync-cluster-0 -c patroni -- patronictl list 
+ Cluster: sync-cluster (7373750354182599290) -----+-----------+----+-----------+---------------------+
| Member         | Host             | Role         | State     | TL | Lag in MB | Tags                |
+----------------+------------------+--------------+-----------+----+-----------+---------------------+
| sync-cluster-0 | 10.244.0.8:7433  | Leader       | running   |  1 |           |                     |
| sync-cluster-1 | 10.244.0.10:7433 | Sync Standby | streaming |  1 |         0 |                     |
| sync-cluster-2 | 10.244.0.12:7433 | Replica      | streaming |  1 |         0 |                     |
| sync-cluster-3 | 10.244.0.14:7433 | Replica      | streaming |  1 |         0 | noloadbalance: true |
| sync-cluster-4 | 10.244.0.19:7433 | Replica      | streaming |  1 |         0 | noloadbalance: true |
| sync-cluster-5 | 10.244.0.20:7433 | Replica      | streaming |  1 |         0 | noloadbalance: true |
+----------------+------------------+--------------+-----------+----+-----------+---------------------+
```

The primary instance will be elected among all the replication groups that are either ha or ha-read, since the Patroni tag noloadbalance was added to the ha "group", the members of such group will not be targeted by the read-only service <cluster name>-replicas.

## Updating the cluster to `readonly` role:

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
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
  replication:
    mode: sync
    role: ha-read
    syncInstances: 1
    groups:
    - instances: 3
      role: readonly
      name: group1-sync-cluster
```

```sh
$ kubectl -n failover exec -it sync-cluster-0 -c patroni -- patronictl list 
+ Cluster: sync-cluster (7373750354182599290) -----+-----------+----+-----------+------------------+
| Member         | Host             | Role         | State     | TL | Lag in MB | Tags             |
+----------------+------------------+--------------+-----------+----+-----------+------------------+
| sync-cluster-0 | 10.244.0.8:7433  | Leader       | running   |  1 |           |                  |
| sync-cluster-1 | 10.244.0.10:7433 | Sync Standby | streaming |  1 |         0 |                  |
| sync-cluster-2 | 10.244.0.12:7433 | Replica      | streaming |  1 |         0 |                  |
| sync-cluster-3 | 10.244.0.14:7433 | Replica      | streaming |  1 |         0 | nofailover: true |
| sync-cluster-4 | 10.244.0.19:7433 | Replica      | streaming |  1 |         0 | nofailover: true |
| sync-cluster-5 | 10.244.0.20:7433 | Replica      | streaming |  1 |         0 | nofailover: true |
+----------------+------------------+--------------+-----------+----+-----------+------------------+
```

The same as the latest example but using the `readonly` role that adds the Patroni `nofailover` tag to the "group", hence these members will never be promoted to leader.

## Combining roles!

The next example explains how creating many groups within the cluster is supported.

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
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
  replication:
    mode: sync
    role: ha-read
    syncInstances: 1
    groups:
    - instances: 1
      role: ha
      name: group1-sync-cluster
    - instances: 1
      role: readonly
      name: group2-sync-cluster
    - instances: 1
      role: ha-read
      name: group3-sync-cluster
```

```sh
$ kubectl -n failover exec -it sync-cluster-0 -c patroni -- patronictl list 
+ Cluster: sync-cluster (7373750354182599290) -----+-----------+----+-----------+---------------------+
| Member         | Host             | Role         | State     | TL | Lag in MB | Tags                |
+----------------+------------------+--------------+-----------+----+-----------+---------------------+
| sync-cluster-0 | 10.244.0.8:7433  | Leader       | running   |  1 |           |                     |
| sync-cluster-1 | 10.244.0.10:7433 | Sync Standby | streaming |  1 |         0 |                     |
| sync-cluster-2 | 10.244.0.12:7433 | Replica      | streaming |  1 |         0 |                     |
| sync-cluster-3 | 10.244.0.14:7433 | Replica      | streaming |  1 |         0 | noloadbalance: true |
| sync-cluster-4 | 10.244.0.19:7433 | Replica      | streaming |  1 |         0 | nofailover: true    |
| sync-cluster-5 | 10.244.0.20:7433 | Replica      | streaming |  1 |         0 |                     |
+----------------+------------------+--------------+-----------+----+-----------+---------------------+
```

Now we have three groups where each took a different role and act based on the role defined in the cluster YAML.
