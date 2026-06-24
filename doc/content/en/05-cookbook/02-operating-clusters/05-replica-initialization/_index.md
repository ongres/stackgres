---
title: Replica initialization strategy
weight: 5
url: /cookbook/operating-clusters/replica-initialization
description: Choose how new replicas are cloned when the cluster scales up or self-heals.
showToc: true
---

## What it does

Controls how the operator seeds a new standby Pod — whether it streams a base backup
directly from the primary, copies from an existing replica, or restores from an SGBackup
object. You configure this through `spec.replication.initialization` on an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). The default mode is
`FromExistingBackup`.

## When to use it

- You are scaling up a cluster and want to avoid the extra load that `pg_basebackup`
  places on the primary.
- Your cluster already maintains continuous backups and you want new replicas to restore
  from them instead of streaming from a live instance.
- A replica Pod was evicted or its PVC was lost and the operator is rebuilding it; you
  want to control where it seeds from.
- You need to cap the network or disk bandwidth that a seeding operation is allowed to
  consume.

## How to do it

### Stream from the primary

The simplest strategy — always run `pg_basebackup` from the primary:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  replication:
    initialization:
      mode: FromPrimary   # pg_basebackup always targets the primary
  # ...rest unchanged
```

```bash
kubectl apply -f cluster.yaml
```

### Clone from an existing replica

Offloads the seeding I/O from the primary; falls back to `FromPrimary` if no healthy
replica is available:

```yaml
spec:
  replication:
    initialization:
      mode: FromReplica
```

### Restore from a recent backup

The default. The operator picks the most recent SGBackup. Use `backupNewerThan` (ISO 8601
duration) to set a freshness threshold — if no backup is newer than that value the mode
falls back to `FromReplica`:

```yaml
spec:
  replication:
    initialization:
      mode: FromExistingBackup  # default
      backupNewerThan: P1D      # only use backups younger than 1 day
      backupRestorePerformance:
        downloadConcurrency: 4  # parallel download streams (default: min(files, 10))
        maxNetworkBandwidth: 104857600  # 100 MiB/s cap
        maxDiskBandwidth: 52428800      # 50 MiB/s cap
```

### Always create a fresh backup first

`FromNewlyCreatedBackup` triggers a new SGBackup before seeding. If `backupNewerThan` is
set and a recent-enough backup already exists, it reuses that one instead of creating
another:

```yaml
spec:
  replication:
    initialization:
      mode: FromNewlyCreatedBackup
      backupNewerThan: PT6H   # skip creation if a backup < 6 h old exists
```

## How it works

When the operator needs to initialize a standby (scale-up or Pod rebuild) it reads
`spec.replication.initialization.mode` and acts accordingly:

- `FromPrimary` / `FromReplica` — runs `pg_basebackup` against the chosen source.
  `FromReplica` falls back to `FromPrimary` when no replica is reachable.
- `FromExistingBackup` — selects the newest SGBackup that satisfies `backupNewerThan`
  (if set) and restores it using the WAL-G restore path. Falls back through older backups
  and eventually to `FromReplica` if none remain.
- `FromNewlyCreatedBackup` — creates a new SGBackup, waits for it to complete, then
  restores it. Falls back to `FromExistingBackup` behaviour when a sufficiently recent
  backup already exists.

`backupRestorePerformance` throttles the restore I/O: `downloadConcurrency` controls
parallel object-store streams; `maxNetworkBandwidth` and `maxDiskBandwidth` impose byte-
per-second caps on network and disk respectively.

The field is reconciled live — you can patch a running SGCluster and the new mode takes
effect the next time a standby Pod is initialized.

## What to expect

- With `FromPrimary` or `FromReplica`, watch the new Pod's logs for `pg_basebackup`
  progress. Large databases will keep the source busy for the duration.
- With `FromExistingBackup` or `FromNewlyCreatedBackup`, the Pod enters a restore phase
  before streaming catches up; this is usually faster than a full base backup over the
  network but depends on backup freshness and object-store throughput.
- After seeding completes, Patroni connects the new instance to the replication stream
  and it catches up with WAL replay.

## Pitfalls

- **Backup modes require a configured backup.** `FromExistingBackup` and
  `FromNewlyCreatedBackup` fail (and fall back) if there are no SGBackup objects in the
  same namespace, or if no backup configuration is attached to the cluster. Verify your
  backup schedule is active before relying on these modes.
- **`FromPrimary` adds load to the primary.** For large clusters or high-traffic
  primaries, prefer `FromReplica` or a backup-based mode to avoid saturating the
  primary's network and I/O during a scale-up or self-heal event.
- **`backupNewerThan` affects fallback behavior.** If no SGBackup satisfies the
  threshold, both backup modes fall back to `FromReplica` (and ultimately `FromPrimary`),
  which may still load the primary — set the threshold generously or ensure your backup
  schedule runs frequently enough.
- **`FromNewlyCreatedBackup` holds the Pod in init until the backup finishes.** On large
  databases the standby Pod will not join the cluster until the fresh backup completes.
  Use `backupNewerThan` to allow reuse of a recent backup and avoid the wait.
