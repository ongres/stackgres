---
title: Copying a backup across namespaces
weight: 3
url: /cookbook/managing-backups/cross-namespace-copy
description: Reference a completed backup from another namespace.
showToc: true
---

## What it does

Creates an [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) in a target namespace
that references a completed backup taken by a cluster living in a different namespace. The
operator reconciles the new resource against the existing backup data in object storage —
no new base backup is taken.

The cross-namespace reference is expressed through `spec.sgCluster` using the
`<source namespace>.<cluster name>` form.

## When to use it

- You need to restore a cluster from a backup that was taken in a different namespace (for
  example, promoting a staging backup to production).
- You want to make a backup logically visible in a second namespace without copying the
  underlying data in object storage.
- You are building a multi-tenant setup where a central namespace owns backups that other
  teams reference.

## How to do it

Assume the source backup exists in namespace `my-cluster` for cluster `cluster`. Create the
cross-namespace SGBackup in the target namespace (here `my-restore`):

```bash
kubectl create namespace my-restore
```

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  namespace: my-restore           # target namespace
  name: cluster-backup-copy
spec:
  # Namespace-prefixed form: <source namespace>.<cluster name>
  sgCluster: my-cluster.cluster
  managedLifecycle: false         # keep permanent; not subject to retention pruning
```

```bash
kubectl apply -f backup-copy.yaml
```

`spec.sgCluster` is set to `my-cluster.cluster` — the dot-separated namespace and cluster
name tells the operator this is a cross-namespace reference to a completed backup rather
than a request to take a new one.

Setting `managedLifecycle: false` (the default) makes the copy permanent so that the
retention policy of the source cluster cannot remove it.

Watch the operator reconcile the resource:

```bash
kubectl get sgbackup -n my-restore cluster-backup-copy -o yaml
```

Once reconciled, `status.process.status` transitions to `Completed` and
`status.backupPath` reflects the path in object storage.

## How it works

When the operator sees `spec.sgCluster` containing a dot, it treats the value as
`<namespace>.<clusterName>` and looks up the corresponding completed SGBackup in the
source namespace. It copies the backup metadata (backup path, internal name, configuration
snapshot) into the `status` of the new SGBackup without transferring any data between
storage locations. Both SGBackup resources then point to the same backup artefacts in
object storage.

Because no data is moved, the target namespace must have network and credential access to
the same object storage bucket used by the source cluster. If the source backup is itself
already a cross-namespace copy, `spec.sgCluster` may reuse the same prefixed value of
that copy.

## What to expect

- The new SGBackup in `my-restore` appears with `status.process.status: Completed`
  shortly after the operator reconciles it.
- The `status.backupPath` and `status.internalName` match those of the original backup.
- The copy is visible in the StackGres Web UI under the target namespace.
- A cluster in `my-restore` can reference this SGBackup for a restore without
  cross-namespace access to the source cluster.

## Pitfalls

- **The prefixed form is required for cross-namespace copies.** Using a plain cluster name
  (without the `<namespace>.` prefix) causes the operator to look for a cluster in the
  same namespace as the SGBackup, which will fail if no such cluster exists there.
- **Object storage must be reachable from both namespaces.** The backup data lives in the
  source cluster's object storage. The target namespace (and any cluster that restores from
  the copy) must be able to reach the same bucket with valid credentials. Verify that the
  SGObjectStorage configuration — or the equivalent credentials Secret — is accessible from
  the target namespace.
- **Source backup must be completed.** The cross-namespace reference only works against a
  backup with `status.process.status: Completed`. Referencing an in-progress or failed
  backup leaves the copy stuck in a pending state.
- **Deleting the copy does not delete the source data.** Removing the SGBackup in the
  target namespace only removes the Kubernetes resource; the underlying backup artefacts
  in object storage are governed by the source cluster's retention policy (or its own
  permanent SGBackup).
