---
title: SGDbOps
weight: 9
url: reference/crd/sgdbops
description: Details about SGDbOps configurations
showToc: true
---

The `SGDbOps` custom resource represents database operations that are performed on a Postgres cluster.

___
**Kind:** SGDbOps

**listKind:** SGDbOpsList

**plural:** sgdbops

**singular:** sgdbops
___

**Spec**

| <div style="width:11rem">Property</div>               | Required | Updatable | Default | <div style="width:4rem">Type</div> | Description |
|:------------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| sgCluster                                             | ✓        |           |         | string  | {{< crd-field-description SGDbOps.spec.sgCluster >}} |
| [scheduling](#scheduling)                             |          |           |         | object  | {{< crd-field-description SGDbOps.spec.scheduling >}} |
| op                                                    | ✓        |           |         | string  | {{< crd-field-description SGDbOps.spec.op >}} |
| runAt                                                 |          |           |         | string  | {{< crd-field-description SGDbOps.spec.runAt >}} |
| timeout                                               |          |           |         | string  | {{< crd-field-description SGDbOps.spec.timeout >}} |
| maxRetries                                            |          |           |         | integer | {{< crd-field-description SGDbOps.spec.maxRetries >}} |
| [benchmark](#benchmark)                               |          |           |         | object  | {{< crd-field-description SGDbOps.spec.benchmark >}} |
| [vacuum](#vacuum)                                     |          |           |         | object  | {{< crd-field-description SGDbOps.spec.vacuum >}} |
| [repack](#repack)                                     |          |           |         | object  | {{< crd-field-description SGDbOps.spec.repack >}} |
| [majorVersionUpgrade](#major-version-upgrade)         |          |           |         | object  | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade >}} |
| [restart](#restart)                                   |          |           |         | object  | {{< crd-field-description SGDbOps.spec.restart >}} |
| [minorVersionUpgrade](#minor-version-upgrade)         |          |           |         | object  | {{< crd-field-description SGDbOps.spec.minorVersionUpgrade >}} |
| [securityUpgrade](#security-upgrade)                  |          |           |         | object  | {{< crd-field-description SGDbOps.spec.securityUpgrade >}} |

**Status**

| Property                                               | Required | Updatable | Default | Type    | Description |
|:-------------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| [conditions](#conditions)                              |          |           |         | array   | {{< crd-field-description SGDbOps.status.conditions >}} |
| opRetries                                              |          |           |         | integer | {{< crd-field-description SGDbOps.status.opRetries >}} |
| opStarted                                              |          |           |         | string  | {{< crd-field-description SGDbOps.status.opStarted >}} |
| [benchmark](#benchmark-status)                         |          |           |         | object  | {{< crd-field-description SGDbOps.status.benchmark >}} |
| [majorVersionUpgrade](#major-version-upgrade-status)   |          |           |         | object  | {{< crd-field-description SGDbOps.status.majorVersionUpgrade >}} |
| [restart](#restart-status)                             |          |           |         | object  | {{< crd-field-description SGDbOps.status.restart >}} |
| [minorVersionUpgrade](#minor-version-upgrade-status)   |          |           |         | object  | {{< crd-field-description SGDbOps.status.minorVersionUpgrade >}} |
| [securityUpgrade](#security-upgrade-status)            |          |           |         | object  | {{< crd-field-description SGDbOps.status.securityUpgrade >}} |

## Scheduling

StackGres pod scheduling configuration.

| <div style="width:8rem">Property</div> | Required | Updatable | <div style="width:4rem">Type</div> | Default                 | Description |
|:---------------------------------------|----------|-----------|:---------|:------------------------|:------------|
| nodeSelector                           |          | ✓         | object   |                         | {{< crd-field-description SGDbOps.spec.scheduling.nodeSelector >}} |
| tolerations                            |          | ✓         | array    |                         | {{< crd-field-description SGDbOps.spec.scheduling.tolerations >}}  |
| nodeAffinity                           |          | ✓         | object   |                         | {{< crd-field-description SGDbOps.spec.scheduling.nodeAffinity >}} |
| podAffinity                            |          | ✓         | object   |                         | {{< crd-field-description SGDbOps.spec.scheduling.podAffinity >}} |
| podAntiAffinity                        |          | ✓         | object   |                         | {{< crd-field-description SGDbOps.spec.scheduling.podAntiAffinity >}} |

## Benchmark

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| type                                       | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.benchmark.type >}} |
| [pgbench](#pgbench)                        |          |           | object   |                              | {{< crd-field-description SGDbOps.spec.benchmark.pgbench >}} |
| connectionType                             |          |           | string   | primary-service              | {{< crd-field-description SGDbOps.spec.benchmark.connectionType >}} |

## Pgbench

| <div style="width:13rem">Property</div>    | Required | Updatable | <div style="width:5rem">Type</div> | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| databaseSize                               | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.databaseSize >}} |
| duration                                   | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.duration >}} |
| usePreparedStatements                      |          |           | boolean  | false                        | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.usePreparedStatements >}} |
| concurrentClients                          |          |           | integer  | 1                            | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.concurrentClients >}} |
| threads                                    |          |           | integer  | 1                            | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.threads >}} |

## Vacuum

| <div style="width:11rem">Property</div>    | Required | Updatable | <div style="width:5rem">Type</div>  | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| full                                       |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.full >}} |
| freeze                                     |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.freeze >}} |
| analyze                                    |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.analyze >}} |
| disablePageSkipping                        |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.disablePageSkipping >}} |
| [databases](#vacuum-database)              |          |           | array    |                              | {{< crd-field-description SGDbOps.spec.vacuum.databases >}} |

## Vacuum database

| <div style="width:11rem">Property</div>    | Required | Updatable | <div style="width:5rem">Type</div> | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| name                                       | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.vacuum.databases.items.name >}} |
| full                                       |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.databases.items.full >}} |
| freeze                                     |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.databases.items.freeze >}} |
| analyze                                    |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.databases.items.analyze >}} |
| disablePageSkipping                        |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.databases.items.disablePageSkipping >}} |

