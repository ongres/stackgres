---
title: Container Configuration
weight: 4
url: /administration/configuration/containers
description: How to configure sidecar containers and optimize resource usage.
showToc: true
---

StackGres clusters include several sidecar containers that provide additional functionality. You can disable these sidecars to optimize resource usage when their features are not needed.

## Default Container Architecture

A typical SGCluster pod includes:

| Container | Purpose | Default |
|-----------|---------|---------|
| `patroni` | PostgreSQL + Patroni HA | Always enabled |
| `pgbouncer` | Connection pooling | Enabled |
| `envoy` | Proxy with metrics | Disabled |
| `postgres-util` | Admin utilities (psql, etc.) | Enabled |
| `prometheus-postgres-exporter` | Metrics exporter | Enabled |

## Disabling Connection Pooling

PgBouncer provides connection pooling, reducing the overhead of PostgreSQL connections. Disable it if:

- Your application manages its own connection pool
- You need direct PostgreSQL connections for specific features
- You're running benchmarks without pooling

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    disableConnectionPooling: true
```

### Impact of Disabling

- **Resource savings**: ~50-100MB memory per pod
- **Lost functionality**: No connection pooling, higher connection overhead
- **Connection limits**: May hit PostgreSQL `max_connections` faster

### When to Disable

| Scenario | Recommendation |
|----------|----------------|
| Application has connection pool | Consider disabling |
| High-frequency short connections | Keep enabled |
| Long-lived connections | Consider disabling |
| Limited resources | Consider disabling |

## Disabling Metrics Exporter

The Prometheus exporter collects PostgreSQL metrics. Disable it if:

- You don't use Prometheus monitoring
- You have an external monitoring solution
- Resources are constrained

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  configurations:
    observability:
      disableMetrics: true
```

### Impact of Disabling

- **Resource savings**: ~50-100MB memory per pod
- **Lost functionality**: No Prometheus metrics from PostgreSQL
- **Monitoring**: Must use alternative monitoring methods

### Alternative: External Monitoring

If using external monitoring, you can still access PostgreSQL statistics:

```sql
-- Query pg_stat_* views directly
SELECT * FROM pg_stat_activity;
SELECT * FROM pg_stat_database;
```

## Disabling Postgres Utilities

The `postgres-util` container provides administration tools like `psql`, `pg_dump`, and other utilities. Disable if:

- You don't need CLI access to the database
- Resources are extremely constrained
- You use external tools exclusively

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    disablePostgresUtil: true
```

### Impact of Disabling

- **Resource savings**: ~100-200MB memory per pod
- **Lost functionality**: No `kubectl exec` access to psql and utilities
- **Administration**: Must connect from external clients

### Accessing PostgreSQL Without postgres-util

```bash
# Use a separate pod
kubectl run psql --rm -it --image=postgres:16 -- \
  psql -h my-cluster -U postgres

# Or port-forward and use local client
kubectl port-forward svc/my-cluster 5432:5432
psql -h localhost -U postgres
```

## Enabling Envoy Proxy

The Envoy sidecar provides protocol-level metrics and traffic management. Enable it for:

- Detailed connection metrics
- Protocol-level observability
- Traffic control capabilities

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    disableEnvoy: false  # Enable Envoy (disabled by default)
```

### Envoy Benefits

- **Protocol metrics**: Queries per second, latency histograms
- **Connection tracking**: Active connections, connection duration
- **Error tracking**: Protocol errors, connection failures

### Envoy Resource Usage

- **Memory**: ~100-200MB per pod
- **CPU**: Minimal overhead for typical workloads

### When to Enable Envoy

| Scenario | Recommendation |
|----------|----------------|
| Need detailed query metrics | Enable |
| Debugging connection issues | Enable |
| Resource-constrained environment | Keep disabled |
| Simple deployments | Keep disabled |

## Combined Configuration Examples

### Minimal Resource Configuration

For resource-constrained environments:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: minimal-cluster
spec:
  pods:
    disableConnectionPooling: true
    disablePostgresUtil: true
    disableEnvoy: true  # Already default
  configurations:
    observability:
      disableMetrics: true
```

**Savings**: ~300-400MB memory per pod

### Full Observability Configuration

For comprehensive monitoring:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: observable-cluster
spec:
  pods:
    disableConnectionPooling: false
    disablePostgresUtil: false
    disableEnvoy: false  # Enable Envoy
  configurations:
    observability:
      disableMetrics: false
      prometheusAutobind: true
```

### Production Recommended

Balanced configuration for production:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: production-cluster
spec:
  pods:
    disableConnectionPooling: false  # Keep connection pooling
    disablePostgresUtil: false       # Keep admin tools
    disableEnvoy: true               # Disable unless needed
  configurations:
    observability:
      disableMetrics: false          # Keep metrics
      prometheusAutobind: true
```

## Restart Requirements

Changing these settings requires a cluster restart:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: apply-container-changes
spec:
  sgCluster: my-cluster
  op: restart
  restart:
    method: ReducedImpact
    onlyPendingRestart: true
```

Check if restart is needed:

```bash
kubectl get sgcluster my-cluster -o jsonpath='{.status.conditions}' | \
  jq '.[] | select(.type=="PendingRestart")'
```

## Resource Planning

### Memory Estimates by Configuration

| Configuration | Estimated Memory per Pod |
|---------------|-------------------------|
| All enabled + Envoy | 800MB - 1.2GB |
| Default (no Envoy) | 600MB - 900MB |
| Minimal (all disabled) | 300MB - 500MB |

### Calculating Total Resources

```
Total Memory = (Base PostgreSQL + Enabled Sidecars) × Instances

Example:
- Base PostgreSQL: 400MB
- PgBouncer: 100MB
- Metrics Exporter: 100MB
- Postgres-util: 150MB
- 3 instances

Total = (400 + 100 + 100 + 150) × 3 = 2.25GB
```

## Related Documentation

- [Instance Profiles]({{% relref "04-administration-guide/04-configuration/01-instance-profile" %}})
- [Connection Pooling]({{% relref "04-administration-guide/04-configuration/03-connection-pooling" %}})
- [Monitoring]({{% relref "04-administration-guide/08-monitoring" %}})
