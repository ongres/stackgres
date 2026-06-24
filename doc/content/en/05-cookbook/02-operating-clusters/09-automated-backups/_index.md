---
title: Configuring automated backups
weight: 9
url: /cookbook/operating-clusters/automated-backups
description: Schedule recurring backups and a retention policy on a cluster.
showToc: true
---

## What it does

Adds a `backups` entry to `spec.configurations` of a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to point it at an
[SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}), set a cron
schedule for periodic base backups, and limit stored backups with a retention count. The
operator reconciles the change in place; no cluster restart is required.

## When to use it

- You want to enable or update the recurring base-backup schedule for an existing cluster.
- You need to enforce a retention policy so old backups are pruned automatically.
- You are switching the storage backend or compression algorithm without recreating the
  cluster.

## How to do it

### 1. Ensure an SGObjectStorage exists

The cluster must reference an
[SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) that is already
present in the same namespace. Create one first if it does not exist yet.

### 2. Patch the SGCluster

Add or update `spec.configurations.backups` on the cluster:

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
  configurations:
    backups:
    - sgObjectStorage: my-object-storage   # name of an SGObjectStorage in this namespace
      cronSchedule: "0 2 * * *"            # daily at 02:00 UTC
      retention: 7                          # keep the 7 most recent base backups
      compression: lz4                      # lz4 (default) | lzma | zstd | brotli
```

```bash
kubectl apply -f cluster.yaml
```

The `backups` field is a list, so you can configure more than one backup destination; each
entry is independent.

### 3. Verify the schedule is active

```bash
kubectl get sgcluster -n my-cluster cluster \
  -o jsonpath='{.spec.configurations.backups[0].cronSchedule}'
```

Check that a scheduled backup job appears at the expected time:

```bash
kubectl get sgbackup -n my-cluster
```

## How it works

StackGres reads `spec.configurations.backups` and creates a Kubernetes `CronJob` for each
entry. At each scheduled time the job triggers WAL-G to write a new base backup to the
object storage location indicated by the `SGObjectStorage`. Continuous WAL archiving runs
independently between base backups so that point-in-time recovery is always possible
within the window covered by stored WAL.

After each successful backup, the operator evaluates the `retention` count and deletes the
oldest base backups (and their associated WAL) that exceed it. The `path` field, if
omitted, is filled in by the operator and should not be changed afterward.

## What to expect

- The first automatic backup runs at the next cron trigger. To create an on-demand backup
  immediately, create an [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) resource
  pointing at the same cluster.
- Watch backup status:

  ```bash
  kubectl get sgbackup -n my-cluster -w
  ```

- Completed backups appear with `status.process.status: Completed`. Failed ones show error
  details under `status.process.failure`.

## Pitfalls

- **SGObjectStorage must exist before the cluster references it.** The validating webhook
  rejects an `SGCluster` whose `backups[].sgObjectStorage` name does not resolve to an
  existing `SGObjectStorage` in the same namespace. Create the storage resource first.
- **`retention` is a count of base backups, not a duration.** Setting `retention: 7` keeps
  the seven most recent base backups; the actual time covered depends on `cronSchedule`
  frequency. It does not mean "seven days".
- **`path` is dangerous to set manually.** If two clusters share the same `path` their WAL
  histories will be mixed, producing unrecoverable backups. Leave the field unset and let
  the operator populate it.
- **Volume snapshots require a compatible CSI driver.** If you set `useVolumeSnapshot: true`
  the node must have a CSI driver that supports the `VolumeSnapshot` API, and
  `volumeSnapshotClass` must reference a valid `VolumeSnapshotClass`. WAL archiving to an
  `SGObjectStorage` is still required even when using volume snapshots.
- **`cronSchedule` omitted means no automatic base backups.** WAL archiving runs
  continuously, but base backups are never triggered unless `cronSchedule` is set. Restores
  require a base backup to exist.
