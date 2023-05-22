---
title: K8s Internal DNS
weight: 2
url: administration/cluster/connection/dns
description: Describes how to connect on the cluster inside the k8s environment.
showToc: true
---

With every StackGres cluster that you deploy a few services will be deployed.  To connect to the database you only need to be aware of two services: the primary and the replica service.

The primary service is used to connect to the primary node and the replica service is used to access any of the replica nodes.

These services will follow a convention that is based in the cluster name and the function of the service, so that, the name of the services will be:

 - `<cluster-name>` for the primary service
 - `<cluster-name>-replicas` for the replica service

Both services will accept connections to ports `5432` and `5433` where:

1. Port `5432` will point to pgbouncer - used by the application
1. Port `5433` will point to postgres - used for replication purposes

Therefore, given a cluster with name `cluster` in the namespace `default`, the primary node will accessible through the URL: `cluster.default:5432`.
Meanwhile, the replica node is accessible through the URL: `cluster-replicas.default:5432`.

## Psql Example

For the following example we assume that we have a StackGres cluster named `cluster` in the namespace `default`.

In a pod that is running in the same Kubernetes cluster as StackGres and that has `psql` installed, we can connect to the primary node using the `postgres` user and the password that you retrieve as described in [Retrieving the Generated Password]({{% relref "04-administration-guide/02-connecting-to-the-cluster/01-passwords" %}}):

```
PGPASSWORD=$PASSWORD psql -h cluster.default -U postgres
```
