---
title: Backup Large databases
weight: 10
url: /runbooks/backup-large-databases
description: Backing up large database in seconds using snapshots
---

As enterprises grow their data grows with them, leading to a huge increase in data ingestion, resulting in very large databases. Although databases can store that much data, certain operations become impacted whenever companies begin to reach databases with large amounts of data (in terabytes).
Because of those impacts, leading software has to give companies the proper tools to diminish the difficulty in those operations. One of the most basic actions a company must consider with databases is Backup, and backups are usually the most impacted operation whenever your database starts to increase in size.
This runbook is going to show how easily you can backup large databases using StackGres.

## Backups

The first problem you will encounter when creating backups of large databases is time. The operations are going to start taking a long time to finish. One would change their strategy to an incremental backup solution, this way they only have one backup that takes a long to finish (the full backup) and all subsequent backups (incremental) are faster. This still doesn't save the company from doing those monthly (or weekly, biweekly, etc) full backups.

## Snapshots

So here come snapshots, ultimately they are photos of the current state of your file system, saving all file change operations. Because they don't have a full copy of your files they don't take long to execute, hence the name "snapshots", they are extremely quick.

## StackGres

In StackGres, we provide the user with automated backups for PostgreSQL, those backups can be created using snapshots or using Wal-G. The latter option works very well but is constricted to the issues mentioned above when used against very large databases. A way to circumvent that is by using snapshots. Snapshots are provided in StackGres by using Kubernetes VolumeSnapshots called through the CSI driver of your storage of choice.
In this example we have are using ZFS as our filesystem and OpenEBS with a ZFS CSI driver and MinIO to schedule backups using snapshots and doing PTIR (point-in-time-recovery).

## Requirements

- MinIO (or another Object Storage)
- OpenEBS (for local machines)
- ZFS
- StackGres
- Helm
- Kubernetes

Below are the steps taken to setup the environment:

## Configuration

### ZFS

Install ZFS.
```bash
apt install zfsutils-linux
```

Set up the ZFS pool
```bash
zpool create data-pool /dev/sda
```

14TB pool created
```bash
zfs list
```

### MinIO and StackGres Helm charts

Add and update the repositories
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

Install MinIO and StackGres
```yaml
resources:
  requests:
    memory: 128Mi
Persistence:
  storageClass: "openebs-zfspv"
  enabled: true
  size: 128Mi
  existingClaim: ""
provisioning:
  enabled: true
  buckets:
  - name: test
    policy: none
    purge: true
readinessProbe:
  initialDelaySeconds: 5
```
```bash
helm install -n minio --create-namespace minio bitnami/minio --version 12.8.7 -f /opt/ongres/minio/minio.yaml
```

```bash
helm install --create-namespace --namespace stackgres stackgres-operator \
--set-string adminui.service.type=ClusterIP \
stackgres-charts/stackgres-operator --version 1.8.1
```

### StorageClass
Create the StorageClass
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: openebs-zfspv
parameters:
  fstype: "zfs"
  poolname: "data-pool"
provisioner: zfs.csi.openebs.io
volumeBindingMode: WaitForFirstConsumer
```

```bash
kubectl apply -f storageclass.yaml
```

### Object Storage
Create the ObjectStorage
```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: backup-stackgres
spec:
  type: s3Compatible
  s3Compatible:
    bucket: test
    enablePathStyleAddressing: true
    endpoint: http://minio.minio:9000
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          key: root-user
          name: minio
        secretAccessKey:
          key: root-password
          name: minio
```

```bash
kubectl apply -f sgobjectstorage.yaml
```

### VolumeSnapshotClass
Create the VolumeSnapshotClass
```yaml
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshotClass
metadata:
  name: my-snapshotclass
  annotations:
    snapshot.storage.k8s.io/is-default-class: "true"
driver: zfs.csi.openebs.io
deletionPolicy: Delete
```

```bash
kubectl apply -f volumesnapshotclass.yaml
```

### StackGres Cluster
Create the SGCluster
```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-db
spec:
  postgres:
    version: '16'
  instances: 1
  pods:
    persistentVolume:
      size: '10Ti'
      storageClass: openebs-zfspv
  configurations:
    backups:
    - sgObjectStorage: backup-stackgres
      retention: 10
      cronSchedule: "*/15 * * * *"
      compression: lz4
      useVolumeSnapshot: true
      volumeSnapshotClass: my-snapshotclass
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
```

```bash
kubectl apply -f sgcluster.yaml
```

Check that the PVC has allocated space in the ZFS pool
```bash
zfs list
NAME                                                 USED  AVAIL     REFER  MOUNTPOINT
data-pool                                            489M  14.4T       96K  /data-pool
data-pool/pvc-f9aeccd2-e895-426c-a4a7-73a3200e8938   488M  10.0T      472M  legacy
```

As shown above, a 10 TiB persistent volume.

### StackGres Backup
Run a StackGres backup
```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  name: backup-my-db
spec:
  sgCluster: my-db
  managedLifecycle: true
```

```bash
kubectl apply -f sgbackup.yaml
```

## Backup information

```bash
kubectl describe sgbackup backup-my-db

Name:         backup-my-db
Namespace:    default
Labels:       <none>
Annotations:  stackgres.io/operatorVersion: 1.8.1
API Version:  stackgres.io/v1
Kind:         SGBackup
Metadata:
  Creation Timestamp:  2024-03-13T10:55:32Z
...
Size:
      Compressed:       10995116277760
      Uncompressed:     10995116277760
...
Status:             Completed
    Timing:
      End:     2024-03-13T10:55:41Z
      Start:   2024-03-13T10:55:34Z
      Stored:  2024-03-13T10:55:42.133Z
...
Volume Snapshot:
    Name:            backup-my-db
```

Duration: 7 seconds

## Snapshot information

```bash
kubectl describe sgbackup backup-my-db

Name:         backup-my-db
Namespace:    default
Labels:       <none>
Annotations:  <none>
API Version:  snapshot.storage.k8s.io/v1
Kind:         VolumeSnapshot
...
Status:
  Bound Volume Snapshot Content Name:  snapcontent-bf6e234a-a6ef-416c-ac89-ebdfd168b889
  Creation Time:                       2024-03-13T10:55:37Z
  Ready To Use:                        true
  Restore Size:                        10Ti
```

## Restore

```bash
kubectl get pod my-db-restore-0 -o yaml

lastTransitionTime: "2024-03-13T13:54:30Z"
    status: "True"
    type: PodScheduled

...

lastTransitionTime: "2024-03-13T13:54:45Z"
    status: "True"
    type: Ready
```

Restore time: 15 seconds
PITR: T-5 minutes
