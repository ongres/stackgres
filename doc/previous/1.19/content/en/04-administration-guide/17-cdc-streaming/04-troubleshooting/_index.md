---
title: Troubleshooting
weight: 4
url: /administration/cdc-streaming/troubleshooting
description: Common issues and solutions for SGStream CDC operations.
---

This guide covers common issues encountered with SGStream and their solutions.

## Diagnosing Issues

### Check Stream Status

```bash
# Get detailed status
kubectl get sgstream my-stream -o yaml

# Check conditions
kubectl get sgstream my-stream -o jsonpath='{.status.conditions}' | jq

# Check failure message
kubectl get sgstream my-stream -o jsonpath='{.status.failure}'
```

### Check Pod Status

```bash
# Find stream pod
kubectl get pods -l stackgres.io/stream-name=my-stream

# Describe pod for events
kubectl describe pod -l stackgres.io/stream-name=my-stream

# Check logs
kubectl logs -l stackgres.io/stream-name=my-stream --tail=100
```

### Check Events

```bash
kubectl get events --field-selector involvedObject.name=my-stream --sort-by='.lastTimestamp'
```

## Common Issues

### Stream Fails to Start

#### Symptom
Stream pod is in `CrashLoopBackOff` or `Error` state.

#### Possible Causes and Solutions

**1. Source database not accessible**

```bash
# Check connectivity from cluster
kubectl run test-connection --rm -it --image=postgres:16 -- \
  psql -h source-cluster -U postgres -c "SELECT 1"
```

Solution: Verify network policies, service names, and credentials.

**2. Invalid credentials**

```bash
# Verify secret exists
kubectl get secret stream-credentials

# Check secret contents
kubectl get secret stream-credentials -o jsonpath='{.data.password}' | base64 -d
```

Solution: Update the secret with correct credentials.

**3. Logical replication not enabled**

```bash
# Check wal_level on source
kubectl exec source-cluster-0 -c postgres-util -- psql -c "SHOW wal_level"
```

Solution: For external PostgreSQL, set `wal_level = logical` and restart.

**4. Insufficient replication slots**

```bash
# Check max_replication_slots
kubectl exec source-cluster-0 -c postgres-util -- psql -c "SHOW max_replication_slots"

# Check current slots
kubectl exec source-cluster-0 -c postgres-util -- psql -c "SELECT * FROM pg_replication_slots"
```

Solution: Increase `max_replication_slots` in PostgreSQL configuration.

---

### Replication Slot Already Exists

#### Symptom
Error: `replication slot "xxx" already exists`

#### Solution

1. Check if another stream is using the slot:
```bash
kubectl get sgstream --all-namespaces
```

2. If the slot is orphaned, drop it manually:
```bash
kubectl exec source-cluster-0 -c postgres-util -- psql -c \
  "SELECT pg_drop_replication_slot('orphaned_slot_name')"
```

3. Or specify a unique slot name:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        slotName: unique_slot_name
```

---

### Publication Already Exists

#### Symptom
Error: `publication "xxx" already exists`

#### Solution

1. Use the existing publication:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        publicationName: existing_publication
        publicationAutocreateMode: disabled
```

2. Or drop the orphaned publication:
```bash
kubectl exec source-cluster-0 -c postgres-util -- psql -c \
  "DROP PUBLICATION orphaned_publication"
```

---

### High Replication Lag

#### Symptom
`milliSecondsBehindSource` keeps increasing.

#### Possible Causes and Solutions

**1. Target can't keep up**

Increase batch size and tune connection pool:
```yaml
spec:
  target:
    sgCluster:
      debeziumProperties:
        batchSize: 1000
        connectionPoolMax_size: 64
        useReductionBuffer: true
```

**2. Network latency**

Check network between source and target:
```bash
kubectl exec stream-pod -- ping target-cluster
```

**3. Insufficient resources**

Increase stream pod resources:
```yaml
spec:
  pods:
    resources:
      requests:
        cpu: 2000m
        memory: 2Gi
      limits:
        cpu: 4000m
        memory: 4Gi
```

**4. Large transactions**

For bulk operations, consider:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        maxBatchSize: 8192
        maxQueueSize: 32768
```

---

### WAL Disk Space Issues

#### Symptom
Source database running out of disk space due to WAL accumulation.

#### Causes
- Stream is paused or slow
- Replication slot is blocking WAL cleanup

#### Solutions

1. Check slot status:
```bash
kubectl exec source-cluster-0 -c postgres-util -- psql -c \
  "SELECT slot_name, active, pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) as lag_bytes
   FROM pg_replication_slots"
```

2. If stream is stuck, consider restarting:
```bash
kubectl delete pod -l stackgres.io/stream-name=my-stream
```

3. Enable heartbeats to acknowledge WAL:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        heartbeatIntervalMs: 30000
```

4. For emergency cleanup (data loss risk):
```bash
# Only if stream can be recreated
kubectl exec source-cluster-0 -c postgres-util -- psql -c \
  "SELECT pg_drop_replication_slot('stuck_slot')"
```

