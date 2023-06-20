---
title: SGShardedCluster
weight: 12
url: /reference/crd/sgshardedcluster
description: Details about SGShardedCluster
showToc: true
---

StackGres PostgreSQL sharded clusters are created using the `SGShardedCluster` custom resource.

___

**Kind:** SGShardedCluster

**listKind:** SGShardedClusterList

**plural:** sgshardedclusters

**singular:** sgshardedcluster
___

**Spec**

| <div style="width:12rem">Property</div>                                                    | Required | Updatable | <div style="width:5rem">Type</div> | Default           | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:-------------------------------------------------------------------|
| type                                                                                       | ✓        | ✓         | string                             |                   | {{< crd-field-description SGShardedCluster.spec.type >}}    |
| database                                                                                   | ✓        | ✓         | string                             |                   | {{< crd-field-description SGShardedCluster.spec.database >}}    |
| [postgres](#postgres)                                                                      |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.postgres >}}              |
| [replication](#replication)                                                                |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.replication >}}           |
| [postgresServices](#postgres-services)                                                     |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.postgresServices >}}      |
| [configurations](#configurations)                                                          |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.configurations >}}        |
| [metadata](#metadata)                                                                      |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.metadata >}}           |
| [coordinator](#coordinator)                                                                | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator >}}              |
| [shards](#shards)                                                                          | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards >}}              |
| prometheusAutobind                                                                         |          | ✓         | boolean                            | false             | {{< crd-field-description SGShardedCluster.spec.prometheusAutobind >}}    |
| [distributedLogs](#distributed-logs)                                                       |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.distributedLogs >}}       |
| [nonProductionOptions](#non-production-options)                                            |          | ✓         | array                              |                   | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions >}}  |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  postgres:
    version: 'latest'
  coordinator:
    instances: 1
    pods:
      persistentVolume:
        size: '5Gi'
  shards:
    clusters: 2
    instancesPerCluster: 1
    pods:
      persistentVolume:
        size: '5Gi'
```

## Postgres

| <div style="width:6rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default  | Description                                                      |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------|:-----------------------------------------------------------------|
| version                                | ✓        | ✓         | string                             |          | {{< crd-field-description SGShardedCluster.spec.postgres.version >}}    |
| flavor                                 |          |           | string                             |          | {{< crd-field-description SGShardedCluster.spec.postgres.flavor >}}     |
| [extensions](#postgres-extensions)     |          | ✓         | array                              |          | {{< crd-field-description SGShardedCluster.spec.postgres.extensions >}} |
| [ssl](#postgres-ssl)                   |          | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.postgres.ssl >}}        |

## Postgres extensions

Extensions that will be installed in the clusters.

| <div style="width:6rem">Property</div> | Required | Updatable | <div style="width:3rem">Type</div> | <div style="width:6rem">Default</div> | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:------------|
| name                                   | ✓        | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.postgres.extensions.items.name >}} |
| version                                |          | ✓         | string                             | stable                                | {{< crd-field-description SGShardedCluster.spec.postgres.extensions.items.version >}} |
| publisher                              |          | ✓         | string                             | com.ongres                            | {{< crd-field-description SGShardedCluster.spec.postgres.extensions.items.publisher >}} |
| repository                             |          | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.postgres.extensions.items.repository >}} |

Example:

``` yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  postgres:
    extensions:
      - {name: 'timescaledb', version: '2.3.1'}
```

### Postgres SSL

By default, support for SSL connections to Postgres is disabled, to enable it configure this section.
SSL connections will be handled by Envoy using [Postgres filter's SSL termination](https://github.com/envoyproxy/envoy/issues/10942).

| <div style="width:6rem">Property</div>                  | Required | Updatable | Type     | Default  | Description |
|:--------------------------------------------------------|----------|-----------|:---------|:---------|:------------|
| enabled                                                 |          |           | string   | false    | {{< crd-field-description SGShardedCluster.spec.postgres.ssl.enabled >}} |
| [certificateSecretKeySelector](#ssl-certificate-secret) |          |           | object   |          | {{< crd-field-description SGShardedCluster.spec.postgres.ssl.certificateSecretKeySelector >}} |
| [privateKeySecretKeySelector](#ssl-private-key-secret)  |          |           | object   |          | {{< crd-field-description SGShardedCluster.spec.postgres.ssl.privateKeySecretKeySelector >}} |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  postgres:
    ssl:
      enabled: true
      certificateSecretKeySelector:
        name: stackgres-secrets
        key: cert
      privateKeySecretKeySelector:
        name: stackgres-secrets
        key: key
```

#### SSL Certificate Secret

| Property  | Required | Updatable | Type     | Default  | Description |
|:----------|----------|-----------|:---------|:---------|:------------|
| name      | ✓        | ✓         | string   |          | {{< crd-field-description SGShardedCluster.spec.postgres.ssl.certificateSecretKeySelector.name >}} |
| key       | ✓        | ✓         | string   |          | {{< crd-field-description SGShardedCluster.spec.postgres.ssl.certificateSecretKeySelector.key >}} |

#### SSL Private Key Secret

| Property  | Required | Updatable | Type     | Default  | Description |
|:----------|----------|-----------|:---------|:---------|:------------|
| name      | ✓        | ✓         | string   |          | {{< crd-field-description SGShardedCluster.spec.postgres.ssl.privateKeySecretKeySelector.name >}} |
| key       | ✓        | ✓         | string   |          | {{< crd-field-description SGShardedCluster.spec.postgres.ssl.privateKeySecretKeySelector.key >}} |

## Postgres Services

Specifies the service configuration for the sharded cluster:

| <div style="width:5rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:4rem">Default</div>             | Description                                                            |
|:---------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------------------|:-----------------------------------------------------------------------|
| [coordinator](#services)               |          | ✓         | object                             |  | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator >}}  |
| [shards](#shards-services)             |          | ✓         | object                             |           | {{< crd-field-description SGShardedCluster.spec.postgresServices.shards >}}  |

### Coordinator Services

| <div style="width:5rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:4rem">Default</div>           | Description                                                            |
|:---------------------------------------|----------|-----------|:-----------------------------------|:------------------------------------------------|:-----------------------------------------------------------------------|
| [any](#postgres-service)               |          | ✓         | object                             |                                                 | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any >}}  |
| [primary](#postgres-service)           |          | ✓         | object                             |                                                 | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.primary >}}  |
| customPorts                            |          | ✓         | array                              |                                                 | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.customPorts >}}  |

### Shards Services

| <div style="width:5rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:4rem">Default</div>           | Description                                                            |
|:---------------------------------------|----------|-----------|:-----------------------------------|:------------------------------------------------|:-----------------------------------------------------------------------|
| [primaries](#postgres-service)         |          | ✓         | object                             |                                                 | {{< crd-field-description SGShardedCluster.spec.postgresServices.shards.primaries >}}  |
| customPorts                            |          | ✓         | array                              |                                                 | {{< crd-field-description SGShardedCluster.spec.postgresServices.shards.customPorts >}}  |

#### Postgres Service

| <div style="width:8rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div> | <div style="width:5rem">Default</div> | Description                                                                 |
|:---------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:----------------------------------------------------------------------------|
| enabled                                |          | ✓         | boolean                            | true                                  | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.enabled >}}  |
| type                                   |          | ✓         | string                             | ClusterIP                             | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.type >}}  |
| allocateLoadBalancerNodePorts          |          | ✓         | boolean                            |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.allocateLoadBalancerNodePorts >}}  |
| externalIPs                            |          | ✓         | array                              |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.externalIPs >}}  |
| healthCheckNodePort                    |          | ✓         | integer                            |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.healthCheckNodePort >}}  |
| internalTrafficPolicy                  |          | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.internalTrafficPolicy >}}  |
| ipFamilies                             |          | ✓         | array                              |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.ipFamilies >}}  |
| ipFamilyPolicy                         |          | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.ipFamilyPolicy >}}  |
| loadBalancerClass                      |          | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.loadBalancerClass >}}  |
| loadBalancerIP                         |          | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.loadBalancerIP >}}  |
| loadBalancerSourceRanges               |          | ✓         | array                              |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.loadBalancerSourceRanges >}}  |
| sessionAffinity                        |          | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.sessionAffinity >}}  |
| sessionAffinityConfig                  |          | ✓         | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.postgresServices.coordinator.any.sessionAffinityConfig >}}  |

