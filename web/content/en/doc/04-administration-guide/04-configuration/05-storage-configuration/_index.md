---
title: Storage Configuration
weight: 5
url: /doc/latest/administration/configuration/storage
description: How to configure persistent storage for StackGres clusters.
showToc: true
---

This guide covers storage configuration options for StackGres clusters, including volume sizing, storage classes, and advanced security settings.

## Persistent Volume Configuration

Every SGCluster requires persistent storage for PostgreSQL data. Configure storage in the `spec.pods.persistentVolume` section:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    persistentVolume:
      size: '50Gi'
      storageClass: 'fast-ssd'
```

### Required Settings

| Setting | Description |
|---------|-------------|
| `size` | Volume size (e.g., `10Gi`, `100Gi`, `1Ti`) |

### Optional Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `storageClass` | Kubernetes StorageClass name | Cluster default |
| `fsGroupChangePolicy` | Volume permission policy | `OnRootMismatch` |
| `volumeAttributesClassName` | Kubernetes VolumeAttributesClass applied to the PVCs | None |
| `ioLimits` | Per-volume cgroup v2 `io.max` I/O throttling | None |

## Storage Size

Specify volume size using Kubernetes quantity format:

```yaml
spec:
  pods:
    persistentVolume:
      size: '100Gi'  # 100 Gibibytes
```

Supported units:
- `Mi` - Mebibytes (1024 KiB)
- `Gi` - Gibibytes (1024 MiB)
- `Ti` - Tebibytes (1024 GiB)

### Sizing Guidelines

| Workload | Recommended Size | Notes |
|----------|-----------------|-------|
| Development | 10-50Gi | Minimal testing |
| Small production | 50-200Gi | Light workloads |
| Medium production | 200Gi-1Ti | Standard workloads |
| Large production | 1Ti+ | Heavy workloads, analytics |

Consider:
- Current data size plus growth projections
- WAL files (typically 10-20% of total)
- Temporary files for operations
- Backup staging area

## Storage Class

The storage class determines the underlying storage technology:

```yaml
spec:
  pods:
    persistentVolume:
      size: '100Gi'
      storageClass: 'premium-ssd'
```

### Common Storage Classes

**Cloud Providers:**

```yaml
# AWS EBS (gp3)
storageClass: 'gp3'

# GCP Persistent Disk (SSD)
storageClass: 'premium-rwo'

# Azure Managed Disk (Premium SSD)
storageClass: 'managed-premium'
```

**On-premises:**

```yaml
# Local NVMe storage
storageClass: 'local-nvme'

# Ceph RBD
storageClass: 'rook-ceph-block'

# OpenEBS
storageClass: 'openebs-cstor-sparse'
```

### Storage Class Requirements

For PostgreSQL workloads, storage classes should support:
- `ReadWriteOnce` access mode
- Volume expansion (for online resizing)
- Snapshot capability (for backups)
- High IOPS for transaction logs

## fsGroupChangePolicy

The `fsGroupChangePolicy` setting controls how Kubernetes handles file ownership when mounting volumes. This affects pod startup time and security.

```yaml
spec:
  pods:
    persistentVolume:
      size: '100Gi'
      fsGroupChangePolicy: 'OnRootMismatch'
```

### Available Policies

| Policy | Description | Use Case |
|--------|-------------|----------|
| `OnRootMismatch` | Only change ownership if root directory permissions don't match | **Recommended** - Faster startup, minimal overhead |
| `Always` | Always recursively change ownership on mount | Strict security, slower startup |

### OnRootMismatch (Recommended)

The default and recommended setting. Kubernetes only changes file ownership if the root directory of the volume has incorrect permissions:

```yaml
fsGroupChangePolicy: 'OnRootMismatch'
```

Benefits:
- Fast pod startup (no recursive permission scan)
- Reduced I/O during mounting
- Suitable for most production workloads

### Always

Forces Kubernetes to recursively change ownership of all files every time the volume is mounted:

```yaml
fsGroupChangePolicy: 'Always'
```

Use when:
- Strict security compliance is required
- Volume contents may have mixed ownership
- After restoring data from external sources

> **Warning**: With large data volumes, `Always` can significantly increase pod startup time.

### Performance Impact

| Volume Size | `OnRootMismatch` Startup | `Always` Startup |
|-------------|-------------------------|------------------|
| 10Gi | ~1 second | 1-5 seconds |
| 100Gi | ~1 second | 10-60 seconds |
| 1Ti | ~1 second | 1-10 minutes |

The difference becomes significant with large volumes or many small files.

## Volume Attributes Class

The `volumeAttributesClassName` setting references a Kubernetes [VolumeAttributesClass](https://kubernetes.io/docs/concepts/storage/volume-attributes-classes/) that is applied to the PVCs created for the cluster. This lets you tune mutable volume attributes (for example IOPS or throughput on supporting CSI drivers) without recreating the volume:

```yaml
spec:
  pods:
    persistentVolume:
      size: '100Gi'
      volumeAttributesClassName: 'high-iops'
