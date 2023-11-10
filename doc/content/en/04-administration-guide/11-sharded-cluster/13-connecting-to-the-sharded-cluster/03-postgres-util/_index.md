---
title: Sharded Cluster Local connection using postgres-util
weight: 4
url: /administration/sharded-cluster/connection/pgutil
description: Describes how to connect on the sharded cluster using kubectl and the postgres-util sidecar container.
---

Local Connection to the database has to be through the `postgres-utils` sidecar.
This sidecar has all PostgreSQL binaries that are not present in the main container called `patroni` like the `psql` command.

This main container only have the required binaries and utilities to be able to configure the postgres sharded cluster and the HA configuration.


## Access to postgres-util sidecar

First we'll check the if the container is present in the pods, for these example we have a sharded cluster named `stackgres`, composed of three pods and installed in the `default` namespace:

```
kubectl get pods -n default -l app=StackGresShardedCluster,stackgres.io/shardedcluster=true
```

output:

```
NAME                READY   STATUS    RESTARTS   AGE
stackgres-coord-0   5/5     Running   0          12m
stackgres-shard0-0  5/5     Running   0          12m
stackgres-shard1-0  5/5     Running   0          11m
```

As you can see in the list we have `5/5` containers (sidecars) ready. To check the list of these containers we can run the next command:

```
kubectl get pods stackgres-coord-0 -n default -o jsonpath='{.spec.containers[*].name}*'
```

output:

```
patroni envoy pgbouncer postgres-util prometheus-postgres-exporter
```

At this point we already checked that sidecar `postgres-util` is up and running.
Now, to access the postgres instance through this sidecar you can either connect via a terminal and then access the `psql`, or execute the `psql` command directly:

Directly via `psql`:

```
$ kubectl exec -it stackgres-coord-0 -c postgres-util -- psql
psql (14.6 OnGres Inc.)
Type "help" for help.

postgres=# 
```

Or in a `bash` console:

```
$ kubectl exec -it stackgres-coord-0 -c postgres-util -- bash
bash-4.4$ psql
psql (14.6 OnGres Inc.)
Type "help" for help.

postgres=# 
```

The `psql` command connects via unix socket directly and doesn't require a password.
Alternatively, you could specify the port `6432` and then the connection goes through the connection pooling tool (PgBouncer), and you will be prompted for the `postgres` password.