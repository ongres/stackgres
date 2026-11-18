---
title: Replication Modes
weight: 1
url: /administration/replication/modes
description: This section lists the available replication modes
showToc: true
---

Stackgres handles the proper settings in the Postgres and Patroni configuration files. It offers some options to set different replication modes, as follows:

{{% children style="li" depth="1" description="true" %}}

Let's dive into each of these options.

## Understanding Replication Trade-offs

When choosing a replication mode, consider the trade-offs between data durability and availability:

### Asynchronous Replication

When in asynchronous mode, the cluster is allowed to lose some committed transactions. When the primary server fails or becomes unavailable, a sufficiently healthy standby will automatically be promoted to primary. Any transactions that have not been replicated to that standby remain in a "forked timeline" on the primary and are effectively unrecoverable (the data is still there, but recovering it requires manual effort by data recovery specialists).

> **Note:** The amount of data loss is proportional to the replication delay at the time of failover.

### Synchronous Replication

An SGCluster can be configured to use synchronous replication, allowing it to confirm that all changes made by a transaction have been transferred to one or more synchronous standby servers.

When in synchronous mode, a standby will not be promoted unless it is certain that the standby contains all transactions that may have returned a successful commit status to clients (clients can change the behavior per transaction using PostgreSQL's `synchronous_commit` setting). This means the system may be unavailable for writes even though some servers are available.

**Important characteristics:**
- Synchronous mode does NOT guarantee multi-node durability under all circumstances
- When no suitable standby is available, the primary server will still accept writes but does not guarantee their replication
- When the primary fails in this mode, no standby will be promoted until the original primary comes back
- This behavior makes synchronous mode usable with 2-node clusters
- When a standby crashes, commits will block until the primary switches to standalone mode

### Strict Synchronous Replication

When it is absolutely necessary to guarantee that each write is stored durably on at least two nodes, use strict synchronous mode. This mode prevents synchronous replication from being switched off on the primary when no synchronous standby candidates are available.

**Trade-off:** The primary will not be available for writes (unless the Postgres transaction explicitly turns off `synchronous_mode`), blocking all client write requests until at least one synchronous replica comes up.

> **Warning:** Because of the way synchronous replication is implemented in PostgreSQL, it is still possible to lose transactions even when using strict synchronous mode. If the PostgreSQL backend is cancelled while waiting to acknowledge replication (due to client timeout or backend failure), transaction changes become visible to other backends before being replicated.