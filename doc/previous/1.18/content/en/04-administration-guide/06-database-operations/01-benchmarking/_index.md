---
title: Benchmarking
weight: 1
url: /administration/database-operations/benchmarking
description: How to run benchmarks and sample real queries on StackGres clusters.
showToc: true
---

StackGres provides built-in benchmarking capabilities through SGDbOps to measure and analyze your cluster's performance. This includes both synthetic benchmarks (pgbench) and real query sampling.

## Overview

The benchmark operation supports two types:
- **pgbench**: Synthetic TPC-B like benchmark
- **sampling**: Capture and replay real production queries

## Running pgbench Benchmarks

### Basic Benchmark

Run a simple TPC-B style benchmark:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: benchmark-basic
spec:
  sgCluster: my-cluster
  op: benchmark
  benchmark:
    type: pgbench
    pgbench:
      databaseSize: 1Gi
      duration: PT10M
      concurrentClients: 10
```

Apply and monitor:

```bash
kubectl apply -f benchmark.yaml
kubectl get sgdbops benchmark-basic -w
```

### Benchmark Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `databaseSize` | Size of test database | Required |
| `duration` | How long to run (ISO 8601) | Required |
| `concurrentClients` | Simulated clients | `1` |
| `threads` | Worker threads | `1` |
| `mode` | Benchmark type | `tpcb-like` |
| `samplingRate` | Transaction sampling rate | `1.0` |

### Benchmark Modes

#### TPC-B Like (Default)

Standard read-write workload:

```yaml
benchmark:
  type: pgbench
  pgbench:
    databaseSize: 1Gi
    duration: PT10M
    mode: tpcb-like
```

#### Select Only

Read-only workload for replicas:

```yaml
benchmark:
  type: pgbench
  connectionType: replicas-service
  pgbench:
    databaseSize: 1Gi
    duration: PT10M
    mode: select-only
```

#### Custom Scripts

Use your own SQL scripts:

```yaml
benchmark:
  type: pgbench
  pgbench:
    databaseSize: 1Gi
    duration: PT10M
    mode: custom
    custom:
      initialization:
        script: |
          CREATE TABLE benchmark_data (
            id SERIAL PRIMARY KEY,
            value TEXT,
            created_at TIMESTAMP DEFAULT NOW()
          );
          INSERT INTO benchmark_data (value)
          SELECT md5(random()::text) FROM generate_series(1, 10000);
      scripts:
        - script: |
            \set id random(1, 10000)
            SELECT * FROM benchmark_data WHERE id = :id;
          weight: 7
        - script: |
            INSERT INTO benchmark_data (value) VALUES (md5(random()::text));
          weight: 3
```

### Connection Type

Choose where to run the benchmark:

```yaml
benchmark:
  connectionType: primary-service   # Default for write workloads
  # connectionType: replicas-service  # For read-only tests
```

## Query Sampling

Query sampling captures real production queries for later replay, enabling realistic performance testing.

### Sampling Modes

The sampling operation supports three modes for selecting top queries:

| Mode | Description | Use Case |
|------|-------------|----------|
| `time` | Select slowest queries | Performance optimization |
| `calls` | Select most frequent queries | Capacity planning |
| `custom` | Custom query selection | Advanced analysis |

### Time-Based Sampling (Default)

Capture the slowest queries:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: sample-slow-queries
spec:
  sgCluster: my-cluster
  op: benchmark
  benchmark:
    type: sampling
    sampling:
      mode: time
      targetDatabase: myapp
      topQueriesCollectDuration: PT1H
      samplingDuration: PT30M
      queries: 10
```

### Call-Based Sampling

Capture the most frequently called queries:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: sample-frequent-queries
spec:
  sgCluster: my-cluster
  op: benchmark
  benchmark:
    type: sampling
    sampling:
      mode: calls
      targetDatabase: myapp
      topQueriesCollectDuration: PT2H
      samplingDuration: PT1H
      queries: 20
```

### Custom Query Selection

Use a custom SQL query to select which queries to sample:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: sample-custom
spec:
  sgCluster: my-cluster
  op: benchmark
  benchmark:
    type: sampling
    sampling:
      mode: custom
      targetDatabase: myapp
      topQueriesCollectDuration: PT1H
      samplingDuration: PT30M
      customTopQueriesQuery: |
        SELECT query, calls, total_exec_time
        FROM pg_stat_statements
        WHERE query NOT LIKE '%pg_%'
          AND query NOT LIKE 'COMMIT%'
          AND query NOT LIKE 'BEGIN%'
        ORDER BY total_exec_time DESC
        LIMIT 10
```

### Sampling Configuration Options

