---
title: Monitoring Streams
weight: 3
url: /administration/cdc-streaming/monitoring-streams
description: How to monitor SGStream progress, health, and performance.
---

This guide explains how to monitor SGStream resources and understand their status.

## Stream Status Overview

Every SGStream resource includes a comprehensive status section that tracks:

- **Conditions**: Overall stream health (Running, Failed, Completed)
- **Snapshot Status**: Progress of initial data capture
- **Streaming Status**: Real-time change capture metrics
- **Events Status**: Event processing statistics

## Checking Stream Status

### Basic Status

```bash
# List all streams
kubectl get sgstream

# Example output:
# NAME               STATUS    AGE
# migration-stream   Running   2h
# backup-stream      Failed    1d
```

### Detailed Status

```bash
kubectl get sgstream migration-stream -o yaml
```

The status section contains:

```yaml
status:
  conditions:
    - type: Running
      status: "True"
      reason: OperationRunning
      lastTransitionTime: "2024-01-15T10:30:00Z"
    - type: Failed
      status: "False"
      reason: OperationNotFailed
    - type: Completed
      status: "False"
      reason: OperationNotCompleted

  snapshot:
    snapshotRunning: false
    snapshotCompleted: true
    snapshotDurationInSeconds: 3600
    capturedTables:
      - public.users
      - public.orders
    totalTableCount: 2
    remainingTableCount: 0
    totalNumberOfEventsSeen: 1500000
    numberOfEventsFiltered: 0
    lastEvent: "2024-01-15T10:30:00Z"
    rowsScanned:
      public.users: 50000
      public.orders: 1450000
    queueTotalCapacity: 8192
    queueRemainingCapacity: 8192

  streaming:
    connected: true
    milliSecondsBehindSource: 150
    totalNumberOfEventsSeen: 25000
    numberOfCreateEventsSeen: 15000
    numberOfUpdateEventsSeen: 8000
    numberOfDeleteEventsSeen: 2000
    numberOfCommittedTransactions: 5000
    lastTransactionId: "txn-12345"
    sourceEventPosition:
      lsn: "0/1234567"
      txId: "12345"

  events:
    totalNumberOfEventsSeen: 1525000
    numberOfEventsFiltered: 0
    lastEvent: "2024-01-15T12:45:00Z"
```

## Understanding Conditions

### Running Condition

| Status | Reason | Description |
|--------|--------|-------------|
| True | OperationRunning | Stream is actively processing |
| False | OperationNotRunning | Stream is stopped or waiting |

### Failed Condition

| Status | Reason | Description |
|--------|--------|-------------|
| True | OperationFailed | Stream encountered an error |
| True | OperationTimedOut | Stream exceeded timeout |
| True | OperationLockLost | Lost distributed lock |
| False | OperationNotFailed | No failures detected |

### Completed Condition

| Status | Reason | Description |
|--------|--------|-------------|
| True | OperationCompleted | Stream finished successfully |
| False | OperationNotCompleted | Stream still running or not started |

## Monitoring Snapshot Progress

During the initial snapshot phase:

```bash
# Watch snapshot progress
kubectl get sgstream migration-stream -o jsonpath='{.status.snapshot}' | jq

# Check tables remaining
kubectl get sgstream migration-stream -o jsonpath='{.status.snapshot.remainingTableCount}'

# Check rows scanned per table
kubectl get sgstream migration-stream -o jsonpath='{.status.snapshot.rowsScanned}'
```

Key metrics during snapshot:

- **snapshotRunning**: `true` while snapshot is in progress
- **totalTableCount**: Total tables to snapshot
- **remainingTableCount**: Tables not yet completed
- **rowsScanned**: Rows processed per table
- **snapshotDurationInSeconds**: Time spent snapshotting

## Monitoring Streaming Progress

After snapshot completes, monitor real-time streaming:

```bash
# Check streaming lag
kubectl get sgstream migration-stream -o jsonpath='{.status.streaming.milliSecondsBehindSource}'

# Check if connected
kubectl get sgstream migration-stream -o jsonpath='{.status.streaming.connected}'

# View event counts
kubectl get sgstream migration-stream -o jsonpath='{.status.streaming}'
```

Key streaming metrics:

- **connected**: Whether connected to source
- **milliSecondsBehindSource**: Replication lag in milliseconds
- **totalNumberOfEventsSeen**: Total events processed
- **numberOfCreateEventsSeen**: INSERT events
- **numberOfUpdateEventsSeen**: UPDATE events
- **numberOfDeleteEventsSeen**: DELETE events
- **numberOfCommittedTransactions**: Completed transactions

## Viewing Stream Logs

