---
title: Sharding
weight: 11
url: /doc/latest/features/sharding
description: Create horizontally scalable PostgreSQL clusters with automatic sharding.
---

StackGres supports creating sharded PostgreSQL clusters using the SGShardedCluster custom resource. Sharding enables horizontal scaling by distributing data across multiple PostgreSQL instances.

## What is Sharding?

Sharding is a database architecture pattern that partitions data horizontally across multiple database instances (workers). Each worker contains a subset of the total data, allowing:

- **Horizontal scalability**: Add more workers to handle increased load
- **Improved performance**: Queries can be parallelized across workers
- **Larger datasets**: Store more data than a single instance can handle

## StackGres Sharding Architecture

A StackGres sharded cluster consists of:

- **Coordinator**: Routes queries to appropriate workers
- **Workers**: Individual PostgreSQL clusters holding data partitions

![SG Sharded Architecture](SG_StackGres_ShardedCluster_Architecture.png "StackGres-Sharded_Architecture")

## Sharding Technologies

StackGres supports multiple sharding technologies:

| Technology | Description |
|------------|-------------|
| Citus | Distributed PostgreSQL extension |
| ShardingSphere | Database middleware for sharding |
| DDP (Distributed Data Platform) | Native distributed tables |

## Key Features

- **Single configuration**: Define an entire sharded cluster in one SGShardedCluster resource
- **Automatic management**: StackGres handles worker creation and coordination
- **High availability**: Each worker is a fully HA PostgreSQL cluster
- **Unified monitoring**: Monitor all workers from a single dashboard
- **Day-2 operations**: Perform operations across all workers simultaneously

## Getting Started

For detailed setup instructions, see the [Sharded Cluster Administration Guide]({{% relref "04-administration-guide/14-sharded-cluster" %}}).

## Related Resources

- [SGShardedCluster Reference]({{% relref "06-crd-reference/11-sgshardedcluster" %}})
- [Sharded Cluster Operations]({{% relref "04-administration-guide/14-sharded-cluster/16-database-operations" %}})
