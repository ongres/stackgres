---
title: Custom Patroni initial configuration
weight: 6
url: /cookbook/creating-clusters/patroni-initial-config
description: Provide Patroni bootstrap settings that only apply when the cluster is first created.
showToc: true
---

## What it does

Sets Patroni configuration values that are applied only during the one-time bootstrap of a
new [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). The field
`spec.configurations.patroni.initialConfig` accepts a free-form object whose keys are merged
into the Patroni configuration file before the cluster is initialized for the first time.
Because this field is **immutable**, its content is permanently fixed at creation; it can
never be changed on a running cluster.

## When to use it

- You need to seed Patroni settings — such as custom `postgresql.callbacks`, `pre_promote`
  hooks, or `before_stop` hooks — before the first primary is ever started.
- You need bootstrap-time Patroni settings that Patroni does not expose through its dynamic
  configuration API and therefore cannot be adjusted after the cluster is initialized.
- You are replicating a tuning baseline from an existing Patroni deployment into a new
  StackGres cluster.

For any Patroni setting that can be changed after the cluster is running, use
`spec.configurations.patroni.dynamicConfig` instead — it is updatable and takes effect
without recreating the cluster.

## How to do it

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
  pods:
    persistentVolume:
      size: 10Gi
  configurations:
    patroni:
      initialConfig:                        # applied once at bootstrap; immutable
        postgresql:
          callbacks:
            on_start: /etc/patroni/on_start.sh    # called when Postgres starts
          pre_promote: /etc/patroni/pre_promote.sh # called before a failover promotion
        ttl: 30                             # leader lease TTL in seconds (default 30)
        loop_wait: 10                       # main loop interval in seconds (default 10)
        retry_timeout: 10                   # DCS request retry timeout in seconds
```

```bash
kubectl apply -f cluster.yaml
```

`initialConfig` is a free-form object: any key that is valid in a Patroni YAML configuration
file may appear here, subject to the exclusions listed below. StackGres merges the provided
values on top of its own generated configuration before writing the Patroni configuration
file for the very first time.

The following top-level Patroni sections are always ignored even if specified:
`name`, `namespace`, `log`, `bootstrap`, `citus`, `postgresql` (except
`postgresql.callbacks`, `postgresql.pre_promote`, `postgresql.before_stop`, and
`postgresql.pg_ctl_timeout`), `restapi`, `ctl`, `watchdog`, and `tags`.

See the [Patroni YAML configuration reference](https://patroni.readthedocs.io/en/latest/yaml_configuration.html)
for the full list of supported keys.

## How it works

When the operator creates the StatefulSet, it renders the complete Patroni configuration
file for each Pod. For the `initialConfig` fields that are not excluded, the operator merges
the user-supplied values on top of the generated defaults. Because the merge happens before
the primary Pod starts Patroni for the first time, every value in `initialConfig` is visible
to Patroni's bootstrap sequence. Patroni writes its initial cluster state to its DCS (Kubernetes
Endpoints/ConfigMaps) during that bootstrap; once the state is stored, the field is no longer
read on subsequent reconciliation passes.

Settings that Patroni persists in the DCS at bootstrap — such as `ttl`, `loop_wait`, and
`retry_timeout` — can subsequently be adjusted via `dynamicConfig` or the Patroni REST API
without recreating the cluster.

## What to expect

- Watch the primary Pod start and bootstrap:

  ```bash
  kubectl logs -n my-cluster cluster-0 -c patroni -f
  ```

- Inspect the resolved Patroni configuration that the Pod is running:

  ```bash
  kubectl exec -n my-cluster cluster-0 -c patroni -- patronictl show-config
  ```

- Confirm the cluster is healthy and the primary has been elected:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o yaml
  ```

## Pitfalls

- **`initialConfig` is immutable.** The StackGres validating webhook rejects any `kubectl
  apply` or `kubectl patch` that changes `spec.configurations.patroni.initialConfig` on an
  existing cluster with the message: `Cannot update patroni initial configuration`. If you
  need to change a bootstrap-only setting you must create a new cluster.
- **Use `dynamicConfig` for runtime-adjustable settings.** Many Patroni parameters (such as
  `ttl`, `loop_wait`, and `retry_timeout`) can be changed on a live cluster. Placing them in
  `initialConfig` hard-codes them at creation time and prevents later updates through
  `initialConfig`. Use `spec.configurations.patroni.dynamicConfig` for those — it is
  updatable and covers the Patroni [dynamic configuration](https://patroni.readthedocs.io/en/latest/dynamic_configuration.html)
  surface.
- **Ignored sections are silently dropped.** Keys under the excluded top-level sections
  (`bootstrap`, `postgresql.*` outside the allowed callbacks, etc.) are accepted by the API
  but do nothing. Verify that the setting you intend to configure is not in an excluded
  section before relying on it.
- **Patroni version compatibility.** The configuration keys accepted by `initialConfig`
  depend on the Patroni version bundled with the StackGres release. Consult the Patroni
  documentation that matches your StackGres version if an expected key has no effect.
