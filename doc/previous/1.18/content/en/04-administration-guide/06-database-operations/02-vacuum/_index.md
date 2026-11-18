---
title: Vacuum
weight: 2
url: /administration/database-operations/vacuum
description: How to run vacuum operations on StackGres clusters.
showToc: true
---

PostgreSQL's [VACUUM](https://www.postgresql.org/docs/current/sql-vacuum.html) command reclaims storage occupied by dead tuples. In normal PostgreSQL operation, tuples that are deleted or obsoleted by an update are not physically removed from their table; they remain present until a VACUUM is done. StackGres allows you to run vacuum operations declaratively through [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}).

## When to Use Vacuum

- After bulk deletes or updates that leave many dead tuples
- When table bloat is consuming significant disk space
- To update planner statistics (with the `analyze` option)
- To prevent transaction ID wraparound issues (with the `freeze` option)

## Basic Vacuum

Run a vacuum with analyze (the default) on all databases:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: vacuum-all
spec:
  sgCluster: my-cluster
  op: vacuum
  vacuum:
    analyze: true
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `full` | boolean | `false` | Reclaims more space but exclusively locks the table and requires extra disk space for the rewrite. Use only when significant space must be reclaimed. |
| `freeze` | boolean | `false` | Aggressively freezes tuples. Equivalent to setting `vacuum_freeze_min_age` and `vacuum_freeze_table_age` to zero. Redundant when `full` is `true`. |
| `analyze` | boolean | `true` | Updates planner statistics after vacuuming. |
| `disablePageSkipping` | boolean | `false` | Disables all page-skipping behavior. Only needed when the visibility map contents are suspect due to hardware or software issues causing database corruption. |
| `databases` | array | all databases | List of specific databases to vacuum. Omit to vacuum all databases. |

## Full Vacuum

A full vacuum rewrites the entire table to a new disk file, reclaiming all dead space. This is significantly slower and requires an exclusive lock on the table:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: vacuum-full
spec:
  sgCluster: my-cluster
  op: vacuum
  vacuum:
    full: true
    analyze: true
```

> A full vacuum requires extra disk space since it writes a new copy of the table before releasing the old one. Only use this when a significant amount of space needs to be reclaimed.

## Targeting Specific Databases

You can target specific databases and apply different options per database:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: vacuum-targeted
spec:
  sgCluster: my-cluster
  op: vacuum
  vacuum:
    databases:
    - name: app_production
      full: true
      analyze: true
    - name: app_analytics
      freeze: true
      analyze: true
```

When the `databases` field is omitted, the vacuum operation runs against all databases in the cluster.

## Freeze Vacuum

Use freeze to prevent transaction ID wraparound. This is equivalent to running `VACUUM FREEZE`:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: vacuum-freeze
spec:
  sgCluster: my-cluster
  op: vacuum
  vacuum:
    freeze: true
```

## Monitoring the Operation

After creating the SGDbOps resource, you can monitor the progress:

```
kubectl get sgdbops vacuum-all -w
```

The operation status is tracked in `SGDbOps.status.conditions`. When the operation completes successfully, the status will show `Completed`.

## Related Documentation

- [SGDbOps CRD Reference]({{% relref "06-crd-reference/08-sgdbops" %}})
- [Repack Operation]({{% relref "04-administration-guide/06-database-operations/03-repack" %}})
