---
title: Benchmarking with pgbench
weight: 7
url: /cookbook/running-operations/benchmark
description: Run a pgbench benchmark and read the results from the status.
showToc: true
---

## What it does

Runs [pgbench](https://www.postgresql.org/docs/current/pgbench.html) against a running
cluster and writes the results — transactions per second, latency average, latency
standard deviation, and per-statement latencies — into the resource's status. The
operation is expressed as an [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}})
resource with `op: benchmark` and is reconciled by the operator.

## When to use it

- You want a repeatable, operator-managed baseline measurement of cluster throughput.
- You are evaluating hardware, storage class, or configuration changes and need
  comparable numbers before and after.
- You want to verify that a cluster can sustain a target TPS under a given concurrency
  before promoting it to production.

## How to do it

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: benchmark
spec:
  # Name of the cluster to benchmark.
  sgCluster: cluster
  # The operation type.
  op: benchmark
  benchmark:
    # Only pgbench is supported for active load generation.
    type: pgbench
    # Connect to the primary for a read/write workload.
    # Use replicas-service for a read-only workload.
    connectionType: primary-service
    pgbench:
      # Size of the pgbench dataset expressed in MiB, GiB, or TiB.
      databaseSize: 1Gi
      # ISO 8601 duration: run the benchmark for 2 minutes.
      duration: PT2M
      # Number of concurrent client sessions. Defaults to 1.
      concurrentClients: 4
      # Worker threads inside pgbench. Keep <= concurrentClients. Defaults to 1.
      threads: 4
      # Benchmark mode:
      #   tpcb-like   – TPC-B-inspired mix of SELECT/UPDATE/INSERT (default for primary).
      #   select-only – SELECT-only workload (default for replicas).
      mode: tpcb-like
      # Protocol for query submission: simple | extended | prepared.
      # prepared reuses parse results from the second iteration onward.
      queryMode: simple
```

```bash
kubectl apply -f benchmark.yaml
```

Watch the operation progress:

```bash
kubectl get sgdbops -n my-cluster benchmark -w
```

Once the condition `Completed` is `True`, read the results:

```bash
kubectl get sgdbops -n my-cluster benchmark \
  -o jsonpath='{.status.benchmark.pgbench}' | jq .
```

Key fields in `.status.benchmark.pgbench`:

| Field | Description |
|-------|-------------|
| `scaleFactor` | Scale factor used (`--scale`) |
| `transactionsProcessed` | Total transactions completed |
| `transactionsPerSecond.excludingConnectionsEstablishing` | TPS (value + unit) |
| `transactionsPerSecond.includingConnectionsEstablishing` | TPS including connect time |
| `latency.average` | Mean transaction latency (value + unit) |
| `latency.standardDeviation` | Latency standard deviation (value + unit) |

## How it works

The operator creates a Job Pod that runs in two phases:

1. **Initialization** — pgbench populates a dedicated benchmark database (or the database
   named in `spec.benchmark.database`) with the pgbench schema at the scale implied by
   `databaseSize`. If no `database` is specified, a randomly-named database is created and
   dropped after the run.
2. **Benchmark** — pgbench drives the workload for the requested `duration` using
   `concurrentClients` sessions and `threads` worker threads. Aggregate results are written
   to `.status.benchmark.pgbench` when the run finishes.

The `connectionType` field controls which Kubernetes Service is used. `primary-service`
targets the read/write primary; `replicas-service` targets the read-only replicas service,
which automatically selects `select-only` mode by default.

## What to expect

- The SGDbOps transitions through conditions `Running` → `Completed` (or `Failed`).
- Initialization can take several minutes for large `databaseSize` values; the benchmark
  phase itself runs for exactly the specified `duration`.
- After completion, `.status.benchmark.pgbench.transactionsPerSecond` and
  `.status.benchmark.pgbench.latency` contain the primary metrics to compare across runs.
- A compressed HdrHistogram is also stored in `.status.benchmark.pgbench.hdrHistogram` for
  detailed latency percentile analysis.

## Pitfalls

- **Generates real load and writes data.** pgbench performs INSERT, UPDATE, and SELECT
  statements. Do not run this operation against a cluster that is serving production
  traffic; use a dedicated test cluster or run during a maintenance window.
- **Temporary database.** When `spec.benchmark.database` is not set, pgbench creates and
  then drops a temporary database. Ensure the superuser has the `CREATEDB` privilege or
  supply explicit credentials via `spec.benchmark.credentials`.
- **`databaseSize` consumes disk space.** The generated dataset must fit on the cluster's
  data volume. Requesting a size close to the volume limit will cause the initialization
  phase to fail.
- **`threads` must not exceed `concurrentClients`.** pgbench distributes clients across
  threads; setting `threads` greater than `concurrentClients` is rejected.
- **SGDbOps is single-use.** Once an SGDbOps completes (or fails), create a new resource
  to run the benchmark again; editing a completed resource has no effect.
- **`usePreparedStatements` is deprecated.** Use `queryMode: prepared` instead; the old
  field is ignored.
