---
title: Scaling Sharded Clusters
weight: 14
url: /administration/sharded-cluster/scaling
description: How to scale sharded clusters by adding workers, replicas, or changing resources.
showToc: true
---

This guide covers scaling operations for SGShardedCluster, including horizontal scaling (adding workers or replicas) and vertical scaling (changing resources).

## Scaling Overview

SGShardedCluster supports multiple scaling dimensions:

| Dimension | Component | Configuration |
|-----------|-----------|---------------|
| **Horizontal - Workers** | Number of worker clusters | `spec.workers.clusters` |
| **Horizontal - Replicas** | Replicas per worker | `spec.workers.instancesPerCluster` |
| **Horizontal - Coordinators** | Coordinator instances | `spec.coordinator.instances` |
| **Horizontal - Query routers** (Citus only) | Number of query router clusters | `spec.coordinator.queryRouterClusters` |
| **Vertical** | CPU/Memory | `spec.coordinator/workers.sgInstanceProfile` |

## Adding Workers

To add more worker clusters, increase the `clusters` value:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: my-sharded-cluster
spec:
  workers:
    clusters: 5  # Increased from 3 to 5
    instancesPerCluster: 2
    pods:
      persistentVolume:
        size: 50Gi
```

Apply the change:

```bash
kubectl apply -f sgshardedcluster.yaml
```

Or patch directly:

```bash
kubectl patch sgshardedcluster my-sharded-cluster --type merge \
  -p '{"spec":{"workers":{"clusters":5}}}'
```

### What Happens When Adding Workers

1. New worker clusters are created with the specified configuration
2. Each new worker gets the configured number of replicas
3. For Citus: New workers are registered with the coordinator
4. Data is **not** automatically rebalanced to new workers

### Rebalancing Data (Citus)

After adding workers, use SGShardedDbOps to rebalance data:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: rebalance-after-scale
spec:
  sgShardedCluster: my-sharded-cluster
  op: resharding
  resharding:
    citus:
      threshold: 0.1  # Rebalance if utilization differs by 10%
```

## Adding Replicas

To increase replicas per worker for better read scalability:

```yaml
spec:
  workers:
    clusters: 3
    instancesPerCluster: 3  # Increased from 2 to 3
```

Or patch:

```bash
kubectl patch sgshardedcluster my-sharded-cluster --type merge \
  -p '{"spec":{"workers":{"instancesPerCluster":3}}}'
```

### Replica Considerations

- New replicas are created from the primary via streaming replication
- Initial sync may take time depending on data size
- Consider replication mode (`sync` vs `async`) for consistency requirements

## Scaling Coordinators

Scale coordinator instances for high availability:

```yaml
spec:
  coordinator:
    instances: 3  # Increased from 2 to 3
```

### Coordinator Scaling Notes

- Minimum recommended: 2 instances for HA
- Coordinators handle metadata and query routing
- All coordinators can handle read/write queries

## Scaling Query Routers (Citus only)

For Citus sharded clusters, you can horizontally scale the number of read/write entrypoints by adding query router SGClusters. Query routers do not store sharded data — they route queries to the workers — so they can be added or removed without resharding.

Add or remove query routers by changing `spec.coordinator.queryRouterClusters`:

```yaml
spec:
  coordinator:
    instances: 2
    queryRouterClusters: 3  # Increased from 1 to 3
```

Or patch directly:

```bash
kubectl patch sgshardedcluster my-sharded-cluster --type merge \
  -p '{"spec":{"coordinator":{"queryRouterClusters":3}}}'
```

Each new query router is created as a single-instance SGCluster named `<cluster-name>-router<index>` (or following the configured `queryRouterClusterNameTemplate`). The operator registers it in the Citus topology with `shouldhaveshards` set to `false` so the rebalancer never assigns workers to it.

For configuration details and connection guidance see [Query Routers]({{% relref "04-administration-guide/14-sharded-cluster/01-citus-sharding-technology#query-routers" %}}).

## Vertical Scaling

### Using Instance Profiles

First, create an SGInstanceProfile with desired resources:

```yaml
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  name: large-profile
spec:
  cpu: "4"
  memory: "16Gi"
```

Then reference it in the sharded cluster:

```yaml
spec:
  coordinator:
    sgInstanceProfile: large-profile
  workers:
    sgInstanceProfile: large-profile
```

### Different Profiles for Coordinators and Workers

```yaml
spec:
  coordinator:
    sgInstanceProfile: coordinator-profile  # Smaller, query routing
  workers:
    sgInstanceProfile: worker-profile        # Larger, data storage
```

### Applying Vertical Scaling

Vertical scaling requires a restart. Use SGShardedDbOps for controlled rolling restart:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: apply-new-profile
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  restart:
    method: ReducedImpact
    onlyPendingRestart: true
```

## Autoscaling

SGShardedCluster supports automatic scaling based on metrics.

### Horizontal Autoscaling (KEDA)

Enable connection-based horizontal scaling:

```yaml
spec:
  coordinator:
    autoscaling:
      mode: horizontal
      horizontal:
        minInstances: 2
        maxInstances: 5
        # Scale based on active connections
        cooldownPeriod: 300
        pollingInterval: 30
  workers:
    autoscaling:
      mode: horizontal
      horizontal:
        minInstances: 1
        maxInstances: 3
```

### Vertical Autoscaling (VPA)

Enable CPU/memory recommendations:

```yaml
spec:
  coordinator:
    autoscaling:
      mode: vertical
      vertical:
        # VPA will recommend resource adjustments
  workers:
    autoscaling:
      mode: vertical
```

## Scale-Down Operations

### Reducing Workers

Reducing the number of workers requires data migration:

1. **For Citus**: Drain workers before removal:
```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: drain-workers
spec:
  sgShardedCluster: my-sharded-cluster
  op: resharding
  resharding:
    citus:
      drainOnly: true
```

2. After draining, reduce the cluster count:
```bash
kubectl patch sgshardedcluster my-sharded-cluster --type merge \
  -p '{"spec":{"workers":{"clusters":3}}}'
```

### Reducing Replicas

Reducing replicas is straightforward:

```bash
kubectl patch sgshardedcluster my-sharded-cluster --type merge \
  -p '{"spec":{"workers":{"instancesPerCluster":1}}}'
```

## Monitoring Scaling Operations

### Check Cluster Status

```bash
# View overall status
kubectl get sgshardedcluster my-sharded-cluster

# Check individual worker clusters
kubectl get sgcluster -l stackgres.io/shardedcluster-name=my-sharded-cluster

# View pods
kubectl get pods -l stackgres.io/shardedcluster-name=my-sharded-cluster
```

### Check DbOps Progress

```bash
kubectl get sgshardeddbops rebalance-after-scale -o yaml
```

## Best Practices

1. **Plan capacity ahead**: Scale before reaching limits
2. **Test in staging**: Validate scaling operations in non-production first
3. **Monitor during scaling**: Watch metrics during scale operations
4. **Use ReducedImpact**: For vertical scaling, use reduced impact restarts
5. **Backup before major changes**: Create a backup before significant scaling
6. **Rebalance after adding workers**: Data doesn't automatically redistribute
