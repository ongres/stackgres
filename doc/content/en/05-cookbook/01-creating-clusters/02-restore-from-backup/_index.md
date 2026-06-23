---
title: Restoring from a backup
weight: 2
url: /cookbook/creating-clusters/restore-from-backup
description: Create a new SGCluster whose data is initialized from an existing SGBackup.
showToc: true
---

## What it does

Bootstraps a **new** cluster from the data of an existing
[SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}), instead of from an empty
`initdb`. The primary's first start restores the backup (base backup plus WAL replay), so
the new cluster comes up with the data as it was at the time of the backup.

## When to use it

- Recovering from a disaster: rebuild a cluster from the last good backup.
- Cloning a database into a new cluster — for staging, testing, or a migration dry-run.
- Combined with [Point-in-time recovery]({{% relref "05-cookbook/01-creating-clusters" %}}),
  to roll the data forward to a specific instant.

This is a **creation-only** capability: `spec.initialData` only affects bootstrap and cannot
be changed afterwards. Restoring into an *existing* cluster is not possible — you always
create a new cluster.

## How to do it

First, find the backup to restore from. Backups live in the same namespace and are listed as
SGBackup resources:

```bash
kubectl get sgbackups -n my-cluster
```

```
NAME                AGE
backup-demo-1       3h33m
```

Then create a new SGCluster that references it via `spec.initialData.restore.fromBackup.name`:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: restored-cluster
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
        name: backup-demo-1
      downloadDiskConcurrency: 1
```

```bash
kubectl apply -f restored-cluster.yaml
```

`downloadDiskConcurrency` controls how many streams are used to fetch the backup from object
storage; raise it to speed up restores of large databases at the cost of more network and
disk I/O.

## How it works

1. The new cluster's primary Pod starts in restore mode rather than running `initdb`.
2. StackGres configures Patroni to fetch the base backup referenced by the SGBackup and
   replay the WAL needed to make the data consistent.
3. Once the primary is up and consistent, replicas (if `instances` > 1) are cloned from it,
   and the cluster behaves like any other from then on.

The backup must be reachable: its WAL and base backup must still exist in the object storage
configured for the source cluster, and that storage must be accessible from the new cluster's
namespace.

## What to expect

- The first start takes longer than a fresh cluster — restore time scales with database size
  and the amount of WAL to replay. Watch progress in the primary Pod logs:

  ```bash
  kubectl logs -n my-cluster restored-cluster-0 -c patroni -f
  ```

- The restored Postgres version should match (or be compatible with) the version the backup
  was taken from. Restoring across major Postgres versions is not supported by a physical
  backup restore.
- The new cluster is independent: it does not continue the source cluster's backup chain
  unless you configure backups on it explicitly.

## Pitfalls

- **`initialData` is immutable.** Editing `spec.initialData` on a running cluster is rejected
  by the webhook with *"Cannot update SGCluster's restore configuration"*. Plan the restore at
  creation time; to restore again, create another cluster.
- **Missing or expired backup.** If the referenced SGBackup, its base backup, or the required
  WAL segments are gone (for example, removed by retention), the restore fails and the primary
  never reaches a ready state. Verify the SGBackup exists and is `Completed` first.
- **Storage credentials.** The new cluster needs access to the object storage that holds the
  backup. Ensure the relevant
  [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) and its credentials
  Secret exist in the target namespace.
- **Anti-affinity / scheduling.** As with any creation, a multi-instance restored cluster on a
  single node will leave replicas `Pending` under the `production` profile — see
  [A simple cluster]({{% relref "05-cookbook/01-creating-clusters/01-simple-cluster" %}}).
