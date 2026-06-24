---
title: Configuring connection pooling
weight: 7
url: /cookbook/operating-clusters/connection-pooling
description: Author an SGPoolingConfig to tune PgBouncer for a cluster.
showToc: true
---

## What it does

Defines a named
[SGPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}})
resource that holds PgBouncer settings, then attaches it to an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) via
`spec.configurations.sgPoolingConfig`. The operator reconciles the change in place —
no cluster re-creation required.

## When to use it

- You need to cap the number of server connections PostgreSQL sees, or you want to
  control how connections are pooled (session, transaction, or statement mode).
- You want to tune per-database or per-user PgBouncer parameters independently of
  cluster topology.
- You want to disable the built-in pooler entirely for a cluster where the
  application manages its own connection pooling.

## How to do it

### Create the SGPoolingConfig

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  namespace: my-cluster
  name: pgbouncerconf
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:                  # [pgbouncer] section
        pool_mode: transaction    # session | transaction | statement
        max_client_conn: "200"    # max connections from clients
        default_pool_size: "20"   # server connections per user/database pair
        server_idle_timeout: "60"
      databases:                  # [databases] section (optional)
        mydb:
          pool_size: "10"         # override pool size for a specific database
      users:                      # [users] section (optional)
        myapp:
          pool_mode: session      # override pool mode for a specific user
```

```bash
kubectl apply -f pgbouncerconf.yaml
```

All values under `pgbouncer.ini.pgbouncer`, `pgbouncer.ini.databases`, and
`pgbouncer.ini.users` are string maps that map directly to the corresponding
sections of `pgbouncer.ini`. Refer to the
[PgBouncer configuration reference](https://www.pgbouncer.org/config.html) for
the full set of accepted keys.

### Attach it to the cluster

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
  pods:
    persistentVolume:
      size: 10Gi
  configurations:
    sgPoolingConfig: pgbouncerconf   # name of the SGPoolingConfig above
```

```bash
kubectl apply -f cluster.yaml
```

### Disable connection pooling

To run without any pooler sidecar, set `pods.disableConnectionPooling` on the
cluster instead of referencing an SGPoolingConfig:

```yaml
spec:
  pods:
    disableConnectionPooling: true
```

## How it works

Each cluster Pod runs a PgBouncer sidecar container. When the operator reconciles
the `SGPoolingConfig`, it regenerates the `pgbouncer.ini` from the
`spec.pgBouncer.pgbouncer.ini` object and restarts the pooler process. The
`[pgbouncer]`, `[databases]`, and `[users]` sections are written in the order they
appear in the spec. Any parameter not set in the SGPoolingConfig falls back to the
operator's default, which can be inspected in `status.pgBouncer.defaultParameters`
on the resource after the first reconciliation.

If `spec.configurations.sgPoolingConfig` is not set on the cluster, the operator
creates and uses a default SGPoolingConfig automatically.

## What to expect

- Check that the operator has reconciled the new config:

  ```bash
  kubectl get sgpoolingconfig -n my-cluster pgbouncerconf -o yaml
  ```

- Verify the pooler is healthy on each Pod:

  ```bash
  kubectl exec -n my-cluster cluster-0 -c envoy -- \
    psql -p 6432 -U postgres pgbouncer -c "SHOW POOLS;"
  ```

- Changing a parameter triggers a rolling restart of the PgBouncer sidecar;
  in-flight client connections are briefly interrupted.

## Pitfalls

- **`pool_mode: transaction` breaks session-level features.** Named prepared
  statements, advisory locks, `SET LOCAL`, and other session-scoped state do not
  survive across transactions when pooling is in transaction mode. Applications that
  rely on these features must use `pool_mode: session` — or disable pooling
  altogether.
- **Applying changes briefly cycles the pooler.** The operator restarts the
  PgBouncer process on each Pod when the config changes. Short client-side retry
  logic is strongly recommended.
- **`disableConnectionPooling` is per-cluster.** Setting it removes the PgBouncer
  sidecar from every Pod in that cluster; a referenced `sgPoolingConfig` is
  ignored when pooling is disabled.
- **String values only.** All PgBouncer parameters must be quoted strings even
  when the underlying type is numeric (e.g. `max_client_conn: "200"`).
