---
title: Tuning restore performance
weight: 4
url: /cookbook/creating-clusters/restore-performance
description: Speed up restoring a backup when bootstrapping a cluster.
showToc: true
---

## What it does

Controls how many parallel streams the backup fetch process uses when bootstrapping a new
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) from an existing backup. The
field `spec.initialData.restore.downloadDiskConcurrency` sets the stream count; raising it
above `1` enables parallel fetching and can significantly cut restore time for large
databases.

## When to use it

- Your backup is large (hundreds of gigabytes or more) and a single-stream restore is
  noticeably slow.
- The object storage backend and the cluster nodes have enough network bandwidth and disk
  I/O headroom to justify parallelism.
- You are cloning a production database and want the staging cluster available as quickly as
  possible.

This setting only makes sense at cluster creation time — it has no effect on a running
cluster (see Pitfalls).

## How to do it

Create the cluster with `downloadDiskConcurrency` set in `spec.initialData.restore`:

```bash
kubectl create namespace my-cluster
```

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 1
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 50Gi
  initialData:
    restore:
      fromBackup:
        name: my-backup       # name of an existing SGBackup in this namespace
      downloadDiskConcurrency: 4   # fetch 4 streams in parallel instead of 1
```

```bash
kubectl apply -f cluster.yaml
```

`downloadDiskConcurrency` is an integer with a minimum value of `1`. Omitting it leaves the
operator to apply its default. Setting it to `1` disables parallelism (single-stream
restore, same as the default). Values of `2` or higher enable parallel fetching. Choose a
value that fits the available I/O; `4`–`8` is a reasonable starting point for a fast
network with NVMe storage.

The `fromBackup.name` field must reference a completed
[SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) in the same namespace.

## How it works

When the primary Pod starts in restore mode, StackGres instructs the WAL-G backup tool to
open multiple concurrent connections to object storage and download different segments of
the base backup at the same time. The segments are assembled into a consistent base backup
on the local volume, after which WAL replay brings the data to the point the backup
represents. Once the primary is consistent, any additional instances are cloned from it via
streaming replication as usual.

The concurrency setting affects only the base-backup fetch phase; WAL replay is always
sequential.

## What to expect

- Restore time decreases roughly in proportion to the number of streams, up to the
  bottleneck (network throughput, storage IOPS, or object-storage API rate limits).
- Watch progress in the primary Pod logs:

  ```bash
  kubectl logs -n my-cluster cluster-0 -c patroni -f
  ```

- After the primary reaches a ready state, subsequent Pods join as replicas through
  streaming replication. Their startup time is independent of `downloadDiskConcurrency`.

## Pitfalls

- **`initialData` is immutable.** The validating webhook rejects any attempt to change
  `spec.initialData` — including `downloadDiskConcurrency` — on a running cluster. Decide
  on the concurrency value before creating the cluster; to change it you must create a new
  cluster from the backup.
- **Higher concurrency increases I/O pressure.** Each additional stream adds network
  bandwidth consumption and disk write load. On nodes with limited network or storage
  throughput, an overly high value can saturate resources and slow the restore or starve
  other workloads.
- **Object-storage API rate limits.** Some object-storage providers throttle concurrent
  requests. If the restore fails or slows unexpectedly, lower the concurrency value and
  check the Pod logs for rate-limit errors.
- **Verify the SGBackup first.** Regardless of the concurrency setting, the restore fails if
  the referenced SGBackup or its WAL segments are missing or expired. Confirm the backup is
  in `Completed` state before bootstrapping.
