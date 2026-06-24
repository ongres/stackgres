---
title: Propagating annotations and labels
weight: 28
url: /cookbook/operating-clusters/annotations-labels
description: Attach custom annotations and labels to the resources StackGres creates.
showToc: true
---

## What it does

Populates `spec.metadata.annotations` and `spec.metadata.labels` on a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) so that the operator
propagates your custom metadata to the Kubernetes resources it owns. Each sub-key
targets a specific resource class: `allResources` covers everything, while
`clusterPods`, `services`, `primaryService`, `replicasService`, and `serviceAccount`
let you target individual resource types.

## When to use it

- You want a cost-allocation or team-ownership label on every Pod and Service the
  cluster creates, for example to satisfy a corporate tagging policy.
- An external tool (a service mesh, a log forwarder, or a monitoring agent) selects
  targets by annotation and you need those annotations on cluster Pods or Services.
- You are integrating StackGres with Kubernetes admission controllers or policy engines
  that require specific labels to be present.

## How to do it

Patch (or update) the `SGCluster` to add `spec.metadata`:

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
  metadata:
    annotations:
      # Applied to every resource the operator creates for this cluster.
      allResources:
        cost-center: "platform-team"
      # Applied only to cluster Pods.
      clusterPods:
        prometheus.io/scrape: "true"
        prometheus.io/port: "9187"
      # Applied to the primary (read-write) Service.
      primaryService:
        service.beta.kubernetes.io/aws-load-balancer-internal: "true"
      # Applied to the replicas (read-only) Service.
      replicasService:
        service.beta.kubernetes.io/aws-load-balancer-internal: "true"
      # Applied to both Services.
      services:
        networking.example.com/policy: "allow-internal"
      # Applied to the ServiceAccount.
      serviceAccount:
        eks.amazonaws.com/role-arn: "arn:aws:iam::123456789012:role/my-role"
    labels:
      # Applied to every resource the operator creates for this cluster.
      allResources:
        team: platform
        environment: production
      # Applied only to cluster Pods.
      clusterPods:
        app.kubernetes.io/component: database
      # Applied to both Services.
      services:
        app.kubernetes.io/component: database
```

```bash
kubectl apply -f cluster.yaml
```

To add or change a single annotation without rewriting the whole manifest, use
`kubectl patch`:

```bash
kubectl patch sgcluster -n my-cluster cluster --type merge -p '
spec:
  metadata:
    labels:
      allResources:
        cost-center: platform-team
        environment: production
'
```

## How it works

After the `SGCluster` is updated, the operator's reconciliation loop reads
`spec.metadata.annotations` and `spec.metadata.labels` and merges them into the
`metadata` of each managed resource on the next reconcile cycle. More specific keys
take precedence over `allResources` — if the same key appears in both `allResources`
and `clusterPods`, the `clusterPods` value wins for Pods. The changes are live: there
is no restart or rolling update required for labels and annotations alone.

## What to expect

After applying the manifest, verify propagation on Pods and Services:

```bash
kubectl get pods -n my-cluster -l app=StackGresCluster \
  -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.labels}{"\n"}{end}'

kubectl get svc -n my-cluster \
  -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.annotations}{"\n"}{end}'
```

The custom entries appear alongside the operator-managed keys within one reconciliation
cycle (typically a few seconds).

## Pitfalls

- **Some keys are managed by the operator and must not be overridden.** StackGres sets
  labels such as `app`, `role`, `cluster-name`, and several internal annotation keys on
  its resources. If you set the same key under `spec.metadata`, the operator's value
  wins on the next reconcile and your value is silently dropped. Use distinct key
  prefixes (for example `mycompany.com/key`) to avoid conflicts.
- **`allResources` and per-resource keys are merged, not replaced.** Adding a new key
  to `allResources` does not remove keys you previously set only in `clusterPods`.
  Remove keys explicitly if you no longer need them.
- **Label values must conform to Kubernetes syntax.** Values longer than 63 characters,
  values containing disallowed characters, or keys missing a valid prefix cause the
  `SGCluster` update to be rejected by the Kubernetes API server before StackGres sees
  it.
- **`serviceAccount` annotations are evaluated at Pod scheduling time.** Changing an
  annotation used by a workload identity provider (for example an IAM role ARN) takes
  effect only for newly created Pods, not for already-running ones.
