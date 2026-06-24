---
title: Initializing data with SQL scripts
weight: 8
url: /cookbook/creating-clusters/bootstrap-sql-scripts
description: Run SQL at cluster bootstrap to create users, databases, and schema.
showToc: true
---

## What it does

Attaches one or more [SGScript]({{% relref "06-crd-reference/10-sgscript" %}}) resources to an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) via `spec.managedSql.scripts`. Each
entry in that list names an SGScript; the operator runs every script entry in the referenced
SGScript exactly once, in sequence, and tracks what has already run so it is never repeated.
This is how you provision users, databases, extensions, and schema at bootstrap — or add more
later without re-creating the cluster.

## When to use it

- You need a database user, a database, or initial schema to exist before the application
  connects for the first time.
- You want to version-control SQL initialization alongside the cluster manifest.
- You need to add or change initialization SQL on a running cluster without a restart.

## How to do it

### 1. Create the SGScript

The SGScript holds one or more SQL statements. Each entry runs as the `postgres` superuser in
auto-commit mode. Use `script` for short inline SQL; use `scriptFrom.secretKeyRef` to keep
credentials out of plain-text manifests.

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  namespace: my-cluster
  name: cluster-init
spec:
  scripts:
    # Entry 1 — create an application database and user inline
    - name: create-appdb
      database: postgres
      script: |
        CREATE DATABASE appdb;
        CREATE USER appuser WITH PASSWORD 'changeme';
        GRANT ALL PRIVILEGES ON DATABASE appdb TO appuser;

    # Entry 2 — load schema DDL from a Secret (keeps credentials out of plain text)
    - name: apply-schema
      database: appdb
      scriptFrom:
        secretKeyRef:
          name: appdb-schema-secret
          key: schema.sql
```

```bash
kubectl apply -f sgscript.yaml
```

To store the DDL in a Secret:

```bash
kubectl create secret generic appdb-schema-secret \
  --from-file=schema.sql=./schema.sql \
  -n my-cluster
```

### 2. Reference the SGScript from the SGCluster

Add `spec.managedSql` to the cluster manifest and list the SGScript by name:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 1
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 10Gi
  managedSql:
    # Allow the cluster to keep starting even if a non-critical script fails.
    continueOnSGScriptError: false
    scripts:
      - sgScript: cluster-init
```

```bash
kubectl apply -f cluster.yaml
```

## How it works

After the cluster is bootstrapped and Patroni has elected a primary, the operator's
cluster-controller reconciles `spec.managedSql`. For each entry in `scripts` it fetches the
referenced SGScript, iterates its `scripts` list in order, and executes any entry whose `id`
has not yet been recorded as completed in the cluster status. Execution state is stored in
`status.managedSql` so reruns after a pod restart do not re-execute completed entries.

When `continueOnSGScriptError` is `false` (the default), a failing script entry halts
execution of all subsequent entries in that SGScript and prevents later SGScript references
from running. Setting it to `true` logs the error and continues with the next entry.

Because tracking is by entry `id`, you can add new entries to an SGScript (or add a new
SGScript reference to the cluster) at any time; the operator will pick them up during the
next reconciliation and execute only the new entries.

## What to expect

- Watch script execution progress in the cluster status:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o jsonpath='{.status.managedSql}'
  ```

- Completed entries show a `failed: false` status; failed entries show `failed: true` with
  an error message.
- A new script entry is executed within seconds of the SGScript being updated and the
  operator completing its next reconciliation loop.

## Pitfalls

- **Scripts run exactly once per entry `id`.** To rerun a script, add a new entry (or change
  the entry's `id`). Editing the SQL text of an existing entry without changing its `id` has
  no effect on entries that have already been marked as executed.
- **A failing script blocks bootstrap by default.** If `continueOnSGScriptError` is `false`
  and an early entry fails, subsequent entries — including those that create users the
  application needs — never run. Either fix the failing script or set
  `continueOnSGScriptError: true` for non-critical initialization.
- **Store credentials in a Secret, not inline.** Using `scriptFrom.secretKeyRef` prevents
  passwords and tokens from appearing in the SGScript manifest, in `kubectl get` output, and
  in audit logs. Never embed credentials in the `script` field.
- **SGScript must be in the same namespace as the SGCluster.** Cross-namespace references
  are not supported; the operator looks up the SGScript by name within the cluster's
  namespace.
- **`script` and `scriptFrom` are mutually exclusive.** Each entry in `spec.scripts` must
  use one or the other, not both. Similarly, within `scriptFrom`, exactly one of
  `secretKeyRef` or `configMapKeyRef` is required.
