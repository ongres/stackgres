---
title: SGDbOps
weight: 9
url: reference/crd/sgdbops
description: Details about SGDbOps configurations
---

The database operations CR represent an operation that is performed on a cluster.

___
**Kind:** SGDbOps

**listKind:** SGDbOpsList

**plural:** sgdbops

**singular:** sgdbops
___

**Spec**

| Property                                              | Required | Updatable | Default | Type    | Description |
|:------------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| sgCluster                                             | ✓        |           |         | string  | {{< crd-field-description SGDbOps.spec.sgCluster >}} |
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
| [securityUpgrade](#secutiry-upgrade)                  |          |           |         | object  | {{< crd-field-description SGDbOps.spec.securityUpgrade >}} |

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
| [securityUpgrade](#secutiry-upgrade-status)            |          |           |         | object  | {{< crd-field-description SGDbOps.status.securityUpgrade >}} |

## Benchmark

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| type                                       | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.benchmark.type >}} |
| [pgbench](#pgbench)                        |          |           | object   |                              | {{< crd-field-description SGDbOps.spec.benchmark.pgbench >}} |
| connectionType                             |          |           | string   | primary-service              | {{< crd-field-description SGDbOps.spec.benchmark.connectionType >}} |

## Pgbench

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| databaseSize                               | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.databaseSize >}} |
| duration                                   | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.duration >}} |
| usePreparedStatements                      |          |           | boolean  | false                        | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.usePreparedStatements >}} |
| concurrentClients                          |          |           | integer  | 1                            | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.concurrentClients >}} |
| threads                                    |          |           | integer  | 1                            | {{< crd-field-description SGDbOps.spec.benchmark.pgbench.threads >}} |

## Vacuum

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| full                                       |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.full >}} |
| freeze                                     |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.freeze >}} |
| analyze                                    |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.analyze >}} |
| disablePageSkipping                        |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.vacuum.disablePageSkipping >}} |
| [databases](#vacuum-database)              |          |           | array    |                              | {{< crd-field-description SGDbOps.spec.vacuum.databases >}} |

## Vacuum database

| Property                                   | Required | Updatable | Type     | Default                      | Description |
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

## Repack database

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| name                                       | ✓        |           | string   |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.name >}} |
| noOrder                                    |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.noOrder >}} |
| waitTimeout                                |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.waitTimeout >}} |
| noKillBackend                              |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.noKillBackend >}} |
| noAnalyze                                  |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.noAnalyze >}} |
| excludeExtension                           |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.repack.databases.items.excludeExtension >}} |

## Major Version Upgrade

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| link                                       |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.link >}} |
| clone                                      |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.clone >}} |
| check                                      |          |           | boolean  |                              | {{< crd-field-description SGDbOps.spec.majorVersionUpgrade.check >}} |

## Restart

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| method                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.restart.method >}} |

## Minor Version Upgrade

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| method                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.minorVersionUpgrade.method >}} |

## Security Upgrade

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| method                                     |          |           | string   |                              | {{< crd-field-description SGDbOps.spec.securityUpgrade.method >}} |

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

## Pgbench Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| scaleFactor                                |          |           | numeric  |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.scaleFactor >}} |
| transactionsProcessed                      |          |           | integer  |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.transactionsProcessed >}} |
| latencyAverage                             |          |           | numeric  |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.latencyAverage >}} |
| tpsIncludingConnectionsEstablishing        |          |           | numeric  |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.tpsIncludingConnectionsEstablishing >}} |
| tpsExcludingConnectionsEstablishing        |          |           | numeric  |                              | {{< crd-field-description SGDbOps.status.benchmark.pgbench.tpsExcludingConnectionsEstablishing >}} |

## Major Version Upgrade Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| primaryInstance                            |          |           | string   |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.primaryInstance >}} |
| initialInstances                           |          |           | array    |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.initialInstances >}} |
| pendingToRestartInstances                  |          |           | array    |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.pendingToRestartInstances >}} |
| restartedInstances                         |          |           | array    |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.restartedInstances >}} |
| failure                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.majorVersionUpgrade.failure >}} |

## Restart Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| primaryInstance                            |          |           | string   |                              | {{< crd-field-description SGDbOps.status.restart.primaryInstance >}} |
| initialInstances                           |          |           | array    |                              | {{< crd-field-description SGDbOps.status.restart.initialInstances >}} |
| pendingToRestartInstances                  |          |           | array    |                              | {{< crd-field-description SGDbOps.status.restart.pendingToRestartInstances >}} |
| restartedInstances                         |          |           | array    |                              | {{< crd-field-description SGDbOps.status.restart.restartedInstances >}} |
| switchoverInitiated                        |          |           | boolean  |                              | {{< crd-field-description SGDbOps.status.restart.switchoverInitiated >}} |
| failure                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.restart.failure >}} |

## Minor Version Upgrade Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| primaryInstance                            |          |           | string   |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.primaryInstance >}} |
| initialInstances                           |          |           | array    |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.initialInstances >}} |
| pendingToRestartInstances                  |          |           | array    |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.pendingToRestartInstances >}} |
| restartedInstances                         |          |           | array    |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.restartedInstances >}} |
| switchoverInitiated                        |          |           | boolean  |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.switchoverInitiated >}} |
| failure                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.minorVersionUpgrade.failure >}} |

## Security Upgrade Status

| Property                                   | Required | Updatable | Type     | Default                      | Description |
|:-------------------------------------------|----------|-----------|:---------|:-----------------------------|:------------|
| primaryInstance                            |          |           | string   |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.primaryInstance >}} |
| initialInstances                           |          |           | array    |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.initialInstances >}} |
| pendingToRestartInstances                  |          |           | array    |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.pendingToRestartInstances >}} |
| restartedInstances                         |          |           | array    |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.restartedInstances >}} |
| switchoverInitiated                        |          |           | boolean  |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.switchoverInitiated >}} |
| failure                                    |          |           | string   |                              | {{< crd-field-description SGDbOps.status.securityUpgrade.failure >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: benchmark
spec:
 sgCluster: my-cluster
 op: benchmark
 benchmark:
   type: pgbench
   pgbench:
     databaseSize: 1Gi
     duration: P5M
     concurrentClients: 10
     threads: 10
   connectionType: primary-service
status:
 opStatus: completed
 opRetries: 0
 benchmark:
   pgbench:
     scaleFactor: 4
     transactionsProcessed: 3000000
     latencyAverage: 0.054
     tpsIncludingConnectionsEstablishing: 29300
     tpsExcludingConnectionsEstablishing: 30500
```
