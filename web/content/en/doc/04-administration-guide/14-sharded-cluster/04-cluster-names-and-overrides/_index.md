---
title: Cluster Names and Overrides
weight: 4
url: /doc/latest/administration/sharded-cluster/names-and-overrides
description: How to name the SGClusters that compose an SGShardedCluster and how to override the configuration of individual workers.
showToc: true
---

An SGShardedCluster is materialized as a coordinator SGCluster plus one SGCluster per worker (and, for Citus, one SGCluster per query router). The operator derives the names of those SGClusters from the SGShardedCluster name, but you can customize them and override the configuration of individual SGClusters when needed.

## Naming the Coordinator SGCluster

By default the coordinator SGCluster is named after the SGShardedCluster with the `-coord` suffix. For an SGShardedCluster named `cluster`, the coordinator SGCluster is `cluster-coord`.

If you want a specific name, set `spec.coordinator.clusterName`:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: cluster
spec:
  coordinator:
    clusterName: my-coordinator
    instances: 2
    pods:
      persistentVolume:
        size: '10Gi'
```

With this configuration the coordinator SGCluster is named `my-coordinator` instead of `cluster-coord`. The same name is used for the coordinator's Postgres configuration name, the coordinator script, and the Pods (`my-coordinator-0`, `my-coordinator-1`, ...).

> `coordinator.clusterName` can only be set on creation. Changing it later is rejected by the validator because it would orphan the existing SGCluster, configuration and Pods.

## Naming Worker SGClusters

By default the worker SGClusters are named after the SGShardedCluster with the `-worker` suffix and a zero-based index appended. For an SGShardedCluster named `cluster` with `workers.clusters: 3`, the worker SGClusters are `cluster-worker0`, `cluster-worker1` and `cluster-worker2`.

You can customize the prefix using `spec.workers.clusterNameTemplate`. The operator appends the zero-based index to that template:

```yaml
spec:
  workers:
    clusterNameTemplate: worker
    clusters: 3
    instancesPerCluster: 2
    pods:
      persistentVolume:
        size: '10Gi'
```

This produces SGClusters named `worker0`, `worker1` and `worker2` (and Pods `worker0-0`, `worker0-1`, `worker1-0`, ...).

> `workers.clusterNameTemplate` can only be set on creation. The same applies to `coordinator.queryRouterClusterNameTemplate` (see [Query Routers]({{% relref "04-administration-guide/14-sharded-cluster/01-citus-sharding-technology#query-routers" %}})).

### Choosing a Template

Keep these constraints in mind when picking a value:

* The final SGCluster name (`<template><index>`) must be a valid [DNS-1123 label](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#dns-label-names) — at most 63 characters, lowercase letters, digits and `-`.
* The SGCluster name is used as a label value on the generated Pods and Services, so leaving room for the index suffix prevents truncation issues.
* The template is shared by all workers; if you need a different per-worker name, that is not supported — use the [overrides](#overriding-individual-sgclusters) section instead to change other settings.

## Overriding Individual SGClusters

By default every worker SGCluster shares the same configuration declared at `spec.workers.*`. The `spec.workers.overrides` array lets you change settings on a subset of those SGClusters without forking the configuration of the rest.

Each entry in `overrides` selects one or more SGClusters via the `index`/`indexes` fields and (optionally) targets workers or query routers via `type`. The remaining fields of the entry mirror the worker spec and replace the corresponding values on the selected SGClusters.

### Selecting SGClusters: `index` vs `indexes`

You must set exactly one of `index` and `indexes` on each override entry — they are mutually exclusive and one of the two is required.

* **`index`** selects a single SGCluster by its zero-based identifier. Use it when you want to target one specific SGCluster.

  ```yaml
  spec:
    workers:
      clusters: 4
      instancesPerCluster: 2
      overrides:
      - index: 0
        sgInstanceProfile: large-profile
  ```

  The example above applies `large-profile` to worker SGCluster index `0` only; the other three workers keep the default profile.

* **`indexes`** selects one or more SGClusters at once. Each entry of the list can be:

  * an **integer** — a single SGCluster identifier (e.g. `3`)
  * a **range** — two integers separated by a dash that include both endpoints (e.g. `1-2` selects identifiers `1` and `2`)
  * the literal string **`all`** — expands to every configured identifier of the targeted `type` (every worker, or every query router when `type: QueryRouter`)

  ```yaml
  spec:
    workers:
      clusters: 5
      instancesPerCluster: 2
      overrides:
      - indexes: [0, "2-4"]
        sgInstanceProfile: large-profile
  ```

  This targets workers `0`, `2`, `3` and `4` in a single override entry.

  ```yaml
  spec:
    coordinator:
      queryRouterClusters: 3
    workers:
      clusters: 4
      instancesPerCluster: 2
      overrides:
      - indexes: ["all"]
        type: QueryRouter
        pods:
          scheduling:
            nodeSelector:
              workload: routers
  ```

  When `indexes` contains `all`, the entry expands to every SGCluster of the chosen type. With `type: QueryRouter` it covers every query router; with `type: Worker` (or no `type`) it would expand to nothing unless paired with `coordinator.queryRouterClusters` — `all` is intended primarily for query routers, and it expands to an empty list when there are no query routers configured.

  Identifiers used across `index`/`indexes` entries must not overlap. If two override entries select the same `(type, index)` pair the operator rejects the SGShardedCluster with an `Workers overrides must contain unique indexes` error.

### Selecting the SGCluster Kind: `type`

The `type` field on an override entry chooses whether the entry applies to a regular worker SGCluster or to a query router SGCluster. Allowed values:

* **`Worker`** — the entry overrides a regular worker SGCluster, that is one of those configured by `spec.workers.clusters`. The `index` (or `indexes`) is the zero-based identifier of the worker SGCluster.
* **`QueryRouter`** — the entry overrides a query router SGCluster, that is one of those configured by `spec.coordinator.queryRouterClusters`. The `index` (or `indexes`) is the zero-based query router identifier (i.e. `0` selects the first query router), not the offset Citus group identifier. See [Query Routers]({{% relref "04-administration-guide/14-sharded-cluster/01-citus-sharding-technology#query-routers" %}}) for details about the index offset.

If `type` is omitted the entry defaults to `Worker`. `QueryRouter` is only valid when the SGShardedCluster has `spec.type: citus`.

The following example overrides one specific worker and all the query routers from the same `overrides` array:

```yaml
spec:
  type: citus
  coordinator:
    instances: 2
    queryRouterClusters: 3
  workers:
    clusters: 4
    instancesPerCluster: 2
    overrides:
    - index: 0
      type: Worker
      pods:
        persistentVolume:
          size: '50Gi'   # bigger PV for the first worker SGCluster
    - indexes: ["all"]
      type: QueryRouter
      sgInstanceProfile: router-profile  # dedicated profile for routers
```

### What an Override Can Change

An override entry can replace values on the selected SGCluster(s) for sections such as `instancesPerCluster`, `sgInstanceProfile`, `replication`, `configurations`, `managedSql`, `metadata` and `pods` (see the [SGShardedCluster reference]({{% relref "06-crd-reference/11-sgshardedcluster" %}}) for the full list). Fields that are not specified on the override fall back to the values declared at `spec.workers.*` (or `spec.coordinator.*` for query routers).
