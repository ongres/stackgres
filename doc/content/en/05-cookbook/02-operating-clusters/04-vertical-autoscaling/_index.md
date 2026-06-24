---
title: Vertical autoscaling (VPA)
weight: 4
url: /cookbook/operating-clusters/vertical-autoscaling
description: Let StackGres adjust per-Pod CPU and memory automatically.
showToc: true
---

## What it does

Continuously right-sizes the CPU and memory requests of the cluster Pods within
configurable bounds, using the Vertical Pod Autoscaler (VPA). You control this through
`spec.autoscaling`: the `mode` that enables or disables vertical (and/or horizontal)
autoscaling, per-container `minAllowed` and `maxAllowed` resource bounds, and an optional
custom `vertical.recommender`.

## When to use it

- You provisioned the cluster with rough resource estimates and want the operator to
  converge on the right sizing over time without manual intervention.
- You want a safety net against resource under-provisioning for workloads with variable
  query patterns.
- You are running multiple clusters on shared nodes and want each cluster to consume only
  what it actually needs.

## How to do it

Enable vertical-only autoscaling and set per-container resource bounds:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 10Gi
  autoscaling:
    mode: vertical          # only VPA; use "all" to also enable HPA
    minAllowed:
      patroni:
        cpu: 500m
        memory: 512Mi
      pgbouncer:
        cpu: 100m
        memory: 64Mi
      envoy:
        cpu: 100m
        memory: 64Mi
    maxAllowed:
      patroni:
        cpu: "4"
        memory: 8Gi
      pgbouncer:
        cpu: 500m
        memory: 256Mi
      envoy:
        cpu: 500m
        memory: 256Mi
```

```bash
kubectl apply -f cluster.yaml
```

`mode` accepts `all` *(default)*, `vertical`, `horizontal`, or `none`.
`minAllowed` / `maxAllowed` constrain the VPA recommendations for each sidecar container
(`patroni`, `pgbouncer`, `envoy`); each accepts `cpu` and `memory` in standard Kubernetes
quantity notation.

To pin a specific VPA recommender instead of the cluster default, add:

```yaml
  autoscaling:
    mode: vertical
    vertical:
      recommender: my-custom-recommender
```

See the full field reference at
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}).

## How it works

When vertical autoscaling is active, the operator creates a `VerticalPodAutoscaler`
object targeting the cluster StatefulSet. The VPA recommender observes CPU and memory
usage for each container and produces updated resource requests. The operator applies
those recommendations, bounded by `minAllowed` and `maxAllowed`, to the Pod spec.
Because Kubernetes cannot update running Pod resources in-place (in most configurations),
applying a new recommendation triggers a rolling restart: replica Pods are restarted
first, and the primary is handled last via a Patroni-driven switchover so the cluster
remains available throughout.

## What to expect

- After enabling VPA, allow several hours of traffic for the recommender to accumulate
  enough samples before meaningful recommendations appear.
- Check the VPA object to see current recommendations:

  ```bash
  kubectl get vpa -n my-cluster
  ```

- The SGCluster status reflects the current resource configuration after each
  reconciliation cycle.
- Reducing `maxAllowed` below the current running resources will trigger a restart of
  the affected Pods on the next reconciliation.

## Pitfalls

- **VPA operator is required.** Without the
  [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler)
  installed in the cluster, setting `mode: vertical` or `mode: all` has no effect — no
  error is raised but no VPA object will be managed.
- **Applying new resource bounds triggers a rolling restart.** Each recommendation
  applied to the StatefulSet causes Pods to be restarted (replicas first, primary last
  via switchover). Schedule changes during a maintenance window on latency-sensitive
  clusters.
- **Bounds must be consistent.** `minAllowed` values must not exceed `maxAllowed` for
  the same container; mismatched bounds are rejected by the validating webhook.
- **VPA and HPA can conflict on memory.** When `mode: all`, ensure the HPA metric
  (connection usage) and VPA memory recommendations do not work against each other;
  prefer `mode: vertical` if you are not also using horizontal autoscaling.
