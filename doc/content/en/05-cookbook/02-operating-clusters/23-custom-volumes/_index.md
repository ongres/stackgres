---
title: Mounting custom volumes
weight: 23
url: /cookbook/operating-clusters/custom-volumes
description: Attach extra volumes to the cluster Pods.
showToc: true
---

## What it does

Adds extra Kubernetes volumes to every Pod of a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) and mounts them into one or
more containers. The fields involved are `pods.customVolumes` (declares the volumes),
`pods.customVolumeMounts` (mounts them into regular containers), and
`pods.customInitVolumeMounts` (mounts them into init containers).

## When to use it

- You need to inject a ConfigMap or Secret as files into the `patroni` container — for
  example, custom SSL certificates, a `pg_hba.conf` fragment, or an external configuration
  file.
- A sidecar or init container you added via `customContainers` or `customInitContainers`
  needs access to shared storage (e.g. an `emptyDir` scratch space or an NFS share).
- You want to expose Kubernetes Downward API metadata to a workload running inside the Pod.

## How to do it

The example below mounts a ConfigMap named `extra-config` as read-only files inside the
`patroni` container at `/etc/extra-config`, using an `emptyDir` scratchpad shared with a
custom init container.

```bash
kubectl create namespace my-cluster
```

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
    customVolumes:
      - name: extra-config        # operator will expose this as "custom-extra-config"
        configMap:
          name: extra-config      # must exist in the same namespace
      - name: scratch             # emptyDir shared between containers
        emptyDir: {}
    customVolumeMounts:
      patroni:                    # map key is the container name
        - name: custom-extra-config   # note the "custom-" prefix added by the operator
          mountPath: /etc/extra-config
          readOnly: true
        - name: custom-scratch
          mountPath: /var/scratch
    customInitVolumeMounts:
      cluster-reconciliation:     # map key is the init container name
        - name: custom-scratch
          mountPath: /var/scratch
```

```bash
kubectl apply -f cluster.yaml
```

To add volumes to a cluster that is already running, patch the spec directly:

```bash
kubectl patch sgcluster -n my-cluster cluster \
  --type=merge \
  -p '{
    "spec": {
      "pods": {
        "customVolumes": [{"name":"extra-config","configMap":{"name":"extra-config"}}],
        "customVolumeMounts": {
          "patroni": [{"name":"custom-extra-config","mountPath":"/etc/extra-config","readOnly":true}]
        }
      }
    }
  }'
```

## How it works

`customVolumes`, `customVolumeMounts`, and `customInitVolumeMounts` are all marked
`updatable` in the SGCluster spec. When the operator detects a change it rebuilds the Pod
template and performs a rolling restart of the StatefulSet so that each Pod picks up the
new volume configuration.

The operator automatically prepends `custom-` to every name declared in `customVolumes`
before injecting it into the Pod spec. Volume mount entries in `customVolumeMounts` and
`customInitVolumeMounts` must therefore reference the prefixed name (e.g. `custom-scratch`,
not `scratch`).

Only a restricted set of volume types is permitted: `configMap`, `downwardAPI`, `emptyDir`,
`gitRepo`, `glusterfs`, `hostPath`, `nfs`, `projected`, and `secret`. CSI or persistent
volume claims are not supported in this field.

## What to expect

- A rolling restart is triggered. The cluster remains available during the restart because
  replicas are restarted first, followed by the primary.
- Verify the volumes are present after the restart:

  ```bash
  kubectl exec -n my-cluster cluster-0 -c patroni -- ls /etc/extra-config
  ```

- If the referenced ConfigMap or Secret does not exist, the Pod will fail to start with an
  `CreateContainerConfigError`. Create the missing resource first.

## Pitfalls

- **Reserved mount paths will break the cluster.** The operator mounts several paths inside
  `patroni` and other containers (for example `/var/lib/postgresql`, `/etc/patroni`,
  `/var/run/postgresql`). Mounting a custom volume onto any of these paths silently
  shadows the original content and will cause the cluster to malfunction. Always choose a
  distinct, application-specific mount path such as `/etc/extra-config` or `/mnt/mydata`.
- **Volume names must be unique within the Pod.** After the `custom-` prefix is applied,
  the resulting name must not collide with any volume already created by the operator.
  Inspecting the Pod spec (`kubectl get pod cluster-0 -o yaml`) is the easiest way to
  check for conflicts before applying the change.
- **Map keys must match real container names.** If the container name used as a key in
  `customVolumeMounts` or `customInitVolumeMounts` does not match an existing container in
  the Pod, the mount is silently ignored. Use `kubectl get pod cluster-0 -o jsonpath='{.spec.containers[*].name}'`
  to list valid container names.
- **Changes may require a restart.** Both `customVolumes` and the mount fields are
  documented as "may require restart". The operator will perform a rolling restart of the
  StatefulSet; plan the change during a low-traffic window if the application is
  sensitive to short primary failovers.
