---
title: Tuning
weight: 19
url: /administration/tuning
description: How to configure your system and PostgreSQL in StackGres to improve performance.
showToc: true
---

There exist multiple kinds and levels of tuning and optimization for the system and PostgreSQL. This section will try to explain all of them briefly.

## OS-Level Tuning

**Disk:**
- Disable atime for data files (PostgreSQL doesn't rely on it)

**Memory:**
- Enable huge pages for large memory allocation to boost performance

**Virtual Machines:**
- Pin resources and pre-allocate disks
- Disable `wal_recycle` and `wal_init_zero` when using COW filesystems
- Separate I/O workloads and optimize networking

## PostgreSQL Configuration Tuning

For tuning PostgreSQL configuration parameters based on usage and hardware resources, see the comprehensive guide at [PostgreSQL Configuration Tuning Guide](https://postgresqlco.nf/tuning-guide).

Key parameters to consider:
- `shared_buffers`: Typically 25% of available RAM
- `work_mem`: Per-operation memory for sorts and hashes
- `effective_cache_size`: Estimate of memory available for disk caching
- `maintenance_work_mem`: Memory for maintenance operations

## Query and Schema Optimization

Improving queries and adding indexes can greatly boost performance:

1. **Identify slow/frequent queries** - Focus on queries that are slow or called with high frequency
2. **Analyze query plans** with `EXPLAIN ANALYZE`
3. **Add appropriate indexes** based on the analysis

For a detailed guide on interpreting query plans, see [Explaining PostgreSQL EXPLAIN](https://www.timescale.com/learn/explaining-postgresql-explain).

## Connection Pooling Optimization

PgBouncer in transaction mode can significantly improve performance by multiplexing connections:

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  name: optimized-pooling
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        pool_mode: transaction
        max_client_conn: '1000'
        default_pool_size: '80'
```

> **Note:** Transaction mode may require application changes since session objects cannot be used.

## Vertical Scaling

Identify bottlenecks (CPU, Memory, Disks, Network) and scale resources incrementally:

**CPU:**
- Clock speed matters for large datasets
- Larger L3 caches improve performance
- Many fast cores help OLTP workloads

**Memory:**
- Most cost-effective upgrade with greatest impact
- OS uses available RAM to cache data aggressively
- Install as much RAM as possible upfront

**Disks:**
- NVMe or SSDs for I/O-bound workloads
- Separate WAL, data, and index storage across dedicated disks or tablespaces
- Use RAID 1 or RAID 10 for reliability and performance

**Network:**
- Faster or bonded network cards speed up base backups for large databases

## Horizontal Read Scaling

Move read-only traffic to replicas to scale reads without sharding:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  replication:
    mode: sync
    syncInstances: 1
    groups:
    - instances: 1
      role: readonly
```

Using synchronous replication allows consistent reads from replicas while freeing primary resources.

## Horizontal Write Scaling

After trying out all the other optimizations, you should consider sharding the database. Sharding is a technique that allows scaling horizontally a database by splitting its data into shards distributed among multiple database but capable of being used as a single database. This is achieved by defining a distribution key in each of the table so that the data for a specific distribution key is all contained in a single database. For more information about sharding and sharding technologies offered by StackGres see the [sharded cluster section]({{% relref "04-administration-guide/14-sharded-cluster" %}}).