---
title: SGCluster
weight: 1
url: /reference/crd/sgcluster
description: Details about SGCluster
showToc: true
---

___

**Kind:** SGCluster

**listKind:** SGClusterList

**plural:** sgclusters

**singular:** sgcluster

**shortNames** sgclu
___
 
StackGres PostgreSQL cluster can be created using an SGCluster Custom Resource.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  instances: 1
  postgres:
    version: 'latest'
  pods:
    persistentVolume:
      size: '5Gi'
  sgInstanceProfile: 'size-s'
```

See also [Cluster Creation section]({{% relref "04-administration-guide/03-cluster-creation" %}}).





<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr><tr>
      <td><b>apiVersion</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>stackgres.io/v1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGCluster</td>
      </tr>
      <tr>
      <td><b><a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta">metadata</a></b></td>
      <td>✓</td>
      <td>✓</td>
      <td></td>
      <td>object</td>
      <td>Refer to the Kubernetes API documentation for the fields of the `metadata` field.</td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Specification of the desired behavior of a StackGres cluster.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Current status of a StackGres cluster.<br/>
        </td>
      </tr></tbody>
</table>

## SGCluster.spec
<sup><sup>[↩ Parent](#sgcluster)</sup></sup>

Specification of the desired behavior of a StackGres cluster.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>instances</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of instances for the StackGres cluster. Each instance is a Pod containing one Postgres server.
  Out of all of the Postgres servers, one is elected as the primary, the rest remain as read-only replicas.
<br/>
          <br/>
            <i>Minimum</i>: 0<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpods">pods</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Cluster pod's configuration.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgres">postgres</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allows to configure Postgres features
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscaling">autoscaling</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allows to configure horizontal and vertical Pod autoscaling for the SGCluster's Pods.

Horizontal Pod Autoscaling will use replicas connections usage (active connections / max connections) as the metric to control the upscale or downscale of the replicas.
Horizontal Pod Autoscaling require the [KEDA operator](https://github.com/kedacore/keda) to be installed in the Kuberentes cluster.

Vertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.
Vertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kuberentes cluster.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurations">configurations</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Cluster custom configurations.

<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecdistributedlogs">distributedLogs</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          StackGres features a functionality for all pods to send Postgres, Patroni and PgBouncer logs to a central (distributed) location, which is in turn another Postgres database. Logs can then be accessed via SQL interface or from the web UI. This section controls whether to enable this feature or not. If not enabled, logs are send to the pod's standard output.

<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecinitialdata">initialData</a></b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>object</td>
        <td>
          Cluster initialization data options. Cluster may be initialized empty, or from a backup restoration.

**This field can only be set on creation.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecmanagedsql">managedSql</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allows to reference SQL scripts that will be applied to the cluster live.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecmetadata">metadata</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Metadata information for cluster created resources.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecnonproductionoptions">nonProductionOptions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservices">postgresServices</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Kubernetes [services](https://kubernetes.io/docs/concepts/services-networking/service/) created or managed by StackGres.<br/>
        </td>
      </tr>
      <tr>
        <td><b>profile</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The profile allow to change in a convenient place a set of configuration defaults that affect how the cluster is generated.

All those defaults can be overwritten by setting the correspoinding fields.

Available profiles are:

* `production`:

  Prevents two Pods from running in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `false` by default).
  Sets both limits and requests using `SGInstanceProfile` for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `false` by default).
  Sets requests using the referenced `SGInstanceProfile` for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `false` by default).

* `testing`:

  Allows two Pods to running in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `true` by default).
  Sets both limits and requests using `SGInstanceProfile` for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `false` by default).
  Sets requests using the referenced `SGInstanceProfile` for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `false` by default).

* `development`:

  Allows two Pods from running in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `true` by default).
  Unset both limits and requests for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `true` by default).
  Unsets requests for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `true` by default).

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: production<br/>
        </td>
      </tr>
      <tr>
        <td><b>prometheusAutobind</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          **Deprecated** use instead .spec.configurations.observability.prometheusAutobind.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefrom">replicateFrom</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Make the cluster a read-only standby replica allowing to replicate from another PostgreSQL instance and acting as a rely.

Changing this section is allowed to fix issues or to change the replication source.

Removing this section convert the cluster in a normal cluster where the standby leader is converted into the a primary instance.

<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplication">replication</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allows to configure Postgres replication mode and HA roles groups.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgInstanceProfile</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the [SGInstanceProfile](https://stackgres.io/doc/latest/reference/crd/sginstanceprofile/).

A SGInstanceProfile defines CPU and memory limits. Must exist before creating a cluster.

When no profile is set, a default (1 core, 2 GiB RAM) one is used.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspectoinstallpostgresextensionsindex">toInstallPostgresExtensions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of Postgres extensions to install.

**This section is filled by the operator.**
<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.pods
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Cluster pod's configuration.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodspersistentvolume">persistentVolume</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod's persistent volume configuration.

<br/>
        </td>
      </tr>
      <tr>
        <td><b>customContainers</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of custom application containers that run within the cluster's Pods.

The name used in this section will be prefixed with the string `c-` so that when
  referencing them in the .spec.containers section of SGInstanceProfile the name used
  have to be prepended with the same prefix.

**Changing this field may require a restart.**
  
See: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#container-v1-core
<br/>
        </td>
      </tr>
      <tr>
        <td><b>customInitContainers</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of custom application init containers that run within the cluster's Pods. The
  custom init containers will run following the defined sequence as the end of
  cluster's Pods init containers.

The name used in this section will be prefixed with the string `c-` so that when
  referencing them in the .spec.containers section of SGInstanceProfile the name used
  have to be prepended with the same prefix.

**Changing this field may require a restart.**
  
See: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#container-v1-core
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodscustominitvolumemountskeyindex">customInitVolumeMounts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string][]object</td>
        <td>
          Custom Pod volumes to mount into the specified init container's filesystem.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodscustomvolumemountskeyindex">customVolumeMounts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string][]object</td>
        <td>
          Custom Pod volumes to mount into the specified container's filesystem.<br/>
        </td>
      </tr>
      <tr>
        <td><b>customVolumes</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of custom volumes that may be used along with any container defined in
  customInitContainers or customContainers sections.

The name used in this section will be prefixed with the string `c-` so that when
  referencing them in the customInitContainers or customContainers sections the name used
  have to be prepended with the same prefix.

Only the following volume types are allowed: configMap, downwardAPI, emptyDir,
  gitRepo, glusterfs, hostPath, nfs, projected and secret

**Changing this field may require a restart.**
  
See: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#volume-v1-core
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableConnectionPooling</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          If set to `true`, avoids creating a connection pooling (using [PgBouncer](https://www.pgbouncer.org/)) sidecar.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableEnvoy</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          If set to `true`, avoids creating the `envoy` sidecar. This sidecar is used as the endge proxy for the cluster's Pods providing extra metrics to the monitoring layer.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableMetricsExporter</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          **Deprecated** use instead .spec.configurations.observability.disableMetrics.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disablePostgresUtil</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          If set to `true`, avoids creating the `postgres-util` sidecar. This sidecar contains usual Postgres administration utilities *that are not present in the main (`patroni`) container*, like `psql`. Only disable if you know what you are doing.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodslivenessprobe">livenessProbe</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.<br/>
        </td>
      </tr>
      <tr>
        <td><b>managementPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          managementPolicy controls how pods are created during initial scale up, when replacing pods
  on nodes, or when scaling down. The default policy is `OrderedReady`, where pods are created
  in increasing order (pod-0, then pod-1, etc) and the controller will wait until each pod is
  ready before continuing. When scaling down, the pods are removed in the opposite order.
  The alternative policy is `Parallel` which will create pods in parallel to match the desired
  scale without waiting, and on scale down will delete all pods at once.
<br/>
          <br/>
            <i>Default</i>: OrderedReady<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsreadinessprobe">readinessProbe</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsresources">resources</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod custom resources configuration.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsscheduling">scheduling</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Pod custom scheduling, affinity and topology spread constratins configuration.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b>terminationGracePeriodSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Optional duration in seconds the pod needs to terminate gracefully. May be decreased in delete request. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). If this value is nil, the default grace period will be used instead. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. Defaults to 30 seconds.<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.pods.persistentVolume
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

Pod's persistent volume configuration.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    persistentVolume:
      size: '5Gi'
      storageClass: default
```


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>size</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Size of the PersistentVolume set for each instance of the cluster. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>storageClass</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.
<br/>
        </td>
      </tr></tbody>
</table>

 SGCluster.spec.pods.customInitVolumeMounts[key][index]
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

VolumeMount describes a mounting of a Volume within a container.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#volumemount-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>mountPath</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Path within the container at which the volume should be mounted.  Must not contain ':'.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This must match the Name of a Volume.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mountPropagation</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          mountPropagation determines how mounts are propagated from the host to container and the other way around. When not set, MountPropagationNone is used. This field is beta in 1.10. When RecursiveReadOnly is set to IfPossible or to Enabled, MountPropagation must be None or unspecified (which defaults to None).<br/>
        </td>
      </tr>
      <tr>
        <td><b>readOnly</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Mounted read-only if true, read-write otherwise (false or unspecified). Defaults to false.<br/>
        </td>
      </tr>
      <tr>
        <td><b>recursiveReadOnly</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RecursiveReadOnly specifies whether read-only mounts should be handled recursively.

If ReadOnly is false, this field has no meaning and must be unspecified.

If ReadOnly is true, and this field is set to Disabled, the mount is not made recursively read-only.  If this field is set to IfPossible, the mount is made recursively read-only, if it is supported by the container runtime.  If this field is set to Enabled, the mount is made recursively read-only if it is supported by the container runtime, otherwise the pod will not be started and an error will be generated to indicate the reason.

If this field is set to IfPossible or Enabled, MountPropagation must be set to None (or be unspecified, which defaults to None).