## Configurations

Custom configurations.

| <div style="width:9rem">Property</div>                                    | Required | Updatable | <div style="width:4rem">Type</div> | Default           | Description |
|:--------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:------------|
| [backups](#backups)                                                       |          | ✓         | array                              |                   | {{< crd-field-description SGShardedCluster.spec.configurations.backups >}}   |
| [credentials](#credentials)                                               |          |           | object                             |                   | {{< crd-field-description SGShardedCluster.spec.configurations.credentials >}}   |

Example:

``` yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  configurations:
    backups:
    - sgObjectStorage: 'backupconf'
```

### Backups

| <div style="width:9rem">Property</div>                                    | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:5rem">Default</div> | Description |
|:--------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:------------|
| [sgObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}})   |          | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.sgObjectStorage >}} |
| paths                                                                     |          | ✓         | array                              |                                       | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.paths >}}   |
| retention                                                                 |          | ✓         | integer                            | 5                                     | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.retention >}} |
| cronSchedule                                                              |          | ✓         | string                             | 05:00 UTC                             | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.cronSchedule >}} |
| compression                                                               |          | ✓         | string                             | lz4                                   | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.compression >}} |
| [performance](#backups-performance)                                       |          | ✓         | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.performance >}} |

#### Backups Performance

| <div style="width:12rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div>     | Default                           | Description |
|:----------------------------------------|----------|-----------|:---------------------------------------|:----------------------------------|:------------|
| maxDiskBandwidth                        |          | ✓         | integer                                | unlimited                         | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.performance.maxDiskBandwidth >}} |
| maxNetworkBandwidth                     |          | ✓         | integer                                | unlimited                         | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.performance.maxNetworkBandwidth >}} |
| uploadDiskConcurrency                   |          | ✓         | integer                                | 1                                 | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.performance.uploadDiskConcurrency >}} |
| uploadConcurrency                       |          | ✓         | integer                                | 16                                | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.performance.uploadConcurrency >}} |
| downloadConcurrency                     |          | ✓         | integer                                | min(10, # of objects to download) | {{< crd-field-description SGShardedCluster.spec.configurations.backups.items.performance.downloadConcurrency >}} |

## Credentials

| <div style="width:9rem">Property</div>                                    | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:5rem">Default</div> | Description |
|:--------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:------------|
| [patroni](#patroni-credentials)                                           |          |           | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.patroni >}} |
| [users](#users-credentials)                                               |          |           | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users >}} |

### Patroni Credentials

| <div style="width:7rem">Property</div>              | Required | Updatable | <div style="width:4rem">Type</div> | Default  | Description |
|:----------------------------------------------------|----------|-----------|:-----------------------------------|:---------|:------------|
| restApiPassword                                     | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.patroni.restApiPassword >}}       |

### Users Credentials

| <div style="width:7rem">Property</div>              | Required | Updatable | <div style="width:4rem">Type</div> | Default  | Description |
|:----------------------------------------------------|----------|-----------|:-----------------------------------|:---------|:------------|
| [superuser](users-credentials-superuser)         | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.superuser >}}       |
| [replication](users-credentials-replication)     | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.replication >}}       |
| [authenticator](users-credentials-authenticator) | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.authenticator >}}       |