| Option | Description | Required |
|--------|-------------|----------|
| `mode` | Selection mode (`time`, `calls`, `custom`) | No |
| `targetDatabase` | Database to sample | No (default: `postgres`) |
| `topQueriesCollectDuration` | Duration to collect query stats (ISO 8601) | Yes |
| `samplingDuration` | Duration to sample queries (ISO 8601) | Yes |
| `queries` | Number of queries to capture | No (default: `10`) |
| `topQueriesFilter` | SQL WHERE clause filter | No |
| `topQueriesPercentile` | Percentile threshold (0-99) | No |
| `topQueriesMin` | Minimum query count threshold | No |
| `customTopQueriesQuery` | Custom selection query | Required if mode=`custom` |
| `samplingMinInterval` | Minimum interval between samples (ms) | No |
| `omitTopQueriesInStatus` | Don't store queries in status | No |

### Duration Format

Durations use ISO 8601 format (`PnDTnHnMn.nS`):

| Example | Meaning |
|---------|---------|
| `PT10M` | 10 minutes |
| `PT1H` | 1 hour |
| `PT2H30M` | 2 hours 30 minutes |
| `P1DT12H` | 1 day 12 hours |

## Replaying Sampled Queries

After sampling, replay the captured queries as a benchmark:

### Step 1: Run Sampling

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: sample-production
spec:
  sgCluster: my-cluster
  op: benchmark
  benchmark:
    type: sampling
    sampling:
      mode: time
      targetDatabase: production
      topQueriesCollectDuration: PT2H
      samplingDuration: PT1H
      queries: 15
```

### Step 2: Replay Queries

Reference the sampling SGDbOps in a replay benchmark:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: replay-production
spec:
  sgCluster: my-cluster
  op: benchmark
  benchmark:
    type: pgbench
    database: test_replay
    pgbench:
      duration: PT30M
      concurrentClients: 20
      mode: replay
      samplingSGDbOps: sample-production
```

### Selective Replay

Replay specific queries by index:

```yaml
benchmark:
  type: pgbench
  pgbench:
    duration: PT15M
    mode: replay
    samplingSGDbOps: sample-production
    custom:
      scripts:
        - replay: 0  # First sampled query
          weight: 5
        - replay: 2  # Third sampled query
          weight: 3
        - replay: 4  # Fifth sampled query
          weight: 2
```

## Viewing Results

### Check Operation Status

```bash
kubectl get sgdbops benchmark-basic -o yaml
```

### Benchmark Results

Results are stored in `.status.benchmark`:

```yaml
status:
  benchmark:
    pgbench:
      scaleFactor: 100
      transactionsProcessed: 150432
      transactionsPerSecond:
        includingConnectionsEstablishing:
          value: 2507
        excludingConnectionsEstablishing:
          value: 2532
      latency:
        average:
          value: 3.98
          unit: ms
        standardDeviation:
          value: 2.15
          unit: ms
```

### Sampling Results

Sampled queries are available in the status:

```bash
kubectl get sgdbops sample-production -o jsonpath='{.status.benchmark.sampling.topQueries}' | jq
```

## Example Workflows

### Performance Baseline

Establish performance baseline for a cluster:

```yaml
# Step 1: Run standard benchmark
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: baseline-benchmark
spec:
  sgCluster: prod-cluster
  op: benchmark
  benchmark:
    type: pgbench
    pgbench:
      databaseSize: 10Gi
      duration: PT30M
      concurrentClients: 50
      threads: 4
---
# Step 2: Sample real queries
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: baseline-sampling
spec:
  sgCluster: prod-cluster
  op: benchmark
  benchmark:
    type: sampling
    sampling:
      mode: time
      targetDatabase: production
      topQueriesCollectDuration: P1D
      samplingDuration: PT4H
      queries: 20
```

### Pre-Upgrade Testing

Test performance before PostgreSQL upgrade:

```yaml
# On current version
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: pre-upgrade-benchmark
spec:
  sgCluster: my-cluster
  op: benchmark
  benchmark:
    type: pgbench
    pgbench:
      databaseSize: 5Gi
      duration: PT15M
      concurrentClients: 20
```

Compare results after upgrade to identify regressions.

### Replica Performance

Test read performance on replicas:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: replica-benchmark
spec:
  sgCluster: my-cluster
  op: benchmark
  benchmark:
    type: pgbench
    connectionType: replicas-service
    pgbench:
      databaseSize: 2Gi
      duration: PT10M
      concurrentClients: 30
      mode: select-only
```

## Best Practices

1. **Isolate benchmark database**: Let SGDbOps create a temporary database to avoid affecting production data

2. **Run during low traffic**: Schedule benchmarks during maintenance windows

3. **Use realistic data sizes**: Match `databaseSize` to your production scale factor

4. **Sample before major changes**: Capture query patterns before upgrades or configuration changes

5. **Multiple sampling modes**: Use both `time` and `calls` modes for comprehensive analysis

6. **Appropriate duration**: Use longer durations for more stable results

7. **Monitor cluster resources**: Watch CPU, memory, and I/O during benchmarks

## Related Documentation

- [SGDbOps Reference]({{% relref "06-crd-reference/08-sgdbops" %}})
- [Database Operations]({{% relref "04-administration-guide/06-database-operations/01-benchmarking" %}})
- [Monitoring]({{% relref "04-administration-guide/08-monitoring" %}})
