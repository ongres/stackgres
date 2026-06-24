---
title: Pod scheduling and placement
weight: 20
url: /cookbook/operating-clusters/pod-scheduling
description: Control where cluster Pods are scheduled with affinity, tolerations, and spread.
showToc: true
---

## What it does

Configures `spec.pods.scheduling` on an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to control how Kubernetes
schedules the cluster's Pods. The sub-fields — `nodeSelector`, `tolerations`,
`nodeAffinity`, `podAffinity`, `podAntiAffinity`, `topologySpreadConstraints`, and
`priorityClassName` — map directly to the corresponding Kubernetes Pod spec fields. A
parallel `spec.pods.scheduling.backup` sub-object applies the same controls to backup
Job Pods. Changes are reconciled by the operator without recreating the cluster, though
they may require a rolling restart.

## When to use it

- You have dedicated database nodes tainted to repel general workloads and need the
  cluster Pods to tolerate those taints.
- You want to pin Pods to a specific node pool (for example nodes with local NVMe or
  a particular availability zone) using a `nodeSelector` or `nodeAffinity`.
- You want to guarantee that no two cluster Pods land on the same topology domain (zone,
  rack) beyond what the built-in anti-affinity already provides.
- You need to assign a `PriorityClass` so that database Pods are evicted last under
  node pressure.
- You want backup Jobs to run on separate, cheaper nodes to avoid competing for
  resources with the live cluster.

## How to do it

Patch (or update) the `SGCluster` with the desired scheduling settings:

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
    scheduling:
      # Only schedule on nodes labelled role=postgres
      nodeSelector:
        role: postgres

      # Tolerate the taint applied to dedicated database nodes
      tolerations:
        - key: dedicated
          operator: Equal
          value: postgres
          effect: NoSchedule

      # Prefer to spread Pods across availability zones
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: StackGresCluster

      # Elevate scheduling priority to reduce eviction risk
      priorityClassName: database-critical

      # Schedule backup Jobs on cheaper spot/preemptible nodes
      backup:
        nodeSelector:
          workload: batch
        tolerations:
          - key: spot
            operator: Exists
            effect: NoSchedule
```

```bash
kubectl apply -f cluster.yaml
```

## How it works

When the operator reconciles the `SGCluster` it projects the `scheduling` fields
directly onto the `PodSpec` of the managed `StatefulSet`. `nodeSelector` and
`tolerations` are applied verbatim. `nodeAffinity`, `podAffinity`, and
`podAntiAffinity` are merged into the Pod's `affinity` block. The operator adds its
own `podAntiAffinity` term under the `production` profile (the default) to prevent two
cluster Pods from co-locating on the same node; rules you add are merged alongside that
built-in term, they do not replace it. `topologySpreadConstraints` are also appended to
any the operator may already set. The `backup` sub-object applies the same projection
to the `PodSpec` of the Kubernetes `Job` that runs each backup.

Because `spec.pods.scheduling` is marked **updatable, may require restart** in the
[SGCluster reference]({{% relref "06-crd-reference/01-sgcluster" %}}), the operator
will reconcile the change and trigger a rolling restart if the new constraints require
Pods to move.

## What to expect

- Verify that the StatefulSet carries the new scheduling configuration:

  ```bash
  kubectl get statefulset -n my-cluster cluster \
    -o jsonpath='{.spec.template.spec.nodeSelector}'
  ```

- Confirm all Pods are running on nodes that match the selector:

  ```bash
  kubectl get pods -n my-cluster -o wide
  ```

- Check the cluster status for any pending-restart condition:

  ```bash
  kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.status.conditions}'
  ```

## Pitfalls

- **The `production` profile adds Pod anti-affinity by default.** The default profile
  (`spec.profile: production`) prevents two cluster Pods from running on the same node.
  If your cluster has fewer nodes than instances, Pods will stay `Pending`. Either add
  more nodes or switch to a non-production profile via
  `spec.nonProductionOptions.disableClusterPodAntiAffinity: true` (not recommended for
  production).
- **Over-constraining leaves Pods Pending.** Combining `nodeSelector`, `nodeAffinity`,
  `tolerations`, and `topologySpreadConstraints` can produce a set of constraints that
  no node satisfies. Always verify that enough matching, taint-free nodes exist before
  applying restrictive rules.
- **`priorityClassName` must exist before the cluster is updated.** The operator does
  not create `PriorityClass` objects. Reference a class that is already defined in the
  cluster, or Pods will fail to be admitted.
- **Backup Pod scheduling is independent.** The `spec.pods.scheduling.backup` sub-object
  controls only the backup Job Pods. Backup Jobs will still use the cluster's default
  node pool unless you explicitly set `spec.pods.scheduling.backup`.
- **Changes may require a rolling restart.** Updating scheduling fields causes the
  operator to reconcile the `StatefulSet`, which triggers a rolling restart. Schedule
  changes during a maintenance window or use an `SGDbOps` restart to control the timing.
