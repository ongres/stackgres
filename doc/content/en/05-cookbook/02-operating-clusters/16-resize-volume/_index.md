---
title: Resizing the persistent volume
weight: 16
url: /cookbook/operating-clusters/resize-volume
description: Grow the data volume of a running cluster.
showToc: true
---

## What it does

Increases the `pods.persistentVolume.size` field on a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). The operator reconciles the
change by expanding each PersistentVolumeClaim in place; no data movement or Pod recreation
is required.

## When to use it

- Disk usage on the data volume is approaching capacity and you need more headroom without
  rebuilding the cluster.
- You are preparing for a large data ingestion or a major Postgres upgrade that requires
  extra temporary space.
- You want to grow storage independently of CPU or memory (as opposed to resizing the
  instance profile).

## How to do it

Update `spec.pods.persistentVolume.size` to the desired new value. The new value must be
larger than the current one.

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
      size: 20Gi   # increased from the previous value, e.g. 10Gi
```

```bash
kubectl apply -f cluster.yaml
```

Or patch it directly without editing the full manifest:

```bash
kubectl patch sgcluster -n my-cluster cluster \
  --type=merge \
  -p '{"spec":{"pods":{"persistentVolume":{"size":"20Gi"}}}}'
```

Monitor the PVC expansion progress across all instances:

```bash
kubectl get pvc -n my-cluster -w
```

Once each PVC reaches `Bound` status with the new capacity, the expansion is complete.

## How it works

`pods.persistentVolume.size` is marked as `updatable` in the SGCluster spec. When the
operator detects a change it issues a resize request to each PersistentVolumeClaim backing
the cluster Pods. Kubernetes delegates the actual expansion to the CSI driver of the
StorageClass. Depending on the driver the expansion may happen online (while the PVC is
mounted) or require a Pod restart; most modern CSI drivers (including the cloud-provider
ones) support online expansion. The operator resizes each PVC in turn and the cluster
remains available throughout.

## What to expect

- Each PVC transitions briefly to `FilesystemResizePending` while the node-side resize
  completes, then returns to `Bound`. Confirm the new size:

  ```bash
  kubectl get pvc -n my-cluster \
    -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.capacity.storage}{"\n"}{end}'
  ```

- The change is reflected in the SGCluster status once all PVCs have been expanded.
- No failover or restarts are triggered solely by this operation under typical CSI drivers.

## Pitfalls

- **The StorageClass must allow volume expansion.** The StorageClass used by the cluster
  must have `allowVolumeExpansion: true`. If it does not, the PVC resize request is rejected
  by Kubernetes and the cluster status will show an error. Check with:

  ```bash
  kubectl get storageclass <name> -o jsonpath='{.allowVolumeExpansion}'
  ```

- **Volumes can only grow, not shrink.** Kubernetes does not support reducing the size of a
  PVC. Setting `size` to a value smaller than the current one is rejected by the validating
  webhook. To downsize storage, follow the Volume Downsize runbook under the 09-runbooks
  section of the administration guide.
- **Specify size in binary units.** The field accepts Mebibytes, Gibibytes, or Tebibytes
  (e.g. `500Mi`, `20Gi`, `2Ti`). Kubernetes normalises the value; always verify the
  resulting PVC capacity rather than relying on the raw spec string.
- **CSI driver capability varies.** Some older or on-premises CSI drivers require the Pod to
  be restarted before the filesystem sees the new capacity. Check the driver documentation
  and watch for a `FileSystemResizeFailed` event on the PVC if expansion stalls.
