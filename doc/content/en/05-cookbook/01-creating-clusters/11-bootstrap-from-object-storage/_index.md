---
title: Bootstrapping from object storage
weight: 11
url: /cookbook/creating-clusters/bootstrap-from-object-storage
description: Continuously replicate from backups stored in object storage.
showToc: true
---

## What it does

Turns a new [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) into a read-only
standby that continuously applies WAL segments fetched from an
[SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) bucket. You point
`spec.replicateFrom.storage` at a WAL-G-compatible object storage path and supply the source
cluster's credentials via `spec.replicateFrom.users`; the operator keeps the standby in sync
for as long as the section is present.

## When to use it

- Building a warm standby (disaster recovery target) that lags at most one WAL segment
  behind the source, without a direct network path between the two clusters.
- Cross-region or cross-cloud replication where object storage is the only common layer.
- Keeping a staging cluster continuously up to date from production backups.

## How to do it

### 1. Prerequisites

An [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) pointing at the
bucket that the source cluster writes to must already exist in the **target** namespace, and
the matching credentials Secret must be present there too.

### 2. Bootstrap the standby from a backup

Because the `storage`-only replication mode has no live primary to clone from, the cluster
must start from a base backup. Create the cluster referencing both the restore backup and
the replication source in one manifest:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 1
  postgres:
    version: '17'
  pods:
    persistentVolume:
      size: 10Gi
  initialData:
    restore:
      fromBackup:
        name: source-backup          # SGBackup in the same namespace
  replicateFrom:
    storage:
      sgObjectStorage: source-backups  # SGObjectStorage name in this namespace
      path: /source-cluster            # path inside the bucket (WAL-G layout)
    users:
      superuser:
        username:
          name: source-credentials
          key: superuser-username
        password:
          name: source-credentials
          key: superuser-password
      replication:
        username:
          name: source-credentials
          key: replication-username
        password:
          name: source-credentials
          key: replication-password
      authenticator:
        username:
          name: source-credentials
          key: authenticator-username
        password:
          name: source-credentials
          key: authenticator-password
```

```bash
kubectl apply -f cluster.yaml
```

`replicateFrom.storage.sgObjectStorage` is the name of the SGObjectStorage resource in the
same namespace. `replicateFrom.storage.path` is the prefix inside the bucket where the
source cluster's WAL-G files are stored.

`replicateFrom.users` holds `SecretKeySelector` references for the `superuser`,
`replication`, and `authenticator` roles (all required); `monitor` is optional.

### 3. Promote the standby

Removing `spec.replicateFrom` and re-applying converts the standby leader into a primary and
makes the cluster writable:

```bash
kubectl patch sgcluster -n my-cluster cluster \
  --type=json \
  -p '[{"op":"remove","path":"/spec/replicateFrom"}]'
```

## How it works

The operator configures Patroni in standby-cluster mode. After the base-backup restore
finishes, Patroni continuously fetches WAL segments from the object storage path through
WAL-G and replays them. There is no streaming connection to the source cluster; replication
lag equals the time between a WAL segment being archived and being fetched. Both
`spec.replicateFrom.storage` and `spec.replicateFrom.users` are updatable, so the source
can be redirected without recreating the cluster.

## What to expect

- First startup is slower than a fresh cluster because it restores the base backup before
  entering standby mode. Monitor progress in the primary Pod:

  ```bash
  kubectl logs -n my-cluster cluster-0 -c patroni -f
  ```

- The standby is read-only for as long as `spec.replicateFrom` is present. All writes are
  rejected.
- Replication lag depends on how frequently the source cluster archives WAL segments and on
  object storage latency; it is typically in the range of seconds to a few minutes.

## Pitfalls

- **Object storage must be reachable from the target namespace.** The SGObjectStorage resource
  and its credentials Secret must exist in the same namespace as the standby cluster. Copying
  credentials across namespaces is not automatic.
- **`initialData` is immutable.** `spec.initialData.restore` can only be set at creation time.
  If you omit it, Patroni has no base to start from and the cluster never becomes ready.
- **Path must match the source exactly.** WAL-G will find no files if `path` does not match
  the prefix used when the source cluster archives. Check the source SGBackup or the WAL-G
  archive path in the source cluster's backup configuration.
- **User credentials must match the source.** The `replicateFrom.users` secrets must contain
  the actual passwords of the source cluster's roles; mismatched credentials cause Patroni
  to fail role synchronization after promotion.
- **Promotion is irreversible without a new restore.** Once `spec.replicateFrom` is removed
  the cluster becomes an independent primary. Re-adding the section reattaches it as a standby
  only if the data on disk is still consistent with the source timeline.
