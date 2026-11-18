---
title: Apache ShardingSphere sharding technology
weight: 2
url: /administration/sharded-cluster/shardingsphere
description: Details about ShardingSphere sharding technology.
---

## Overview

Apache ShardingSphere is an ecosystem to transform any database into a distributed database system, and enhance it with sharding, elastic scaling, encryption features and more.

StackGres uses [ShardingSphere Proxy](https://shardingsphere.apache.org/document/current/en/quick-start/shardingsphere-proxy-quick-start/) as the coordinator entry point to distribute SQL traffic among the PostgreSQL shards. Unlike Citus, which uses a PostgreSQL extension, ShardingSphere operates as an external middleware proxy that sits between the application and the database shards.

This implementation requires the [ShardingSphere Operator](https://shardingsphere.apache.org/oncloud/current/en/user-manual/cn-sn-operator/) to be installed in the Kubernetes cluster. StackGres will create a ComputeNode resource that the ShardingSphere Operator manages.

## Mode Configuration

ShardingSphere supports two operating modes:

### Standalone Mode

In Standalone mode, ShardingSphere Proxy runs as a single instance. This mode is suitable for development and testing environments.

> **Note:** Standalone mode cannot have more than 1 coordinator instance.

### Cluster Mode

In Cluster mode, ShardingSphere Proxy runs with a distributed governance center for metadata persistence and coordination. This mode is required for production environments where high availability is needed.

Cluster mode requires a repository for storing metadata. Supported repository types:

- **ZooKeeper**: Recommended for production deployments
- **Etcd**: Alternative distributed key-value store

## Creating a basic ShardingSphere Sharded Cluster

First, ensure the ShardingSphere Operator is installed in your Kubernetes cluster.

Then create the SGShardedCluster resource:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: cluster
spec:
  type: shardingsphere
  database: mydatabase
  postgres:
    version: '15'
  coordinator:
    instances: 2
    configurations:
      shardingSphere:
        mode:
          type: Cluster
          repository:
            type: ZooKeeper
    pods:
      persistentVolume:
        size: '10Gi'
  shards:
    clusters: 4
    instancesPerCluster: 2
    pods:
      persistentVolume:
        size: '10Gi'
```

This configuration will create a ShardingSphere Proxy coordinator in Cluster mode using ZooKeeper for metadata persistence, and 4 PostgreSQL shards with 2 Pods each.

## Authority Configuration

ShardingSphere allows configuring users and privileges for the proxy layer. This is configured through the `authority` section of the ShardingSphere configuration:

```yaml
configurations:
  shardingSphere:
    authority:
      users:
        - user: admin
          password: secret
      privilege:
        type: ALL_PERMITTED
```

## Key Differences from Citus

| Feature | ShardingSphere | Citus |
|---------|---------------|-------|
| **Coordinator** | External middleware proxy (ComputeNode) | PostgreSQL extension |
| **Query routing** | ShardingSphere Proxy handles SQL parsing and routing | Citus distributed query engine |
| **Dependencies** | Requires ShardingSphere Operator | No external dependencies |
| **Connection type** | Application connects to ShardingSphere Proxy | Application connects directly to PostgreSQL |
