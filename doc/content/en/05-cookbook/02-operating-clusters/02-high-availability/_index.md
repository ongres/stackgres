---
title: High availability and replication
weight: 2
url: /cookbook/operating-clusters/high-availability
description: Change the replication mode, synchronous standbys, and HA roles of a running SGCluster.
showToc: true
---

## What it does

Controls how the cluster replicates data between the primary and its standbys, and how
roles are assigned for high availability. You tune this through `spec.replication`:
the replication `mode`, the number of synchronous standbys (`syncInstances`), the HA
`role`, and optional replication `groups` for mixed-role topologies.

## When to use it

- You need stronger durability guarantees than the default asynchronous replication.
- You want some replicas dedicated to read-only traffic and others reserved purely for
  failover.
- You are tuning the trade-off between commit latency and the risk of losing committed
  transactions on failover.

## How to do it

### Replication mode

By default replication is asynchronous. To require commits to reach standbys synchronously:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  replication:
    mode: sync
    syncInstances: 1
  # ...rest unchanged
```

`mode` accepts:

- `async` *(default)* — the cluster may lose some committed transactions on failover.
- `sync` — a standby is not promoted unless it is known to hold all committed transactions;
  if no suitable synchronous standby is available, commits wait.
- `strict-sync` — like `sync` but never falls back to asynchronous; the primary refuses to
  commit rather than risk data loss.
- `sync-all` / `strict-sync-all` — apply the synchronous requirement against all standbys.

`syncInstances` is the number of synchronous standbys (default `1`, and it must be less
than the total number of instances). It applies to the synchronous modes.

### HA roles and replication groups

The implicit main group has a `role` of `ha-read` by default (eligible for failover **and**
exposed through the `-replicas` read-only Service). Set it to `ha` to keep instances
failover-eligible but **not** served read-only traffic:

```yaml
spec:
  instances: 5
  replication:
    role: ha-read
    groups:
    - name: reporting
      role: readonly
      instances: 2
```

Replication `groups` let a subset of instances take a different role (for example a
`readonly` group that never becomes primary). The implicit main group holds the total
number of instances minus the sum of all explicit groups.

## How it works

StackGres configures Patroni accordingly. For synchronous modes it sets PostgreSQL's
`synchronous_standby_names` / `synchronous_commit` so the primary waits for the required
standbys to acknowledge each commit. Patroni elects the primary among instances whose role
is `ha` or `ha-read`; only `ha-read` instances of the main group are published via the
`-replicas` Service. Changing these fields is reconciled on the live cluster — no
re-creation needed.

## What to expect

- Switching to a synchronous mode increases commit latency, because the primary waits for
  standby acknowledgement before returning success.
- `strict-sync` favors durability over availability: if there is no eligible synchronous
  standby, writes block. Make sure you have enough healthy standbys before enabling it.
- Role/group changes adjust which Pods are reachable through the `-replicas` Service and
  which are candidates for promotion; existing connections are not dropped abruptly.

## Pitfalls

- **`syncInstances` must leave a quorum.** It must be less than `instances`. Requesting
  more synchronous standbys than you have healthy replicas will block commits.
- **Synchronous ≠ multi-node durability in every case.** Under certain failure scenarios
  `sync` does not guarantee zero data loss (see the SGCluster reference notes on
  `replication.mode`). Use `strict-sync` if you must never lose a committed transaction —
  accepting that writes may stall.
- **Too few instances for HA.** A single-instance cluster has no standby to fail over to;
  set `instances` ≥ 2 (3+ recommended) before relying on synchronous replication or
  automatic failover.
- **Group instance math.** The sum of instances across explicit `groups` must not exceed
  `spec.instances`; the remainder forms the implicit main group. Mis-sized groups are
  rejected by the validating webhook.
