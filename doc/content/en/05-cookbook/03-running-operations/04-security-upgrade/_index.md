---
title: Security upgrade
weight: 4
url: /cookbook/running-operations/security-upgrade
description: Roll out new container images to pick up security fixes.
showToc: true
---

## What it does

Restarts all Pods in an [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) so they
adopt updated StackGres container images. This is the standard way to apply security patches
that ship in a new operator version without changing the Postgres version or cluster
configuration. The operation is triggered by creating an
[SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) with `op: securityUpgrade`.

## When to use it

- After upgrading the StackGres operator, to ensure running Pods use the newly released
  images (which may include OS-level CVE fixes).
- When a security advisory requires refreshing the base image without a Postgres minor
  version bump.
- As a controlled alternative to deleting Pods manually — the operator handles the restart
  order and preserves availability.

## How to do it

Create an SGDbOps resource that names the target cluster and selects the `securityUpgrade`
operation. The optional `method` field controls whether extra capacity is provisioned during
the rollout:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: security-upgrade
spec:
  sgCluster: cluster        # the SGCluster to upgrade
  op: securityUpgrade
  securityUpgrade:
    method: ReducedImpact   # InPlace (default) or ReducedImpact
```

Apply it:

```bash
kubectl apply -f security-upgrade.yaml
```

Watch progress in the resource status:

```bash
kubectl get sgdbops -n my-cluster security-upgrade -o yaml
```

### `method` values

| Value | Behaviour |
|---|---|
| `InPlace` | Restarts existing Pods one at a time. No extra nodes required. Slower recovery if only one instance is present. |
| `ReducedImpact` | Provisions a temporary extra replica with the new image before restarting each existing Pod. Requires additional cluster resources during the operation. |

## How it works

The operator locks the cluster for the duration of the operation. It then iterates over
each Pod according to the chosen `method`:

- **`InPlace`**: replicas are restarted first, then a switchover moves the primary role to
  an already-updated replica, and the former primary is restarted last.
- **`ReducedImpact`**: a new temporary instance is started with the updated image. Once it
  joins and is healthy, the original Pod is restarted; the temporary instance is removed
  after the full rollout completes.

Progress is tracked in `status.securityUpgrade`, which records `initialInstances`,
`pendingToRestartInstances`, and `primaryInstance` so you can monitor exactly where the
rollout stands. See the [SGDbOps reference]({{% relref "06-crd-reference/08-sgdbops" %}})
for the full status schema.

## What to expect

- The operation causes a **rolling restart**. Each replica restart is brief, but the
  primary switchover briefly interrupts write traffic.
- With a single-instance cluster and `InPlace`, there is no standby to fail over to —
  the primary restarts directly, causing a short write outage.
- After the operation completes, the SGDbOps resource remains as a record. You can inspect
  it or delete it once you no longer need the audit trail.

## Pitfalls

- **Schedule during a maintenance window for sensitive workloads.** Even with
  `ReducedImpact`, the primary switchover causes a brief interruption. Coordinate with
  application teams or use `spec.runAt` to defer the job to a low-traffic period (see
  the [SGDbOps reference]({{% relref "06-crd-reference/08-sgdbops" %}}) for scheduling
  fields).
- **`ReducedImpact` requires spare capacity.** If the namespace or node pool is resource-
  constrained, the temporary extra Pod may fail to schedule and block the operation.
  Verify available CPU and memory before using this method.
- **The operator must be upgraded first.** A security upgrade rolls out the images that
  the currently installed operator version provides. If the operator has not been upgraded
  yet, creating the SGDbOps will simply restart Pods onto the same images — no security
  content changes until the operator itself is updated.
- **One SGDbOps at a time per cluster.** While the SGDbOps holds the cluster lock, no
  other operation (restart, vacuum, etc.) can run against the same SGCluster. Wait for
  the operation to complete or delete the resource before starting another.
