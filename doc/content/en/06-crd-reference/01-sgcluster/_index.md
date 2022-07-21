---
title: SGCluster
weight: 1
url: reference/crd/sgcluster
description: Details about SGCluster configurations
showToc: true
---

StackGres PostgreSQL cluster can be created using a cluster Custom Resource (CR) in Kubernetes.

___

**Kind:** SGCluster

**listKind:** SGClusterList

**plural:** sgclusters

**singular:** sgcluster
___

**Spec**

| Property                                                                                   | Required | Updatable | Type     | Default                             | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:---------|:------------------------------------|:-------------------------------------------------------------------|
| instances                                                                                  | ✓        | ✓         | integer  |                                     | {{< crd-field-description SGCluster.spec.instances >}}             |
| [postgres](#postgres)                                                                      |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.postgres >}}              |
| [replication](#replication)                                                                |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.replication >}}           |
| [sgInstanceProfile]({{% relref "/06-crd-reference/02-sginstanceprofile" %}})               |          | ✓         | string   | will be generated                   | {{< crd-field-description SGCluster.spec.sgInstanceProfile >}}     |
| [metadata](#metadata)                                                                      |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.metadata >}}              |
| [postgresServices](#postgres-services)                                                     |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.postgresServices >}}      |
| [pods](#pods)                                                                              | ✓        | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.pods >}}                  |
| [configurations](#configurations)                                                          |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.configurations >}}        |
| prometheusAutobind                                                                         |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.prometheusAutobind >}}    |
| [initialData](#initial-data-configuration)                                                 |          |           | object   |                                     | {{< crd-field-description SGCluster.spec.initialData >}}           |
| [managedSql](#managed-sql)                                                                 |          |           | object   |                                     | {{< crd-field-description SGCluster.spec.managedSql >}}            |
| [distributedLogs](#distributed-logs)                                                       |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.distributedLogs >}}       |
| [nonProductionOptions](#non-production-options)                                            |          | ✓         | array    |                                     | {{< crd-field-description SGCluster.spec.nonProductionOptions >}}  |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  instances: 1
  postgres:
    version: 'latest'
  pods:
    persistentVolume:
      size: '5Gi'
  sgInstanceProfile: 'size-xs'
```

## Postgres

| Property                            | Required | Updatable | Type     | Default  | Description |
|:------------------------------------|----------|-----------|:---------|:---------|:------------|
| version                             | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.version >}}       |
| flavor                              |          |           | string   |          | {{< crd-field-description SGCluster.spec.postgres.flavor >}}       |
| [extensions](#postgres-extensions)  |          | ✓         | array    |          | {{< crd-field-description SGCluster.spec.postgres.extensions >}}    |
| [ssl](#postgres-ssl)                |          | ✓         | object   |          | {{< crd-field-description SGCluster.spec.postgres.ssl >}} |

## Postgres extensions

Extensions to be installed in the cluster.

| Property         | Required | Updatable | Type     | Default           | Description |
|:-----------------|----------|-----------|:---------|:------------------|:------------|
| name             | ✓        | ✓         | string   |                   | {{< crd-field-description SGCluster.spec.postgres.extensions.items.name >}} |
| version          |          | ✓         | string   | stable            | {{< crd-field-description SGCluster.spec.postgres.extensions.items.version >}} |
| publisher        |          | ✓         | string   | com.ongres        | {{< crd-field-description SGCluster.spec.postgres.extensions.items.publisher >}} |
| repository       |          | ✓         | string   |                   | {{< crd-field-description SGCluster.spec.postgres.extensions.items.repository >}} |

Example:

``` yaml

apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  postgres:
    extensions:
      - {name: 'timescaledb', version: '2.3.1'}
```

### Postgres SSL

By default, support for SSL connections to Postgres is disabled, to enable it configure this section. SSL connections will
 be handled by Envoy using [Postgres filter's SSL termination](https://github.com/envoyproxy/envoy/issues/10942).

| Property                   | Required | Updatable | Type     | Default  | Description |
|:---------------------------|----------|-----------|:---------|:---------|:------------|
| enabled                    |          |           | string   | false    | {{< crd-field-description SGCluster.spec.postgres.ssl.enabled >}} |
| [certificateSecretKeySelector](#ssl-certificate-secret) |          |           | object   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.certificateSecretKeySelector >}} |
| [privateKeySecretKeySelector](#ssl-private-key-secret)  |          |           | object   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.privateKeySecretKeySelector >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  postgres:
    ssl:
      enabled: true
      certificateSecretKeySelector:
        name: stackgres-secrets
        key: cert
      secretKeyRef:
        name: stackgres-secrets
        key: key
```

#### SSL Certificate Secret

| Property  | Required | Updatable | Type     | Default  | Description |
|:----------|----------|-----------|:---------|:---------|:------------|
| name      | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.certificateSecretKeySelector.name >}} |
| key       | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.certificateSecretKeySelector.key >}} |

#### SSL Private Key Secret

| Property  | Required | Updatable | Type     | Default  | Description |
|:----------|----------|-----------|:---------|:---------|:------------|
| name      | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.privateKeySecretKeySelector.name >}} |
| key       | ✓        | ✓         | string   |          | {{< crd-field-description SGCluster.spec.postgres.ssl.privateKeySecretKeySelector.key >}} |

## Replication

| Property                            | Required | Updatable | Type     | Default  | Description |
|:------------------------------------|----------|-----------|:---------|:---------|:------------|
| mode                                |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replication.mode >}}       |
| role                                |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replication.role >}}       |
| syncInstances                       |          | ✓         | integer  |          | {{< crd-field-description SGCluster.spec.replication.syncInstances >}} |
| [groups](#replication-group)        |          | ✓         | array    |          | {{< crd-field-description SGCluster.spec.replication.groups >}}     |

## Replication group

| Property                            | Required | Updatable | Type     | Default  | Description |
|:------------------------------------|----------|-----------|:---------|:---------|:------------|
| name                                |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replication.groups.items.name >}}       |
| role                                |          | ✓         | string   |          | {{< crd-field-description SGCluster.spec.replication.groups.items.role >}}       |
| instances                           |          | ✓         | integer  |          | {{< crd-field-description SGCluster.spec.replication.groups.items.instances >}}  |