If this field is not specified, it is treated as an equivalent of Disabled.<br/>
        </td>
      </tr>
      <tr>
        <td><b>subPath</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Path within the volume from which the container's volume should be mounted. Defaults to "" (volume's root).<br/>
        </td>
      </tr>
      <tr>
        <td><b>subPathExpr</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Expanded path within the volume from which the container's volume should be mounted. Behaves similarly to SubPath but environment variable references $(VAR_NAME) are expanded using the container's environment. Defaults to "" (volume's root). SubPathExpr and SubPath are mutually exclusive.<br/>
        </td>
      </tr></tbody>
</table>

 SGCluster.spec.pods.customVolumeMounts[key][index]
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

VolumeMount describes a mounting of a Volume within a container.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#volumemount-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>mountPath</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Path within the container at which the volume should be mounted.  Must not contain ':'.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This must match the Name of a Volume.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mountPropagation</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          mountPropagation determines how mounts are propagated from the host to container and the other way around. When not set, MountPropagationNone is used. This field is beta in 1.10. When RecursiveReadOnly is set to IfPossible or to Enabled, MountPropagation must be None or unspecified (which defaults to None).<br/>
        </td>
      </tr>
      <tr>
        <td><b>readOnly</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Mounted read-only if true, read-write otherwise (false or unspecified). Defaults to false.<br/>
        </td>
      </tr>
      <tr>
        <td><b>recursiveReadOnly</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RecursiveReadOnly specifies whether read-only mounts should be handled recursively.

If ReadOnly is false, this field has no meaning and must be unspecified.

If ReadOnly is true, and this field is set to Disabled, the mount is not made recursively read-only.  If this field is set to IfPossible, the mount is made recursively read-only, if it is supported by the container runtime.  If this field is set to Enabled, the mount is made recursively read-only if it is supported by the container runtime, otherwise the pod will not be started and an error will be generated to indicate the reason.

If this field is set to IfPossible or Enabled, MountPropagation must be set to None (or be unspecified, which defaults to None).

If this field is not specified, it is treated as an equivalent of Disabled.<br/>
        </td>
      </tr>
      <tr>
        <td><b>subPath</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Path within the volume from which the container's volume should be mounted. Defaults to "" (volume's root).<br/>
        </td>
      </tr>
      <tr>
        <td><b>subPathExpr</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Expanded path within the volume from which the container's volume should be mounted. Behaves similarly to SubPath but environment variable references $(VAR_NAME) are expanded using the container's environment. Defaults to "" (volume's root). SubPathExpr and SubPath are mutually exclusive.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.pods.livenessProbe
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failureThreshold</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialDelaySeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>periodSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>successThreshold</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>terminationGracePeriodSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr>
      <tr>
        <td><b>timeoutSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.pods.readinessProbe
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failureThreshold</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialDelaySeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>periodSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>successThreshold</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>terminationGracePeriodSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr>
      <tr>
        <td><b>timeoutSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.pods.resources
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

Pod custom resources configuration.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsresourcescontainerskey">containers</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]object</td>
        <td>
          Resources configuration to be merged with the specified container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableResourcesRequestsSplitFromTotal</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          When set to `true` the resources requests values in fields `SGInstanceProfile.spec.requests.cpu` and `SGInstanceProfile.spec.requests.memory` will represent the resources
 requests of the patroni container and the total resources requests calculated by adding the resources requests of all the containers (including the patroni container).

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b>enableClusterLimitsRequirements</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          When enabled resource limits for containers other than the patroni container wil be set just like for patroni contianer as specified in the SGInstanceProfile.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>failWhenTotalIsHigher</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the reconciliation of the cluster will fail if `disableResourcesRequestsSplitFromTotal` is not set or set to `false` and the sum of the CPU or memory
 of all the containers except patroni is equals or higher than the total specified in `SGInstanceProfile.spec.requests.cpu` or `SGInstanceProfile.spec.requests.memory`.

When `false` (the default) and `disableResourcesRequestsSplitFromTotal` is not set or set to `false` and the sum of the CPU or memory
 of all the containers except patroni is equals or higher than the total specified in `SGInstanceProfile.spec.requests.cpu` or `SGInstanceProfile.spec.requests.memory`
 then the patroni container resources will be set to 0.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsresourcesinitcontainerskey">initContainers</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]object</td>
        <td>
          Resources configuration to be merged with the specified init container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.<br/>
        </td>
      </tr></tbody>
</table>

 SGCluster.spec.pods.resources.containers[key]
<sup><sup>[↩ Parent](#sgclusterspecpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsresourcescontainerskeyclaimsindex">claims</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.

This is an alpha field and requires enabling the DynamicResourceAllocation feature gate.

This field is immutable. It can only be set for containers.<br/>
        </td>
      </tr>
      <tr>
        <td><b>limits</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/<br/>
        </td>
      </tr>
      <tr>
        <td><b>requests</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/<br/>
        </td>
      </tr></tbody>
</table>

## SGCluster.spec.pods.resources.containers[key].claims[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsresourcescontainerskey)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.<br/>
        </td>
      </tr>
      <tr>
        <td><b>request</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.<br/>
        </td>
      </tr></tbody>
</table>

 SGCluster.spec.pods.resources.initContainers[key]
<sup><sup>[↩ Parent](#sgclusterspecpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsresourcesinitcontainerskeyclaimsindex">claims</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.

This is an alpha field and requires enabling the DynamicResourceAllocation feature gate.

This field is immutable. It can only be set for containers.<br/>
        </td>
      </tr>
      <tr>
        <td><b>limits</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/<br/>
        </td>
      </tr>
      <tr>
        <td><b>requests</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/<br/>
        </td>
      </tr></tbody>
</table>

## SGCluster.spec.pods.resources.initContainers[key].claims[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsresourcesinitcontainerskey)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.<br/>
        </td>
      </tr>
      <tr>
        <td><b>request</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.pods.scheduling
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

Pod custom scheduling, affinity and topology spread constratins configuration.

**Changing this field may require a restart.**


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackup">backup</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Backup Pod custom scheduling and affinity configuration.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinity">nodeAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#nodeaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          NodeSelector is a selector which must be true for the pod to fit on a node. Selector which must match a node's labels for the pod to be scheduled on that node. More info: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinity">podAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinity">podAntiAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podantiaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>priorityClassName</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          If specified, indicates the pod's priority. "system-node-critical" and "system-cluster-critical" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingtolerationsindex">tolerations</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If specified, the pod's tolerations.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#toleration-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingtopologyspreadconstraintsindex">topologySpreadConstraints</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#topologyspreadconstraint-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

Backup Pod custom scheduling and affinity configuration.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinity">nodeAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#nodeaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          NodeSelector is a selector which must be true for the pod to fit on a node. Selector which must match a node's labels for the pod to be scheduled on that node. More info: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinity">podAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinity">podAntiAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podantiaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>priorityClassName</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          If specified, indicates the pod's priority. "system-node-critical" and "system-cluster-critical" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuptolerationsindex">tolerations</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If specified, the pod's tolerations.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#toleration-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#nodeaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference">preference</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          Weight associated with matching the corresponding nodeSelectorTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex">nodeSelectorTerms</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          Required. A list of node selector terms. The terms are ORed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackup)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          weight associated with matching the corresponding podAffinityTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackup)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podantiaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          weight associated with matching the corresponding podAffinityTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.backup.tolerations[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackup)</sup></sup>

The pod this Toleration is attached to tolerates any taint that matches the triple <key,value,effect> using the matching operator <operator>.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>effect</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Effect indicates the taint effect to match. Empty means match all taint effects. When specified, allowed values are NoSchedule, PreferNoSchedule and NoExecute.<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Key is the taint key that the toleration applies to. Empty means match all taint keys. If the key is empty, operator must be Exists; this combination means to match all values and all keys.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Operator represents a key's relationship to the value. Valid operators are Exists and Equal. Defaults to Equal. Exists is equivalent to wildcard for value, so that a pod can tolerate all taints of a particular category.<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerationSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          TolerationSeconds represents the period of time the toleration (which must be of effect NoExecute, otherwise this field is ignored) tolerates the taint. By default, it is not set, which means tolerate the taint forever (do not evict). Zero and negative values will be treated as 0 (evict immediately) by the system.<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr>
      <tr>
        <td><b>value</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Value is the taint value the toleration matches to. If the operator is Exists, the value should be empty, otherwise just a regular string.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#nodeaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference">preference</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          Weight associated with matching the corresponding nodeSelectorTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex">nodeSelectorTerms</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          Required. A list of node selector terms. The terms are ORed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          weight associated with matching the corresponding podAffinityTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podantiaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          weight associated with matching the corresponding podAffinityTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

The pod this Toleration is attached to tolerates any taint that matches the triple <key,value,effect> using the matching operator <operator>.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>effect</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Effect indicates the taint effect to match. Empty means match all taint effects. When specified, allowed values are NoSchedule, PreferNoSchedule and NoExecute.<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Key is the taint key that the toleration applies to. Empty means match all taint keys. If the key is empty, operator must be Exists; this combination means to match all values and all keys.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Operator represents a key's relationship to the value. Valid operators are Exists and Equal. Defaults to Equal. Exists is equivalent to wildcard for value, so that a pod can tolerate all taints of a particular category.<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerationSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          TolerationSeconds represents the period of time the toleration (which must be of effect NoExecute, otherwise this field is ignored) tolerates the taint. By default, it is not set, which means tolerate the taint forever (do not evict). Zero and negative values will be treated as 0 (evict immediately) by the system.<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr>
      <tr>
        <td><b>value</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Value is the taint value the toleration matches to. If the operator is Exists, the value should be empty, otherwise just a regular string.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.topologySpreadConstraints[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

TopologySpreadConstraint specifies how to spread matching pods among the given topology.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxSkew</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          MaxSkew describes the degree to which pods may be unevenly distributed. When `whenUnsatisfiable=DoNotSchedule`, it is the maximum permitted difference between the number of matching pods in the target topology and the global minimum. The global minimum is the minimum number of matching pods in an eligible domain or zero if the number of eligible domains is less than MinDomains. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 2/2/1: In this case, the global minimum is 1. | zone1 | zone2 | zone3 | |  P P  |  P P  |   P   | - if MaxSkew is 1, incoming pod can only be scheduled to zone3 to become 2/2/2; scheduling it onto zone1(zone2) would make the ActualSkew(3-1) on zone1(zone2) violate MaxSkew(1). - if MaxSkew is 2, incoming pod can be scheduled onto any zone. When `whenUnsatisfiable=ScheduleAnyway`, it is used to give higher precedence to topologies that satisfy it. It's a required field. Default value is 1 and 0 is not allowed.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          TopologyKey is the key of node labels. Nodes that have a label with this key and identical values are considered to be in the same topology. We consider each <key, value> as a "bucket", and try to put balanced number of pods into each bucket. We define a domain as a particular instance of a topology. Also, we define an eligible domain as a domain whose nodes meet the requirements of nodeAffinityPolicy and nodeTaintsPolicy. e.g. If TopologyKey is "kubernetes.io/hostname", each Node is a domain of that topology. And, if TopologyKey is "topology.kubernetes.io/zone", each zone is a domain of that topology. It's a required field.<br/>
        </td>
      </tr>
      <tr>
        <td><b>whenUnsatisfiable</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          WhenUnsatisfiable indicates how to deal with a pod if it doesn't satisfy the spread constraint. - DoNotSchedule (default) tells the scheduler not to schedule it. - ScheduleAnyway tells the scheduler to schedule the pod in any location,
  but giving higher precedence to topologies that would help reduce the
  skew.
A constraint is considered "Unsatisfiable" for an incoming pod if and only if every possible node assignment for that pod would violate "MaxSkew" on some topology. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 3/1/1: | zone1 | zone2 | zone3 | | P P P |   P   |   P   | If WhenUnsatisfiable is set to DoNotSchedule, incoming pod can only be scheduled to zone2(zone3) to become 3/2/1(3/1/2) as ActualSkew(2-1) on zone2(zone3) satisfies MaxSkew(1). In other words, the cluster can still be imbalanced, but scheduler won't make it *more* imbalanced. It's a required field.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingtopologyspreadconstraintsindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select the pods over which spreading will be calculated. The keys are used to lookup values from the incoming pod labels, those key-value labels are ANDed with labelSelector to select the group of existing pods over which spreading will be calculated for the incoming pod. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. MatchLabelKeys cannot be set when LabelSelector isn't set. Keys that don't exist in the incoming pod labels will be ignored. A null or empty list means only match against labelSelector.

This is a beta field and requires the MatchLabelKeysInPodTopologySpread feature gate to be enabled (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>minDomains</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>integer</td>
        <td>
          MinDomains indicates a minimum number of eligible domains. When the number of eligible domains with matching topology keys is less than minDomains, Pod Topology Spread treats "global minimum" as 0, and then the calculation of Skew is performed. And when the number of eligible domains with matching topology keys equals or greater than minDomains, this value has no effect on scheduling. As a result, when the number of eligible domains is less than minDomains, scheduler won't schedule more than maxSkew Pods to those domains. If value is nil, the constraint behaves as if MinDomains is equal to 1. Valid values are integers greater than 0. When value is not nil, WhenUnsatisfiable must be DoNotSchedule.

For example, in a 3-zone cluster, MaxSkew is set to 2, MinDomains is set to 5 and pods with the same labelSelector spread as 2/2/2: | zone1 | zone2 | zone3 | |  P P  |  P P  |  P P  | The number of domains is less than 5(MinDomains), so "global minimum" is treated as 0. In this situation, new pod with the same labelSelector cannot be scheduled, because computed skew will be 3(3 - 0) if new Pod is scheduled to any of the three zones, it will violate MaxSkew.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeAffinityPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          NodeAffinityPolicy indicates how we will treat Pod's nodeAffinity/nodeSelector when calculating pod topology spread skew. Options are: - Honor: only nodes matching nodeAffinity/nodeSelector are included in the calculations. - Ignore: nodeAffinity/nodeSelector are ignored. All nodes are included in the calculations.

If this value is nil, the behavior is equivalent to the Honor policy. This is a beta-level feature default enabled by the NodeInclusionPolicyInPodTopologySpread feature flag.<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeTaintsPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          NodeTaintsPolicy indicates how we will treat node taints when calculating pod topology spread skew. Options are: - Honor: nodes without taints, along with tainted nodes for which the incoming pod has a toleration, are included. - Ignore: node taints are ignored. All nodes are included.

If this value is nil, the behavior is equivalent to the Ignore policy. This is a beta-level feature default enabled by the NodeInclusionPolicyInPodTopologySpread feature flag.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.topologySpreadConstraints[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingtopologyspreadconstraintsindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpodsschedulingtopologyspreadconstraintsindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.pods.scheduling.topologySpreadConstraints[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingtopologyspreadconstraintsindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.postgres
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

This section allows to configure Postgres features


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Postgres version used on the cluster. It is either of:
*  The string 'latest', which automatically sets the latest major.minor Postgres version.
*  A major version, like '14' or '13', which sets that major version and the latest minor version.
*  A specific major.minor version, like '14.4'.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresextensionsindex">extensions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          StackGres support deploy of extensions at runtime by simply adding an entry to this array. A deployed extension still
requires the creation in a database using the [`CREATE EXTENSION`](https://www.postgresql.org/docs/current/sql-createextension.html)
statement. After an extension is deployed correctly it will be present until removed and the cluster restarted.

A cluster restart is required for:
* Extensions that requires to add an entry to [`shared_preload_libraries`](https://postgresqlco.nf/en/doc/param/shared_preload_libraries/) configuration parameter.
* Upgrading extensions that overwrite any file that is not the extension''s control file or extension''s script file.
* Removing extensions. Until the cluster is not restarted a removed extension will still be available.
* Install of extensions that require extra mount. After installed the cluster will require to be restarted.

<br/>
        </td>
      </tr>
      <tr>
        <td><b>flavor</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          Postgres flavor used on the cluster. It is either of:

  * `vanilla` will use the [Official Postgres](https://www.postgresql.org/)
  * `babelfish` will use the [Babelfish for Postgres](https://babelfish-for-postgresql.github.io/babelfish-for-postgresql/).

If not specified then the vanilla Postgres will be used for the cluster.

**This field can only be set on creation.**
<br/>
          <br/>
            <i>Default</i>: vanilla<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresssl">ssl</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allows to use SSL when connecting to Postgres

<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.postgres.extensions[index]
<sup><sup>[↩ Parent](#sgclusterspecpostgres)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the extension to deploy.<br/>
        </td>
      </tr>
      <tr>
        <td><b>publisher</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The id of the publisher of the extension to deploy. If not specified `com.ongres` will be used by default.<br/>
          <br/>
            <i>Default</i>: com.ongres<br/>
        </td>
      </tr>
      <tr>
        <td><b>repository</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The repository base URL from where to obtain the extension to deploy.

**This section is filled by the operator.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The version of the extension to deploy. If not specified version of `stable` channel will be used by default and if only a version is available that one will be used.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.postgres.ssl
<sup><sup>[↩ Parent](#sgclusterspecpostgres)</sup></sup>

This section allows to use SSL when connecting to Postgres

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  postgres:
    ssl:
      enabled: true
      certificateSecretKeySelector:
        name: stackgres-secrets
        key: cert
      privateKeySecretKeySelector:
        name: stackgres-secrets
        key: key
```


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgressslcertificatesecretkeyselector">certificateSecretKeySelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Secret key selector for the certificate or certificate chain used for SSL connections.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>enabled</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Allow to enable SSL for connections to Postgres. By default is `false`.

If `true` certificate and private key will be auto-generated unless fields `certificateSecretKeySelector` and `privateKeySecretKeySelector` are specified.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgressslprivatekeysecretkeyselector">privateKeySecretKeySelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Secret key selector for the private key used for SSL connections.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.postgres.ssl.certificateSecretKeySelector
<sup><sup>[↩ Parent](#sgclusterspecpostgresssl)</sup></sup>

Secret key selector for the certificate or certificate chain used for SSL connections.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of Secret that contains the certificate or certificate chain for SSL connections
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of Secret that contains the certificate or certificate chain for SSL connections
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.postgres.ssl.privateKeySecretKeySelector
<sup><sup>[↩ Parent](#sgclusterspecpostgresssl)</sup></sup>

Secret key selector for the private key used for SSL connections.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of Secret that contains the private key for SSL connections
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of Secret that contains the private key for SSL connections
<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.autoscaling
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

This section allows to configure horizontal and vertical Pod autoscaling for the SGCluster's Pods.

Horizontal Pod Autoscaling will use replicas connections usage (active connections / max connections) as the metric to control the upscale or downscale of the replicas.
Horizontal Pod Autoscaling require the [KEDA operator](https://github.com/kedacore/keda) to be installed in the Kuberentes cluster.

Vertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.
Vertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kuberentes cluster.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalinghorizontal">horizontal</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure horizontal Pod autoscaling aspects.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingmaxallowed">maxAllowed</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to define the higher bound for Pod resources of patroni, pgbouncer and envoy containers
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total maximum number of instances that the SGCluster will have (including the primary instance).

This field is ignored when horizontal Pod autoscaling is disabled.
<br/>
          <br/>
            <i>Minimum</i>: 2<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingminallowed">minAllowed</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to define the lower bound for Pod resources of patroni, pgbouncer and envoy containers
<br/>
        </td>
      </tr>
      <tr>
        <td><b>minInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total minimum number of instances that the SGCluster will have (including the primary instance).

This field is ignored when horizontal Pod autoscaling is disabled.
<br/>
          <br/>
            <i>Minimum</i>: 2<br/>
        </td>
      </tr>
      <tr>
        <td><b>mode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Allow to enable or disable any of horizontal and vertical Pod autoscaling.

Possible values are:
* `all`: both horizontal and vertical Pod autoscaling will be enabled (default)
* `horizontal`: only horizontal Pod autoscaling will be enabled
* `vertical`: only vertical Pod autoscaling will be enabled
* `none`: all autoscaling will be disabled
<br/>
          <br/>
            <i>Default</i>: all<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingvertical">vertical</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure vertical Pod autoscaling aspects.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.autoscaling.horizontal
<sup><sup>[↩ Parent](#sgclusterspecautoscaling)</sup></sup>

Section to configure horizontal Pod autoscaling aspects.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>cooldownPeriod</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The period in seconds before the downscale of replica instances can be triggered.
<br/>
          <br/>
            <i>Default</i>: 300<br/>
        </td>
      </tr>
      <tr>
        <td><b>pollingInterval</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The interval in seconds to check if the scaleup or scaledown have to be triggered.
<br/>
          <br/>
            <i>Default</i>: 30<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicasConnectionsUsageMetricType</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The metric type for connections used metric. See https://keda.sh/docs/latest/concepts/scaling-deployments/#triggers
<br/>
          <br/>
            <i>Default</i>: AverageValue<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicasConnectionsUsageTarget</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target value for replicas connections used in order to trigger the upscale of replica instances.
<br/>
          <br/>
            <i>Default</i>: 0.8<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.autoscaling.maxAllowed
<sup><sup>[↩ Parent](#sgclusterspecautoscaling)</sup></sup>

Allow to define the higher bound for Pod resources of patroni, pgbouncer and envoy containers


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingmaxallowedenvoy">envoy</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to define the higher bound for Pod resources of envoy container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingmaxallowedpatroni">patroni</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to define the higher bound for Pod resources of patroni container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingmaxallowedpgbouncer">pgbouncer</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to define the higher bound for Pod resources of pgbouncer container
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.autoscaling.maxAllowed.envoy
<sup><sup>[↩ Parent](#sgclusterspecautoscalingmaxallowed)</sup></sup>

Allow to define the higher bound for Pod resources of envoy container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>cpu</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The maximum allowed CPU for the envoy container<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The maximum allowed memory for the envoy container<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.autoscaling.maxAllowed.patroni
<sup><sup>[↩ Parent](#sgclusterspecautoscalingmaxallowed)</sup></sup>

Allow to define the higher bound for Pod resources of patroni container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>cpu</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The maximum allowed CPU for the patroni container<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The maximum allowed memory for the patroni container<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.autoscaling.maxAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgclusterspecautoscalingmaxallowed)</sup></sup>

Allow to define the higher bound for Pod resources of pgbouncer container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>cpu</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The maximum allowed CPU for the pgbouncer container<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The maximum allowed memory for the pgbouncer container<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.autoscaling.minAllowed
<sup><sup>[↩ Parent](#sgclusterspecautoscaling)</sup></sup>

Allow to define the lower bound for Pod resources of patroni, pgbouncer and envoy containers


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingminallowedenvoy">envoy</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to define the lower bound for Pod resources of envoy container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingminallowedpatroni">patroni</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to define the lower bound for Pod resources of patroni container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecautoscalingminallowedpgbouncer">pgbouncer</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to define the lower bound for Pod resources of pgbouncer container
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.autoscaling.minAllowed.envoy
<sup><sup>[↩ Parent](#sgclusterspecautoscalingminallowed)</sup></sup>

Allow to define the lower bound for Pod resources of envoy container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>cpu</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The minimum allowed CPU for the envoy container<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The minimum allowed memory for the envoy container<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.autoscaling.minAllowed.patroni
<sup><sup>[↩ Parent](#sgclusterspecautoscalingminallowed)</sup></sup>

Allow to define the lower bound for Pod resources of patroni container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>cpu</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The minimum allowed CPU for the patroni container<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The minimum allowed memory for the patroni container<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.autoscaling.minAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgclusterspecautoscalingminallowed)</sup></sup>

Allow to define the lower bound for Pod resources of pgbouncer container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>cpu</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The minimum allowed CPU for the pgbouncer container<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The minimum allowed memory for the pgbouncer container<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.autoscaling.vertical
<sup><sup>[↩ Parent](#sgclusterspecautoscaling)</sup></sup>

Section to configure vertical Pod autoscaling aspects.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>recommender</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Recommender responsible for generating recommendation for vertical Pod autoscaling. If not specified the default one will be used.
<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.configurations
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Cluster custom configurations.

**Example:**

``` yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  configurations:
    sgPostgresConfig: 'postgresconf'
    sgPoolingConfig: 'pgbouncerconf'
    backups:
    - sgObjectStorage: 'backupconf'
```


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationsbackupsindex">backups</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          List of backups configurations for this SGCluster
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationsbinding">binding</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allows to specify the properties of [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).
 If not specified, then some default will be used.

For more information see https://servicebinding.io/spec/core/1.0.0/
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentials">credentials</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to specify custom credentials for Postgres users and Patroni REST API
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationsobservability">observability</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to specify Observability configuration (related to logs, metrics and traces)<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationspatroni">patroni</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to specify Patroni configuration that will extend the generated one<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationspooling">pooling</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationspostgres">postgres</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section for postgres configuration.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationspostgresexporter">postgresExporter</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgPoolingConfig</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig) used for this cluster.

Each pod contains a sidecar with a connection pooler (currently: [PgBouncer](https://www.pgbouncer.org/)). The connection pooler is implemented as a sidecar.

If not set, a default configuration will be used. Disabling connection pooling altogether is possible if the disableConnectionPooling property of the pods object is set to true.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgPostgresConfig</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig) used for the cluster.

It must exist. When not set, a default Postgres config, for the major version selected, is used.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.configurations.backups[index]
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Backup configuration for this SGCluster


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgObjectStorage</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the [SGObjectStorage](https://stackgres.io/doc/latest/reference/crd/sgobjectstorage) to use for the cluster.

It defines the location in which the the backups will be stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>compression</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>enum</td>
        <td>
          Specifies the backup compression algorithm. Possible options are: lz4, lzma, zstd, brotli. The default method is `lz4`. LZ4 is the fastest method, but compression ratio is the worst. LZMA is way slower, but it compresses backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio, being about 3 times better than LZ4.
<br/>
          <br/>
            <i>Enum</i>: lz4, lzma, zstd, brotli<br/>
        </td>
      </tr>
      <tr>
        <td><b>cronSchedule</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Continuous Archiving backups are composed of periodic *base backups* and all the WAL segments produced in between those base backups. This parameter specifies at what time and with what frequency to start performing a new base backup.

Use cron syntax (`m h dom mon dow`) for this parameter, i.e., 5 values separated by spaces:
*  `m`: minute, 0 to 59.
*  `h`: hour, 0 to 23.
*  `dom`: day of month, 1 to 31 (recommended not to set it higher than 28).
*  `mon`: month, 1 to 12.
*  `dow`: day of week, 0 to 7 (0 and 7 both represent Sunday).

Also ranges of values (`start-end`), the symbol `*` (meaning `first-last`) or even `*/N`, where `N` is a number, meaning ""every `N`, may be used. All times are UTC. It is recommended to avoid 00:00 as base backup time, to avoid overlapping with any other external operations happening at this time.

If not set, full backups are never performed automatically.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>fastVolumeSnapshot</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If specified SGBackup will create a backup forcing a fast start (by setting parameter `fast` to `true` when calling `pg_backup_start`) that will reduce the time the backups may take at the expense of more IO usage.

See also https://www.postgresql.org/docs/current/continuous-archiving.html#BACKUP-LOWLEVEL-BASE-BACKUP
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The maximum number of retries the backup operation is allowed to do after a failure.

A value of `0` (zero) means no retries are made. Defaults to: `3`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>path</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The path were the backup is stored. If not set this field is filled up by the operator.

When provided will indicate were the backups and WAL files will be stored.

> **WARNING**: Most users should leave this field empty since having it manually set could be dangerous. If the value is repeated due to re-creating an SGCluster or
 re-using the same value in another SGCluster and you may get a mixed WAL history with unrecoverable backups.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationsbackupsindexperformance">performance</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration that affects the backup network and disk usage performance.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>reconciliationTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Allow to set a timeout for the reconciliation process that take place after the backup.

If not set defaults to 300 (5 minutes). If set to 0 it will disable timeout.

Failure of reconciliation will not make the backup fail and will be re-tried the next time a SGBackup
 or shecduled backup Job take place.
<br/>
          <br/>
            <i>Default</i>: 300<br/>
        </td>
      </tr>
      <tr>
        <td><b>retainWalsForUnmanagedLifecycle</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If specified, WAL created after any unmanaged lifecycle backups will be retained.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>retention</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          When an automatic retention policy is defined to delete old base backups, this parameter specifies the number of base backups to keep, in a sliding window.

Consequently, the time range covered by backups is `periodicity*retention`, where `periodicity` is the separation between backups as specified by the `cronSchedule` property.

Default is 5.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
            <i>Default</i>: 5<br/>
        </td>
      </tr>
      <tr>
        <td><b>timeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Allow to set a timeout for the backup creation.

If not set it will be disabled and the backup operation will continue until the backup completes or fail. If set to 0 is the same as not being set.

Make sure to set a reasonable high value in order to allow for any unexpected delays during backup creation (network low bandwidth, disk low throughput and so forth).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>useVolumeSnapshot</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If specified SGBackup will use VolumeSnapshot to create backups.

This functionality still require to store WAL files in an SGObjectStorage but could result in much faster backups and restore of those backups.

See also https://kubernetes.io/docs/concepts/storage/volume-snapshots/
<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumeSnapshotClass</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the VolumeSnaphostClass to use to create the VolumeSnapshot for backups.

See also https://kubernetes.io/docs/concepts/storage/volume-snapshots/
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.backups[index].performance
<sup><sup>[↩ Parent](#sgclusterspecconfigurationsbackupsindex)</sup></sup>

Configuration that affects the backup network and disk usage performance.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>downloadConcurrency</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of file to read and 10.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxDiskBandwidth</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Maximum disk read I/O when performing a backup. In bytes (per second).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxNetworkBandwidth</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Maximum storage upload bandwidth used when storing a backup. In bytes (per second).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>uploadConcurrency</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Backup storage may use several concurrent streams to store the data. This parameter configures the number of parallel streams to use. By default, it's set to 16.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr>
      <tr>
        <td><b>uploadDiskConcurrency</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Backup storage may use several concurrent streams to store the data. This parameter configures the number of parallel streams to use to reading from disk. By default, it's set to 1.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.configurations.binding
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

This section allows to specify the properties of [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).
 If not specified, then some default will be used.

For more information see https://servicebinding.io/spec/core/1.0.0/


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>database</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Allow to specify the database name. If not specified, then the default value is `postgres`<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationsbindingpassword">password</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to reference Secret that contains the user's password. If not specified, then the superuser password will be used.<br/>
        </td>
      </tr>
      <tr>
        <td><b>provider</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          It's the reference of custom provider name. If not specified, then the default value will be `stackgres`<br/>
        </td>
      </tr>
      <tr>
        <td><b>username</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Allow to specify the username. If not specified, then the superuser username will be used.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.binding.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationsbinding)</sup></sup>

Allow to reference Secret that contains the user's password. If not specified, then the superuser password will be used.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the Secret<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the Secret<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.configurations.credentials
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Allow to specify custom credentials for Postgres users and Patroni REST API


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialspatroni">patroni</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials for patroni REST API.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsusers">users</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the users.

**Changing this field may require a manual modification of the database users to reflect the new values specified.**

In particular you may have to create those users if username is changed or alter password if it is changed. Here are the SQL commands to perform such operation (replace
  default usernames with the new ones and `***` with their respective passwords):

* Superuser username changed:
```
CREATE ROLE postgres;
```
* Superuser password changed:
```
ALTER ROLE postgres WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS PASSWORD '***';
```
* Replication username changed:
```
CREATE ROLE replicator;
```
* Replication password changed:
```
ALTER ROLE replicator WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN REPLICATION NOBYPASSRLS PASSWORD '***';
```
* Authenticator username changed:
```
CREATE ROLE authenticator;
```
* Authenticator password changed:
```
ALTER ROLE authenticator WITH SUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '***';
```

**Changing this field may require a restart.**
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.patroni
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentials)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials for patroni REST API.

**Changing this field may require a restart.**


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialspatronirestapipassword">restApiPassword</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password for the patroni REST API.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.patroni.restApiPassword
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialspatroni)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password for the patroni REST API.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentials)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the users.

**Changing this field may require a manual modification of the database users to reflect the new values specified.**

In particular you may have to create those users if username is changed or alter password if it is changed. Here are the SQL commands to perform such operation (replace
  default usernames with the new ones and `***` with their respective passwords):

* Superuser username changed:
```
CREATE ROLE postgres;
```
* Superuser password changed:
```
ALTER ROLE postgres WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS PASSWORD '***';
```
* Replication username changed:
```
CREATE ROLE replicator;
```
* Replication password changed:
```
ALTER ROLE replicator WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN REPLICATION NOBYPASSRLS PASSWORD '***';
```
* Authenticator username changed:
```
CREATE ROLE authenticator;
```
* Authenticator password changed:
```
ALTER ROLE authenticator WITH SUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '***';
```

**Changing this field may require a restart.**


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsusersauthenticator">authenticator</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsusersreplication">replication</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsuserssuperuser">superuser</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.authenticator
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsusersauthenticatorpassword">password</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsusersauthenticatorusername">username</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.authenticator.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.authenticator.username
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.replication
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsusersreplicationpassword">password</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsusersreplicationusername">username</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.replication.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.replication.username
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.superuser
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsuserssuperuserpassword">password</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationscredentialsuserssuperuserusername">username</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.superuser.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.credentials.users.superuser.username
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.configurations.observability
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Allow to specify Observability configuration (related to logs, metrics and traces)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableMetrics</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          If set to `true`, avoids creating the Prometheus exporter sidecar. Recommended when there's no intention to use internal monitoring.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>prometheusAutobind</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If set to `true`, a PodMonitor is created for each Prometheus instance as specified in the SGConfig.spec.collector.prometheusOperator.monitors section.<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>receiver</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Indicate the receiver name (for type prometheus) in the configuration for the collector scraper (if not specified the default empty name will be used).<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.configurations.patroni
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Allow to specify Patroni configuration that will extend the generated one

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>dynamicConfig</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to specify Patroni dynamic configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/dynamic_configuration.html

The following configuration fields will be ignored:

* synchronous_mode
* synchronous_mode_strict
* failsafe_mode
* postgresql (except pg_hba section)
* standby_cluster

> NOTE: pg_hba section will be appended with some rules required to make the cluster working as expected.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialConfig</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>object</td>
        <td>
          Allow to specify Patroni configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/yaml_configuration.html

The following configuration fields will be ignored:

* name
* namespace
* log
* bootstrap
* citus
* postgresql # with the exception of postgresql.callbacks, postgresql.pre_promote, postgresql.before_stop and postgresql.pg_ctl_timeout
* restapi
* ctl
* watchdog
* tags

**This field can only be set on creation.**
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.configurations.pooling
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationspoolingpgbouncer">pgBouncer</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.pooling.pgBouncer
<sup><sup>[↩ Parent](#sgclusterspecconfigurationspooling)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecconfigurationspoolingpgbouncerpgbouncerini">pgbouncer.ini</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).

**Changing this field may require a restart.**
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.configurations.pooling.pgBouncer.pgbouncer.ini
<sup><sup>[↩ Parent](#sgclusterspecconfigurationspoolingpgbouncer)</sup></sup>

The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).

**Changing this field may require a restart.**


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>databases</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]map[string]string</td>
        <td>
          The `pgbouncer.ini` (Section [databases]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.

Check [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-databases) for more information about supported parameters.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pgbouncer</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          The `pgbouncer.ini` (Section [pgbouncer]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.

Check [pgbouncer configuration](https://www.pgbouncer.org/config.html#generic-settings) for more information about supported parameters
<br/>
        </td>
      </tr>
      <tr>
        <td><b>users</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]map[string]string</td>
        <td>
          The `pgbouncer.ini` (Section [users]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.

Check [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-users) for more information about supported parameters.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.configurations.postgres
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Section for postgres configuration.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>postgresql.conf</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>map[string]string</td>
        <td>
          The `postgresql.conf` parameters that overwrite those specified in the referenced [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig).

**Changing this field may require a restart.**
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.configurations.postgresExporter
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>queries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) queries.yaml configuration.

The queries defined here will overwrite those created by the operator.

**WARNING**: Changing this may beak some of the functionality that depend on the query overwritten.
<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.distributedLogs
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

StackGres features a functionality for all pods to send Postgres, Patroni and PgBouncer logs to a central (distributed) location, which is in turn another Postgres database. Logs can then be accessed via SQL interface or from the web UI. This section controls whether to enable this feature or not. If not enabled, logs are send to the pod's standard output.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  distributedLogs:
    sgDistributedLogs: distributedlogs
```


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>retention</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Define a retention window with the syntax `<integer> (minutes|hours|days|months)` in which log entries are kept.
  Log entries will be removed when they get older more than the double of the specified retention window.

When this field is changed the retention will be applied only to log entries that are newer than the end of
  the retention window previously specified. If no retention window was previously specified it is considered
  to be of 7 days. This means that if previous retention window is of `7 days` new retention configuration will
  apply after UTC timestamp calculated with: `SELECT date_trunc('days', now() at time zone 'UTC') - INTERVAL '7 days'`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgDistributedLogs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the [SGDistributedLogs](https://stackgres.io/doc/latest/reference/crd/sgdistributedlogs/) to use for this cluster. It must exist.
<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.initialData
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Cluster initialization data options. Cluster may be initialized empty, or from a backup restoration.

**This field can only be set on creation.**


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecinitialdatarestore">restore</a></b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>object</td>
        <td>
          This section allows to restore a cluster from an existing copy of the metadata and data.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecinitialdatascriptsindex">scripts</a></b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>[]object</td>
        <td>
          **Deprecated** use instead .spec.managedSql with SGScript.

A list of SQL scripts executed in sequence, exactly once, when the database is bootstrap and/or after restore is completed.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.initialData.restore
<sup><sup>[↩ Parent](#sgclusterspecinitialdata)</sup></sup>

This section allows to restore a cluster from an existing copy of the metadata and data.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>downloadDiskConcurrency</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>integer</td>
        <td>
          The backup fetch process may fetch several streams in parallel. Parallel fetching is enabled when set to a value larger than one.

If not specified it will be interpreted as latest.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecinitialdatarestorefrombackup">fromBackup</a></b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>object</td>
        <td>
          From which backup to restore and how the process is configured

<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.initialData.restore.fromBackup
<sup><sup>[↩ Parent](#sgclusterspecinitialdatarestore)</sup></sup>

From which backup to restore and how the process is configured

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    restore:
      fromBackup:
        name: stackgres-backup
      downloadDiskConcurrency: 1
```


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          When set to the name of an existing [SGBackup](https://stackgres.io/doc/latest/reference/crd/sgbackup), the cluster is initialized by restoring the
  backup data to it. If not set, the cluster is initialized empty. The selected backup must be in the same namespace.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecinitialdatarestorefrombackuppointintimerecovery">pointInTimeRecovery</a></b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>object</td>
        <td>
          Using Point-in-Time Recovery (PITR) it is possible to restore the database to its state at any moment in the past by setting `restoreToTimestamp`
  to a value between the timestamps at which your chosen SGBackup and the subsequent one were taken. If the chosen SGBackup is the latest one, the
  `restoreToTimestamp` value can be between the timestamps at which that last SGBackup was taken and the current one.

See also: https://www.postgresql.org/docs/current/continuous-archiving.html
<br/>
        </td>
      </tr>
      <tr>
        <td><b>target</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          Specify the [recovery_target](https://postgresqlco.nf/doc/en/param/recovery_target/) that specifies that recovery should end as soon as a consistent
  state is reached, i.e., as early as possible. When restoring from an online backup, this means the point where taking the backup ended.

  Technically, this is a string parameter, but 'immediate' is currently the only allowed value.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetInclusive</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>boolean</td>
        <td>
          Specify the [recovery_target_inclusive](https://postgresqlco.nf/doc/en/param/recovery_target_timeline/) to stop recovery just after the specified
  recovery target (true), or just before the recovery target (false). Applies when targetLsn, pointInTimeRecovery, or targetXid is specified. This
  setting controls whether transactions having exactly the target WAL location (LSN), commit time, or transaction ID, respectively, will be included
  in the recovery. Default is true.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetLsn</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          [recovery_target_lsn](https://postgresqlco.nf/doc/en/param/recovery_target_lsn/) specifies the LSN of the write-ahead log location up to which
  recovery will proceed. The precise stopping point is also influenced by targetInclusive. This parameter is parsed using the system data type
  pg_lsn.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetName</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          [recovery_target_name](https://postgresqlco.nf/doc/en/param/recovery_target_name/) specifies the named restore point
  (created with pg_create_restore_point()) to which recovery will proceed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetTimeline</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          Specify the [recovery_target_timeline](https://postgresqlco.nf/doc/en/param/recovery_target_timeline/) to recover into a particular timeline.
  The default is to recover along the same timeline that was current when the base backup was taken. Setting this to latest recovers to the latest
  timeline found in the archive, which is useful in a standby server. Other than that you only need to set this parameter in complex re-recovery
  situations, where you need to return to a state that itself was reached after a point-in-time recovery.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetXid</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          [recovery_target_xid](https://postgresqlco.nf/doc/en/param/recovery_target_xid/) specifies the transaction ID up to which recovery will proceed.
  Keep in mind that while transaction IDs are assigned sequentially at transaction start, transactions can complete in a different numeric order.
  The transactions that will be recovered are those that committed before (and optionally including) the specified one. The precise stopping point
  is also influenced by targetInclusive.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>uid</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          When set to the UID of an existing [SGBackup](https://stackgres.io/doc/latest/reference/crd/sgbackup), the cluster is initialized by restoring the
  backup data to it. If not set, the cluster is initialized empty. This field is deprecated.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.initialData.restore.fromBackup.pointInTimeRecovery
<sup><sup>[↩ Parent](#sgclusterspecinitialdatarestorefrombackup)</sup></sup>

Using Point-in-Time Recovery (PITR) it is possible to restore the database to its state at any moment in the past by setting `restoreToTimestamp`
  to a value between the timestamps at which your chosen SGBackup and the subsequent one were taken. If the chosen SGBackup is the latest one, the
  `restoreToTimestamp` value can be between the timestamps at which that last SGBackup was taken and the current one.

See also: https://www.postgresql.org/docs/current/continuous-archiving.html


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>restoreToTimestamp</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date, that holds UTC date indicating at which point-in-time the database have to be restored.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.initialData.scripts[index]
<sup><sup>[↩ Parent](#sgclusterspecinitialdata)</sup></sup>

**Deprecated** use instead .spec.managedSql with SGScript.

Scripts are executed in auto-commit mode with the user `postgres` in the specified database (or in database `postgres` if not specified).

Fields `script` and `scriptFrom` are mutually exclusive and only one of them is required.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>database</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          Database where the script is executed. Defaults to the `postgres` database, if not specified.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          Name of the script. Must be unique across this SGCluster.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>script</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          Raw SQL script to execute. This field is mutually exclusive with `scriptFrom` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecinitialdatascriptsindexscriptfrom">scriptFrom</a></b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>object</td>
        <td>
          Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.initialData.scripts[index].scriptFrom
<sup><sup>[↩ Parent](#sgclusterspecinitialdatascriptsindex)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecinitialdatascriptsindexscriptfromconfigmapkeyref">configMapKeyRef</a></b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>object</td>
        <td>
          A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecinitialdatascriptsindexscriptfromsecretkeyref">secretKeyRef</a></b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.initialData.scripts[index].scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgclusterspecinitialdatascriptsindexscriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          The key name within the ConfigMap that contains the SQL script to execute.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          The name of the ConfigMap that contains the SQL script to execute.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.initialData.scripts[index].scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgclusterspecinitialdatascriptsindexscriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td></td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.managedSql
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

This section allows to reference SQL scripts that will be applied to the cluster live.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>continueOnSGScriptError</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true, when any entry of any `SGScript` fail will not prevent subsequent `SGScript` from being executed. By default is `false`.<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecmanagedsqlscriptsindex">scripts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of script references that will be executed in sequence.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgclusterspecmanagedsql)</sup></sup>

A script reference. Each version of each entry of the script referenced will be executed exactly once following the sequence defined
  in the referenced script and skipping any script entry that have already been executed.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>id</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The id is immutable and must be unique across all the `SGScript` entries. It is replaced by the operator and is used to identify the `SGScript` entry.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgScript</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A reference to an `SGScript`<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.metadata
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Metadata information for cluster created resources.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecmetadataannotations">annotations</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.

<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecmetadatalabels">labels</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.

<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.metadata.annotations
<sup><sup>[↩ Parent](#sgclusterspecmetadata)</sup></sup>

Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    annotations:
      clusterPods:
        key: value
      primaryService:
        key: value
      replicasService:
        key: value
```


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>allResources</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Annotations to attach to any resource created or managed by StackGres.<br/>
        </td>
      </tr>
      <tr>
        <td><b>clusterPods</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Annotations to attach to pods created or managed by StackGres.<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryService</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) passed to the `-primary` service.<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicasService</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) passed to the `-replicas` service.<br/>
        </td>
      </tr>
      <tr>
        <td><b>services</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Annotations to attach to all services created or managed by StackGres.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.metadata.labels
<sup><sup>[↩ Parent](#sgclusterspecmetadata)</sup></sup>

Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    labels:
      clusterPods:
        customLabel: customLabelValue
      services:
        customLabel: customLabelValue
```


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>clusterPods</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Labels to attach to Pods created or managed by StackGres.<br/>
        </td>
      </tr>
      <tr>
        <td><b>services</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Labels to attach to Services and Endpoints created or managed by StackGres.<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.nonProductionOptions
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableClusterPodAntiAffinity</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          It is a best practice, on non-containerized environments, when running production workloads, to run each database server on a different server (virtual or physical), i.e., not to co-locate more than one database server per host.

The same best practice applies to databases on containers. By default, StackGres will not allow to run more than one StackGres pod on a given Kubernetes node. Set this property to true to allow more than one StackGres pod per node.

This property default value may be changed depending on the value of field `.spec.profile`.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableClusterResourceRequirements</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          It is a best practice, on containerized environments, when running production workloads, to enforce container's resources requirements.

By default, StackGres will configure resource requirements for all the containers. Set this property to true to prevent StackGres from setting container's resources requirements (except for patroni container, see `disablePatroniResourceRequirements`).

This property default value may be changed depending on the value of field `.spec.profile`.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disablePatroniResourceRequirements</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          It is a best practice, on containerized environments, when running production workloads, to enforce container's resources requirements.

The same best practice applies to databases on containers. By default, StackGres will configure resource requirements for patroni container. Set this property to true to prevent StackGres from setting patroni container's resources requirement.

This property default value may be changed depending on the value of field `.spec.profile`.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b>enableSetClusterCpuRequests</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          **Deprecated** this value is ignored and you can consider it as always `true`.

On containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving
  the workload with less cpu than it requires. It also allow to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.

By default, StackGres will configure cpu requirements to have the same limit and request for all the containers. Set this property to true to prevent StackGres from setting container's cpu requirements request equals to the limit (except for patroni container, see `enablePatroniCpuRequests`)
  when `.spec.requests.containers.<container name>.cpu` `.spec.requests.initContainers.<container name>.cpu` is configured in the referenced `SGInstanceProfile`.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>enableSetClusterMemoryRequests</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          **Deprecated** this value is ignored and you can consider it as always `true`.

On containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving
  the workload with less memory than it requires.

By default, StackGres will configure memory requirements to have the same limit and request for all the containers. Set this property to true to prevent StackGres from setting container's memory requirements request equals to the limit (except for patroni container, see `enablePatroniCpuRequests`)
  when `.spec.requests.containers.<container name>.memory` `.spec.requests.initContainers.<container name>.memory` is configured in the referenced `SGInstanceProfile`.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>enableSetPatroniCpuRequests</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          **Deprecated** this value is ignored and you can consider it as always `true`.

On containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving
  the workload with less cpu than it requires. It also allow to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.

By default, StackGres will configure cpu requirements to have the same limit and request for the patroni container. Set this property to true to prevent StackGres from setting patroni container's cpu requirements request equals to the limit
  when `.spec.requests.cpu` is configured in the referenced `SGInstanceProfile`.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>enableSetPatroniMemoryRequests</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>boolean</td>
        <td>
          **Deprecated** this value is ignored and you can consider it as always `true`.

On containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving
  the workload with less memory than it requires.

By default, StackGres will configure memory requirements to have the same limit and request for the patroni container. Set this property to true to prevent StackGres from setting patroni container's memory requirements request equals to the limit
  when `.spec.requests.memory` is configured in the referenced `SGInstanceProfile`.

**Changing this field may require a restart.**
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>enabledFeatureGates</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of StackGres feature gates to enable (not suitable for a production environment).

Available feature gates are:
* `babelfish-flavor`: Allow to use `babelfish` flavor.
<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.postgresServices
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Kubernetes [services](https://kubernetes.io/docs/concepts/services-networking/service/) created or managed by StackGres.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservicesprimary">primary</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configure the service to the primary with the same name as the SGCluster. A legacy service 

Provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the cluster.

See also https://kubernetes.io/docs/concepts/services-networking/service/
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservicesreplicas">replicas</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configure the service to any replica with the name as the SGCluster plus the `-replicas` suffix.

It provides a stable connection (regardless of replica node failures) to any read-only Postgres server of the cluster. Read-only servers are load-balanced via this service.

See also https://kubernetes.io/docs/concepts/services-networking/service/
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.postgresServices.primary
<sup><sup>[↩ Parent](#sgclusterspecpostgresservices)</sup></sup>

Configure the service to the primary with the same name as the SGCluster. A legacy service 

Provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the cluster.

See also https://kubernetes.io/docs/concepts/services-networking/service/


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>allocateLoadBalancerNodePorts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is "true". It may be set to "false" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.<br/>
        </td>
      </tr>
      <tr>
        <td><b>customPorts</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The list of custom ports that will be exposed by the service.

The names of custom ports will be prefixed with the string `c-` so they do not
  conflict with ports defined for the service.

The names of target ports will be prefixed with the string `c-` so that the ports
  that can be referenced in this section will be only those defined under
  .spec.pods.customContainers[].ports sections were names are also prepended with the same
  prefix.

**Changing this field may require a restart.**

See: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#serviceport-v1-core
<br/>
        </td>
      </tr>
      <tr>
        <td><b>enabled</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Specify if the service should be created or not.<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>externalIPs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#allocateloadbalancernodeports-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>externalTrafficPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          externalTrafficPolicy describes how nodes distribute service traffic they receive on one of the Service's "externally-facing" addresses (NodePorts, ExternalIPs, and LoadBalancer IPs). If set to "Local", the proxy will configure the service in a way that assumes that external load balancers will take care of balancing the service traffic between nodes, and so each node will deliver traffic only to the node-local endpoints of the service, without masquerading the client source IP. (Traffic mistakenly sent to a node with no endpoints will be dropped.) The default value, "Cluster", uses the standard behavior of routing to all endpoints evenly (possibly modified by topology and other features). Note that traffic sent to an External IP or LoadBalancer IP from within the cluster will always get "Cluster" semantics, but clients sending to a NodePort from within the cluster may need to take traffic policy into account when picking a node.<br/>
        </td>
      </tr>
      <tr>
        <td><b>healthCheckNodePort</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          healthCheckNodePort specifies the healthcheck nodePort for the service. This only applies when type is set to LoadBalancer and externalTrafficPolicy is set to Local. If a value is specified, is in-range, and is not in use, it will be used.  If not specified, a value will be automatically allocated.  External systems (e.g. load-balancers) can use this port to determine if a given node holds endpoints for this service or not.  If this field is specified when creating a Service which does not need it, creation will fail. This field will be wiped when updating a Service to no longer need it (e.g. changing type). This field cannot be updated once set.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>internalTrafficPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          InternalTrafficPolicy describes how nodes distribute service traffic they receive on the ClusterIP. If set to "Local", the proxy will assume that pods only want to talk to endpoints of the service on the same node as the pod, dropping the traffic if there are no local endpoints. The default value, "Cluster", uses the standard behavior of routing to all endpoints evenly (possibly modified by topology and other features).<br/>
        </td>
      </tr>
      <tr>
        <td><b>ipFamilies</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          IPFamilies is a list of IP families (e.g. IPv4, IPv6) assigned to this service. This field is usually assigned automatically based on cluster configuration and the ipFamilyPolicy field. If this field is specified manually, the requested family is available in the cluster, and ipFamilyPolicy allows it, it will be used; otherwise creation of the service will fail. This field is conditionally mutable: it allows for adding or removing a secondary IP family, but it does not allow changing the primary IP family of the Service. Valid values are "IPv4" and "IPv6".  This field only applies to Services of types ClusterIP, NodePort, and LoadBalancer, and does apply to "headless" services. This field will be wiped when updating a Service to type ExternalName.

This field may hold a maximum of two entries (dual-stack families, in either order).  These families must correspond to the values of the clusterIPs field, if specified. Both clusterIPs and ipFamilies are governed by the ipFamilyPolicy field.<br/>
        </td>
      </tr>
      <tr>
        <td><b>ipFamilyPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          IPFamilyPolicy represents the dual-stack-ness requested or required by this Service. If there is no value provided, then this field will be set to SingleStack. Services can be "SingleStack" (a single IP family), "PreferDualStack" (two IP families on dual-stack configured clusters or a single IP family on single-stack clusters), or "RequireDualStack" (two IP families on dual-stack configured clusters, otherwise fail). The ipFamilies and clusterIPs fields depend on the value of this field. This field will be wiped when updating a service to type ExternalName.<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerClass</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          loadBalancerClass is the class of the load balancer implementation this Service belongs to. If specified, the value of this field must be a label-style identifier, with an optional prefix, e.g. "internal-vip" or "example.com/internal-vip". Unprefixed names are reserved for end-users. This field can only be set when the Service type is 'LoadBalancer'. If not set, the default load balancer implementation is used, today this is typically done through the cloud provider integration, but should apply for any default implementation. If set, it is assumed that a load balancer implementation is watching for Services with a matching class. Any default load balancer implementation (e.g. cloud providers) should ignore Services that set this field. This field can only be set when creating or updating a Service to type 'LoadBalancer'. Once set, it can not be changed. This field will be wiped when a service is updated to a non 'LoadBalancer' type.<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerIP</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Only applies to Service Type: LoadBalancer. This feature depends on whether the underlying cloud-provider supports specifying the loadBalancerIP when a load balancer is created. This field will be ignored if the cloud-provider does not support the feature. Deprecated: This field was under-specified and its meaning varies across implementations. Using it is non-portable and it may not support dual-stack. Users are encouraged to use implementation-specific annotations when available.<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerSourceRanges</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          If specified and supported by the platform, this will restrict traffic through the cloud-provider load-balancer will be restricted to the specified client IPs. This field will be ignored if the cloud-provider does not support the feature." More info: https://kubernetes.io/docs/tasks/access-application-cluster/create-external-load-balancer/<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservicesprimarynodeports">nodePorts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          nodePorts is a list of ports for exposing a cluster services to the outside world<br/>
        </td>
      </tr>
      <tr>
        <td><b>publishNotReadyAddresses</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          publishNotReadyAddresses indicates that any agent which deals with endpoints for this Service should disregard any indications of ready/not-ready. The primary use case for setting this field is for a StatefulSet's Headless Service to propagate SRV DNS records for its Pods for the purpose of peer discovery. The Kubernetes controllers that generate Endpoints and EndpointSlice resources for Services interpret this to mean that all endpoints are considered "ready" even if the Pods themselves are not. Agents which consume only Kubernetes generated endpoints through the Endpoints or EndpointSlice resources can safely assume this behavior.<br/>
        </td>
      </tr>
      <tr>
        <td><b>sessionAffinity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Supports "ClientIP" and "None". Used to maintain session affinity. Enable client IP based session affinity. Must be ClientIP or None. Defaults to None. More info: https://kubernetes.io/docs/concepts/services-networking/service/#virtual-ips-and-service-proxies<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservicesprimarysessionaffinityconfig">sessionAffinityConfig</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#sessionaffinityconfig-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>enum</td>
        <td>
          type determines how the Service is exposed. Defaults to ClusterIP. Valid
options are ClusterIP, NodePort, LoadBalancer and None. "ClusterIP" allocates
a cluster-internal IP address for load-balancing to endpoints.
"NodePort" builds on ClusterIP and allocates a port on every node.
"LoadBalancer" builds on NodePort and creates
an external load-balancer (if supported in the current cloud).
"None" creates an headless service that can be use in conjunction with `.spec.pods.disableEnvoy`
 set to `true` in order to acces the database using a DNS.
More info:
* https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types
* https://kubernetes.io/docs/concepts/services-networking/service/#headless-services
* https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/
<br/>
          <br/>
            <i>Enum</i>: ClusterIP, LoadBalancer, NodePort, None<br/>
            <i>Default</i>: ClusterIP<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.postgresServices.primary.nodePorts
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesprimary)</sup></sup>

nodePorts is a list of ports for exposing a cluster services to the outside world

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>babelfish</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          the node port that will be exposed to connect to Babelfish instance using SQL Server wire-protocol and T-SQL<br/>
        </td>
      </tr>
      <tr>
        <td><b>pgport</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          the node port that will be exposed to connect to Postgres instance<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicationport</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          the node port that will be exposed to connect to Postgres instance for replication purpose<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.postgresServices.primary.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesprimary)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#sessionaffinityconfig-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservicesprimarysessionaffinityconfigclientip">clientIP</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          ClientIPConfig represents the configurations of Client IP based session affinity.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.postgresServices.primary.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesprimarysessionaffinityconfig)</sup></sup>

ClientIPConfig represents the configurations of Client IP based session affinity.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>timeoutSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          timeoutSeconds specifies the seconds of ClientIP type session sticky time. The value must be >0 && <=86400(for 1 day) if ServiceAffinity == "ClientIP". Default value is 10800(for 3 hours).<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.postgresServices.replicas
<sup><sup>[↩ Parent](#sgclusterspecpostgresservices)</sup></sup>

Configure the service to any replica with the name as the SGCluster plus the `-replicas` suffix.

It provides a stable connection (regardless of replica node failures) to any read-only Postgres server of the cluster. Read-only servers are load-balanced via this service.

See also https://kubernetes.io/docs/concepts/services-networking/service/


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>allocateLoadBalancerNodePorts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is "true". It may be set to "false" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.<br/>
        </td>
      </tr>
      <tr>
        <td><b>customPorts</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The list of custom ports that will be exposed by the service.

The names of custom ports will be prefixed with the string `c-` so they do not
  conflict with ports defined for the service.

The names of target ports will be prefixed with the string `c-` so that the ports
  that can be referenced in this section will be only those defined under
  .spec.pods.customContainers[].ports sections were names are also prepended with the same
  prefix.

**Changing this field may require a restart.**

See: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#serviceport-v1-core
<br/>
        </td>
      </tr>
      <tr>
        <td><b>enabled</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Specify if the service should be created or not.<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>externalIPs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#allocateloadbalancernodeports-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>externalTrafficPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          externalTrafficPolicy describes how nodes distribute service traffic they receive on one of the Service's "externally-facing" addresses (NodePorts, ExternalIPs, and LoadBalancer IPs). If set to "Local", the proxy will configure the service in a way that assumes that external load balancers will take care of balancing the service traffic between nodes, and so each node will deliver traffic only to the node-local endpoints of the service, without masquerading the client source IP. (Traffic mistakenly sent to a node with no endpoints will be dropped.) The default value, "Cluster", uses the standard behavior of routing to all endpoints evenly (possibly modified by topology and other features). Note that traffic sent to an External IP or LoadBalancer IP from within the cluster will always get "Cluster" semantics, but clients sending to a NodePort from within the cluster may need to take traffic policy into account when picking a node.<br/>
        </td>
      </tr>
      <tr>
        <td><b>healthCheckNodePort</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          healthCheckNodePort specifies the healthcheck nodePort for the service. This only applies when type is set to LoadBalancer and externalTrafficPolicy is set to Local. If a value is specified, is in-range, and is not in use, it will be used.  If not specified, a value will be automatically allocated.  External systems (e.g. load-balancers) can use this port to determine if a given node holds endpoints for this service or not.  If this field is specified when creating a Service which does not need it, creation will fail. This field will be wiped when updating a Service to no longer need it (e.g. changing type). This field cannot be updated once set.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>internalTrafficPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          InternalTrafficPolicy describes how nodes distribute service traffic they receive on the ClusterIP. If set to "Local", the proxy will assume that pods only want to talk to endpoints of the service on the same node as the pod, dropping the traffic if there are no local endpoints. The default value, "Cluster", uses the standard behavior of routing to all endpoints evenly (possibly modified by topology and other features).<br/>
        </td>
      </tr>
      <tr>
        <td><b>ipFamilies</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          IPFamilies is a list of IP families (e.g. IPv4, IPv6) assigned to this service. This field is usually assigned automatically based on cluster configuration and the ipFamilyPolicy field. If this field is specified manually, the requested family is available in the cluster, and ipFamilyPolicy allows it, it will be used; otherwise creation of the service will fail. This field is conditionally mutable: it allows for adding or removing a secondary IP family, but it does not allow changing the primary IP family of the Service. Valid values are "IPv4" and "IPv6".  This field only applies to Services of types ClusterIP, NodePort, and LoadBalancer, and does apply to "headless" services. This field will be wiped when updating a Service to type ExternalName.

This field may hold a maximum of two entries (dual-stack families, in either order).  These families must correspond to the values of the clusterIPs field, if specified. Both clusterIPs and ipFamilies are governed by the ipFamilyPolicy field.<br/>
        </td>
      </tr>
      <tr>
        <td><b>ipFamilyPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          IPFamilyPolicy represents the dual-stack-ness requested or required by this Service. If there is no value provided, then this field will be set to SingleStack. Services can be "SingleStack" (a single IP family), "PreferDualStack" (two IP families on dual-stack configured clusters or a single IP family on single-stack clusters), or "RequireDualStack" (two IP families on dual-stack configured clusters, otherwise fail). The ipFamilies and clusterIPs fields depend on the value of this field. This field will be wiped when updating a service to type ExternalName.<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerClass</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          loadBalancerClass is the class of the load balancer implementation this Service belongs to. If specified, the value of this field must be a label-style identifier, with an optional prefix, e.g. "internal-vip" or "example.com/internal-vip". Unprefixed names are reserved for end-users. This field can only be set when the Service type is 'LoadBalancer'. If not set, the default load balancer implementation is used, today this is typically done through the cloud provider integration, but should apply for any default implementation. If set, it is assumed that a load balancer implementation is watching for Services with a matching class. Any default load balancer implementation (e.g. cloud providers) should ignore Services that set this field. This field can only be set when creating or updating a Service to type 'LoadBalancer'. Once set, it can not be changed. This field will be wiped when a service is updated to a non 'LoadBalancer' type.<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerIP</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Only applies to Service Type: LoadBalancer. This feature depends on whether the underlying cloud-provider supports specifying the loadBalancerIP when a load balancer is created. This field will be ignored if the cloud-provider does not support the feature. Deprecated: This field was under-specified and its meaning varies across implementations. Using it is non-portable and it may not support dual-stack. Users are encouraged to use implementation-specific annotations when available.<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerSourceRanges</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          If specified and supported by the platform, this will restrict traffic through the cloud-provider load-balancer will be restricted to the specified client IPs. This field will be ignored if the cloud-provider does not support the feature." More info: https://kubernetes.io/docs/tasks/access-application-cluster/create-external-load-balancer/<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservicesreplicasnodeports">nodePorts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          nodePorts is a list of ports for exposing a cluster services to the outside world<br/>
        </td>
      </tr>
      <tr>
        <td><b>publishNotReadyAddresses</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          publishNotReadyAddresses indicates that any agent which deals with endpoints for this Service should disregard any indications of ready/not-ready. The primary use case for setting this field is for a StatefulSet's Headless Service to propagate SRV DNS records for its Pods for the purpose of peer discovery. The Kubernetes controllers that generate Endpoints and EndpointSlice resources for Services interpret this to mean that all endpoints are considered "ready" even if the Pods themselves are not. Agents which consume only Kubernetes generated endpoints through the Endpoints or EndpointSlice resources can safely assume this behavior.<br/>
        </td>
      </tr>
      <tr>
        <td><b>sessionAffinity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Supports "ClientIP" and "None". Used to maintain session affinity. Enable client IP based session affinity. Must be ClientIP or None. Defaults to None. More info: https://kubernetes.io/docs/concepts/services-networking/service/#virtual-ips-and-service-proxies<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservicesreplicassessionaffinityconfig">sessionAffinityConfig</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#sessionaffinityconfig-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>enum</td>
        <td>
          type determines how the Service is exposed. Defaults to ClusterIP. Valid
options are ClusterIP, NodePort, LoadBalancer and None. "ClusterIP" allocates
a cluster-internal IP address for load-balancing to endpoints.
"NodePort" builds on ClusterIP and allocates a port on every node.
"LoadBalancer" builds on NodePort and creates
an external load-balancer (if supported in the current cloud).
"None" creates an headless service that can be use in conjunction with `.spec.pods.disableEnvoy`
 set to `true` in order to acces the database using a DNS.
More info:
* https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types
* https://kubernetes.io/docs/concepts/services-networking/service/#headless-services
* https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/
<br/>
          <br/>
            <i>Enum</i>: ClusterIP, LoadBalancer, NodePort, None<br/>
            <i>Default</i>: ClusterIP<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.postgresServices.replicas.nodePorts
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesreplicas)</sup></sup>

nodePorts is a list of ports for exposing a cluster services to the outside world

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>babelfish</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          the node port that will be exposed to connect to Babelfish instance using SQL Server wire-protocol and T-SQL<br/>
        </td>
      </tr>
      <tr>
        <td><b>pgport</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          the node port that will be exposed to connect to Postgres instance<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicationport</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          the node port that will be exposed to connect to Postgres instance for replication purpose<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.postgresServices.replicas.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesreplicas)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#sessionaffinityconfig-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecpostgresservicesreplicassessionaffinityconfigclientip">clientIP</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          ClientIPConfig represents the configurations of Client IP based session affinity.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.postgresServices.replicas.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesreplicassessionaffinityconfig)</sup></sup>

ClientIPConfig represents the configurations of Client IP based session affinity.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>timeoutSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          timeoutSeconds specifies the seconds of ClientIP type session sticky time. The value must be >0 && <=86400(for 1 day) if ServiceAffinity == "ClientIP". Default value is 10800(for 3 hours).<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.replicateFrom
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Make the cluster a read-only standby replica allowing to replicate from another PostgreSQL instance and acting as a rely.

Changing this section is allowed to fix issues or to change the replication source.

Removing this section convert the cluster in a normal cluster where the standby leader is converted into the a primary instance.

**Example:**

From SGCluster instance:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  replicateFrom:
    instance:
      sgCluster: my-cluster
```
> **Note:** The above example allow to replicate from another SGCluster instance that in the same namespace and the same K8s cluster.
> 
> This option cannot be combined with external instance, storage and users.

From external instance:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  replicateFrom:
    instance:
      external:
        host: ${HOST_IP}
        port: 5433
    users:
      superuser:
        username:
          name: pg-origin-secret
          key: superuser-username
        password:
          name: pg-origin-secret
          key: superuser-password
      replication:
        username:
          name: pg-origin-secret
          key: replication-username
        password:
          name: pg-origin-secret
          key: replication-password
      authenticator:
        username:
          name: pg-origin-secret
          key: authenticator-username
        password:
          name: pg-origin-secret
          key: authenticator-password
```

> **Note:** Replace the ${HOST_IP} with the actual IP of the external instance.

From Storage:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    restore:
      fromBackup:
        name: backup-name
  replicateFrom:
    storage:
      path: ${PG_ORIGIN_BACKUP_PATH}
      sgObjectStorage: stackgres-backups
    users:
      superuser:
        username:
          name: pg-origin-secret
          key: superuser-username
        password:
          name: pg-origin-secret
          key: superuser-password
      replication:
        username:
          name: pg-origin-secret
          key: replication-username
        password:
          name: pg-origin-secret
          key: replication-password
      authenticator:
        username:
          name: pg-origin-secret
          key: authenticator-username
        password:
          name: pg-origin-secret
          key: authenticator-password
```

> **Note:** Using storage only to replicate from requires to recover from a backup in order to bootstrap the database.
> 
> Replace the ${PG_ORIGIN_BACKUP_PATH} with the actual path in the object storage where the backups are stored.

From external instance and storage:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  replicateFrom:
    instance:
      external:
        host: ${HOST_IP}
        port: 5433
    storage:
      path: ${PG_ORIGIN_BACKUP_PATH}
      sgObjectStorage: stackgres-backups
    users:
      superuser:
        username:
          name: pg-origin-secret
          key: superuser-username
        password:
          name: pg-origin-secret
          key: superuser-password
      replication:
        username:
          name: pg-origin-secret
          key: replication-username
        password:
          name: pg-origin-secret
          key: replication-password
      authenticator:
        username:
          name: pg-origin-secret
          key: authenticator-username
        password:
          name: pg-origin-secret
          key: authenticator-password
```

> **Note**: Replace the ${HOST_IP} with the actual IP of the external instance.
> 
> Replace the ${PG_ORIGIN_BACKUP_PATH} with the actual path in the object storage where the backups are stored.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefrominstance">instance</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configure replication from a PostgreSQL instance.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromstorage">storage</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configure replication from an SGObjectStorage using WAL shipping.

The file structure of the object storage must follow the
  [WAL-G](https://github.com/wal-g/wal-g) file structure.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromusers">users</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the users.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.replicateFrom.instance
<sup><sup>[↩ Parent](#sgclusterspecreplicatefrom)</sup></sup>

Configure replication from a PostgreSQL instance.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefrominstanceexternal">external</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configure replication from an external PostgreSQL instance.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgCluster</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Configure replication from an SGCluster.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.instance.external
<sup><sup>[↩ Parent](#sgclusterspecreplicatefrominstance)</sup></sup>

Configure replication from an external PostgreSQL instance.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>host</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The host of the PostgreSQL to replicate from.<br/>
        </td>
      </tr>
      <tr>
        <td><b>port</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The port of the PostgreSQL to replicate from.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.replicateFrom.storage
<sup><sup>[↩ Parent](#sgclusterspecreplicatefrom)</sup></sup>

Configure replication from an SGObjectStorage using WAL shipping.

The file structure of the object storage must follow the
  [WAL-G](https://github.com/wal-g/wal-g) file structure.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>path</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The path in the SGObjectStorage to replicate from.<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgObjectStorage</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The SGObjectStorage name to replicate from.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromstorageperformance">performance</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration that affects the backup network and disk usage performance during recovery.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.storage.performance
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromstorage)</sup></sup>

Configuration that affects the backup network and disk usage performance during recovery.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>downloadConcurrency</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of file to read and 10.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxDiskBandwidth</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Maximum disk read I/O when performing a backup. In bytes (per second).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxNetworkBandwidth</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Maximum storage upload bandwidth used when storing a backup. In bytes (per second).
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.replicateFrom.users
<sup><sup>[↩ Parent](#sgclusterspecreplicatefrom)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the users.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromusersauthenticator">authenticator</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromusersreplication">replication</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromuserssuperuser">superuser</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.authenticator
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromusersauthenticatorpassword">password</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromusersauthenticatorusername">username</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.authenticator.password
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.authenticator.username
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.replication
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromusersreplicationpassword">password</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromusersreplicationusername">username</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.replication.password
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.replication.username
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.superuser
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromuserssuperuserpassword">password</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicatefromuserssuperuserusername">username</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.superuser.password
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the password of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replicateFrom.users.superuser.username
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the username of the user.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.replication
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

This section allows to configure Postgres replication mode and HA roles groups.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicationgroupsindex">groups</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          StackGres support replication groups where a replication group of a specified number of instances could have different
  replication role. The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicationinitialization">initialization</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to specify how the replicas are initialized.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>mode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The replication mode applied to the whole cluster.
Possible values are:
* `async` (default)
* `sync`
* `strict-sync`
* `sync-all`
* `strict-sync-all`

**async**

When in asynchronous mode the cluster is allowed to lose some committed transactions.
  When the primary server fails or becomes unavailable for any other reason a sufficiently healthy standby
  will automatically be promoted to primary. Any transactions that have not been replicated to that standby
  remain in a "forked timeline" on the primary, and are effectively unrecoverable (the data is still there,
  but recovering it requires a manual recovery effort by data recovery specialists).

**sync**

When in synchronous mode a standby will not be promoted unless it is certain that the standby contains all
  transactions that may have returned a successful commit status to client (clients can change the behavior
  per transaction using PostgreSQL’s `synchronous_commit` setting. Transactions with `synchronous_commit`
  values of `off` and `local` may be lost on fail over, but will not be blocked by replication delays). This
  means that the system may be unavailable for writes even though some servers are available. System
  administrators can still use manual failover commands to promote a standby even if it results in transaction
  loss.

Synchronous mode does not guarantee multi node durability of commits under all circumstances. When no suitable
  standby is available, primary server will still accept writes, but does not guarantee their replication. When
  the primary fails in this mode no standby will be promoted. When the host that used to be the primary comes
  back it will get promoted automatically, unless system administrator performed a manual failover. This behavior
  makes synchronous mode usable with 2 node clusters.

When synchronous mode is used and a standby crashes, commits will block until the primary is switched to standalone
  mode. Manually shutting down or restarting a standby will not cause a commit service interruption. Standby will
  signal the primary to release itself from synchronous standby duties before PostgreSQL shutdown is initiated.

**strict-sync**

When it is absolutely necessary to guarantee that each write is stored durably on at least two nodes, use the strict
  synchronous mode. This mode prevents synchronous replication to be switched off on the primary when no synchronous
  standby candidates are available. As a downside, the primary will not be available for writes (unless the Postgres
  transaction explicitly turns off `synchronous_mode` parameter), blocking all client write requests until at least one
  synchronous replica comes up.

**Note**: Because of the way synchronous replication is implemented in PostgreSQL it is still possible to lose
  transactions even when using strict synchronous mode. If the PostgreSQL backend is cancelled while waiting to acknowledge
  replication (as a result of packet cancellation due to client timeout or backend failure) transaction changes become
  visible for other backends. Such changes are not yet replicated and may be lost in case of standby promotion.

**sync-all**

The same as `sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number
  of instances less one.

**strict-sync-all**

The same as `strict-sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number
  of instances less one.
<br/>
          <br/>
            <i>Default</i>: async<br/>
        </td>
      </tr>
      <tr>
        <td><b>role</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This role is applied to the instances of the implicit replication group that is composed by `.spec.instances` number of instances.
Possible values are:
* `ha-read` (default)
* `ha`
The primary instance will be elected among all the replication groups that are either `ha` or `ha-read`.
Only if the role is set to `ha-read` instances of main replication group will be exposed via the replicas service.
<br/>
          <br/>
            <i>Default</i>: ha-read<br/>
        </td>
      </tr>
      <tr>
        <td><b>syncInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of synchronous standby instances. Must be less than the total number of instances. It is set to 1 by default.
  Only setteable if mode is `sync` or `strict-sync`.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.replication.groups[index]
<sup><sup>[↩ Parent](#sgclusterspecreplication)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>instances</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of StackGres instances for this replication group.

The total number of instance of a cluster is always `.spec.instances`. The sum of the instances in all the replication groups must be
  less than the total number of instances.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr>
      <tr>
        <td><b>role</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This role is applied to the instances of this replication group.
Possible values are:
* `ha-read`
* `ha`
* `readonly`
* `none`
The primary instance will be elected among all the replication groups that are either `ha` or `ha-read`.
Only if the role is set to `readonly` or `ha-read` instances of such replication group will be exposed via the replicas service.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>minInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Minimum number of StackGres instances for this replication group. It is ignored when horizontal Pod autoscaling is disabled (see `.spec.autoscaling`)

The total minimum number of instance of a cluster is always `.spec.autoscaling.minInstances`. The sum of the minimum instances in all the replication groups must be
  less than the total minimum number of instances.

When this field is set the instances value that is provided by the user it is overwritten using the following formula to calculate it:

```
<instances> = max(<minInstances>, <minInstances> * <cluster instances> / <cluster autoscaling minInstances>)
```
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the replication group. If not set will default to the `group-<index>`.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.spec.replication.initialization
<sup><sup>[↩ Parent](#sgclusterspecreplication)</sup></sup>

Allow to specify how the replicas are initialized.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>backupNewerThan</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how old an SGBackup have to be in order to be seleceted
 to initialize a replica.

When `FromExistingBackup` mode is set this field restrict the selection of SGBackup to be used for recovery newer than the
 specified value. 

When `FromNewlyCreatedBackup` mode is set this field skip the creation SGBackup to be used for recovery if one newer than
 the specified value exists. 
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterspecreplicationinitializationbackuprestoreperformance">backupRestorePerformance</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration that affects the backup network and disk usage performance during recovery.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>mode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Allow to specify how the replicas are initialized.

Possible values are:

* `FromPrimary`: When this mode is used replicas will be always created from the primary using `pg_basebackup`.
* `FromReplica`: When this mode is used replicas will be created from another existing replica using
 `pg_basebackup`. Fallsback to `FromPrimary` if there's no replica or it fails.
* `FromExistingBackup`: When this mode is used replicas will be created from an existing SGBackup. If `backupNewerThan` is set
 the SGBackup must be newer than its value. When this mode fails to restore an SGBackup it will try with a previous one (if exists).
 Fallsback to `FromReplica` if there's no backup left or it fails.
* `FromNewlyCreatedBackup`: When this mode is used replicas will be created from a newly created SGBackup.
 Fallsback to `FromExistingBackup` if `backupNewerThan` is set and exists a recent backup newer than its value or it fails.
<br/>
          <br/>
            <i>Default</i>: FromExistingBackup<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.spec.replication.initialization.backupRestorePerformance
<sup><sup>[↩ Parent](#sgclusterspecreplicationinitialization)</sup></sup>

Configuration that affects the backup network and disk usage performance during recovery.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>downloadConcurrency</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of file to read and 10.
<br/>
          <br/>
            <i>Minimum</i>: 1<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxDiskBandwidth</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Maximum disk read I/O when performing a backup. In bytes (per second).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxNetworkBandwidth</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Maximum storage upload bandwidth used when storing a backup. In bytes (per second).
<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.spec.toInstallPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>postgresVersion</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The postgres major version of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>publisher</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The id of the publisher of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>repository</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The repository base URL from where the extension will be installed from.<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The version of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>build</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The build version of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>extraMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The extra mounts of the extension to install.<br/>
        </td>
      </tr></tbody>
</table>

## SGCluster.status
<sup><sup>[↩ Parent](#sgcluster)</sup></sup>

Current status of a StackGres cluster.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>arch</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The architecture on which the cluster has been initialized.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusbinding">binding</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section follow the schema specified in [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).

For more information see https://servicebinding.io/spec/core/1.0.0/
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusconditionsindex">conditions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusdbops">dbOps</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Used by some [SGDbOps](https://stackgres.io/doc/latest/reference/crd/sgdbops) to indicate the operation configuration and status to the operator.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>instances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Actual number of instances for the StackGres cluster. Each instance is a Pod containing one Postgres server.<br/>
        </td>
      </tr>
      <tr>
        <td><b>labelPrefix</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The custom prefix that is prepended to all labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b>labelSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Actual label selector for instances for the StackGres cluster's Pods to be used by autoscaling.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusmanagedsql">managedSql</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section stores the state of referenced SQL scripts that are applied to the cluster live.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>os</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The operative system on which the cluster has been initialized.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatuspodstatusesindex">podStatuses</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of pod statuses.<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicationInitializationFailedSGBackup</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Indicates the latest failed backup for the replication initialization.<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.status.binding
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>

This section follow the schema specified in [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).

For more information see https://servicebinding.io/spec/core/1.0.0/


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the Secret as specified in [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.status.conditions[index]
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastTransitionTime</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Last time the condition transitioned from one status to another.<br/>
        </td>
      </tr>
      <tr>
        <td><b>message</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A human readable message indicating details about the transition.<br/>
        </td>
      </tr>
      <tr>
        <td><b>reason</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The reason for the condition's last transition.<br/>
        </td>
      </tr>
      <tr>
        <td><b>status</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Status of the condition, one of True, False, Unknown.<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Type of deployment condition.<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.status.dbOps
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>

Used by some [SGDbOps](https://stackgres.io/doc/latest/reference/crd/sgdbops) to indicate the operation configuration and status to the operator.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusdbopsmajorversionupgrade">majorVersionUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The major version upgrade configuration and status
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusdbopsminorversionupgrade">minorVersionUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The minor version upgrade configuration and status
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusdbopsrestart">restart</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The minor version upgrade configuration and status
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusdbopssecurityupgrade">securityUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The minor version upgrade configuration and status
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.status.dbOps.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgclusterstatusdbops)</sup></sup>

The major version upgrade configuration and status


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>check</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Run `pg_upgrade` with check option instead of performing the real upgrade
<br/>
        </td>
      </tr>
      <tr>
        <td><b>clone</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Use `--clone` option when running `pg_upgrade`
<br/>
        </td>
      </tr>
      <tr>
        <td><b>dataChecksum</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Indicates if PostgreSQL data checksum is enabled
<br/>
        </td>
      </tr>
      <tr>
        <td><b>encoding</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The PostgreSQL encoding
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that this operation is targetting
<br/>
        </td>
      </tr>
      <tr>
        <td><b>link</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Use `--link` option when running `pg_upgrade`
<br/>
        </td>
      </tr>
      <tr>
        <td><b>locale</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The PostgreSQL locale
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryInstance</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The primary instance that this operation is targetting
<br/>
        </td>
      </tr>
      <tr>
        <td><b>rollback</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Indicates to rollback from a previous major version upgrade
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sourceBackupPath</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The source backup path
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusdbopsmajorversionupgradesourcepostgresextensionsindex">sourcePostgresExtensions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The source PostgreSQL extensions
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sourcePostgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The source PostgreSQL version
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sourceReplicationMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The source replication mode
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sourceSgPostgresConfig</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The source SGPostgresConfig reference
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetPostgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target PostgreSQL version
<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.status.dbOps.majorVersionUpgrade.sourcePostgresExtensions[index]
<sup><sup>[↩ Parent](#sgclusterstatusdbopsmajorversionupgrade)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the extension to deploy.<br/>
        </td>
      </tr>
      <tr>
        <td><b>publisher</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The id of the publisher of the extension to deploy. If not specified `com.ongres` will be used by default.<br/>
          <br/>
            <i>Default</i>: com.ongres<br/>
        </td>
      </tr>
      <tr>
        <td><b>repository</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The repository base URL from where to obtain the extension to deploy.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The version of the extension to deploy. If not specified version of `stable` channel will be used by default and if only a version is available that one will be used.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.status.dbOps.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgclusterstatusdbops)</sup></sup>

The minor version upgrade configuration and status


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that this operation is targetting
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryInstance</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The primary instance that this operation is targetting
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sourcePostgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Postgres version that is currently running on the cluster
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetPostgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The desired Postgres version for the cluster
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.status.dbOps.restart
<sup><sup>[↩ Parent](#sgclusterstatusdbops)</sup></sup>

The minor version upgrade configuration and status


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that this operation is targetting
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryInstance</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The primary instance that this operation is targetting
<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.status.dbOps.securityUpgrade
<sup><sup>[↩ Parent](#sgclusterstatusdbops)</sup></sup>

The minor version upgrade configuration and status


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that this operation is targetting
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryInstance</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The primary instance that this operation is targetting
<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.status.managedSql
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>

This section stores the state of referenced SQL scripts that are applied to the cluster live.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusmanagedsqlscriptsindex">scripts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of statuses for script references.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.status.managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgclusterstatusmanagedsql)</sup></sup>

The status of a script reference.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>completedAt</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          ISO-8601 datetime of when the script execution had completed (mutually exclusive with `failedAt`).<br/>
        </td>
      </tr>
      <tr>
        <td><b>failedAt</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          ISO-8601 datetime of when the script execution had failed (mutually exclusive with `completedAt`).<br/>
        </td>
      </tr>
      <tr>
        <td><b>id</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Identify the associated `SGScript` entry with the same value in the `id` field.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatusmanagedsqlscriptsindexscriptsindex">scripts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of statuses for script entries of referenced script.<br/>
        </td>
      </tr>
      <tr>
        <td><b>startedAt</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          ISO-8601 datetime of when the script execution has been started.<br/>
        </td>
      </tr>
      <tr>
        <td><b>updatedAt</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          ISO-8601 datetime of when the last script execution occurred. Will be reset each time the referenced `SGScripts` entry will be applied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGCluster.status.managedSql.scripts[index].scripts[index]
<sup><sup>[↩ Parent](#sgclusterstatusmanagedsqlscriptsindex)</sup></sup>

The status of a script entry of a referenced script.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If failed, a message of the failure<br/>
        </td>
      </tr>
      <tr>
        <td><b>failureCode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If failed, the error code of the failure. See also https://www.postgresql.org/docs/current/errcodes-appendix.html<br/>
        </td>
      </tr>
      <tr>
        <td><b>id</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Identify the associated script entry with the same value in the `id` field.<br/>
        </td>
      </tr>
      <tr>
        <td><b>intents</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Indicates the number of intents or failures occurred<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The latest version applied<br/>
        </td>
      </tr></tbody>
</table>

### SGCluster.status.podStatuses[index]
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the pod.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgclusterstatuspodstatusesindexinstalledpostgresextensionsindex">installedPostgresExtensions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of Postgres extensions currently installed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The node name where the pod is running.<br/>
        </td>
      </tr>
      <tr>
        <td><b>pendingRestart</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Indicates if the pod requires restart<br/>
        </td>
      </tr>
      <tr>
        <td><b>primary</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Indicates if the pod is the elected primary<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicationGroup</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Indicates the replication group this Pod belongs to.<br/>
        </td>
      </tr></tbody>
</table>

#### SGCluster.status.podStatuses[index].installedPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgclusterstatuspodstatusesindex)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the installed extension.<br/>
        </td>
      </tr>
      <tr>
        <td><b>postgresVersion</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The postgres major version of the installed extension.<br/>
        </td>
      </tr>
      <tr>
        <td><b>publisher</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The id of the publisher of the installed extension.<br/>
        </td>
      </tr>
      <tr>
        <td><b>repository</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The repository base URL from where the extension was installed from.<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The version of the installed extension.<br/>
        </td>
      </tr>
      <tr>
        <td><b>build</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The build version of the installed extension.<br/>
        </td>
      </tr>
      <tr>
        <td><b>extraMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The extra mounts of the installed extension.<br/>
        </td>
      </tr></tbody>
</table>

## Sidecar Containers

A sidecar container is a container that adds functionality to PostgreSQL or to the cluster infrastructure.
Currently StackGres implement following sidecar containers:

* `cluster-controller`: this container is always present, and it is not possible to disable it.
 It serves to reconcile local configurations, collects Pod status, and performs local actions (like extensions installation, execution of SGScript entries, etc.).
* `envoy`: this container is always present, and it is not possible to disable it.
 It serve as a edge proxy from client to PostgreSQL instances or between PostgreSQL instances.
 It enables network metrics collection to provide connection statistics.
* `pgbouncer`: PgBouncer that serves as connection pooler for the PostgreSQL instances.
* `prometheus-postgres-exporter`: Postgres exporter that exports metrics for the PostgreSQL instances.
* `fluent-bit`: Fluent-bit that send logs to a distributed logs cluster.
* `postgres-util`: Contains `psql` and all PostgreSQL common tools in order to perform common administration tasks.

The following example disables all non-essential sidecars:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    disableConnectionPooling: false
    disableMetricsExporter: false
    disablePostgresUtil: false
```
