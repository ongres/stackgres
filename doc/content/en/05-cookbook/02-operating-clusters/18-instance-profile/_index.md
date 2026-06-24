---
title: Sizing instances with an instance profile
weight: 18
url: /cookbook/operating-clusters/instance-profile
description: Author an SGInstanceProfile to define CPU, memory, and per-container resources.
showToc: true
---

## What it does

Defines the CPU and memory limits (and optional requests) that every Pod in a cluster
receives, through an [SGInstanceProfile]({{% relref "06-crd-reference/02-sginstanceprofile" %}})
custom resource. The profile sets limits for the `patroni` container (which runs both
Patroni and PostgreSQL) and, optionally, overrides for each sidecar container and init
container. The cluster references the profile by name via `spec.sgInstanceProfile`.

## When to use it

- You need to right-size a cluster that is currently running with operator-generated defaults.
- A node is too small to schedule the Pods and you must reduce limits to fit.
- You want to align limits and requests (Guaranteed QoS) to enable static CPU management.
- You are running a non-production environment and want minimal resource allocation.

## How to do it

### Create the SGInstanceProfile

```yaml
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  namespace: my-cluster
  name: medium
spec:
  # Limits for the patroni container (Patroni + PostgreSQL).
  # Minimum 2 CPUs and 2Gi RAM are recommended for production.
  cpu: "2"
  memory: 4Gi

  # Optional: set requests equal to limits for Guaranteed QoS / static CPU policy.
  requests:
    cpu: "2"
    memory: 4Gi

  # Optional: override limits for a specific sidecar container.
  # If omitted, the operator fills defaults proportional to the patroni limits.
  containers:
    envoy:
      cpu: 500m
      memory: 256Mi

  # Optional: override limits for init containers.
  # If omitted, the operator fills the patroni container values.
  initContainers:
    setup-scripts:
      cpu: 500m
      memory: 256Mi
```

```bash
kubectl apply -f instance-profile.yaml
```

### Attach the profile to a cluster

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
  sgInstanceProfile: medium   # references the SGInstanceProfile above
  profile: production         # production (default) | testing | development
```

```bash
kubectl apply -f cluster.yaml
```

`spec.profile` controls how the profile is applied:

- `production` *(default)* — enforces pod anti-affinity and applies both limits **and**
  requests from the profile to all containers.
- `testing` — same resource enforcement as `production` but allows multiple Pods per node.
- `development` — disables resource requirements entirely; the profile is ignored for
  scheduling purposes.

## How it works

When the cluster is reconciled the operator reads the referenced SGInstanceProfile and
projects its `cpu` and `memory` values into the `patroni` container's `resources.limits`.
For sidecar containers, if no explicit `containers[name]` override is present, the operator
generates proportional defaults automatically. The `spec.requests` sub-section controls
`resources.requests` independently; by default those totals are split across all containers
so the patroni container receives the total minus the sidecars' share (see
`SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal` to change that
behaviour). Huge pages (`spec.hugePages`, `spec.containers[name].hugePages`) follow the
same structure but are never auto-filled.

## What to expect

- Updating the SGInstanceProfile is immediately picked up by the operator on the next
  reconciliation loop.
- If the new limits differ from the previous ones the operator performs a **rolling restart**
  of the cluster Pods (primary last) to apply the new `resources` spec.
- Monitor the rollout:

  ```bash
  kubectl get pods -n my-cluster -w
  ```

- Confirm the applied resources on a Pod:

  ```bash
  kubectl get pod -n my-cluster cluster-0 -o jsonpath='{.spec.containers[?(@.name=="patroni")].resources}'
  ```

## Pitfalls

- **Rolling restart on every change.** Any update to `spec.cpu`, `spec.memory`, or the
  per-container overrides triggers a rolling restart. Plan profile changes during a
  maintenance window on production clusters.
- **Pods stay Pending under the production profile.** The `production` profile enforces
  resource requests; if the requested CPU or memory exceeds what the nodes can allocate,
  Pods will not be scheduled. Verify node capacity before raising limits, or use
  `spec.profile: testing` on constrained clusters.
- **Anti-affinity blocks scheduling on small clusters.** Under `production`, one Pod per
  node is required. A three-instance cluster needs at least three schedulable nodes. Switch
  to `testing` for single-node or resource-constrained environments.
- **Omitting `spec.requests` does not mean zero requests.** When `requests` is absent the
  operator generates defaults derived from `cpu` and `memory`. To set requests explicitly
  equal to limits (Guaranteed QoS), set `spec.requests.cpu` and `spec.requests.memory`
  to the same values.
- **Changing `spec.profile` may also require a restart.** The `profile` field on SGCluster
  is marked `may require restart`; toggling between `production` and `development` can
  trigger a Pod rollout in addition to the profile resource changes.
