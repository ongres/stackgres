---
title: Running VACUUM
weight: 5
url: /cookbook/running-operations/vacuum
description: Run a VACUUM operation against a cluster.
showToc: true
---

## What it does

Runs PostgreSQL [VACUUM](https://www.postgresql.org/docs/current/sql-vacuum.html) across
all or a selected subset of databases in an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}), using an
[SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) with `op: vacuum`. You can control
whether to run a full rewrite (`full`), aggressive tuple freezing (`freeze`), statistics
collection (`analyze`), and page-skipping behavior (`disablePageSkipping`). Each database
entry can override any of those flags independently.

## When to use it

- Table bloat has grown noticeably and autovacuum has not kept up.
- You need to freeze tuple transaction IDs ahead of an XID-wraparound risk.
- You want to refresh planner statistics across all databases in one pass.
- You want a controlled, operator-managed run rather than triggering VACUUM manually
  through a psql session.

## How to do it

Create an SGDbOps resource and apply it. The simplest form vacuums and analyzes every
database with the operator defaults:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: vacuum-all
spec:
  sgCluster: cluster   # name of the target SGCluster
  op: vacuum
```

```bash
kubectl apply -f vacuum-all.yaml
```

To vacuum only specific databases, or to override flags per database:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: vacuum-selective
spec:
  sgCluster: cluster
  op: vacuum
  vacuum:
    # Global defaults; each database entry can override these
    analyze: true              # update planner statistics (default: true)
    freeze: false              # aggressive tuple freezing (default: false)
    full: false                # rewrite tables to reclaim space (default: false)
    disablePageSkipping: false # ignore visibility-map page skipping (default: false)
    databases:
    - name: app
      full: false
      analyze: true
    - name: reporting
      freeze: true             # pre-freeze this database ahead of XID wraparound
      analyze: false
```

```bash
kubectl apply -f vacuum-selective.yaml
```

Watch progress and read the outcome:

```bash
kubectl get sgdbops -n my-cluster vacuum-selective -o yaml
```

## How it works

The operator creates a Job pod that connects to the primary and issues VACUUM against each
target database in turn. If `spec.vacuum.databases` is not set, all databases in the
cluster are vacuumed using the global flags. Per-database entries in
`spec.vacuum.databases[].name` override the global flags for that database only. After the
Job completes the resource stays in the cluster as a record; delete it when no longer
needed.

See the full field reference at {{% relref "06-crd-reference/08-sgdbops" %}}.

## What to expect

- The operation is non-blocking for a standard VACUUM: reads and writes continue normally
  on the table being processed.
- `analyze: true` (the default) updates `pg_statistic` so the planner can produce better
  query plans immediately after the run.
- Check the `status.conditions` on the SGDbOps for `Running`, `Completed`, or `Failed`.

## Pitfalls

- **VACUUM FULL takes an exclusive lock.** Setting `full: true` rewrites the entire table
  and holds an `ACCESS EXCLUSIVE` lock for the duration. All reads and writes on the table
  are blocked. Avoid on large or heavily-used tables during business hours; prefer the
  default `full: false` and rely on autovacuum for routine maintenance.
- **VACUUM FULL requires extra disk space.** It writes a new copy of the table before
  releasing the old one. Ensure sufficient free space before using it.
- **`disablePageSkipping` is for corruption recovery only.** Using it unnecessarily slows
  down the operation significantly, as every page is read regardless of the visibility map.
- **SGDbOps is a one-shot resource.** Once completed it cannot be re-triggered by editing
  it. Delete and recreate the resource to run the operation again.