### Users Credentials Superuser

| <div style="width:7rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default  | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------|:------------|
| username                               | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.superuser.username >}}       |
| password                               | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.superuser.password >}}       |

### Users Credentials Replication

| <div style="width:7rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default  | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------|:------------|
| username                               | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.replication.username >}}       |
| password                               | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.replication.password >}}       |

### Users Credentials Authenticator

| <div style="width:7rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default  | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------|:------------|
| username                               | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.authenticator.username >}}       |
| password                               | ✓        | ✓         | object                             |          | {{< crd-field-description SGShardedCluster.spec.configurations.credentials.users.authenticator.password >}}       |

## Coordinator

| <div style="width:12rem">Property</div>                                                    | Required | Updatable | <div style="width:5rem">Type</div> | Default           | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:-------------------------------------------------------------------|
| instances                                                                                  | ✓        | ✓         | integer                            |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.instances >}}             |
| [sgInstanceProfile]({{% relref "/06-crd-reference/02-sginstanceprofile" %}})               |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.coordinator.sgInstanceProfile >}}     |
| [replication](#replication)                                                                |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.replication >}}           |
| [pods](#pods)                                                                  | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.pods >}}                  |
| [configurations](#coordinator-configurations)                                              |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.configurations >}}        |
| [managedSql](#managed-sql)                                                                 |          |           | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.managedSql >}}            |
| [metadata](#metadata)                                                          |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.coordinator.metadata >}}           |

### Coordinator Configurations

Custom Postgres configuration for coordinator.

| <div style="width:9rem">Property</div>                                    | Required | Updatable | <div style="width:4rem">Type</div> | Default           | Description |
|:--------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:------------|
| [sgPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.coordinator.configurations.sgPostgresConfig >}} |
| [sgPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}})   |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.coordinator.configurations.sgPoolingConfig >}}  |

