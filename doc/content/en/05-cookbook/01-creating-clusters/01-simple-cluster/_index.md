---
title: A simple cluster
weight: 1
url: /cookbook/creating-clusters/simple-cluster
description: Create a minimal StackGres Postgres cluster with a single custom resource.
showToc: true
---

## What it does

Creates a working high-availability Postgres cluster from a single
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) custom resource. This is the
base recipe that every other *Creating clusters* recipe builds upon.

## When to use it

- You want a cluster up and running with sensible defaults and the least possible YAML.
- You are exploring StackGres, or scripting a reproducible starting point that you will
  customize later.

## How to do it

A namespace to hold the cluster, and the cluster itself:

```bash
kubectl create namespace my-cluster
```

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 1
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 10Gi
```

```bash
kubectl apply -f cluster.yaml
```

Only three things are required: how many `instances` to run, the `postgres.version`, and
the `pods.persistentVolume.size` for each instance. The operator fills in defaults for
everything else — Postgres configuration, connection pooling, an instance profile, services,
and so on.

For more than one instance, raise `instances` (an odd number is recommended so a clear
majority can elect a primary):

```yaml
spec:
  instances: 3
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 10Gi
```

## How it works

When you apply the SGCluster, the operator:

1. Materializes defaults — for example, `version: latest` is resolved to the actual latest
   supported Postgres version, and the default `StorageClass` is used when none is given.
2. Creates a StatefulSet of `instances` Pods, each with a `patroni` container (running
   Postgres), a `cluster-controller` sidecar, and optional sidecars (pooling, metrics
   exporter, `postgres-util`).
3. Lets [Patroni](https://patroni.readthedocs.io/en/latest/) elect one Pod as the primary;
   the rest become replicas kept in sync via PostgreSQL streaming replication.
4. Creates two Services: one named after the cluster pointing to the primary (read/write),
   and a `-replicas` Service load-balancing read-only traffic across replicas.

See [Creating a Cluster]({{% relref "04-administration-guide/02-cluster-creation" %}}) for a
deeper look at the Pod architecture and the available customizations.

## What to expect

- Watch the Pods come up:

  ```bash
  kubectl get pods -n my-cluster -w
  ```

- Inspect cluster status, including which instance is the primary:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o yaml
  ```

- The generated superuser credentials are stored in a Kubernetes Secret named after the
  cluster. See [Connecting to the Cluster]({{% relref "04-administration-guide/03-connecting-to-the-cluster" %}})
  for how to retrieve them and connect.

## Pitfalls

- **Anti-affinity in the production profile.** The default `production` profile spreads Pods
  across different nodes via Pod anti-affinity. On a single-node cluster (kind, minikube, a
  one-node cloud cluster) replicas stay `Pending` because they cannot be scheduled. Use
  `spec.profile: testing` (or `development`) for those environments.
- **No default StorageClass.** If the cluster has no default `StorageClass`, the
  PersistentVolumeClaims stay unbound and Pods never start. Set
  `spec.pods.persistentVolume.storageClass` explicitly — but note this is a
  [creation-only]({{% relref "05-cookbook/01-creating-clusters" %}}) choice.
- **Resource requirements.** The `production` profile enforces resource requests/limits;
  if the nodes cannot satisfy them the Pods stay `Pending`. Size an
  [SGInstanceProfile]({{% relref "06-crd-reference/02-sginstanceprofile" %}}) to fit your
  nodes, or relax requirements with a non-production profile.