## Repack

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| noOrder                                    |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.noOrder >}} |
| waitTimeout                                |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.repack.waitTimeout >}} |
| noKillBackend                              |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.noKillBackend >}} |
| noAnalyze                                  |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.noAnalyze >}} |
| excludeExtension                           |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.excludeExtension >}} |
| [databases](#repack-database)              |          |           | array    |                              | {{< crd-field-description SGDbOps.spec.repack.databases >}} |

### Repack database

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| name                                       | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.name >}} |
| noOrder                                    |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.noOrder >}} |
| waitTimeout                                |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.waitTimeout >}} |
| noKillBackend                              |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.noKillBackend >}} |
| noAnalyze                                  |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.noAnalyze >}} |
| excludeExtension                           |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.excludeExtension >}} |

## Restart

| <div style="width:4rem">Property</div>     | Required | Updatable | <div style="width:4rem">Type</div> | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| method                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.restart.method >}} |

## Security Upgrade

| <div style="width:10rem">Property</div>    | Required | Updatable | <div style="width:5rem">Type</div> | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| method                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.restart.method >}} |
| onlyPendingRestart                         |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.restart.onlyPendingRestart >}} |

## Major Version Upgrade

| <div style="width:9rem">Property</div>     | Required | Updatable | <div style="width:5rem">Type</div> | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| method                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.minorVersionUpgrade.method >}} |
| link                                       |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.link >}} |
| clone                                      |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.clone >}} |
| check                                      |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.check >}} |
| postgresVersion                            |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.postgresVersion >}} |
| sgPostgresConfig                           |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.sgPostgresConfig >}} |
| backupPath                                 |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.backupPath >}} |

## Minor Version Upgrade

| <div style="width:4rem">Property</div>     | Required | Updatable | <div style="width:4rem">Type</div> | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| method                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.minorVersionUpgrade.method >}} |

## Conditions

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| type                                       |          |           | string   |                              | {{< crd-field-description SGDbOps.status.conditions.items.type >}} |
| status                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.status.conditions.items.status >}} |
| reason                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.status.conditions.items.reason >}} |
| lastTransitionTime                         |          |           | string   |                              | {{< crd-field-description SGDbOps.status.conditions.items.lastTransitionTime >}} |
| message                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.conditions.items.message >}} |

## Benchmark Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| [pgbench](#pgbench-status)                 |          |           | object   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench >}} |

### Pgbench Status

