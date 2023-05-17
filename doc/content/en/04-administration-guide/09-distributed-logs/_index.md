---
title: Distributed Logs
weight: 11
url: administration/distributed-logs
aliases: [ /tutorial/complete-cluster/distributed-logs, /tutorial/complete-cluster/distributed-logs ]
description: Details about how to create a distributed log instance.
---

By default, Postgres logs would be written to the ephemeral storage of the `Patroni` container, and could be accessed in the usual manner.
However, in a modern environment this is not ideal because of the ephemeral nature of the storage, and because logs from all pods are on different locations.

StackGres has created a technology stack to send Postgres and Patroni logs to a separate location, called a `Distributed Logs Server`.
This server is represented by the [SGDistributedLogs]({{% relref "06-crd-reference/07-sgdistributedlogs" %}}) CRD.
A distributed log server is a separate Postgres instance, optimized for log storage, using the time-series Timescale extension to support high volume injection and automatic partitioning of logs, as well as log rotation.

This is all handled transparently for you, just go ahead and create the file `sgdistributedlogs-server1.yaml` to use this functionality:

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  namespace: demo
  name: distributedlogs
spec:
  persistentVolume:
    size: 50Gi
```

and deploy it to Kubernetes:

```
kubectl apply -f sgdistributedlogs-server1.yaml
```

This command will create multiple Kubernetes resources.
In particular, it will create a pod for storing the mentioned distributed logs:

```
kubectl -n demo get pods
```

```
NAME                READY   STATUS    RESTARTS   AGE
distributedlogs-0   3/3     Running   1          73s
```

Distributed logs server are multi-tenant: you may reference a distributed log server from more than one cluster.
If a distributed log server is used, Postgres logs will not be stored in the ephemeral pod storage (except temporarily in small buffers).

To see the distributed logs, you may view them in the web console, or connect via `psql` and query them with SQL.

## Accessing Postgres and Patroni Logs

In the admin UI, accessing the logs is easy: go to the web console, navigate to the cluster, and click on the `Logs` pane.

But now, let's do it from the CLI.
You are able to connect to the distributed logs database and query the logs with SQL.
Indeed, the `SGDistributedLogs` resource that we created before led to the creation of a specialized `SGCluster`, used for logs.
You connect to this cluster through its read and write service, similar to any other StackGres cluster.
For the distributed logs, the host name equals the name specified in the `SGDistributedLogs` resource, in our case `distributedlogs`.

In the same way as before, we can retrieve the connection password from the `distributedlogs` secret:

```
$ PGPASSWORD=$(kubectl -n demo get secret distributedlogs --template '{{ printf "%s" (index .data "superuser-password" | base64decode) }}')
```

Then, we can connect to our distributed logs cluster via `psql`:

```
$ kubectl -n demo run psql --env $PGPASSWORD --rm -it --image ongres/postgres-util --restart=Never -- psql -h distributedlogs postgres postgres
```

Now that we're in `psql`, we can query the logs with SQL.
The following commands will list the databases, connect to the database for our current cluster, count the Postgres log entries, describe the logs table, and select all logs of type `ERROR`.
There will be one database per every cluster that is sending logs to this distributed logs server, with the naming scheme `<namespace>_<cluster>`.
You can generate `ERROR` logs by typing any SQL error into the SQL console of the source cluster (not this one).
Logs may take a few seconds to propagate.

```
\l+
\c demo_cluster
\dt log_postgres
select count(*) from log_postgres;
select * from log_postgres where error_severity = 'ERROR';
```

> `\l+` Lists the databases (with additional details) \
> `\c <db>` Connects to a database \
> `\dt <table>` Lists all tables (if `<table>` isn't specified) or a specific table
