---
title: Adding custom init containers
weight: 22
url: /cookbook/operating-clusters/custom-init-containers
description: Run your own init containers before Postgres starts.
showToc: true
---

## What it does

Injects one or more user-defined init containers into every Pod of a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) through
`spec.pods.customInitContainers`. Those containers execute after the operator's own init
containers but before the main containers start. Companion fields —
`pods.customInitVolumeMounts`, `pods.customInitEnv`, and `pods.customInitEnvFrom` — allow
you to mount volumes and inject environment variables scoped to the init containers only.

## When to use it

- You need to pre-populate a shared `emptyDir` volume with configuration, seed data, or
  secrets before Postgres starts.
- You want to wait on an external service (for example, a Vault agent or a schema
  migration tool) before the cluster becomes active.
- You must run a one-time setup script — file permission fix, CA trust store injection,
  licence file placement — that must complete before any Postgres connections are accepted.

## How to do it

### 1. Define the custom init container in the SGCluster

The example below mounts an `emptyDir` volume and uses an init container to write a file
into it before Postgres starts. Note that custom volume and init container names are
automatically prefixed with `custom-` by the operator; use the unprefixed names in the
YAML and the prefixed names when referencing them in `SGInstanceProfile`.

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
    # Declare the shared volume (name will become "custom-init-data" at runtime).
    customVolumes:
    - name: init-data          # operator prepends "custom-" → "custom-init-data"
      emptyDir: {}

    # The init container itself (name becomes "custom-prepare-data" at runtime).
    customInitContainers:
    - name: prepare-data       # operator prepends "custom-" → "custom-prepare-data"
      image: busybox:1.36
      command:
      - sh
      - -c
      - echo "ready" > /mnt/init-data/status.txt
      volumeMounts:
      - name: custom-init-data # use the prefixed name inside the container spec
        mountPath: /mnt/init-data

    # Mount the same volume into the init container via the dedicated map field.
    # The map key is the init container name (with the "custom-" prefix).
    customInitVolumeMounts:
      custom-prepare-data:
      - name: custom-init-data
        mountPath: /mnt/init-data

    # Inject an environment variable into the init container.
    customInitEnv:
      custom-prepare-data:
      - name: INIT_ENV_EXAMPLE
        value: "hello"

    # Optionally pull env vars from a ConfigMap or Secret.
    # customInitEnvFrom:
    #   custom-prepare-data:
    #   - configMapRef:
    #       name: my-init-config
```

```bash
kubectl apply -f cluster.yaml
```

### 2. Verify Pods are running

```bash
kubectl get pods -n my-cluster
kubectl describe pod -n my-cluster -l app=StackGresCluster | grep -A5 "Init Containers"
```

## How it works

After the operator reconciles the `SGCluster`, it rebuilds the `StatefulSet` with the
additional init containers appended after its own (Patroni bootstrap, permissions, etc.).
Init containers run sequentially in declaration order. If any init container exits with a
non-zero code the Pod restarts according to its `restartPolicy`, blocking the main
containers — and therefore Postgres — from starting.

The `customInitVolumeMounts`, `customInitEnv`, and `customInitEnvFrom` fields use a map
keyed by the **prefixed** container name (`custom-<name>`). They are applied by the
operator when it constructs the Pod spec; you can also inline `volumeMounts` and `env`
directly inside the `customInitContainers` entries, but the dedicated map fields make it
easier to patch a single container without re-serialising the entire container spec.

Because all four fields are marked *updatable* in the CRD, the operator reconciles changes
on the next cycle. The change does require a Pod restart to take effect (the StatefulSet is
updated with a rolling restart strategy).

## What to expect

- New init containers appear in `kubectl describe pod` under the **Init Containers** section.
- The cluster enters a `pending-restart` condition after the change is applied. Use an
  `SGDbOps` restart to roll it in a controlled way:

  ```bash
  kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.status.conditions}' | grep PendingRestart
  ```

- Once all Pods have restarted, check that the init container ran to completion:

  ```bash
  kubectl logs -n my-cluster <pod-name> -c custom-prepare-data
  ```

## Pitfalls

- **A failing init container blocks Pod startup.** If the init container exits non-zero,
  Kubernetes restarts the Pod in a `CrashLoopBackOff`. Postgres never starts until all init
  containers succeed. Always add proper error handling and verify images are accessible from
  the cluster nodes.
- **Init containers run in order and sequentially.** They do not run in parallel. A
  long-running init container (for example, one that polls an external dependency) delays
  every Pod in the cluster. Add timeouts or readiness checks to avoid indefinite stalls.
- **Container names are prefixed with `custom-`.** Referencing an init container by its
  bare name in `customInitVolumeMounts`, `customInitEnv`, or `customInitEnvFrom` will not
  match. Always use the `custom-<name>` form as the map key.
- **Volume names are also prefixed.** Custom volumes declared in `customVolumes` are
  likewise prefixed. Use `custom-<volume-name>` when referencing them inside
  `volumeMounts` entries within `customInitContainers`.
- **Only certain volume types are allowed in `customVolumes`.** The operator accepts only:
  `configMap`, `downwardAPI`, `emptyDir`, `gitRepo`, `glusterfs`, `hostPath`, `nfs`,
  `projected`, and `secret`. Other types are rejected at admission.
- **Changes require a Pod restart.** All four fields are marked *may require restart* in
  the CRD. Plan the rollout using an `SGDbOps` restart to keep the cluster available during
  the update.
