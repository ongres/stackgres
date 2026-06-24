---
title: Choosing a storage class
weight: 7
url: /cookbook/creating-clusters/storage-class
description: Pin the StorageClass used for the cluster persistent volumes.
showToc: true
---

## What it does

Pins the Kubernetes `StorageClass` used to provision the
`PersistentVolume` for every instance in an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). The field
`spec.pods.persistentVolume.storageClass` names an existing `StorageClass`; if it is
omitted the cluster inherits whichever `StorageClass` is marked as the cluster-wide default.

## When to use it

- Your cluster runs on a platform with multiple `StorageClass` options (for example a
  fast NVMe class and a cheaper spinning-disk class) and you want to pin Postgres data
  to a specific one.
- The cluster has no default `StorageClass`, so PVCs would stay unbound unless you name
  one explicitly.
- You need consistent, reproducible cluster definitions across environments (staging, prod)
  where the default `StorageClass` differs.

## How to do it

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
  instances: 3
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 20Gi                  # required: size of each instance PVC
      storageClass: fast-nvme     # name of an existing StorageClass; set at creation only
```

```bash
kubectl apply -f cluster.yaml
```

`storageClass` must match the name of a `StorageClass` that already exists in the cluster.
You can list available classes with:

```bash
kubectl get storageclass
```

`size` is required and must be expressed in mebibytes, gibibytes, or tebibytes (for example
`10Gi`). Both fields live under `spec.pods.persistentVolume`; see the
[SGCluster reference]({{% relref "06-crd-reference/01-sgcluster" %}}) for the full schema.

## How it works

When the operator reconciles the SGCluster it creates a Kubernetes `StatefulSet` with a
`volumeClaimTemplate` that references the named `StorageClass`. The Kubernetes volume
provisioner then creates one `PersistentVolume` per instance using that class. Because the
`volumeClaimTemplate` of a `StatefulSet` is immutable in Kubernetes, the `StorageClass`
choice is permanently bound to the PVCs at the moment they are first provisioned.

## What to expect

- Inspect the provisioned PVCs and confirm the correct `StorageClass` was applied:

  ```bash
  kubectl get pvc -n my-cluster -o wide
  ```

- Inspect the cluster status to confirm Pods are running and Patroni has elected a primary:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o yaml
  ```

## Pitfalls

- **Effectively creation-only.** The API field is marked updatable, so editing it is not
  rejected — but it has **no effect on existing instances**, because a `StatefulSet`'s
  `volumeClaimTemplate` is immutable in Kubernetes and the PVCs are already bound to their
  original class. To actually move to a different `StorageClass` you must create a new cluster
  (and restore data into it if needed).
- **No default `StorageClass`.** If `storageClass` is omitted and the cluster has no default
  `StorageClass`, the PVCs stay in `Pending` and no Pod ever starts. Always set the field
  explicitly when operating in environments without a cluster-wide default.
- **`StorageClass` must exist before the cluster is created.** The operator does not create
  the `StorageClass`; if the named class does not exist the PVC provisioning will fail. Verify
  the class is available with `kubectl get storageclass` before applying the SGCluster.
- **Resizing is a separate concern.** `size` (the PVC capacity) is updatable and can be
  increased after creation (subject to the `StorageClass` supporting volume expansion). The
  `storageClass` itself cannot be changed; only `size` can grow.
