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

See also [Instance Configuration section]({{% relref "04-administration-guide/04-configuration/01-instance-profile" %}}).





{{< crd-properties >}}
[
  {
    "description": "stackgres.io/v1",
    "immutable": true,
    "name": "apiVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "SGInstanceProfile",
    "immutable": true,
    "name": "kind",
    "required": true,
    "type": "string"
  },
  {
    "description": "Refer to the Kubernetes API documentation for the fields of the `metadata` field.",
    "link": "https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta",
    "name": "metadata",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sginstanceprofilespec",
    "description": "",
    "name": "spec",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGInstanceProfile.spec
<sup><sup>[↩ Parent](#sginstanceprofile)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sginstanceprofilespeccontainerskey",
    "description": "The CPU(s) (cores) and RAM limits assigned to containers other than patroni container.",
    "name": "containers",
    "required": false,
    "type": "map[string]object"
  },
  {
    "description": "CPU(s) (cores) limits for every resource's Pod that reference this SGInstanceProfile. The suffix `m`\n  specifies millicpus (where 1000m is equal to 1).\n\nThe number of cpu limits is assigned to the patroni container (that runs both Patroni and PostgreSQL).\n\nA minimum of 2 CPUs is recommended.",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sginstanceprofilespechugepages",
    "description": "RAM limits allocated for huge pages of the patroni container (that runs both Patroni and PostgreSQL).",
    "name": "hugePages",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sginstanceprofilespecinitcontainerskey",
    "description": "The CPU(s) (cores) and RAM limits assigned to the init containers.",
    "name": "initContainers",
    "required": false,
    "type": "map[string]object"
  },
  {
    "description": "RAM limits for every resource's Pod that reference this SGInstanceProfile. The suffix `Mi` or `Gi`\n  specifies Mebibytes or Gibibytes, respectively.\n\nThe amount of RAM limits is assigned to the patroni container (that runs both Patroni and PostgreSQL).\n\nA minimum of 2Gi is recommended.",
    "name": "memory",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sginstanceprofilespecrequests",
    "description": "This section allows you to configure the resources requests for each container and, if not specified, it is filled with some defaults based on the fields `.spec.cpu` and `.spec.memory`.\n\nOn containerized environments, when running production workloads, enforcing container resource requirement requests to be equal to the limits in order to achieve the highest level of performance. Doing so, reduces the chances of leaving\n the workload with less resources than it requires. It also allows you to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the exclusive usage of CPUs on the node.\n There are cases where you may need to set cpu requests to the same value as cpu limits in order to achieve static CPU management policy.\n\nBy default the resources requests values in fields `.spec.requests.cpu` and `.spec.requests.memory` represent the total resources requests assigned to each resource's Pod that reference this SGInstanceProfile.\n The resources requests of the patroni container (that runs both Patroni and PostgreSQL) is calculated by subtracting from the total resources requests the resources requests of other containers that are present in the Pod.\n To change this behavior and have the resources requests values in fields `.spec.requests.cpu` and `.spec.requests.memory` represent the resources requests of the patroni container and the total resources requests\n calculated by adding the resources requests of all the containers (including the patroni container) you may set one or more of the following fields to `true`\n (depending on the resource's Pods you need this behaviour to be changed):\n \n* `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.coordinator.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.shards.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.shards.overwrites.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGDistributedLogs.spec.resources.disableResourcesRequestsSplitFromTotal`",
    "name": "requests",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

### SGInstanceProfile.spec.containers[key]
<sup><sup>[↩ Parent](#sginstanceprofilespec)</sup></sup>

The CPU(s) (cores) and RAM limits assigned to a container.

This section, if left empty, will be filled automatically by the operator with
  some defaults that can be proportional to the resources limits assigned to patroni
  container (except for the huge pages that are always left untouched).


{{< crd-properties >}}
[
  {
    "description": "CPU(s) (cores) limits for the specified container. The suffix `m`\n  specifies millicpus (where 1000m is equal to 1).",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sginstanceprofilespeccontainerskeyhugepages",
    "description": "RAM limits for huge pages for the specified container.",
    "name": "hugePages",
    "required": false,
    "type": "object"
  },
  {
    "description": "RAM limits for the specified container. The suffix `Mi` or `Gi`\n  specifies Mebibytes or Gibibytes, respectively.",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGInstanceProfile.spec.containers[key].hugePages
<sup><sup>[↩ Parent](#sginstanceprofilespeccontainerskey)</sup></sup>

RAM limits for huge pages for the specified container.


{{< crd-properties >}}
[
  {
    "description": "RAM limits for huge pages of the specified container with a size of 1Gi. The suffix `Mi`\n  or `Gi` specifies Mebibytes or Gibibytes, respectively.",
    "name": "hugepages-1Gi",
    "required": false,
    "type": "string"
  },
  {
    "description": "RAM limits for huge pages of the specified container with a size of 2Mi. The suffix `Mi`\n  or `Gi` specifies Mebibytes or Gibibytes, respectively.",
    "name": "hugepages-2Mi",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGInstanceProfile.spec.hugePages
<sup><sup>[↩ Parent](#sginstanceprofilespec)</sup></sup>

RAM limits allocated for huge pages of the patroni container (that runs both Patroni and PostgreSQL).


{{< crd-properties >}}
[
  {
    "description": "RAM limits allocated for huge pages of the patroni container (that runs both Patroni and PostgreSQL) with a size of 1Gi. The suffix `Mi` or `Gi`\n  specifies Mebibytes or Gibibytes, respectively.",
    "name": "hugepages-1Gi",
    "required": false,
    "type": "string"
  },
  {
    "description": "RAM limits allocated for huge pages of the patroni container (that runs both Patroni and PostgreSQL) with a size of 2Mi. The suffix `Mi` or `Gi`\n  specifies Mebibytes or Gibibytes, respectively.",
    "name": "hugepages-2Mi",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGInstanceProfile.spec.initContainers[key]
<sup><sup>[↩ Parent](#sginstanceprofilespec)</sup></sup>

The CPU(s) (cores) and RAM limits assigned to an init container.

This section will be filled automatically by the operator with
  the same values of the resources limits assigned to patroni
  container (except for the huge pages that are always left untouched).


{{< crd-properties >}}
[
  {
    "description": "CPU(s) (cores) limits for the specified init container. The suffix\n  `m` specifies millicpus (where 1000m is equal to 1).",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sginstanceprofilespecinitcontainerskeyhugepages",
    "description": "RAM limits for huge pages of the specified init container",
    "name": "hugePages",
    "required": false,
    "type": "object"
  },
  {
    "description": "RAM limits for the specified init container. The suffix `Mi`\n  or `Gi` specifies Mebibytes or Gibibytes, respectively.",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGInstanceProfile.spec.initContainers[key].hugePages
<sup><sup>[↩ Parent](#sginstanceprofilespecinitcontainerskey)</sup></sup>

RAM limits for huge pages of the specified init container


{{< crd-properties >}}
[
  {
    "description": "RAM limits for huge pages of the specified init container with a size of 1Gi. The suffix `Mi` or `Gi`\n  specifies Mebibytes or Gibibytes, respectively.",
    "name": "hugepages-1Gi",
    "required": false,
    "type": "string"
  },
  {
    "description": "RAM limits for huge pages of the specified init container with a size of 2Mi. The suffix `Mi`\n  or `Gi` specifies Mebibytes or Gibibytes, respectively.",
    "name": "hugepages-2Mi",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGInstanceProfile.spec.requests
<sup><sup>[↩ Parent](#sginstanceprofilespec)</sup></sup>

This section allows you to configure the resources requests for each container and, if not specified, it is filled with some defaults based on the fields `.spec.cpu` and `.spec.memory`.

On containerized environments, when running production workloads, enforcing container resource requirement requests to be equal to the limits in order to achieve the highest level of performance. Doing so, reduces the chances of leaving
 the workload with less resources than it requires. It also allows you to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the exclusive usage of CPUs on the node.
 There are cases where you may need to set cpu requests to the same value as cpu limits in order to achieve static CPU management policy.

By default the resources requests values in fields `.spec.requests.cpu` and `.spec.requests.memory` represent the total resources requests assigned to each resource's Pod that reference this SGInstanceProfile.
 The resources requests of the patroni container (that runs both Patroni and PostgreSQL) is calculated by subtracting from the total resources requests the resources requests of other containers that are present in the Pod.
 To change this behavior and have the resources requests values in fields `.spec.requests.cpu` and `.spec.requests.memory` represent the resources requests of the patroni container and the total resources requests
 calculated by adding the resources requests of all the containers (including the patroni container) you may set one or more of the following fields to `true`
 (depending on the resource's Pods you need this behaviour to be changed):
 
* `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.coordinator.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGShardedCluster.spec.shards.overwrites.pods.resources.disableResourcesRequestsSplitFromTotal`
* `SGDistributedLogs.spec.resources.disableResourcesRequestsSplitFromTotal`


{{< crd-properties >}}
[
  {
    "anchor": "sginstanceprofilespecrequestscontainerskey",
    "description": "The CPU(s) (cores) and RAM requests assigned to containers other than patroni container.",
    "name": "containers",
    "required": false,
    "type": "map[string]object"
  },
  {
    "description": "CPU(s) (cores) requests for every resource's Pod that reference this SGInstanceProfile. The suffix `m`\n  specifies millicpus (where 1000m is equal to 1).\n\nBy default the cpu requests values in field `.spec.requests.cpu` represent the total cpu requests assigned to each resource's Pod that reference this SGInstanceProfile.\n The cpu requests of the patroni container (that runs both Patroni and PostgreSQL) is calculated by subtracting from the total cpu requests the cpu requests of other containers that are present in the Pod.\n To change this behavior and have the cpu requests values in field `.spec.requests.cpu` represent the cpu requests of the patroni container and the total cpu requests\n calculated by adding the cpu requests of all the containers (including the patroni container) you may set one or more of the following fields to `true`\n (depending on the resource's Pods you need this behaviour to be changed):\n \n* `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.coordinator.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.shards.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.shards.overwrites.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGDistributedLogs.spec.resources.disableResourcesRequestsSplitFromTotal`",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sginstanceprofilespecrequestsinitcontainerskey",
    "description": "The CPU(s) (cores) and RAM requests assigned to init containers.",
    "name": "initContainers",
    "required": false,
    "type": "map[string]object"
  },
  {
    "description": "RAM requests for every resource's Pod that reference this SGInstanceProfile. The suffix `Mi` or `Gi`\n  specifies Mebibytes or Gibibytes, respectively.\n\nBy default the memory requests values in field `.spec.requests.memory` represent the total memory requests assigned to each resource's Pod that reference this SGInstanceProfile.\n The memory requests of the patroni container (that runs both Patroni and PostgreSQL) is calculated by subtracting from the total memory requests the memory requests of other containers that are present in the Pod.\n To change this behavior and have the memory requests values in field `.spec.requests.memory` represent the memory requests of the patroni container and the total memory requests\n calculated by adding the memory requests of all the containers (including the patroni container) you may set one or more of the following fields to `true`\n (depending on the resource's Pods you need this behaviour to be changed):\n \n* `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.coordinator.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.shards.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGShardedCluster.spec.shards.overwrites.pods.resources.disableResourcesRequestsSplitFromTotal`\n* `SGDistributedLogs.spec.resources.disableResourcesRequestsSplitFromTotal`",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGInstanceProfile.spec.requests.containers[key]
<sup><sup>[↩ Parent](#sginstanceprofilespecrequests)</sup></sup>

The CPU(s) (cores) and RAM requests assigned to a container.

This section, if left empty, will be filled automatically by the operator with
  some defaults that can be proportional to the resources assigned to patroni
  container (except for the huge pages that are always left untouched).


{{< crd-properties >}}
[
  {
    "description": "CPU(s) (cores) requests for the specified container. The suffix `m`\n  specifies millicpus (where 1000m is equal to 1).",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "RAM requests for the specified container. The suffix `Mi` or `Gi`\n  specifies Mebibytes or Gibibytes, respectively.",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGInstanceProfile.spec.requests.initContainers[key]
<sup><sup>[↩ Parent](#sginstanceprofilespecrequests)</sup></sup>

The CPU(s) (cores) and RAM requests assigned to an init container.

This section will be filled automatically by the operator with
  the same values of the resources requests assigned to patroni
  container (except for the huge pages that are always left untouched).


{{< crd-properties >}}
[
  {
    "description": "CPU(s) (cores) requests for the specified init container. The suffix\n  `m` specifies millicpus (where 1000m is equal to 1).",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "RAM requests for the specified init container. The suffix `Mi`\n  or `Gi` specifies Mebibytes or Gibibytes, respectively.",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}
