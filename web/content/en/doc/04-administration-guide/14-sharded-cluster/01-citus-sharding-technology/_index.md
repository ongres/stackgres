---
title: Citus sharding technology
weight: 1
url: /doc/latest/administration/sharded-cluster/citus
description: Details about citus sharding technology.
showToc: true
---

## Citus Use Cases

### Multi-Tenant

The multi-tenant architecture uses hierarchical database modeling to distribute queries across nodes. The tenant ID is stored in a column on each table, and Citus routes queries to the appropriate worker node.

**Best practices:**
- Partition distributed tables by a common tenant_id column
- Convert small cross-tenant tables to reference tables
- Ensure all queries filter by tenant_id

### Real-Time Analytics

Real-time architectures depend on specific distribution properties to achieve highly parallel processing.

**Best practices:**
- Choose a column with high cardinality as the distribution column
- Choose a column with even distribution to avoid skewed data
- Distribute fact and dimension tables on their common columns

### Time-Series

**Important:** Do NOT use the timestamp as the distribution column for time-series data. A hash distribution based on time distributes times seemingly at random, leading to network overhead for range queries.

**Best practices:**
- Use a different distribution column (tenant_id or entity_id)
- Use PostgreSQL table partitioning for time ranges

## Co-located Tables

Co-located tables are distributed tables that share common columns in the distribution key. This improves performance since distributed queries avoid querying more than one Postgres instance for correlated columns.

**Benefits of co-location:**
- Full SQL support for queries on a single set of co-located distributed partitions
- Multi-statement transaction support for modifications
- Aggregation through INSERT..SELECT
- Foreign keys between co-located tables
- Distributed outer joins
- Pushdown CTEs (PostgreSQL >= 12)

Example:
```sql
SELECT create_distributed_table('event', 'tenant_id');
SELECT create_distributed_table('page', 'tenant_id', colocate_with => 'event');
```

## Reference Tables

Reference tables are replicated across all worker nodes and automatically kept in sync during modifications. Use them for small tables that need to be joined with distributed tables.

```sql
SELECT create_reference_table('geo_ips');
```

## Scaling Workers

Adding a new worker is simple - increase the `clusters` field value in the `workers` section:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: my-sharded-cluster
spec:
  workers:
    clusters: 3  # Increased from 2
```

After provisioning, rebalance data using the resharding operation:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: reshard
spec:
  sgShardedCluster: my-sharded-cluster
  op: resharding
  resharding:
    citus: {}
```

## Query Routers

Query routers are special workers that route read/write queries to the workers that store the sharded data, but do not store sharded table data themselves. Like the coordinator, a query router is an SGCluster that participates in the Citus topology, but its `shouldhaveshards` flag is set to `false` so the rebalancer never assigns shards to it.

Query routers allow you to scale the number of read/write entrypoints horizontally without overloading the workers that store the sharded table data. This is useful when the workload is bottlenecked on the coordinator's connection handling, the parser/planner, or the cross-shard query coordination, rather than on the workers' I/O.

> Query routers are only supported when `spec.type` is `citus`.

### Architecture

