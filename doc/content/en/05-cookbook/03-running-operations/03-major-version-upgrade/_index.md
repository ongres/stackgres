---
title: Major version upgrade
weight: 3
url: /cookbook/running-operations/major-version-upgrade
description: Upgrade a cluster across major Postgres versions with pg_upgrade.
showToc: true
---

## What it does

Upgrades a running cluster to a new major Postgres version using
[`pg_upgrade`](https://www.postgresql.org/docs/current/pgupgrade.html). The operation is
expressed as an [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) resource with
`op: majorVersionUpgrade` and is reconciled by the operator. The operator runs
`pg_upgrade` on the primary instance, then re-initializes each replica from the upgraded
primary.

## When to use it

- You need to move a cluster from one Postgres major version to another (for example 15
  to 16).
- You want the operator to orchestrate the `pg_upgrade` sequence rather than running it
  by hand.
- You want to perform a dry-run check before committing to the upgrade.

## How to do it

### Step 1 — update the SGCluster target version

Set `spec.postgres.version` on the
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to the new major version.
This registers intent but does not trigger migration on its own.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  postgres:
    version: "17"   # new target major version
  # ...rest unchanged
```

```bash
kubectl apply -f cluster.yaml
```

### Step 2 — create a matching SGPostgresConfig

The target major version requires a dedicated
[SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) tuned for
that version. Create one before running the operation.

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  namespace: my-cluster
  name: pgconfig-17
spec:
  postgresVersion: "17"
  postgresql.conf: {}
```

### Step 3 — optional check run

Run a preflight check (sets `check: true`) to verify that the upgrade can proceed
without actually migrating data.

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: major-upgrade-check
spec:
  sgCluster: cluster
  op: majorVersionUpgrade
  majorVersionUpgrade:
    postgresVersion: "17"
    sgPostgresConfig: pgconfig-17
    # Perform a dry-run only; no data is migrated.
    check: true
```

```bash
kubectl apply -f major-upgrade-check.yaml
kubectl get sgdbops -n my-cluster major-upgrade-check -w
```

### Step 4 — run the upgrade

Once the check passes, create the real upgrade operation.

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: major-upgrade-to-17
spec:
  sgCluster: cluster
  op: majorVersionUpgrade
  majorVersionUpgrade:
    # Target major version; must match spec.postgres.version on the SGCluster.
    postgresVersion: "17"
    # SGPostgresConfig for the target major version (required).
    sgPostgresConfig: pgconfig-17
    # Use hard links instead of copying files — faster, but old data directory
    # is no longer usable for rollback once the upgrade starts. Default: false.
    link: false
    # Separate backup path for the new major version to avoid mixing WAL files.
    backupPath: "sgbackups/my-cluster/cluster/17"
    # Override extensions for the target version if the default resolution fails.
    # postgresExtensions:
    #   - name: postgis
    #     version: "3.4.2"
```

```bash
kubectl apply -f major-upgrade-to-17.yaml
kubectl get sgdbops -n my-cluster major-upgrade-to-17 -w
```

## How it works

The operator creates a job Pod that drives the upgrade sequence:

1. The primary instance is stopped and `pg_upgrade` is run inside an init container,
   migrating the data directory to the new major version's format.
2. With `link: true` the old data files are hard-linked into the new directory
   (near-instant, but the old cluster can no longer be used as a fallback). With
   `clone: true` reflinks are used on supported file systems. Without either option
   files are copied.
3. Once the primary is running on the new version, each replica is discarded and
   re-initialized from the upgraded primary via `pg_basebackup`.
4. The operator updates `spec.postgres.version` on the SGCluster to reflect the
   completed upgrade.

The `backupPath` field directs WAL archiving for the upgraded cluster to a separate
path so that WAL files for the old and new major versions are never mixed in the same
directory. If omitted the operator fills this in automatically.

## What to expect

- The cluster is **unavailable for writes** while the primary runs `pg_upgrade`. Expect
  a maintenance window proportional to the size of the data directory (unless `link` or
  `clone` is used, which is near-instant for the primary step).
- Replicas are rebuilt after the primary completes, which adds time proportional to the
  size of the primary data directory and network bandwidth.
- The SGDbOps transitions through conditions `Running` → `Completed` (or `Failed`).
- After completion, verify the result:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o jsonpath='{.spec.postgres.version}'
  ```

## Pitfalls

- **An SGPostgresConfig for the target major version is required.** The field
  `spec.majorVersionUpgrade.sgPostgresConfig` must reference a config whose
  `postgresVersion` matches the target major. Without it the operation will fail
  validation.
- **Extensions must be available for the target major.** If an installed extension has
  no build for the new major version, `pg_upgrade` will fail. Supply compatible versions
  via `spec.majorVersionUpgrade.postgresExtensions` or remove the extension before
  upgrading.
- **Downtime is unavoidable.** Unlike a minor version upgrade, a major upgrade cannot
  be performed as a rolling update. The cluster is fully unavailable for writes during
  the primary upgrade step.
- **Take a backup before starting.** `pg_upgrade` is not reversible in place once the
  old data directory has been replaced (especially with `link: true`). Ensure a recent
  backup exists before applying the SGDbOps.
- **`link: true` disables rollback.** Hard-linking reuses the old data files; after a
  successful upgrade those files belong to the new cluster. Do not enable `link` if you
  need to be able to fall back to the old major version from the same data directory.
- **`postgresVersion` must match the SGCluster major.** The value in
  `spec.majorVersionUpgrade.postgresVersion` must match the major version set in
  `spec.postgres.version` on the SGCluster. A mismatch is rejected by the validating
  webhook.
- **SGDbOps is single-use.** A completed (or failed) SGDbOps cannot be re-run. Create
  a new resource to retry the operation.
