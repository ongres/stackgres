---
title: I/O Isolation
weight: 13
url: /runbooks/io-isolation
description: Apply per-pod I/O caps using container-runtime BlockIO classes and Kubernetes extended resources
showToc: true
---

When multiple workloads co-located on the same Kubernetes node share the
 underlying block storage, a heavy workload in one of them (a large `COPY`,
 an aggressive `VACUUM`, a runaway query, or any I/O-intensive
 non-StackGres tenant) can saturate the device and degrade every other
 pod on that node. This is the noisy-neighbor problem at the storage
 layer.

> Co-locating multiple StackGres clusters on the same node is itself an
> anti-pattern that StackGres recommends against. The protection described
> here is mostly relevant when a node is genuinely a shared resource —
> StackGres co-existing with other I/O-active workloads, multi-tenant
> nodes, or environments where node affinity cannot be fully controlled.

This runbook walks a Kubernetes cluster administrator through the steps
 needed to apply hard per-pod IOPS and bandwidth caps on the nodes that
 host StackGres clusters. It relies on three pieces, all standard:

- The container runtime's native **BlockIO classes** mechanism (CRI-O and
  containerd both support it).
- A pod annotation that selects a class.
- A standard Kubernetes **extended resource** advertised on the node, used
  by the kube-scheduler to prevent overcommitment.

No `hostPath` mounts, no privileged init containers in the cluster pods,
 no custom cgroup writes from inside the workload.

The runbook covers configuring CRI-O and containerd directly (applicable
 to any Kubernetes distribution), then documents the OpenShift-specific
 path that delivers the same configuration through the Machine Config
 Operator.

> **WARNING**: the initial setup **requires node restarts**. Enabling the
> BlockIO configuration changes the container runtime's startup
> parameters, which requires restarting `crio` or `containerd` on each
> database node. To avoid in-flight pod disruption the node must be
> drained first. On OpenShift the same applies for **existing** nodes
> already in the cluster (the Machine Config Operator orchestrates the
> drain + reboot one node at a time); **new** nodes provisioned with
> the role label set from the start get the configuration baked in via
> Ignition at first boot and require no restart. Plan the rollout
> accordingly: pool of n nodes needs at least n+1 capacity during the
> initial enablement, unless you are willing to absorb the downtime.

## What you get

Three pieces, working together:

1. **Per-pod caps via cgroup `io.max`**. The container runtime writes the
   cap into the pod's cgroup at container creation, the kernel's
   `blk-throttle` subsystem enforces it. Two pods on the same device,
   each capped at N IOPS, never affect each other below the saturation
   point.
2. **Drive characterization**. `fio` measures the device's sustained
   peak, a conservative fraction of that becomes the node's safe ceiling.
3. **Scheduler-enforced no-overcommit**. The safe ceiling is advertised
   as an extended resource (`stackgres.io/iops`); each StackGres pod
   requests its share; the kube-scheduler refuses to place a pod that
   would push the node over capacity. The same shape of `FailedScheduling`
   event you already get for `cpu` or `memory`.

Pieces 1 and 3 are independent layers. You can adopt 1 first
 (noisy-neighbor protection only) and add 3 later (bounded SLA across the
 node).

## Prerequisites

- A Kubernetes cluster (any distribution) where the nodes use the
  unified cgroup v2 hierarchy. If your nodes are pinned to cgroup v1, the
  BlockIO classes mechanism described here does not apply.
- `cluster-admin` permissions for the `kubectl` session.
- Container runtime on the database nodes is either CRI-O 1.20+ or
  containerd 1.7+. Older runtimes either lack BlockIO support entirely or
  expose it through a different and unstable API surface.
- For non-OpenShift clusters: a mechanism for delivering files to
  `/etc/crio/` or `/etc/containerd/` on each database node (SSH + `scp`,
  Ansible/Salt/Puppet, golden node image, or a privileged DaemonSet).
- A representative spare block device to run `fio` against during
  characterization, or a maintenance window during which an existing
  device can be benchmarked safely.

## Overview of components

| Layer | Component | Purpose |
|---|---|---|
| Node | Node labels | Identify the subset of nodes that will host StackGres clusters. |
| Node | `/etc/crio/blockio.yaml` or `/etc/containerd/blockio.yaml` | Defines the class ladder (named classes mapping to `io.max` values). |
| Runtime | CRI-O `blockio_config_file` option / containerd CRI plugin BlockIO option | Tells the runtime to load the class ladder. |
| Pod | Annotation `blockio.resources.beta.kubernetes.io/pod: <class>` | Selects the class for a given pod. The runtime resolves it at container creation and merges throttle parameters into the OCI runtime spec; `runc` writes them as `io.max` entries when it creates the pod's cgroup. |
| Cluster | Extended resource `stackgres.io/iops` | Node-level capacity advertised on `status.capacity`; pods request a share; the scheduler refuses overcommit. |

