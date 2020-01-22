---
title: Postgres clusters
weight: 1
---

StackGres PostgreSQL cluster can be created using a cluster Custom Resource (CR) in Kubernetes.

___

**Kind:** StackGresCluster

**listKind:** StackGresClusterList

**plural:** sgclusters

**singular:** sgcluster
___

**Properties**

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| instances | ✓ | integer  | Number of instances to be created (for example 1) |   |
| pgVersion | ✓ | string  | PostgreSQL version for the new cluster (for example 11.6) |   |
| volumeSize | ✓ | string  | Storage volume size (for example 5Gi) |   |
| storageClass | ✓ | string  | Storage class name to be used for the cluster (if not specified means default storage class wiil be used) |   |
| [pgConfig](#postgresql-configuration) | ✓ | string  | PostgreSQL configuration to apply |   |
| [connectionPoolingConfig](#connection-pooling-configuration) | ✓ | string  | Pooling configuration to apply |   |
| [resourceProfile](#resource-profile-configuration) | ✓ | string  | Resource profile size to apply |   |
| [sidecars](#sidecar-containers) | ✓ | array  | List of sidecars to include in the cluster |   |
| prometheusAutobind |   | boolean | If enabled a ServiceMonitor will be created for each Prometheus instance found in order to collect metrics | false |
| [backupConfig](#backup-configuration) |   | string | Backup config to apply |   |
| [nonProduction](#non-production-options) |   | array  | Additional parameters for non production environments |   |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
spec:
  instances: 1
  pgVersion: '11.6'
  volumeSize: '5Gi'
  pgConfig: 'postgresconf'
  connectionPoolingConfig: 'pgbouncerconf'
  resourceProfile: 'size-xs'
  backupConfig: 'backupconf'
```

## Sidecar containers

A sidecar container is a container that adds functionality to PostgreSQL or to the cluster
 infrastructure. Currently StackGres implement following sidecar containers:

* `envoy`: this container is always present even if not specified in the configuration. It serve as
 a edge proxy from client to PostgreSQL instances or between PostgreSQL instances. It enables
 network metrics collection to provide connection statistics.
* `pgbouncer`: a container with pgbouncer as the connection pooling for the PostgreSQL instances.
* `prometheus-postgres-exporter`: a container with postgres exporter at the metrics exporter for
 the PostgreSQL instances.
* `postgres-util`: a container with psql and all PostgreSQL common tools in order to connect to the
 database directly as root to perform any administration tasks.

Following sinnept enables all sidecars but `postgres-util`:

```yaml
  sidecars:
    - envoy
    - pgbouncer
    - prometheus-postgres-exporter
```

## Non Production options

Following options should be enabled only when NOT working in a production environment.

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| disableClusterPodAntiAffinity |   | boolean | Disable the pod Anti-Affinity rule | false |
