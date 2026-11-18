---
title: Graceful Switchover
weight: 1
url: /administration/patroni/switchover
description: Details about how to use Patroni to perform a controlled switchover.
---

## Performing a Switchover to a Replica

A switchover (or graceful switchover) is a planned role reversal between the primary and the one of the standby databases.
This is used when there is a planned outage on the primary database or primary server and you do not want to have extended downtime on the primary database.
The switchover allows you to switch the roles of the databases so that the standby database now becomes the primary database and all your users and applications can continue operations on the "new" primary node.


To perform this we will use the `patronictl switchover` command:

As we can see in the cluster status shown before the primary node is the one called `stackgres-0` with the leader role and we are going to switch it to the node called `stackgres-1` so we run:

```
bash-4.4$ patronictl switchover
```

Then we will be asked for the primary node (note that the command already gives us the primary node name):

```
Master [stackgres-0]:
```

then we will be asked for the candidate that we want to promote:

```
Candidate ['stackgres-1', 'stackgres-2'] []:
```

and when we want to make the change:

```
When should the switchover take place (e.g. 2020-01-16T17:23 )  [now]:
```

And as a final question and warning, this show us the current status and asks if we want to proceed with the change:

```
Current cluster topology
+-----------+-------------+------------------+--------+---------+----+-----------+
|  Cluster  |    Member   |       Host       |  Role  |  State  | TL | Lag in MB |
+-----------+-------------+------------------+--------+---------+----+-----------+
| stackgres | stackgres-0 | 10.244.0.11:5433 | Leader | running |  2 |           |
| stackgres | stackgres-1 | 10.244.0.12:5433 |        | running |  2 |       0.0 |
| stackgres | stackgres-2 | 10.244.0.13:5433 |        | running |  2 |       0.0 |
+-----------+-------------+------------------+--------+---------+----+-----------+
Are you sure you want to switchover cluster stackgres, demoting current primary stackgres-0? [y/N]:
```

After accepting the change Patroni will output the operation status and the new cluster status:

```
2020-01-16 16:26:13.03648 Successfully switched over to "stackgres-1"
+-----------+-------------+------------------+--------+---------+----+-----------+
|  Cluster  |    Member   |       Host       |  Role  |  State  | TL | Lag in MB |
+-----------+-------------+------------------+--------+---------+----+-----------+
| stackgres | stackgres-0 | 10.244.0.11:5433 |        | stopped |    |   unknown |
| stackgres | stackgres-1 | 10.244.0.12:5433 | Leader | running |  3 |           |
| stackgres | stackgres-2 | 10.244.0.13:5433 |        | running |    |   unknown |
+-----------+-------------+------------------+--------+---------+----+-----------+
```

The old primary node `stackgres-0` will be stopped and then re-joined to the cluster as a replica.

```
bash-4.4$ patronictl list
+-----------+-------------+------------------+--------+---------+----+-----------+
|  Cluster  |    Member   |       Host       |  Role  |  State  | TL | Lag in MB |
+-----------+-------------+------------------+--------+---------+----+-----------+
| stackgres | stackgres-0 | 10.244.0.11:5433 |        | running |  3 |       0.0 |
| stackgres | stackgres-1 | 10.244.0.12:5433 | Leader | running |  3 |           |
| stackgres | stackgres-2 | 10.244.0.13:5433 |        | running |  3 |       0.0 |
+-----------+-------------+------------------+--------+---------+----+-----------+
```

> **IMPORTANT NOTE:** We strongly recommend to not manipulate the cluster with any other `patronictl` to avoid data loss or damage to the entire configuration. Use the command explained above only if you know what you are doing.
