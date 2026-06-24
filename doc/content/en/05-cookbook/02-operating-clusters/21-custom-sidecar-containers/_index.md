---
title: Adding custom sidecar containers
weight: 21
url: /cookbook/operating-clusters/custom-sidecar-containers
description: Run your own sidecar containers alongside Postgres.
showToc: true
---

## What it does

Injects one or more additional containers into every Pod of a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) via
`spec.pods.customContainers`. The operator reconciles the change by performing a rolling
restart of the cluster Pods. Companion fields `pods.customVolumes`,
`pods.customVolumeMounts`, `pods.customEnv`, and `pods.customEnvFrom` let you attach
volumes, environment variables, and environment variable sources to any container in the
Pod — including the custom ones you add.

## When to use it

- You need a log-shipping agent (for example Fluentd or Vector) co-located with Postgres
  without coupling it to the Postgres image.
- You want a metrics sidecar (for example an application-specific exporter) that reads
  from a shared volume or the Postgres socket.
- You are injecting a secret-rotation agent or a configuration reloader that must share
  the Pod network namespace with Postgres.

## How to do it

The example below adds a lightweight log-forwarding sidecar that tails the Postgres log
directory from a shared `emptyDir` volume. Adjust the image and command to your use case.

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

    # Additional volumes available to any container in the Pod.
    # The operator prepends "custom-" to each name, so reference them
    # as "custom-logbuffer" inside container specs.
    customVolumes:
      - name: logbuffer      # accessed as "custom-logbuffer" in mounts
        emptyDir: {}

    # The sidecar container definition follows the standard Kubernetes
    # Container spec. The name is also prefixed with "custom-" at runtime.
    customContainers:
      - name: log-forwarder   # runs as "custom-log-forwarder" in the Pod
        image: busybox:1.36
        command:
          - sh
          - -c
          - "tail -F /logs/postgresql.log || true"
        resources:
          requests:
            cpu: 50m
            memory: 32Mi
          limits:
            cpu: 100m
            memory: 64Mi

    # Mount the shared volume into the sidecar.
    # Keys are the container names as declared above (without the "custom-" prefix).
    customVolumeMounts:
      log-forwarder:
        - name: logbuffer      # "custom-logbuffer" in the actual Pod spec
          mountPath: /logs

    # Optional: inject environment variables into the sidecar.
    customEnv:
      log-forwarder:
        - name: LOG_LEVEL
          value: "info"
```

Apply the manifest:

```bash
kubectl apply -f cluster.yaml
```

Verify the sidecar is running in each Pod after the rolling restart completes:

```bash
kubectl get pods -n my-cluster -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
```

You should see `custom-log-forwarder` listed alongside the operator-managed containers.

## How it works

`pods.customContainers` is marked `updatable` in the SGCluster spec. When the operator
detects a change it triggers a rolling restart — one Pod at a time — so the cluster
remains available throughout. Each container name you declare is prefixed with `custom-`
in the generated Pod spec; the same prefix applies to entries in `customVolumes`. The
`customVolumeMounts`, `customEnv`, and `customEnvFrom` fields are keyed by the
**unprefixed** container name as written in the spec. The operator merges your definitions
with its own and writes the resulting Pod template to the StatefulSet.

## What to expect

- A rolling restart begins immediately after the SGCluster is reconciled. Watch progress:

  ```bash
  kubectl rollout status statefulset -n my-cluster
  ```

- Confirm the sidecar is present and healthy:

  ```bash
  kubectl get pod -n my-cluster <pod-name> \
    -o jsonpath='{range .status.containerStatuses[?(@.name=="custom-log-forwarder")]}{.state}{"\n"}{end}'
  ```

- Changes to `customContainers`, `customVolumes`, `customVolumeMounts`, `customEnv`, or
  `customEnvFrom` all trigger a rolling restart. Batch related changes into a single
  `kubectl apply` to avoid multiple restarts.

## Pitfalls

- **Name collisions with StackGres containers.** The operator already runs containers named
  `postgres`, `patroni`, `pgbouncer`, `envoy`, `fluent-bit`, and `prometheus-postgres-exporter`
  inside each Pod. Pick names that do not collide with those; the `custom-` prefix reduces
  but does not eliminate the risk if you happen to name a container `postgres`.
- **Custom containers count against Pod resource limits.** The SGInstanceProfile defines
  resources for operator-managed containers. Any `resources` you declare in
  `customContainers` are added on top; size them explicitly to avoid OOM or CPU throttling
  on the node.
- **Only a restricted set of volume types is allowed in `customVolumes`.** Supported types
  are: `configMap`, `downwardAPI`, `emptyDir`, `gitRepo`, `glusterfs`, `hostPath`, `nfs`,
  `projected`, and `secret`. PersistentVolumeClaims are not supported in this field.
- **Changes may require a restart.** All four fields (`customContainers`, `customVolumes`,
  `customVolumeMounts`, `customEnv`, `customEnvFrom`) are marked _may require restart_.
  The operator performs a controlled rolling restart; schedule the change during a
  low-traffic window if your workload is sensitive to brief connection re-routing.
