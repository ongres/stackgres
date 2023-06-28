# stackgres-cluster

![Version: 1.5.0-SNAPSHOT](https://img.shields.io/badge/Version-1.5.0--SNAPSHOT-informational?style=flat-square) ![AppVersion: 1.5.0-SNAPSHOT](https://img.shields.io/badge/AppVersion-1.5.0--SNAPSHOT-informational?style=flat-square)

StackGres Cluster

**Homepage:** <https://stackgres.io>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| OnGres Inc. | <info@ongres.com> | <https://ongres.com> |

## Source Code

* <https://gitlab.com/ongresinc/stackgres>

## Requirements

Kubernetes: `1.18.0-0 - 1.27.x-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| cluster.configurations.backups.compression | string | `"lz4"` |  |
| cluster.configurations.backups.cronSchedule | string | `"*/2 * * * *"` |  |
| cluster.configurations.backups.performance.uploadConcurrency | int | `1` |  |
| cluster.configurations.backups.performance.uploadDiskConcurrency | int | `1` |  |
| cluster.configurations.backups.retention | int | `5` |  |
| cluster.configurations.sgPoolingConfig | string | `"pgbouncerconf"` |  |
| cluster.configurations.sgPostgresConfig | string | `"postgresconf"` |  |
| cluster.create | bool | `true` |  |
| cluster.distributedLogs.retention | string | `"7 days"` |  |
| cluster.distributedLogs.sgDistributedLogs | string | `"distributedlogs"` |  |
| cluster.instances | int | `1` |  |
| cluster.pods.disableConnectionPooling | bool | `false` |  |
| cluster.pods.disableMetricsExporter | bool | `false` |  |
| cluster.pods.disablePostgresUtil | bool | `false` |  |
| cluster.pods.persistentVolume.size | string | `"5Gi"` |  |
| cluster.postgres.version | string | `"latest"` |  |
| cluster.postgresServices.primary.enabled | bool | `true` |  |
| cluster.postgresServices.primary.type | string | `"ClusterIP"` |  |
| cluster.postgresServices.replicas.enabled | bool | `true` |  |
| cluster.postgresServices.replicas.type | string | `"ClusterIP"` |  |
| cluster.prometheusAutobind | bool | `true` |  |
| cluster.replication.mode | string | `"async"` |  |
| cluster.replication.role | string | `"ha-read"` |  |
| cluster.restartClusterRole | string | `"cluster-admin"` |  |
| cluster.restartPrimaryFirst | bool | `false` |  |
| cluster.restartReducedImpact | bool | `true` |  |
| cluster.restartTimeout | int | `300` |  |
| cluster.sgInstanceProfile | string | `"size-xs"` |  |
| configurations.create | bool | `true` |  |
| configurations.objectstorage.azureBlob | object | `{}` |  |
| configurations.objectstorage.create | bool | `false` |  |
| configurations.objectstorage.gcs | object | `{}` |  |
| configurations.objectstorage.s3 | object | `{}` |  |
| configurations.objectstorage.s3Compatible | object | `{}` |  |
| configurations.poolingconfig.create | bool | `true` |  |
| configurations.poolingconfig.pgBouncer."pgbouncer.ini".default_pool_size | string | `"50"` |  |
| configurations.poolingconfig.pgBouncer."pgbouncer.ini".max_client_conn | string | `"200"` |  |
| configurations.poolingconfig.pgBouncer."pgbouncer.ini".pool_mode | string | `"transaction"` |  |
| configurations.postgresconfig."postgresql.conf".checkpoint_timeout | string | `"30"` |  |
| configurations.postgresconfig."postgresql.conf".password_encryption | string | `"scram-sha-256"` |  |
| configurations.postgresconfig."postgresql.conf".random_page_cost | string | `"1.5"` |  |
| configurations.postgresconfig."postgresql.conf".shared_buffers | string | `"256MB"` |  |
| configurations.postgresconfig.create | bool | `true` |  |
| distributedLogs.create | bool | `true` |  |
| distributedLogs.enabled | bool | `false` |  |
| distributedLogs.persistentVolume.size | string | `"5Gi"` |  |
| distributedLogs.postgresServices.primary.type | string | `"ClusterIP"` |  |
| distributedLogs.postgresServices.replicas.enabled | bool | `true` |  |
| distributedLogs.postgresServices.replicas.type | string | `"ClusterIP"` |  |
| instanceProfiles[0].cpu | string | `"500m"` |  |
| instanceProfiles[0].memory | string | `"512Mi"` |  |
| instanceProfiles[0].name | string | `"size-xs"` |  |
| instanceProfiles[1].cpu | string | `"1"` |  |
| instanceProfiles[1].memory | string | `"2Gi"` |  |
| instanceProfiles[1].name | string | `"size-s"` |  |
| instanceProfiles[2].cpu | string | `"2"` |  |
| instanceProfiles[2].memory | string | `"4Gi"` |  |
| instanceProfiles[2].name | string | `"size-m"` |  |
| instanceProfiles[3].cpu | string | `"4"` |  |
| instanceProfiles[3].memory | string | `"8Gi"` |  |
| instanceProfiles[3].name | string | `"size-l"` |  |
| instanceProfiles[4].cpu | string | `"6"` |  |
| instanceProfiles[4].memory | string | `"16Gi"` |  |
| instanceProfiles[4].name | string | `"size-xl"` |  |
| instanceProfiles[5].cpu | string | `"8"` |  |
| instanceProfiles[5].memory | string | `"32Gi"` |  |
| instanceProfiles[5].name | string | `"size-xxl"` |  |
| kind | string | `"SGCluster"` |  |
| nonProductionOptions.disableClusterPodAntiAffinity | bool | `false` |  |
| nonProductionOptions.disableClusterResourceRequirements | bool | `false` |  |
| nonProductionOptions.disablePatroniResourceRequirements | bool | `false` |  |
| shardedCluster.database | string | `"citus"` |  |
| shardedCluster.shards.clusters | int | `2` |  |
| shardedCluster.shards.instancesPerCluster | int | `1` |  |
| shardedCluster.type | string | `"citus"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
