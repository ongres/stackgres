---
title: Manually scaling a cluster
weight: 1
url: /cookbook/operating-clusters/manual-scaling
description: Add or remove instances of a running SGCluster by editing the instance count.
showToc: true
---

## What it does

Changes the size of a running cluster by editing `spec.instances`. Scaling up adds
replicas; scaling down removes them. The primary is never the instance removed.

> Letting StackGres add and remove replicas automatically (`spec.autoscaling`, backed by
> KEDA and the VPA operator) is covered in a separate recipe.

## When to use it

- A read-heavy workload needs more replicas to spread read-only queries.
- You over- or under-provisioned and want to right-size the cluster.
- You need a predictable, explicit instance count rather than load-driven scaling.

## How to do it

Edit the cluster and set the desired number of `instances`:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  # ...rest unchanged
```

```bash
kubectl apply -f cluster.yaml
```

Or patch the count in place without editing the full manifest:

```bash
kubectl patch sgcluster cluster -n my-cluster --type merge -p '{"spec":{"instances":3}}'
```

An odd number is recommended so Patroni always has a clear majority to elect a primary.

## How it works

Updating `spec.instances` updates the underlying StatefulSet. The operator then converges
the running cluster:

- **Scaling up** schedules new Pods. Each new replica starts as a standby and is cloned
  from the primary via streaming replication (and WAL shipping if backups are configured),
  then begins serving read-only traffic through the `-replicas` Service once it has caught
  up.
- **Scaling down** removes the highest-ordinal replicas first, draining them gracefully.
  Read traffic is rebalanced across the remaining replicas. The primary is kept.

## What to expect

- New replicas are not immediately ready — clone and catch-up time scales with database
  size. Watch them join:

  ```bash
  kubectl get pods -n my-cluster -w
  ```

- Scaling down is graceful; existing read connections to a removed replica are closed and
  clients reconnect through the `-replicas` Service.

## Pitfalls

- **Scheduling limits real scale.** Under the `production` profile, Pod anti-affinity needs
  a distinct node per Pod. If you scale beyond the number of available nodes, the extra
  replicas stay `Pending`.
- **Don't scale below your HA needs.** Dropping to a single instance leaves no standby to
  fail over to. Keep `instances` ≥ 2 (3+ recommended) if you rely on automatic failover or
  synchronous replication — see
  [High availability and replication]({{% relref "05-cookbook/02-operating-clusters/02-high-availability" %}}).
- **Update locks.** If an [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) or
  [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) operation holds the cluster lock,
  the scale update is rejected until that operation finishes. Retry afterwards.
