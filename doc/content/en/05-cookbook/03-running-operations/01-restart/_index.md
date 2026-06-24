---
title: Restarting a cluster
weight: 1
url: /cookbook/running-operations/restart
description: Perform a controlled restart of a cluster with SGDbOps.
showToc: true
---

## What it does

Creates an [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) with `op: restart` to
perform an orderly, rolling restart of every Pod in an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). The operator handles the
sequencing: replicas are restarted first, then a switchover moves the primary role to a
healthy replica before the old primary is restarted, minimising write unavailability.

## When to use it

- A configuration change (e.g. a new `SGPostgresConfig`) has been applied and one or more
  instances are in a pending-restart state.
- You want to recycle Pods after a node or image change without editing the SGCluster
  directly.
- You need a controlled maintenance window restart rather than relying on Kubernetes
  eviction or node drains.

## How to do it

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: restart
spec:
  sgCluster: cluster      # name of the target SGCluster
  op: restart
  restart:
    method: ReducedImpact   # spawn a spare replica for lower impact; use InPlace if no spare capacity
    onlyPendingRestart: true  # restart only Pods flagged as pending-restart; omit to restart all
```

Apply the resource:

```bash
kubectl apply -f restart.yaml
```

Watch progress:

```bash
kubectl get sgdbops -n my-cluster restart -o yaml
```

### Choosing a method

| Method | Requires spare capacity | Description |
|---|---|---|
| `InPlace` | No | Restarts each Pod on its existing node; no extra replica is spawned. |
| `ReducedImpact` | Yes | Spawns a new replica first, shifts load, then restarts the old Pod; reduces downtime. |

If `method` is omitted the operator defaults to `InPlace`.

## How it works

1. The operator acquires a lock on the target SGCluster so that no other SGDbOps can run
   concurrently.
2. Each replica Pod is restarted in turn and must become `Ready` before the next one is
   processed.
3. A Patroni switchover is triggered so the current primary hands the leader role to a
   healthy replica.
4. The former primary is then restarted last.
5. With `ReducedImpact`, a new replica Pod is brought up before step 2 to maintain
   replication headroom throughout.
6. When `onlyPendingRestart: true` is set, only Pods that the operator has identified as
   requiring a restart are included; already-current Pods are skipped.

Progress and the list of restarted instances are recorded in `status.restart`.

## What to expect

- Write traffic is interrupted briefly during the switchover (typically a few seconds).
- Read traffic on the replicas service is interrupted per-replica while each Pod restarts.
- The overall duration scales with the number of instances and the time each Pod takes to
  become `Ready`. A three-instance cluster typically completes within a few minutes.
- After the operation completes the SGDbOps resource remains as an audit record. The
  SGCluster's pending-restart condition is cleared.

## Pitfalls

- **`ReducedImpact` needs a free scheduling slot.** The operator spawns an extra replica
  before restarting existing Pods. If all nodes are at capacity the new Pod will stay
  `Pending` and the operation will stall. Either use `InPlace` or free up capacity first.
- **The primary is always restarted last via switchover.** There is no way to skip the
  primary. If Patroni cannot elect a new leader (e.g. no healthy replica is available)
  the switchover will fail and the operation will report an error.
- **Only one SGDbOps can run at a time per cluster.** If another operation is already
  running (or a previous SGDbOps left the lock held), the new restart will queue until the
  lock is released.
- **Deleting and re-creating an SGDbOps re-runs the operation.** SGDbOps resources are
  not idempotent across deletion. If you need to re-run a restart, delete the completed
  resource first and create a new one.
- **`onlyPendingRestart: true` will not restart up-to-date Pods.** If no Pod is
  currently in a pending-restart state the operation completes immediately without
  restarting anything. This is expected — check the cluster status if you believe a
  restart is needed.
