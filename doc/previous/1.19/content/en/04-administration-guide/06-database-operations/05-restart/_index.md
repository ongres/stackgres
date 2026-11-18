---
title: Restart
weight: 5
url: /administration/database-operations/restart
description: How to perform controlled restarts on StackGres clusters.
showToc: true
---

A controlled restart of a StackGres cluster can be performed declaratively through [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}). This is useful when configuration changes require a Pod restart to take effect, or when you need to perform a rolling restart of the cluster for maintenance purposes.

> Since the SGCluster version is now updated on any restart, the `restart` and `securityUpgrade` SGDbOps operations are logically equivalent. You can also perform this operation without creating an SGDbOps by using the [rollout]({{% relref "04-administration-guide/11-rollout" %}}) functionality, which allows the operator to automatically roll out Pod updates based on the cluster's update strategy.

## When to Use

- After configuration changes that require a Pod restart (indicated by pending restart status)
- To perform a rolling restart for maintenance
- To apply changes to the underlying instance profile or Postgres configuration

## Restart Methods

The restart operation supports two methods:

| Method | Description |
|--------|-------------|
| `InPlace` | Restarts each Pod in the existing cluster one at a time. Does not require additional resources but causes longer service disruption when only a single instance is present. |
| `ReducedImpact` | Creates a new updated replica before restarting existing Pods. Requires additional resources to spawn the temporary replica but minimizes downtime. Recommended for production environments. |

## Basic Restart

Perform a rolling restart using the reduced impact method:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-cluster
spec:
  sgCluster: my-cluster
  op: restart
  restart:
    method: ReducedImpact
```

## Restart Only Pending Pods

To restart only the Pods that have pending configuration changes:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-pending
spec:
  sgCluster: my-cluster
  op: restart
  restart:
    method: ReducedImpact
    onlyPendingRestart: true
```

When `onlyPendingRestart` is set to `true`, only Pods detected as needing a restart will be restarted. By default (`false`), all Pods in the cluster are restarted.

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `method` | string | - | The restart method: `InPlace` or `ReducedImpact`. |
| `onlyPendingRestart` | boolean | `false` | If `true`, restarts only Pods that are in pending restart state. |

## Monitoring the Operation

After creating the SGDbOps resource, you can monitor the progress:

```
kubectl get sgdbops restart-cluster -w
```

The operation status is tracked in `SGDbOps.status.conditions`. When the operation completes successfully, the status will show `Completed`.

## Related Documentation

- [SGDbOps CRD Reference]({{% relref "06-crd-reference/08-sgdbops" %}})
- [Rollout Strategy]({{% relref "04-administration-guide/11-rollout" %}})
- [Instance Profile Configuration]({{% relref "04-administration-guide/04-configuration/01-instance-profile" %}})
