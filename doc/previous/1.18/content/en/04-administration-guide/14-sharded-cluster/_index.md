---
title: Sharded Cluster
weight: 12
url: /administration/sharded-cluster
description: Overview of StackGres sharded cluster.
showToc: true
---

## What is a Sharded Cluster

A sharded cluster is a cluster that implements database sharding. Database sharding is the process of storing a large database across multiple machines. This is achieved by separating table rows
 among multiple Postgres primary instances. This approach gives the ability to scale out a database into multiple instances allowing to benefit both reads and writes throughput but also to separate
 data among different instances for security and/or to address regulatory or compliance requirements.

## How is Sharded Cluster implemented

A sharded cluster is implemented by creating an SGCluster called coordinator and one or more SGCluster called shards. The coordinator, as the name implies, coordinates the shards where the data is
 actually stored. StackGres takes care of creating the dependent SGCluster by following the specification set in the SGShardedCluster.

The SGShardedCluster can define the type of sharding (that is the internal sharding implementation used) and the database to be sharded.

Currently three implementations are available:

* `citus`: provided by using [Citus](https://github.com/citusdata/citus) extension.
* `shardingsphere`: provided by using [Apache ShardingSphere](https://shardingsphere.apache.org/) middleware as the coordinator.
* `ddp`: provided by suing [ddp](https://gitlab.ongres.com/ongresinc/extensions/ddp) an SQL only extension that leverages Postgres core functionalities like partitioning, `postgres_fdw` and `dblink` contrib extensions.

## Citus Sharding Technology

Citus is the most popular sharding technology with advanced features like a distributed query engine, columnar storage, and the ability to query the sharded database from any Postgres instance.

StackGres sharded cluster uses the [Patroni integration for Citus](https://patroni.readthedocs.io/en/latest/citus.html). Patroni is aware of the topology of the Postgres clusters, so it is capable of updating the Citus node table whenever a failover in any cluster occurs.

**Architecture:**
- **Coordinator:** A special SGCluster that coordinates queries and manages metadata
- **Shards:** Worker nodes implemented as a group of SGClusters where distributed data lives

> **Terminology note:** Citus documentation calls "shards" the distributed partitions of a table. Each worker contains multiple distributed partitions of a single distributed table. In StackGres documentation, we use "distributed partitions" to avoid confusion.

For more details about Citus sharding technology see the [official Citus documentation](https://docs.citusdata.com/) and have a look at the [Citus sharding technology]({{% relref "04-administration-guide/14-sharded-cluster/01-citus-sharding-technology" %}}) section.

## Apache ShardingSphere Sharding Technology

Apache ShardingSphere is an ecosystem to transform any database into a distributed database system, and enhance it with sharding, elastic scaling, encryption features and more.

StackGres implementation of ShardingSphere as a sharding technology uses the [ShardingSphere Proxy](https://shardingsphere.apache.org/document/current/en/quick-start/shardingsphere-proxy-quick-start/) as an entry point to distribute SQL traffic among the shards. This implementation requires the [ShardingSphere Operator](https://shardingsphere.apache.org/oncloud/current/en/user-manual/cn-sn-operator/) to be installed and will create a ComputeNode for coordination.

**Architecture:**
- **Coordinator:** A ShardingSphere Proxy ComputeNode that routes and distributes SQL queries
- **Shards:** PostgreSQL clusters implemented as a group of SGClusters where distributed data lives

For more details about ShardingSphere sharding technology see the [official Apache ShardingSphere documentation](https://shardingsphere.apache.org/document/current/en/overview/) and have a look at the [ShardingSphere sharding technology]({{% relref "04-administration-guide/14-sharded-cluster/02-shardingsphere-sharding-technology" %}}) section.

## DDP Sharding Technology

DDP (Distributed Data Partitioning) allows you to distribute data across different physical nodes to improve the query performance of high data volumes, taking advantage of distinct nodes' resources. It uses a coordinator as an entry point in charge of sending and distributing queries to the shard nodes.

DDP is an SQL-only extension that leverages Postgres core functionalities like partitioning, `postgres_fdw` and `dblink` contrib extensions. This means no external middleware or third-party extension is required beyond what PostgreSQL already provides.

**Architecture:**
- **Coordinator:** A standard SGCluster that uses `postgres_fdw` to route queries to shard nodes
- **Shards:** PostgreSQL clusters implemented as a group of SGClusters where distributed data lives, accessed via foreign data wrappers

For more details about DDP sharding technology have a look at the [DDP sharding technology]({{% relref "04-administration-guide/14-sharded-cluster/03-ddp-sharding-technology" %}}) section.

## Services

A sharded cluster creates the following Services:

- **Main Service** (same name as SGShardedCluster): Points to the primary Pod of the coordinator for read/write queries and for command queries
- **`-any` Service**: Points to all Pods of the coordinator
- **`-primaries` Service**: Points to all primary Pods of the shards (for Citus this can be also used for read/write queries)
