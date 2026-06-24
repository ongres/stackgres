---
title: Minor version upgrade
weight: 2
url: /cookbook/running-operations/minor-version-upgrade
description: Upgrade a cluster to a newer minor Postgres version.
showToc: true
---

## What it does

Upgrades the Postgres binaries on a running cluster to a newer patch release within the
same major version — for example from 16.2 to 16.4. The operation is expressed as an
[SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) resource and reconciled by the
operator. It updates each instance in turn, restarting Pods in a controlled sequence so
that the cluster remains available throughout.

## When to use it

- A new Postgres minor release ships a security fix or bug fix you need to apply.
- You want the operator to coordinate the rolling restart rather than doing it by hand.
- You need to target a specific patch version rather than accepting whatever the current
  image resolves to.

## How to do it

First, confirm that the target version is available and shares the same major version as
the cluster's current `spec.postgres.version`.

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: minor-version-upgrade
spec:
  # Name of the cluster to upgrade.
  sgCluster: cluster
  # The operation type.
  op: minorVersionUpgrade
  minorVersionUpgrade:
    # Target patch version. Must share the same major version as the SGCluster.
    # Omit to let the operator choose the latest available minor version.
    postgresVersion: "16.4"
    # Rolling restart strategy:
    #   InPlace       – restart each instance with no extra resources.
    #   ReducedImpact – spin up a temporary updated replica first, then
    #                   rotate the rest; minimises read/write disruption.
    method: ReducedImpact
```

```bash
kubectl apply -f minor-version-upgrade.yaml
```

Watch the operation progress:

```bash
kubectl get sgdbops -n my-cluster minor-version-upgrade -w
```

Check completion status:

```bash
kubectl describe sgdbops -n my-cluster minor-version-upgrade
```

## How it works

The operator creates a job Pod that drives the upgrade sequence. For each cluster
instance, in replica-first order:

1. The Pod's image is updated to match the requested `postgresVersion`.
2. The Pod is restarted. Patroni keeps the primary available during replica restarts.
3. Once all replicas are running the new version, the primary is restarted last (triggering
   a Patroni-coordinated failover first, so a replica becomes primary and there is no
   write downtime).

With `method: ReducedImpact` the operator adds a temporary extra replica running the
target version before it begins rotating the existing instances, further reducing the
window where read capacity is degraded.

The `SGCluster`'s `spec.postgres.version` is updated by the operator on successful
completion to match the new patch version.

## What to expect

- The SGDbOps transitions through conditions `Running` → `Completed` (or `Failed`).
- Total elapsed time depends on how quickly each Pod reaches `Running` after restart; with
  `ReducedImpact` it takes slightly longer overall but with less read disruption.
- Each replica restart causes a brief drop in read replica capacity; the primary restart
  causes a short write interruption (typically sub-second with Patroni failover).
- After the operation, inspect the cluster to confirm the new version:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o jsonpath='{.spec.postgres.version}'
  ```

## Pitfalls

- **Same major version required.** `postgresVersion` must share the same major version as
  the cluster (e.g. `16.x` to `16.y`). Attempting a cross-major upgrade with this
  operation will be rejected; use the `majorVersionUpgrade` op instead.
- **Rolling restarts.** Every instance is restarted. Applications should be ready to
  reconnect. Use a connection pooler (PgBouncer via SGPoolingConfig) to absorb reconnects
  transparently.
- **`ReducedImpact` needs spare capacity.** The method temporarily increases the cluster
  size by one Pod. Ensure the namespace has enough node resources to schedule the extra
  instance before choosing this method.
- **Version availability.** The operator can only install versions included in the
  StackGres image set for the current operator release. Requesting a version not present
  in the registry will fail during the Pod image pull.
- **SGDbOps is single-use.** Once an SGDbOps completes (or fails), create a new resource
  to retry or run the operation again; editing a completed resource has no effect.
