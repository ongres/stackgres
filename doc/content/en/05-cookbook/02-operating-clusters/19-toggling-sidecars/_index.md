---
title: Toggling sidecars
weight: 19
url: /cookbook/operating-clusters/toggling-sidecars
description: Disable optional sidecar containers in the cluster Pods.
showToc: true
---

## What it does

Controls which optional sidecar containers are included in each cluster Pod. You can
individually disable the PgBouncer connection pooler (`pods.disableConnectionPooling`),
the Prometheus metrics exporter (`spec.configurations.observability.disableMetrics`), the
`postgres-util` toolbox (`pods.disablePostgresUtil`), and the Envoy edge proxy
(`pods.disableEnvoy`). All four flags are updatable on a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) and are reconciled by the
operator without re-creating the resource.

## When to use it

- You want to reduce Pod resource usage on resource-constrained nodes by removing sidecars
  you do not need.
- Your cluster does not use an external Prometheus stack, so the metrics exporter sidecar
  is unnecessary overhead.
- You manage connection routing outside StackGres (for example, a dedicated PgBouncer
  Deployment) and want to remove the built-in pooler.
- You are running a read-only replica cluster or a standby that does not need Envoy
  metrics.
- You are troubleshooting and want to isolate Postgres behaviour from sidecar interference.

## How to do it

Patch the running cluster to disable any combination of sidecars:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  pods:
    disableConnectionPooling: true   # remove PgBouncer sidecar
    disablePostgresUtil: true        # remove postgres-util sidecar
    disableEnvoy: true               # remove Envoy sidecar
  configurations:
    observability:
      disableMetrics: true           # remove Prometheus exporter sidecar
```

```bash
kubectl apply -f cluster.yaml
```

Or patch a single field without touching the rest of the manifest:

```bash
# Disable connection pooling only
kubectl patch sgcluster cluster -n my-cluster \
  --type merge \
  -p '{"spec":{"pods":{"disableConnectionPooling":true}}}'
```

All four fields default to `false` (sidecars enabled). Setting a field to `true` removes
that sidecar; restoring `false` adds it back. Each change may trigger a rolling restart
of the StatefulSet.

> **Note:** `pods.disableMetricsExporter` also exists in the spec but is deprecated.
> Use `spec.configurations.observability.disableMetrics` instead.

## How it works

When the operator reconciles the SGCluster it rebuilds the Pod template of the managed
StatefulSet, including only the sidecar containers that are not disabled. Kubernetes then
performs a rolling restart: one Pod at a time is replaced with the new template. Patroni
ensures that no primary failover is triggered unnecessarily during the restart.

The affected sidecars and their roles:

| Sidecar | Flag | Role |
|---|---|---|
| `pgbouncer` | `pods.disableConnectionPooling` | Connection pooler (PgBouncer); clients connect through port 5432 on the pooler |
| `prometheus-postgres-exporter` | `spec.configurations.observability.disableMetrics` | Exposes Postgres metrics for Prometheus scraping |
| `postgres-util` | `pods.disablePostgresUtil` | Provides `psql` and common Postgres admin utilities |
| `envoy` | `pods.disableEnvoy` | Edge proxy providing per-query latency and connection metrics |

## What to expect

- Changes are applied via a rolling restart; allow time proportional to the number of
  instances and the restart policy.
- After the rollout, verify the Pod containers:

  ```bash
  kubectl get pods -n my-cluster -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
  ```

- Disabled sidecars no longer consume CPU or memory; resource requests/limits for those
  containers are also removed.

## Pitfalls

- **Disabling connection pooling changes the client connection path.** When
  `disableConnectionPooling` is `true`, PgBouncer is not present and clients must connect
  directly to Postgres (port 5432 on the cluster Service). Any application configured to
  use the pooler port or rely on PgBouncer-specific features (statement-level pooling,
  `pool_mode`, etc.) will lose connectivity or behave unexpectedly until reconfigured.
- **Disabling the metrics exporter removes monitoring.** With `disableMetrics: true`, the
  Prometheus exporter is gone and any dashboards or alerts scraping that endpoint will go
  dark. Make sure to update or silence monitoring rules before disabling.
- **`postgres-util` is the only source of `psql` in the Pod.** If you disable
  `postgres-util`, you lose the ability to `kubectl exec` into the sidecar and run `psql`
  or other Postgres utilities. Use an external client or re-enable the sidecar before
  attempting interactive administration.
- **Rolling restart on every toggle.** Each change to a sidecar flag triggers a rolling
  restart of all Pods. Avoid toggling sidecars repeatedly in production; plan which
  sidecars you need before making changes.
- **Envoy metrics disappear.** Disabling Envoy removes per-connection and per-query proxy
  metrics from the monitoring stack. If you rely on Envoy-sourced metrics in Grafana or
  Prometheus alerting rules, update those rules before disabling.