## Shards

| <div style="width:12rem">Property</div>                                                    | Required | Updatable | <div style="width:5rem">Type</div> | Default           | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:-------------------------------------------------------------------|
| clusters                                                                                   | ✓        | ✓         | integer                            |                   | {{< crd-field-description SGShardedCluster.spec.shards.clusters >}}             |
| instancesPerCluster                                                                        | ✓        | ✓         | integer                            |                   | {{< crd-field-description SGShardedCluster.spec.shards.instancesPerCluster >}}             |
| [replication](#replication)                                                                |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.replication >}}           |
| [sgInstanceProfile]({{% relref "/06-crd-reference/02-sginstanceprofile" %}})               |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.shards.sgInstanceProfile >}}     |
| [pods](#pods)                                                                              | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.pods >}}                  |
| [configurations](#shards-configurations)                                                   |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.configurations >}}        |
| [managedSql](#managed-sql)                                                                 |          |           | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.managedSql >}}            |
| [metadata](#metadata)                                                                      |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.metadata >}}           |
| [overrides](#shards-overrides)                                                             |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.overrides >}}           |

### Shards Overrides

| <div style="width:12rem">Property</div>                                                    | Required | Updatable | <div style="width:5rem">Type</div> | Default           | Description                                                        |
|:-------------------------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:-------------------------------------------------------------------|
| clusters                                                                                   | ✓        | ✓         | integer                            |                   | {{< crd-field-description SGShardedCluster.spec.shards.overrides.items.index >}}             |
| instancesPerCluster                                                                        | ✓        | ✓         | integer                            |                   | {{< crd-field-description SGShardedCluster.spec.shards.overrides.items.instancesPerCluster >}}             |
| [replication](#replication)                                                                |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.overrides.items.replication >}}           |
| [sgInstanceProfile]({{% relref "/06-crd-reference/02-sginstanceprofile" %}})               |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.shards.sgInstanceProfile >}}     |
| [pods](#pods)                                                                              | ✓        | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.overrides.items.pods >}}                  |
| [configurations](#shards-configurations)                                                   |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.overrides.items.configurations >}}        |
| [managedSql](#managed-sql)                                                                 |          |           | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.overrides.items.managedSql >}}            |
| [metadata](#metadata)                                                                      |          | ✓         | object                             |                   | {{< crd-field-description SGShardedCluster.spec.shards.overrides.items.metadata >}}           |

### Shards Configurations

Custom Postgres configuration for shards.

| <div style="width:9rem">Property</div>                                    | Required | Updatable | <div style="width:4rem">Type</div> | Default           | Description |
|:--------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:------------------|:------------|
| [sgPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.shards.configurations.sgPostgresConfig >}} |
| [sgPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}})   |          | ✓         | string                             | will be generated | {{< crd-field-description SGShardedCluster.spec.shards.configurations.sgPoolingConfig >}}  |

## Replication

| <div style="width:8rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default  | Description                                                            |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------|:-----------------------------------------------------------------------|
| mode                                   |          | ✓         | string                             |          | {{< crd-field-description SGShardedCluster.spec.replication.mode >}}          |
| syncInstances                          |          | ✓         | integer                            |          | {{< crd-field-description SGShardedCluster.spec.replication.syncInstances >}} |

## Pods

Cluster's pod configuration

| <div style="width:14rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div> | <div style="width:4rem">Default</div> | Description |
|:----------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:------------|
| [persistentVolume](#persistent-volume)  | ✓        | ✓         | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.persistentVolume >}} |
| disableConnectionPooling                |          | ✓         | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.disableConnectionPooling >}} |
| disableMetricsExporter                  |          | ✓         | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.disableMetricsExporter >}} |
| disablePostgresUtil                     |          | ✓         | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.disablePostgresUtil >}} |
| [scheduling](#scheduling)               |          | ✓         | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling >}} |
| managementPolicy                        |          | ✓         | string                             | OrderedReady                          | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.managementPolicy >}} |
| [resources](#resources)                  |          | ✓         | object                             |                                       | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.resources >}} |
| customVolumes                           |          | ✓         | array                              |                                       | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.customVolumes >}}  |
| customInitContainers                    |          | ✓         | array                              |                                       | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.customInitContainers >}}  |
| customContainers                        |          | ✓         | array                              |                                       | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.customContainers >}}  |

