---
title: Stream Configuration
weight: 2
url: /administration/cdc-streaming/stream-configuration
description: Advanced configuration options for SGStream including Debezium properties.
---

This guide covers advanced configuration options for SGStream resources.

## Source Configuration

### SGCluster Source

When using an SGCluster as source:

```yaml
spec:
  source:
    type: SGCluster
    sgCluster:
      name: source-cluster
      database: mydb                    # Target database (default: postgres)
      username:                         # Optional: custom credentials
        name: secret-name
        key: username
      password:
        name: secret-name
        key: password
      includes:                         # Tables to include (regex patterns)
        - "public\\.orders"
      excludes:                         # Tables to exclude (regex patterns)
        - "public\\.temp_.*"
      skipDropReplicationSlotAndPublicationOnTombstone: false
      debeziumProperties:               # Debezium PostgreSQL connector options
        # ... see below
```

### External Postgres Source

```yaml
spec:
  source:
    type: Postgres
    postgres:
      host: postgres.example.com        # Required
      port: 5432                         # Default: 5432
      database: production
      username:
        name: secret-name
        key: username
      password:
        name: secret-name
        key: password
```

## Debezium Source Properties

The `debeziumProperties` section allows fine-tuning of the Debezium PostgreSQL connector.

### Replication Configuration

```yaml
debeziumProperties:
  # Logical decoding plugin (default: pgoutput)
  pluginName: pgoutput

  # Replication slot name (auto-generated if not specified)
  slotName: my_stream_slot

  # Drop slot when stream stops gracefully (default: true)
  slotDropOnStop: true

  # Enable slot failover for PostgreSQL 17+ (default: false)
  slotFailover: false

  # Publication name (auto-generated if not specified)
  publicationName: my_publication

  # Publication auto-creation mode
  # all_tables: Create for all tables (default)
  # disabled: Publication must exist
  # filtered: Create only for filtered tables
  # no_tables: Create empty publication
  publicationAutocreateMode: all_tables
```

### Snapshot Configuration

Control how initial snapshots are performed:

```yaml
debeziumProperties:
  # Snapshot mode (default: initial)
  # always: Snapshot on every start
  # initial: Snapshot only if no offsets exist
  # initial_only: Snapshot only, then stop
  # no_data/never: Skip snapshot, stream only
  # when_needed: Snapshot if offsets unavailable
  # configuration_based: Use snapshot config properties
  snapshotMode: initial

  # Snapshot isolation level (default: serializable)
  # serializable: Highest consistency, blocks DDL
  # repeatable_read: Good consistency, allows some anomalies
  # read_committed: Lower consistency, better performance
  snapshotIsolationMode: serializable

  # Snapshot locking mode (default: none)
  # shared: Hold table locks during schema read
  # none: No locks (don't use if DDL may occur)
  snapshotLockingMode: none

  # Tables to include in snapshot (regex patterns)
  snapshotIncludeCollectionList:
    - "public\\.important_table"

  # Rows per fetch during snapshot (default: 10240)
  snapshotFetchSize: 10240

  # Maximum threads for parallel snapshots (default: 1)
  snapshotMaxThreads: 4

  # Delay before starting snapshot in ms
  snapshotDelayMs: 5000
```

### Incremental Snapshots

For capturing changes while streaming continues:

```yaml
debeziumProperties:
  # Chunk size for incremental snapshots (default: 1024)
  incrementalSnapshotChunkSize: 2048

  # Watermarking strategy
  # insert_insert: Two entries per chunk (default)
  # insert_delete: One entry, deleted after chunk
  incrementalSnapshotWatermarkingStrategy: insert_insert

  # Read-only mode (no watermark writes)
  readOnly: false
```

### Data Type Handling

