---
title: Taking an on-demand backup
weight: 1
url: /cookbook/managing-backups/on-demand-backup
description: Trigger a one-off backup of a cluster.
showToc: true
---

## What it does

Creates an [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) resource that
immediately triggers a full backup of the target cluster. The operator spawns a dedicated
Job, uploads the backup to the object storage configured in the cluster, and records the
outcome — along with size, timing, and LSN information — in the resource's `status`.

## When to use it

- You need a point-in-time safety net before a risky change (schema migration, major
  upgrade, bulk delete).
- Automated scheduled backups are not frequent enough for a particular workload window.
- You want to copy a consistent snapshot to a different namespace or storage location.

## How to do it

The cluster must already have backup storage configured (see the *Automated backups*
recipe and the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}})
reference). Once that is in place, apply a minimal SGBackup:

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  namespace: my-cluster
  name: pre-migration-backup  # any descriptive name
spec:
  sgCluster: cluster           # must match the SGCluster name in the same namespace
  managedLifecycle: false      # false (default) — this backup is kept permanently
```

```bash
kubectl apply -f pre-migration-backup.yaml
```

Watch progress until `status.process.status` transitions to `completed`:

```bash
kubectl get sgbackup -n my-cluster pre-migration-backup -o yaml
```

Or use `-w` to stream events:

```bash
kubectl get sgbackup -n my-cluster -w
```

To create a backup that the automated retention policy can clean up once it falls outside
the retention window, set `managedLifecycle: true`.

## How it works

When the SGBackup is created the operator reconciles it and:

1. Schedules a Kubernetes Job on the primary Pod of the target cluster.
2. The Job takes a full base backup via `pgbackrest` (or volume snapshot, depending on the
   cluster's backup configuration) and streams it to the object storage defined by the
   SGObjectStorage referenced in the cluster's `configurations.backups` section.
3. WAL segments continue to flow to the same storage, keeping the backup recoverable via
   PITR.
4. After the backup completes, the Job runs a retention reconciliation that removes
   backups with `managedLifecycle: true` that fall outside the configured retention window.
5. The operator writes the result — status, timing, compressed/uncompressed size, LSN
   boundaries, and the source Pod — into `status`.

See the [SGBackup reference]({{% relref "06-crd-reference/06-sgbackup" %}}) for a full
description of the status fields.

## What to expect

- The `status.process.status` field progresses through `Running` and then `Completed`
  (or `Failed` if something goes wrong).
- `status.backupInformation.size.compressed` and `.uncompressed` report the backup size
  in bytes once the backup completes.
- `status.process.timing.start`, `.end`, and `.stored` record when the backup began,
  finished, and was safely committed to object storage.
- On failure, `status.process.failure` contains the error message. The operator retries
  up to `spec.maxRetries` times (default `3`) before marking the backup as `Failed`.

## Pitfalls

- **No backup storage configured.** SGBackup requires the target cluster to have at least
  one entry in `spec.configurations.backups` that points to a valid
  [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}). Applying an
  SGBackup against a cluster without this configuration will fail immediately.
- **Permanent backups accumulate.** `managedLifecycle: false` (the default) means the
  backup is never removed automatically. Create permanent backups deliberately and delete
  the SGBackup resource when it is no longer needed to avoid unbounded storage growth.
- **Retention reconciliation runs after every backup.** When the Job finishes, the
  operator also enforces the retention window and may delete older managed backups. If
  reconciliation itself fails, the backup still succeeds and the reconciliation is
  retried on the next backup event.
- **Cross-namespace copies.** To back up a cluster into a different namespace, prefix
  `spec.sgCluster` with the source namespace: `<source-namespace>.<cluster-name>`.
  The source backup must already exist and be in `Completed` state.
