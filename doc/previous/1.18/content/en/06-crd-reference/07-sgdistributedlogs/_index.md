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
    "description": "SGDistributedLogs",
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
    "anchor": "sgdistributedlogsspec",
    "description": "",
    "name": "spec",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsstatus",
    "description": "",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGDistributedLogs.spec
<sup><sup>[↩ Parent](#sgdistributedlogs)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecpersistentvolume",
    "description": "Pod's persistent volume configuration",
    "name": "persistentVolume",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsspecconfigurations",
    "description": "Cluster custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsspecmetadata",
    "description": "Metadata information for cluster created resources.",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsspecnonproductionoptions",
    "description": "",
    "name": "nonProductionOptions",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsspecpostgresservices",
    "description": "Kubernetes [services](https://kubernetes.io/docs/concepts/services-networking/service/) created or managed by StackGres.",
    "name": "postgresServices",
    "required": false,
    "type": "object"
  },
  {
    "default": "production",
    "description": "The profile allow to change in a convenient place a set of configuration defaults that affect how the cluster is generated.\n\nAll those defaults can be overwritten by setting the corresponding fields.\n\nAvailable profiles are:\n\n* `production`:\n\n  Prevents two Pods from running in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `false` by default).\n  Sets both limits and requests using `SGInstanceProfile` for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `false` by default).\n  Sets requests using the referenced `SGInstanceProfile` for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `false` by default).\n\n* `testing`:\n\n  Allows two Pods to run in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `true` by default).\n  Sets both limits and requests using `SGInstanceProfile` for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `false` by default).\n  Sets requests using the referenced `SGInstanceProfile` for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `false` by default).\n\n* `development`:\n\n  Allows two Pods to run in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `true` by default).\n  Unset both limits and requests for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `true` by default).\n  Unsets requests for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `true` by default).\n\n**Changing this field may require a restart.**",
    "name": "profile",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdistributedlogsspecresources",
    "description": "Pod custom resources configuration.",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsspecscheduling",
    "description": "Pod custom scheduling and affinity configuration.\n\n**Changing this field may require a restart.**",
    "name": "scheduling",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGInstanceProfile](https://stackgres.io/doc/latest/04-postgres-cluster-management/03-resource-profiles/). A SGInstanceProfile defines CPU and memory limits. Must exist before creating a distributed logs. When no profile is set, a default (currently: 1 core, 2 GiB RAM) one is used.\n\n**Changing this field may require a restart.**",
    "name": "sgInstanceProfile",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.spec.persistentVolume
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Pod's persistent volume configuration

{{< crd-properties >}}
[
  {
    "description": "Size of the PersistentVolume set for the pod of the cluster for distributed logs. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).",
    "name": "size",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.",
    "name": "storageClass",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.spec.configurations
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Cluster custom configurations.


{{< crd-properties >}}
[
  {
    "description": "Name of the [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig) used for the distributed logs. It must exist. When not set, a default Postgres config, for the major version selected, is used.\n\n**Changing this field may require a restart.**",
    "name": "sgPostgresConfig",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.spec.metadata
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Metadata information for cluster created resources.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecmetadataannotations",
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsspecmetadatalabels",
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.",
    "name": "labels",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.spec.metadata.annotations
<sup><sup>[↩ Parent](#sgdistributedlogsspecmetadata)</sup></sup>

Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.

**Example:**


```yaml

apiVersion: stackgres.io/v1
kind: SGDistributedLogs
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



{{< crd-properties >}}
[
  {
    "description": "Annotations to attach to any resource created or managed by StackGres.",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Annotations to attach to pods created or managed by StackGres.",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) passed to the `-primary` service.",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) passed to the `-replicas` service.",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Annotations to attach to all services created or managed by StackGres.",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.spec.metadata.labels
<sup><sup>[↩ Parent](#sgdistributedlogsspecmetadata)</sup></sup>

Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.

**Example:**


```yaml

apiVersion: stackgres.io/v1
kind: SGDistributedLogs
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



{{< crd-properties >}}
[
  {
    "description": "Labels to attach to Pods created or managed by StackGres.",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Labels to attach to Services and Endpoints created or managed by StackGres.",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.spec.nonProductionOptions
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "It is a best practice, on non-containerized environments, when running production workloads, to run each database server on a different server (virtual or physical), i.e., not to co-locate more than one database server per host.\n\nThe same best practice applies to databases on containers. By default, StackGres will not allow to run more than one StackGres or Distributed Logs pod on a given Kubernetes node. If set to `true` it will allow more than one StackGres pod per node.\n\n**Changing this field may require a restart.**",
    "name": "disableClusterPodAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "It is a best practice, on containerized environments, when running production workloads, to enforce container's resources requirements.\n\nBy default, StackGres will configure resource requirements for all the containers. Set this property to true to prevent StackGres from setting container's resources requirements (except for patroni container, see `disablePatroniResourceRequirements`).\n\n**Changing this field may require a restart.**",
    "name": "disableClusterResourceRequirements",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "It is a best practice, on containerized environments, when running production workloads, to enforce container's resources requirements.\n\nThe same best practice applies to databases on containers. By default, StackGres will configure resource requirements for patroni container. Set this property to true to prevent StackGres from setting patroni container's resources requirement.\n\n**Changing this field may require a restart.**",
    "name": "disablePatroniResourceRequirements",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less cpu than it requires. It also allow to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.\n\nBy default, StackGres will configure cpu requirements to have the same limit and request for all the containers. Set this property to true to prevent StackGres from setting container's cpu requirements request equals to the limit (except for patroni container, see `enablePatroniCpuRequests`)\n  when `.spec.requests.containers.\u003ccontainer name\u003e.cpu` `.spec.requests.initContainers.\u003ccontainer name\u003e.cpu` is configured in the referenced `SGInstanceProfile`.\n\n**Changing this field may require a restart.**",
    "name": "enableSetClusterCpuRequests",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less memory than it requires.\n\nBy default, StackGres will configure memory requirements to have the same limit and request for all the containers. Set this property to true to prevent StackGres from setting container's memory requirements request equals to the limit (except for patroni container, see `enablePatroniCpuRequests`)\n  when `.spec.requests.containers.\u003ccontainer name\u003e.memory` `.spec.requests.initContainers.\u003ccontainer name\u003e.memory` is configured in the referenced `SGInstanceProfile`.\n\n**Changing this field may require a restart.**",
    "name": "enableSetClusterMemoryRequests",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less cpu than it requires. It also allow to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.\n\nBy default, StackGres will configure cpu requirements to have the same limit and request for the patroni container. Set this property to true to prevent StackGres from setting patroni container's cpu requirements request equals to the limit\n  when `.spec.requests.cpu` is configured in the referenced `SGInstanceProfile`.\n\n**Changing this field may require a restart.**",
    "name": "enableSetPatroniCpuRequests",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less memory than it requires.\n\nBy default, StackGres will configure memory requirements to have the same limit and request for the patroni container. Set this property to true to prevent StackGres from setting patroni container's memory requirements request equals to the limit\n  when `.spec.requests.memory` is configured in the referenced `SGInstanceProfile`.\n\n**Changing this field may require a restart.**",
    "name": "enableSetPatroniMemoryRequests",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

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



{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecpostgresservicesprimary",
    "description": "Configuration for the `-primary` service. It provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the cluster.",
    "name": "primary",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsspecpostgresservicesreplicas",
    "description": "Configuration for the `-replicas` service. It provides a stable connection (regardless of replica node failures) to any read-only Postgres server of the cluster. Read-only servers are load-balanced via this service.",
    "name": "replicas",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.spec.postgresServices.primary
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservices)</sup></sup>

Configuration for the `-primary` service. It provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the cluster.

{{< crd-properties >}}
[
  {
    "description": "allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is \"true\". It may be set to \"false\" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.",
    "name": "allocateLoadBalancerNodePorts",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#allocateloadbalancernodeports-v1-core",
    "name": "externalIPs",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "externalTrafficPolicy describes how nodes distribute service traffic they receive on one of the Service's \"externally-facing\" addresses (NodePorts, ExternalIPs, and LoadBalancer IPs). If set to \"Local\", the proxy will configure the service in a way that assumes that external load balancers will take care of balancing the service traffic between nodes, and so each node will deliver traffic only to the node-local endpoints of the service, without masquerading the client source IP. (Traffic mistakenly sent to a node with no endpoints will be dropped.) The default value, \"Cluster\", uses the standard behavior of routing to all endpoints evenly (possibly modified by topology and other features). Note that traffic sent to an External IP or LoadBalancer IP from within the cluster will always get \"Cluster\" semantics, but clients sending to a NodePort from within the cluster may need to take traffic policy into account when picking a node.",
    "name": "externalTrafficPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "healthCheckNodePort specifies the healthcheck nodePort for the service. This only applies when type is set to LoadBalancer and externalTrafficPolicy is set to Local. If a value is specified, is in-range, and is not in use, it will be used.  If not specified, a value will be automatically allocated.  External systems (e.g. load-balancers) can use this port to determine if a given node holds endpoints for this service or not.  If this field is specified when creating a Service which does not need it, creation will fail. This field will be wiped when updating a Service to no longer need it (e.g. changing type). This field cannot be updated once set.",
    "format": "int32",
    "name": "healthCheckNodePort",
    "required": false,
    "type": "integer"
  },
  {
    "description": "InternalTrafficPolicy describes how nodes distribute service traffic they receive on the ClusterIP. If set to \"Local\", the proxy will assume that pods only want to talk to endpoints of the service on the same node as the pod, dropping the traffic if there are no local endpoints. The default value, \"Cluster\", uses the standard behavior of routing to all endpoints evenly (possibly modified by topology and other features).",
    "name": "internalTrafficPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "IPFamilies is a list of IP families (e.g. IPv4, IPv6) assigned to this service. This field is usually assigned automatically based on cluster configuration and the ipFamilyPolicy field. If this field is specified manually, the requested family is available in the cluster, and ipFamilyPolicy allows it, it will be used; otherwise creation of the service will fail. This field is conditionally mutable: it allows for adding or removing a secondary IP family, but it does not allow changing the primary IP family of the Service. Valid values are \"IPv4\" and \"IPv6\".  This field only applies to Services of types ClusterIP, NodePort, and LoadBalancer, and does apply to \"headless\" services. This field will be wiped when updating a Service to type ExternalName.\n\nThis field may hold a maximum of two entries (dual-stack families, in either order).  These families must correspond to the values of the clusterIPs field, if specified. Both clusterIPs and ipFamilies are governed by the ipFamilyPolicy field.",
    "name": "ipFamilies",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "IPFamilyPolicy represents the dual-stack-ness requested or required by this Service. If there is no value provided, then this field will be set to SingleStack. Services can be \"SingleStack\" (a single IP family), \"PreferDualStack\" (two IP families on dual-stack configured clusters or a single IP family on single-stack clusters), or \"RequireDualStack\" (two IP families on dual-stack configured clusters, otherwise fail). The ipFamilies and clusterIPs fields depend on the value of this field. This field will be wiped when updating a service to type ExternalName.",
    "name": "ipFamilyPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "loadBalancerClass is the class of the load balancer implementation this Service belongs to. If specified, the value of this field must be a label-style identifier, with an optional prefix, e.g. \"internal-vip\" or \"example.com/internal-vip\". Unprefixed names are reserved for end-users. This field can only be set when the Service type is 'LoadBalancer'. If not set, the default load balancer implementation is used, today this is typically done through the cloud provider integration, but should apply for any default implementation. If set, it is assumed that a load balancer implementation is watching for Services with a matching class. Any default load balancer implementation (e.g. cloud providers) should ignore Services that set this field. This field can only be set when creating or updating a Service to type 'LoadBalancer'. Once set, it can not be changed. This field will be wiped when a service is updated to a non 'LoadBalancer' type.",
    "name": "loadBalancerClass",
    "required": false,
    "type": "string"
  },
  {
    "description": "Only applies to Service Type: LoadBalancer. This feature depends on whether the underlying cloud-provider supports specifying the loadBalancerIP when a load balancer is created. This field will be ignored if the cloud-provider does not support the feature. Deprecated: This field was under-specified and its meaning varies across implementations. Using it is non-portable and it may not support dual-stack. Users are encouraged to use implementation-specific annotations when available.",
    "name": "loadBalancerIP",
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified and supported by the platform, this will restrict traffic through the cloud-provider load-balancer will be restricted to the specified client IPs. This field will be ignored if the cloud-provider does not support the feature.\" More info: https://kubernetes.io/docs/tasks/access-application-cluster/create-external-load-balancer/",
    "name": "loadBalancerSourceRanges",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgdistributedlogsspecpostgresservicesprimarynodeports",
    "description": "nodePorts is a list of ports for exposing a cluster services to the outside world",
    "name": "nodePorts",
    "required": false,
    "type": "object"
  },
  {
    "description": "publishNotReadyAddresses indicates that any agent which deals with endpoints for this Service should disregard any indications of ready/not-ready. The primary use case for setting this field is for a StatefulSet's Headless Service to propagate SRV DNS records for its Pods for the purpose of peer discovery. The Kubernetes controllers that generate Endpoints and EndpointSlice resources for Services interpret this to mean that all endpoints are considered \"ready\" even if the Pods themselves are not. Agents which consume only Kubernetes generated endpoints through the Endpoints or EndpointSlice resources can safely assume this behavior.",
    "name": "publishNotReadyAddresses",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Supports \"ClientIP\" and \"None\". Used to maintain session affinity. Enable client IP based session affinity. Must be ClientIP or None. Defaults to None. More info: https://kubernetes.io/docs/concepts/services-networking/service/#virtual-ips-and-service-proxies",
    "name": "sessionAffinity",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdistributedlogsspecpostgresservicesprimarysessionaffinityconfig",
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
    "default": "ClusterIP",
    "description": "type determines how the Service is exposed. Defaults to ClusterIP. Valid\noptions are ClusterIP, NodePort, LoadBalancer and None. \"ClusterIP\" allocates\na cluster-internal IP address for load-balancing to endpoints.\n\"NodePort\" builds on ClusterIP and allocates a port on every node.\n\"LoadBalancer\" builds on NodePort and creates\nan external load-balancer (if supported in the current cloud).\n\"None\" creates a headless service that can be used in conjunction with `.spec.pods.disableEnvoy`\n set to `true` in order to access the database using a DNS.\nMore info:\n* https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types\n* https://kubernetes.io/docs/concepts/services-networking/service/#headless-services\n* https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/",
    "enum": [
      "ClusterIP",
      "LoadBalancer",
      "NodePort",
      "None"
    ],
    "name": "type",
    "required": false,
    "type": "enum"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.postgresServices.primary.nodePorts
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservicesprimary)</sup></sup>

nodePorts is a list of ports for exposing a cluster services to the outside world

{{< crd-properties >}}
[
  {
    "description": "the node port that will be exposed to connect to Postgres instance",
    "name": "pgport",
    "required": false,
    "type": "integer"
  },
  {
    "description": "the node port that will be exposed to connect to Postgres instance for replication purpose",
    "name": "replicationport",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.postgresServices.primary.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservicesprimary)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecpostgresservicesprimarysessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.postgresServices.primary.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservicesprimarysessionaffinityconfig)</sup></sup>

ClientIPConfig represents the configurations of Client IP based session affinity.

{{< crd-properties >}}
[
  {
    "description": "timeoutSeconds specifies the seconds of ClientIP type session sticky time. The value must be \u003e0 \u0026\u0026 \u003c=86400(for 1 day) if ServiceAffinity == \"ClientIP\". Default value is 10800(for 3 hours).",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.spec.postgresServices.replicas
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservices)</sup></sup>

Configuration for the `-replicas` service. It provides a stable connection (regardless of replica node failures) to any read-only Postgres server of the cluster. Read-only servers are load-balanced via this service.

{{< crd-properties >}}
[
  {
    "description": "allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is \"true\". It may be set to \"false\" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.",
    "name": "allocateLoadBalancerNodePorts",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Specify if the `-replicas` service should be created or not.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#allocateloadbalancernodeports-v1-core",
    "name": "externalIPs",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "externalTrafficPolicy describes how nodes distribute service traffic they receive on one of the Service's \"externally-facing\" addresses (NodePorts, ExternalIPs, and LoadBalancer IPs). If set to \"Local\", the proxy will configure the service in a way that assumes that external load balancers will take care of balancing the service traffic between nodes, and so each node will deliver traffic only to the node-local endpoints of the service, without masquerading the client source IP. (Traffic mistakenly sent to a node with no endpoints will be dropped.) The default value, \"Cluster\", uses the standard behavior of routing to all endpoints evenly (possibly modified by topology and other features). Note that traffic sent to an External IP or LoadBalancer IP from within the cluster will always get \"Cluster\" semantics, but clients sending to a NodePort from within the cluster may need to take traffic policy into account when picking a node.",
    "name": "externalTrafficPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "healthCheckNodePort specifies the healthcheck nodePort for the service. This only applies when type is set to LoadBalancer and externalTrafficPolicy is set to Local. If a value is specified, is in-range, and is not in use, it will be used.  If not specified, a value will be automatically allocated.  External systems (e.g. load-balancers) can use this port to determine if a given node holds endpoints for this service or not.  If this field is specified when creating a Service which does not need it, creation will fail. This field will be wiped when updating a Service to no longer need it (e.g. changing type). This field cannot be updated once set.",
    "format": "int32",
    "name": "healthCheckNodePort",
    "required": false,
    "type": "integer"
  },
  {
    "description": "InternalTrafficPolicy describes how nodes distribute service traffic they receive on the ClusterIP. If set to \"Local\", the proxy will assume that pods only want to talk to endpoints of the service on the same node as the pod, dropping the traffic if there are no local endpoints. The default value, \"Cluster\", uses the standard behavior of routing to all endpoints evenly (possibly modified by topology and other features).",
    "name": "internalTrafficPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "IPFamilies is a list of IP families (e.g. IPv4, IPv6) assigned to this service. This field is usually assigned automatically based on cluster configuration and the ipFamilyPolicy field. If this field is specified manually, the requested family is available in the cluster, and ipFamilyPolicy allows it, it will be used; otherwise creation of the service will fail. This field is conditionally mutable: it allows for adding or removing a secondary IP family, but it does not allow changing the primary IP family of the Service. Valid values are \"IPv4\" and \"IPv6\".  This field only applies to Services of types ClusterIP, NodePort, and LoadBalancer, and does apply to \"headless\" services. This field will be wiped when updating a Service to type ExternalName.\n\nThis field may hold a maximum of two entries (dual-stack families, in either order).  These families must correspond to the values of the clusterIPs field, if specified. Both clusterIPs and ipFamilies are governed by the ipFamilyPolicy field.",
    "name": "ipFamilies",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "IPFamilyPolicy represents the dual-stack-ness requested or required by this Service. If there is no value provided, then this field will be set to SingleStack. Services can be \"SingleStack\" (a single IP family), \"PreferDualStack\" (two IP families on dual-stack configured clusters or a single IP family on single-stack clusters), or \"RequireDualStack\" (two IP families on dual-stack configured clusters, otherwise fail). The ipFamilies and clusterIPs fields depend on the value of this field. This field will be wiped when updating a service to type ExternalName.",
    "name": "ipFamilyPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "loadBalancerClass is the class of the load balancer implementation this Service belongs to. If specified, the value of this field must be a label-style identifier, with an optional prefix, e.g. \"internal-vip\" or \"example.com/internal-vip\". Unprefixed names are reserved for end-users. This field can only be set when the Service type is 'LoadBalancer'. If not set, the default load balancer implementation is used, today this is typically done through the cloud provider integration, but should apply for any default implementation. If set, it is assumed that a load balancer implementation is watching for Services with a matching class. Any default load balancer implementation (e.g. cloud providers) should ignore Services that set this field. This field can only be set when creating or updating a Service to type 'LoadBalancer'. Once set, it can not be changed. This field will be wiped when a service is updated to a non 'LoadBalancer' type.",
    "name": "loadBalancerClass",
    "required": false,
    "type": "string"
  },
  {
    "description": "Only applies to Service Type: LoadBalancer. This feature depends on whether the underlying cloud-provider supports specifying the loadBalancerIP when a load balancer is created. This field will be ignored if the cloud-provider does not support the feature. Deprecated: This field was under-specified and its meaning varies across implementations. Using it is non-portable and it may not support dual-stack. Users are encouraged to use implementation-specific annotations when available.",
    "name": "loadBalancerIP",
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified and supported by the platform, this will restrict traffic through the cloud-provider load-balancer will be restricted to the specified client IPs. This field will be ignored if the cloud-provider does not support the feature.\" More info: https://kubernetes.io/docs/tasks/access-application-cluster/create-external-load-balancer/",
    "name": "loadBalancerSourceRanges",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgdistributedlogsspecpostgresservicesreplicasnodeports",
    "description": "nodePorts is a list of ports for exposing a cluster services to the outside world",
    "name": "nodePorts",
    "required": false,
    "type": "object"
  },
  {
    "description": "publishNotReadyAddresses indicates that any agent which deals with endpoints for this Service should disregard any indications of ready/not-ready. The primary use case for setting this field is for a StatefulSet's Headless Service to propagate SRV DNS records for its Pods for the purpose of peer discovery. The Kubernetes controllers that generate Endpoints and EndpointSlice resources for Services interpret this to mean that all endpoints are considered \"ready\" even if the Pods themselves are not. Agents which consume only Kubernetes generated endpoints through the Endpoints or EndpointSlice resources can safely assume this behavior.",
    "name": "publishNotReadyAddresses",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Supports \"ClientIP\" and \"None\". Used to maintain session affinity. Enable client IP based session affinity. Must be ClientIP or None. Defaults to None. More info: https://kubernetes.io/docs/concepts/services-networking/service/#virtual-ips-and-service-proxies",
    "name": "sessionAffinity",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdistributedlogsspecpostgresservicesreplicassessionaffinityconfig",
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
    "default": "ClusterIP",
    "description": "type determines how the Service is exposed. Defaults to ClusterIP. Valid\noptions are ClusterIP, NodePort, LoadBalancer and None. \"ClusterIP\" allocates\na cluster-internal IP address for load-balancing to endpoints.\n\"NodePort\" builds on ClusterIP and allocates a port on every node.\n\"LoadBalancer\" builds on NodePort and creates\nan external load-balancer (if supported in the current cloud).\n\"None\" creates a headless service that can be used in conjunction with `.spec.pods.disableEnvoy`\n set to `true` in order to access the database using a DNS.\nMore info:\n* https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types\n* https://kubernetes.io/docs/concepts/services-networking/service/#headless-services\n* https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/",
    "enum": [
      "ClusterIP",
      "LoadBalancer",
      "NodePort",
      "None"
    ],
    "name": "type",
    "required": false,
    "type": "enum"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.postgresServices.replicas.nodePorts
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservicesreplicas)</sup></sup>

nodePorts is a list of ports for exposing a cluster services to the outside world

{{< crd-properties >}}
[
  {
    "description": "the node port that will be exposed to connect to Postgres instance",
    "name": "pgport",
    "required": false,
    "type": "integer"
  },
  {
    "description": "the node port that will be exposed to connect to Postgres instance for replication purpose",
    "name": "replicationport",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.postgresServices.replicas.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservicesreplicas)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecpostgresservicesreplicassessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.postgresServices.replicas.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgdistributedlogsspecpostgresservicesreplicassessionaffinityconfig)</sup></sup>

ClientIPConfig represents the configurations of Client IP based session affinity.

{{< crd-properties >}}
[
  {
    "description": "timeoutSeconds specifies the seconds of ClientIP type session sticky time. The value must be \u003e0 \u0026\u0026 \u003c=86400(for 1 day) if ServiceAffinity == \"ClientIP\". Default value is 10800(for 3 hours).",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.spec.resources
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Pod custom resources configuration.

{{< crd-properties >}}
[
  {
    "description": "When set to `true` the resources requests values in fields `SGInstanceProfile.spec.requests.cpu` and `SGInstanceProfile.spec.requests.memory` will represent the resources\n requests of the patroni container and the total resources requests calculated by adding the resources requests of all the containers (including the patroni container).\n\n**Changing this field may require a restart.**",
    "name": "disableResourcesRequestsSplitFromTotal",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When set to `true` resources limits for containers other than the patroni container will be set just like for patroni container as specified in the SGInstanceProfile.\n\n**Changing this field may require a restart.**",
    "name": "enableClusterLimitsRequirements",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.spec.scheduling
<sup><sup>[↩ Parent](#sgdistributedlogsspec)</sup></sup>

Pod custom scheduling and affinity configuration.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinity",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "nodeAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "NodeSelector is a selector which must be true for the pod to fit on a node. Selector which must match a node's labels for the pod to be scheduled on that node. More info: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/",
    "name": "nodeSelector",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinity",
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core",
    "name": "podAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.",
    "name": "priorityClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingtolerationsindex",
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.spec.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgdistributedlogsspecscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
    "description": "A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.",
    "name": "preference",
    "requireRestart": true,
    "required": true,
    "type": "object"
  },
  {
    "description": "Weight associated with matching the corresponding nodeSelectorTerm, in the range 1-100.",
    "format": "int32",
    "name": "weight",
    "requireRestart": true,
    "required": true,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "The label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "The label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "The label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "The label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.spec.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgdistributedlogsspecscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
    "description": "Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key \u003ctopologyKey\u003e matches that of any node on which a pod of the set of pods is running",
    "name": "podAffinityTerm",
    "requireRestart": true,
    "required": true,
    "type": "object"
  },
  {
    "description": "weight associated with matching the corresponding podAffinityTerm, in the range 1-100.",
    "format": "int32",
    "name": "weight",
    "requireRestart": true,
    "required": true,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

{{< crd-properties >}}
[
  {
    "description": "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.",
    "name": "topologyKey",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "matchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "mismatchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "namespaceSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means \"this pod's namespace\".",
    "name": "namespaces",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

{{< crd-properties >}}
[
  {
    "description": "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.",
    "name": "topologyKey",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "matchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "mismatchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "namespaceSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means \"this pod's namespace\".",
    "name": "namespaces",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.spec.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgdistributedlogsspecscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
    "description": "Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key \u003ctopologyKey\u003e matches that of any node on which a pod of the set of pods is running",
    "name": "podAffinityTerm",
    "requireRestart": true,
    "required": true,
    "type": "object"
  },
  {
    "description": "weight associated with matching the corresponding podAffinityTerm, in the range 1-100.",
    "format": "int32",
    "name": "weight",
    "requireRestart": true,
    "required": true,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

{{< crd-properties >}}
[
  {
    "description": "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.",
    "name": "topologyKey",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "matchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "mismatchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "namespaceSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means \"this pod's namespace\".",
    "name": "namespaces",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

{{< crd-properties >}}
[
  {
    "description": "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.",
    "name": "topologyKey",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "matchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "mismatchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "namespaceSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means \"this pod's namespace\".",
    "name": "namespaces",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGDistributedLogs.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.spec.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgdistributedlogsspecscheduling)</sup></sup>

The pod this Toleration is attached to tolerates any taint that matches the triple <key,value,effect> using the matching operator <operator>.

{{< crd-properties >}}
[
  {
    "description": "Effect indicates the taint effect to match. Empty means match all taint effects. When specified, allowed values are NoSchedule, PreferNoSchedule and NoExecute.",
    "name": "effect",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "Key is the taint key that the toleration applies to. Empty means match all taint keys. If the key is empty, operator must be Exists; this combination means to match all values and all keys.",
    "name": "key",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator represents a key's relationship to the value. Valid operators are Exists, Equal, Lt, and Gt. Defaults to Equal. Exists is equivalent to wildcard for value, so that a pod can tolerate all taints of a particular category. Lt and Gt perform numeric comparisons (requires feature gate TaintTolerationComparisonOperators).",
    "name": "operator",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "TolerationSeconds represents the period of time the toleration (which must be of effect NoExecute, otherwise this field is ignored) tolerates the taint. By default, it is not set, which means tolerate the taint forever (do not evict). Zero and negative values will be treated as 0 (evict immediately) by the system.",
    "format": "int64",
    "name": "tolerationSeconds",
    "requireRestart": true,
    "required": false,
    "type": "integer"
  },
  {
    "description": "Value is the taint value the toleration matches to. If the operator is Exists, the value should be empty, otherwise just a regular string.",
    "name": "value",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

## SGDistributedLogs.status
<sup><sup>[↩ Parent](#sgdistributedlogs)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsstatusconditionsindex",
    "description": "",
    "name": "conditions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdistributedlogsstatusconnectedclustersindex",
    "description": "The list of connected `sgclusters`",
    "name": "connectedClusters",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdistributedlogsstatusdatabasesindex",
    "description": "The list of database status",
    "name": "databases",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "The hash of the configuration file that is used by fluentd",
    "name": "fluentdConfigHash",
    "required": false,
    "type": "string"
  },
  {
    "description": "The custom prefix that is prepended to all labels.",
    "name": "labelPrefix",
    "required": false,
    "type": "string"
  },
  {
    "description": "Flag to indicate the previous existing ConfigMap has been removed.",
    "name": "oldConfigMapRemoved",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The used Postgres version",
    "name": "postgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "The used Timescaledb version",
    "name": "timescaledbVersion",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.status.conditions[index]
<sup><sup>[↩ Parent](#sgdistributedlogsstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Last time the condition transitioned from one status to another.",
    "name": "lastTransitionTime",
    "required": false,
    "type": "string"
  },
  {
    "description": "A human readable message indicating details about the transition.",
    "name": "message",
    "required": false,
    "type": "string"
  },
  {
    "description": "The reason for the condition's last transition.",
    "name": "reason",
    "required": false,
    "type": "string"
  },
  {
    "description": "Status of the condition, one of True, False, Unknown.",
    "name": "status",
    "required": false,
    "type": "string"
  },
  {
    "description": "Type of deployment condition.",
    "name": "type",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.status.connectedClusters[index]
<sup><sup>[↩ Parent](#sgdistributedlogsstatus)</sup></sup>

A connected `sgcluster`

{{< crd-properties >}}
[
  {
    "anchor": "sgdistributedlogsstatusconnectedclustersindexconfig",
    "description": "The configuration for `sgdistributedlogs` of this `sgcluster`",
    "name": "config",
    "required": false,
    "type": "object"
  },
  {
    "description": "The `sgcluster` name",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "The `sgcluster` namespace",
    "name": "namespace",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGDistributedLogs.status.connectedClusters[index].config
<sup><sup>[↩ Parent](#sgdistributedlogsstatusconnectedclustersindex)</sup></sup>

The configuration for `sgdistributedlogs` of this `sgcluster`

{{< crd-properties >}}
[
  {
    "description": "The retention window that has been applied to tables",
    "name": "retention",
    "required": false,
    "type": "string"
  },
  {
    "description": "The `sgdistributedlogs` to which this `sgcluster` is connected to",
    "name": "sgDistributedLogs",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDistributedLogs.status.databases[index]
<sup><sup>[↩ Parent](#sgdistributedlogsstatus)</sup></sup>

A database status

{{< crd-properties >}}
[
  {
    "description": "The database name that has been created",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "The retention window that has been applied to tables",
    "name": "retention",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}
