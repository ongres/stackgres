---
title: Point-in-time recovery (PITR)
weight: 3
url: /cookbook/creating-clusters/point-in-time-recovery
description: Bootstrap a new cluster and recover its data to a specific moment in the past.
showToc: true
---

## What it does

Bootstraps a new cluster from an existing
[SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) and replays WAL up to a chosen
recovery target — a timestamp, LSN, transaction ID, or named restore point. This extends
the *Restoring from a backup* recipe by letting you stop recovery at any point after the
backup was taken, not just at the backup boundary.

## When to use it

- A destructive operation (accidental `DROP TABLE`, bad migration) happened at a known time
  and you need the data as it was just before that instant.
- You want to verify the database state at an arbitrary moment in the past without touching
  the live cluster.
- You need to replay to a specific transaction ID or LSN for forensic or compliance purposes.

This is a **creation-only** capability: it only applies at cluster bootstrap. To recover to a
different point, create another cluster.

## How to do it

### Timestamp-based recovery

List the available backups to pick one that precedes your target time:

```bash
kubectl get sgbackups -n my-cluster
```

```
NAME           AGE
backup-2024    5h12m
```

Create a new SGCluster with `spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp`:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 1
  postgres:
    version: '17.10'
  pods:
    persistentVolume:
      size: 10Gi
  initialData:
    restore:
      fromBackup:
        name: backup-2024               # SGBackup taken before the target time
        pointInTimeRecovery:
          restoreToTimestamp: "2024-03-15T14:30:00Z"  # ISO 8601 UTC timestamp
      downloadDiskConcurrency: 1
```

```bash
kubectl apply -f cluster.yaml
```

### Alternative recovery targets

Instead of (or in addition to) `pointInTimeRecovery`, the `fromBackup` object accepts
several mutually-understood PostgreSQL recovery targets directly as sibling fields:

```yaml
      fromBackup:
        name: backup-2024
        # Stop at a named restore point created with pg_create_restore_point():
        targetName: "before-migration"

        # Or stop at a specific WAL LSN:
        # targetLsn: "0/50000A8"

        # Or stop at a specific transaction ID:
        # targetXid: "12345"

        # Stop as soon as a consistent state is reached (earliest possible):
        # target: immediate

        # Recover into a specific timeline (default: same as the backup's):
        # targetTimeline: latest

        # Include the transaction at the target boundary (default: true):
        # targetInclusive: true
```

Only one recovery-target field (`pointInTimeRecovery`, `targetName`, `targetXid`,
`targetLsn`, or `target`) should be set at a time. See
[SGCluster reference]({{% relref "06-crd-reference/01-sgcluster" %}}) for the full field
descriptions.

## How it works

1. The new cluster's primary Pod starts in restore mode, fetching the base backup referenced
   by the named SGBackup.
2. Patroni replays WAL segments from object storage, advancing until it reaches the
   specified recovery target.
3. Recovery stops at that point; the primary opens for read/write, and replicas (if
   `instances` > 1) are then cloned from it.

The recovery target fields map directly to PostgreSQL's `recovery_target_time`,
`recovery_target_lsn`, `recovery_target_xid`, `recovery_target_name`, and `recovery_target`
parameters written into `recovery.conf` (or `postgresql.conf` on Postgres 12+).

## What to expect

- Startup takes longer than a fresh cluster. The closer your target time is to the backup
  timestamp, the faster WAL replay completes. Watch progress:

  ```bash
  kubectl logs -n my-cluster cluster-0 -c patroni -f
  ```

- The cluster comes up with the data exactly at the recovery boundary. Any transactions after
  that point are gone; they do not appear even after the cluster is running.
- Verify the recovered state before promoting or routing production traffic to the new cluster.

## Pitfalls

- **`restoreToTimestamp` must be within the WAL window.** The timestamp must fall between
  the chosen SGBackup's completion time and the next backup's start (or the current time if
  it is the latest backup). A timestamp outside the available WAL archive causes recovery to
  stall or fail at WAL fetch time.
- **`initialData` is immutable.** The webhook rejects any update to `spec.initialData` on a
  running cluster with *"Cannot update SGCluster's restore configuration"*. Plan the target
  carefully at creation time; to recover to a different point, create a new cluster.
- **Choose the right backup.** The SGBackup must have been taken *before* the recovery
  target. Picking a backup newer than your target time means recovery has no WAL to replay
  backwards — PostgreSQL physical recovery only moves forward.
- **WAL archive availability.** All WAL segments between the backup and the target must
  still exist in object storage. If retention policies have removed them, recovery halts.
  Verify the
  [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) retention settings
  and that credentials are available in the target namespace.
- **`targetInclusive` boundary semantics.** By default the transaction at the exact target
  boundary is included. Set `targetInclusive: false` to stop *just before* it — useful when
  the target transaction is the one you want to exclude.
