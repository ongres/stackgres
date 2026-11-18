---
title: Repack
weight: 3
url: /administration/database-operations/repack
description: How to run pg_repack operations on StackGres clusters.
showToc: true
---

[pg_repack](https://github.com/reorg/pg_repack) is a PostgreSQL extension that removes bloat from tables and indexes without holding exclusive locks for extended periods. Unlike `VACUUM FULL`, which locks the table for the entire duration, pg_repack can reorganize tables online. StackGres allows you to run repack operations declaratively through [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}).

## When to Use Repack vs Vacuum

- **Vacuum**: Reclaims dead tuple space without rewriting the table. Fast, minimal locking, but does not reduce table size on disk (except `VACUUM FULL`).
- **Repack**: Rewrites the table to eliminate bloat while allowing reads and writes to continue. Preferred when you need to reclaim disk space with minimal downtime.
- **Vacuum Full**: Rewrites the table like repack but holds an exclusive lock for the entire duration. Use only when repack is not an option.

## Basic Repack

Run a repack on all databases:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: repack-all
spec:
  sgCluster: my-cluster
  op: repack
  repack: {}
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `noOrder` | boolean | `false` | If `true`, performs a vacuum full instead of a cluster (reorder by index). |
| `waitTimeout` | string | not set | ISO 8601 duration (e.g. `PT30S`) to set a timeout to cancel other backends on conflict. |
| `noKillBackend` | boolean | `false` | If `true`, does not kill other backends when timed out. |
| `noAnalyze` | boolean | `false` | If `true`, skips the analyze step at the end of the repack. |
| `excludeExtension` | boolean | `false` | If `true`, skips tables belonging to a specific extension. |
| `databases` | array | all databases | List of specific databases to repack. Omit to repack all databases. |

## Repack with Timeout

When other sessions hold locks on the tables being repacked, you can set a timeout to cancel conflicting backends:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: repack-timeout
spec:
  sgCluster: my-cluster
  op: repack
  repack:
    waitTimeout: PT2M
    noKillBackend: false
```

Setting `waitTimeout` to `PT2M` (2 minutes) means pg_repack will wait up to 2 minutes for conflicting backends before canceling them. Set `noKillBackend: true` if you want to avoid canceling other backends when the timeout is reached.

## Targeting Specific Databases

You can target specific databases and apply different options per database:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: repack-targeted
spec:
  sgCluster: my-cluster
  op: repack
  repack:
    databases:
    - name: app_production
      waitTimeout: PT1M
      noKillBackend: true
    - name: app_analytics
      noAnalyze: true
```

When the `databases` field is omitted, the repack operation runs against all databases in the cluster.

## Monitoring the Operation

After creating the SGDbOps resource, you can monitor the progress:

```
kubectl get sgdbops repack-all -w
```

The operation status is tracked in `SGDbOps.status.conditions`. When the operation completes successfully, the status will show `Completed`.

## Related Documentation

- [SGDbOps CRD Reference]({{% relref "06-crd-reference/08-sgdbops" %}})
- [Vacuum Operation]({{% relref "04-administration-guide/06-database-operations/02-vacuum" %}})