## 1. Label the database nodes

The label is used as a `nodeSelector` for the StackGres clusters you will
 later place on these nodes, and (on OpenShift) drives `MachineConfigPool`
 membership.

```bash
kubectl label node nvme-db-01 node-role.kubernetes.io/stackgres-db=
kubectl label node nvme-db-02 node-role.kubernetes.io/stackgres-db=
kubectl label node nvme-db-03 node-role.kubernetes.io/stackgres-db=
```

Verify:

```bash
kubectl get nodes -l node-role.kubernetes.io/stackgres-db
```

Expected output:

```
NAME         STATUS   ROLES                     AGE    VERSION
nvme-db-01   Ready    stackgres-db,worker       120d   v1.28.x
nvme-db-02   Ready    stackgres-db,worker       120d   v1.28.x
nvme-db-03   Ready    stackgres-db,worker       120d   v1.28.x
```

## 2. Characterize the drive with `fio`

You need a single integer per node hardware profile: the safe IOPS ceiling
 you will advertise as the extended resource and use as the upper bound of
 your BlockIO class ladder. The procedure is run once per hardware profile,
 not per node — identical hardware yields identical numbers.

> **Critical: prefill the SSD before measuring.** Empty-SSD performance is
> 30–50% higher than steady-state because the FTL has not yet allocated
> its over-provisioning. Skipping this yields numbers customers will hit
> the wall on after a few weeks of production load.

Deploy a temporary privileged Pod on the target node that has `fio`
 installed and `/dev` from the host mounted (raw block device access is
 required so the test bypasses any filesystem). The Pod targets the
 specific node via `nodeName`:

```yaml
kubectl create ns fio
cat << 'EOF' | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: fio-characterize
  namespace: fio
spec:
  nodeName: nvme-db-01
  restartPolicy: Never
  containers:
    - name: fio
      image: debian:bookworm-slim
      command:
        - sh
        - -c
        - "apt-get update -qq && apt-get install -y -qq fio && sleep infinity"
      securityContext:
        privileged: true
      volumeMounts:
        - name: dev
          mountPath: /dev
  volumes:
    - name: dev
      hostPath:
        path: /dev
EOF
```

> On OpenShift, the privileged Pod requires the `privileged` SCC to be
> bound to the namespace's `default` service account:
> `oc adm policy add-scc-to-user privileged -z default -n fio`.
> You will also have to label the `fio` namespace with:
```bash
kubectl label namespace fio \
    pod-security.kubernetes.io/enforce=privileged \
    pod-security.kubernetes.io/warn=privileged \
    pod-security.kubernetes.io/audit=privileged \
    --overwrite
```
> 
> Alternatively, run the `fio` commands directly on the node via
> `oc debug node/nvme-db-01`.

Wait for the Pod to be `Running` (the apt install takes ~30 seconds):

```bash
kubectl wait -n fio --for=condition=Ready pod/fio-characterize --timeout=120s
```

**Prefill (run once per drive — destroys the data on the device!):**

```bash
kubectl exec -n fio -it fio-characterize -- \
  fio --name=prefill --filename=/dev/nvme0n1 \
      --ioengine=libaio --direct=1 --rw=write --bs=1M \
      --iodepth=16 --numjobs=1 \
      --size=100% --loops=2
```

**Test 1 — random 4k read IOPS:**

```bash
kubectl exec -n fio -it fio-characterize -- \
  fio --name=randread-4k --filename=/dev/nvme0n1 \
      --ioengine=libaio --direct=1 --rw=randread --bs=4k \
      --iodepth=64 --numjobs=4 \
      --time_based --runtime=300s --ramp_time=60s \
      --group_reporting
```

**Test 2 — random 4k write IOPS:**

```bash
kubectl exec -n fio -it fio-characterize -- \
  fio --name=randwrite-4k --filename=/dev/nvme0n1 \
      --ioengine=libaio --direct=1 --rw=randwrite --bs=4k \
      --iodepth=64 --numjobs=4 \
      --time_based --runtime=300s --ramp_time=60s \
      --group_reporting
```

**Test 3 — sequential 1M read bandwidth:**

```bash
kubectl exec -n fio -it fio-characterize -- \
  fio --name=read-1m --filename=/dev/nvme0n1 \
      --ioengine=libaio --direct=1 --rw=read --bs=1M \
      --iodepth=16 --numjobs=2 \
      --time_based --runtime=180s --ramp_time=30s \
      --group_reporting
```

