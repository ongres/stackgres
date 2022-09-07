---
title: Through k8s internal DNS
weight: 1
url: administration/cluster/connection/dns
description: Describes how to connect on the cluster inside the k8s environment.
showToc: true
---

With every StackGres cluster that you deploy a few services will be deployed.  To connect to the database you only need to be aware of two services: the primary and the replica service.

The primary service is used to connect to the primary node and the replica service is used to access any of the replica nodes.

These services will follow a convention that is based in the cluster name and the function of the service, so that, the name of the services will be:

 - `${CLUSTER-NAME}`
 - `${CLUSTER-NAME}-replicas`

Both services will accept connections to ports `5432` and `5433` where:

1. Port `5432` will point to pgbouncer - used by the application
1. Port `5433` will point to postgres - used for replication purposes

Therefore, given a cluster with name "stackgres" in the namespace "demo", the primary node will accessible through
 the URL: `stackgres.demo:5432`.  Meanwhile, the replica node is accessible through the URL: `stackgres-replicas.demo:5432`.

## Examples

For all the following examples we're going to assume that we have a StackGres cluster named `stackgres` in the namespace `demo`.

### `psql`

With a pod with `psql` running in the same kubernetes cluster than the StackGres cluster, we can connect to the primary node with the following command:

``` sh
PGPASSWORD=1775-d517-4136-958 psql -h stackgres.demo -U postgres
```