```yaml
debeziumProperties:
  # Decimal handling (default: precise)
  # precise: java.math.BigDecimal
  # double: double (may lose precision)
  # string: formatted string
  decimalHandlingMode: precise

  # Time precision (default: adaptive)
  # adaptive: Match database precision
  # adaptive_time_microseconds: TIME as microseconds
  # connect: Kafka Connect format (milliseconds)
  timePrecisionMode: adaptive

  # Binary data handling (default: bytes)
  # bytes: byte array
  # base64: base64 encoded
  # base64-url-safe: URL-safe base64
  # hex: hexadecimal
  binaryHandlingMode: bytes

  # HSTORE handling (default: json)
  # json: JSON string
  # map: MAP type
  hstoreHandlingMode: json

  # Interval handling (default: numeric)
  # numeric: microseconds
  # string: ISO 8601 string
  intervalHandlingMode: numeric

  # Money precision digits (default: 2)
  moneyFractionDigits: 2
```

### Column Masking and Transformation

Protect sensitive data:

```yaml
debeziumProperties:
  # Truncate columns to specified length
  columnTruncateToLengthChars:
    - "public\\.users\\.description"  # Truncate to property name length

  # Mask columns with asterisks
  columnMaskWithLengthChars:
    - "public\\.users\\.ssn"

  # Hash columns for pseudonymization
  columnMaskHash:
    SHA-256:                           # Hash algorithm
      randomSalt123:                   # Salt value
        - "public\\.users\\.email"
        - "public\\.orders\\.customer_name"

  # Propagate source column types
  columnPropagateSourceType:
    - ".*"  # All columns
```

### Performance Tuning

```yaml
debeziumProperties:
  # Batch size (default: 2048)
  maxBatchSize: 4096

  # Queue size (default: 8192)
  maxQueueSize: 16384

  # Queue size in bytes (default: 0 = unlimited)
  maxQueueSizeInBytes: 0

  # Poll interval in ms (default: 500)
  pollIntervalMs: 250

  # Status update interval in ms (default: 10000)
  statusUpdateIntervalMs: 5000

  # Heartbeat interval in ms (default: 0 = disabled)
  heartbeatIntervalMs: 30000

  # Slot retry settings
  slotMaxRetries: 6
  slotRetryDelayMs: 10000
```

### Error Handling

```yaml
debeziumProperties:
  # Event processing failure mode (default: fail)
  # fail: Stop on error
  # warn: Log and skip
  # skip: Silently skip
  eventProcessingFailureHandlingMode: fail

  # Max retries for retriable errors (default: -1 = unlimited)
  errorsMaxRetries: 10

  # Operations to skip
  skippedOperations:
    - "t"  # Skip truncate events
```

## Target Configuration

### SGCluster Target

```yaml
spec:
  target:
    type: SGCluster
    sgCluster:
      name: target-cluster
      database: mydb

      # Skip DDL import (let Debezium create tables)
      skipDdlImport: false

      # Roles to skip during DDL import (regex)
      ddlImportRoleSkipFilter: "replicator|authenticator"

      # Performance optimizations
      skipDropPrimaryKeys: false           # Drop PKs during snapshot
      skipDropIndexesAndConstraints: false # Drop indexes during snapshot
      skipRestoreIndexesAfterSnapshot: false

      # JDBC sink properties
      debeziumProperties:
        insertMode: upsert
        batchSize: 500
        deleteEnabled: true
        truncateEnabled: true
```

### SGCluster Target Debezium Properties

```yaml
debeziumProperties:
  # Insert mode (default: upsert)
  # insert: INSERT only
  # update: UPDATE only
  # upsert: INSERT or UPDATE based on PK
  insertMode: upsert

  # Primary key mode (default: record_key)
  # none: No primary key
  # record_key: From event key
  # record_value: From event value
  primaryKeyMode: record_key

  # Specific primary key fields
  primaryKeyFields:
    - id
    - tenant_id

  # Batch size for writes (default: 500)
  batchSize: 1000

  # Enable delete handling (default: true)
  deleteEnabled: true

  # Enable truncate handling (default: true)
  truncateEnabled: true

  # Schema evolution (default: basic)
  # none: No schema changes
  # basic: Add missing columns
  schemaEvolution: basic

  # Connection pool settings
  connectionPoolMin_size: 5
  connectionPoolMax_size: 32
  connectionPoolTimeout: 1800

  # Use reduction buffer (default: false)
  useReductionBuffer: true

  # Retry settings
  flushMaxRetries: 5
  flushRetryDelayMs: 1000
```

