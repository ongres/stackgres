---
title: DDP sharding technology
weight: 3
url: /administration/sharded-cluster/ddp
description: Details about DDP sharding technology.
---

## Overview

DDP (Distributed Data Partitioning) allows you to distribute data across different physical nodes to improve the query performance of high data volumes, taking advantage of distinct nodes' resources.

DDP is an SQL-only sharding implementation that leverages PostgreSQL core functionalities:

- **Partitioning**: Uses `PARTITION BY RANGE` to create virtual shards that map to physical worker nodes
- **`postgres_fdw`**: Creates foreign data wrapper connections to remote worker nodes, allowing the coordinator to query data transparently
- **`dblink`**: Used for management operations like checking worker connection status and creating distributed restore points

No external middleware or third-party extension is required beyond what PostgreSQL already provides.

## How DDP Works

DDP uses the coordinator as the entry point for all queries. The coordinator maintains foreign table definitions that map to tables on the worker nodes via `postgres_fdw`. When a query is executed, PostgreSQL's query planner routes the query to the appropriate worker based on the partition definitions.

### Virtual Shards

DDP introduces the concept of virtual shards. Virtual shards are range partitions on the coordinator that map to foreign tables on the worker nodes. This allows fine-grained control over data distribution:

- Multiple virtual shards can exist on a single physical shard
- Virtual shards can be moved between physical shards for rebalancing

### Shard Connections

Each worker is connected to the coordinator via `postgres_fdw` foreign servers. DDP provides SQL functions to manage these connections:

- `ddp_create_shard_connection()`: Creates a new FDW server connection to a worker
- `ddp_change_shard_connection()`: Modifies an existing worker connection
- `ddp_drop_shard_connection()`: Removes a worker connection
- `ddp_get_shard_status_connection()`: Checks worker connection status
- `ddp_has_shard_connection()`: Checks if a worker connection exists

### Data Distribution

DDP provides functions to manage data distribution across workers:

- `ddp_create_vs()`: Creates virtual shards with range partitioning
- `ddp_drop_vs()`: Removes virtual shards
- `ddp_add_vs_in_shard()`: Adds virtual shards to worker nodes using `dblink`
- `ddp_tables_distribution()`: Reports table distribution information

## Creating a basic DDP Sharded Cluster

Create the SGShardedCluster resource:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: cluster
spec:
  type: ddp
  database: mydatabase
  postgres:
    version: '15'
  coordinator:
    instances: 2
    pods:
      persistentVolume:
        size: '10Gi'
  workers:
    clusters: 4
    instancesPerCluster: 2
    pods:
      persistentVolume:
        size: '10Gi'
```

This configuration will create a coordinator with 2 Pods and 4 workers with 2 Pods each. The coordinator uses `postgres_fdw` to connect to the worker nodes and route queries.

## Distributed Restore Points

DDP supports creating distributed restore points across all workers using two-phase commit (2PC). This allows consistent point-in-time recovery across the entire sharded cluster:

```sql
SELECT ddp_create_restore_point('my_restore_point');
```

## Key Differences from Citus

| Feature | DDP | Citus |
|---------|-----|-------|
| **Implementation** | SQL-only using PostgreSQL core features | PostgreSQL extension |
| **Dependencies** | None (uses `postgres_fdw`, `dblink`, partitioning) | Citus extension |
| **Query routing** | PostgreSQL partition pruning and FDW | Citus distributed query engine |
| **Data distribution** | Range-based virtual shards | Hash-based distribution |
| **Coordinator** | Standard PostgreSQL with FDW | PostgreSQL with Citus extension |
