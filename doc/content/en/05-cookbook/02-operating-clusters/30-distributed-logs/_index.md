---
title: Integrating distributed logs
weight: 30
url: /cookbook/operating-clusters/distributed-logs
description: Send a cluster logs to a centralized SGDistributedLogs instance.
showToc: true
---

## What it does

Adds a `distributedLogs` entry to `spec` of a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to point it at an
[SGDistributedLogs]({{% relref "06-crd-reference/07-sgdistributedlogs" %}}) instance.
Once set, all pods in the cluster ship Postgres, Patroni, and PgBouncer log lines to that
central Postgres database. Logs are then queryable via SQL or browsable from the Web Console.
The operator reconciles the change in place; no cluster restart is required.

## When to use it

- You want centralized, queryable log storage across one or more clusters.
- You need to audit or troubleshoot across multiple databases from a single location.
- You want to view structured logs directly from the StackGres Web Console.

## How to do it

### 1. Ensure an SGDistributedLogs instance exists

The cluster must reference an
[SGDistributedLogs]({{% relref "06-crd-reference/07-sgdistributedlogs" %}}) that is already
present in the same namespace. Create one first if it does not exist:

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  namespace: my-cluster
  name: distributedlogs
spec:
  persistentVolume:
    size: 20Gi        # storage for the log database
```

```bash
kubectl apply -f distributedlogs.yaml
```

### 2. Patch the SGCluster

Add `spec.distributedLogs` to the existing cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  postgres:
    version: "16"
  pods:
    persistentVolume:
      size: 10Gi
  distributedLogs:
    sgDistributedLogs: distributedlogs   # name of the SGDistributedLogs in this namespace
    retention: "7 days"                  # keep entries for 7 days (optional, default ~7 days)
```

```bash
kubectl apply -f cluster.yaml
```

The `retention` field accepts the format `<integer> (minutes|hours|days|months)`.

### 3. Verify the integration is active

```bash
kubectl get sgcluster -n my-cluster cluster \
  -o jsonpath='{.spec.distributedLogs.sgDistributedLogs}'
```

Check that the distributed logs cluster is running and receiving data:

```bash
kubectl get sgdistributedlogs -n my-cluster
```

## How it works

When `spec.distributedLogs.sgDistributedLogs` is set, the operator configures a Fluentd
sidecar in each cluster pod. The sidecar tails Postgres, Patroni, and PgBouncer log files,
parses them into structured records, and forwards them to the Postgres database managed by
the referenced `SGDistributedLogs` instance. Each cluster gets its own table partition
inside that database.

The `retention` window controls how long log entries are kept. Entries older than twice the
specified window are removed by a background process. If no retention is set, the operator
defaults to a 7-day window.

## What to expect

- Log forwarding begins within seconds of the operator reconciling the patch.
- Logs are viewable from the StackGres Web Console under the cluster's **Logs** tab.
- To query logs directly, connect to the `SGDistributedLogs` Postgres database and query
  the relevant partition table.
- Watch the distributed logs pod to confirm it is healthy:

  ```bash
  kubectl get pods -n my-cluster -l app=StackGresDistributedLogs
  ```

## Pitfalls

- **SGDistributedLogs must exist before the cluster references it.** The validating webhook
  rejects an `SGCluster` whose `distributedLogs.sgDistributedLogs` name does not resolve to
  an existing `SGDistributedLogs` in the same namespace. Create the instance first.
- **Both resources must be in the same namespace.** Cross-namespace references are not
  supported.
- **`retention` changes apply only to future log entries.** When the retention window is
  shortened, existing entries older than the new window are not immediately removed;
  removal is bounded by the previously configured window (or 7 days if none was set).
- **Logs are visible in the Web Console only when distributed logging is enabled.** Without
  `spec.distributedLogs`, log output goes only to the pod's standard output and is not
  accessible from the UI.
- **Persistent volume size for SGDistributedLogs should be sized for log volume.** A busy
  cluster generates significant log data; provision enough storage and monitor disk usage to
  avoid the log database filling up.
