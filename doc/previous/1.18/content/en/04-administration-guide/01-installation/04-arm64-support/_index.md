---
title: ARM64 Architecture Support
weight: 4
url: /administration/installation/arm64
description: Running StackGres on ARM64 architecture (aarch64).
showToc: true
---

StackGres supports ARM64 (aarch64) architecture, enabling deployment on ARM-based infrastructure including AWS Graviton, Apple Silicon, and other ARM processors.

## Overview

StackGres provides multi-architecture container images that run on both:
- **amd64** (x86_64) - Intel/AMD processors
- **arm64** (aarch64) - ARM processors

The architecture is automatically detected and the appropriate image layers are used.

## Supported Components

### Operator Images

All StackGres operator components are available for ARM64:

| Component | ARM64 Support |
|-----------|--------------|
| stackgres-operator | Yes |
| stackgres-restapi | Yes |
| stackgres-jobs | Yes |
| stackgres-admin-ui | Yes |

### PostgreSQL Images

StackGres PostgreSQL images are built for both architectures:

| Image | ARM64 Support |
|-------|--------------|
| patroni | Yes |
| pgbouncer | Yes |
| envoy | Yes |
| postgres-util | Yes |
| prometheus-postgres-exporter | Yes |
| fluent-bit | Yes |

## Extension Availability

PostgreSQL extensions are built separately for each architecture. Extension availability may vary between amd64 and arm64.

### Checking Extension Architecture

The cluster status shows the detected architecture:

```bash
kubectl get sgcluster my-cluster -o jsonpath='{.status.arch}'
```

Output: `aarch64` or `x86_64`

### Extension Repository

Extensions are downloaded based on the detected architecture:
- **x86_64**: Standard extension packages
- **aarch64**: ARM64-specific packages

Some extensions may only be available for x86_64. Check the [extensions catalog](https://stackgres.io/extensions/) for availability.

### Common ARM64 Extensions

These popular extensions are available on ARM64:

| Extension | ARM64 | Notes |
|-----------|-------|-------|
| postgis | Yes | Full support |
| pgvector | Yes | Full support |
| pg_stat_statements | Yes | Built-in |
| pg_cron | Yes | Full support |
| timescaledb | Yes | Full support |
| pg_repack | Yes | Full support |

### Extensions with Limited ARM64 Support

Some extensions may have limited or no ARM64 support:

| Extension | ARM64 | Notes |
|-----------|-------|-------|
| citus | Limited | Check version |
| pgaudit | Yes | Recent versions |

> **Tip**: If an extension isn't available for ARM64, consider using a custom extension build or switching to x86_64 for that workload.

## Cloud Provider Support

### AWS (Graviton)

Deploy on AWS Graviton processors for cost-effective ARM64 instances:

```yaml
# Node affinity for Graviton instances
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: graviton-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '100Gi'
    scheduling:
      nodeAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          nodeSelectorTerms:
            - matchExpressions:
                - key: kubernetes.io/arch
                  operator: In
                  values:
                    - arm64
```

Or use node selector:

```yaml
pods:
  scheduling:
    nodeSelector:
      kubernetes.io/arch: arm64
```

### GCP (Tau T2A)

Deploy on Google Cloud's ARM-based instances:

```yaml
pods:
  scheduling:
    nodeSelector:
      cloud.google.com/machine-family: t2a
      kubernetes.io/arch: arm64
```

### Azure (Ampere)

Deploy on Azure's ARM-based VMs:

```yaml
pods:
  scheduling:
    nodeSelector:
      kubernetes.io/arch: arm64
```

## Installation on ARM64

### Helm Installation

The Helm chart automatically selects the correct image architecture:

```bash
helm install stackgres-operator \
  --namespace stackgres \
  --create-namespace \
  stackgres-charts/stackgres-operator
```

No additional configuration is needed - the operator detects the node architecture.

### Mixed Architecture Clusters

For Kubernetes clusters with both amd64 and arm64 nodes:

```bash
# Operator on specific architecture
helm install stackgres-operator \
  --namespace stackgres \
  --set operator.nodeSelector."kubernetes\.io/arch"=arm64 \
  stackgres-charts/stackgres-operator
```

### Database Clusters on ARM64

Target ARM64 nodes for database workloads:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: arm-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '50Gi'
    scheduling:
      nodeSelector:
        kubernetes.io/arch: arm64
      tolerations:
        - key: "arm64"
          operator: "Exists"
          effect: "NoSchedule"
```

## Performance Considerations

### Benefits of ARM64

- **Cost efficiency**: ARM instances typically cost 20-40% less
- **Power efficiency**: Lower power consumption per operation
- **Good single-thread performance**: Competitive with x86_64

### Workload Suitability

| Workload Type | ARM64 Suitability |
|---------------|-------------------|
| OLTP | Excellent |
| Read-heavy | Excellent |
| Write-heavy | Good |
| Analytics/OLAP | Good |
| Extension-heavy | Verify availability |

## Verification

### Check Operator Architecture

```bash
kubectl get pods -n stackgres -o wide
kubectl exec -n stackgres deploy/stackgres-operator -- uname -m
```

### Check Cluster Architecture

```bash
kubectl exec my-cluster-0 -c patroni -- uname -m
```

Expected output: `aarch64` for ARM64 or `x86_64` for AMD64.

## Best Practices

1. **Test extensions**: Verify all required extensions are available on ARM64 before migration

2. **Use node selectors**: Explicitly target ARM64 nodes for predictable behavior

3. **Monitor performance**: Compare metrics between architectures during migration

4. **Keep images updated**: ARM64 support improves with each release

5. **Plan for mixed clusters**: If some workloads need x86_64, design cluster topology accordingly

## Related Documentation

- [Installation Prerequisites]({{% relref "04-administration-guide/01-installation/01-pre-requisites" %}})
- [Pod Scheduling]({{% relref "04-administration-guide/04-configuration/06-pod-scheduling" %}})
- [PostgreSQL Extensions]({{% relref "04-administration-guide/07-postgres-extensions" %}})
