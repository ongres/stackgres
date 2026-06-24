---
title: Applying a custom Postgres configuration
weight: 6
url: /cookbook/operating-clusters/custom-postgres-config
description: Author an SGPostgresConfig and apply tuned postgresql.conf settings to a cluster.
showToc: true
---

## What it does

Creates an [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) resource
that holds a set of `postgresql.conf` parameters, then attaches it to a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) via
`spec.configurations.sgPostgresConfig`. The operator reconciles the change without
recreating the cluster; parameters that require a server restart are applied on the next
controlled restart.

## When to use it

- You need to tune memory settings (`shared_buffers`, `work_mem`, `effective_cache_size`)
  to match the hardware your cluster runs on.
- You want to enable or configure extensions or logging settings that differ from the
  operator defaults.
- You are applying workload-specific tuning (OLTP vs. analytics, connection-heavy vs.
  batch) without recreating the cluster.

## How to do it

### 1. Create the SGPostgresConfig

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  namespace: my-cluster
  name: custom-pgconfig
spec:
  # Must match the major version of the target cluster exactly.
  postgresVersion: "16"
  postgresql.conf:
    shared_buffers: "512MB"
    work_mem: "16MB"
    effective_cache_size: "1536MB"
    checkpoint_completion_target: "0.9"
    log_min_duration_statement: "1000"
```

```bash
kubectl apply -f custom-pgconfig.yaml
```

### 2. Attach it to the cluster

Patch (or update) the `SGCluster` to reference the new config:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  postgres:
    version: "16"
  pods:
    persistentVolume:
      size: 10Gi
  configurations:
    sgPostgresConfig: custom-pgconfig   # name of the SGPostgresConfig above
```

```bash
kubectl apply -f cluster.yaml
```

### 3. Restart if required

Parameters that require a Postgres restart (for example `shared_buffers`) leave the
cluster in a `pending-restart` state. Apply a controlled rolling restart via `SGDbOps`:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: restart-after-pgconfig
spec:
  sgCluster: cluster
  op: restart
  restart:
    method: InPlace
```

```bash
kubectl apply -f restart.yaml
kubectl wait sgdbops -n my-cluster restart-after-pgconfig \
  --for=condition=Completed --timeout=300s
```

## How it works

The `SGPostgresConfig` is a standalone resource. The operator reads the
`postgresql.conf` map from its `spec` and merges the values into the configuration it
generates for each Postgres instance. Parameters that can be reloaded without a restart
(for example `work_mem`, `log_min_duration_statement`) take effect on the next
reconciliation cycle; parameters that require a restart are staged and the cluster status
reflects that a restart is pending. The `SGDbOps` restart operation performs a rolling
restart — one instance at a time — so the cluster stays available throughout.

## What to expect

- After applying the `SGCluster` patch, verify the config is linked:

  ```bash
  kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.spec.configurations.sgPostgresConfig}'
  ```

- Check whether a restart is pending:

  ```bash
  kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.status.conditions}' | grep PendingRestart
  ```

- For parameters that reload without restart, changes propagate within one reconciliation
  cycle (typically a few seconds).

## Pitfalls

- **`postgresVersion` must match the cluster major version.** The operator rejects or
  ignores an `SGPostgresConfig` whose `postgresVersion` does not match the major version
  of the `SGCluster` it is attached to. Always keep them in sync.
- **Some parameters are managed by StackGres and cannot be overridden.** The operator
  controls settings such as `listen_addresses`, `port`, `data_directory`, and several
  Patroni-required parameters. Values you set for those fields are silently ignored; the
  operator's value wins.
- **Restart-requiring changes need an explicit `SGDbOps` restart.** The operator does not
  restart Postgres automatically when a parameter change requires it. The cluster enters a
  `pending-restart` condition; schedule an `SGDbOps` restart to apply those changes with
  minimal downtime.
- **Deleting an `SGPostgresConfig` while it is in use is blocked.** The validating webhook
  prevents deletion of a config that is still referenced by a cluster. Remove the
  reference from `SGCluster` first, then delete the resource.