### Persistent Volume

Configuration of the persistent volumes that the coordinator cluster pods are going to use.

| <div style="width:7rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | <div style="width:11rem">Default</div> | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------------------------------------|:------------|
| size                                   | ✓        | ✓         | string                             |                                        | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.persistentVolume.size >}} |
| storageClass                           |          | ✓         | string                             | default storage class                  | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.persistentVolume.storageClass >}} |

### Scheduling

StackGres pod scheduling configuration.

| <div style="width:14rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:----------------------------------------|----------|-----------|:-----------------------------------|:--------|:------------|
| nodeSelector                            |          | ✓         | object                             |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.nodeSelector >}} |
| tolerations                             |          | ✓         | array                              |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.tolerations >}} |
| nodeAffinity                            |          | ✓         | object                             |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity >}} |
| priorityClassName                       |          | ✓         | string                             |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.priorityClassName >}} |
| podAffinity                             |          | ✓         | object                             |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity >}} |
| podAntiAffinity                         |          | ✓         | object                             |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity >}} |
| topologySpreadConstraints               |          | ✓         | array                              |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity >}} |
| [backup](#backup)                       |          | ✓         | object                             |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.backup >}} |

#### Backup

StackGres backup pod scheduling configuration.

| <div style="width:8rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default        | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:---------------|:------------|
| nodeSelector                           |          | ✓         | object                             |                | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector >}} |
| tolerations                            |          | ✓         | array                              |                | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations >}} |
| nodeAffinity                           |          | ✓         | object                             |                | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity >}} |
| priorityClassName                      |          | ✓         | string                             |                | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.backup.priorityClassName >}} |
| podAffinity                            |          | ✓         | object                             |                | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity >}} |
| podAntiAffinity                        |          | ✓         | object                             |                | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity >}} |

### Resources

StackGres pod resources configuration.

| <div style="width:14rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:----------------------------------------|----------|-----------|:-----------------------------------|:--------|:------------|
| enableClusterLimitsRequirements         |          | ✓         | boolean                            |         | {{< crd-field-description SGShardedCluster.spec.coordinator.pods.resources.enableClusterLimitsRequirements >}} |

## Managed SQL

By default, StackGres creates empty databases.
To execute SQL scripts at startup, the managed SQL options can be used to reference an SGScript.
When this configuration is applied, the scripts are executed in the coordinator ASAP.

