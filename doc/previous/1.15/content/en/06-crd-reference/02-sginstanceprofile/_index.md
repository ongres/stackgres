---
title: SGInstanceProfile
weight: 2
url: /reference/crd/sginstanceprofile
description: Details about SGInstanceProfile
shotToc: true
---

___
**Kind:** SGInstanceProfile

**listKind:** SGInstanceProfileList

**plural:** sginstanceprofiles

**singular:** sginstanceprofile

**shortNames** sginp
___

The `SGInstanceProfile` custom resource represents the CPU and memory resources assigned to each pod of the Postgres cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
  kind: SGInstanceProfile
  metadata:
    name: size-l
  spec:
    cpu: "4"
    memory: 8Gi
```

See also [Instance Configuration section]({{% relref "04-administration-guide/04-configuration/02-instance-profile" %}}).





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
      <td>SGInstanceProfile</td>
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
        <td><b><a href="#sginstanceprofilespec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

## SGInstanceProfile.spec
<sup><sup>[↩ Parent](#sginstanceprofile)</sup></sup>



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
        <td><b><a href="#sginstanceprofilespeccontainerskey">containers</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]object</td>
        <td>
          The CPU(s) (cores) and RAM limits assigned to containers other than patroni container.
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
          CPU(s) (cores) limits for every resource's Pod that reference this SGInstanceProfile. The suffix `m`
  specifies millicpus (where 1000m is equals to 1).

The number of cpu limits is assigned to the patroni container (that runs both Patroni and PostgreSQL).

A minimum of 2 cpu is recommended.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sginstanceprofilespechugepages">hugePages</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          RAM limits allocated for huge pages of the patroni container (that runs both Patroni and PostgreSQL).
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sginstanceprofilespecinitcontainerskey">initContainers</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]object</td>
        <td>
          The CPU(s) (cores) and RAM limits assigned to the init containers.<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits for every resource's Pod that reference this SGInstanceProfile. The suffix `Mi` or `Gi`
  specifies Mebibytes or Gibibytes, respectively.

The amount of RAM limits is assigned to the patroni container (that runs both Patroni and PostgreSQL).

A minimum of 2Gi is recommended.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sginstanceprofilespecrequests">requests</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allow to configure the resources requests for each container and, if not specified, it is filled with some defaults based on the fields `.spec.cpu` and `.spec.memory` will be set.

On containerized environments, when running production workloads, enforcing container's resources requirements requests to be equals to the limits in order to achieve the highest level of performance. Doing so, reduces the chances of leaving
 the workload with less resources than it requires. It also allow to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.
 There are cases where you may need to set cpu requests to the same value as cpu limits in order to achieve static CPU management policy.

By default the resources requests values in fields `.spec.requests.cpu` and `.spec.requests.memory` represent the total resources requests assigned to each resource's Pod that reference this SGInstanceProfile.
 The resources requests of the patroni container (that runs both Patroni and PostgreSQL) is calculated by subtracting from the total resources requests the resources requests of other containers that are present in the Pod.
 To change this behavior and having the resources requests values in fields `.spec.requests.cpu` and `.spec.requests.memory` to represent the resources requests of the patroni container and the total resources requests
 calculated by adding the resources requests of all the containers (including the patroni container) you may set one or more of the following fields to `true`
 (depending on the resource's Pods you need this behaviour to be changed):
 
* `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.coordinator.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.ovewrites.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGDistributedLogs.spec.resources.disableResourcesRequestsSplitFromTotal`
<br/>
        </td>
      </tr></tbody>
</table>

 SGInstanceProfile.spec.containers[key]
<sup><sup>[↩ Parent](#sginstanceprofilespec)</sup></sup>

The CPU(s) (cores) and RAM limits assigned to a container.

This section, if left empty, will be filled automatically by the operator with
  some defaults that can be proportional to the resources limits assigned to patroni
  container (except for the huge pages that are always left untouched).


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
          CPU(s) (cores) limits for the specified container. The suffix `m`
  specifies millicpus (where 1000m is equals to 1).
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sginstanceprofilespeccontainerskeyhugepages">hugePages</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          RAM limits for huge pages for the specified container.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits for the specified container. The suffix `Mi` or `Gi`
  specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr></tbody>
</table>

## SGInstanceProfile.spec.containers[key].hugePages
<sup><sup>[↩ Parent](#sginstanceprofilespeccontainerskey)</sup></sup>

RAM limits for huge pages for the specified container.


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
        <td><b>hugepages-1Gi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits for huge pages of the specified container with a size of 1Gi. The suffix `Mi`
  or `Gi` specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>hugepages-2Mi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits for huge pages of the specified container with a size of 2Mi. The suffix `Mi`
  or `Gi` specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr></tbody>
</table>

### SGInstanceProfile.spec.hugePages
<sup><sup>[↩ Parent](#sginstanceprofilespec)</sup></sup>

RAM limits allocated for huge pages of the patroni container (that runs both Patroni and PostgreSQL).


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
        <td><b>hugepages-1Gi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits allocated for huge pages of the patroni container (that runs both Patroni and PostgreSQL) with a size of 1Gi. The suffix `Mi` or `Gi`
  specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>hugepages-2Mi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits allocated for huge pages of the patroni container (that runs both Patroni and PostgreSQL) with a size of 2Mi. The suffix `Mi` or `Gi`
  specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr></tbody>
</table>

 SGInstanceProfile.spec.initContainers[key]
<sup><sup>[↩ Parent](#sginstanceprofilespec)</sup></sup>

The CPU(s) (cores) and RAM limits assigned to a init container.

This section will be filled automatically by the operator with
  the same values of the resources limits assigned to patroni
  container (except for the huge pages that are always left untouched).


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
          CPU(s) (cores) limits for the specified init container. The suffix
  `m` specifies millicpus (where 1000m is equals to 1).
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sginstanceprofilespecinitcontainerskeyhugepages">hugePages</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          RAM limits for huge pages of the specified init container
<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits for the specified init container. The suffix `Mi`
  or `Gi` specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr></tbody>
</table>

## SGInstanceProfile.spec.initContainers[key].hugePages
<sup><sup>[↩ Parent](#sginstanceprofilespecinitcontainerskey)</sup></sup>

RAM limits for huge pages of the specified init container


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
        <td><b>hugepages-1Gi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits for huge pages of the specified init container with a size of 1Gi. The suffix `Mi` or `Gi`
  specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>hugepages-2Mi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM limits for huge pages of the specified init container with a size of 2Mi. The suffix `Mi`
  or `Gi` specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr></tbody>
</table>

### SGInstanceProfile.spec.requests
<sup><sup>[↩ Parent](#sginstanceprofilespec)</sup></sup>

This section allow to configure the resources requests for each container and, if not specified, it is filled with some defaults based on the fields `.spec.cpu` and `.spec.memory` will be set.

On containerized environments, when running production workloads, enforcing container's resources requirements requests to be equals to the limits in order to achieve the highest level of performance. Doing so, reduces the chances of leaving
 the workload with less resources than it requires. It also allow to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.
 There are cases where you may need to set cpu requests to the same value as cpu limits in order to achieve static CPU management policy.

By default the resources requests values in fields `.spec.requests.cpu` and `.spec.requests.memory` represent the total resources requests assigned to each resource's Pod that reference this SGInstanceProfile.
 The resources requests of the patroni container (that runs both Patroni and PostgreSQL) is calculated by subtracting from the total resources requests the resources requests of other containers that are present in the Pod.
 To change this behavior and having the resources requests values in fields `.spec.requests.cpu` and `.spec.requests.memory` to represent the resources requests of the patroni container and the total resources requests
 calculated by adding the resources requests of all the containers (including the patroni container) you may set one or more of the following fields to `true`
 (depending on the resource's Pods you need this behaviour to be changed):
 
* `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.coordinator.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.ovewrites.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGDistributedLogs.spec.resources.disableResourcesRequestsSplitFromTotal`


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
        <td><b><a href="#sginstanceprofilespecrequestscontainerskey">containers</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]object</td>
        <td>
          The CPU(s) (cores) and RAM requests assigned to containers other than patroni container.
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
          CPU(s) (cores) requests for every resource's Pod that reference this SGInstanceProfile. The suffix `m`
  specifies millicpus (where 1000m is equals to 1).

By default the cpu requests values in field `.spec.requests.cpu` represent the total cpu requests assigned to each resource's Pod that reference this SGInstanceProfile.
 The cpu requests of the patroni container (that runs both Patroni and PostgreSQL) is calculated by subtracting from the total cpu requests the cpu requests of other containers that are present in the Pod.
 To change this behavior and having the cpu requests values in field `.spec.requests.cpu` to represent the cpu requests of the patroni container and the total cpu requests
 calculated by adding the cpu requests of all the containers (including the patroni container) you may set one or more of the following fields to `true`
 (depending on the resource's Pods you need this behaviour to be changed):
 
* `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.coordinator.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.ovewrites.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGDistributedLogs.spec.resources.disableResourcesRequestsSplitFromTotal`
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sginstanceprofilespecrequestsinitcontainerskey">initContainers</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]object</td>
        <td>
          The CPU(s) (cores) and RAM requests assigned to init containers.<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM requests for every resource's Pod that reference this SGInstanceProfile. The suffix `Mi` or `Gi`
  specifies Mebibytes or Gibibytes, respectively.

By default the memory requests values in field `.spec.requests.memory` represent the total memory requests assigned to each resource's Pod that reference this SGInstanceProfile.
 The memory requests of the patroni container (that runs both Patroni and PostgreSQL) is calculated by subtracting from the total memory requests the memory requests of other containers that are present in the Pod.
 To change this behavior and having the memory requests values in field `.spec.requests.memory` to represent the memory requests of the patroni container and the total memory requests
 calculated by adding the memory requests of all the containers (including the patroni container) you may set one or more of the following fields to `true`
 (depending on the resource's Pods you need this behaviour to be changed):
 
* `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.coordinator.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.ovewrites.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGDistributedLogs.spec.resources.disableResourcesRequestsSplitFromTotal`
<br/>
        </td>
      </tr></tbody>
</table>

 SGInstanceProfile.spec.requests.containers[key]
<sup><sup>[↩ Parent](#sginstanceprofilespecrequests)</sup></sup>

The CPU(s) (cores) and RAM requests assigned to a container.

This section, if left empty, will be filled automatically by the operator with
  some defaults that can be proportional to the resources assigned to patroni
  container (except for the huge pages that are always left untouched).


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
          CPU(s) (cores) requests for the specified container. The suffix `m`
  specifies millicpus (where 1000m is equals to 1).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM requests for the specified container. The suffix `Mi` or `Gi`
  specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr></tbody>
</table>

 SGInstanceProfile.spec.requests.initContainers[key]
<sup><sup>[↩ Parent](#sginstanceprofilespecrequests)</sup></sup>

The CPU(s) (cores) and RAM requests assigned to a init container.

This section will be filled automatically by the operator with
  the same values of the resources requests assigned to patroni
  container (except for the huge pages that are always left untouched).


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
          CPU(s) (cores) requests for the specified init container. The suffix
  `m` specifies millicpus (where 1000m is equals to 1).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>memory</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          RAM requests for the specified init container. The suffix `Mi`
  or `Gi` specifies Mebibytes or Gibibytes, respectively.
<br/>
        </td>
      </tr></tbody>
</table>
