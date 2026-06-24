---
title: Tuning Patroni dynamic configuration
weight: 8
url: /cookbook/operating-clusters/patroni-dynamic-config
description: Adjust Patroni settings on a running cluster.
showToc: true
---

## What it does

Adjusts Patroni runtime settings on a live
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) by setting
`spec.configurations.patroni.dynamicConfig`. The operator reconciles the change and pushes
the new values to Patroni's DCS-backed dynamic configuration without restarting the cluster
or recreating any Pods.

## When to use it

- You need to tune the leader lease TTL (`ttl`), main-loop interval (`loop_wait`), or DCS
  retry timeout (`retry_timeout`) on a cluster that is already running.
- You want to configure Patroni's failover behaviour — such as `maximum_lag_on_failover` or
  `max_timelines_history` — after the cluster has been created.
- You need to add or modify custom `pg_hba` rules that Patroni manages through its dynamic
  configuration API.
- A setting was locked in at creation via `initialConfig` (for example `ttl`) and you now
  want to change it without recreating the cluster.

## How to do it

Patch the existing `SGCluster` to add or update `spec.configurations.patroni.dynamicConfig`:

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
    patroni:
      dynamicConfig:                    # updatable; pushed to Patroni's DCS config
        ttl: 30                         # leader lock TTL in seconds (Patroni default: 30)
        loop_wait: 10                   # main loop interval in seconds (default: 10)
        retry_timeout: 10               # DCS/REST API call retry timeout (default: 10)
        maximum_lag_on_failover: 1048576  # max replica lag in bytes before exclusion
        postgresql:
          pg_hba:
            - "host all all 10.0.0.0/8 scram-sha-256"  # appended to operator-managed rules
```

```bash
kubectl apply -f cluster.yaml
```

`dynamicConfig` is a free-form object: any key valid in Patroni's
[dynamic configuration](https://patroni.readthedocs.io/en/latest/dynamic_configuration.html)
may appear here. The following top-level fields are always ignored even if specified:
`synchronous_mode`, `synchronous_mode_strict`, `postgresql` (except the `pg_hba` section),
and `standby_cluster`. Custom `pg_hba` entries are appended after the operator's own
required rules.

## How it works

When `spec.configurations.patroni.dynamicConfig` is set or changed, the operator writes the
merged configuration to the Patroni DCS (Kubernetes ConfigMaps). Patroni picks up the
change on its next loop iteration — within `loop_wait` seconds — and applies the new values
to all cluster members without restarting Postgres. Because the update travels through the
DCS, it reaches every member (primary and replicas) simultaneously.

The field is **updatable**: `kubectl apply` or `kubectl patch` on a running cluster is
accepted by the validating webhook, and the operator reconciles the change on its next pass.
This makes `dynamicConfig` the correct mechanism for any Patroni setting that can be
adjusted post-bootstrap.

## What to expect

- Verify the operator has reconciled the change by inspecting the live Patroni config:

  ```bash
  kubectl exec -n my-cluster cluster-0 -c patroni -- patronictl show-config
  ```

- Check that the new values are visible in the cluster status:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o jsonpath='{.spec.configurations.patroni.dynamicConfig}'
  ```

- Settings such as `ttl` and `loop_wait` take effect within one loop cycle (default 10 s)
  with no cluster downtime.

## Pitfalls

- **`dynamicConfig` is not `initialConfig`.** `spec.configurations.patroni.initialConfig`
  is immutable and applied only at cluster creation (the "Custom Patroni initial
  configuration" recipe in the Creating Clusters section covers it). `dynamicConfig` is the
  updatable counterpart and is the right field for day-two changes.
- **Not every Patroni setting is dynamically adjustable.** Patroni itself distinguishes
  between settings that can be changed through the DCS and those that require a full config
  reload or node restart. Settings outside Patroni's dynamic configuration surface (see the
  [Patroni dynamic configuration docs](https://patroni.readthedocs.io/en/latest/dynamic_configuration.html))
  must be placed in `initialConfig` at creation time and cannot be changed on a live
  cluster.
- **Ignored fields are silently dropped.** The operator removes `synchronous_mode`,
  `synchronous_mode_strict`, the `postgresql` section (except `pg_hba`), and
  `standby_cluster` before writing to the DCS. Specifying those keys has no effect;
  StackGres manages them independently.
- **`pg_hba` entries are appended, not replaced.** The operator always injects its own
  required `pg_hba` rules first. Rules in `dynamicConfig.postgresql.pg_hba` are appended
  after them. You cannot remove operator-managed rules through this field.
- **Patroni version compatibility.** The configuration keys accepted by `dynamicConfig`
  depend on the Patroni version bundled with the StackGres release. Consult the Patroni
  documentation that matches your StackGres version if an expected key has no effect.
