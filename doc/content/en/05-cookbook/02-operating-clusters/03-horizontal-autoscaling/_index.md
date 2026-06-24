---
title: Horizontal autoscaling (KEDA)
weight: 3
url: /cookbook/operating-clusters/horizontal-autoscaling
description: Let StackGres add and remove replicas automatically based on load.
showToc: true
---

## What it does

Scales the replica count of a running [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}})
automatically between a minimum and a maximum, using KEDA to react to replica connection
pressure. You control this through `spec.autoscaling`: the `mode`, the `minInstances` and
`maxInstances` bounds, and the `horizontal` tuning knobs
(`replicasConnectionsUsageTarget`, `replicasConnectionsUsageMetricType`,
`cooldownPeriod`, `pollingInterval`).

## When to use it

- Your cluster experiences predictable peaks (morning traffic spikes, batch jobs) and you
  want replicas added automatically rather than by hand.
- You need a cost-efficient floor for quiet periods while still being able to absorb bursts.
- You have already set up manual scaling and want to hand replica-count control over to the
  autoscaler.

## How to do it

Enable horizontal autoscaling by setting `mode` to `horizontal` (or `all` if you also want
vertical Pod autoscaling) and providing bounds. KEDA triggers upscaling when the average
connection usage across replicas exceeds the target ratio:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 2          # initial replica count; KEDA will take over from here
  # ...postgres, pods, etc. unchanged...
  autoscaling:
    mode: horizontal    # horizontal only; use "all" to also enable vertical
    minInstances: 2     # floor — never fewer than 2 total instances (primary + 1 replica)
    maxInstances: 5     # ceiling — never more than 5 total instances
    horizontal:
      replicasConnectionsUsageTarget: "0.8"   # scale up when usage exceeds 80%
      replicasConnectionsUsageMetricType: AverageValue  # default
      pollingInterval: 30    # seconds between metric checks (default 30)
      cooldownPeriod: 300    # seconds before a downscale is allowed (default 300)
```

```bash
kubectl apply -f cluster.yaml
```

After the operator reconciles, a KEDA `ScaledObject` is created in the cluster namespace.
Verify it:

```bash
kubectl get scaledobject -n my-cluster
```

## How it works

StackGres creates a KEDA `ScaledObject` that watches the replica connection-usage metric
exported by the cluster's Prometheus endpoint. When the `AverageValue` of active connections
divided by `max_connections` exceeds `replicasConnectionsUsageTarget` across replicas, KEDA
increases `spec.instances` on the SGCluster. When load drops and the `cooldownPeriod` has
elapsed, KEDA scales back down, always respecting `minInstances` and `maxInstances`. The
operator reconciles each change to `spec.instances` as usual, adding or removing replica
Pods via the StatefulSet.

## What to expect

- Upscaling is responsive: a new replica Pod is created within seconds of KEDA triggering,
  but the replica takes additional time to stream up and enter the read-ready pool.
- Downscaling is conservative by default (`cooldownPeriod: 300`). Increase this value if
  your workload has sharp-but-brief spikes that should not immediately shrink the fleet.
- The `-replicas` Service load-balances across all healthy replicas, so additional instances
  start receiving read traffic as soon as they are ready.

## Pitfalls

- **KEDA must be installed.** Horizontal autoscaling has no effect if the
  [KEDA operator](https://github.com/kedacore/keda) is absent from the cluster. The
  `ScaledObject` will not be created and `spec.instances` will not change automatically.
- **Do not edit `spec.instances` manually while autoscaling is active.** When `mode` is
  `horizontal` or `all`, KEDA owns the instance count. Manual edits will be overwritten on
  the next KEDA reconciliation cycle.
- **Node count and anti-affinity cap real scale.** The `production` profile applies Pod
  anti-affinity. If you have fewer nodes than `maxInstances`, extra replica Pods will stay
  `Pending`. Either provision more nodes or relax anti-affinity before raising the ceiling.
- **`minInstances` minimum is 2.** The field requires at least `2` (primary + one replica).
  Setting it to `1` is rejected by the validating webhook.
- **Relation to manual scaling.** If you want to return to manual control, set `mode: none`
  (or remove `autoscaling`) and then adjust `spec.instances` directly. See the
  *Manual scaling* recipe for details.
