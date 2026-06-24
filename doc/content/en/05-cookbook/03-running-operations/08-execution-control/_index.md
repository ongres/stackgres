---
title: Scheduling, timeouts and retries
weight: 8
url: /cookbook/running-operations/execution-control
description: Control when an operation runs and how failures are retried.
showToc: true
---

## What it does

Controls the execution lifecycle of any
[SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) resource: when it starts,
how long it is allowed to run, and what the operator does when it fails. The fields
`spec.runAt`, `spec.timeout`, `spec.maxRetries`, `spec.retryDelay`,
`spec.retryLimit`, and `spec.retryMaxDelay` are cross-cutting — they apply to every
operation type (`restart`, `minorVersionUpgrade`, `vacuum`, `repack`, `benchmark`,
and so on) and are reconciled by the operator without any application changes.

## When to use it

- You need to run an operation during an agreed maintenance window rather than
  immediately on `kubectl apply`.
- You want to time-box a long-running operation so that it cannot block cluster
  reconciliation indefinitely.
- An operation is prone to transient failures (network blips, brief unavailability of
  a replica) and you want the operator to retry automatically before declaring it
  failed.
- You are tuning the internal retry cadence for operations that issue many small
  sub-commands (vacuum, repack) against a busy cluster.

## How to do it

The example below wraps a `restart` operation with a scheduled start, a hard
timeout, and automatic retries. The same fields can be added to any `SGDbOps`.

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: scheduled-restart
spec:
  # Cluster to operate on.
  sgCluster: cluster
  op: restart

  # Schedule the operation for a future UTC time (ISO 8601).
  # If omitted, or if the time is already in the past, the operation starts ASAP.
  runAt: "2025-09-01T02:00:00Z"

  # Cancel and mark the operation Failed (reason: OperationTimedOut) if it has
  # not completed within this duration. ISO 8601 format PnDTnHnMn.nS.
  # If omitted, the operation never times out.
  timeout: PT30M

  # Retry the entire operation up to this many times after a top-level failure.
  # Default is 0 (no retries).
  maxRetries: 2

  # Initial delay for the internal exponential back-off used when individual
  # sub-commands inside the operation script are retried. Default: PT1S.
  retryDelay: PT2S

  # Maximum number of times a single sub-command is retried before the
  # operation fails. Default: 10.
  retryLimit: 15

  # Cap on the exponential back-off delay between sub-command retries.
  # Default: PT1M.
  retryMaxDelay: PT2M

  restart:
    method: InPlace
```

Apply the resource:

```bash
kubectl apply -f scheduled-restart.yaml
```

Watch the operation status:

```bash
kubectl get sgdbops -n my-cluster scheduled-restart -w
```

Inspect conditions (including any timeout or failure reason):

```bash
kubectl describe sgdbops -n my-cluster scheduled-restart
```

## How it works

**Scheduling.** When `spec.runAt` is set to a future time, the operator holds the
operation in a pending state until that instant is reached. If the field is absent
or refers to a moment already in the past, execution begins as soon as the resource
is created and the operator picks it up.

**Timeout.** Once the operation transitions to `Running`, the operator tracks elapsed
time. If `spec.timeout` elapses before the operation completes, the operator cancels
the running job and sets the `Failed` condition with `reason: OperationTimedOut`.
Without `spec.timeout` the operation can run indefinitely.

**Top-level retries.** `spec.maxRetries` governs how many times the operator
re-launches the entire operation job after a top-level failure. The first attempt
is not counted; `maxRetries: 2` means up to three total attempts.

**Sub-command retries.** Inside each attempt, the operation script retries
individual commands using exponential back-off. `spec.retryDelay` sets the starting
delay, `spec.retryMaxDelay` caps how large the delay can grow, and `spec.retryLimit`
sets the per-command retry ceiling. These fields tune internal resilience without
affecting `maxRetries`.

## What to expect

- The SGDbOps transitions: `Pending` (waiting for `runAt`) → `Running` →
  `Completed` or `Failed`.
- A timed-out operation sets the `Failed` condition with
  `reason: OperationTimedOut` in `.status.conditions`.
- When `maxRetries > 0` the operator increments a retry counter visible in
  `.status` before re-launching; the overall elapsed time still counts against
  `spec.timeout`.
- After all retries are exhausted without success the condition `Failed` is set
  permanently.

Check the final conditions:

```bash
kubectl get sgdbops -n my-cluster scheduled-restart \
  -o jsonpath='{.status.conditions[*]}'
```

## Pitfalls

- **`runAt` in the past runs ASAP.** A timestamp that has already elapsed is
  treated the same as no `runAt` at all. Double-check the UTC value before
  applying.
- **`maxRetries` defaults to 0.** Without an explicit value the operation makes
  exactly one attempt. Set `maxRetries` explicitly whenever transient failures are
  plausible.
- **Timeout is wall-clock time, not active CPU time.** It includes any time spent
  waiting in `retryDelay` back-off, so set `timeout` to comfortably exceed the
  expected worst-case duration plus all retry delays.
- **SGDbOps is single-use.** Once a resource reaches `Completed` or exhausts its
  retries, create a new SGDbOps to run the operation again; editing a finished
  resource has no effect.
- **`retryLimit` is per sub-command, not per operation.** It controls how many
  times an individual script command is retried inside a single top-level attempt,
  not how many times the whole operation restarts. Use `maxRetries` for the latter.
