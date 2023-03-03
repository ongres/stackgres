---
title: Distributed Logs
weight: 5
url: tutorial/complete-cluster/distributed-logs
description: Details about how to create a distributed log instance.
---

By default, Postgres logs are written to the ephemeral storage of the `Patroni` container, and can be accessed in the usual manner.
However, this is not ideal because of the ephemeral nature of the storage, and because logs from all pods are on different locations.

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

```plain
kubectl apply -f sgdistributedlogs-server1.yaml
```

This last command will trigger the creation of multiple Kubernetes resources (other than metadata).
In particular, it will create a pod for storing the mentioned distributed logs:

```bash
kubectl -n demo get pods
```

```plain
NAME                READY   STATUS    RESTARTS   AGE
distributedlogs-0   3/3     Running   1          73s
```

Distributed logs server are multi-tenant: you may reference a distributed log server from more than one cluster.
If a distributed log server is used, Postgres logs will not be stored in the ephemeral pod storage (except temporarily in small buffers).

To see the distributed logs, you may view them in the web console, or connect via `psql` and query them with SQL.
