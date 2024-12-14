---
title: SGCluster
weight: 1
url: /reference/crd/sgcluster
description: Details about SGCluster
showToc: true
---

___

**Kind:** SGCluster

**listKind:** SGClusterList

**plural:** sgclusters

**singular:** sgcluster

**shortNames** sgclu
___
 
StackGres PostgreSQL cluster can be created using an SGCluster Custom Resource.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  instances: 1
  postgres:
    version: 'latest'
  pods:
    persistentVolume:
      size: '5Gi'
  sgInstanceProfile: 'size-s'
```

See also [Cluster Creation section]({{% relref "04-administration-guide/02-cluster-creation" %}}).

{{% include "generated/SGCluster.md" %}}

## Sidecar Containers

A sidecar container is a container that adds functionality to PostgreSQL or to the cluster infrastructure.
Currently StackGres implement following sidecar containers:

* `cluster-controller`: this container is always present, and it is not possible to disable it.
 It serves to reconcile local configurations, collects Pod status, and performs local actions (like extensions installation, execution of SGScript entries, etc.).
* `envoy`: this container is always present, and it is not possible to disable it.
 It serve as a edge proxy from client to PostgreSQL instances or between PostgreSQL instances.
 It enables network metrics collection to provide connection statistics.
* `pgbouncer`: PgBouncer that serves as connection pooler for the PostgreSQL instances.
* `prometheus-postgres-exporter`: Postgres exporter that exports metrics for the PostgreSQL instances.
* `fluent-bit`: Fluent-bit that send logs to a distributed logs cluster.
* `postgres-util`: Contains `psql` and all PostgreSQL common tools in order to perform common administration tasks.

The following example disables all non-essential sidecars:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    disableConnectionPooling: false
    disableMetricsExporter: false
    disablePostgresUtil: false
```
