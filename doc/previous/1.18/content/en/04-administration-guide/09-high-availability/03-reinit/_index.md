---
title: Replica Re-initialization
weight: 3
url: /administration/patroni/reinit
description: Details about how to use Patroni to reinit the database replicas nodes.
---

## Performing a re-initialization of a Replica

Re-initialization of a replica has to be performed when Postgres is not able to catch-up with the primary database and Patroni is not capable of recovering it automatically.
The re-initialization of a Replica allows you to copy its data from scratch directly from the primary and recover it completely.

To perform this we will use the `patronictl reinit` command:

As we can see in the cluster status shown before the primary node is the one called `stackgres-0` with the leader role and we are going to reinit the node called `stackgres-1` so we run:

```
bash-4.4$ patronictl reinit stackgres
```

Then this show us the current status and we will be asked for the replica node (note that the command already gives us the replica node name):

```
+ Cluster: stackgres -------------+---------+---------+----+-----------+
| Member       | Host             | Role    | State   | TL | Lag in MB |
+--------------+------------------+---------+---------+----+-----------+
| stackgres-0  | 10.244.0.23:7433 | Leader  | running |  2 |           |
| stackgres-1  | 10.244.0.22:7433 | Replica | running |  2 |         0 |
+--------------+------------------+---------+---------+----+-----------+
Which member do you want to reinitialize [stackgres-1]? []: 
```

And as a final question and warning asks if we want to proceed with the change:

```
Are you sure you want to reinitialize members stackgres-1? [y/N]:
```

After accepting the change Patroni will output the operation status:

```
Success: reinitialize for member stackgres-1
```

The old replica node `stackgres-1` will be stopped and then re-joined to the cluster.

```
bash-4.4$ patronictl list
+ Cluster: stackgres -------------+---------+---------+----+-----------+
| Member       | Host             | Role    | State   | TL | Lag in MB |
+--------------+------------------+---------+---------+----+-----------+
| stackgres-0  | 10.244.0.23:7433 | Leader  | running |  2 |           |
| stackgres-1  | 10.244.0.22:7433 | Replica | running |  2 |         0 |
+--------------+------------------+---------+---------+----+-----------+
```

> **IMPORTANT NOTE:** We strongly recommend to not manipulate the cluster with any other `patronictl` to avoid data loss or damage to the entire configuration. Use the command explained above only if you know what you are doing.
