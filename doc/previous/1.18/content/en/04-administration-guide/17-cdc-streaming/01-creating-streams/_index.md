---
title: Creating Streams
weight: 1
url: /administration/cdc-streaming/creating-streams
description: Step-by-step guide to creating SGStream resources for CDC operations.
---

This guide walks you through creating SGStream resources for different use cases.

## Basic Stream: SGCluster to SGCluster Migration

The most common use case is migrating data between two StackGres clusters.

### Step 1: Ensure Source Cluster Exists

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: source-cluster
spec:
  instances: 2
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '10Gi'
```

### Step 2: Create Target Cluster

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: target-cluster
spec:
  instances: 2
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '10Gi'
```

### Step 3: Create the Stream

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: migration-stream
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
      database: myapp  # Optional: defaults to 'postgres'
  target:
    type: SGCluster
    sgCluster:
      name: target-cluster
      database: myapp
  maxRetries: -1  # Run continuously
  pods:
    persistentVolume:
      size: 1Gi
```

Apply all resources:

```bash
kubectl apply -f source-cluster.yaml
kubectl apply -f target-cluster.yaml
kubectl apply -f migration-stream.yaml
```

## Stream to CloudEvent Endpoint

Stream database changes to an HTTP endpoint that accepts CloudEvents.

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: events-stream
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
      database: orders
      includes:
        - "public\\.orders"      # Only stream the orders table
        - "public\\.order_items"
  target:
    type: CloudEvent
    cloudEvent:
      format: json
      binding: http
      http:
        url: https://events.example.com/webhook
        headers:
          Authorization: "Bearer ${TOKEN}"
        connectTimeout: "5s"
        readTimeout: "30s"
        retryLimit: 5
        retryBackoffDelay: 60
  pods:
    persistentVolume:
      size: 1Gi
```

## Stream from External PostgreSQL

Capture changes from any PostgreSQL database with logical replication enabled.

### Prerequisites on External PostgreSQL

```sql
-- Ensure wal_level is set to logical (requires restart)
ALTER SYSTEM SET wal_level = 'logical';

-- Create a user for replication
CREATE USER cdc_user WITH REPLICATION PASSWORD 'secure_password';

-- Grant necessary permissions
GRANT SELECT ON ALL TABLES IN SCHEMA public TO cdc_user;
```

### Create Credentials Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: external-pg-credentials
type: Opaque
stringData:
  username: cdc_user
  password: secure_password
```

### Create the Stream

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: external-migration
spec:
  source:
    type: Postgres
    postgres:
      host: external-postgres.example.com
      port: 5432
      database: production
      username:
        name: external-pg-credentials
        key: username
      password:
        name: external-pg-credentials
        key: password
  target:
    type: SGCluster
    sgCluster:
      name: target-cluster
  pods:
    persistentVolume:
      size: 2Gi
```

## Stream with Table Filtering

Control which tables are captured using include/exclude patterns.

### Include Specific Tables

```yaml
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
      includes:
        - "public\\.users"
        - "public\\.orders"
        - "inventory\\..*"  # All tables in inventory schema
```

### Exclude Tables

```yaml
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
      excludes:
        - "public\\.audit_logs"
        - "temp\\..*"  # Exclude all temp schema tables
```

## Stream with Custom Credentials

Use specific database users instead of the superuser.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: stream-credentials
type: Opaque
stringData:
  username: stream_user
  password: stream_password
---
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: custom-auth-stream
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
      username:
        name: stream-credentials
        key: username
      password:
        name: stream-credentials
        key: password
  target:
    type: SGCluster
    sgCluster:
      name: target-cluster
      username:
        name: stream-credentials
        key: username
      password:
        name: stream-credentials
        key: password
  pods:
    persistentVolume:
      size: 1Gi
```

## Stream with PgLambda (Serverless Processing)

Process each change event with a custom JavaScript function via Knative.

### Prerequisites

- Knative Serving must be installed in your cluster

### Create the Stream

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: lambda-stream
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
  target:
    type: PgLambda
    pgLambda:
      scriptType: javascript
      script: |
        // Access the CloudEvent
        const data = event.data;

        // Log the change
        console.log('Received change:', JSON.stringify(data));

        // Process based on operation type
        if (data.op === 'c') {
          console.log('New record inserted:', data.after);
        } else if (data.op === 'u') {
          console.log('Record updated:', data.before, '->', data.after);
        } else if (data.op === 'd') {
          console.log('Record deleted:', data.before);
        }

        // Send response
        response.writeHead(200);
        response.end('OK');
      knative:
        http:
          connectTimeout: "10s"
          readTimeout: "60s"
  pods:
    persistentVolume:
      size: 1Gi
```

### Script from ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: lambda-script
data:
  handler.js: |
    const data = event.data;
    // Your processing logic here
    response.writeHead(200);
    response.end('OK');
---
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: lambda-stream
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
  target:
    type: PgLambda
    pgLambda:
      scriptType: javascript
      scriptFrom:
        configMapKeyRef:
          name: lambda-script
          key: handler.js
  pods:
    persistentVolume:
      size: 1Gi
```

## One-Time Migration (Job Mode)

For migrations that should complete and not restart:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: one-time-migration
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
      debeziumProperties:
        snapshotMode: initial_only  # Snapshot only, no streaming
  target:
    type: SGCluster
    sgCluster:
      name: target-cluster
  maxRetries: 3  # Retry up to 3 times on failure
  pods:
    persistentVolume:
      size: 1Gi
```

## Verifying Stream Creation

After creating a stream, verify it's running:

```bash
# Check stream status
kubectl get sgstream

# View detailed status
kubectl get sgstream migration-stream -o yaml

# Check the stream pod
kubectl get pods -l app=StackGresStream

# View stream logs
kubectl logs -l app=StackGresStream -f
```

## Next Steps

- [Stream Configuration]({{% relref "04-administration-guide/17-cdc-streaming/02-stream-configuration" %}}) - Advanced configuration options
- [Monitoring Streams]({{% relref "04-administration-guide/17-cdc-streaming/03-monitoring-streams" %}}) - Monitor stream progress
- [Troubleshooting]({{% relref "04-administration-guide/17-cdc-streaming/04-troubleshooting" %}}) - Common issues and solutions
