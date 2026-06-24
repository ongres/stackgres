---
title: Customizing health probes
weight: 24
url: /cookbook/operating-clusters/health-probes
description: Tune the readiness and liveness probes of the cluster Pods.
showToc: true
---

## What it does

Overrides the default readiness and liveness probe timings on the Pods of a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) by setting
`spec.pods.readinessProbe` and `spec.pods.livenessProbe`. Both objects accept the standard
Kubernetes probe timing fields. The operator reconciles the change by rolling the Pods with
the updated probe configuration.

## When to use it

- The cluster Pods are being marked `NotReady` during normal startup because the default
  `initialDelaySeconds` is too short for your hardware or workload.
- Slow queries or heavy checkpoints are causing liveness probe timeouts and triggering
  unnecessary container restarts.
- You want to widen the `failureThreshold` window on the readiness probe to keep a replica
  in the load-balancer pool longer during a controlled maintenance window.
- You need to align probe behaviour with cluster-wide `terminationGracePeriodSeconds`
  settings.

## How to do it

Add (or update) `spec.pods.readinessProbe` and `spec.pods.livenessProbe` in the SGCluster
manifest. Both sections accept the same set of fields; omit any field to keep the
Kubernetes default.

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
    readinessProbe:
      initialDelaySeconds: 15   # wait before the first check (default: 0)
      periodSeconds: 10         # how often to check (default: 10)
      timeoutSeconds: 5         # max seconds per check (default: 1)
      failureThreshold: 6       # consecutive failures before NotReady (default: 3)
      successThreshold: 1       # consecutive successes to become Ready (default: 1)
    livenessProbe:
      initialDelaySeconds: 60   # give Postgres time to start (default: 0)
      periodSeconds: 20         # check less frequently under load (default: 10)
      timeoutSeconds: 5         # allow slower responses (default: 1)
      failureThreshold: 3       # restarts after this many consecutive failures (default: 3)
```

Apply the change:

```bash
kubectl apply -f cluster.yaml
```

Or patch a single field without editing the full manifest:

```bash
kubectl patch sgcluster -n my-cluster cluster \
  --type=merge \
  -p '{"spec":{"pods":{"livenessProbe":{"initialDelaySeconds":60,"timeoutSeconds":5}}}}'
```

## How it works

Both `readinessProbe` and `livenessProbe` are marked `updatable` in the SGCluster spec.
When the operator detects a change it performs a rolling restart of the cluster Pods,
injecting the new probe values into each Pod spec. The probes themselves are executed by the
kubelet against the `patroni` container inside each Pod; StackGres does not alter the probe
command, only the timing parameters you supply.

`readinessProbe` controls whether traffic is sent to a Pod. A Pod failing its readiness
probe is removed from the Service endpoints but is not restarted. `livenessProbe` controls
Pod restarts: a Pod that fails `failureThreshold` consecutive liveness checks is killed and
rescheduled by Kubernetes.

## What to expect

Verify that the probe settings reached the running Pods:

```bash
kubectl get pod -n my-cluster cluster-0 \
  -o jsonpath='{.spec.containers[?(@.name=="patroni")].readinessProbe}'
```

```bash
kubectl get pod -n my-cluster cluster-0 \
  -o jsonpath='{.spec.containers[?(@.name=="patroni")].livenessProbe}'
```

Both outputs should reflect the values you set in the SGCluster spec. During the rolling
restart, one Pod at a time is cycled; the cluster remains available as long as at least one
primary and one replica are healthy.

## Pitfalls

- **Aggressive liveness probes cause needless restarts under load.** If `timeoutSeconds` or
  `periodSeconds` are too short, a busy primary that is running a long checkpoint can fail
  enough consecutive checks to trigger a restart and a failover. Widen `timeoutSeconds` and
  increase `initialDelaySeconds` before tightening `failureThreshold`.
- **`successThreshold` must be 1 for liveness probes.** Kubernetes enforces this constraint;
  setting any other value is rejected by the API server validation.
- **Changes trigger a rolling restart.** Updating probe timings causes the operator to
  restart Pods one at a time. Plan this change during a low-traffic window or ensure your
  application tolerates brief replica unavailability.
- **Omitted fields inherit Kubernetes defaults, not previous values.** If you apply a
  partial `livenessProbe` object, any field you leave out reverts to the Kubernetes default
  (e.g. `timeoutSeconds` defaults to 1 second), not to the value that was previously
  configured. Always specify every field you want to control.
- **`terminationGracePeriodSeconds` inside the probe overrides the Pod-level setting.**
  Setting this field on the probe itself (an int64 in seconds) changes how long Kubernetes
  waits after the probe triggers a termination signal. Leave it unset unless you specifically
  need a different grace period for probe-triggered shutdowns.
