---
title: SGDistributedLogs
weight: 7
url: /reference/crd/sgdistributedlogs
description: Details about SGDistributedLogs
showToc: true
---

___

**Kind:** SGDistributedLogs

**listKind:** SGDistributedLogsList

**plural:** sgdistributedlogs

**singular:** sgdistributedlogs

**shortNames** sgdil
___

The `SGDistributedLogs` custom resource represents a distributed logs cluster.
When a Postgres cluster is configured to use distributed logs, all logs from different sources will be forwarded to the distributed logs cluster.
Under the hood, distributed log cluster use an `SGCluster`.
Therefore, the distributed log cluster can be queried using SQL as well, for example using *postgres-util*.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  name: distributedlogs
spec:
  persistentVolume:
    size: 10Gi
```

See also [Distribtued Logs section]({{%  relref "04-administration-guide/12-distributed-logs" %}}).





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
      <td>SGDistributedLogs</td>
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
        <td><b><a href="#sgdistributedlogsspec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

## SGDistributedLogs.spec
<sup><sup>[↩ Parent](#sgdistributedlogs)</sup></sup>



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
        <td><b><a href="#sgdistributedlogsspecpersistentvolume">persistentVolume</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod's persistent volume configuration<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecconfigurations">configurations</a></b></td>
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
        <td><b><a href="#sgdistributedlogsspecmetadata">metadata</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Metadata information from cluster created resources.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecnonproductionoptions">nonProductionOptions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecpostgresservices">postgresServices</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Kubernetes [services](https://kubernetes.io/docs/concepts/services-networking/service/) created or managed by StackGres.

<br/>
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
        <td><b><a href="#sgdistributedlogsspecresources">resources</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod custom resources configuration.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecscheduling">scheduling</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Pod custom scheduling and affinity configuration.

**Changing this field may require a restart.**
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
          Name of the [SGInstanceProfile](https://stackgres.io/doc/latest/04-postgres-cluster-management/03-resource-profiles/). A SGInstanceProfile defines CPU and memory limits. Must exist before creating a distributed logs. When no profile is set, a default (currently: 1 core, 2 GiB RAM) one is used.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspectoinstallpostgresextensionsindex">toInstallPostgresExtensions</a></b></td>
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

### SGDistributedLogs.spec.persistentVolume
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Pod's persistent volume configuration

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
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Size of the PersistentVolume set for the pod of the cluster for distributed logs. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).
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

### SGDistributedLogs.spec.configurations
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Cluster custom configurations.


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
        <td><b>sgPostgresConfig</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>string</td>
        <td>
          Name of the [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig) used for the distributed logs. It must exist. When not set, a default Postgres config, for the major version selected, is used.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr></tbody>
</table>

### SGDistributedLogs.spec.metadata
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Metadata information from cluster created resources.

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
        <td><b><a href="#sgdistributedlogsspecmetadataannotations">annotations</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.<br/>
        </td>
      </tr></tbody>
</table>

#### SGDistributedLogs.spec.metadata.annotations
<sup><sup>[↩ Parent](#sgdistributedlogsspecmetadata)</sup></sup>

Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.

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
        <td><b>pods</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Annotations to attach to pods created or managed by StackGres.<br/>
        </td>
      </tr>
      <tr>
        <td><b>services</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Annotations to attach to services created or managed by StackGres.<br/>
        </td>
      </tr></tbody>
</table>

### SGDistributedLogs.spec.nonProductionOptions
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>



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

The same best practice applies to databases on containers. By default, StackGres will not allow to run more than one StackGres or Distributed Logs pod on a given Kubernetes node. If set to `true` it will allow more than one StackGres pod per node.

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
        </td>
      </tr></tbody>
</table>

### SGDistributedLogs.spec.postgresServices
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Kubernetes [services](https://kubernetes.io/docs/concepts/services-networking/service/) created or managed by StackGres.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  name: stackgres
spec:
  postgresServices:
    primary:
      type: ClusterIP
    replicas:
      enabled: true
      type: ClusterIP
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
        <td><b><a href="#sgdistributedlogsspecpostgresservicesprimary">primary</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration for the `-primary` service. It provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the cluster.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecpostgresservicesreplicas">replicas</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration for the `-replicas` service. It provides a stable connection (regardless of replica node failures) to any read-only Postgres server of the cluster. Read-only servers are load-balanced via this service.<br/>
        </td>
      </tr></tbody>
</table>

#### SGDistributedLogs.spec.postgresServices.primary
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservices)</sup></sup>

Configuration for the `-primary` service. It provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the cluster.

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
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) passed to the `-primary` service.

<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerIP</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Specify loadBalancer IP of Postgres primary service for Distributed Log<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>enum</td>
        <td>
          Specifies the type of Kubernetes service(`ClusterIP`, `LoadBalancer`, `NodePort`)<br/>
          <br/>
            <i>Enum</i>: ClusterIP, LoadBalancer, NodePort<br/>
        </td>
      </tr></tbody>
</table>

#### SGDistributedLogs.spec.postgresServices.replicas
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservices)</sup></sup>

Configuration for the `-replicas` service. It provides a stable connection (regardless of replica node failures) to any read-only Postgres server of the cluster. Read-only servers are load-balanced via this service.

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
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) passed to the `-replicas` service.<br/>
        </td>
      </tr>
      <tr>
        <td><b>enabled</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Specify if the `-replicas` service should be created or not.<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerIP</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Specify loadBalancer IP of Postgres replica service for Distributed Log<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>enum</td>
        <td>
          Specifies the type of Kubernetes service(`ClusterIP`, `LoadBalancer`, `NodePort`).<br/>
          <br/>
            <i>Enum</i>: ClusterIP, LoadBalancer, NodePort<br/>
        </td>
      </tr></tbody>
</table>

### SGDistributedLogs.spec.resources
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

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
          When set to `true` resources limits for containers other than the patroni container wil be set just like for patroni contianer as specified in the SGInstanceProfile.

**Changing this field may require a restart.**
<br/>
        </td>
      </tr></tbody>
</table>

### SGDistributedLogs.spec.scheduling
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Pod custom scheduling and affinity configuration.

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
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinity">nodeAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#nodeaffinity-v1-core<br/>
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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinity">podAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#podaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinity">podAntiAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#podantiaffinity-v1-core<br/>
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
        <td><b><a href="#sgdistributedlogsspecschedulingtolerationsindex">tolerations</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If specified, the pod's tolerations.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#toleration-v1-core<br/>
        </td>
      </tr></tbody>
</table>

#### SGDistributedLogs.spec.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgdistributedlogsspecscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#nodeaffinity-v1-core

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
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>object</td>
        <td>
          A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDistributedLogs.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinity)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference">preference</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDistributedLogs.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinity)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex">nodeSelectorTerms</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          Required. A list of node selector terms. The terms are ORed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDistributedLogs.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDistributedLogs.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

#### SGDistributedLogs.spec.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgdistributedlogsspecscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#podaffinity-v1-core

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinity)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. Also, MatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MismatchLabelKeys and LabelSelector. Also, MismatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinity)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. Also, MatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MismatchLabelKeys and LabelSelector. Also, MismatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

#### SGDistributedLogs.spec.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgdistributedlogsspecscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#podantiaffinity-v1-core

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]object</td>
        <td>
          If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinity)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. Also, MatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MismatchLabelKeys and LabelSelector. Also, MismatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinity)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. Also, MatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td>✓</td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MismatchLabelKeys and LabelSelector. Also, MismatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

#### SGDistributedLogs.spec.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecscheduling)</sup></sup>

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

### SGDistributedLogs.spec.toInstallPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>



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

## SGDistributedLogs.status
<sup><sup>[↩ Parent](#sgdistributedlogs)</sup></sup>



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
        <td><b><a href="#sgdistributedlogsstatusconditionsindex">conditions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsstatusconnectedclustersindex">connectedClusters</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of connected `sgclusters`<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdistributedlogsstatusdatabasesindex">databases</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of database status<br/>
        </td>
      </tr>
      <tr>
        <td><b>fluentdConfigHash</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The hash of the configuration file that is used by fluentd<br/>
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
        <td><b><a href="#sgdistributedlogsstatuspodstatusesindex">podStatuses</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of pod statuses.<br/>
        </td>
      </tr></tbody>
</table>

### SGDistributedLogs.status.conditions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsstatus)</sup></sup>



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

### SGDistributedLogs.status.connectedClusters[index]
<sup><sup>[↩ Parent](#sgdistributedlogsstatus)</sup></sup>

A connected `sgcluster`

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
        <td><b><a href="#sgdistributedlogsstatusconnectedclustersindexconfig">config</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The configuration for `sgdistributedlgos` of this `sgcluster`<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The `sgcluster` name<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespace</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The `sgcluster` namespace<br/>
        </td>
      </tr></tbody>
</table>

#### SGDistributedLogs.status.connectedClusters[index].config
<sup><sup>[↩ Parent](#sgdistributedlogsstatusconnectedclustersindex)</sup></sup>

The configuration for `sgdistributedlgos` of this `sgcluster`

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
          The retention window that has been applied to tables<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgDistributedLogs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The `sgdistributedlogs` to which this `sgcluster` is connected to<br/>
        </td>
      </tr></tbody>
</table>

### SGDistributedLogs.status.databases[index]
<sup><sup>[↩ Parent](#sgdistributedlogsstatus)</sup></sup>

A database status

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
          The database name that has been created<br/>
        </td>
      </tr>
      <tr>
        <td><b>retention</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The retention window that has been applied to tables<br/>
        </td>
      </tr></tbody>
</table>

### SGDistributedLogs.status.podStatuses[index]
<sup><sup>[↩ Parent](#sgdistributedlogsstatus)</sup></sup>



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
        <td><b><a href="#sgdistributedlogsstatuspodstatusesindexinstalledpostgresextensionsindex">installedPostgresExtensions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of extensions currently installed.<br/>
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
      </tr></tbody>
</table>

#### SGDistributedLogs.status.podStatuses[index].installedPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsstatuspodstatusesindex)</sup></sup>



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
          The repository base URL from where the extension was installed.<br/>
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
      </tr></tbody>
</table>
