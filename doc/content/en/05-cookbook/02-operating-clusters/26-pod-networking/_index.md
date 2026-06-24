---
title: Pod networking
weight: 26
url: /cookbook/operating-clusters/pod-networking
description: Adjust DNS and host networking settings of the cluster Pods.
showToc: true
---

## What it does

Controls low-level networking settings of each cluster Pod via the `spec.pods` section of
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}). The fields
`pods.hostNetwork`, `pods.dnsPolicy`, `pods.dnsConfig`, `pods.setHostnameAsFQDN`, and
`pods.statefulSetServiceName` are all updatable on a running resource and are reconciled
by the operator without re-creating the StatefulSet.

## When to use it

- Your cluster Pods must bind on node-level ports (for example, a bare-metal deployment
  where a load-balancer is not available) and you need `hostNetwork: true`.
- You run in a network environment where the cluster-internal DNS resolver is unreliable
  and need to point Pods at a custom nameserver via `dnsConfig`.
- Applications require Pods to announce themselves with a fully qualified domain name
  (FQDN) rather than just the short hostname (`setHostnameAsFQDN: true`).
- You use a custom headless Service to govern Pod DNS identity and need to override
  `statefulSetServiceName` to match.
- You must force a specific DNS policy (for example `ClusterFirstWithHostNet` when
  `hostNetwork` is enabled).

## How to do it

Apply a patch to the running cluster. The example below enables host networking and
sets the matching DNS policy together with a custom search domain:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  pods:
    hostNetwork: true                   # use the node's network namespace
    dnsPolicy: ClusterFirstWithHostNet  # required when hostNetwork is true
    setHostnameAsFQDN: true             # advertise Pod FQDN as hostname
    dnsConfig:
      nameservers:
        - 10.96.0.10                    # additional resolver (appended to policy defaults)
      searches:
        - my-cluster.svc.cluster.local  # extra search domain
      options:
        - name: ndots
          value: "5"
```

```bash
kubectl apply -f cluster.yaml
```

To change only a single field without modifying the full manifest:

```bash
# Override DNS policy only
kubectl patch sgcluster cluster -n my-cluster \
  --type merge \
  -p '{"spec":{"pods":{"dnsPolicy":"ClusterFirstWithHostNet"}}}'
```

To point Pods at a custom headless Service (the Service must already exist):

```bash
kubectl patch sgcluster cluster -n my-cluster \
  --type merge \
  -p '{"spec":{"pods":{"statefulSetServiceName":"my-headless-svc"}}}'
```

## How it works

When the operator reconciles the SGCluster it rebuilds the Pod template of the managed
StatefulSet. Kubernetes performs a rolling restart — one Pod at a time — so that Patroni
can maintain quorum and avoid an unnecessary primary failover.

| Field | Default | Effect |
|---|---|---|
| `pods.hostNetwork` | `false` | When `true`, Pods share the node network namespace and bind ports directly on the node |
| `pods.dnsPolicy` | `ClusterFirst` | Controls which resolver Pods consult; must be `ClusterFirstWithHostNet` when `hostNetwork` is `true` |
| `pods.dnsConfig` | — | Merged on top of the policy-generated config; supports `nameservers`, `searches`, and resolver `options` |
| `pods.setHostnameAsFQDN` | `false` | When `true`, the kernel hostname is set to the full FQDN instead of the short Pod name |
| `pods.statefulSetServiceName` | — | Overrides the governing Service used for Pod DNS identity in the StatefulSet |

## What to expect

- Each change triggers a rolling restart of the StatefulSet Pods; allow time proportional
  to the number of instances.
- Confirm the active DNS policy after the rollout:

  ```bash
  kubectl get pod -n my-cluster -o jsonpath='{.items[0].spec.dnsPolicy}'
  ```

- Verify host-network mode:

  ```bash
  kubectl get pod -n my-cluster -o jsonpath='{.items[0].spec.hostNetwork}'
  ```

## Pitfalls

- **`hostNetwork: true` exposes Pods on node ports.** All container ports become directly
  accessible on the node's IP address. This can cause port conflicts if another process on
  the same node is already using the same port (for example, port 5432). Ensure the nodes
  are dedicated to or reserved for StackGres Pods before enabling host networking.
- **`dnsPolicy` must be `ClusterFirstWithHostNet` with `hostNetwork`.** Using the default
  `ClusterFirst` policy when `hostNetwork` is `true` causes in-cluster DNS resolution to
  fail because the Pod's `/etc/resolv.conf` is not populated from the cluster DNS. Always
  set `dnsPolicy: ClusterFirstWithHostNet` when enabling host networking.
- **`statefulSetServiceName` must reference an existing headless Service.** The StatefulSet
  controller requires the governing Service to exist before Pods are (re-)created. If you
  change this field and the target Service is missing, new Pods will fail to start.
- **Rolling restart on every change.** Each field update reconciles the Pod template and
  triggers a rolling restart of all instances. Avoid making repeated incremental changes
  in production; batch all networking adjustments into a single apply.
