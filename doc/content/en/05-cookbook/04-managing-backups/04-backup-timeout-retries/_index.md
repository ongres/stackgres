---
title: Backup timeouts and retries
weight: 4
url: /cookbook/managing-backups/backup-timeout-retries
description: Tune how a backup retries and times out.
showToc: true
---

## What it does

Configures the retry and timeout behavior on an
[SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) resource. By tuning
`spec.maxRetries`, `spec.timeout`, `spec.reconciliationTimeout`, and the exponential
backoff fields (`spec.retryDelay`, `spec.retryMaxDelay`, `spec.retryLimit`), you control
how many times the backup job retries after a failure, how long each internal command may
take, and how long the post-backup reconciliation step is allowed to run before it is
deferred to the next cycle. The operator reconciles these fields without recreating the
backup.

## When to use it

- A backup is failing intermittently and you want to give it more retries before marking it
  as failed.
- Backups are running against a slow or throttled storage backend and the default retry
  delays are too aggressive.
- You want to cap the total wall-clock time a backup can occupy before the operator gives
  up and marks it failed.
- Post-backup reconciliation is timing out on a large cluster and you need to extend or
  disable the reconciliation timeout.

## How to do it

Apply an `SGBackup` manifest that sets the relevant fields. All retry and timeout fields
are optional; omit any you do not need to change from defaults.

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  namespace: my-cluster
  name: cluster-backup
spec:
  sgCluster: cluster

  # Retry behavior for the overall backup operation
  maxRetries: 5              # retry the whole backup up to 5 times (default: 3); 0 = no retries

  # Timeout for the overall backup creation (seconds)
  # If not set or set to 0, the backup runs until it completes or fails with no wall-clock limit.
  timeout: 3600              # abort and mark failed if the backup takes longer than 1 hour

  # Timeout for the post-backup reconciliation pass (seconds)
  # Default: 300 (5 minutes). Set to 0 to disable.
  reconciliationTimeout: 600 # allow up to 10 minutes for reconciliation

  # Exponential backoff for individual commands inside the backup script
  retryDelay: PT2S           # initial retry delay (ISO 8601 duration, default: PT1S)
  retryMaxDelay: PT2M        # maximum retry delay the backoff may reach (default: PT1M)
  retryLimit: 15             # max retries for each individual command (default: 10)
```

```bash
kubectl apply -f cluster-backup.yaml
```

To update an existing backup rather than rewrite the whole manifest, use a patch:

```bash
kubectl patch sgbackup -n my-cluster cluster-backup \
  --type=merge -p '{"spec":{"maxRetries":5,"timeout":3600}}'
```

## How it works

The operator creates a Kubernetes Job to run the backup. The Job uses the values from
`spec` as follows:

- **`maxRetries`** (integer, default `3`) — the maximum number of times the entire backup
  operation is retried after a failure. A value of `0` means the backup is attempted once
  with no further retries.
- **`timeout`** (integer, seconds) — if set to a positive value the backup job is cancelled
  and the resource is marked failed once this wall-clock limit is exceeded. If not set, or
  set to `0`, no limit is enforced and the backup continues until it either completes or
  fails on its own.
- **`reconciliationTimeout`** (integer, seconds, default `300`) — sets the time allowed for
  the post-backup reconciliation phase (retention evaluation, WAL cleanup, and so on).
  Expiry of this timeout does **not** fail the backup; the reconciliation is deferred and
  retried the next time any backup job runs for the cluster. Set to `0` to disable the
  timeout entirely.
- **`retryDelay`** (ISO 8601 duration, default `PT1S`) — the initial delay used by the
  exponential backoff applied when a command inside the backup script is retried.
- **`retryMaxDelay`** (ISO 8601 duration, default `PT1M`) — the ceiling the exponential
  backoff delay may grow to before it stops increasing.
- **`retryLimit`** (integer, default `10`) — the maximum number of retries for each
  individual command executed by the backup script before that command is considered
  permanently failed.

## What to expect

Monitor the backup as it runs:

```bash
kubectl get sgbackup -n my-cluster cluster-backup -w
```

Once complete, inspect the result and any failure message:

```bash
kubectl get sgbackup -n my-cluster cluster-backup \
  -o jsonpath='{.status.process.status}'

kubectl get sgbackup -n my-cluster cluster-backup \
  -o jsonpath='{.status.process.failure}'
```

A successfully completed backup shows `status.process.status: Completed`. If it is still
retrying after a transient failure the status may cycle through intermediate states before
settling.

## Pitfalls

- **`reconciliationTimeout` expiry does not fail the backup.** A reconciliation timeout is
  a non-fatal condition. The backup itself is still marked `Completed` and the
  reconciliation (retention cleanup, WAL pruning) is silently deferred to the next backup
  job run. If reconciliation keeps timing out on every run your cluster's retained backups
  may grow beyond the intended retention window.
- **`timeout` cancels the backup.** Unlike `reconciliationTimeout`, a `timeout` expiry is
  fatal: the operator cancels the running backup job and marks the resource as `failed`.
  Make sure the value is high enough to accommodate your expected backup size, storage
  bandwidth, and any transient delays (network congestion, disk throttling).
- **`maxRetries: 0` disables top-level retries.** If the backup fails on the first attempt
  no further tries are made. This is useful to prevent repeated attempts from consuming
  storage I/O during a known maintenance window but should not be left as a permanent
  setting if the storage backend is prone to transient errors.
- **`retryDelay` and `retryMaxDelay` use ISO 8601 durations.** The format is
  `PnDTnHnMn.nS` (for example `PT30S` for 30 seconds, `PT2M` for 2 minutes). Providing a
  plain integer or an invalid string will cause the resource to be rejected by the webhook.
