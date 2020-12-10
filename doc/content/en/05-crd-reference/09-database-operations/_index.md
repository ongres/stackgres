---
title: Database Operations
weight: 9
url: reference/crd/sgdbops
---

The database operations CR represent an operation that is performed on a cluster.

___
**Kind:** SGDbOps

**listKind:** SGDbOpsList

**plural:** sgdbops

**singular:** sgdbops
___

**Spec**

| Property                  | Required | Updatable | Default | Type    | Description |
|:--------------------------|----------|-----------|:--------|:--------|:------------|
| sgCluster                 | ✓        |           |         | string  | {{< crd-field-description SGDbOps.spec.sgCluster >}} |
| op                        | ✓        |           |         | string  | {{< crd-field-description SGDbOps.spec.op >}} |
| runAt                     |          |           |         | string  | {{< crd-field-description SGDbOps.spec.runAt >}} |
| timeout                   |          |           |         | string  | {{< crd-field-description SGDbOps.spec.timeout >}} |
| maxRetries                |          |           |         | integer | {{< crd-field-description SGDbOps.spec.maxRetries >}} |
| [benchmark](#benchmark)   |          |           |         | object  | {{< crd-field-description SGDbOps.spec.benchmark >}} |

**Status**

| Property                         | Required | Updatable | Default | Type    | Description |
|:---------------------------------|----------|-----------|:--------|:--------|:------------|
| [conditions](#conditions)        |          |           |         | array   | {{< crd-field-description SGDbOps.status.conditions >}} |
| opRetries                        |          |           |         | integer | {{< crd-field-description SGDbOps.status.opRetries >}} |
| opStarted                        |          |           |         | string  | {{< crd-field-description SGDbOps.status.opStarted >}} |
| [benchmark](#benchmark-status)   |          |           |         | object  | {{< crd-field-description SGDbOps.status.benchmark >}} |

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

Example:

```yaml
apiVersion: stackgres.io/v1beta1
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
