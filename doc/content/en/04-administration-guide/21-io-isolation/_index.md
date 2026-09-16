---
title: I/O Isolation
weight: 21
url: /administration/io-isolation
description: Protect co-located Postgres clusters from noisy neighbors with per-pod I/O caps.
showToc: true
---

**I/O Isolation is a technique designed to tackle the "noisy neighbor" problem at the storage layer**. If a given node co-hosts more than one workload sharing the same I/O resources (e.g. a local NVMe drive), burst behavior of one or some of them may negatively affect the performance of the other workloads (when the device saturates). I/O isolation prevents this problem, by leveraging cgroup v2 I/O limits, similar to how CPU and/or memory limits can be applied. StackGres provides not one, but two fully-featured I/O isolation implementations.

It is not uncommon for Kubernetes nodes to be of large size; and to share some resources (such as local NVMe drives) across the running workloads (Pods) in the node. This creates potential I/O contention, if the devices are driven to saturation. Speaking in Postgres terms, if more than one database Pod is scheduled in the same node; the activity of one (e.g. a large `COPY`, an aggressive `VACUUM`, a batch/ETL job, a runaway query, etc) can saturate the device and degrade every other pod on that node.

StackGres allows for I/O limiting at the storage layer. It is an enterprise-grade capability, aimed at achieving a level of isolation similar to what cloud providers implement for multi-tenant storage.