### CloudEvent Target

```yaml
spec:
  target:
    type: CloudEvent
    cloudEvent:
      format: json
      binding: http
      http:
        url: https://events.example.com/ingest
        headers:
          Authorization: "Bearer token"
          X-Custom-Header: "value"
        connectTimeout: "10s"
        readTimeout: "30s"
        retryLimit: 5
        retryBackoffDelay: 60
        skipHostnameVerification: false
```

### PgLambda Target

```yaml
spec:
  target:
    type: PgLambda
    pgLambda:
      scriptType: javascript
      script: |
        // event, request, response are available
        console.log(JSON.stringify(event.data));
        response.writeHead(200);
        response.end('OK');
      knative:
        labels:
          app: my-lambda
        annotations:
          autoscaling.knative.dev/minScale: "1"
        http:
          connectTimeout: "10s"
          readTimeout: "60s"
```

## Pod Configuration

```yaml
spec:
  pods:
    persistentVolume:
      size: 2Gi
      storageClass: fast-ssd

    # Resource requests/limits
    resources:
      requests:
        cpu: 500m
        memory: 512Mi
      limits:
        cpu: 2000m
        memory: 2Gi

    # Scheduling
    scheduling:
      nodeSelector:
        workload: streaming
      tolerations:
        - key: dedicated
          operator: Equal
          value: streaming
          effect: NoSchedule
      nodeAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          nodeSelectorTerms:
            - matchExpressions:
                - key: node-type
                  operator: In
                  values:
                    - streaming
```

## Metadata Configuration

Add custom labels and annotations:

```yaml
spec:
  metadata:
    labels:
      allResources:
        environment: production
      pods:
        team: data-platform
    annotations:
      pods:
        prometheus.io/scrape: "true"
      serviceAccount:
        eks.amazonaws.com/role-arn: "arn:aws:iam::..."
```

## Debezium Engine Properties

Global engine settings (separate from source connector):

```yaml
spec:
  debeziumEngineProperties:
    # Offset commit policy
    offsetCommitPolicy: PeriodicCommitOffsetPolicy

    # Offset flush interval (default: 60000)
    offsetFlushIntervalMs: 30000

    # Error retry settings
    errorsMaxRetries: -1
    errorsRetryDelayInitialMs: 300
    errorsRetryDelayMaxMs: 10000
```

## Signaling

Send signals to the stream via annotations:

```yaml
metadata:
  annotations:
    # Stop streaming gracefully
    debezium-signal.stackgres.io/tombstone: "{}"

    # Execute SQL on target (SGCluster target only)
    debezium-signal.stackgres.io/command: |
      {"command": "ANALYZE public.orders;"}
```

## Complete Example

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: production-migration
  labels:
    app: migration
spec:
  source:
    type: SGCluster
    sgCluster:
      name: legacy-cluster
      database: production
      includes:
        - "public\\.users"
        - "public\\.orders"
        - "public\\.products"
      debeziumProperties:
        snapshotMode: initial
        snapshotFetchSize: 20000
        maxBatchSize: 4096
        columnMaskHash:
          SHA-256:
            salt123:
            - "public\\.users\\.email"
  target:
    type: SGCluster
    sgCluster:
      name: new-cluster
      database: production
      debeziumProperties:
        insertMode: upsert
        batchSize: 1000
        useReductionBuffer: true
  maxRetries: -1
  pods:
    persistentVolume:
      size: 5Gi
      storageClass: fast-ssd
    resources:
      requests:
        cpu: 1000m
        memory: 1Gi
      limits:
        cpu: 4000m
        memory: 4Gi
  metadata:
    labels:
      pods:
        app: migration
    annotations:
      pods:
        prometheus.io/scrape: "true"
```

## Next Steps

- [Monitoring Streams]({{% relref "04-administration-guide/17-cdc-streaming/03-monitoring-streams" %}}) - Track stream progress and health

