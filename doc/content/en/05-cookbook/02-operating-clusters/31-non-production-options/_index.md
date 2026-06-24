---
title: Non-production options
weight: 31
url: /cookbook/operating-clusters/non-production-options
description: Relax production safeguards for development and testing.
showToc: true
---

## What it does

Configures `spec.nonProductionOptions` on an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to relax the
safeguards that StackGres applies by default for production workloads. The
available knobs are:

| Field | Effect when `true` |
|---|---|
| `disableClusterPodAntiAffinity` | Allows more than one cluster Pod per node |
| `disablePatroniResourceRequirements` | Removes CPU/memory limits and requests from the `patroni` container |
| `disableClusterResourceRequirements` | Removes resource requests from all sidecar containers |
| `enabledFeatureGates` | Activates experimental StackGres features (e.g. `babelfish-flavor`) |

All fields are updatable on a running cluster and reconciled by the operator
without recreating the resource, though changes may require a rolling restart.

## When to use it

- You are running a local or CI environment with fewer nodes than cluster
  instances, and Pods stay `Pending` because pod anti-affinity cannot be
  satisfied.
- You are testing on a resource-constrained node and the default resource
  requirements from the referenced `SGInstanceProfile` cause scheduling
  failures or OOM kills.
- You need to evaluate an experimental feature (such as Babelfish) that is
  gated behind `enabledFeatureGates`.

## How to do it

Patch the running `SGCluster` with the desired `nonProductionOptions`:

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
      size: 5Gi
  nonProductionOptions:
    # Allow multiple cluster Pods on the same node
    disableClusterPodAntiAffinity: true

    # Remove resource limits/requests from the patroni container
    disablePatroniResourceRequirements: true

    # Remove resource requests from all sidecar containers
    disableClusterResourceRequirements: true

    # Enable an experimental feature gate (optional)
    enabledFeatureGates:
      - babelfish-flavor
```

```bash
kubectl apply -f cluster.yaml
```

Or patch a single field without touching the rest of the manifest:

```bash
# Allow co-located Pods without updating the full manifest
kubectl patch sgcluster cluster -n my-cluster \
  --type merge \
  -p '{"spec":{"nonProductionOptions":{"disableClusterPodAntiAffinity":true}}}'
```

## How it works

The three `disable*` booleans override the defaults that `spec.profile` would
otherwise set. The default profile is `production`, which enforces anti-affinity
and resource requirements. The `testing` profile relaxes only anti-affinity; the
`development` profile relaxes all three. Setting a field explicitly in
`nonProductionOptions` always takes precedence over the profile default,
regardless of which profile is active.

`enabledFeatureGates` is a string list of feature identifiers. The only
currently available gate is `babelfish-flavor`, which unlocks the Babelfish
Postgres flavor. Feature gates are intended for evaluation environments only;
they are not validated for production use.

Because all four fields are marked **updatable, may require restart** in the
[SGCluster reference]({{% relref "06-crd-reference/01-sgcluster" %}}), the
operator reconciles the change and triggers a rolling restart when the updated
constraints require Pods to be rebuilt.

## What to expect

- After applying, verify the StatefulSet Pod anti-affinity rule (or its
  absence):

  ```bash
  kubectl get statefulset -n my-cluster cluster \
    -o jsonpath='{.spec.template.spec.affinity}'
  ```

- Verify that resource requirements have been removed from the `patroni`
  container:

  ```bash
  kubectl get statefulset -n my-cluster cluster \
    -o jsonpath='{.spec.template.spec.containers[?(@.name=="patroni")].resources}'
  ```

- Monitor the rolling restart until all Pods are running:

  ```bash
  kubectl rollout status statefulset/cluster -n my-cluster
  ```

## Pitfalls

- **These options weaken production guarantees; do not use them on production
  clusters.** Disabling anti-affinity means two or more Postgres instances can
  share a node. If that node fails, you lose more than one replica. Disabling
  resource requirements removes the isolation that prevents a runaway query
  from starving other containers.
- **Use `spec.profile` as the higher-level alternative.** Setting
  `spec.profile: development` or `spec.profile: testing` adjusts the same
  defaults in a single, self-documenting field. Use `nonProductionOptions`
  only when you need to deviate from a profile on individual fields.
- **`enabledFeatureGates` are experimental.** Feature gates enable code paths
  that are not production-tested. Enabling a gate on a cluster that later
  receives a StackGres upgrade may produce unexpected behaviour if the gate
  semantics change between releases.
- **Rolling restart is triggered on every change.** Each update to
  `nonProductionOptions` reconciles the StatefulSet and restarts Pods. Plan
  changes together rather than applying them one at a time.
- **`enableSetClusterCpuRequests`, `enableSetClusterMemoryRequests`,
  `enableSetPatroniCpuRequests`, and `enableSetPatroniMemoryRequests` are
  deprecated.** They still exist in the schema but are ignored by the operator,
  which always behaves as if they are `true`. Do not set them in new manifests.