---

### Snapshot Takes Too Long

#### Symptom
Snapshot phase runs for extended periods.

#### Solutions

1. Increase parallelism:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        snapshotMaxThreads: 4
        snapshotFetchSize: 20000
```

2. Snapshot only required tables:
```yaml
spec:
  source:
    sgCluster:
      includes:
        - "public\\.important_table"
      debeziumProperties:
        snapshotIncludeCollectionList:
          - "public\\.important_table"
```

3. Use incremental snapshots for large tables:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        snapshotMode: no_data  # Skip initial snapshot
```

Then trigger incremental snapshots via signals.

---

### Data Type Conversion Errors

#### Symptom
Errors about unsupported or mismatched data types.

#### Solutions

1. Enable unknown datatype handling:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        includeUnknownDatatypes: true
        binaryHandlingMode: base64
```

2. Use custom converters for specific types:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        converters:
          geometry:
            type: io.debezium.connector.postgresql.converters.GeometryConverter
```

---

### CloudEvent Target Connection Refused

#### Symptom
Events not being delivered to CloudEvent endpoint.

#### Solutions

1. Verify endpoint URL:
```bash
kubectl run curl --rm -it --image=curlimages/curl -- \
  curl -v https://events.example.com/health
```

2. Check TLS settings:
```yaml
spec:
  target:
    cloudEvent:
      http:
        skipHostnameVerification: true  # For self-signed certs
```

3. Increase timeouts:
```yaml
spec:
  target:
    cloudEvent:
      http:
        connectTimeout: "30s"
        readTimeout: "60s"
        retryLimit: 10
```

---

### Stream Keeps Restarting

#### Symptom
Stream pod restarts frequently.

#### Possible Causes

1. **Out of memory**
```bash
kubectl describe pod -l stackgres.io/stream-name=my-stream | grep -A5 "Last State"
```

Solution: Increase memory limits.

2. **Transient errors**

Enable retries:
```yaml
spec:
  source:
    sgCluster:
      debeziumProperties:
        errorsMaxRetries: 10
        retriableRestartConnectorWaitMs: 30000
```

3. **PersistentVolume issues**

Check PVC status:
```bash
kubectl get pvc -l stackgres.io/stream-name=my-stream
```

---

### Cannot Delete Stream

#### Symptom
SGStream stuck in `Terminating` state.

#### Solutions

1. Check for finalizers:
```bash
kubectl get sgstream my-stream -o jsonpath='{.metadata.finalizers}'
```

2. Remove finalizers if stuck:
```bash
kubectl patch sgstream my-stream -p '{"metadata":{"finalizers":null}}' --type=merge
```

3. Clean up orphaned resources:
```bash
# Delete replication slot manually
kubectl exec source-cluster-0 -c postgres-util -- psql -c \
  "SELECT pg_drop_replication_slot('my_stream_slot')"

# Delete publication
kubectl exec source-cluster-0 -c postgres-util -- psql -c \
  "DROP PUBLICATION IF EXISTS my_stream_publication"
```

---

### Graceful Shutdown

To stop a stream gracefully and clean up resources:

1. Send tombstone signal:
```bash
kubectl annotate sgstream my-stream \
  debezium-signal.stackgres.io/tombstone='{}'
```

2. Wait for stream to complete:
```bash
kubectl get sgstream my-stream -w
```

3. Delete the stream:
```bash
kubectl delete sgstream my-stream
```

---

## Debug Mode

Enable verbose logging for detailed troubleshooting:

```yaml
spec:
  pods:
    customContainers:
      - name: stream
        env:
          - name: DEBUG_STREAM
            value: "true"
          - name: QUARKUS_LOG_LEVEL
            value: "DEBUG"
```

## Getting Help

If issues persist:

1. Collect diagnostic information:
```bash
# Stream status
kubectl get sgstream my-stream -o yaml > stream-status.yaml

# Pod logs
kubectl logs -l stackgres.io/stream-name=my-stream --tail=500 > stream-logs.txt

# Events
kubectl get events --field-selector involvedObject.name=my-stream > stream-events.txt

# Source database status
kubectl exec source-cluster-0 -c postgres-util -- psql -c \
  "SELECT * FROM pg_replication_slots" > replication-slots.txt
```

2. Check the [StackGres documentation](https://stackgres.io/doc/)
3. Open an issue on [GitHub](https://github.com/ongres/stackgres/issues)

## Related Documentation

- [Creating Streams]({{% relref "04-administration-guide/17-cdc-streaming/01-creating-streams" %}})
- [Stream Configuration]({{% relref "04-administration-guide/17-cdc-streaming/02-stream-configuration" %}})
- [Monitoring Streams]({{% relref "04-administration-guide/17-cdc-streaming/03-monitoring-streams" %}})
- [SGStream CRD Reference]({{% relref "06-crd-reference/15-sgstream" %}})
