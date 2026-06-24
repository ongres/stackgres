---
title: Running pg_repack
weight: 6
url: /cookbook/running-operations/repack
description: Reclaim bloat online with pg_repack.
showToc: true
---

## What it does

Removes table and index bloat from a running cluster without acquiring long exclusive
locks, using [`pg_repack`](https://github.com/reorg/pg_repack). The operation is
expressed as an [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) resource with
`op: repack` and is reconciled by the operator. It runs against the primary instance and
leaves the cluster fully available to read and write traffic throughout.

## When to use it

- Tables or indexes have accumulated dead tuples and free-space fragmentation that
  `VACUUM` cannot reclaim (e.g. after bulk deletes or high-churn workloads).
- You need to reclaim disk space or improve sequential-scan performance without the
  write lock that `VACUUM FULL` / `CLUSTER` would require.
- You want the operator to coordinate and observe the repack job rather than running
  `pg_repack` by hand inside a Pod.

## How to do it

Ensure the `pg_repack` extension is installed in every target database before applying
the operation.

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: repack
spec:
  # Name of the cluster to operate on.
  sgCluster: cluster
  # The operation type.
  op: repack
  repack:
    # List of databases to repack. Omit to repack all databases in the cluster.
    databases:
      - name: app
        # Skip tables that belong to an installed extension (default: false).
        excludeExtension: false
        # Do a VACUUM FULL-style rewrite instead of a CLUSTER-ordered rewrite (default: false).
        noOrder: false
        # Skip the ANALYZE pass after repacking (default: false).
        noAnalyze: false
        # Do not kill conflicting backends when waitTimeout expires (default: false).
        noKillBackend: false
        # ISO 8601 duration; timeout before cancelling conflicting backends.
        # Omit to wait indefinitely.
        waitTimeout: PT30S
```

```bash
kubectl apply -f repack.yaml
```

Watch the operation progress:

```bash
kubectl get sgdbops -n my-cluster repack -w
```

Check completion status:

```bash
kubectl describe sgdbops -n my-cluster repack
```

## How it works

The operator creates a job Pod on the primary instance. For each target database the job
runs `pg_repack` with the options derived from `spec.repack`. The tool:

1. Creates a shadow copy of each table in the same schema.
2. Applies ongoing changes from a trigger-captured log while the copy is built.
3. Swaps the original and shadow tables under a brief exclusive lock, then drops the
   original.
4. Rebuilds all associated indexes on the new physical layout.

Because only the final swap requires an exclusive lock, normal read and write traffic
continues during the bulk of the operation. Top-level fields in `spec.repack`
(`excludeExtension`, `noOrder`, `noAnalyze`, `noKillBackend`, `waitTimeout`) serve as
defaults for all databases and can be overridden per-database entry in the `databases`
list.

See the full field reference at [{{% relref "06-crd-reference/08-sgdbops" %}}].

## What to expect

- The SGDbOps transitions through conditions `Running` → `Completed` (or `Failed`).
- Elapsed time scales with table size; large tables can take minutes to hours.
- Disk usage temporarily doubles for the largest table being repacked — the shadow copy
  lives on the same volume until the swap completes.
- CPU and I/O load increase noticeably on the primary during the operation.
- After the operation, inspect the SGDbOps status for per-database results:

  ```bash
  kubectl get sgdbops -n my-cluster repack -o jsonpath='{.status}'
  ```

## Pitfalls

- **`pg_repack` extension must be pre-installed.** The extension must exist in every
  target database (`CREATE EXTENSION pg_repack`) before the operation is submitted.
  The job will fail immediately for any database that is missing it.
- **Disk space.** pg_repack needs roughly as much free space as the largest table being
  repacked (shadow copy + index rebuilds). Verify `PersistentVolumeClaim` capacity before
  running on large tables.
- **Primary-only.** The operation runs on the primary; replica instances are not touched.
  Index bloat on replicas is addressed only after the next base-backup restore or a
  `restart` operation.
- **Long-running transactions block the final swap.** If `waitTimeout` is not set,
  pg_repack waits indefinitely for conflicting backends. Set a reasonable duration and
  pair it with `noKillBackend: false` so stale connections are cleared automatically.
- **`SGDbOps` is single-use.** Once an SGDbOps completes (or fails), create a new
  resource to retry or run the operation again; editing a completed resource has no effect.
