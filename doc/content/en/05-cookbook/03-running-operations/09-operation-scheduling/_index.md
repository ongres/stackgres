---
title: Scheduling operation Pods
weight: 9
url: /cookbook/running-operations/operation-scheduling
description: Control where the SGDbOps Job Pod is scheduled.
showToc: true
---

## What it does

Configures `spec.scheduling` on an
[SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) resource to control where
Kubernetes places the Pod that executes the operation. The sub-fields —
`nodeSelector`, `tolerations`, `nodeAffinity`, `podAffinity`, `podAntiAffinity`, and
`priorityClassName` — map directly to the corresponding Kubernetes Pod spec fields.
The operator reconciles the field before launching the Job, so scheduling constraints
take effect on the very first (and any retry) attempt.

## When to use it

- The operation Pod needs to run on nodes tainted for maintenance or batch workloads
  and must tolerate those taints.
- You want to pin the operation Job to a node pool with high I/O capacity (for example
  during a major version upgrade that reads and writes large data volumes).
- Your cluster namespace applies `PodAntiAffinity` rules that would conflict with an
  unconstrained operation Pod and you need to steer it away explicitly.
- You want to assign a `PriorityClass` so the operation Pod is not evicted under node
  pressure before it completes.

## How to do it

Define the `SGDbOps` resource with the desired scheduling constraints under
`spec.scheduling`. The example below runs a cluster restart and pins the operation
Pod to dedicated maintenance nodes:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: restart-scheduled
spec:
  # Name of the cluster the operation targets.
  sgCluster: cluster
  op: restart

  scheduling:
    # Only schedule the operation Pod on nodes labelled workload=maintenance
    nodeSelector:
      workload: maintenance

    # Tolerate the taint applied to dedicated maintenance nodes
    tolerations:
      - key: dedicated
        operator: Equal
        value: maintenance
        effect: NoSchedule

    # Prefer to run on a node in the same zone as the primary, fall back to any node
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 50
          preference:
            matchExpressions:
              - key: topology.kubernetes.io/zone
                operator: In
                values:
                  - us-east-1a

    # Use a high-priority class so the Pod is not evicted before the operation finishes
    priorityClassName: database-ops
```

```bash
kubectl apply -f restart-scheduled.yaml
```

Watch the operation until it completes:

```bash
kubectl get sgdbops -n my-cluster restart-scheduled -w
```

## How it works

When the operator reconciles the `SGDbOps` resource it projects the `spec.scheduling`
fields directly onto the `PodSpec` of the Kubernetes `Job` it creates to drive the
operation. `nodeSelector` and `tolerations` are applied verbatim. `nodeAffinity`,
`podAffinity`, and `podAntiAffinity` are merged into the Pod's `affinity` block.
`priorityClassName` sets the corresponding field on the Pod spec.

The `scheduling` field is marked **updatable** in the
[SGDbOps reference]({{% relref "06-crd-reference/08-sgdbops" %}}), so it can be
patched between retries if an initial constraint turns out to be wrong — but the
running Job Pod is not mutated; only the next attempt (controlled by `spec.maxRetries`)
picks up the updated constraints.

## What to expect

- Verify that the Job Pod landed on the expected node:

  ```bash
  kubectl get pods -n my-cluster -l sgdbops=restart-scheduled -o wide
  ```

- Confirm the Pod's affinity and tolerations were applied as intended:

  ```bash
  kubectl get pod -n my-cluster \
    $(kubectl get pods -n my-cluster -l sgdbops=restart-scheduled -o name | head -1) \
    -o jsonpath='{.spec.nodeSelector}{"\n"}{.spec.tolerations}'
  ```

- Monitor the SGDbOps conditions to confirm the operation completed:

  ```bash
  kubectl describe sgdbops -n my-cluster restart-scheduled
  ```

## Pitfalls

- **Over-constrained scheduling leaves the operation Pod Pending and the operation
  never starts.** Combining `nodeSelector`, `nodeAffinity`, and `tolerations` can
  produce a set of requirements that no node satisfies. Always confirm that at least
  one schedulable node matches all constraints before applying the resource.
- **`priorityClassName` must exist before the SGDbOps is created.** The operator does
  not create `PriorityClass` objects. Referencing a class that does not exist in the
  cluster will cause the Job Pod to be rejected by the admission webhook.
- **Scheduling constraints apply to every retry.** If `spec.maxRetries` is greater
  than zero and the operation fails, each retry Pod is launched with the same
  `spec.scheduling` values. If the constraint is the root cause of the failure (for
  example, the targeted nodes are full), retries will also fail until you patch
  `spec.scheduling` to relax or correct the constraint.
- **SGDbOps is single-use.** Once an SGDbOps resource reaches a terminal state
  (`Completed` or `Failed` with retries exhausted), create a new resource to run the
  operation again; editing a completed resource has no effect.
