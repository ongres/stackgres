---
title: Sharded Cluster Backup and Restore
weight: 15
url: /administration/sharded-cluster/backup-restore
description: How to backup and restore sharded clusters using SGShardedBackup.
showToc: true
---

This guide covers backup and restore operations for SGShardedCluster using the SGShardedBackup resource.

## How Sharded Backups Work

SGShardedBackup coordinates backups across all components of a sharded cluster:

1. **Coordinator Backup**: Captures metadata, distributed tables configuration, and coordinator data
2. **Shard Backups**: Creates individual backups for each shard cluster
3. **Coordination**: Ensures consistent point-in-time recovery across all components

Each SGShardedBackup creates multiple underlying SGBackup resources (one per shard and coordinator).

## Prerequisites

Before creating backups, configure object storage in your sharded cluster:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: my-sharded-cluster
spec:
  configurations:
    backups:
      - sgObjectStorage: my-backup-storage
        cronSchedule: '0 5 * * *'
        retention: 7
        compression: lz4
```

## Creating Manual Backups

### Basic Backup

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedBackup
metadata:
  name: manual-backup
spec:
  sgShardedCluster: my-sharded-cluster
```

Apply:

```bash
kubectl apply -f sgshardedbackup.yaml
```

### Backup with Options

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedBackup
metadata:
  name: manual-backup-with-options
spec:
  sgShardedCluster: my-sharded-cluster
  managedLifecycle: false    # Don't auto-delete with retention policy
  timeout: 7200              # 2 hour timeout (in seconds)
  maxRetries: 3              # Retry up to 3 times on failure
```

## Automated Backups

Configure automated backups in the sharded cluster spec:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: my-sharded-cluster
spec:
  configurations:
    backups:
      - sgObjectStorage: s3-backup-storage
        cronSchedule: '0 */6 * * *'  # Every 6 hours
        retention: 14                 # Keep 14 backups
        compression: lz4
        performance:
          maxNetworkBandwidth: 100000000  # 100 MB/s
          maxDiskBandwidth: 100000000
          uploadDiskConcurrency: 2
```

### Backup Schedule Examples

| Schedule | Description |
|----------|-------------|
| `0 5 * * *` | Daily at 5 AM |
| `0 */6 * * *` | Every 6 hours |
| `0 0 * * 0` | Weekly on Sunday |
| `0 0 1 * *` | Monthly on the 1st |

## Monitoring Backup Status

### Check Backup Progress

```bash
# List sharded backups
kubectl get sgshardedbackup

# View detailed status
kubectl get sgshardedbackup manual-backup -o yaml
```

### Backup Status Fields

```yaml
status:
  process:
    status: Completed  # Running, Completed, Failed
    timing:
      start: "2024-01-15T05:00:00Z"
      end: "2024-01-15T05:45:00Z"
      stored: "2024-01-15T05:46:00Z"
  sgBackups:           # Individual backup references
    - my-sharded-cluster-coord-backup-xxxxx
    - my-sharded-cluster-shard0-backup-xxxxx
    - my-sharded-cluster-shard1-backup-xxxxx
  backupInformation:
    postgresVersion: "15.3"
    size:
      compressed: 1073741824    # 1 GB compressed
      uncompressed: 5368709120  # 5 GB uncompressed
```

### Check Individual Shard Backups

```bash
# List all related SGBackups
kubectl get sgbackup -l stackgres.io/shardedbackup-name=manual-backup
```

## Restoring from Backup

### Create New Cluster from Backup

To restore a sharded cluster from backup, create a new SGShardedCluster with restore configuration:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: restored-sharded-cluster
spec:
  type: citus
  database: sharded
  postgres:
    version: '15'
  coordinator:
    instances: 2
    pods:
      persistentVolume:
        size: 20Gi
  shards:
    clusters: 3
    instancesPerCluster: 2
    pods:
      persistentVolume:
        size: 50Gi
  initialData:
    restore:
      fromBackup:
        name: manual-backup
