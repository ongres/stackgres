---
title: Automated Failover
weight: 2
url: administration/patroni/failover
description: Details about how Patroni performs an automated failover.
---

## Patroni and Automated Failover

Patroni will handle failovers in case nodes become unavailable.
This is done automatically, and cluster admins don't have to care about it too much.

The following demonstrates Patroni's failover behavior.

You can query the Patroni status by running `patronictl` on `patroni`'s container.
In this case, we have a cluster `cluster` with two instances.
Assuming we query the instance `cluster-0`:

```
$ kubectl exec -it cluster-0 -n stackgres -c patroni -- patronictl list 
+ Cluster: cluster (6979461716096839850) ---+---------+----+-----------+
| Member    | Host                | Role    | State   | TL | Lag in MB |
+-----------+---------------------+---------+---------+----+-----------+
| cluster-0 | 192.168.59.169:7433 | Leader  | running |  1 |           |
| cluster-1 | 192.168.12.150:7433 | Replica | running |  1 |         0 |
+-----------+---------------------+---------+---------+----+-----------+
```

Here you can see the two nodes, that `cluster-0` is the leader node, the Postgres timeline and the lag. Let's trigger now a failover. For example, let's kill the `cluster-0` pod:

```
$ kubectl delete pod cluster-0 -n stackgres
pod "cluster-0" deleted
```

When killed, two things will happen in parallel:

* Kubernetes will create a new pod, which will take over `cluster-0`. It will attach the previous disk to this node. This operation may be quite fast (a few seconds).
* The replica (`cluster-1`) may or may not fail to see the leader renewing its lock.
  This is due to the timeout that govern when a leader lock expires.
  If the pod creation operation takes less time than the lock expiration, the new pod will take over the lock quickly enough to "hold" it, and `cluster-0` will remain the primary pod.
  For a few seconds, there was no primary instance.
  The new `cluster-0` node will be promoted to a new timeline (2), which will be shown in the Patroni state.
  If, however, the lock expired before `cluster-0` was re-created, `cluster-1` will be elected as the new leader.

In any case, the situation should restore back to normal, just with a timeline increased and a potential inversion of the Leader.
You may repeat this process as you wish, killing either a primary or replica pod.

```
$ kubectl exec -it cluster-1 -n stackgres -c patroni -- patronictl list 
+ Cluster: cluster (6979461716096839850) ---+---------+----+-----------+
| Member    | Host                | Role    | State   | TL | Lag in MB |
+-----------+---------------------+---------+---------+----+-----------+
| cluster-0 | 192.168.40.142:7433 | Replica | running |  2 |           |
| cluster-1 | 192.168.12.150:7433 | Leader  | running |  2 |         0 |
+-----------+---------------------+---------+---------+----+-----------+
```
