---
title: Observability and metrics
weight: 11
url: /cookbook/operating-clusters/observability-metrics
description: Expose Prometheus metrics and integrate with the Prometheus operator.
showToc: true
---

## What it does

Enables the `postgres_exporter` sidecar on each cluster Pod and, optionally, creates a
`PodMonitor` for each Prometheus instance discovered by the Prometheus operator. The
relevant fields live under `spec.configurations.observability` and
`spec.configurations.postgresExporter` on the
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). The operator reconciles
the change without recreating the cluster.

## When to use it

- You want to scrape standard Postgres metrics (connections, replication lag, table
  bloat, lock waits) from every cluster Pod.
- You have the Prometheus operator installed and want StackGres to wire up scrape targets
  automatically via `PodMonitor` objects.
- You need to extend or replace the default `postgres_exporter` query set with custom
  queries for application-level metrics.

## How to do it

### 1. Enable metrics export and Prometheus autobinding

Patch (or update) the `SGCluster` with the `observability` block:

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
    observability:
      # Create a PodMonitor for each Prometheus found by the Prometheus operator.
      # Requires the Prometheus operator to be installed in the cluster.
      prometheusAutobind: true
```

```bash
kubectl apply -f cluster.yaml
```

### 2. (Optional) Add custom postgres_exporter queries

The `queries` map under `spec.configurations.postgresExporter` is passed verbatim to
`postgres_exporter` as its `queries.yaml`. Values here overwrite the operator defaults
for any matching query key:

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
    observability:
      prometheusAutobind: true
    postgresExporter:
      queries:
        pg_replication:
          query: >-
            SELECT CASE WHEN NOT pg_is_in_recovery() THEN 0
            ELSE EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp()))::int END
            AS lag
          metrics:
            - lag:
                usage: GAUGE
                description: Replication lag behind primary in seconds
```

```bash
kubectl apply -f cluster.yaml
```

## How it works

Each cluster Pod runs a `postgres_exporter` sidecar alongside the `patroni` container.
The sidecar connects to the local Postgres instance and exposes metrics on port `9187`
at the path `/metrics`.

When `prometheusAutobind` is `true`, the operator creates a `PodMonitor` resource in
the same namespace for each Prometheus instance listed in
`SGConfig.spec.collector.prometheusOperator.monitors`. The Prometheus operator picks
up these monitors automatically; no manual scrape-config editing is required.

Custom `queries` in `spec.configurations.postgresExporter` are merged into the
`queries.yaml` that the operator generates. Any key you supply overwrites the
operator-provided query of the same name; keys not present in your map keep their
operator defaults.

## What to expect

- Confirm the sidecar is running on the primary Pod:

  ```bash
  kubectl get pod -n my-cluster cluster-0 \
    -o jsonpath='{.spec.containers[*].name}'
  ```

  The output should include `postgres-exporter`.

- Verify the metrics endpoint responds:

  ```bash
  kubectl exec -n my-cluster cluster-0 -c postgres-exporter -- \
    wget -qO- http://localhost:9187/metrics | head -20
  ```

- Check that the `PodMonitor` was created (when `prometheusAutobind: true`):

  ```bash
  kubectl get podmonitor -n my-cluster
  ```

## Pitfalls

- **`prometheusAutobind` requires the Prometheus operator.** If the Prometheus operator
  is not installed, setting `prometheusAutobind: true` has no effect; no `PodMonitor`
  is created and no error is surfaced. Install the Prometheus operator first, and ensure
  the correct monitor entries exist in `SGConfig.spec.collector.prometheusOperator.monitors`.
- **Disabling the metrics exporter removes the metrics endpoint.** Setting
  `spec.configurations.observability.disableMetrics: true` prevents the
  `postgres_exporter` sidecar from being created. Any `PodMonitor` that still points to
  port `9187` on those Pods will produce scrape errors. Disable only when monitoring is
  intentionally not needed.
- **Custom queries overwrite operator defaults for the same key.** If a query key you
  supply also exists in the operator's built-in set, your definition wins. Some operator
  internals depend on those built-in queries; review the generated `queries.yaml` before
  replacing a key.
- **`pods.disableMetricsExporter` is deprecated.** Older manifests may use
  `spec.pods.disableMetricsExporter`; this field still works but is superseded by
  `spec.configurations.observability.disableMetrics`. Migrate to the new path.