Each query router is a single-instance SGCluster created and managed by the operator alongside the coordinator and the workers. Query routers inherit their spec from the coordinator (apart from `instances`, which is fixed to one) so they share the same Postgres version, configuration, pooling and pods configuration. They participate in the Citus topology as additional Citus nodes, with a dedicated `groupid` derived from the query router index offset (see [Index Offset](#index-offset)).

Query routers do not have replicas: each query router cluster is composed of a single Pod. To scale the number of query router entrypoints, increase `coordinator.queryRouterClusters`.

### Enabling Query Routers

Set `spec.coordinator.queryRouterClusters` to the desired number of query router workers:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: cluster
spec:
  type: citus
  database: mydatabase
  coordinator:
    instances: 2
    queryRouterClusters: 2
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

The example above creates two query router SGClusters (one Pod each) in addition to the coordinator and the four workers.

### Naming

By default the query router SGClusters are named after the SGShardedCluster name with the `-router` suffix and the zero-based index appended. For example, with an SGShardedCluster called `cluster` and `queryRouterClusters: 2`, the operator creates the SGClusters `cluster-router0` and `cluster-router1`.

You can override the template via `spec.coordinator.queryRouterClusterNameTemplate`:

```yaml
spec:
  coordinator:
    queryRouterClusters: 2
    queryRouterClusterNameTemplate: my-router
```

This produces `my-router0` and `my-router1` instead. As with the workers' `clusterNameTemplate`, this field can only be set on creation.

### Index Offset

Query routers register as Citus nodes with a `groupid` starting at `1024`. This offset keeps the regular workers' group identifiers (which start at `1`) separated from the query routers' group identifiers, so adding or removing query routers never collides with the worker group numbering.

If you plan to have more than 1023 regular worker SGClusters, raise the offset accordingly with `spec.coordinator.queryRouterIndexOffset` (minimum `1024`):

```yaml
spec:
  coordinator:
    queryRouterIndexOffset: 4096
    queryRouterClusters: 2
```

### Connecting Through Query Routers

Each query router SGCluster exposes a Kubernetes Service named after the SGCluster (for example `cluster-router0`). Applications can connect through any of these services to issue read/write queries; the query router will forward the query to the appropriate worker.

You can enable, disable or customize the type of the query router primary services centrally through `spec.postgresServices.coordinator.queryRouters`:

```yaml
spec:
  postgresServices:
    coordinator:
      queryRouters:
        type: LoadBalancer
        enabled: true
```

This setting is propagated to the primary Service of every query router SGCluster. Replicas Services on query router SGClusters are always disabled because query routers are single-instance clusters.

### Overriding Specific Query Routers

Like worker overrides, you can override individual query router clusters via `spec.workers.overrides` by setting `type: QueryRouter` on the override entry. The `index` (or `indexes`) refers to the zero-based query router identifier (i.e. `0` selects the first query router), not the offset Citus group identifier.

See [Cluster Names and Overrides]({{% relref "04-administration-guide/14-sharded-cluster/04-cluster-names-and-overrides" %}}) for the full reference of the `index`, `indexes` and `type` fields.

### Scaling Query Routers

Add or remove query routers by changing `spec.coordinator.queryRouterClusters`:

```bash
kubectl patch sgshardedcluster my-sharded-cluster --type merge \
  -p '{"spec":{"coordinator":{"queryRouterClusters":3}}}'
```

Query routers do not store sharded data, so adding or removing them does not require resharding. The operator updates the Citus node table automatically and a scheduled job (registered via `pg_cron`) keeps the `shouldhaveshards` flag of the query router nodes set to `false`.

## Distributed Partitioned Tables

Citus allows creating partitioned tables that are also distributed for time-series workloads. With partitioned tables, removing old historical data is fast and doesn't generate bloat:

```sql
CREATE TABLE github_events (
  event_id bigint,
  event_type text,
  repo_id bigint,
  created_at timestamp
) PARTITION BY RANGE (created_at);

SELECT create_distributed_table('github_events', 'repo_id');

SELECT create_time_partitions(
  table_name         := 'github_events',
  partition_interval := '1 month',
  end_at             := now() + '12 months'
);
```

## Columnar Storage

Citus supports columnar storage for distributed partitioned tables. This append-only format can greatly reduce data size and improve query performance, especially for numerical values:

```sql
CALL alter_old_partitions_set_access_method(
  'github_events',
  '2015-01-01 06:00:00' /* older_than */,
  'columnar'
);
```

> **Note:** Columnar storage disallows updating and deleting rows, but you can still remove entire partitions.

## Creating a basic Citus Sharded Cluster

Create the SGShardedCluster resource:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: cluster
spec:
  type: citus
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

This configuration will create a coordinator with 2 Pods and 4 workers with 2 Pods each.

By default the coordinator node has a synchronous replica to avoid losing any metadata that could break the sharded cluster.

The workers are where sharded data lives and have a replica in order to provide high availability to the cluster.

![SG Sharded Cluster](SG_Sharded_Cluster.png "StackGres-Sharded_Cluster")

After all the Pods are Ready you can view the topology of the newly created sharded cluster by issuing the following command:

```
kubectl exec -n my-cluster cluster-coord-0 -c patroni -- patronictl list
+ Citus cluster: cluster --+------------------+--------------+---------+----+-----------+
| Group | Member           | Host             | Role         | State   | TL | Lag in MB |
+-------+------------------+------------------+--------------+---------+----+-----------+
|     0 | cluster-coord-0  | 10.244.0.16:7433 | Leader       | running |  1 |           |
|     0 | cluster-coord-1  | 10.244.0.34:7433 | Sync Standby | running |  1 |         0 |
|     1 | cluster-worker0-0 | 10.244.0.19:7433 | Leader       | running |  1 |           |
|     1 | cluster-worker0-1 | 10.244.0.48:7433 | Replica      | running |  1 |         0 |
|     2 | cluster-worker1-0 | 10.244.0.20:7433 | Leader       | running |  1 |           |
|     2 | cluster-worker1-1 | 10.244.0.42:7433 | Replica      | running |  1 |         0 |
|     3 | cluster-worker2-0 | 10.244.0.22:7433 | Leader       | running |  1 |           |
|     3 | cluster-worker2-1 | 10.244.0.43:7433 | Replica      | running |  1 |         0 |
|     4 | cluster-worker3-0 | 10.244.0.27:7433 | Leader       | running |  1 |           |
|     4 | cluster-worker3-1 | 10.244.0.45:7433 | Replica      | running |  1 |         0 |
+-------+------------------+------------------+--------------+---------+----+-----------+
```

You may also check that they are already configured in Citus by running the following command:

```
$ kubectl exec -n my-cluster cluster-coord-0 -c patroni -- psql -d mydatabase -c 'SELECT * FROM pg_dist_node'
 nodeid | groupid |  nodename   | nodeport | noderack | hasmetadata | isactive | noderole | nodecluster | metadatasynced | shouldhaveshards 
--------+---------+-------------+----------+----------+-------------+----------+----------+-------------+----------------+------------------
      1 |       0 | 10.244.0.34 |     7433 | default  | t           | t        | primary  | default     | t              | f
      3 |       2 | 10.244.0.20 |     7433 | default  | t           | t        | primary  | default     | t              | t
      2 |       1 | 10.244.0.19 |     7433 | default  | t           | t        | primary  | default     | t              | t
      4 |       3 | 10.244.0.22 |     7433 | default  | t           | t        | primary  | default     | t              | t
      5 |       4 | 10.244.0.27 |     7433 | default  | t           | t        | primary  | default     | t              | t
(5 rows)
```

Please, take into account that the `groupid` column of the `pg_dist_node` table is the same as the Patroni Group column above. In particular, the group with identifier 0 is the coordinator group (coordinator have `shouldhaveshards` column set to `f`).

For a more complete configuration please have a look at [Create Citus Sharded Cluster Section]({{% relref "04-administration-guide/14-sharded-cluster/01-citus-sharding-technology/12-sharded-cluster-creation" %}}).