---
title: Replication
weight: 10
url: /administration/replication
aliases: [ /tutorial/admin/replication ]
description: This page contains details about how to create different cluster architecture topologies by using the replication features.
---

StackGres supports all Postgres and Patroni features to set the different replication options that come with these technologies. Indeed, StackGres doesn't use any custom replication mechanism or protocol, it fully relies upon the official Postgres replication development. Furthermore, StackGres relies upon the Patroni HA development, therefore, failover, switchover, and replication should work as any other Postgres cluster managed by Patroni.

## Available Replication Modes

An SGCluster supports the following replication modes:

| Mode | Description |
|------|-------------|
| `async` | Default mode. Asynchronous replication where some committed transactions may be lost on failover. |
| `sync` | Synchronous replication to a specified number of replicas (`syncInstances`). |
| `strict-sync` | Strict synchronous replication that blocks writes if no synchronous replica is available. |
| `sync-all` | Synchronous replication to all replicas. |
| `strict-sync-all` | Strict synchronous replication to all replicas. |

To configure synchronous replication with a specific number of sync replicas:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  replication:
    mode: sync
    syncInstances: 1
```

The `sync-all` and `strict-sync-all` modes do not require the `syncInstances` field since all replicas perform synchronous replication.

{{% children style="li" depth="1" description="true" %}}