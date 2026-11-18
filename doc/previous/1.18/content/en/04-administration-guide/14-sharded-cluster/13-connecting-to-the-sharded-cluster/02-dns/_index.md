---
title: Sharded Cluster K8s Internal DNS
weight: 2
url: /administration/sharded-cluster/connection/dns
description: Describes how to connect to the sharded cluster inside the K8s environment.
showToc: true
---

With every StackGres sharded cluster that you deploy a few services will be deployed along with it.
To connect to the database, you only need to be aware of three services: the coordinator primary and any service and the shards primaries service.

The coordinator primary service is used to connect to the primary node of the coordinator cluster, the coordinator any service is used to access any node of the coordinator cluster and the shards primaries service is used to connect to the primary node of any shard cluster.

The service name follow a convention that is based on the sharded cluster name and the function of the service:

 - `<cluster-name>` for the coordinator primary service
 - `<cluster-name>-reads` for the coordinator any service
 - `<cluster-name>-shards` for the shards primaries service

All this services will accept connections to ports `5432` and `5433` where:

 - Port `5432` connects to pgbouncer - used by the application
 - Port `5433` connects to postgres - used for replication purposes

Therefore, given a sharded cluster with name `cluster` in the namespace `default`, the primary node of the coordinator cluster will accessible through the URL: `cluster.default:5432`.
Meanwhile, any node of the coordinator cluster will be accessible through the URL: `cluster-reads.default:5432`.
Finally, primary node of any shard cluster will be accessible through the URL: `cluster-primaries.default:5432`.

## Psql Example

For the following example we assume that we have a StackGres sharded cluster named `cluster` in the namespace `default`.

In a pod that is running in the same Kubernetes cluster as StackGres and that has `psql` installed, we can connect to the coordinator primary node using the `postgres` user and the password that you retrieve as described in [Retrieving the Generated Password]({{% relref "04-administration-guide/03-connecting-to-the-cluster/01-passwords" %}}):

```
PGPASSWORD=$PASSWORD psql -h cluster.default -U postgres
```