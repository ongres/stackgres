---
title: Through k8s Internal DNS
weight: 1
url: administration/cluster/connection/dns
description: Describes how to connect to the cluster inside the K8s environment.
showToc: true
---

With every StackGres cluster that you deploy a few services will be deployed along with it.
To connect to the database, you only need to be aware of two services: the primary and the replica service.

The primary service is used to connect to the primary node, and the replica service is used to access any of the replica nodes.

The service name follow a convention that is based on the cluster name and the function of the service:

 - `${CLUSTER-NAME}` for the primary service
 - `${CLUSTER-NAME}-replicas` for the replica service

Both services will accept connections to ports `5432` and `5433` where:

 - Port `5432` connects to pgbouncer - used by the application
 - Port `5433` connects to postgres - used for replication purposes

Therefore, given a cluster with name `stackgres` in the namespace `demo`, the primary node will accessible through the URL: `stackgres.demo:5432`.
Meanwhile, the replica node is accessible through the URL: `stackgres-replicas.demo:5432`.

## Examples

For all the following examples we're going to assume that we have a StackGres cluster named `stackgres` in the namespace `demo`.

### `psql`

In a pod that is running in the same Kubernetes cluster as StackGres and that has `psql` installed, we can connect to the primary node with the following command:

``` sh
PGPASSWORD=1775-d517-4136-958 psql -h stackgres.demo -U postgres
```
