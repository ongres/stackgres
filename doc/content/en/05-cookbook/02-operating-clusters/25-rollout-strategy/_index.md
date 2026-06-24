---
title: Rollout and update strategy
weight: 25
url: /cookbook/operating-clusters/rollout-strategy
description: Control how changes are rolled out across the cluster Pods.
showToc: true
---

## What it does

Configures `spec.pods.updateStrategy` on a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to control when and how Pod
changes are applied. The strategy governs whether updates happen immediately, only through
an explicit `SGDbOps` operation, on a cron-based schedule, or never automatically.

## When to use it

- You want Pod updates to happen automatically as soon as the operator detects a pending
  change (type `Always`).
- You need to constrain restarts to a maintenance window defined by a cron expression
  (type `Schedule`).
- You want full control and require every rollout to be triggered explicitly via an
  `SGDbOps` restart, security upgrade, or minor version upgrade (type `OnlyDbOps`, the
  default).
- You need to freeze a cluster completely and apply changes only by deleting Pods manually
  (type `Never`).

## How to do it

### Switch to scheduled rollouts

Configure a maintenance window — every Sunday between 02:00 and 03:00 UTC — during which
the operator may restart Pods when a change is pending:

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
    updateStrategy:
      type: Schedule           # apply changes only inside the defined windows
      method: ReducedImpact    # spin up an extra instance before restarting each replica
      schedule:
        - cron: "0 2 * * 0"   # start of the window: Sundays at 02:00 UTC
          duration: PT1H       # ISO 8601 duration — window lasts one hour
```

```bash
kubectl apply -f cluster.yaml
```

### Freeze updates entirely (OnlyDbOps — the default)

Omit `updateStrategy` or set it explicitly to have updates occur only when you submit an
`SGDbOps`:

```yaml
    updateStrategy:
      type: OnlyDbOps   # default; no automatic rollout
      method: InPlace   # default; update existing instances in place
```

### Trigger the rollout manually when ready

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: rollout-restart
spec:
  sgCluster: cluster
  op: restart
  restart:
    method: InPlace
```

```bash
kubectl apply -f rollout-restart.yaml
kubectl wait sgdbops -n my-cluster rollout-restart \
  --for=condition=Completed --timeout=600s
```

## How it works

`pods.updateStrategy` is an updatable field that the operator reconciles without
recreating the cluster. The `type` field controls the trigger:

- `Always` — the operator restarts Pods as soon as it detects a pending change.
- `Schedule` — the operator respects the `schedule` array; each entry pairs a UNIX cron
  expression (`cron`) with an ISO 8601 duration (`duration`) that defines how long the
  window stays open. Multiple windows may be listed.
- `OnlyDbOps` (default) — no automatic restart; a restart only happens when an `SGDbOps`
  of type `restart`, `securityUpgrade`, or `minorVersionUpgrade` is applied.
- `Never` — the operator never restarts Pods on its own, even if an `SGDbOps` is created.
  Changes take effect only when Pods are deleted manually.

The `method` field controls impact during the rollout:

- `InPlace` (default) — each Pod is restarted in place, one at a time.
- `ReducedImpact` — a temporary extra instance is created before each replica restart,
  preserving read-only capacity throughout the rollout.

The annotation `stackgres.io/rollout` can override `type` at runtime (`always`, `schedule`,
`never`) without editing the resource spec — except when `type: Never` is set, which cannot
be overridden by annotation.

## What to expect

- Verify the current update strategy:

  ```bash
  kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.spec.pods.updateStrategy}'
  ```

- Check whether any Pods are pending a restart:

  ```bash
  kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.status.conditions}' | grep PendingRestart
  ```

- For `Schedule` type, the operator will not restart Pods outside the defined windows even
  if changes are pending. Changes accumulate and are applied at the next open window.

## Pitfalls

- **`Never` blocks all automatic and SGDbOps-triggered restarts.** With `type: Never`,
  changes are not applied until you delete Pods manually. There is no SGDbOps shortcut —
  even submitting a restart SGDbOps will not trigger a rollout. Use this type only when you
  need hard freeze guarantees.
- **`schedule.cron` uses UNIX cron syntax, not Kubernetes CronJob syntax.** The expression
  is a standard five-field cron (`m h dom mon dow`). There is no `timezone` field; times
  are interpreted by the operator in UTC.
- **`schedule.duration` must be an ISO 8601 duration.** Use the format `PnDTnHnMn.nS`
  (e.g., `PT30M` for thirty minutes, `PT2H` for two hours). An omitted or zero duration
  means the window closes immediately, so the update would never run.
- **`method: ReducedImpact` temporarily increases instance count.** The operator creates
  an extra Pod during the rollout. Ensure your namespace has sufficient CPU, memory, and
  storage quota for one additional instance before enabling this method.
- **`managementPolicy: Parallel` is independent of `updateStrategy`.** `pods.managementPolicy`
  (`OrderedReady` by default) controls StatefulSet Pod creation order during scale-up and
  scale-down, not the rollout sequence. Changing it to `Parallel` affects how new Pods are
  brought up, not how rolling restarts are sequenced.
