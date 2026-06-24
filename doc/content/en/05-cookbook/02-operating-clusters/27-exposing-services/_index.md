---
title: Exposing the cluster services
weight: 27
url: /cookbook/operating-clusters/exposing-services
description: Choose the Kubernetes Service type and metadata for primary and replica endpoints.
showToc: true
---

## What it does

Controls the Kubernetes Services that StackGres creates for a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). Two Services are managed:

- **primary** (`<cluster-name>`) — always points to the current read-write leader. A legacy
  alias `<cluster-name>-primary` is also kept for backward compatibility.
- **replicas** (`<cluster-name>-replicas`) — load-balances across all read-only standbys.

Both Services default to `ClusterIP`. This recipe shows how to change their `type` and
toggle them on or off via `spec.postgresServices`.

## When to use it

- You need to reach the cluster from outside the Kubernetes cluster (e.g. a legacy
  application or a BI tool) and want a `LoadBalancer` or `NodePort` endpoint.
- You want to disable the replicas Service entirely because you route read traffic through
  PgBouncer or an application-layer proxy.
- You need to restrict which source IPs can reach a cloud load balancer
  (`loadBalancerSourceRanges`) or select a specific load-balancer implementation
  (`loadBalancerClass`).

## How to do it

Patch `spec.postgresServices` on the running cluster. Only the sub-sections you include are
changed; omitted sections keep their current values.

The example below exposes the primary as a `LoadBalancer` (for external write traffic) and
keeps the replicas as `ClusterIP` (internal read traffic only):

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
  postgresServices:
    primary:
      # enabled defaults to true; set false to delete the Service entirely.
      enabled: true
      # type: ClusterIP | NodePort | LoadBalancer | None
      type: LoadBalancer
      # Restrict access to specific CIDR blocks (LoadBalancer only).
      loadBalancerSourceRanges:
        - "10.0.0.0/8"
    replicas:
      enabled: true
      type: ClusterIP   # keep replicas internal
```

```bash
kubectl apply -f cluster.yaml
```

Or use a targeted patch without rewriting the whole manifest:

```bash
kubectl patch sgcluster -n my-cluster cluster \
  --type=merge \
  -p '{"spec":{"postgresServices":{"primary":{"type":"LoadBalancer","loadBalancerSourceRanges":["10.0.0.0/8"]}}}}'
```

Watch until the cloud provider assigns an external IP:

```bash
kubectl get svc -n my-cluster -w
```

## How it works

`spec.postgresServices` is marked `updatable` in the
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) spec. The operator reconciles
the change by updating the corresponding Kubernetes `Service` objects in place — no Pod
restart or failover is triggered. Patroni continuously updates the endpoint selectors so
that the primary Service always resolves to the current leader regardless of Service type.

When `enabled` is set to `false` the operator deletes the Service. Setting it back to
`true` recreates it.

## What to expect

- For `LoadBalancer`, the Service enters `Pending` state while the cloud controller
  provisions the load balancer. Confirm the external IP once it appears:

  ```bash
  kubectl get svc -n my-cluster cluster \
    -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
  ```

- For `NodePort`, each node exposes the Postgres port. Retrieve the assigned port:

  ```bash
  kubectl get svc -n my-cluster cluster \
    -o jsonpath='{.spec.ports[?(@.name=="pgport")].nodePort}'
  ```

- The operator propagates the change within one reconciliation cycle (typically a few
  seconds for `ClusterIP`/`NodePort`; cloud provisioning time varies for `LoadBalancer`).

## Pitfalls

- **LoadBalancer Services incur cost and provisioning time.** Every `LoadBalancer` Service
  allocates a cloud load balancer (and often a public IP). Provisioning can take one to
  several minutes. Use `ClusterIP` or `NodePort` when an external endpoint is not required.
- **Write traffic must use the primary Service; the replicas Service is read-only.** Sending
  write queries to `<cluster-name>-replicas` will fail with a read-only transaction error
  because standbys do not accept DML. Always direct application write connections to
  `<cluster-name>` (or `<cluster-name>-primary` for backward compatibility).
- **Disabling the primary Service cuts off all in-cluster connections.** Setting
  `postgresServices.primary.enabled: false` removes the Service that most applications use
  to reach Postgres. Ensure no workloads depend on it before disabling.
- **`loadBalancerSourceRanges` is only enforced by the cloud provider.** The field asks
  the cloud load balancer to filter traffic; it is not a Kubernetes-level network policy.
  Some providers ignore it. Use a `NetworkPolicy` for stricter enforcement.
- **`type: None` (headless) requires disabling Envoy.** A headless Service is only useful
  when `spec.pods.disableEnvoy` is also `true`; otherwise Envoy intercepts all traffic and
  the DNS-based routing benefit is lost.
