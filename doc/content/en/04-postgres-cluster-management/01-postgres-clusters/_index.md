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

**Spec**

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| instances | ✓ | integer  | Number of instances to be created (for example 1) |   |
| pgVersion | ✓ | string  | PostgreSQL version for the new cluster (for example 11.6) |   |
| volumeSize | ✓ | string  | Storage volume size (for example 5Gi) |   |
| storageClass | ✓ | string  | Storage class name to be used for the cluster (if not specified means default storage class wiil be used) |   |
| [pgConfig]({{% relref "/04-postgres-cluster-management/02-configuration-tuning/02-postgres-configuration" %}}) | ✓ | string  | PostgreSQL configuration to apply |   |
| [connectionPoolingConfig]({{% relref "/04-postgres-cluster-management/02-configuration-tuning/03-connection-pooling-configuration" %}}) | ✓ | string  | Pooling configuration to apply |   |
| [resourceProfile]({{% relref "/04-postgres-cluster-management/03-resource-profiles" %}}) | ✓ | string  | Resource profile size to apply |   |
| [sidecars](#sidecar-containers) | ✓ | array  | List of sidecars to include in the cluster |   |
| prometheusAutobind |   | boolean | If enabled a ServiceMonitor will be created for each Prometheus instance found in order to collect metrics | false |
| [backupConfig]({{% relref "/04-postgres-cluster-management/04-backups/_index.md#configuration" %}}) |   | string | Backup config to apply |   |
| [restore](#restore-configuration) |   | object | Cluter restoration options |   |
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

## Restore configuration


By default, stackgres it's creates as an empty database. To create a cluster with data 
 from an existent backup, we have the restore options. It works, by simply indicating the 
 backup CR UUI that we want to restore. 

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| fromBackup | ✓ | string  | The backup CR UID to restore the cluster data |   |
| autoCopySecrets | | boolean | If you are creating a cluster in a different namespace than where backup CR is, you might need to copy the secrets where the credentials to access the backup storage to
 the namespace where you are installing the cluster. If is set to true stackgres will do it
 automatically.  | true |
| downloadDiskConcurrency | | integer | How many concurrent stream will be created while downloading the backup | 1 |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
spec:
  ...
  restore:
    fromBackup: d7e660a9-377c-11ea-b04b-0242ac110004
    autoCopySecrets: true
    downloadDiskConcurrency: 1
  ...
```

## Non Production options

Following options should be enabled only when NOT working in a production environment.

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| disableClusterPodAntiAffinity |   | boolean | Disable the pod Anti-Affinity rule | false |
