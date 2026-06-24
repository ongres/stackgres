---
title: Limiting disk I/O
weight: 17
url: /cookbook/operating-clusters/disk-io-limits
description: Cap the read/write IOPS and bandwidth of cluster Pods.
showToc: true
---

## What it does

Applies per-Pod disk I/O throttling via the cgroup v2 `io.max` interface. You set limits
through `spec.pods.persistentVolume.ioLimits` in an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}):
`readIops`, `writeIops`, `readMiBps`, and `writeMiBps`. Each field is independent and
optional; set only the ones you need. All four fields are updatable on a running cluster.

> **Alpha feature.** The `io-limits` feature gate must be enabled in the SGConfig before
> any `ioLimits` value is accepted by the validating webhook.

## When to use it

- Multiple StackGres clusters share the same local storage device (e.g. NVMe) on a node
  and one cluster's heavy workload — a large COPY, aggressive VACUUM, or an unoptimized
  query — saturates I/O for its neighbors.
- You want to give each cluster a guaranteed slice of device bandwidth or IOPS on
  on-premises nodes using local-volume CSI drivers such as TopoLVM or OpenEBS LocalPV.
- You need a soft ceiling on a non-production cluster so it cannot starve production
  workloads sharing the same hardware.

## How to do it

### Enable the feature gate

The feature gate must be enabled at the operator level before any cluster can use it.
Edit or patch your SGConfig:

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  namespace: stackgres
  name: stackgres
spec:
  featureGates:
  - io-limits
```

```bash
kubectl apply -f sgconfig.yaml
```

### Set I/O limits on the cluster

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 100Gi
      ioLimits:
        readIops: 10000    # max read IOPS per Pod
        writeIops: 8000    # max write IOPS per Pod
        readMiBps: 500     # max read throughput in MiB/s per Pod
        writeMiBps: 400    # max write throughput in MiB/s per Pod
```

```bash
kubectl apply -f cluster.yaml
```

You can omit any of the four fields to leave that dimension unlimited. To remove a limit
later, delete the field (or set the whole `ioLimits` object to `{}`) and re-apply.

## How it works

When any `ioLimits` value is set, the operator injects an init container named
`setup-io-limits` into each Pod. That init container runs as root (with only the `CHOWN`
capability) and writes the requested limits to the `io.max` file in the Pod's cgroup v2
hierarchy under `/sys/fs/cgroup`. The `cluster-controller` sidecar also mounts
`/sys/fs/cgroup` from the host so it can update the limits during reconciliation without
restarting Pods.

Limits are enforced at the block-device layer, independently per Pod. Because each Pod
owns its own cgroup slice, two clusters sharing a physical NVMe device get separate
enforcement entries. However, if multiple volumes within a single Pod map to the same
backing device, they share one `io.max` entry and cannot be throttled independently of
each other.

Changing the `ioLimits` fields is a day-two, in-place operation: the operator reconciles
the running StatefulSet and updates the limits without re-creating Pods.

## What to expect

- Once applied, `kubectl describe pod -n my-cluster` shows the `setup-io-limits` init
  container in each Pod's init container list.
- Limits apply per Pod, so a three-instance cluster effectively reserves up to
  `instances × limit` from the shared device.
- Throughput-sensitive operations (WAL writes, checkpoints, VACUUM) slow down if they
  exceed the configured ceiling. Monitor `pg_stat_bgwriter` and WAL metrics to detect
  artificial stalls.

## Pitfalls

- **Feature gate required.** Submitting a cluster with any `ioLimits` value without the
  `io-limits` feature gate in SGConfig is rejected by the validating webhook.
- **cgroup v2 nodes only.** Nodes still running cgroup v1 do not support `io.max`; the
  init container will fail and the Pod will not start. Verify cgroup v2 with
  `stat -fc %T /sys/fs/cgroup` — it must report `cgroup2fs`.
- **Root init container.** The `setup-io-limits` container runs as root. Environments with
  restrictive security policies (for example an OpenShift SCC that blocks root containers)
  cannot use this feature.
- **Overly tight limits degrade performance.** Setting limits too low causes WAL writes to
  stall, increasing commit latency and potentially triggering replication lag on standbys.
  Start conservatively, measure actual I/O under peak load, and increase limits if you
  observe unexpected slowdowns.
- **CSI driver must expose block-device paths.** The cgroup `io.max` mechanism targets
  block-device major:minor numbers. If the CSI driver virtualizes the device and does not
  expose the underlying block device, the limits have no effect.
