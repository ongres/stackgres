---
title: Cluster K8s Internal DNS
weight: 2
url: /administration/cluster/connection/dns
description: Describes how to connect to the cluster inside the K8s environment.
showToc: true
---

With every StackGres cluster that you deploy a few services will be deployed along with it.
To connect to the database, you only need to be aware of two services: the primary and the replica service.

The primary service is used to connect to the primary node, and the replica service is used to access any of the replica nodes.

The service names follow a convention that is based on the cluster name and the function of the service:

 - `<cluster-name>` for the primary service
 - `<cluster-name>-replicas` for the replica service

Both services will accept connections to ports `5432` and `5433` where:

 - Port `5432` connects to pgbouncer - used by the application
 - Port `5433` connects to postgres - used for replication purposes

Therefore, given a cluster with name `cluster` in the namespace `default`, the primary node will be accessible through the URL: `cluster.default:5432`.
Meanwhile, the replica node is accessible through the URL: `cluster-replicas.default:5432`.

## Psql Example

For the following example we assume that we have a StackGres cluster named `cluster` in the namespace `default`.

In a pod that is running in the same Kubernetes cluster as StackGres and that has `psql` installed, we can connect to the primary node using the `postgres` user and the password that you retrieve as described in [Retrieving the Generated Password]({{% relref "04-administration-guide/03-connecting-to-the-cluster/01-passwords" %}}):

```
PGPASSWORD=$PASSWORD psql -h cluster.default -U postgres
```