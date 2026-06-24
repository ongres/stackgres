---
title: Promoting or re-pointing a standby
weight: 29
url: /cookbook/operating-clusters/promote-standby
description: Promote a standby cluster to primary, or change its replication source.
showToc: true
---

## What it does

Modifies `spec.replicateFrom` on a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to either change its
replication source (re-point) or remove the section entirely (promote). The operator
reconciles the change in place without rebuilding the cluster.

## When to use it

- You are cutting over from a source instance (external or another SGCluster) and need
  to promote the standby to a writable primary.
- The replication source has moved to a new host or a different SGCluster and you need
  to re-point without rebuilding.
- You are completing a migration started with the *standby from external Postgres* or
  *standby from another SGCluster* creation recipe.

## How to do it

### Re-pointing to a different source

Update `spec.replicateFrom.instance.sgCluster` to the new source cluster name. The
operator reconnects the standby to the new source during the next reconciliation loop.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 2
  postgres:
    version: "16"
  pods:
    persistentVolume:
      size: 20Gi
  replicateFrom:
    instance:
      sgCluster: new-source-cluster   # changed from the previous source
```

```bash
kubectl apply -f cluster.yaml
```

Or patch it directly:

```bash
kubectl patch sgcluster -n my-cluster cluster \
  --type=merge \
  -p '{"spec":{"replicateFrom":{"instance":{"sgCluster":"new-source-cluster"}}}}'
```

### Promoting to a standalone primary

Remove `spec.replicateFrom` entirely. The operator detects the absence of the field and
tells Patroni to promote the standby leader to a normal primary.

```bash
kubectl patch sgcluster -n my-cluster cluster \
  --type=json \
  -p='[{"op":"remove","path":"/spec/replicateFrom"}]'
```

After this, both the primary Service (`cluster`) and the replica Service
(`cluster-replicas`) become active within the cluster itself.

## How it works

`spec.replicateFrom` is marked as `updatable` in the SGCluster spec. When the operator
detects a change it updates the Patroni configuration on each Pod via a rolling
reconciliation. Patroni then negotiates the new replication topology. When the field is
removed, the operator lifts the standby constraint and Patroni promotes the current
standby leader to primary. The remaining instances re-attach as streaming replicas of the
newly promoted primary.

## What to expect

- Re-pointing: existing data already replicated is retained; Patroni re-establishes
  streaming from the new source. Monitor replication lag with:

  ```bash
  kubectl exec -n my-cluster -it cluster-0 -c patroni -- \
    patronictl -c /etc/patroni/postgres.yml list
  ```

- Promotion: the cluster transitions from read-only to read-write. The primary Service
  immediately starts routing write traffic to the promoted instance. Verify the cluster
  is healthy:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o jsonpath='{.status.conditions}'
  ```

- No Pod restarts or PVC movements are required for either operation under normal
  circumstances.

## Pitfalls

- **Stop writes to the old source before promoting.** Removing `replicateFrom` promotes
  the standby to a primary while the old source may still be accepting writes. If both
  sides accept writes simultaneously, a split-brain situation arises and data diverges
  with no automatic resolution. Always stop or redirect writes on the old source first.
- **Re-pointing does not replay missed WAL.** If the new source is ahead of or behind the
  standby's current WAL position, Patroni may need to re-clone. Monitor Pod events and
  Patroni logs for `replica is too far behind` messages.
- **`instance.sgCluster` and `instance.external` are mutually exclusive.** You cannot
  set both sub-fields at the same time; the validating webhook rejects the combination.
- **Namespace scope for sgCluster references.** `instance.sgCluster` refers to a cluster
  in the same namespace. Cross-namespace replication requires using
  `instance.external` with the cluster's Service hostname instead.
