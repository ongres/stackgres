---
title: CDC Streaming
weight: 17
url: /features/cdc-streaming
description: Change Data Capture (CDC) streaming with Debezium
---

Change Data Capture (CDC) is a powerful pattern for tracking and streaming database changes in real-time.
StackGres integrates [Debezium Engine](https://debezium.io/documentation/reference/stable/development/engine.html) to provide CDC capabilities through the `SGStream` custom resource.

With SGStream, you can capture changes from PostgreSQL databases and stream them to various targets, enabling use cases such as:

- **Data Migration**: Stream data from one PostgreSQL cluster to another, enabling zero-downtime migrations
- **Event-Driven Architectures**: Emit database changes as CloudEvents to trigger downstream services
- **Real-Time Analytics**: Stream changes to analytics platforms for real-time processing
- **Microservices Integration**: Keep microservices synchronized with database state changes

## How It Works

SGStream performs two distinct operations to capture and stream database changes:

1. **Snapshotting**: Captures the current content of the data source at a specific point in time, streaming it as if the records were changes. This provides a complete view of the database state as a stream of events.

2. **Streaming**: Captures changes happening in real-time using PostgreSQL logical replication, continuously streaming INSERT, UPDATE, and DELETE events to the configured target.

The CDC process uses PostgreSQL's logical decoding with the `pgoutput` plugin, creating a replication slot and publication to track changes.

## Supported Sources

SGStream can capture changes from:

- **SGCluster**: Any StackGres PostgreSQL cluster in the same namespace
- **Postgres**: Any external PostgreSQL instance with logical replication enabled

## Supported Targets

Captured changes can be streamed to:

- **SGCluster**: Another StackGres cluster for data migration or replication
- **CloudEvent**: HTTP endpoints accepting CloudEvents format for event-driven architectures
- **PgLambda**: Serverless functions via Knative for custom event processing

## Custom Signaling

SGStream extends Debezium's functionality with a custom signaling channel that allows you to send signals via Kubernetes annotations on the SGStream resource:

```yaml
metadata:
  annotations:
    debezium-signal.stackgres.io/tombstone: "{}"
```

Available custom signals include:

- **tombstone**: Gracefully stop streaming and clean up the logical replication slot
- **command**: Execute SQL commands on the target database (SGCluster target only)

## Alpha Feature

SGStream is currently an **alpha feature** (API version `v1alpha1`). While fully functional, the API may change in future releases. It is recommended to test thoroughly in non-production environments before deploying to production.

Have a look at the [CDC Streaming Guide]({{% relref "04-administration-guide/17-cdc-streaming" %}}) to learn how to create and configure streams, and the [SGStream CRD Reference]({{% relref "06-crd-reference/15-sgstream" %}}) for the complete specification.
