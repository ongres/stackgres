---
title: High Availability
weight: 7
url: administration/patroni
aliases: [ /administration/patroni/management ]
description: Details about how to use Patroni to handle the database replicas and primary nodes.
---

A StackGres cluster has a full high-availability PostgreSQL configuration managed by [Patroni](https://github.com/zalando/patroni).

![Patroni Management](patroni-management.png "Patroni Management")

The replicas correspond to pods in the StackGres cluster, and can be listed using `kubectl`:

```
kubectl get pods -n default -l app=StackGresCluster,stackgres.io/cluster=true
````

> **Note:** Change `-n` param to point to your namespace, in this example we use default.

And we'll get an output like:

```
NAME          READY   STATUS    RESTARTS   AGE
stackgres-0   5/5     Running   0          163m
stackgres-1   5/5     Running   0          163m
stackgres-2   5/5     Running   0          162m
```

## Identifying the Master and Replica Nodes

One of the most important task is to be able to identify which node is the current master and which ones the replica nodes.

There are two different ways to accomplish this. The first one is with the `kubectl` command using the pod labels:

To identify the master node:

```
$ kubectl get pods -n default -l app=StackGresCluster -l role=master
NAME          READY   STATUS    RESTARTS   AGE
stackgres-0   5/5     Running   0          165m
```

To identify the replica nodes:

```
$ kubectl get pods -n default -l app=StackGresCluster,stackgres.io/cluster=true -l role=replica
NAME          READY   STATUS    RESTARTS   AGE
stackgres-1   5/5     Running   0          165m
stackgres-2   5/5     Running   0          165m
```

The other way is to use the own Patroni commands. But first we need to connect to the Patroni container:

```
kubectl exec -it stackgres-0 -c patroni -- bash
```

Once you are connected to it run the Patroni command:

```
$ patronictl list
+-----------+-------------+------------------+--------+---------+----+-----------+
|  Cluster  |    Member   |       Host       |  Role  |  State  | TL | Lag in MB |
+-----------+-------------+------------------+--------+---------+----+-----------+
| stackgres | stackgres-0 | 10.244.0.11:5433 | Leader | running |  2 |           |
| stackgres | stackgres-1 | 10.244.0.12:5433 |        | running |  2 |       0.0 |
| stackgres | stackgres-2 | 10.244.0.13:5433 |        | running |  2 |       0.0 |
+-----------+-------------+------------------+--------+---------+----+-----------+
```

As you can see we get the cluster status from a Patroni node.
We can retrieve some valuable information here:

- Who is the master node
- Who are the replica nodes
- The IP address and port
- The state of each node
- The lag in MB in case some nodes are not up-to-date