| <div style="width:13rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div> | Default  | Description |
|:----------------------------------------|----------|-----------|:-----------------------------------|:---------|:------------|
| continueOnSGScriptError                 |          | ✓         | boolean                            |          | {{< crd-field-description SGShardedCluster.spec.coordinator.managedSql.continueOnSGScriptError >}} |
| [scripts](#managed-sql-scripts)         |          | ✓         | array                              |          | {{< crd-field-description SGShardedCluster.spec.coordinator.managedSql.scripts >}} |

## Managed SQL Scripts

| <div style="width:5rem">Property</div> | Required | Updatable | <div style="width:5rem">Type</div> | <div style="width:6rem">Default</div> | Description |
|:---------------------------------------|----------|-----------|:-----------------------------------|:--------------------------------------|:------------|
| id                                     |          |           | integer                            | auto-filled                           | {{< crd-field-description SGShardedCluster.spec.coordinator.managedSql.scripts.items.id >}} |
| sgScript                               |          | ✓         | string                             |                                       | {{< crd-field-description SGShardedCluster.spec.coordinator.managedSql.scripts.items.sgScript >}} |

## Metadata

Defines custom metadata for StackGres generated Kubernetes resources.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| [annotations](#annotations)   |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.annotations >}} |
| [labels](#labels)             |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.labels >}} |

### Annotations

Defines custom annotations for StackGres generated Kubernetes resources.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| allResources                  |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.annotations.allResources >}} |
| clusterPods                   |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.annotations.clusterPods >}} |
| services                      |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.annotations.services >}} |
| primaryService               |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.annotations.primaryService >}} |
| replicasService              |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.annotations.replicasService >}} |

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
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

Defines custom labels for StackGres generated Kubernetes resources.

| Property                      | Required | Updatable | Type     | Default        | Description |
|:------------------------------|----------|-----------|:---------|:---------------|:------------|
| clusterPods                   |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.labels.clusterPods >}} |
| services                      |          | ✓         | object   |                | {{< crd-field-description SGShardedCluster.spec.metadata.labels.services >}} |

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  metadata:
    labels:
      clusterPods:
        customLabel: customLabelValue
      services:
        customLabel: customLabelValue
```

## Distributed Logs

Distributed logs clusters configuration.
Where the PostgreSQL logs are being sent to.

| <div style="width:9rem">Property</div>                                       | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:-----------------------------------------------------------------------------|----------|-----------|:-----------------------------------|:--------|:------------|
| [sgDistributedLogs]({{% relref "/06-crd-reference/07-sgdistributedlogs" %}}) |          |           | string                             |         | {{< crd-field-description SGShardedCluster.spec.distributedLogs.sgDistributedLogs >}} |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  distributedLogs:
    sgDistributedLogs: distributedlogs
```

## Non Production Options

The following options should NOT be enabled in a production environment.

| <div style="width:19rem">Property</div> | Re&shy;quired | Up&shy;datable | <div style="width:5rem">Type</div> | <div style="width:4rem">Default</div> | Description |
|:----------------------------------------|---------------|----------------|:-----------------------------------|:--------------------------------------|:------------|
| disableClusterPodAntiAffinity           |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.disableClusterPodAntiAffinity >}} |
| disablePatroniResourceRequirements      |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.disablePatroniResourceRequirements >}} |
| disableClusterResourceRequirements      |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.disableClusterResourceRequirements >}} |
| enableSetPatroniCpuRequests             |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enableSetPatroniCpuRequests >}} |
| enableSetClusterCpuRequests             |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enableSetClusterCpuRequests >}} |
| enableSetPatroniMemoryRequests          |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enableSetPatroniMemoryRequests >}} |
| enableSetClusterMemoryRequests          |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enableSetClusterMemoryRequests >}} |
| enabledFeatureGates                     |               | ✓              | boolean                            | false                                 | {{< crd-field-description SGShardedCluster.spec.nonProductionOptions.enabledFeatureGates >}} |