StackGres supports two mechanisms to put a **hard per-pod ceiling** on disk I/O. Both are enforced by the same kernel machinery: [cgroup v2 `io.max`](https://docs.kernel.org/admin-guide/cgroup-v2.html) entries handled by the `blk-throttle` subsystem. The two mechanisms differ in how the limit is delivered and which trade-offs they carry. They can coexist on the same node.

Two properties are common to both:

- The cap is a **hard upper bound**: a capped pod never exceeds its configured IOPS/bandwidth, regardless of what else runs on the node.
- The cap is only a **soft lower bound**: a capped pod achieves its limit only while the device is not saturated by uncapped I/O. To achieve effective I/O isolation, two conditions must be met:
    1. All (potentially I/O intensive) Pods must be I/O capped. Alternatively, you can keep them off the node (for example via taint-based node reservation, see [this runbook]({{% relref "/09-runbooks/13-io-isolation#1-reserve-the-database-nodes-with-a-taint" %}})).
    1. The sum of all I/O Pod caps running on a node must be less than the (measured) total capacity of the underlying storage resource.


## Option 1: `ioLimits` in the SGCluster spec (StackGres 1.19+)

> **Note:** this is an **alpha** feature.

This is the simplest mechanism to enforce I/O isolation in StackGres. It's fully automated by StackGres and user-facing configuration is trivial, by setting the appropriate limits directly into the [`SGCluster`]({{% relref "/06-crd-reference/01-sgcluster" %}})'s `.spec.pods.persistentVolume.ioLimits` field (for [`SGShardedCluster`]({{% relref "/06-crd-reference/11-sgshardedcluster" %}}), the same object is available under `spec.coordinator.pods.persistentVolume` and `spec.workers.pods.persistentVolume`).

With this configuration set, StackGres itself writes the `io.max` limit for the cluster's data volume. E.g.:

```yaml
spec:
  pods:
    persistentVolume:
      size: '100Gi'
      ioLimits:
        readIops: 10000
        writeIops: 8000
        readMiBps: 500
        writeMiBps: 400
```

The StackGres cluster controller resolves the block device backing the data volume at runtime and writes the limit into the pod's cgroup, re-applying it on every reconciliation cycle. Changing the values is a **live operation**: edit the SGCluster and the new limit is in force within seconds, with no pod restart required.

The trade-off is privilege: the cluster pods gain an init container running as root (with only the `CHOWN` capability, used solely to change the ownership of the pod's `io.max` cgroup file) and mount the host's `/sys/fs/cgroup`. For this reason the feature is disabled by default and must be enabled globally by the operator administrator, by adding the `io-limits` feature gate under `.spec.featureGates` of the [SGConfig]({{% relref "/06-crd-reference/12-sgconfig" %}}). Environments that forbid root containers or host-path mounts (for example restrictive OpenShift [SCCs](https://docs.openshift.com/container-platform/latest/authentication/managing-security-context-constraints.html)) cannot use this option --use option 2 instead.

Field reference and further details: see the [storage configuration]({{% relref "/04-administration-guide/04-configuration/05-storage-configuration" %}}) page.


## Option 2: container-runtime BlockIO classes

If the security posture of the first mechanism is not adequate for your needs, StackGres offers another mechanism based on configuring the container runtime's BlockIO support, selected through a Pod annotation. This technique does not require using a privileged init container nor mounting the cgroup host path; but requires node preparation ahead and it's more involved. It is however recommended for stricter security environments, including hardened OpenShift environments.

The class selection travels as a pod annotation, `blockio.resources.beta.kubernetes.io/pod` (a per-container variant, `blockio.resources.beta.kubernetes.io/container.<name>`, also exists). Note that this is a beta annotation convention defined and consumed by the container runtimes ([CRI-O and containerd, via goresctrl](https://github.com/intel/goresctrl/blob/main/doc/blockio.md)): the kubelet passes pod annotations through to the runtime. For purity, note there's no graduated Kubernetes API behind it. StackGres propagates these annotations to the cluster pods.

To leverage this mechanism, the container runtime (CRI-O or containerd) has to be previously configured, at the node level, with a ladder of named **BlockIO classes** (for example `io-1k`, `io-5k`, `io-20k`, each mapping to IOPS/bandwidth values for the node's data disks). Each StackGres cluster then selects a class with a single pod annotation, propagated via the SGCluster spec:

```yaml
spec:
  metadata:
    annotations:
      clusterPods:
        blockio.resources.beta.kubernetes.io/pod: io-5k
```

Nothing in the pods is privileged (all configuration lives on the node) which makes this the option that works under hardened security policies. The costs are operational: the class ladder must be delivered to every database node (directly, or via the [Machine Config Operator](https://docs.openshift.com/container-platform/latest/machine_configuration/index.html) on OpenShift), the runtime must be restarted once to enable it, and class changes on a running cluster take effect only after the pods are restarted (for example via an [SGDbOps `restart`]({{% relref "/06-crd-reference/08-sgdbops" %}})).

The complete node-preparation procedure, verification steps, limitations, and troubleshooting are in the [I/O isolation runbook]({{% relref "/09-runbooks/13-io-isolation" %}}).

## Choosing between them

| | `ioLimits` (option 1) | BlockIO classes (option 2) |
|---|---|---|
| Availability | StackGres 1.19+ (as an alpha feature, behind the `io-limits` feature gate) | Any supported StackGres version; CRI-O 1.20+ or containerd 1.7+ |
| Configuration surface | The SGCluster spec, per cluster | Node files + runtime config + a pod annotation |
| Node preparation | None | Class ladder on every node + one runtime restart |
| Privileges in the pods | Root init container + host cgroup mount (SCC-sensitive) | None |
| Throttled device | The data volume's own block device, resolved at runtime | The devices the class targets (recommended: the physical disks) |
| Cgroup scope | Pod-level (all containers aggregate) | Per container |
| Changing a limit | Live, seconds, no restart | Annotation change requires a pod restart; ladder change is a node-level operation |
| Scheduler no-overcommit | Not included (can be layered manually) | Pairs with the [extended-resource](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#extended-resources) layer [in the runbook]({{% relref "/09-runbooks/13-io-isolation#5-advertise-iops-capacity-as-an-extended-resource" %}}) |

As a rule of thumb: if your platform allows the privileged plumbing, option 1 is operationally much simpler: no node access, live retuning, no restart-to-apply semantics, volume automatically selected. If you run under restrictive security policies (typical hardened OpenShift), or you want scheduler-enforced capacity budgeting, use option 2. Both are designed to hold their configured caps independently and concurrently, even on shared disks.
