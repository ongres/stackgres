---
title: Replication Group Mode
weight: 6
url: /administration/replication/modes/replication-group
description: This section describes the involved steps and concepts under the Stackgres replication group option and roles.
---

In Stackgres, the replication group is intended to manage Patroni tags for group members. In this way, it is possible to specify that a certain number of instances are tagged with the Patroni `nofailover` or `noloadbalance` creating groups of members within the cluster that are not taken into account to send reads or not to be promoted as the leader in a failure event. Hence, the role concept is explained at the same time.

Please, read the [CRD reference](https://stackgres.io/doc/latest/reference/crd/sgcluster/#sgclusterspecreplicationgroupsindex) for details.

The next example will help to understand the feature by creating a cluster with 6 members, where 3 members are indicated to group a `ha-read` role. The `ha-read` role is the default one, therefore it does not add any specific attribute to these members.

```sh
cat << EOF | kubectl apply -f -
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
EOF
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

# Updating the `repligation.group.role` to `ha`

The next example starts to make changes to the cluster and shows in action the SG design

```sh
cat << EOF | kubectl apply -f -
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
  prometheusAutobind: true
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
EOF
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


The primary instance will be elected among all the replication groups that are either ha or ha-read, since the Patroni tag `noloadbalance` was added to the "group"

# Updating the cluster to `readonly` role:

```sh
cat << EOF | kubectl apply -f -
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
  prometheusAutobind: true
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
EOF
sgcluster.stackgres.io/sync-cluster configured
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

The same as the latest example but using the `readonly` role SG added the Patroni `nofailover` tag to the "group", hence these members will never be promoted to Leader.

# Mixing roles!

The next example explains how SG supports the idea of creating many group within the cluster

```sh
cat << EOF | kubectl apply -f -
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
  prometheusAutobind: true
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
EOF
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

The three groups now took different roles and each one is acting based on the defined by SG yaml file.