```bash
# Get stream pod name
kubectl get pods -l app=StackGresStream,stackgres.io/stream-name=migration-stream

# View logs
kubectl logs -l app=StackGresStream,stackgres.io/stream-name=migration-stream -f

# View logs with timestamps
kubectl logs -l app=StackGresStream,stackgres.io/stream-name=migration-stream --timestamps -f
```

### Log Levels

Enable debug logging for troubleshooting:

```yaml
spec:
  pods:
    # Enable debug mode via environment variables
    customContainers:
      - name: stream
        env:
          - name: DEBUG_STREAM
            value: "true"
```

## Kubernetes Events

Stream operations emit Kubernetes events:

```bash
# View stream events
kubectl get events --field-selector involvedObject.name=migration-stream

# Example events:
# StreamCreated - Stream resource created
# StreamUpdated - Stream configuration changed
# StreamConfigFailed - Configuration error
# StreamFailed - Stream operation failed
```

## Monitoring with kubectl Watch

```bash
# Watch stream status continuously
kubectl get sgstream migration-stream -w

# Watch with custom columns
kubectl get sgstream -o custom-columns=\
NAME:.metadata.name,\
RUNNING:.status.conditions[?(@.type=="Running")].status,\
LAG:.status.streaming.milliSecondsBehindSource,\
EVENTS:.status.events.totalNumberOfEventsSeen
```

## Checking Job/Deployment Status

SGStream creates either a Job or Deployment:

```bash
# For Job mode (maxRetries >= 0)
kubectl get jobs -l stackgres.io/stream-name=migration-stream
kubectl describe job migration-stream

# For Deployment mode (maxRetries = -1)
kubectl get deployments -l stackgres.io/stream-name=migration-stream
kubectl describe deployment migration-stream
```

## Health Checks

### Stream Health Script

```bash
#!/bin/bash
STREAM_NAME=$1

# Get stream status
STATUS=$(kubectl get sgstream $STREAM_NAME -o json)

# Check if running
RUNNING=$(echo $STATUS | jq -r '.status.conditions[] | select(.type=="Running") | .status')
FAILED=$(echo $STATUS | jq -r '.status.conditions[] | select(.type=="Failed") | .status')

if [ "$FAILED" == "True" ]; then
    echo "CRITICAL: Stream $STREAM_NAME has failed"
    echo "Failure: $(echo $STATUS | jq -r '.status.failure')"
    exit 2
elif [ "$RUNNING" == "True" ]; then
    LAG=$(echo $STATUS | jq -r '.status.streaming.milliSecondsBehindSource // 0')
    if [ "$LAG" -gt 60000 ]; then
        echo "WARNING: Stream $STREAM_NAME lag is ${LAG}ms"
        exit 1
    fi
    echo "OK: Stream $STREAM_NAME is running, lag: ${LAG}ms"
    exit 0
else
    echo "WARNING: Stream $STREAM_NAME is not running"
    exit 1
fi
```

### Prometheus Alerts (Example)

```yaml
groups:
  - name: sgstream
    rules:
      - alert: SGStreamNotRunning
        expr: |
          kube_customresource_sgstream_status_condition{condition="Running"} != 1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "SGStream {{ $labels.name }} is not running"

      - alert: SGStreamHighLag
        expr: |
          sgstream_streaming_milliseconds_behind_source > 60000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "SGStream {{ $labels.name }} has high replication lag"
```

## Useful Monitoring Commands

```bash
# Stream summary
kubectl get sgstream -o custom-columns=\
'NAME:.metadata.name,'\
'SOURCE:.spec.source.type,'\
'TARGET:.spec.target.type,'\
'RUNNING:.status.conditions[?(@.type=="Running")].status,'\
'AGE:.metadata.creationTimestamp'

# Check all streams for failures
kubectl get sgstream -o json | jq -r '
  .items[] |
  select(.status.conditions[]? | select(.type=="Failed" and .status=="True")) |
  "\(.metadata.name): \(.status.failure)"'

# Get streaming metrics
kubectl get sgstream -o json | jq -r '
  .items[] |
  "\(.metadata.name): events=\(.status.events.totalNumberOfEventsSeen // 0), lag=\(.status.streaming.milliSecondsBehindSource // "N/A")ms"'
```

## Replication Slot Monitoring

Monitor the PostgreSQL replication slot created by the stream:

```bash
# Connect to source cluster
kubectl exec -it source-cluster-0 -c postgres-util -- psql

# Check replication slots
SELECT slot_name, active, restart_lsn, confirmed_flush_lsn
FROM pg_replication_slots
WHERE slot_name LIKE '%stream%';

# Check replication lag
SELECT slot_name,
       pg_current_wal_lsn() - confirmed_flush_lsn AS lag_bytes
FROM pg_replication_slots;
```

## Next Steps

- [Stream Configuration]({{% relref "04-administration-guide/17-cdc-streaming/02-stream-configuration" %}}) - Tune performance settings