```

### Point-in-Time Recovery (PITR)

Restore to a specific point in time:

```yaml
spec:
  initialData:
    restore:
      fromBackup:
        name: manual-backup
        pointInTimeRecovery:
          restoreToTimestamp: "2024-01-15T10:30:00Z"
```

### Restore Options

```yaml
spec:
  initialData:
    restore:
      fromBackup:
        name: manual-backup
      downloadDiskConcurrency: 2  # Parallel download threads
```

## Backup Retention

### Managed Lifecycle

Backups with `managedLifecycle: true` are automatically deleted based on the retention policy:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedBackup
metadata:
  name: auto-managed-backup
spec:
  sgShardedCluster: my-sharded-cluster
  managedLifecycle: true  # Subject to retention policy
```

### Manual Backup Retention

Backups with `managedLifecycle: false` must be deleted manually:

```bash
kubectl delete sgshardedbackup manual-backup
```

## Backup Storage Configuration

### Using Different Storage Classes

```yaml
spec:
  configurations:
    backups:
      - sgObjectStorage: primary-storage
        cronSchedule: '0 5 * * *'
        retention: 7
      - sgObjectStorage: archive-storage  # Long-term storage
        cronSchedule: '0 0 1 * *'         # Monthly
        retention: 12
        path: /archive
```

### Backup Compression Options

| Option | Description | Use Case |
|--------|-------------|----------|
| `lz4` | Fast, moderate compression | Default, balanced |
| `lzma` | High compression, slower | Storage-constrained |
| `zstd` | Good compression, fast | Recommended |
| `brotli` | High compression | Long-term archives |

## Volume Snapshots

For faster backups using Kubernetes VolumeSnapshots:

```yaml
spec:
  configurations:
    backups:
      - sgObjectStorage: s3-storage
        cronSchedule: '0 5 * * *'
        useVolumeSnapshot: true
        volumeSnapshotClass: csi-snapclass
```

Requirements:
- CSI driver with snapshot support
- VolumeSnapshotClass configured
- Sufficient snapshot quota

## Backup Performance Tuning

### Network and Disk Limits

```yaml
spec:
  configurations:
    backups:
      - sgObjectStorage: s3-storage
        performance:
          maxNetworkBandwidth: 200000000  # 200 MB/s
          maxDiskBandwidth: 200000000
          uploadDiskConcurrency: 4
```

### Timeout Configuration

For large clusters, increase timeout:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedBackup
metadata:
  name: large-cluster-backup
spec:
  sgShardedCluster: my-large-sharded-cluster
  timeout: 21600  # 6 hours (in seconds)
```

## Cross-Region Backup

Configure backup replication to another region:

1. Create SGObjectStorage in the target region
2. Configure multiple backup destinations:

```yaml
spec:
  configurations:
    backups:
      - sgObjectStorage: primary-region-storage
        cronSchedule: '0 5 * * *'
        retention: 7
      - sgObjectStorage: dr-region-storage
        cronSchedule: '0 6 * * *'  # Offset by 1 hour
        retention: 7
        path: /disaster-recovery
```

## Best Practices

1. **Test restores regularly**: Periodically restore to verify backups work
2. **Use managed lifecycle**: Let retention policies manage backup cleanup
3. **Multiple storage locations**: Configure backups to different regions
4. **Monitor backup size**: Track backup growth over time
5. **Secure storage credentials**: Use proper secret management
6. **Document recovery procedures**: Maintain runbooks for restore operations

## Related Documentation

- [SGShardedBackup CRD Reference]({{% relref "06-crd-reference/13-sgshardedbackup" %}})
- [Backup Encryption]({{% relref "04-administration-guide/05-backups/05-encryption" %}})
- [Object Storage Configuration]({{% relref "06-crd-reference/09-sgobjectstorage" %}})
