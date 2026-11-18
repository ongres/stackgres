---
title: Pod Scheduling
weight: 6
url: /administration/configuration/pod-scheduling
description: How to control pod placement with nodeSelector, affinity, tolerations, and topology spread.
showToc: true
---

StackGres provides comprehensive pod scheduling options to control where cluster pods run. This enables optimizing for performance, availability, compliance, and resource utilization.

## Overview

Pod scheduling in StackGres is configured through `spec.pods.scheduling`:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    scheduling:
      nodeSelector:
        node-type: database
      tolerations:
        - key: "dedicated"
          operator: "Equal"
          value: "postgresql"
          effect: "NoSchedule"
      nodeAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          nodeSelectorTerms:
            - matchExpressions:
                - key: topology.kubernetes.io/zone
                  operator: In
                  values:
                    - us-east-1a
                    - us-east-1b
```

> **Note**: Changing scheduling configuration may require a cluster restart.

## Node Selector

The simplest way to constrain pods to specific nodes using labels:

```yaml
spec:
  pods:
    scheduling:
      nodeSelector:
        node-type: database
        disk-type: ssd
```

### Common Use Cases

**Dedicated database nodes:**
```yaml
nodeSelector:
  workload: postgresql
```

**Specific hardware:**
```yaml
nodeSelector:
  cpu-type: amd-epyc
  memory-size: high
```

**Region/zone placement:**
```yaml
nodeSelector:
  topology.kubernetes.io/zone: us-east-1a
```

### Labeling Nodes

Label nodes to match your selectors:

```bash
# Add labels
kubectl label node node-1 node-type=database
kubectl label node node-2 node-type=database

# Verify
kubectl get nodes -l node-type=database
```

## Tolerations

Tolerations allow pods to be scheduled on nodes with matching taints:

```yaml
spec:
  pods:
    scheduling:
      tolerations:
        - key: "dedicated"
          operator: "Equal"
          value: "postgresql"
          effect: "NoSchedule"
```

### Toleration Fields

| Field | Description |
|-------|-------------|
| `key` | Taint key to match |
| `operator` | `Equal` or `Exists` |
| `value` | Taint value (for `Equal` operator) |
| `effect` | `NoSchedule`, `PreferNoSchedule`, or `NoExecute` |
| `tolerationSeconds` | Time to tolerate `NoExecute` taints |

### Examples

**Tolerate dedicated database nodes:**
```yaml
tolerations:
  - key: "dedicated"
    operator: "Equal"
    value: "postgresql"
    effect: "NoSchedule"
```

**Tolerate any taint with a key:**
```yaml
tolerations:
  - key: "database-only"
    operator: "Exists"
    effect: "NoSchedule"
```

**Tolerate node pressure temporarily:**
```yaml
tolerations:
  - key: "node.kubernetes.io/memory-pressure"
    operator: "Exists"
    effect: "NoSchedule"
```

### Tainting Nodes

Set up taints on dedicated nodes:

```bash
# Add taint
kubectl taint nodes node-1 dedicated=postgresql:NoSchedule
kubectl taint nodes node-2 dedicated=postgresql:NoSchedule

# Remove taint
kubectl taint nodes node-1 dedicated=postgresql:NoSchedule-
```

## Node Affinity

Node affinity provides more expressive node selection rules:

### Required Affinity

Pods must be scheduled on matching nodes:

```yaml
spec:
  pods:
    scheduling:
      nodeAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          nodeSelectorTerms:
            - matchExpressions:
                - key: node-type
                  operator: In
                  values:
                    - database
                    - database-high-memory
```

### Preferred Affinity

Pods prefer matching nodes but can run elsewhere:

```yaml
spec:
  pods:
    scheduling:
      nodeAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
                - key: disk-type
                  operator: In
                  values:
                    - nvme
          - weight: 50
            preference:
              matchExpressions:
                - key: disk-type
                  operator: In
                  values:
                    - ssd
```

### Operators

| Operator | Description |
|----------|-------------|
| `In` | Value in list |
| `NotIn` | Value not in list |
| `Exists` | Key exists |
| `DoesNotExist` | Key doesn't exist |
| `Gt` | Greater than (numeric) |
| `Lt` | Less than (numeric) |

### Multi-Zone Distribution

Spread pods across availability zones:

```yaml
nodeAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
    nodeSelectorTerms:
      - matchExpressions:
          - key: topology.kubernetes.io/zone
            operator: In
            values:
              - us-east-1a
              - us-east-1b
              - us-east-1c
