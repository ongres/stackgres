---
title: Cluster rollout
weight: 21
url: /doc/latest/administration/rollout
description: Details about the rollout of an SGCluster.
showToc: true
---

The rollout of the SGCluster's Pods is orchestrated automatically by the operator with the cluster's update strategy configuration.

The `SGCluster.spec.pods.updateStrategy` section allows you to control how and when Pod updates are performed in your StackGres cluster. This configuration is essential for managing rolling updates, maintenance windows, and minimizing service disruption during cluster operations.

By default a rollout can be performed only by creating a `restart` (or `securityUpgrade` or `minorVersionUpgrade`) SGDbOps.

## Update strategy configuration

### Overview

The update strategy is configured in the `SGCluster` custom resource under `.spec.pods.updateStrategy`:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    updateStrategy:
      type: OnlyDbOps
      method: InPlace
```

### Update Strategy Fields

#### Type

The `type` field controls when Pod updates are performed. The default value is `OnlyDbOps`.

| Value | Description |
|:------|:------------|
| `Always` | Updates are performed as soon as possible when changes are detected. |
| `Schedule` | Updates are performed only during specified time windows. |
| `OnlyDbOps` | Updates are performed only when an SGDbOps of type `restart`, `securityUpgrade`, or `minorVersionUpgrade` targets the SGCluster. This is the **default** value. |
| `Never` | Updates are never performed automatically. Pods must be deleted manually to trigger updates. |

#### Method

The `method` field controls how the rolling update is performed. The default value is `InPlace`.

| Value | Description |
|:------|:------------|
| `InPlace` | Updates are performed on existing instances. In case only one instance is present, service disruption will last longer. This is the **default** value. |
| `ReducedImpact` | Before the update, a new instance is created to reduce impact on read-only replicas. This requires additional resources but minimizes service disruption. |

#### Schedule

The `schedule` field is an array of time windows during which updates are allowed. This field is only used when `type` is set to `Schedule`.

Each schedule entry has the following fields:

| Field | Type | Description |
|:------|:-----|:------------|
| `cron` | string | A UNIX cron expression indicating the start of the update window. |
| `duration` | string | An ISO 8601 duration in the format `PnDTnHnMn.nS` indicating the window duration. |

### Examples

#### Default Configuration (OnlyDbOps)

This is the default behavior. Updates only happen when explicitly triggered via SGDbOps (see [restart operation](#restart-operation)):

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    updateStrategy:
      type: OnlyDbOps
      method: InPlace
```

#### Automatic Updates with Reduced Impact

Updates are performed automatically as soon as changes are detected, using the reduced impact method:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    updateStrategy:
      type: Always
      method: ReducedImpact
```

#### Scheduled Maintenance Windows

Updates are only performed during scheduled maintenance windows:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    updateStrategy:
      type: Schedule
      method: ReducedImpact
      schedule:
        - cron: "0 2 * * 0"     # Every Sunday at 2:00 AM
          duration: "PT4H"      # 4 hour window
        - cron: "0 3 * * 3"     # Every Wednesday at 3:00 AM
          duration: "PT2H"      # 2 hour window
```

#### Manual Updates Only

Disable automatic updates entirely. Pods must be deleted manually:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  pods:
    updateStrategy:
      type: Never
```

### How Update Strategy Interacts with SGDbOps

Even when `type` is set to `Never`, SGDbOps operations of type `restart`, `securityUpgrade`, or `minorVersionUpgrade` will still trigger Pod updates. This allows you to have full control over when updates happen while still being able to perform maintenance operations.

The `method` setting in the update strategy serves as the default method for SGDbOps operations. However, you can override this by specifying a different method directly in the SGDbOps resource.

For more information about restart operations, see the [Manual Cluster Restart]({{% relref "04-administration-guide/11-rollout/01-manual-restart" %}}) section.

### Detecting Pending Updates

You can check if a cluster has pending updates by examining the `PendingRestart` condition:

```bash
kubectl get sgclusters.stackgres.io -A --template '
{{- range $item := .items }}
  {{- range $item.status.conditions }}
    {{- if eq .type "PendingRestart" }}
      {{- printf "%s.%s %s=%s\n" $item.metadata.namespace $item.metadata.name .type .status }}
    {{- end }}
  {{- end }}
{{- end }}'
```

### Best Practices

1. **Production environments**: Use `type: OnlyDbOps` or `type: Schedule` to have full control over when updates occur.

2. **Testing environments**: Use `type: Always` for immediate updates during development.

3. **High availability**: Use `method: ReducedImpact` when you have strict availability requirements and can afford the additional resources.

4. **Maintenance windows**: Use `type: Schedule` with appropriate cron expressions to ensure updates only happen during low-traffic periods.

## Restart operation

The `restart` operation in SGDbOps allows you to perform controlled restarts of your StackGres cluster.

### When to Use Restart Operations

A restart operation is typically needed when:

- Configuration changes require a Pod restart (indicated by the `PendingRestart` condition)
- You need to apply security patches or updates
- You want to refresh the cluster state
- Performing maintenance operations

You can check if a restart is pending by examining the cluster's conditions:

```bash
kubectl get sgclusters.stackgres.io -A --template '
{{- range $item := .items }}
  {{- range $item.status.conditions }}
    {{- if eq .type "PendingRestart" }}
      {{- printf "%s.%s %s=%s\n" $item.metadata.namespace $item.metadata.name .type .status }}
    {{- end }}
  {{- end }}
{{- end }}'
```

### Basic Restart Operation

To perform a basic restart of all Pods in a cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-my-cluster
  namespace: default
spec:
  sgCluster: my-cluster
  op: restart
```

