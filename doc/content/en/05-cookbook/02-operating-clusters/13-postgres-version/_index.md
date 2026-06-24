---
title: Changing the Postgres version
weight: 13
url: /cookbook/operating-clusters/postgres-version
description: Understand how postgres.version interacts with version upgrades.
showToc: true
---

## What it does

Controls which Postgres version the cluster runs by setting `spec.postgres.version` on an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). A minor version change (same
major) is rolled out automatically. A major version change requires a separate
[SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) `majorVersionUpgrade` operation
— editing `spec.postgres.version` alone is not sufficient and will not initiate the
migration.

## When to use it

- You want to pin the cluster to a specific minor release, or move to the latest minor
  within the current major.
- A new Postgres minor version is available and you want to schedule the rollout.
- You are preparing a major version upgrade and need to understand the two-step process.

## How to do it

### Minor version update

Edit `spec.postgres.version` to the desired major, major.minor, or `latest` value, then
apply. The operator will restart Pods according to the configured rollout strategy.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  postgres:
    version: "16.4"   # pin to a specific minor release
  # ...rest unchanged
```

```bash
kubectl apply -f cluster.yaml
```

`version` accepts:

- `latest` — resolves to the latest supported major.minor at reconciliation time.
- A major version string, e.g. `"16"` — tracks the latest minor for that major.
- A specific `major.minor` string, e.g. `"16.4"` — pins to an exact release.

### Major version upgrade

A major version upgrade (for example from Postgres 15 to 16) must be performed via an
SGDbOps resource with `op: majorVersionUpgrade`. First update `spec.postgres.version` on
the SGCluster to the new major, then create the operation:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: upgrade-to-16
spec:
  sgCluster: cluster
  op: majorVersionUpgrade
  majorVersionUpgrade:
    postgresVersion: "16"   # must match the major in spec.postgres.version
```

```bash
kubectl apply -f upgrade.yaml
```

The operator will run `pg_upgrade` on the primary, then re-initialize each replica from
the upgraded primary.

## How it works

When `spec.postgres.version` changes, the operator updates the Pod template with a new
image that packages the requested Postgres binaries. Pod restarts are controlled by the
rollout annotation strategy on the SGCluster:

- Create a `restart` SGDbOps to trigger an orderly rolling restart.
- Annotate the SGCluster with `stackgres.io/rollout: always` to restart Pods immediately.
- Annotate with `stackgres.io/schedule` and a cron expression to defer restarts to a
  maintenance window.
- Annotate with `stackgres.io/rollout: never` to suppress automatic restarts even when
  an SGDbOps is created.

For a `majorVersionUpgrade` SGDbOps, the operator orchestrates `pg_upgrade` inside an
init container on the primary instance. Once the primary is upgraded, each replica is
re-cloned via `pg_basebackup` from the new primary. A separate backup path should be used
after the upgrade to avoid mixing WAL files from the old and new major versions (see
`spec.majorVersionUpgrade.backupPath` in the
[SGDbOps reference]({{% relref "06-crd-reference/08-sgdbops" %}})).

## What to expect

- A minor version rollout restarts Pods one at a time (rolling restart). Existing client
  connections to the primary are briefly interrupted during the primary switchover.
- A major version upgrade causes a longer maintenance window: the primary runs
  `pg_upgrade`, then each replica is rebuilt. The cluster is unavailable for writes during
  the upgrade of the primary.
- After either operation, `kubectl get sgcluster -n my-cluster cluster -o yaml` will show
  the resolved version in the status.

## Pitfalls

- **Major upgrades require SGDbOps, not just a field edit.** Changing `spec.postgres.version`
  to a new major without creating a `majorVersionUpgrade` SGDbOps leaves the cluster in an
  inconsistent state — the Pods will not restart and the data directory will not be
  migrated. Always pair the field change with the SGDbOps.
- **`postgresVersion` in SGDbOps must match the major in SGCluster.** The
  `spec.majorVersionUpgrade.postgresVersion` field must carry the same major version as
  the updated `spec.postgres.version` on the SGCluster, otherwise the operation is
  rejected by the validating webhook.
- **Take a backup before any major upgrade.** `pg_upgrade` is not reversible in place.
  Ensure a recent backup exists before starting the operation so you can restore if
  something goes wrong.
- **Extensions must be compatible with the target major.** If any installed extension does
  not have a build for the target major version, the upgrade will fail. Check extension
  availability or provide compatible versions via
  `spec.majorVersionUpgrade.toInstallPostgresExtensions` in the SGDbOps.
- **`latest` drifts on cluster recreation.** If you rely on `latest` and the cluster is
  deleted and re-created, it may resolve to a newer major than before. Pin to a major or
  explicit `major.minor` string for reproducible environments.
