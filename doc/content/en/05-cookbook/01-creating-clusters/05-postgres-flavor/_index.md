---
title: Choosing the Postgres flavor
weight: 5
url: /cookbook/creating-clusters/postgres-flavor
description: Select the Vanilla PostgreSQL or Babelfish flavor at cluster creation.
showToc: true
---

## What it does

Sets the Postgres distribution the cluster runs through `spec.postgres.flavor`. Two values
are available:

- `vanilla` *(default)* — the official [PostgreSQL](https://www.postgresql.org/) distribution.
- `babelfish` — [Babelfish for PostgreSQL](https://babelfish-for-postgresql.github.io/babelfish-for-postgresql/),
  which adds a SQL Server wire-protocol listener and T-SQL compatibility layer on top of Postgres.

See the [SGCluster reference]({{% relref "06-crd-reference/01-sgcluster" %}}) for the full
field specification.

## When to use it

- You are migrating an application from Microsoft SQL Server and want to run against a
  Postgres-backed engine that speaks T-SQL and the TDS protocol without rewriting the
  application.
- You need to evaluate SQL Server compatibility in a Kubernetes-native, operator-managed
  cluster.

If neither of these applies, leave `flavor` unset; the operator defaults to `vanilla`.

## How to do it

### Vanilla (default)

Omitting `flavor` gives you vanilla Postgres:

```bash
kubectl create namespace my-cluster
```

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  postgres:
    version: latest
    # flavor: vanilla  # this is the default; shown here for clarity
  pods:
    persistentVolume:
      size: 10Gi
```

```bash
kubectl apply -f cluster.yaml
```

### Babelfish

Babelfish requires the `babelfish-flavor` feature gate to be enabled under
`spec.nonProductionOptions.enabledFeatureGates`. Because Babelfish is currently limited to a
single instance, set `instances: 1`:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 1                         # Babelfish supports only a single instance
  postgres:
    version: latest
    flavor: babelfish
  pods:
    persistentVolume:
      size: 10Gi
  nonProductionOptions:
    enabledFeatureGates:
    - babelfish-flavor                 # required to unlock the babelfish flavor
```

```bash
kubectl apply -f cluster.yaml
```

## How it works

The operator uses `flavor` to select the correct container image at bootstrap time. For
`babelfish`, the image includes the Babelfish patches and the TDS listener that accepts SQL
Server client connections alongside the standard Postgres port. An additional NodePort
service is created (see `spec.postgresServices`) to expose the Babelfish TDS port. Because
the initialization procedure differs between the two flavors, the field is locked after the
cluster data directory is created.

## What to expect

- For a `babelfish` cluster, two ports are available: the standard Postgres port (`5432`)
  and the Babelfish TDS port (default `1433`). SQL Server clients connect to the TDS port;
  standard Postgres clients connect on the Postgres port.
- Check the cluster is ready:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o yaml
  ```

- Confirm the Babelfish port is exposed:

  ```bash
  kubectl get svc -n my-cluster
  ```

## Pitfalls

- **`flavor` is immutable.** The validating webhook rejects any attempt to change
  `spec.postgres.flavor` on a running cluster. Decide on the distribution before bootstrapping;
  to switch flavors you must create a new cluster and migrate the data.
- **`babelfish-flavor` feature gate is required.** Specifying `flavor: babelfish` without
  adding `babelfish-flavor` to `spec.nonProductionOptions.enabledFeatureGates` is rejected at
  admission time.
- **Babelfish is limited to a single instance.** The current implementation does not support
  multi-instance (HA) Babelfish clusters. Keep `instances: 1`; attempting a higher value with
  the `babelfish` flavor is unsupported.
- **Not suitable for production HA.** Because Babelfish requires the non-production feature
  gate and is constrained to a single instance, it has no automatic failover. Do not use it
  as a production primary database.
