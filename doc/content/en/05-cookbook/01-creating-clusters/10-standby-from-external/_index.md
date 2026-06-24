---
title: Bootstrapping as a standby of an external Postgres
weight: 10
url: /cookbook/creating-clusters/standby-from-external
description: Replicate into StackGres from a Postgres instance running outside the cluster.
showToc: true
---

## What it does

Configures a new [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to stream
replication from an external (non-StackGres) PostgreSQL instance via
`spec.replicateFrom.instance.external`. The cluster starts as a read-only standby and
stays in sync with the source. When you are ready to cut over, removing the
`replicateFrom` section promotes the standby leader to a standalone primary.

## When to use it

- You are migrating an existing PostgreSQL instance into Kubernetes and need zero
  (or near-zero) downtime cutover.
- You want to shadow production traffic on a StackGres cluster before switching over.
- You need to test StackGres configuration and extensions against a live replica of
  a production database running outside the cluster.

## How to do it

### 1. Store the external credentials in a Secret

The operator reads all credential references from a Kubernetes Secret. Create one that
holds the four required keys:

```bash
kubectl create namespace my-cluster

kubectl create secret generic pg-origin-secret \
  --namespace my-cluster \
  --from-literal=superuser-username=postgres \
  --from-literal=superuser-password=<superuser-password> \
  --from-literal=replication-username=replicator \
  --from-literal=replication-password=<replication-password> \
  --from-literal=authenticator-username=authenticator \
  --from-literal=authenticator-password=<authenticator-password>
```

### 2. Create the standby SGCluster

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 2
  postgres:
    version: "16"           # must match the external instance's major version
  pods:
    persistentVolume:
      size: 20Gi
  replicateFrom:
    instance:
      external:
        host: 192.0.2.10    # hostname or IP reachable from the cluster nodes
        port: 5432
    users:
      superuser:
        username:
          name: pg-origin-secret
          key: superuser-username
        password:
          name: pg-origin-secret
          key: superuser-password
      replication:
        username:
          name: pg-origin-secret
          key: replication-username
        password:
          name: pg-origin-secret
          key: replication-password
      authenticator:
        username:
          name: pg-origin-secret
          key: authenticator-username
        password:
          name: pg-origin-secret
          key: authenticator-password
```

```bash
kubectl apply -f cluster.yaml
```

### 3. Promote to a standalone cluster

Once you are ready to cut over (external instance stopped or redirected), remove
`spec.replicateFrom` from the SGCluster. The operator reconciles the change and
promotes the standby leader to a normal primary:

```bash
kubectl patch sgcluster -n my-cluster cluster \
  --type=json \
  -p='[{"op":"remove","path":"/spec/replicateFrom"}]'
```

## How it works

The operator passes `replicateFrom.instance.external` to Patroni as the upstream
source. Patroni bootstraps each Pod's PGDATA by cloning from the external instance
using the `replication` user, then keeps it current via streaming replication.
The `superuser` credentials allow Patroni to connect for management queries, and the
`authenticator` credentials are used by PgBouncer for connection pooling. While
`replicateFrom` is present, the standby leader cannot accept writes. Removing the
section triggers an operator reconciliation that lifts the standby constraint and
lets Patroni promote.

## What to expect

- Pods enter a `ClusterWaitingToBeReady` phase while the initial base backup clone
  is running. For large databases this can take several minutes.
- During steady-state replication, check lag with:

  ```bash
  kubectl exec -n my-cluster -it cluster-0 -c patroni -- \
    patronictl -c /etc/patroni/postgres.yml list
  ```

- After promotion the cluster behaves exactly like one created with the *simple
  cluster* recipe; both the primary Service and the `-replicas` Service become active.

## Pitfalls

- **Network reachability is required.** The external instance must be reachable on
  the specified `host` and `port` from every Pod in the cluster. A wrong address or
  a firewall rule silently blocks the clone and the Pods never become ready.
- **Credentials must match exactly.** The replication user must exist on the external
  instance with the `REPLICATION` privilege and the password stored in the Secret must
  be correct. A mismatch causes authentication failures during the initial clone.
- **Postgres major version must match.** Streaming replication is version-specific.
  Set `spec.postgres.version` to the same major version as the external source;
  mismatches are rejected during the bootstrap phase.
- **Promote only after the external instance is idle or redirected.** Promoting while
  the source is still accepting writes risks split-brain. Stop writes to the external
  instance (or re-point its clients) before removing `replicateFrom`.
- **`replicateFrom` is updatable but cannot be combined with `sgCluster`.** You can
  change the external host or credentials on a running cluster for failover scenarios,
  but `instance.external` and `instance.sgCluster` are mutually exclusive.