```

## Pod Affinity

Control co-location with other pods:

### Pod Affinity (Co-location)

Schedule near specific pods:

```yaml
spec:
  pods:
    scheduling:
      podAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                app: my-application
            topologyKey: kubernetes.io/hostname
```

### Pod Anti-Affinity (Separation)

Avoid co-location with specific pods:

```yaml
spec:
  pods:
    scheduling:
      podAntiAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                app: StackGresCluster
                stackgres.io/cluster-name: my-cluster
            topologyKey: kubernetes.io/hostname
```

> **Note**: StackGres automatically configures pod anti-affinity in `production` profile to spread instances across nodes.

### Topology Keys

| Key | Scope |
|-----|-------|
| `kubernetes.io/hostname` | Single node |
| `topology.kubernetes.io/zone` | Availability zone |
| `topology.kubernetes.io/region` | Region |

## Topology Spread Constraints

Fine-grained control over pod distribution:

```yaml
spec:
  pods:
    scheduling:
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: StackGresCluster
              stackgres.io/cluster-name: my-cluster
```

### Configuration Options

| Field | Description |
|-------|-------------|
| `maxSkew` | Maximum difference in pod count between zones |
| `topologyKey` | Node label for topology domain |
| `whenUnsatisfiable` | `DoNotSchedule` or `ScheduleAnyway` |
| `labelSelector` | Pods to consider for spreading |

### Even Zone Distribution

```yaml
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels:
        stackgres.io/cluster-name: my-cluster
```

## Priority Class

Set pod priority for scheduling and preemption:

```yaml
spec:
  pods:
    scheduling:
      priorityClassName: high-priority-database
```

Create a PriorityClass:

```yaml
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: high-priority-database
value: 1000000
globalDefault: false
description: "Priority class for PostgreSQL databases"
```

## Backup Pod Scheduling

Configure separate scheduling for backup pods:

```yaml
spec:
  pods:
    scheduling:
      backup:
        nodeSelector:
          workload: backup
        tolerations:
          - key: "backup-only"
            operator: "Exists"
            effect: "NoSchedule"
```

This allows running backups on different nodes than the database.

## Complete Examples

### High Availability Production Setup

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: ha-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  profile: production
  pods:
    persistentVolume:
      size: '100Gi'
    scheduling:
      # Run only on dedicated database nodes
      nodeSelector:
        node-type: database
      # Tolerate dedicated node taints
      tolerations:
        - key: "dedicated"
          operator: "Equal"
          value: "postgresql"
          effect: "NoSchedule"
      # Prefer NVMe storage nodes
      nodeAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
                - key: storage-type
                  operator: In
                  values:
                    - nvme
      # Spread across availability zones
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              stackgres.io/cluster-name: ha-cluster
      # High priority
      priorityClassName: database-critical
```

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
  profile: development
  pods:
    persistentVolume:
      size: '10Gi'
    scheduling:
      # Prefer spot/preemptible nodes
      nodeAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
                - key: node-lifecycle
                  operator: In
                  values:
                    - spot
      tolerations:
        - key: "spot-instance"
          operator: "Exists"
          effect: "NoSchedule"
```

### Multi-Region Disaster Recovery

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: dr-cluster
spec:
  instances: 5
  postgres:
    version: '16'
  pods:
    scheduling:
      # Require specific regions
      nodeAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          nodeSelectorTerms:
            - matchExpressions:
                - key: topology.kubernetes.io/region
                  operator: In
                  values:
                    - us-east-1
                    - us-west-2
      # Spread across regions and zones
      topologySpreadConstraints:
        - maxSkew: 2
          topologyKey: topology.kubernetes.io/region
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              stackgres.io/cluster-name: dr-cluster
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              stackgres.io/cluster-name: dr-cluster
```

### Backup on Separate Infrastructure

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  pods:
    scheduling:
      # Database pods on high-performance nodes
      nodeSelector:
        workload: database
        performance: high
      # Backup pods on cost-optimized nodes
      backup:
        nodeSelector:
          workload: backup
          cost: optimized
        tolerations:
          - key: "backup-workload"
            operator: "Exists"
            effect: "NoSchedule"
```

## Related Documentation

- [Instance Profiles]({{% relref "04-administration-guide/04-configuration/01-instance-profile" %}})
- [SGCluster Scheduling Reference]({{% relref "06-crd-reference/01-sgcluster#sgclusterspecpodsscheduling" %}})
- [Cluster Profiles]({{% relref "04-administration-guide/04-configuration" %}})