Apply the operation:

```bash
kubectl apply -f restart-dbops.yaml
```

### Restart Configuration Options

The `restart` section supports the following options:

| Field | Type | Default | Description |
|:------|:-----|:--------|:------------|
| `method` | string | `InPlace` | The method used to perform the restart. Either `InPlace` or `ReducedImpact`. |
| `onlyPendingRestart` | boolean | `false` | When `true`, only Pods with pending restart status are restarted. |

### Restart Methods

#### InPlace Restart

The in-place method restarts Pods without creating additional replicas. This is resource-efficient but may cause longer service disruption if you have a single-instance cluster.

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-inplace
  namespace: default
spec:
  sgCluster: my-cluster
  op: restart
  restart:
    method: InPlace
```

**Service Disruption:**
- Read-write connections are disrupted when the primary Pod is deleted until Patroni elects a new primary
- Read-only connections are disrupted when only one replica exists and that replica Pod is deleted

#### ReducedImpact Restart

The reduced impact method spawns a new replica before restarting existing Pods. This minimizes service disruption but requires additional cluster resources.

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-reduced-impact
  namespace: default
spec:
  sgCluster: my-cluster
  op: restart
  restart:
    method: ReducedImpact
```

This method is recommended for production environments where high availability is critical.

### Restart Only Pending Pods

To restart only those Pods that have pending changes (instead of all Pods):

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-pending-only
  namespace: default
spec:
  sgCluster: my-cluster
  op: restart
  restart:
    method: ReducedImpact
    onlyPendingRestart: true
```

### Scheduled Restart

You can schedule a restart operation to run at a specific time using the `runAt` field:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: scheduled-restart
  namespace: default
spec:
  sgCluster: my-cluster
  op: restart
  runAt: "2024-12-15T02:00:00Z"
  restart:
    method: ReducedImpact
```

### Restart with Timeout

Set a timeout to automatically cancel the operation if it takes too long:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-with-timeout
  namespace: default
spec:
  sgCluster: my-cluster
  op: restart
  timeout: PT30M  # 30 minute timeout
  restart:
    method: ReducedImpact
```

### Restart with Retries

Configure automatic retries in case of failures:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-with-retries
  namespace: default
spec:
  sgCluster: my-cluster
  op: restart
  maxRetries: 3
  restart:
    method: ReducedImpact
```

### Monitoring Restart Progress

#### Check Operation Status

```bash
kubectl get sgdbops restart-my-cluster -n default -o yaml
```

#### Watch Operation Progress

```bash
kubectl get sgdbops restart-my-cluster -n default -w
```

#### Check Restart Status Details

The operation status includes detailed information about the restart progress:

```bash
kubectl get sgdbops restart-my-cluster -n default -o jsonpath='{.status.restart}' | jq
```

Status fields include:
- `primaryInstance`: The primary instance when the operation started
- `initialInstances`: List of instances present when the operation started
- `pendingToRestartInstances`: Instances that are pending restart
- `restartedInstances`: Instances that have been restarted
- `switchoverInitiated`: Timestamp when switchover was initiated
- `switchoverFinalized`: Timestamp when switchover completed

### Integration with Update Strategy

The restart SGDbOps operation works in conjunction with the cluster's [update strategy](#update-strategy-configuration). Key points:

1. **Method inheritance**: If you don't specify a `method` in the SGDbOps, the cluster's `updateStrategy.method` is used.

2. **Override behavior**: Specifying a `method` in the SGDbOps overrides the cluster's default method for that operation.

3. **Update strategy type**: Restart operations are always allowed regardless of the cluster's `updateStrategy.type` setting. Even with `type: Never`, an explicit restart SGDbOps will be executed.

4. **Rollout operations**: The restart operation is classified as a "rollout operation" alongside `securityUpgrade` and `minorVersionUpgrade`. These operations trigger Pod updates according to the specified method.

### Important Considerations

#### Parameter Changes Requiring Primary-First Restart

If any of the following PostgreSQL parameters are changed to a **lower** value, the primary instance must be restarted before any replica:

- `max_connections`
- `max_prepared_transactions`
- `max_wal_senders`
- `max_locks_per_transaction`

In this case, the service disruption for read-write connections will last longer, depending on how long it takes the primary instance to restart.

#### Cluster with Single Instance

For single-instance clusters, the `InPlace` method will cause a complete service outage during the restart. Consider using `ReducedImpact` if you need to minimize downtime, as it will temporarily add a replica before restarting.

## Related Documentation

- [Manual Cluster Restart]({{% relref "04-administration-guide/11-rollout/01-manual-restart" %}})
- [SGDbOps CRD Reference]({{% relref "06-crd-reference/08-sgdbops" %}})
- [SGCluster CRD Reference]({{% relref "06-crd-reference/01-sgcluster" %}})