| Property                                     | Required | Updatable | Type     | Default                      | Description |
|:---------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| scaleFactor                                  |          |           | numeric  |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.scaleFactor >}} |
| transactionsProcessed                        |          |           | integer  |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsProcessed >}} |
| [latency](#pgbench-status-latency)           |          |           | object   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.latency >}} |
| [transactionsPerSecond](#pgbench-status-tps) |          |           | object   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsPerSecond >}} |

#### Pgbench Status Latency

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| [average](#pgbench-status-latency-average)                                    |          |           | object   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.latency.average >}} |
| [standartDeviation](#pgbench-status-latency-standard-deviation)                          |          |           | object   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.latency.standardDeviation >}} |

##### Pgbench Status Latency Average

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| value                                      |          |           | number   |        0.00                  | {{< crd-field-description SGDbOps.status.benchmark.pgbench.latency.average.value >}} |
| unit                                       |          |           | string   |        ms                    | {{< crd-field-description SGDbOps.status.benchmark.pgbench.latency.average.unit >}} |

##### Pgbench Status Latency Standard Deviation

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| value                                      |          |           | number   |        0.00                  | {{< crd-field-description SGDbOps.status.benchmark.pgbench.latency.standardDeviation.value >}} |
| unit                                       |          |           | string   |        ms                    | {{< crd-field-description SGDbOps.status.benchmark.pgbench.latency.standardDeviation.unit >}} |

#### Pgbench Status TPS

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| [includingConnectionsEstablishing](#pgbench-status-tps-including-connections-establishing)                                    |          |           | object   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing >}} |
| [excludingConnectionsEstablishing](#pgbench-status-tps-excluding-connections-establishing)                                    |          |           | object   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing >}} |

##### Pgbench Status TPS Including Connections Establishing

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| value                                      |          |           | number   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.value >}} |
| unit                                       |          |           | string   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.unit >}} |

##### Pgbench Status TPS Excluding Connections Establishing

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| value                                      |          |           | number   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.value >}} |
| unit                                       |          |           | string   |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.unit >}} |

## Major Version Upgrade Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| primaryInstance                            |          |           | string   |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.primaryInstance >}} |
| initialInstances                           |          |           | array    |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.initialInstances >}} |
| pendingToRestartInstances                  |          |           | array    |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.pendingToRestartInstances >}} |
| restartedInstances                         |          |           | array    |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.restartedInstances >}} |
| failure                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.failure >}} |
| sourcePostgresVersion                      |          |           | string   |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.sourcePostgresVersion >}} |
| targetPostgresVersion                      |          |           | string   |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.targetPostgresVersion >}} |

## Restart Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| primaryInstance                            |          |           | string   |                              | {{< crd-field-description SGDbOps.status.restart.primaryInstance >}} |
| initialInstances                           |          |           | array    |                              | {{< crd-field-description SGDbOps.status.restart.initialInstances >}} |
| pendingToRestartInstances                  |          |           | array    |                              | {{< crd-field-description SGDbOps.status.restart.pendingToRestartInstances >}} |
| restartedInstances                         |          |           | array    |                              | {{< crd-field-description SGDbOps.status.restart.restartedInstances >}} |
| switchoverInitiated                        |          |           | string   |                              | {{< crd-field-description SGDbOps.status.restart.switchoverInitiated >}} |
| switchoverFinalized                        |          |           | string   |                              | {{< crd-field-description SGDbOps.status.restart.switchoverFinalized >}} |
| failure                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.restart.failure >}} |

## Minor Version Upgrade Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| primaryInstance                            |          |           | string   |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.primaryInstance >}} |
| initialInstances                           |          |           | array    |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.initialInstances >}} |
| pendingToRestartInstances                  |          |           | array    |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.pendingToRestartInstances >}} |
| restartedInstances                         |          |           | array    |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.restartedInstances >}} |
| switchoverInitiated                        |          |           | string   |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.switchoverInitiated >}} |
| switchoverFinalized                        |          |           | string   |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.switchoverFinalized >}} |
| failure                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.failure >}} |
| sourcePostgresVersion                      |          |           | string   |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.sourcePostgresVersion >}} |
| targetPostgresVersion                      |          |           | string   |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.targetPostgresVersion >}} |

## Security Upgrade Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| primaryInstance                            |          |           | string   |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.primaryInstance >}} |
| initialInstances                           |          |           | array    |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.initialInstances >}} |
| pendingToRestartInstances                  |          |           | array    |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.pendingToRestartInstances >}} |
| restartedInstances                         |          |           | array    |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.restartedInstances >}} |
| switchoverInitiated                        |          |           | string   |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.switchoverInitiated >}} |
| switchoverFinalized                        |          |           | string   |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.switchoverFinalized >}} |
| failure                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.failure >}} |

## Example

Example for a Pgbench benchmark:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: benchmark
spec:
 sgCluster: my-cluster
 op: benchmark
 maxRetries: 1
 benchmark:
   type: pgbench
   pgbench:
     databaseSize: 1Gi
     duration: P0DT0H10M0S
     concurrentClients: 10
     threads: 10
   connectionType: primary-service
```
