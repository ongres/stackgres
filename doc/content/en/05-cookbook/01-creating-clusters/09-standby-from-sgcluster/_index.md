---
title: Bootstrapping as a standby of another SGCluster
weight: 9
url: /cookbook/creating-clusters/standby-from-sgcluster
description: Create a read-only cluster that continuously replicates from another SGCluster.
showToc: true
---

## What it does

Creates a new [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) that acts as a
physical standby of an existing SGCluster running in the same Kubernetes namespace. The
standby cluster streams WAL continuously from the source and stays read-only until you
explicitly promote it. You configure this through `spec.replicateFrom.instance.sgCluster`.

## When to use it

- You need a live, readable replica of a production cluster for reporting or testing, kept
  in sync by streaming replication.
- You are preparing a blue/green cutover: bring up a standby, verify it, then promote when
  ready.
- You want a warm standby in a separate namespace or with different sizing, ready to take
  over if the primary cluster becomes unavailable.

## How to do it

Create the source namespace and cluster if they are not already running (see the *A simple
cluster* recipe for details). Then create a second namespace and apply the standby cluster:

```bash
kubectl create namespace my-cluster
```

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster        # must be the same namespace as the source cluster
  name: cluster-standby
spec:
  instances: 2
  postgres:
    version: "16"              # must be compatible with the source cluster version
  pods:
    persistentVolume:
      size: 10Gi
  replicateFrom:
    instance:
      sgCluster: cluster       # name of the source SGCluster in the same namespace
```

```bash
kubectl apply -f cluster-standby.yaml
```

The single required addition to a normal SGCluster manifest is `spec.replicateFrom.instance.sgCluster`,
set to the name of the source cluster. Everything else — sizing, storage class, profiles —
can be specified independently.

## How it works

When the operator reconciles the new SGCluster it detects `replicateFrom.instance.sgCluster`
and bootstraps the standby leader by connecting directly to the primary of the source
cluster via streaming replication. Patroni is configured so that the standby leader
continuously replays WAL received from the source; any additional instances in the standby
cluster replicate in turn from the standby leader, forming a cascaded replication chain.

The standby cluster exposes its own pair of Services (primary-endpoint and `-replicas`)
but all connections are read-only for as long as `replicateFrom` is present. The operator
keeps this configuration reconciled: if you change `sgCluster` to point at a different
source, StackGres reconfigures streaming replication accordingly without recreating the
Pods.

Removing `spec.replicateFrom` entirely signals promotion: the standby leader is converted
into a normal primary and the cluster begins accepting writes.

## What to expect

- Watch the standby Pods come up and begin replicating:

  ```bash
  kubectl get pods -n my-cluster -w
  ```

- Inspect the cluster status to confirm the standby role:

  ```bash
  kubectl get sgcluster -n my-cluster cluster-standby -o yaml
  ```

- All connections through the standby cluster's Services are read-only. Write attempts
  return a `read-only transaction` error from PostgreSQL.

## Pitfalls

- **The standby is permanently read-only.** No writes are accepted until you promote it
  by removing `spec.replicateFrom` (see the *Promoting a standby* recipe). Plan your
  cutover before directing application traffic.
- **Source and target must be in the same namespace.** `replicateFrom.instance.sgCluster`
  resolves by name within the same Kubernetes namespace. Cross-namespace replication
  requires the `external` instance form instead.
- **Postgres version compatibility.** The standby cluster's `postgres.version` must be
  identical to, or a minor-version compatible with, the source cluster. A major-version
  mismatch causes replication to fail at bootstrap. Verify both clusters run the same
  major version before applying.
- **`sgCluster` cannot be combined with `external` or `storage`.** These are mutually
  exclusive sources within `spec.replicateFrom.instance`. The validating webhook rejects
  manifests that set both.
- **Changing `sgCluster` on a live standby.** Pointing the standby at a different source
  cluster is allowed but triggers a full re-bootstrap from the new source, discarding
  any WAL not yet promoted. Only do this intentionally.
