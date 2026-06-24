---
title: Managed backup lifecycle
weight: 2
url: /cookbook/managing-backups/managed-lifecycle
description: Let the retention policy manage a backup, or keep it permanently.
showToc: true
---

## What it does

Sets `spec.managedLifecycle` on an existing
[SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) to control whether the automated
retention policy is allowed to delete it. The operator reconciles the field without
recreating the backup.

## When to use it

- You want to pin a specific backup permanently so the retention policy never removes it
  (for example, a baseline before a major migration).
- You want to hand a previously pinned backup back to the retention policy so it can be
  pruned when it falls outside the retention window.
- You are auditing which backups are protected and which are subject to automated cleanup.

## How to do it

### Inspect the current setting

```bash
kubectl get sgbackup -n my-cluster -o wide
kubectl get sgbackup -n my-cluster <backup-name> \
  -o jsonpath='{.spec.managedLifecycle}'
```

### Pin a backup permanently (opt out of retention)

Set `managedLifecycle: false` (the default). The retention policy will never delete this
backup; you must remove it manually with `kubectl delete sgbackup`.

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  namespace: my-cluster
  name: pre-migration-baseline    # name of the existing SGBackup
spec:
  sgCluster: cluster
  managedLifecycle: false         # default — retention policy will NOT delete this backup
```

```bash
kubectl apply -f pre-migration-baseline.yaml
```

### Hand a backup back to the retention policy

Set `managedLifecycle: true`. The next time any backup job runs for the cluster, the
operator evaluates the retention window and may delete this backup if it falls outside it.

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  namespace: my-cluster
  name: pre-migration-baseline
spec:
  sgCluster: cluster
  managedLifecycle: true          # retention policy CAN delete this backup
```

```bash
kubectl apply -f pre-migration-baseline.yaml
```

Alternatively, use a strategic-merge patch to avoid rewriting the full manifest:

```bash
kubectl patch sgbackup -n my-cluster pre-migration-baseline \
  --type=merge -p '{"spec":{"managedLifecycle":true}}'
```

## How it works

`spec.managedLifecycle` is an optional boolean (default `false`). The operator mirrors it
to `status.process.managedLifecycle` during reconciliation; the status field may lag
briefly while the operator converges.

After each successful backup job the operator runs retention evaluation. It counts the
base backups stored for the cluster and removes the oldest ones that exceed the `retention`
count configured on the `SGCluster`. Only backups whose `spec.managedLifecycle` is `true`
are candidates for deletion during this sweep. Backups with `managedLifecycle: false` are
invisible to the retention policy and remain in object storage until you delete the
`SGBackup` resource explicitly.

See the [SGBackup CRD reference]({{% relref "06-crd-reference/06-sgbackup" %}}) for the
full field list.

## What to expect

- After patching, confirm the value is stored:

  ```bash
  kubectl get sgbackup -n my-cluster pre-migration-baseline \
    -o jsonpath='{.spec.managedLifecycle}'
  ```

- The operator converges `status.process.managedLifecycle` within one reconciliation cycle.
  Watch it with:

  ```bash
  kubectl get sgbackup -n my-cluster pre-migration-baseline \
    -o jsonpath='{.status.process.managedLifecycle}'
  ```

- A backup marked `managedLifecycle: true` is not deleted immediately; it is only pruned if
  it falls outside the retention window the next time a backup job runs.

## Pitfalls

- **`managedLifecycle: true` means the retention policy can delete the backup.** This is
  the opposite of what the name might suggest. Think of it as "this backup participates in
  managed (automated) lifecycle events", which includes deletion. Setting it to `false`
  (the default) keeps the backup permanently until you remove it yourself.
- **The default is `false`.** On-demand backups created by applying an `SGBackup` manifest,
  as well as scheduled backups created by the operator, start with `managedLifecycle: false`
  unless you explicitly set it to `true`. Automated scheduled backups must be set to `true`
  if you want the retention count to take effect on them.
- **Deleting the `SGBackup` resource also removes the data from object storage.** When you
  manually delete an `SGBackup` (which is the only way to remove one with
  `managedLifecycle: false`), the operator removes the backup data from the object storage
  backend as part of the next reconciliation cycle.
- **`status.process.managedLifecycle` is transient.** Do not rely on the status field for
  authoritative intent; read `spec.managedLifecycle` instead.
