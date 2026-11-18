---
title: Sharded Database Operations
weight: 16
url: /administration/sharded-cluster/database-operations
description: Day-2 operations for sharded clusters using SGShardedDbOps.
showToc: true
---

SGShardedDbOps allows you to perform day-2 database operations on sharded clusters, including restarts, resharding, and security upgrades.

> The `restart` and `securityUpgrade` operations are logically equivalent since the SGShardedCluster version is updated on any restart. These operations can also be performed without creating an SGShardedDbOps by using the [rollout]({{% relref "04-administration-guide/11-rollout" %}}) functionality, which allows the operator to automatically roll out Pod updates based on the cluster's update strategy.

## Available Operations

| Operation | Description | Use Case |
|-----------|-------------|----------|
| `restart` | Rolling restart of all pods | Apply configuration changes, clear memory |
| `resharding` | Rebalance data across shards | After adding shards, optimize distribution |
| `securityUpgrade` | Upgrade security patches | Apply security fixes |

## Restart Operation

### Basic Restart

Restart all pods in the sharded cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: cluster-restart
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
```

### Restart Methods

#### InPlace Restart

Restarts pods without creating additional replicas. Faster but may cause brief unavailability:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: inplace-restart
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  restart:
    method: InPlace
```

#### ReducedImpact Restart

Creates a new replica before restarting each pod, minimizing impact:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: reduced-impact-restart
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  restart:
    method: ReducedImpact
```

### Restart Only Pending

Restart only pods that require a restart (e.g., after configuration change):

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: pending-restart
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  restart:
    method: ReducedImpact
    onlyPendingRestart: true
```

## Resharding Operation (Citus)

Resharding rebalances data distribution across shards. This is essential after adding new shards.

### Basic Resharding

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: rebalance-shards
spec:
  sgShardedCluster: my-sharded-cluster
  op: resharding
  resharding:
    citus:
      threshold: 0.1  # Rebalance if nodes differ by 10% in utilization
```

### Threshold Configuration

The `threshold` determines when rebalancing occurs based on utilization difference:

| Threshold | Behavior |
|-----------|----------|
| `0.0` | Always rebalance (aggressive) |
| `0.1` | Rebalance if >10% difference |
| `0.2` | Rebalance if >20% difference |
| `1.0` | Never rebalance |

### Drain-Only Mode

Move all data off specific shards before removal:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: drain-shards
spec:
  sgShardedCluster: my-sharded-cluster
  op: resharding
  resharding:
    citus:
      drainOnly: true
```

### Custom Rebalance Strategy

Use a specific Citus rebalance strategy:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: custom-rebalance
spec:
  sgShardedCluster: my-sharded-cluster
  op: resharding
  resharding:
    citus:
      threshold: 0.1
      rebalanceStrategy: by_disk_size
```

Available strategies depend on Citus version:
- `by_shard_count`: Balance number of shards (default)
- `by_disk_size`: Balance disk usage

## Security Upgrade

Apply security patches without changing PostgreSQL version:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: security-upgrade
spec:
  sgShardedCluster: my-sharded-cluster
  op: securityUpgrade
  securityUpgrade:
    method: ReducedImpact
```

### Security Upgrade Methods

- **InPlace**: Faster, brief unavailability possible
- **ReducedImpact**: Zero-downtime, creates temporary replicas

## Scheduling Operations

### Run at Specific Time

Schedule an operation for a future time:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: scheduled-restart
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  runAt: "2024-01-20T03:00:00Z"  # Run at 3 AM UTC
  restart:
    method: ReducedImpact
```

### Timeout Configuration

Set a maximum duration for the operation:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: restart-with-timeout
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  timeout: PT2H  # Fail if not completed in 2 hours
  restart:
    method: ReducedImpact
```

### Retry Configuration

Configure automatic retries on failure:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: restart-with-retry
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  maxRetries: 3
  restart:
    method: ReducedImpact
```

## Monitoring Operations

### Check Operation Status

```bash
# List all operations
kubectl get sgshardeddbops

# View detailed status
kubectl get sgshardeddbops cluster-restart -o yaml
```

### Status Fields

```yaml
status:
  conditions:
    - type: Running
      status: "True"
      reason: OperationRunning
    - type: Completed
      status: "False"
    - type: Failed
      status: "False"
  opStarted: "2024-01-15T10:00:00Z"
  opRetries: 0
  restart:
    pendingToRestartSgClusters:
      - my-sharded-cluster-shard1
    restartedSgClusters:
      - my-sharded-cluster-coord
      - my-sharded-cluster-shard0
```

### Status Conditions

| Condition | Description |
|-----------|-------------|
| `Running` | Operation is in progress |
| `Completed` | Operation finished successfully |
| `Failed` | Operation failed |
| `OperationTimedOut` | Operation exceeded timeout |

### Watch Operation Progress

```bash
kubectl get sgshardeddbops cluster-restart -w
```

## Pod Scheduling for Operations

Control where operation pods run:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: scheduled-maintenance
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  scheduling:
    nodeSelector:
      node-type: maintenance
    tolerations:
      - key: maintenance
        operator: Exists
        effect: NoSchedule
```

## Operation Examples

### Post-Scaling Rebalance

After adding shards, rebalance data:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: post-scale-rebalance
spec:
  sgShardedCluster: my-sharded-cluster
  op: resharding
  resharding:
    citus:
      threshold: 0.0  # Force rebalance
```

### Maintenance Window Restart

Schedule restart during maintenance window:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: maintenance-restart
spec:
  sgShardedCluster: my-sharded-cluster
  op: restart
  runAt: "2024-01-21T02:00:00Z"
  timeout: PT4H
  restart:
    method: ReducedImpact
    onlyPendingRestart: true
```

### Emergency Security Patch

Apply urgent security update:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: urgent-security-upgrade
spec:
  sgShardedCluster: my-sharded-cluster
  op: securityUpgrade
  securityUpgrade:
    method: InPlace  # Faster for urgent patches
```

## Canceling Operations

To cancel a running operation, delete the resource:

```bash
kubectl delete sgshardeddbops cluster-restart
```

Note: Cancellation may leave the cluster in an intermediate state. Review cluster status after cancellation.

## Best Practices

1. **Use ReducedImpact for production**: Minimizes downtime during operations
2. **Schedule during low-traffic periods**: Use `runAt` for maintenance windows
3. **Set appropriate timeouts**: Prevent operations from running indefinitely
4. **Monitor operations**: Watch progress and be ready to intervene
5. **Backup before major operations**: Create backup before resharding or upgrades
6. **Test in staging**: Validate operations in non-production first

## Related Documentation

- [SGShardedDbOps CRD Reference]({{% relref "06-crd-reference/14-sgshardeddbops" %}})
- [Cluster Rollout]({{% relref "04-administration-guide/11-rollout" %}})
- [Scaling Sharded Clusters]({{% relref "04-administration-guide/14-sharded-cluster/14-scaling" %}})
- [SGDbOps for Regular Clusters]({{% relref "06-crd-reference/08-sgdbops" %}})