**Test 4 — sequential 1M write bandwidth:**

```bash
kubectl exec -n fio -it fio-characterize -- \
  fio --name=write-1m --filename=/dev/nvme0n1 \
      --ioengine=libaio --direct=1 --rw=write --bs=1M \
      --iodepth=16 --numjobs=2 \
      --time_based --runtime=180s --ramp_time=30s \
      --group_reporting
```

Each test ends with a summary block. The relevant lines look like:

```
randread-4k: (groupid=0, jobs=4): err= 0: pid=...
  read: IOPS=212k, BW=826MiB/s (866MB/s)(242GiB/300004msec)
  ...
```

Record `IOPS=` from tests 1 and 2, `BW=` from tests 3 and 4.

**Derive the safe ceilings:**

```
stackgres.io/iops          = floor(0.75 × min(test1_iops, test2_iops))
stackgres.io/io-bandwidth  = floor(0.75 × min(test3_bw,   test4_bw))   # if used
```

The `min()` is conservative — it ensures the cap holds for the worst-case
 read/write mix. The 0.75 factor leaves headroom for system I/O,
 filesystem overhead, and run-to-run variance.

Clean up the characterization Pod:

```bash
kubectl delete ns fio
```

Carry the numbers above into [step 3](#3-author-the-blockio-class-ladder)
 (to pick class values that fit under the ceiling) and
 [step 5](#5-advertise-iops-capacity-as-an-extended-resource) (to advertise
 the ceiling itself).

## 3. Author the BlockIO class ladder

Write a `blockio.yaml` file that defines the named classes you want to
 offer. The format is shared between CRI-O and containerd. Class names
 and tier values are entirely up to you — StackGres imposes no specific
 naming convention. Stay below the safe ceiling derived in
 [step 2](#2-characterize-the-drive-with-fio).

Example ladder (adjust devices and values for your environment):

```yaml
# blockio.yaml
Classes:

  stackgres-io-1k:
    - Devices: ["/dev/nvme[0-9]n[0-9]", "/dev/dm-[0-9]*"]
      ThrottleReadIOPS: 1k
      ThrottleWriteIOPS: 1k
      ThrottleReadBps: 50M
      ThrottleWriteBps: 50M

  stackgres-io-5k:
    - Devices: ["/dev/nvme[0-9]n[0-9]", "/dev/dm-[0-9]*"]
      ThrottleReadIOPS: 5k
      ThrottleWriteIOPS: 5k
      ThrottleReadBps: 200M
      ThrottleWriteBps: 200M

  stackgres-io-20k:
    - Devices: ["/dev/nvme[0-9]n[0-9]", "/dev/dm-[0-9]*"]
      ThrottleReadIOPS: 20k
      ThrottleWriteIOPS: 20k
      ThrottleReadBps: 800M
      ThrottleWriteBps: 800M
```

Device globs are expanded by the runtime against the real devices present
 on each node. Classes that reference devices absent on a given node
 simply produce no cgroup entries for that pod on that node — they do not
 fail.

Save this file locally; the next step places it on the database nodes via
 the path appropriate to your runtime/platform.

## 4. Install and enable the BlockIO configuration

This step has three alternative paths. Pick exactly one per node hardware
 profile, based on the container runtime in use and whether the cluster
 is managed by the OpenShift Machine Config Operator:

- [**4.1**](#41-cri-o-direct-configuration) — CRI-O nodes on any Kubernetes (manual configuration)
- [**4.2**](#42-containerd-direct-configuration) — containerd nodes on any Kubernetes (manual configuration)
- [**4.3**](#43-openshift-configure-via-machine-config-operator) — OpenShift (configuration delivered through MCO)

How you deliver the configuration files to each node in 4.1 / 4.2 is a
 node-management problem outside the scope of this runbook — typical
 choices are SSH + `scp`, a configuration-management tool
 (Ansible/Salt/Puppet), bakes into a golden node image, or a privileged
 DaemonSet that writes the files and restarts the runtime. The commands
 below are written as if executed directly on each node; adapt them to
 your delivery mechanism.

### 4.1 CRI-O direct configuration

Applies to clusters running CRI-O as the container runtime, where you
 have direct write access to `/etc/crio/` on each node.

**Step 1.** Place the class ladder on each database node:

```bash
# on each db node:
install -m 0644 blockio.yaml /etc/crio/blockio.yaml
```

**Step 2.** Add a CRI-O drop-in configuration file that points the
 runtime at the class ladder:

```bash
# on each db node:
mkdir -p /etc/crio/crio.conf.d
cat > /etc/crio/crio.conf.d/99-stackgres-blockio.conf << 'EOF'
[crio.runtime]
blockio_config_file = "/etc/crio/blockio.yaml"
EOF
```

**Step 3.** Restart CRI-O so the configuration takes effect. Drain the
 node first to avoid in-flight pod disruption:

```bash
# on the control plane:
kubectl drain nvme-db-01 --ignore-daemonsets --delete-emptydir-data

# on the node:
systemctl restart crio

# on the control plane:
kubectl uncordon nvme-db-01
```

**Step 4.** Verify the runtime picked up the configuration:

```bash
# on the node:
crio status config | grep blockio_config_file
```

Expected output:

```
blockio_config_file = "/etc/crio/blockio.yaml"
```

Inspect the CRI-O journal for the class load message:

```bash
journalctl -u crio --since "5 min ago" | grep -i blockio
```

Expected output: lines acknowledging the config file path and the loaded
 class names (`stackgres-io-1k`, `stackgres-io-5k`, ...).

Skip to [step 5](#5-advertise-iops-capacity-as-an-extended-resource).

### 4.2 containerd direct configuration

Applies to clusters running containerd as the container runtime, where
 you have direct write access to `/etc/containerd/` on each node.

> **⚠ Verify the exact configuration field on your containerd version.**
> Containerd's BlockIO support landed in the 1.7 series and the
> configuration plumbing has shifted between minor releases. Run
> `containerd config dump` on a target node and confirm the path of the
> BlockIO option in your installed version before applying the snippet
> below. The example uses the v2 plugin path for the CRI plugin; on some
> distributions or older builds you may need to use a
> `cri_blockio_config_file` key or place the file in a runtime-specific
> subsection.

**Step 1.** Place the class ladder on each database node:

```bash
# on each db node:
install -m 0644 blockio.yaml /etc/containerd/blockio.yaml
```

**Step 2.** Edit `/etc/containerd/config.toml` to point containerd at
 the class ladder. Add (or merge into the existing CRI plugin section):

```toml
version = 2

[plugins."io.containerd.grpc.v1.cri"]
  enable_blockio = true

[plugins."io.containerd.grpc.v1.cri".containerd]
  blockio_config_file = "/etc/containerd/blockio.yaml"
```

**Step 3.** Restart containerd. Drain the node first to avoid in-flight
 pod disruption:

```bash
# on the control plane:
kubectl drain nvme-db-01 --ignore-daemonsets --delete-emptydir-data

# on the node:
systemctl restart containerd

# on the control plane:
kubectl uncordon nvme-db-01
```

**Step 4.** Verify containerd loaded the configuration:

```bash
# on the node:
containerd config dump | grep -A1 blockio
```

Expected output (subset, exact key names depend on the version):

```
    enable_blockio = true
    blockio_config_file = "/etc/containerd/blockio.yaml"
```

Inspect the containerd journal for any parse errors:

```bash
journalctl -u containerd --since "5 min ago" | grep -i blockio
```

Skip to [step 5](#5-advertise-iops-capacity-as-an-extended-resource).

### 4.3 OpenShift: configure via Machine Config Operator

Applies to OpenShift clusters, where node configuration is managed
 declaratively through `MachineConfigPool`, `MachineConfig`, and
 `ContainerRuntimeConfig`. Direct edits under `/etc/crio/` are not
 supported and would be reverted by MCO.

#### 4.3.1 Create the MachineConfigPool

A node can only be in one pool. A custom pool takes precedence over
 `worker`, so labelled nodes leave `worker` and join `stackgres-db`. The
 other workers stay untouched.

```yaml
cat << 'EOF' | oc apply -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfigPool
metadata:
  name: stackgres-db
  labels:
    pools.operator.machineconfiguration.openshift.io/stackgres-db: ""
spec:
  machineConfigSelector:
    matchExpressions:
      - key: machineconfiguration.openshift.io/role
        operator: In
        values: [worker, stackgres-db]
  nodeSelector:
    matchLabels:
      node-role.kubernetes.io/stackgres-db: ""
  maxUnavailable: 1
EOF
```

Wait for the pool to reconcile. This triggers a rolling drain + reboot of
 existing nodes, one at a time, since they switch pools — make sure the
 pool has n+1 capacity if you cannot tolerate disruption:

```bash
oc get mcp stackgres-db -w
```

Expected output once stable:

```
NAME            CONFIG                                           UPDATED   UPDATING   DEGRADED   MACHINECOUNT   READYMACHINECOUNT   UPDATEDMACHINECOUNT   DEGRADEDMACHINECOUNT
stackgres-db    rendered-stackgres-db-<hash>                     True      False      False      3              3                   3                     0
```

> **Tip:** for new nodes provisioned with `node-role.kubernetes.io/stackgres-db=`
> set from the start, the BlockIO configuration is baked in at first boot
> via Ignition. No drain, no rolling restart. This is the recommended
> provisioning path for greenfield deployments.

#### 4.3.2 Deliver the BlockIO config via MachineConfig

Base64-encode the `blockio.yaml` authored in
 [step 3](#3-author-the-blockio-class-ladder) and embed it in a
 `MachineConfig`:

```bash
BLOCKIO_B64=$(base64 -w0 blockio.yaml)

cat << EOF | oc apply -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  name: 50-stackgres-blockio-config
  labels:
    machineconfiguration.openshift.io/role: stackgres-db
spec:
  config:
    ignition:
      version: 3.2.0
    storage:
      files:
        - path: /etc/crio/blockio.yaml
          mode: 0644
          contents:
            source: data:text/plain;base64,${BLOCKIO_B64}
EOF
```

Verify the file landed on a node:

```bash
oc debug node/nvme-db-01 -- chroot /host cat /etc/crio/blockio.yaml
```

Expected output: the YAML you authored in step 3, verbatim.

#### 4.3.3 Enable BlockIO in CRI-O

CRI-O has to be told to load `/etc/crio/blockio.yaml`. Ship a CRI-O
 drop-in configuration file via a second `MachineConfig` — it's just
 another Ignition file, so MCO delivers it to every node in the pool the
 same way it delivered `/etc/crio/blockio.yaml` in
 [step 4.3.2](#432-deliver-the-blockio-config-via-machineconfig):

```bash
CRIO_CONF_B64=$(printf '[crio.runtime]\nblockio_config_file = "/etc/crio/blockio.yaml"\n' | base64 -w0)

cat << EOF | oc apply -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  name: 51-stackgres-blockio-crio-config
  labels:
    machineconfiguration.openshift.io/role: stackgres-db
spec:
  config:
    ignition:
      version: 3.2.0
    storage:
      files:
        - path: /etc/crio/crio.conf.d/99-stackgres-blockio.conf
          mode: 0644
          contents:
            source: data:text/plain;base64,${CRIO_CONF_B64}
EOF
```

MCO restarts CRI-O whenever its config files change, so applying this
 triggers a rolling drain + reboot of the MCP — one node at a time. Watch
 the rollout:

```bash
oc get mcp stackgres-db -w
```

Wait until `UPDATED=True` and `UPDATING=False` again.

**Verify CRI-O loaded the configuration on a node:**

```bash
oc debug node/nvme-db-01 -- chroot /host crio status config | grep blockio
```

Expected output:

```
blockio_config_file = "/etc/crio/blockio.yaml"
```

If the blockio drop-in was loaded recently, the CRI-O journal will also
 show the load message (this fires only on drop-in reloads, not on every
 restart — an empty result here doesn't necessarily mean failure):

```bash
oc debug node/nvme-db-01 -- chroot /host journalctl -u crio --since "10 min ago" | grep -i blockio
```

Continue to [step 5](#5-advertise-iops-capacity-as-an-extended-resource).

## 5. Advertise IOPS capacity as an extended resource

This step is independent of the BlockIO class wiring above. Skip it if you
 only want noisy-neighbor protection without scheduler-enforced
 no-overcommit; come back to it later when you want the full bounded SLA.

Patch each node's status capacity with the safe ceiling derived in
 [step 2](#2-characterize-the-drive-with-fio). The path uses JSON Pointer
 escaping — `~1` is the literal escape for `/` and is easy to miss:

```bash
kubectl patch node nvme-db-01 \
  --subresource=status \
  --type=json \
  -p '[{"op": "add",
        "path": "/status/capacity/stackgres.io~1iops",
        "value": "150000"}]'
```

Repeat per node, substituting the appropriate value if hardware profiles
 differ.

Verify:

```bash
kubectl describe node nvme-db-01 | grep -A1 -E 'Capacity|Allocatable' | grep -E 'stackgres.io/iops|---'
```

Expected output:

```
  stackgres.io/iops:  150000
  stackgres.io/iops:  150000
```

(The two lines correspond to `Capacity` and `Allocatable`.)

> **Persistence caveat.** `status.capacity` is rewritten by the kubelet
> periodically. Kubernetes 1.20+ preserves unknown extended resources
> across status updates, but kubelet restarts, cluster upgrades, and node
> replacements are edge cases where the patched value can be lost. Treat
> the patch as part of node provisioning and document it in your runbooks;
> if a node loses the value, re-run the patch.

## 6. Validate end-to-end with an SGCluster

Create a test SGCluster that exercises the full path: the BlockIO class
 annotation is propagated to its pods, the `stackgres.io/iops` resource
 request is set on the patroni container, and a custom `fio` sidecar
 mounts the same data PV so I/O performance can be measured against the
 actual storage path Postgres will use.

> **About the storage class.** The example below uses `topolvm-nvme`, an
> LVM-based local CSI provisioner. The choice of provisioner is not
> arbitrary — the BlockIO cap has to apply to the device the workload
> actually writes to:
>
> - **Per-PVC device-mapper devices.** LVM-based local CSI drivers expose
>   each PVC as its own `/dev/dm-N` device. That's why the `blockio.yaml`
>   in [step 3](#3-author-the-blockio-class-ladder) includes
>   `/dev/dm-[0-9]*` in the device globs — the cap is enforced on the
>   dm device that the Postgres process writes to.
> - **Local storage.** I/O stays on the node, so a kernel-block-layer cap
>   is meaningful. With network-attached storage (Ceph RBD, EBS, GCE PD,
>   etc.) the bottleneck is the network and a per-device cgroup throttle
>   would be at the wrong layer.
>
> Equivalent alternatives that provide the same shape of local LVM-backed
> PV:
>
> - **[OpenEBS LVM-LocalPV](https://openebs.io/docs/concepts/local-storage-user-guide/local-pv-lvm)** —
>   vanilla Kubernetes, same TopoLVM-style semantics.
> - **Red Hat LVM Storage Operator (LVMS)** — **recommended on OpenShift**.
>   Available from OperatorHub, it is TopoLVM repackaged with proper SCC
>   handling and an `LVMCluster` CR that drives VG setup. Installing the
>   upstream TopoLVM Helm chart on OpenShift requires manual SCC bindings
>   and (depending on chart version) has container-image packaging issues
>   that LVMS avoids. The provisioner StorageClass LVMS creates is
>   typically named `lvms-<device-class>` (for example `lvms-vg1`).
> - Any other CSI driver that creates one block device per PVC and lives
>   on the node — Ondat, the Local Storage Operator paired with
>   `LocalVolume` raw block PVs, etc.
>
> Substitute the `storageClass` field below for whichever provisioner you
> use; the rest of this section is independent of the storage choice.

> If you skipped [step 5](#5-advertise-iops-capacity-as-an-extended-resource)
> (extended resource layer), remove the `pods.resources` block from the
> SGCluster spec below — otherwise the cluster pod will stay `Pending`
> because no node advertises the requested resource.

Adjust the `storageClass`, `sgInstanceProfile`, and Postgres version to
 values that exist in your environment:

```yaml
kubectl create ns io-isolation
cat << 'EOF' | kubectl apply -f -
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: io-isolation-test
  namespace: io-isolation
spec:
  instances: 1
  postgres:
    version: '16'
  sgInstanceProfile: size-xs
  metadata:
    annotations:
      clusterPods:
        blockio.resources.beta.kubernetes.io/pod: stackgres-io-5k
  pods:
    persistentVolume:
      storageClass: topolvm-nvme
      size: 10Gi
    scheduling:
      nodeSelector:
        node-role.kubernetes.io/stackgres-db: ""
    resources:
      containers:
        patroni:
          requests:
            stackgres.io/iops: "5000"
          limits:
            stackgres.io/iops: "5000"
    customContainers:
      - name: fio
        image: openeuler/fio
        command:
          - sh
          - -c
          - "sleep infinity"
        volumeMounts:
          - name: io-isolation-test-data
            mountPath: /var/lib/postgresql
EOF
```

Note: the custom container declared as `fio` appears in the pod as `custom-fio`
 — StackGres prepends `custom-` to custom container names.

Wait for the cluster pod to be running:

```bash
kubectl wait -n io-isolation --for=condition=Ready pod/io-isolation-test-0 --timeout=300s
```

If the pod stays `Pending`, inspect the events — `Insufficient
 stackgres.io/iops` means more capacity needs to be advertised
 ([step 5](#5-advertise-iops-capacity-as-an-extended-resource)) or the
 request needs to be lowered.

### 6.1 Verify the cgroup `io.max`

Get the pod UID and the node it was scheduled onto:

```bash
POD_UID=$(kubectl get pod -n io-isolation io-isolation-test-0 -o jsonpath='{.metadata.uid}')
NODE=$(kubectl get pod -n io-isolation io-isolation-test-0 -o jsonpath='{.spec.nodeName}')
echo "POD_UID=${POD_UID} NODE=${NODE}"
```

Open a shell on `${NODE}` (via SSH, `kubectl debug node/${NODE}`, or
 `oc debug node/${NODE}`) and locate the pod's `io.max` files:

```bash
# on the node:
find /sys/fs/cgroup -name 'io.max' -path "*pod${POD_UID//-/_}*" 2>/dev/null
```

(With the systemd cgroup driver, the path embeds the pod UID with dashes
 replaced by underscores. With the cgroupfs driver, dashes are preserved
 — adjust the substitution if the search returns nothing.)

The `find` returns one cgroup per process structure in the pod, which is
 more than one per workload container. Three categories show up — be
 careful which one you read:

- `kubepods-burstable-pod<UID>.slice/io.max` — the pod-level cgroup.
- `kubepods-burstable-pod<UID>.slice/crio-<container-ID>.scope/io.max` —
  one per workload container. **These should carry the throttle.**
- `kubepods-burstable-pod<UID>.slice/crio-conmon-<container-ID>.scope/io.max` —
  CRI-O's container-monitor sidecars (one per workload container).
  **These are not workload containers and are expected to be empty.**
  Ignore them when reading.

To map the `<container-ID>` values back to container names — so you know
 which scope corresponds to the **patroni** container — run:

```bash
kubectl -n io-isolation get pod io-isolation-test-0 -o json | \
  jq -r '.status.containerStatuses[] | "\(.name)\t\(.containerID | sub("cri-o://"; ""))"'
```

Expected output:

```
cluster-controller             3c6a5eb9c9c2b027eee9692c6a89d864b6c0c20b55c678546ec3062f82b1f51f
custom-fio                     35ae858a4d3a73f3a09a0a7419e9eeb85fd180a7ae7ea1755b7b38e011cc86c1
patroni                        0576fcfca34ff9c195d9b9715da5fbcbff150a6bfa99c38f1863b774d28ee47a
pgbouncer                      7e5d9e6e169b1923fe0964efd0098afb65c958ca7d8460458bc9354f4c66c684
postgres-util                  1f64dbb394e447959ca6b3eaec94bfceead578ce978326bdacbafd76c5da954a
prometheus-postgres-exporter   a2144dcf8a1e28109f37dd7055340e81c4254d5389cdb0b0951dd1486da4776f
```

Dump `io.max` for the patroni container's scope, substituting the
 container ID from the mapping above:

```bash
# on the node:
cat /sys/fs/cgroup/kubepods.slice/kubepods-burstable.slice/kubepods-burstable-pod${POD_UID//-/_}.slice/crio-<patroni-id>.scope/io.max
```

Expected output (exact device numbers depend on your storage stack):

```
259:0 rbps=209715200 wbps=209715200 riops=5000 wiops=5000
```

Device major numbers you might see in the output, depending on the
 backing storage:

| Major | Device type |
|---|---|
| `7` | loop devices (lab setups, e.g. file-backed LVM) |
| `8` | SCSI / SATA |
| `252`, `253` | device-mapper (LVM logical volumes, dm-thin) |
| `259` | NVMe |

If the `major:minor` in the output matches one of the entries the
 `Devices:` glob in your `blockio.yaml` would expand to, the throttle is
 attached to the right device.

This confirms:
- The annotation was recognized by the container runtime on the node.
- The throttle parameters from the `stackgres-io-5k` class were resolved.
- The kernel wrote the matching `io.max` entries into the pod's cgroup.

If `io.max` on the **patroni** container's scope (not the conmon ones —
 those are always empty) is absent or contains `max` for every field, the
 annotation was silently ignored — see
 [Troubleshooting](#troubleshooting) below.

### 6.2 Load-test against the data PV

Run `fio` from inside the `custom-fio` custom container, against a file on the
 same data PV that Patroni uses. The cgroup applies at the pod level so
 the cap that throttles Postgres also throttles `fio` here — the measured
 IOPS therefore reflect exactly the cap Postgres will see at runtime.

Once the pod is `Ready`, the `openeuler/fio` image used in the
 `custom-fio` container has `fio` already on `$PATH` — no startup install
 step is needed.

Run a random 4k read test (1 GB file, 60 seconds, direct I/O bypasses
 the page cache):

```bash
kubectl exec -n io-isolation -it io-isolation-test-0 -c custom-fio -- \
  fio --name=cap-check-read \
      --filename=/var/lib/postgresql/fio-test.dat \
      --size=1G \
      --ioengine=libaio --direct=1 --rw=randread --bs=4k \
      --iodepth=64 --numjobs=4 --runtime=60s --time_based \
      --group_reporting
```

Expected output (relevant line):

```
  read: IOPS=5012, BW=19.6MiB/s (20.5MB/s)(1176MiB/60003msec)
```

The reported `IOPS=` should land within a few percent of the
 `stackgres-io-5k` cap (5000), **not** the device's native peak. If the
 number is much higher, the throttle is not applied — re-check
 [section 6.1](#61-verify-the-cgroup-iomax) and the troubleshooting
 section.

Optionally run the write side to confirm symmetric throttling:

```bash
kubectl exec -n io-isolation -it io-isolation-test-0 -c custom-fio -- \
  fio --name=cap-check-write \
      --filename=/var/lib/postgresql/fio-test.dat \
      --size=1G \
      --ioengine=libaio --direct=1 --rw=randwrite --bs=4k \
      --iodepth=64 --numjobs=4 --runtime=60s --time_based \
      --group_reporting
```

### 6.3 Clean up the test cluster

```bash
kubectl delete ns io-isolation
```

## Troubleshooting

**Annotation silently ignored — `io.max` shows no throttle.** The most
 common cause is that the container runtime has not loaded the BlockIO
 config file. Causes to check, in order:

1. The configuration key is wrong for your runtime/version (see the
   verification notes in [step 4.1](#41-cri-o-direct-configuration),
   [step 4.2](#42-containerd-direct-configuration), and
   [step 4.3.3](#433-enable-blockio-in-cri-o-via-containerruntimeconfig)).
2. The runtime was not restarted after the configuration change — for
   CRI-O direct: `systemctl status crio`; for containerd direct:
   `systemctl status containerd`; for OpenShift: `oc get mcp stackgres-db`
   should show `UPDATED=True`.
3. The class name in the pod annotation does not match any class defined
   in `blockio.yaml`. The runtime ignores unknown class names without
   emitting an event.
4. The YAML is malformed and the runtime failed to parse it at startup —
   `journalctl -u crio` or `journalctl -u containerd` on the node will
   show the parse error.

**Pod stays `Pending` with `Insufficient stackgres.io/iops`.** The
 scheduler refuses to overcommit. Either reduce the pod's request, free
 up capacity by deleting/draining another pod, or expand the node's
 advertised capacity (only if your characterization supports it).

**`kubectl describe node` no longer shows `stackgres.io/iops`.** The
 kubelet dropped the value during a status refresh. Re-run the patch from
 [step 5](#5-advertise-iops-capacity-as-an-extended-resource).

**ZFS volumes show no write throttling.** ZFS does not correctly attribute
 buffered writes to the originating cgroup, so write throttling is
 ineffective on ZFS-backed PVs. This is a kernel/filesystem property, not
 a configuration error. Read throttling still works. Use a non-ZFS
 filesystem if write throttling is required.

## Rollback

The rollback procedure mirrors the configuration path you took in
 [step 4](#4-install-and-enable-the-blockio-configuration). Take the
 corresponding subsection below.

### Rollback for CRI-O direct configuration (step 4.1)

On each database node:

```bash
rm /etc/crio/crio.conf.d/99-stackgres-blockio.conf
rm /etc/crio/blockio.yaml
systemctl restart crio
```

Drain each node beforehand to avoid disruption to running pods.

### Rollback for containerd direct configuration (step 4.2)

On each database node, remove the `enable_blockio` and `blockio_config_file`
 entries from `/etc/containerd/config.toml`, then:

```bash
rm /etc/containerd/blockio.yaml
systemctl restart containerd
```

Drain each node beforehand to avoid disruption to running pods.

### Rollback for OpenShift (step 4.3)

Delete the `ContainerRuntimeConfig` and `MachineConfig`. MCO rolls the
 deletion out the same way it rolled out the creation (drain + reboot,
 one node at a time):

```bash
oc delete containerruntimeconfig stackgres-db-blockio
oc delete machineconfig 50-stackgres-blockio-config
```

To dismantle the pool entirely and return nodes to the `worker` pool:

```bash
oc label node nvme-db-01 node-role.kubernetes.io/stackgres-db-
oc label node nvme-db-02 node-role.kubernetes.io/stackgres-db-
oc label node nvme-db-03 node-role.kubernetes.io/stackgres-db-
oc delete mcp stackgres-db
```

### Rollback for the extended resource (step 5)

Independent of which configuration path you used:

```bash
kubectl patch node nvme-db-01 \
  --subresource=status \
  --type=json \
  -p '[{"op": "remove",
        "path": "/status/capacity/stackgres.io~1iops"}]'
```

Each rollback step is independent — for example, you can leave the
 BlockIO classes loaded and only remove the extended resource if you want
 to keep noisy-neighbor protection but stop enforcing scheduler-level
 no-overcommit.