### Metadata

Holds custom metadata information for StackGres generated resources to have.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| [annotations](#annotations)   |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations >}} |
| [labels](#labels)             |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.labels >}} |

### Annotations

Holds custom annotations for StackGres generated resources to have.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| allResources                  |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.allResources >}} |
| clusterPods                   |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.clusterPods >}} |
| services                      |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.services >}} |
| primaryService               |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.primaryService >}} |
| replicasService              |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.annotations.replicasService >}} |

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    annotations:
      clusterPods:
        customAnnotations: customAnnotationValue
      primaryService:
        customAnnotations: customAnnotationValue
      replicasService:
        customAnnotations: customAnnotationValue
```

### Labels

Holds custom labels for StackGres generated resources to have.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| clusterPods                   |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.metadata.labels.clusterPods >}} |

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    labels:
      clusterPods:
        customLabel: customLabelValue
```

## Postgres Services

Specifies the service configuration for the cluster:

| Property                            | Required | Updatable | Type     | Default                              | Description                                                            |
|:------------------------------------|----------|-----------|:---------|:-------------------------------------|:-----------------------------------------------------------------------|
| [Primary](#primary-service-type)    |          | ✓         | object   | [primary](#primary-service-type)   | {{< crd-field-description SGCluster.spec.postgresServices.primary >}}  |
| [Replicas](#replicas-service-type)  |          | ✓         | object   | [replicas](#replicas-service-type) | {{< crd-field-description SGCluster.spec.postgresServices.replicas >}} |

### Primary service type

| Property                        | Required | Updatable | Type     | Default   | Description                                                                 |
|:--------------------------------|----------|-----------|:---------|:----------|:----------------------------------------------------------------------------|
| enabled                         |          | ✓         | boolean  | true      | {{< crd-field-description SGCluster.spec.postgresServices.primary.enabled >}}  |
| type                            |          | ✓         | string   | ClusterIP | {{< crd-field-description SGCluster.spec.postgresServices.primary.type >}}  |
| externalIPs                     |          | ✓         | array    |           | {{< crd-field-description SGCluster.spec.postgresServices.primary.externalIPs >}}  |
| loadBalancerIP                     |          | ✓         | string    |           | {{< crd-field-description SGCluster.spec.postgresServices.primary.loadBalancerIP >}}  |

### Replicas service type

| Property                        | Required | Updatable | Type     | Default   | Description                                                                 |
|:--------------------------------|----------|-----------|:---------|:----------|:----------------------------------------------------------------------------|
| enabled                         |          | ✓         | boolean  | true      | {{< crd-field-description SGCluster.spec.postgresServices.replicas.enabled >}}  |
| type                            |          | ✓         | string   | ClusterIP | {{< crd-field-description SGCluster.spec.postgresServices.replicas.type >}}  |
| externalIPs                     |          | ✓         | array    |           | {{< crd-field-description SGCluster.spec.postgresServices.replicas.externalIPs >}}  |
| loadBalancerIP                     |          | ✓         | string    |           | {{< crd-field-description SGCluster.spec.postgresServices.replicas.loadBalancerIP >}}  |

## Pods

Cluster's pod configuration

| Property                               | Required | Updatable | Type     | Default                             | Description |
|:---------------------------------------|----------|-----------|:---------|:------------------------------------|:------------|
| [persistentVolume](#persistent-volume) | ✓        | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.pods.persistentVolume >}} |
| disableConnectionPooling               |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disableConnectionPooling >}} |
| disableMetricsExporter                 |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disableMetricsExporter >}} |
| disablePostgresUtil                    |          | ✓         | boolean  | false                               | {{< crd-field-description SGCluster.spec.pods.disablePostgresUtil >}} |
| [scheduling](#scheduling)              |          | ✓         | object   |                                     | {{< crd-field-description SGCluster.spec.pods.scheduling >}} |
| managementPolicy                       |          | ✓         | string   | OrderedReady                        | {{< crd-field-description SGCluster.spec.pods.managementPolicy >}} |

### Sidecar containers

A sidecar container is a container that adds functionality to PostgreSQL or to the cluster
 infrastructure. Currently StackGres implement following sidecar containers:

* `envoy`: this container is always present, and is not possible to disable it. It serve as
 a edge proxy from client to PostgreSQL instances or between PostgreSQL instances. It enables
 network metrics collection to provide connection statistics.
* `pgbouncer`: a container with pgbouncer as the connection pooling for the PostgreSQL instances.
* `prometheus-postgres-exporter`: a container with postgres exporter that exports metrics for
 the PostgreSQL instances.
* `fluent-bit`: a container with fluent-bit that send logs to a distributed logs cluster.
* `postgres-util`: a container with psql and all PostgreSQL common tools in order to connect to the
 database directly as root to perform any administration tasks.

The following example, disable all optional sidecars:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    disableConnectionPooling: false
    disableMetricsExporter: false
    disablePostgresUtil: false
```

### Persistent Volume

Holds the configurations of the persistent volume that the cluster pods are going to use.

| Property     | Required | Updatable | Type     | Default                             | Description |
|:-------------|----------|-----------|:---------|:------------------------------------|:------------|
| size         | ✓        | ✓         | string   |                                     | {{< crd-field-description SGCluster.spec.pods.persistentVolume.size >}} |
| storageClass |          | ✓         | string   | default storage class               | {{< crd-field-description SGCluster.spec.pods.persistentVolume.storageClass >}} |

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    persistentVolume:
      size: '5Gi'
      storageClass: default
```

### Scheduling

Holds scheduling configuration for StackGres pods to have.

| Property                    | Required | Updatable | Type     | Default        | Description |
|:----------------------------|----------|-----------|:---------|:---------------|:------------|
| nodeSelector                |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.pods.scheduling.nodeSelector >}} |
| [nodeAffinity](#node-affinity) |          | ✓         | object    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.nodeAffinity >}} |
| [tolerations](#tolerations) |          | ✓         | array    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.tolerations >}} |
| [backup](#backup)           |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup >}} |

#### Node Affinity

Sets the pod's affinity to restrict it to run only on a certain set of node(s)

| Property          | Required | Updatable | Type     | Default                 | Description |
|:------------------|----------|-----------|:---------|:------------------------|:------------|
| requiredDuringSchedulingIgnoredDuringExecution  |          | ✓         | object   |                         | {{< crd-field-description SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution >}} |
| preferredDuringSchedulingIgnoredDuringExecution |          | ✓         | array   |                         | {{< crd-field-description SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution >}} |

See Kubernetes pod node affinity [definition](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#node-affinity{}) for more details.
<br>
<br>

#### Tolerations

Holds scheduling configuration for StackGres pods to have.

| Property          | Required | Updatable | Type     | Default                 | Description |
|:------------------|----------|-----------|:---------|:------------------------|:------------|
| key               |          | ✓         | string   |                         | {{< crd-field-description SGCluster.spec.pods.scheduling.tolerations.items.key >}} |
| operator          |          | ✓         | string   | Equal                   | {{< crd-field-description SGCluster.spec.pods.scheduling.tolerations.items.operator >}} |
| value             |          | ✓         | string   |                         | {{< crd-field-description SGCluster.spec.pods.scheduling.tolerations.items.value >}} |
| effect            |          | ✓         | string   | match all taint effects | {{< crd-field-description SGCluster.spec.pods.scheduling.tolerations.items.effect >}} |
| tolerationSeconds |          | ✓         | string   | 0                       | {{< crd-field-description SGCluster.spec.pods.scheduling.tolerations.items.tolerationSeconds >}} |

#### Backup

Holds scheduling configuration for StackGres Backups pods to have.

| Property                    | Required | Updatable | Type     | Default        | Description |
|:----------------------------|----------|-----------|:---------|:---------------|:------------|
| nodeSelector                |          | ✓         | object   |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup.nodeSelector >}} |
| [nodeAffinity](#node-affinity) |          | ✓         | object    |                | {{< crd-field-description SGCluster.spec.pods.scheduling.backup.nodeAffinity >}} |

## Configurations

Custom configurations to be applied to the cluster.

| Property                                                                  | Required | Updatable | Type     | Default           | Description |
|:--------------------------------------------------------------------------|----------|-----------|:---------|:------------------|:------------|
| [sgPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) |          | ✓         | string   | will be generated | {{< crd-field-description SGCluster.spec.configurations.sgPostgresConfig >}} |
| [sgPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}})   |          | ✓         | string   | will be generated | {{< crd-field-description SGCluster.spec.configurations.sgPoolingConfig >}}  |
| [sgBackupConfig]({{% relref "06-crd-reference/05-sgbackupconfig" %}})     |          | ✓         | string   |                   | {{< crd-field-description SGCluster.spec.configurations.sgBackupConfig >}}   |
Example:

``` yaml

apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  configurations:
    sgPostgresConfig: 'postgresconf'
    sgPoolingConfig: 'pgbouncerconf'
    sgBackupConfig: 'backupconf'

```

## Initial Data Configuration

Specifies the cluster initialization data configurations

| Property                          | Required | Updatable | Type     | Default | Description |
|:----------------------------------|----------|-----------|:---------|:--------|:------------|
| [restore](#restore-configuration) |          |           | object   |         | {{< crd-field-description SGCluster.spec.initialData.restore >}} |
| [scripts](#scripts-configuration) |          |           | object   |         | {{< crd-field-description SGCluster.spec.initialData.scripts >}} |

## Restore configuration

By default, stackgres it's creates as an empty database. To create a cluster with data
 from an existent backup, we have the restore options. It works, by simply indicating the
 backup CR UUI that we want to restore.

| Property                                 | Required | Updatable | Type     | Default | Description |
|:-----------------------------------------|----------|-----------|:---------|:--------|:------------|
| [fromBackup](#from-backup-configuration) | ✓        |           | object   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup >}} |
| downloadDiskConcurrency                  |          |           | integer  | 1       | {{< crd-field-description SGCluster.spec.initialData.restore.downloadDiskConcurrency >}} |

### From backup configuration

| Property                                   | Required | Updatable | Type     | Default | Description |
|:-------------------------------------------|----------|-----------|:---------|:--------|:------------|
| name                                       | ✓        |           | string   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup.name >}} |
| [pointInTimeRecovery](#pitr-configuration) |          |           | object   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup.pointInTimeRecovery >}} |

### PITR configuration

| Property           | Required | Updatable | Type     | Default | Description |
|:-------------------|----------|-----------|:---------|:--------|:------------|
| restoreToTimestamp |          |           | string   |         | {{< crd-field-description SGCluster.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    restore:
      fromBackup:
        uid: d7e660a9-377c-11ea-b04b-0242ac110004
      downloadDiskConcurrency: 1
```

## Scripts configuration

By default, stackgres creates as an empty database. To execute some scripts, we have the scripts
 options where you can specify a script or reference a key in a ConfigMap or a Secret that contains
 the script to execute.

| Property                   | Required | Updatable | Type     | Default  | Description |
|:---------------------------|----------|-----------|:---------|:---------|:------------|
| name                       |          |           | string   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.name >}} |
| database                   |          |           | string   | postgres | {{< crd-field-description SGCluster.spec.initialData.scripts.items.database >}} |
| script                     |          |           | string   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.script >}} |
| [scriptFrom](#script-from) |          |           | object   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.scriptFrom >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    scripts:
    - name: create-stackgres-user
      scriptFrom:
        secretKeyRef: # read the user from a Secret to maintain credentials in a safe place
          name: stackgres-secret-sqls-scripts
          key: create-stackgres-user.sql
    - name: create-stackgres-database
      script: |
        CREATE DATABASE stackgres WITH OWNER stackgres;
    - name: create-stackgres-schema
      database: stackgres
      scriptFrom:
        configMapKeyRef: # read long script from a ConfigMap to avoid have to much data in the helm releasea and the sgcluster CR
          name: stackgres-sqls-scripts
          key: create-stackgres-schema.sql
```

### Script from

| Property                                  | Required | Updatable | Type     | Default  | Description |
|:------------------------------------------|----------|-----------|:---------|:---------|:------------|
| [configMapKeyRef](#script-from-configmap) |          |           | object   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.scriptFrom.configMapKeyRef >}} |
| [secretKeyRef](#script-from-configmap)    |          |           | object   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.scriptFrom.secretKeyRef >}} |

#### Script from ConfigMap

| Property  | Required | Updatable | Type     | Default  | Description |
|:----------|----------|-----------|:---------|:---------|:------------|
| name      |          |           | string   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.scriptFrom.configMapKeyRef.name >}} |
| key       |          |           | string   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.scriptFrom.configMapKeyRef.key >}} |

#### Script from Secret

| Property  | Required | Updatable | Type     | Default  | Description |
|:----------|----------|-----------|:---------|:---------|:------------|
| name      |          |           | string   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.scriptFrom.secretKeyRef.name >}} |
| key       |          |           | string   |          | {{< crd-field-description SGCluster.spec.initialData.scripts.items.scriptFrom.secretKeyRef.key >}} |

## Managed SQL

By default, stackgres creates as an empty database. To execute some scripts, we have the managed SQL
 options where you can reference an SGScript contains the script to execute. When this configuration is
 changed scripts are executed ASAP.

| Property                       | Required | Updatable | Type     | Default  | Description |
|:-------------------------------|----------|-----------|:---------|:---------|:------------|
| continueOnSGScriptError        |          | ✓         | boolean  |          | {{< crd-field-description SGCluster.spec.managedSql.continueOnSGScriptError >}} |
| [scripts](managed-sql-scripts) |          | ✓         | boolean  |          | {{< crd-field-description SGCluster.spec.managedSql.scripts >}} |

## Managed SQL Scripts

| Property  | Required | Updatable | Type     | Default     | Description |
|:----------|----------|-----------|:---------|:------------|:------------|
| id        |          |           | integer  | auto-filled  | {{< crd-field-description SGCluster.spec.managedSql.scripts.items.id >}} |
| sgScript  |          | ✓         | string   |             | {{< crd-field-description SGCluster.spec.managedSql.scripts.items.sgScript >}} |

## Distributed logs
Specifies the distributed logs cluster to send logs to:

| Property                                                                     | Required | Updatable | Type     | Default | Description |
|:-----------------------------------------------------------------------------|----------|-----------|:---------|:--------|:------------|
| [sgDistributedLogs]({{% relref "/06-crd-reference/07-sgdistributedlogs" %}}) |          |           | string   |         | {{< crd-field-description SGCluster.spec.distributedLogs.sgDistributedLogs >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  distributedLogs:
    sgDistributedLogs: distributedlogs
```

## Non Production options

The following options should NOT be enabled in a production environment.

| Property                           | Required | Updatable | Type     | Default | Description |
|:-----------------------------------|----------|-----------|:---------|:--------|:------------|
| disableClusterPodAntiAffinity      |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.disableClusterPodAntiAffinity >}} |
| disablePatroniResourceRequirements |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.disablePatroniResourceRequirements >}} |
| disableClusterResourceRequirements |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.disableClusterResourceRequirements >}} |
| enableSetPatroniCpuRequests        |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enableSetPatroniCpuRequests >}} |
| enableSetClusterCpuRequests        |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enableSetClusterCpuRequests >}} |
| enableSetPatroniMemoryRequests     |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enableSetPatroniMemoryRequests >}} |
| enableSetClusterMemoryRequests     |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enableSetClusterMemoryRequests >}} |
| enabledFeatureGates                |          | ✓         | boolean  | false   | {{< crd-field-description SGCluster.spec.nonProductionOptions.enabledFeatureGates >}} |