```

> **Note**: This requires the Kubernetes VolumeAttributesClass feature to be available in the cluster and a CSI driver that supports the referenced attributes.

On an SGShardedCluster, configure it per cluster type:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: sharded-cluster
spec:
  coordinator:
    pods:
      persistentVolume:
        size: '50Gi'
        volumeAttributesClassName: 'high-iops'
  workers:
    pods:
      persistentVolume:
        size: '100Gi'
        volumeAttributesClassName: 'high-iops'
```

## I/O Limits

The `ioLimits` object sets per-volume cgroup v2 `io.max` I/O throttling on the data persistent volume. Each field is an optional integer; leave a field empty to remove that particular limit.

> **Warning**: This is an **alpha** feature.

> **Warning**: Enabling I/O limits requires **root permissions** on the node. When any `ioLimits` value is set, StackGres adds an init container named `setup-io-limits` to each Pod that runs **as root** (with only the `CHOWN` capability), and both that init container and the `cluster-controller` container mount the host path `/sys/fs/cgroup` in order to write the `io.max` file. The nodes must use cgroup v2. On clusters that forbid root containers (for example a restrictive OpenShift SCC) this feature cannot be used.

```yaml
spec:
  pods:
    persistentVolume:
      size: '100Gi'
      ioLimits:
        readIops: 10000
        writeIops: 8000
        readMiBps: 500
        writeMiBps: 400
```

### I/O Limit Fields

| Field | Description |
|-------|-------------|
| `readIops` | Maximum read operations per second |
| `writeIops` | Maximum write operations per second |
| `readMiBps` | Maximum read throughput in MiB per second |
| `writeMiBps` | Maximum write throughput in MiB per second |

The same `ioLimits` object is available on `SGShardedCluster.spec.coordinator.pods.persistentVolume` and `SGShardedCluster.spec.workers.pods.persistentVolume`.

## Volume Expansion

If your storage class supports expansion, you can increase volume size:

### Step 1: Update Cluster Spec

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    persistentVolume:
      size: '200Gi'  # Increased from 100Gi
```

### Step 2: Apply and Wait

```bash
kubectl apply -f cluster.yaml

# Monitor PVC status
kubectl get pvc -l stackgres.io/cluster-name=my-cluster -w
```

> **Note**: Volume expansion may require a pod restart depending on the storage provider.

## Storage for Different Components

### Data Volume

The primary data volume for PostgreSQL:

```yaml
spec:
  pods:
    persistentVolume:
      size: '100Gi'
```

### Distributed Logs Storage

Separate storage for distributed logs:

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  name: logs-cluster
spec:
  persistentVolume:
    size: '50Gi'
    storageClass: 'standard'
```

### Sharded Cluster Storage

Configure storage per cluster type (coordinator and worker):

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: sharded-cluster
spec:
  coordinator:
    pods:
      persistentVolume:
        size: '50Gi'
  workers:
    pods:
      persistentVolume:
        size: '100Gi'  # Each worker gets this size
```

## Example Configurations

### Development Environment

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: dev-cluster
spec:
  instances: 1
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '10Gi'
```

### Production Environment

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: prod-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '500Gi'
      storageClass: 'premium-ssd'
      fsGroupChangePolicy: 'OnRootMismatch'
```

### High-Security Environment

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: secure-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '200Gi'
      storageClass: 'encrypted-ssd'
      fsGroupChangePolicy: 'Always'  # Strict ownership enforcement
```

## Related Documentation

- [Instance Profiles]({{% relref "04-administration-guide/04-configuration/01-instance-profile" %}})
- [Volume Downsize Runbook]({{% relref "09-runbooks/02-volume-downsize" %}})
- [Backup Configuration]({{% relref "04-administration-guide/05-backups" %}})
