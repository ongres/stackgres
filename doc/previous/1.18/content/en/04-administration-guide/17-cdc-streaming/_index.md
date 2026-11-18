---
title: CDC Streaming
weight: 17
url: /administration/cdc-streaming
description: How to set up and manage Change Data Capture (CDC) streaming with SGStream.
---

StackGres provides Change Data Capture (CDC) capabilities through the `SGStream` custom resource, powered by [Debezium Engine](https://debezium.io/documentation/reference/stable/development/engine.html).
This allows you to capture database changes in real-time and stream them to various targets.

## Prerequisites

Before creating an SGStream, ensure you have:

1. **A running StackGres cluster** (if using SGCluster as source or target)
2. **Logical replication enabled** on the source database (enabled by default on SGCluster)
3. **Appropriate database permissions** for the CDC user (superuser or replication privileges)
4. **Sufficient WAL retention** to prevent data loss during snapshotting

For external PostgreSQL sources, ensure:
- `wal_level` is set to `logical`
- `max_replication_slots` is sufficient for your streams
- `max_wal_senders` allows additional connections

## Quick Start

Here's a minimal example to stream changes from one SGCluster to another:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: my-stream
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
  target:
    type: SGCluster
    sgCluster:
      name: target-cluster
  pods:
    persistentVolume:
      size: 1Gi
```

Apply with:

```bash
kubectl apply -f sgstream.yaml
```

Check the stream status:

```bash
kubectl get sgstream my-stream -o yaml
```

## Stream Lifecycle

1. **Creation**: When you create an SGStream, the operator creates a Deployment or Job (based on `maxRetries` setting)
2. **Initialization**: The stream pod initializes Debezium with your configuration
3. **Snapshotting**: If configured, captures the initial database state
4. **Streaming**: Continuously captures and forwards database changes
5. **Completion/Termination**: Stream completes (Job) or runs indefinitely (Deployment)

## Execution Modes

The stream execution mode is controlled by the `maxRetries` field:

| Value | Mode | Description |
|-------|------|-------------|
| `-1` (default) | Deployment | Runs indefinitely, automatically restarts on failure |
| `0` | Job | Runs once, no retries on failure |
| `> 0` | Job | Runs with specified number of retry attempts |

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Source         │     │  SGStream Pod   │     │  Target         │
│  (PostgreSQL)   │────▶│  (Debezium)     │────▶│  (SGCluster/    │
│                 │     │                 │     │   CloudEvent/   │
│                 │     │                 │     │   PgLambda)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        │                       │
        ▼                       ▼
   Replication            Persistent
   Slot + WAL             Volume
                          (Offsets)
```

## Topics

{{% children style="li" depth="1" description="true" %}}
