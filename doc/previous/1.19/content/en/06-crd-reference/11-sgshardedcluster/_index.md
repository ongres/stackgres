---
title: SGShardedCluster
weight: 11
url: /reference/crd/sgshardedcluster
description: Details about SGShardedCluster
showToc: true
---

___

**Kind:** SGShardedCluster

**listKind:** SGShardedClusterList

**plural:** sgshardedclusters

**singular:** sgshardedcluster

**shortNames** sgscl
___

StackGres PostgreSQL sharded clusters are created using the `SGShardedCluster` Custom Resource.

**Example:**

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  postgres:
    version: 'latest'
  type: citus
  database: database
  coordinator:
    instances: 1
    pods:
      persistentVolume:
        size: '5Gi'
  workers:
    clusters: 2
    instancesPerCluster: 1
    pods:
      persistentVolume:
        size: '5Gi'
```

See also [Sharded Cluster Creation section]({{%  relref "04-administration-guide/14-sharded-cluster" %}}).





{{< crd-properties >}}
[
  {
    "description": "stackgres.io/v1beta1",
    "immutable": true,
    "name": "apiVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "SGShardedCluster",
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
    "anchor": "sgshardedclusterspec",
    "description": "Specification of the desired behavior of a StackGres sharded cluster.",
    "name": "spec",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterstatus",
    "description": "Current status of a StackGres sharded cluster.",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGShardedCluster.spec
<sup><sup>[↩ Parent](#sgshardedcluster)</sup></sup>

Specification of the desired behavior of a StackGres sharded cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinator",
    "description": "The coordinator is a StackGres cluster responsible for coordinating data storage and access to the workers.",
    "name": "coordinator",
    "required": true,
    "type": "object"
  },
  {
    "description": "The database name that will be created and used across all node and where \"partitioned\" (distributed) tables will live in.",
    "name": "database",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecpostgres",
    "description": "This section allows to configure Postgres features",
    "name": "postgres",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurations",
    "description": "Sharded cluster custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecdistributedlogs",
    "description": "StackGres features a functionality for all pods to send Postgres, Patroni and PgBouncer logs to a central (distributed) location, which is in turn another Postgres database. Logs can then be accessed via SQL interface or from the web UI. This section controls whether to enable this feature or not. If not enabled, logs are sent to the pod's standard output.",
    "name": "distributedLogs",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecinitialdata",
    "description": "Sharded cluster initialization data options. Sharded cluster may be initialized empty, or from a sharded backup restoration.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "initialData",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecmetadata",
    "description": "Metadata information from any cluster created resources.",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecnonproductionoptions",
    "description": "",
    "name": "nonProductionOptions",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecpostgresservices",
    "description": "Kubernetes [services](https://kubernetes.io/docs/concepts/services-networking/service/) created or managed by StackGres.",
    "name": "postgresServices",
    "required": false,
    "type": "object"
  },
  {
    "default": "production",
    "description": "The profile allows changing in a convenient place a set of configuration defaults that affect how the cluster is generated.\n\nAll those defaults can be overwritten by setting the correspoinding fields.\n\nAvailable profiles are:\n\n* `production`:\n\n  Prevents two Pods from running in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `false` by default).\n  Sets both limits and requests using `SGInstanceProfile` for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `false` by default).\n  Sets requests using the referenced `SGInstanceProfile` for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `false` by default).\n\n* `testing`:\n\n  Allows two Pods to running in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `true` by default).\n  Sets both limits and requests using `SGInstanceProfile` for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `false` by default).\n  Sets requests using the referenced `SGInstanceProfile` for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `false` by default).\n\n* `development`:\n\n  Allows two Pods from running in the same Node (set `.spec.nonProductionOptions.disableClusterPodAntiAffinity` to `true` by default).\n  Unset both limits and requests for `patroni` container that runs both Patroni and Postgres (set `.spec.nonProductionOptions.disablePatroniResourceRequirements` to `true` by default).\n  Unsets requests for sidecar containers other than `patroni` (set `.spec.nonProductionOptions.disableClusterResourceRequirements` to `true` by default).\n\n**Changing this field may require a restart.**",
    "name": "profile",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefrom",
    "description": "Make the sharded cluster a read-only standby replica allowing replication from another sharded cluster and acting as a relay.\n\nChanging this section is allowed to fix issues or to change the replication source.\n\nRemoving this section convert the sharded cluster in a normal sharded cluster where the standby leader of each SGCluster is converted into a primary instance.",
    "name": "replicateFrom",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplication",
    "description": "This section allows to configure the global Postgres replication mode.\n\nThe main replication group is implicit and contains the total number of instances less the sum of all\n  instances in other replication groups.\n\nThe total number of instances is always specified by `.spec.instances`.",
    "name": "replication",
    "required": false,
    "type": "object"
  },
  {
    "description": "**Deprecated** use `workers` instead.\n\nThe workers are a group of StackGres clusters where the partitioned data chunks are stored.\n\nWhen referring to the cluster in the descriptions below it applies to any worker's StackGres cluster.",
    "name": "shards",
    "required": false,
    "type": "object"
  },
  {
    "description": "The sharding technology that will be used for the sharded cluster.\n\nAvailable technologies are:\n\n* `citus`\n* `ddp`\n* `shardingsphere`\n\n**Citus**\n\nCitus is a PostgreSQL extension that transforms Postgres into a distributed database—so you can achieve high performance at any scale.\n\nSee also https://github.com/citusdata/citus\n\n**DDP**\n\nDDP (Distributed Data Partitioning) allows you to distribute data across different physical nodes to improve the query performance of high data volumes, taking advantage of distinct nodes’ resources. Using the entry point named coordinator in charge of sending/distributing the queries to different nodes named workers.\n\n**ShardingSphere**\n\nApache ShardingSphere is an ecosystem to transform any database into a distributed database system, and enhance it with sharding, elastic scaling, encryption features \u0026 more.\n\nStackGres implementation of ShardingSphere as a sharding technology uses the [ShardingSphere Proxy](https://shardingsphere.apache.org/document/current/en/quick-start/shardingsphere-proxy-quick-start/) as an entry point to distribute SQL traffic among the workers.\n\nThis implementation requires the [ShardingSphere Operator](https://shardingsphere.apache.org/oncloud/current/en/user-manual/cn-sn-operator/) to be installed and will create a ComputeNode",
    "name": "type",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkers",
    "description": "The workers are a group of StackGres clusters where the partitioned data chunks are stored.\n\nWhen referring to the cluster in the descriptions below it applies to any worker's StackGres cluster.",
    "name": "workers",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.coordinator
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

The coordinator is a StackGres cluster responsible for coordinating data storage and access to the workers.


{{< crd-properties >}}
[
  {
    "description": "Number of StackGres instances for the cluster. Each instance contains one Postgres server.\n  Out of all of the Postgres servers, one is elected as the primary, the rest remain as read-only replicas.\n\nIf sharding type is `shardingsphere` then, instead of an SGCluster a ComputeNode will be created.\n\nSee also https://shardingsphere.apache.org/oncloud/current/en/user-manual/cn-sn-operator/#computenode",
    "minimum": 0,
    "name": "instances",
    "required": true,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpods",
    "description": "Cluster pod's configuration.\n\nIf sharding type is `shardingsphere` then this section will apply to the ComputeNode.",
    "name": "pods",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscaling",
    "description": "This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.\n\nVertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.\nVertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.",
    "name": "autoscaling",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of coordinator StackGres cluster.\n\nWhen this name is specified it will be used as the coordinator SGCluster name.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "clusterName",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurations",
    "description": "Coordinator custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatormanagedsql",
    "description": "This section allows referencing SQL scripts that will be applied to the cluster live.\n\nIf sharding type is `shardingsphere` then this section will be applied to the first cluster worker.\n In this case the database postgres will also provide a foreign server called `shardingsphere` and\n the superuser user mappings that will allow running DistQL queries using command like the following:\n\n\n```\n\nSELECT * FROM dblink('shardingsphere', 'SHOW STORAGE UNITS')\n  AS _(name text, type text, host text, port int, db text,\n    connection_timeout_milliseconds int, idle_timeout_milliseconds int,\n    max_lifetime_milliseconds int, max_pool_size int, min_pool_size int,\n    read_only boolean, other_attributes text);\n\n```\n\n\nSee https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-proxy/distsql",
    "name": "managedSql",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatormetadata",
    "description": "Metadata information from coordinator cluster created resources.\n\nIf sharding type is `shardingsphere` then this section is applied to the ComputeNode.",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name template of query router worker's StackGres clusters.\n\nThe SGCluster name will be created using the name template and the index (starting at 0) as a suffix.\nFor instance with a `clusterNameTemplate` set to `myworker` the first SGCluster name will be `myworker0`.\n\nBy default the query router cluster name template is `\u003ccluster name\u003e-router`.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "queryRouterClusterNameTemplate",
    "required": false,
    "type": "string"
  },
  {
    "description": "Number of query router StackGres clusters, each composed of a single instance.\n\nQuery routers are special workers that do not store sharded table data. Like the coordinator, query routers\nare StackGres clusters responsible for coordinating access to the workers but do not coordinate data storage.\nThe instances created as query routers allow horizontal scaling of the entrypoints for read/write operations\nwithout overloading the workers that store the sharded table data.\n\nQuery router StackGres clusters inherit the same characteristics as the coordinator in their spec, with the\nexception of the `instances` which is fixed to one and the `managedSql` that requires an override to be set.\nThis is because a query router, like the coordinator, is not meant to store data and is architecturally more\nsimilar to a coordinator than to a worker. Their values can still be overridden via `.spec.workers.overrides`.\nSee also field `queryRouterIndexOffset`.\n\nThis parameter can only be set if `.spec.type` is `citus`.",
    "minimum": 0,
    "name": "queryRouterClusters",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Allow to set the index offset for query router workers. Needed only if you plan to have more than 1023 regular\nworker nodes.",
    "minimum": 1024,
    "name": "queryRouterIndexOffset",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorreplication",
    "description": "This section allows to configure the global Postgres replication mode.\n\nThe main replication group is implicit and contains the total number of instances less the sum of all\n  instances in other replication groups.\n\nThe total number of instances is always specified by `.spec.instances`.\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "replication",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGInstanceProfile](https://stackgres.io/doc/latest/reference/crd/sginstanceprofile/).\n\nA SGInstanceProfile defines CPU and memory limits. Must exist before creating a cluster.\n\nWhen no profile is set, a default (1 core, 2 GiB RAM) one is used.\n\n**Changing this field may require a restart.**",
    "name": "sgInstanceProfile",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.coordinator.pods
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinator)</sup></sup>

Cluster pod's configuration.

If sharding type is `shardingsphere` then this section will apply to the ComputeNode.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodspersistentvolume",
    "description": "Pod's persistent volume configuration.\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "persistentVolume",
    "required": true,
    "type": "object"
  },
  {
    "description": "A list of custom application containers that run within the coordinator cluster's Pods.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customContainers",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom application init containers that run within the workers cluster's Pods. The\n  custom init containers will run following the defined sequence at the end of\n  cluster's Pods init containers.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volumes that may be used along with any container defined in\n  customInitContainers or customContainers sections for the coordinator.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the customInitContainers or customContainers sections the name used\n  has to be prepended with the same prefix.\n\nOnly the following volume types are allowed: configMap, downwardAPI, emptyDir,\n  gitRepo, glusterfs, hostPath, nfs, projected and secret\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "customVolumes",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating a connection pooling (using [PgBouncer](https://www.pgbouncer.org/)) sidecar.\n\nIf sharding type is `citus` and `.spec.configurations.citus.connectToPooler` is `true` (the default) the nodes with this field set to `true` are not added to `pg_dist_poolinfo`, so Citus connects directly to their Postgres.\n\nIf sharding type is `shardingsphere` then this field is ignored.\n\n**Changing this field may require a restart.**",
    "name": "disableConnectionPooling",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "If set to `false`, creates the `envoy` sidecar. This sidecar is used as the edge proxy for the cluster's Pods providing extra metrics to the monitoring layer.\n\n**Changing this field may require a restart.**",
    "name": "disableEnvoy",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** use `.spec.configurations.observability.disableMetrics` instead.",
    "name": "disableMetricsExporter",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating the `postgres-util` sidecar. This sidecar contains usual Postgres administration utilities *that are not present in the main (`patroni`) container*, like `psql`. Only disable if you know what you are doing.\n\nIf sharding type is `shardingsphere` then this field is ignored.\n\n**Changing this field may require a restart.**",
    "name": "disablePostgresUtil",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsdnsconfig",
    "description": "PodDNSConfig defines the DNS parameters of a pod in addition to those generated from DNSPolicy.",
    "name": "dnsConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Set DNS policy for the pod. Defaults to \"ClusterFirst\". Valid values are 'ClusterFirstWithHostNet', 'ClusterFirst', 'Default' or 'None'. DNS parameters given in DNSConfig will be merged with the policy selected with DNSPolicy. To have DNS options set along with hostNetwork, you have to specify DNS policy explicitly to 'ClusterFirstWithHostNet'.",
    "name": "dnsPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "Host networking requested for this pod. Use the host's network namespace. When using HostNetwork you should specify ports so the scheduler is aware. When `hostNetwork` is true, specified `hostPort` fields in port definitions must match `containerPort`, and unspecified `hostPort` fields in port definitions are defaulted to match `containerPort`. Default to false.",
    "name": "hostNetwork",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodslivenessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "livenessProbe",
    "required": false,
    "type": "object"
  },
  {
    "description": "managementPolicy controls how pods are created during initial scale up, when replacing pods\n  on nodes, or when scaling down. The default policy is `OrderedReady`, where pods are created\n  in increasing order (pod-0, then pod-1, etc) and the controller will wait until each pod is\n  ready before continuing. When scaling down, the pods are removed in the opposite order.\n  The alternative policy is `Parallel` which will create pods in parallel to match the desired\n  scale without waiting, and on scale down will delete all pods at once.\n\nIf sharding type is `shardingsphere` then this field is ignored.",
    "name": "managementPolicy",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsreadinessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "readinessProbe",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsresources",
    "description": "Pod custom resources configuration.",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsscheduling",
    "description": "Pod custom scheduling, affinity and topology spread constratins configuration.\n\n**Changing this field may require a restart.**",
    "name": "scheduling",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "If true the pod's hostname will be configured as the pod's FQDN, rather than the leaf name (the default). In Linux containers, this means setting the FQDN in the hostname field of the kernel (the nodename field of struct utsname). In Windows containers, this means setting the registry value of hostname for the registry key HKEY_LOCAL_MACHINE\\\\SYSTEM\\\\CurrentControlSet\\\\Services\\\\Tcpip\\\\Parameters to FQDN. If a pod does not have FQDN, this has no effect. Default to false.",
    "name": "setHostnameAsFQDN",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsstartupprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "startupProbe",
    "required": false,
    "type": "object"
  },
  {
    "description": "serviceName is the name of the service that governs this StatefulSet. This service must exist before the StatefulSet, and is responsible for the network identity of the set. Pods get DNS/hostnames that follow the pattern: pod-specific-string.serviceName.default.svc.cluster.local where \"pod-specific-string\" is managed by the StatefulSet controller.",
    "name": "statefulSetServiceName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully. May be decreased in delete request. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). If this value is nil, the default grace period will be used instead. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. Defaults to 30 seconds.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsupdatestrategy",
    "default": {
      "type": "OnlyDbOps"
    },
    "description": "This section indicates the strategy that the SGCluster controller will use to perform updates.\n\nIt includes any additional parameters necessary to perform the update for the indicated strategy.\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.",
    "name": "updateStrategy",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.persistentVolume
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpods)</sup></sup>

Pod's persistent volume configuration.

If sharding type is `shardingsphere` then this section is ignored.


{{< crd-properties >}}
[
  {
    "description": "Size of the PersistentVolume set for each instance of the cluster. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).\n\nIf sharding type is `shardingsphere` then this field is ignored.",
    "name": "size",
    "required": true,
    "type": "string"
  },
  {
    "description": "fsGroupChangePolicy defines behavior of changing ownership and permission of the volume before being exposed inside Pod. This field will only apply to volume types which support fsGroup based ownership(and permissions). It will have no effect on ephemeral volume types such as: secret, configmaps and emptydir. Valid values are \"OnRootMismatch\" and \"Always\". If not specified, \"Always\" is used. Note that this field cannot be set when spec.os.name is windows.",
    "name": "fsGroupChangePolicy",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodspersistentvolumeiolimits",
    "description": "This section allows to define per-volume I/O limits for nodes using [cgroup v2 `io.max` features](https://docs.kernel.org/admin-guide/cgroup-v2.html).\n\n\u003e **NOTE**: This is an **alpha** feature.\n\nWhen multiple PostgreSQL clusters share the same local storage device on a Kubernetes node, a single cluster running a heavy workload\n (for example large COPY, aggressive VACUUM, or an unoptimized query) can saturate the device's I/O bandwidth and degrade performance\n for every other cluster on that node. StackGres provides this mechanism to prevent this class of noisy-neighbor problem.\nThe mechanism operates at the cgroup + block-device layer, not at the PVC abstraction. Since each pod has its own cgroup, limits are\n enforced independently across pods even when they share the same physical device — this is the core of the noisy-neighbor protection.\nHowever, if multiple volumes within the same pod share the same backing device, their limits target a single entry in that pod's cgroup\n and cannot be enforced independently of each other.\n\nThis feature works on top of existing CSI drivers such as TopoLVM or OpenEBS LocalPV. The primary target is on-premises deployments where\n multiple StackGres clusters share local NVMe storage on the same node.\n\n\u003e **IMPORTANT**: Setting any of the value above will require to create an init container named `setup-io-limits` in each SGCluster's Pods\n\u003e  running as root with only the `CHOWN` cabability set. Also both the `setup-io-limits` init container and the `cluster-controller`\n\u003e  container will require to mount the host path `/sys/fs/cgroup` in order to be able to set owner of and write to the `io.max` file that\n\u003e  will allows to set the io limits.\n\u003e  For this reason this feature must be enabled globally by the operator administrator by adding the `io-limits` feature gate under\n\u003e  `.spec.featureGates` of the SGConfig. Setting any of the value above without the `io-limits` feature gate enabled will be rejected.",
    "name": "ioLimits",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.\n\nIf sharding type is `shardingsphere` then this field is ignored.",
    "name": "storageClass",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.\n\nSee https://kubernetes.io/docs/concepts/storage/volume-attributes-classes/",
    "name": "volumeAttributesClassName",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.persistentVolume.ioLimits
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodspersistentvolume)</sup></sup>

This section allows to define per-volume I/O limits for nodes using [cgroup v2 `io.max` features](https://docs.kernel.org/admin-guide/cgroup-v2.html).

> **NOTE**: This is an **alpha** feature.

When multiple PostgreSQL clusters share the same local storage device on a Kubernetes node, a single cluster running a heavy workload
 (for example large COPY, aggressive VACUUM, or an unoptimized query) can saturate the device's I/O bandwidth and degrade performance
 for every other cluster on that node. StackGres provides this mechanism to prevent this class of noisy-neighbor problem.
The mechanism operates at the cgroup + block-device layer, not at the PVC abstraction. Since each pod has its own cgroup, limits are
 enforced independently across pods even when they share the same physical device — this is the core of the noisy-neighbor protection.
However, if multiple volumes within the same pod share the same backing device, their limits target a single entry in that pod's cgroup
 and cannot be enforced independently of each other.

This feature works on top of existing CSI drivers such as TopoLVM or OpenEBS LocalPV. The primary target is on-premises deployments where
 multiple StackGres clusters share local NVMe storage on the same node.

> **IMPORTANT**: Setting any of the value above will require to create an init container named `setup-io-limits` in each SGCluster's Pods
>  running as root with only the `CHOWN` cabability set. Also both the `setup-io-limits` init container and the `cluster-controller`
>  container will require to mount the host path `/sys/fs/cgroup` in order to be able to set owner of and write to the `io.max` file that
>  will allows to set the io limits.
>  For this reason this feature must be enabled globally by the operator administrator by adding the `io-limits` feature gate under
>  `.spec.featureGates` of the SGConfig. Setting any of the value above without the `io-limits` feature gate enabled will be rejected.


{{< crd-properties >}}
[
  {
    "description": "Max read IO operations per second. Leave empty to remove this limit.",
    "name": "readIops",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max read MiB per second. Leave empty to remove this limit.",
    "name": "readMiBps",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max write IO operations per second. Leave empty to remove this limit.",
    "name": "writeIops",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max write MiB per second. Leave empty to remove this limit.",
    "name": "writeMiBps",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.dnsConfig
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpods)</sup></sup>

PodDNSConfig defines the DNS parameters of a pod in addition to those generated from DNSPolicy.

{{< crd-properties >}}
[
  {
    "description": "A list of DNS name server IP addresses. This will be appended to the base nameservers generated from DNSPolicy. Duplicated nameservers will be removed.",
    "name": "nameservers",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsdnsconfigoptionsindex",
    "description": "A list of DNS resolver options. This will be merged with the base options generated from DNSPolicy. Duplicated entries will be removed. Resolution options given in Options will override those that appear in the base DNSPolicy.",
    "name": "options",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of DNS search domains for host-name lookup. This will be appended to the base search paths generated from DNSPolicy. Duplicated search paths will be removed.",
    "name": "searches",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.dnsConfig.options[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsdnsconfig)</sup></sup>

PodDNSConfigOption defines DNS resolver options of a pod.

{{< crd-properties >}}
[
  {
    "description": "Name is this DNS resolver option's name. Required.",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "Value is this DNS resolver option's value.",
    "name": "value",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.livenessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.readinessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.resources
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpods)</sup></sup>

Pod custom resources configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsresourcescontainerskey",
    "description": "Resources configuration to be merged with the specified container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "containers",
    "required": false,
    "type": "map[string]object"
  },
  {
    "description": "When set to `true` the resources requests values in fields `SGInstanceProfile.spec.requests.cpu` and `SGInstanceProfile.spec.requests.memory` will represent the resources\n requests of the patroni container and the total resources requests calculated by adding the resources requests of all the containers (including the patroni container).\n\n**Changing this field may require a restart.**",
    "name": "disableResourcesRequestsSplitFromTotal",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When enabled resource limits for containers other than the patroni container will be set just like for patroni container as specified in the SGInstanceProfile.\n\n**Changing this field may require a restart.**",
    "name": "enableClusterLimitsRequirements",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When set to `true` the reconciliation of the cluster will fail if `disableResourcesRequestsSplitFromTotal` is not set or set to `false` and the sum of the CPU or memory\n of all the containers except patroni is equal to or higher than the total specified in `SGInstanceProfile.spec.requests.cpu` or `SGInstanceProfile.spec.requests.memory`.\n\nWhen `false` (the default) and `disableResourcesRequestsSplitFromTotal` is not set or set to `false` and the sum of the CPU or memory\n of all the containers except patroni is equal to or higher than the total specified in `SGInstanceProfile.spec.requests.cpu` or `SGInstanceProfile.spec.requests.memory`\n then the patroni container resources will be set to 0.",
    "name": "failWhenTotalIsHigher",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsresourcesinitcontainerskey",
    "description": "Resources configuration to be merged with the specified init container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "initContainers",
    "required": false,
    "type": "map[string]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.resources.containers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsresourcescontainerskeyclaimsindex",
    "description": "Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.\n\nThis field depends on the DynamicResourceAllocation feature gate.\n\nThis field is immutable. It can only be set for containers.",
    "name": "claims",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "limits",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "requests",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.resources.containers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsresourcescontainerskey)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

{{< crd-properties >}}
[
  {
    "description": "Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.",
    "name": "request",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.resources.initContainers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsresourcesinitcontainerskeyclaimsindex",
    "description": "Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.\n\nThis field depends on the DynamicResourceAllocation feature gate.\n\nThis field is immutable. It can only be set for containers.",
    "name": "claims",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "limits",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "requests",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.resources.initContainers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsresourcesinitcontainerskey)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

{{< crd-properties >}}
[
  {
    "description": "Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.",
    "name": "request",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpods)</sup></sup>

Pod custom scheduling, affinity and topology spread constratins configuration.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackup",
    "description": "Backup Pod custom scheduling and affinity configuration.",
    "name": "backup",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core",
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
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podantiaffinity-v1-core",
    "name": "podAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "PreemptionPolicy is the Policy for preempting pods with lower priority. One of Never, PreemptLowerPriority. Defaults to PreemptLowerPriority if unset.",
    "name": "preemptionPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.",
    "name": "priorityClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "RuntimeClassName refers to a RuntimeClass object in the node.k8s.io group, which should be used to run this pod.  If no RuntimeClass resource matches the named class, the pod will not be run. If unset or empty, the \"legacy\" RuntimeClass will be used, which is an implicit class with an empty definition that uses the default runtime handler. More info: https://git.k8s.io/enhancements/keps/sig-node/585-runtime-class",
    "name": "runtimeClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod will be dispatched by specified scheduler. If not specified, the pod will be dispatched by default scheduler.",
    "name": "schedulerName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#topologyspreadconstraint-v1-core",
    "name": "topologySpreadConstraints",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsscheduling)</sup></sup>

Backup Pod custom scheduling and affinity configuration.

{{< crd-properties >}}
[
  {
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core",
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
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podantiaffinity-v1-core",
    "name": "podAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "PreemptionPolicy is the Policy for preempting pods with lower priority. One of Never, PreemptLowerPriority. Defaults to PreemptLowerPriority if unset.",
    "name": "preemptionPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.",
    "name": "priorityClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "RuntimeClassName refers to a RuntimeClass object in the node.k8s.io group, which should be used to run this pod.  If no RuntimeClass resource matches the named class, the pod will not be run. If unset or empty, the \"legacy\" RuntimeClass will be used, which is an implicit class with an empty definition that uses the default runtime handler. More info: https://git.k8s.io/enhancements/keps/sig-node/585-runtime-class",
    "name": "runtimeClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod will be dispatched by specified scheduler. If not specified, the pod will be dispatched by default scheduler.",
    "name": "schedulerName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.startupProbe
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.updateStrategy
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpods)</sup></sup>

This section indicates the strategy that the SGCluster controller will use to perform updates.

It includes any additional parameters necessary to perform the update for the indicated strategy.

When a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of
 the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the
 primary is demoted instead of being restarted.

The Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters
 `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is
 decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,
 see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover
 to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is
 performed.


{{< crd-properties >}}
[
  {
    "default": "InPlace",
    "description": "Indicates the method of the update strategy. Default is `InPlace`.\n\n* `InPlace`: update will be performed on the existing instances.\n* `ReducedImpact`: before an update is performed on the existing instances a new instance if created to reduce the impact on read-only replicas.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "default": "PT5M",
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS` indicating the delay that has to pass between the restart of a\n Postgres instance performed by the SGCluster controller and the next one. Default is `PT5M`.",
    "name": "restartDelay",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsupdatestrategyscheduleindex",
    "description": "Indicates a list of windows of time where an update can be performed.",
    "name": "schedule",
    "required": false,
    "type": "[]object"
  },
  {
    "default": "OnlyDbOps",
    "description": "Indicates the type of the update strategy. Default is `OnlyDbOps`.\n\n* `Always`: update will be performed as soon as possible.\n* `Schedule`: update will be performed as specified in the schedule section where you can configure windows of time where the update can be performed.\n* `OnlyDbOps`: update will be performed only when an SGDbOps of type restart, securityUpgrade or minorVersionUpgrade targeting the SGCluster is started.\n* `Never`: update will never be performed (even if annotation `stackgres.io/rollout` is present with a different value) unless the Pods are deleted manually.\n\nAlternatively to this configuration you may specify the following annotations:\n\n* `stackgres.io/rollout=always`: Same behavior as `Always`.\n* `stackgres.io/rollout=schedule`: Same behavior as `Schedule`.\n* `stackgres.io/rollout=never`: Same behavior as `Never` (even if type has a different value).\n\nUnless `Never` (or the annotation `stackgres.io/rollout=never`) is specified an `SGDbOps` of type restart, securityUpgrade or minorVersionUpgrade targeting the SGCluster\n will trigger an update of the Pods.\n\nThe update of the Pods will be performed only if Pods are pending any change. Changes in the configuration may not require restarting the Pod and, in such cases, only\n the Postgres instance will be restarted.",
    "name": "type",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.updateStrategy.schedule[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsupdatestrategy)</sup></sup>

Indicates a window of time where an update can be performed.

{{< crd-properties >}}
[
  {
    "description": "A UNIX cron expression indicating the start of the window of time where the update can be performed.",
    "name": "cron",
    "required": false,
    "type": "string"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that, together with the cron expression, indicates the end of the window of time where the update can be performed.",
    "name": "duration",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.coordinator.autoscaling
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinator)</sup></sup>

This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.

Vertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.
Vertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalinghorizontal",
    "description": "Section to configure horizontal Pod autoscaling aspects.",
    "name": "horizontal",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingmaxallowed",
    "description": "Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "maxAllowed",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingminallowed",
    "description": "Defines the lower bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "minAllowed",
    "required": false,
    "type": "object"
  },
  {
    "default": "vertical",
    "description": "Allows enabling or disabling any of horizontal and vertical Pod autoscaling.\n\nPossible values are:\n* `vertical`: only vertical Pod autoscaling will be enabled (default)\n* `none`: all autoscaling will be disabled",
    "enum": [
      "vertical",
      "none"
    ],
    "name": "mode",
    "required": false,
    "type": "enum"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingvertical",
    "description": "Section to configure vertical Pod autoscaling aspects.",
    "name": "vertical",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.horizontal
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscaling)</sup></sup>

Section to configure horizontal Pod autoscaling aspects.


{{< crd-properties >}}
[
  {
    "default": 300,
    "description": "The period in seconds before the downscale of replica instances can be triggered.",
    "name": "cooldownPeriod",
    "required": false,
    "type": "integer"
  },
  {
    "default": 30,
    "description": "The interval in seconds to check if the scaleup or scaledown have to be triggered.",
    "name": "pollingInterval",
    "required": false,
    "type": "integer"
  },
  {
    "default": "AverageValue",
    "description": "The metric type for connections used metric. See https://keda.sh/docs/latest/concepts/scaling-deployments/#triggers",
    "name": "replicasConnectionsUsageMetricType",
    "required": false,
    "type": "string"
  },
  {
    "default": "0.8",
    "description": "The target value for replicas connections used in order to trigger the upscale of replica instances.",
    "name": "replicasConnectionsUsageTarget",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.maxAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscaling)</sup></sup>

Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingmaxallowedenvoy",
    "description": "Defines the higher bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingmaxallowedpatroni",
    "description": "Defines the higher bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingmaxallowedpgbouncer",
    "description": "Defines the higher bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.maxAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of envoy container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the envoy container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the envoy container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.maxAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of patroni container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the patroni container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the patroni container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.maxAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of pgbouncer container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the pgbouncer container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the pgbouncer container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.minAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscaling)</sup></sup>

Defines the lower bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingminallowedenvoy",
    "description": "Defines the lower bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingminallowedpatroni",
    "description": "Defines the lower bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorautoscalingminallowedpgbouncer",
    "description": "Defines the lower bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.minAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of envoy container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the envoy container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the envoy container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.minAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of patroni container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the patroni container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the patroni container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.minAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of pgbouncer container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the pgbouncer container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the pgbouncer container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.autoscaling.vertical
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorautoscaling)</sup></sup>

Section to configure vertical Pod autoscaling aspects.


{{< crd-properties >}}
[
  {
    "description": "Recommender responsible for generating recommendation for vertical Pod autoscaling. If not specified the default one will be used.",
    "name": "recommender",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.coordinator.configurations
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinator)</sup></sup>

Coordinator custom configurations.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationspatroni",
    "description": "Allows specifying Patroni configuration that will extend the generated one\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationspooling",
    "description": "",
    "name": "pooling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationspostgres",
    "description": "Section for postgres configuration.",
    "name": "postgres",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationspostgresexporter",
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.",
    "name": "postgresExporter",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig) used for this cluster. Each pod contains a sidecar with a connection pooler (currently: [PgBouncer](https://www.pgbouncer.org/)). The connection pooler is implemented as a sidecar.\n\nIf not set, a default configuration will be used. Disabling connection pooling altogether is possible if the disableConnectionPooling property of the pods object is set to true.\n\nIf sharding type is `shardingsphere` then this field is ignored.\n\n**Changing this field may require a restart.**",
    "name": "sgPoolingConfig",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig) used for the cluster. It must exist. When not set, a default Postgres config, for the major version selected, is used.\n\nIf sharding type is `shardingsphere` then this field is ignored.\n\n**Changing this field may require a restart.**",
    "name": "sgPostgresConfig",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingsphere",
    "description": "Allows specifying Sharding Sphere Proxy configuration that will extend the generated one.\n\nThis section is required when sharding type is `shardingsphere` otherwise is ignored.",
    "name": "shardingSphere",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurations)</sup></sup>

Allows specifying Patroni configuration that will extend the generated one

If sharding type is `shardingsphere` then this section is ignored.


{{< crd-properties >}}
[
  {
    "description": "Configure Patroni to use FQDN instead of the Pod's assigned IPs to connect between Patroni and Postgres instances.",
    "name": "connectUsingFqdn",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Allows specifying Patroni dynamic configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/dynamic_configuration.html\n\nThe following configuration fields will be ignored:\n\n* synchronous_mode\n* synchronous_mode_strict\n* postgresql\n* standby_cluster\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "dynamicConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Allows specifying Patroni configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/yaml_configuration.html\n\nThe following configuration fields will be ignored:\n\n* name\n* namespace\n* log\n* bootstrap\n* citus\n* postgresql # with the exception of postgresql.callbacks, postgresql.pre_promote, postgresql.before_stop and postgresql.pg_ctl_timeout\n* restapi\n* ctl\n* watchdog\n* tags\n\nIf sharding type is `shardingsphere` then this section is ignored.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "initialConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Annotations that must be set on the generated SGClusters before Patroni is started. See `SGCluster.spec.configurations.patroni.startGateAnnotations`.\n\nIf sharding type is `citus` the operator also adds the annotation required to start the query routers only once they have been registered by the coordinator.",
    "name": "startGateAnnotations",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.pooling
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurations)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationspoolingpgbouncer",
    "description": "",
    "name": "pgBouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.pooling.pgBouncer
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationspooling)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationspoolingpgbouncerpgbouncerini",
    "description": "The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).\n\n**Changing this field may require a restart.**",
    "name": "pgbouncer.ini",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.pooling.pgBouncer.pgbouncer.ini
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationspoolingpgbouncer)</sup></sup>

The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "description": "The `pgbouncer.ini` (Section [databases]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-databases) for more information about supported parameters.",
    "name": "databases",
    "requireRestart": true,
    "required": false,
    "type": "map[string]map[string]string"
  },
  {
    "description": "The `pgbouncer.ini` (Section [pgbouncer]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#generic-settings) for more information about supported parameters",
    "name": "pgbouncer",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "The `pgbouncer.ini` (Section [users]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-users) for more information about supported parameters.",
    "name": "users",
    "requireRestart": true,
    "required": false,
    "type": "map[string]map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.postgres
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurations)</sup></sup>

Section for postgres configuration.

{{< crd-properties >}}
[
  {
    "description": "The `postgresql.conf` parameters that overwrite those specified in the referenced [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig).\n\n**Changing this field may require a restart.**",
    "name": "postgresql.conf",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.postgresExporter
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurations)</sup></sup>

Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.

{{< crd-properties >}}
[
  {
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) queries.yaml configuration.\n\nThe queries defined here will overwrite those created by the operator.\n\n**WARNING**: Changing this may beak some of the functionality that depend on the query overwritten.",
    "name": "queries",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurations)</sup></sup>

Allows specifying Sharding Sphere Proxy configuration that will extend the generated one.

This section is required when sharding type is `shardingsphere` otherwise is ignored.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingspheremode",
    "description": "Allows configuring the Sharding Sphere Proxy mode.",
    "name": "mode",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthority",
    "description": "Allows configuring the Sharding Sphere Proxy authority.",
    "name": "authority",
    "required": false,
    "type": "object"
  },
  {
    "description": "Properties that will be set in the ShardingSphere Proxy configuration.\n\nSome properties will be overwritten with the configuration generated by the operator. In particular:\n\n* `server-lists`",
    "name": "properties",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingsphereserviceaccount",
    "description": "Section to configure ServiceAccount used by ShardingSphere operator.\n\nYou may configure a global value under operator configuration section\n `SGConfig.spec.shardingSphere.serviceAccount`.",
    "name": "serviceAccount",
    "required": false,
    "type": "object"
  },
  {
    "description": "The version of the ShardingSphere Proxy. If not specified latest version available will be used.",
    "name": "version",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.mode
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingsphere)</sup></sup>

Allows configuring the Sharding Sphere Proxy mode.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingspheremoderepository",
    "description": "",
    "name": "repository",
    "required": true,
    "type": "object"
  },
  {
    "description": "Allows configuring the Sharding Sphere Proxy mode type. Options available are:\n\n* `Standalone`\n* `Cluster`\n\nWhen `Standalone` only 1 coordinator instance may be set.",
    "name": "type",
    "required": true,
    "type": "string"
  },
  {
    "description": "Properties that will be set in the ShardingSphere Proxy configuration.\n\nSome properties will be overwritten with the configuration generated by the operator. In particular:\n\n* `proxy-frontend-database-protocol-type`\n* `proxy-default-port`",
    "name": "properties",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.mode.repository
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingspheremode)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Allows configuring the Sharding Sphere Proxy repository type. Options available are:\n\n* `Memory`\n* `ZooKeeper`\n* `Etcd`\n\nWhen `mode.type` is `standalone` then `repository.type` must be memory.\nWhen `mode.type` is `cluster` then `repository.type` could be any of zooKeeper or etcd.",
    "name": "type",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingspheremoderepositoryetcd",
    "description": "Allows configuring Etcd repository for Sharding Sphere Proxy.",
    "name": "etcd",
    "required": false,
    "type": "object"
  },
  {
    "description": "Properties that will be set in the ShardingSphere Proxy configuration for the Repository.\n\nSome properties will be overwritten with the configuration generated by the operator. In particular:\n\n* `server-lists`",
    "name": "properties",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingspheremoderepositoryzookeeper",
    "description": "Allows configuring ZooKeeper repository for Sharding Sphere Proxy.",
    "name": "zooKeeper",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.mode.repository.etcd
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingspheremoderepository)</sup></sup>

Allows configuring Etcd repository for Sharding Sphere Proxy.

{{< crd-properties >}}
[
  {
    "description": "List of Etcd servers to connect to.",
    "name": "serverList",
    "required": true,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.mode.repository.zooKeeper
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingspheremoderepository)</sup></sup>

Allows configuring ZooKeeper repository for Sharding Sphere Proxy.

{{< crd-properties >}}
[
  {
    "description": "List of ZooKeeper servers to connect to.",
    "name": "serverList",
    "required": true,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.authority
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingsphere)</sup></sup>

Allows configuring the Sharding Sphere Proxy authority.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthorityprivilege",
    "description": "Allows configuring the Sharding Sphere Proxy authority privilege.",
    "name": "privilege",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthorityusersindex",
    "description": "Allows configuring extra users other than the superuser (by default superuser username is postgres).\n\nSee also https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-proxy/yaml-config/authority/#authentication-configuration",
    "name": "users",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.authority.privilege
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthority)</sup></sup>

Allows configuring the Sharding Sphere Proxy authority privilege.

{{< crd-properties >}}
[
  {
    "description": "Allows configuring the Sharding Sphere Proxy authority privilege type.\n\nSee also https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-proxy/yaml-config/authority/#authorization-configuration",
    "name": "type",
    "required": false,
    "type": "string"
  },
  {
    "description": "Allows configuring the mappings between users and databases.\n\nSee also https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-proxy/yaml-config/authority/#database_permitted",
    "name": "userDatabaseMappings",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.authority.users[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthority)</sup></sup>

Allows configuring an extra user other than the superuser (by default superuser username is postgres).


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthorityusersindexpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthorityusersindexuser",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "user",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.authority.users[index].password
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthorityusersindex)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.authority.users[index].user
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthorityusersindex)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.serviceAccount
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingsphere)</sup></sup>

Section to configure ServiceAccount used by ShardingSphere operator.

You may configure a global value under operator configuration section
 `SGConfig.spec.shardingSphere.serviceAccount`.


{{< crd-properties >}}
[
  {
    "description": "The name of the ServiceAccount used by ShardingSphere operator",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "The namespace of the ServiceAccount used by ShardingSphere operator",
    "name": "namespace",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.coordinator.managedSql
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinator)</sup></sup>

This section allows referencing SQL scripts that will be applied to the cluster live.

If sharding type is `shardingsphere` then this section will be applied to the first cluster worker.
 In this case the database postgres will also provide a foreign server called `shardingsphere` and
 the superuser user mappings that will allow running DistQL queries using command like the following:


```

SELECT * FROM dblink('shardingsphere', 'SHOW STORAGE UNITS')
  AS _(name text, type text, host text, port int, db text,
    connection_timeout_milliseconds int, idle_timeout_milliseconds int,
    max_lifetime_milliseconds int, max_pool_size int, min_pool_size int,
    read_only boolean, other_attributes text);

```


See https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-proxy/distsql


{{< crd-properties >}}
[
  {
    "description": "If true, when any entry of any `SGScript` fail will not prevent subsequent `SGScript` from being executed. By default is `false`.",
    "name": "continueOnSGScriptError",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatormanagedsqlscriptsindex",
    "description": "A list of script references that will be executed in sequence.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatormanagedsql)</sup></sup>

A script reference. Each version of each entry of the script referenced will be executed exactly once following the sequence defined
  in the referenced script and skipping any script entry that have already been executed.


{{< crd-properties >}}
[
  {
    "description": "The id is immutable and must be unique across all the `SGScript` entries. It is replaced by the operator and is used to identify the `SGScript` entry.",
    "name": "id",
    "required": false,
    "type": "integer"
  },
  {
    "description": "A reference to an `SGScript`",
    "name": "sgScript",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.coordinator.metadata
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinator)</sup></sup>

Metadata information from coordinator cluster created resources.

If sharding type is `shardingsphere` then this section is applied to the ComputeNode.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatormetadataannotations",
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatormetadatalabels",
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.",
    "name": "labels",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.metadata.annotations
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatormetadata)</sup></sup>

Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.

{{< crd-properties >}}
[
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "serviceAccount",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.metadata.labels
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatormetadata)</sup></sup>

Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.

{{< crd-properties >}}
[
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "serviceAccount",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.coordinator.replication
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinator)</sup></sup>

This section allows to configure the global Postgres replication mode.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.

If sharding type is `shardingsphere` then this section is ignored.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorreplicationinitialization",
    "description": "Allows specifying how the replicas are initialized.",
    "name": "initialization",
    "required": false,
    "type": "object"
  },
  {
    "default": "sync-all",
    "description": "The replication mode applied to the whole cluster.\nPossible values are:\n* `async` (default)\n* `sync`\n* `strict-sync`\n* `sync-all`\n* `strict-sync-all`\n\n**async**\n\nWhen in asynchronous mode the cluster is allowed to lose some committed transactions.\nWhen the primary server fails or becomes unavailable for any other reason a sufficiently healthy standby\nwill automatically be promoted to primary. Any transactions that have not been replicated to that standby\nremain in a \"forked timeline\" on the primary, and are effectively unrecoverable (the data is still there,\nbut recovering it requires a manual recovery effort by data recovery specialists).\n\n**sync**\n\nWhen in synchronous mode a standby will not be promoted unless it is certain that the standby contains all\ntransactions that may have returned a successful commit status to client (clients can change the behavior\nper transaction using PostgreSQL’s `synchronous_commit` setting. Transactions with `synchronous_commit`\nvalues of `off` and `local` may be lost on fail over, but will not be blocked by replication delays). This\nmeans that the system may be unavailable for writes even though some servers are available. System\nadministrators can still use manual failover commands to promote a standby even if it results in transaction\nloss.\n\nSynchronous mode does not guarantee multi node durability of commits under all circumstances. When no suitable\nstandby is available, primary server will still accept writes, but does not guarantee their replication. When\nthe primary fails in this mode no standby will be promoted. When the host that used to be the primary comes\nback it will get promoted automatically, unless system administrator performed a manual failover. This behavior\nmakes synchronous mode usable with 2 node clusters.\n\nWhen synchronous mode is used and a standby crashes, commits will block until the primary is switched to standalone\nmode. Manually shutting down or restarting a standby will not cause a commit service interruption. Standby will\nsignal the primary to release itself from synchronous standby duties before PostgreSQL shutdown is initiated.\n\n**strict-sync**\n\nWhen it is absolutely necessary to guarantee that each write is stored durably on at least two nodes, use the strict\nsynchronous mode. This mode prevents synchronous replication to be switched off on the primary when no synchronous\nstandby candidates are available. As a downside, the primary will not be available for writes (unless the Postgres\ntransaction explicitly turns off `synchronous_mode` parameter), blocking all client write requests until at least one\nsynchronous replica comes up.\n\n**Note**: Because of the way synchronous replication is implemented in PostgreSQL it is still possible to lose\ntransactions even when using strict synchronous mode. If the PostgreSQL backend is cancelled while waiting to acknowledge\nreplication (as a result of packet cancellation due to client timeout or backend failure) transaction changes become\nvisible for other backends. Such changes are not yet replicated and may be lost in case of standby promotion.\n\n**sync-all**\n\nThe same as `sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number\nof instances less one.\n\n**strict-sync-all**\n\nThe same as `strict-sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number\nof instances less one.",
    "name": "mode",
    "required": false,
    "type": "string"
  },
  {
    "description": "Number of synchronous standby instances. Must be less than the total number of instances. It is set to 1 by default.\n  Only setteable if mode is `sync` or `strict-sync`.",
    "minimum": 1,
    "name": "syncInstances",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.replication.initialization
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorreplication)</sup></sup>

Allows specifying how the replicas are initialized.


{{< crd-properties >}}
[
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how old an SGBackup have to be in order to be seleceted\n to initialize a replica.\n\nWhen `FromExistingBackup` mode is set this field restrict the selection of SGBackup to be used for recovery newer than the\n specified value. \n\nWhen `FromNewlyCreatedBackup` mode is set this field skip the creation SGBackup to be used for recovery if one newer than\n the specified value exists.",
    "name": "backupNewerThan",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorreplicationinitializationbackuprestoreperformance",
    "description": "Configuration that affects the backup network and disk usage performance during recovery.",
    "name": "backupRestorePerformance",
    "required": false,
    "type": "object"
  },
  {
    "default": "FromExistingBackup",
    "description": "Allows specifying how the replicas are initialized.\n\nPossible values are:\n\n* `FromPrimary`: When this mode is used replicas will be always created from the primary using `pg_basebackup`.\n* `FromReplica`: When this mode is used replicas will be created from another existing replica using\n `pg_basebackup`. Fallsback to `FromPrimary` if there's no replica or it fails.\n* `FromExistingBackup`: When this mode is used replicas will be created from an existing SGBackup. If `backupNewerThan` is set\n the SGBackup must be newer than its value. When this mode fails to restore an SGBackup it will try with a previous one (if exists).\n Fallsback to `FromReplica` if there's no backup left or it fails.\n* `FromNewlyCreatedBackup`: When this mode is used replicas will be created from a newly created SGBackup.\n Fallsback to `FromExistingBackup` if `backupNewerThan` is set and exists a recent backup newer than its value or it fails.",
    "name": "mode",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.replication.initialization.backupRestorePerformance
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorreplicationinitialization)</sup></sup>

Configuration that affects the backup network and disk usage performance during recovery.


{{< crd-properties >}}
[
  {
    "description": "Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of file to read and 10.",
    "minimum": 1,
    "name": "downloadConcurrency",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum disk read I/O when performing a backup. In bytes (per second).",
    "name": "maxDiskBandwidth",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum storage upload bandwidth used when storing a backup. In bytes (per second).",
    "name": "maxNetworkBandwidth",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.postgres
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

This section allows to configure Postgres features


{{< crd-properties >}}
[
  {
    "description": "Postgres version used on the cluster. It is either of:\n*  The string 'latest', which automatically sets the latest major.minor Postgres version.\n*  A major version, like '14' or '13', which sets that major version and the latest minor version.\n*  A specific major.minor version, like '14.4'.",
    "name": "version",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecpostgresextensionsindex",
    "description": "StackGres supports deploying extensions at runtime by simply adding an entry to this array. A deployed extension still\nrequires the creation in a database using the [`CREATE EXTENSION`](https://www.postgresql.org/docs/current/sql-createextension.html)\nstatement. After an extension is deployed correctly it will be present until removed and the cluster restarted.\n\nA cluster restart is required for:\n* Extensions that requires to add an entry to [`shared_preload_libraries`](https://postgresqlco.nf/en/doc/param/shared_preload_libraries/) configuration parameter.\n* Upgrading extensions that overwrite any file that is not the extension's control file or extension's script file.\n* Removing extensions. Until the cluster is not restarted a removed extension will still be available.\n* Install of extensions that require extra mount. After installed the cluster will require to be restarted.",
    "name": "extensions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Postgres flavor used on the cluster. It is either of:\n*  `babelfish` will use the [Babelfish for Postgres](https://babelfish-for-postgresql.github.io/babelfish-for-postgresql/).\n\nIf not specified then the vanilla Postgres will be used for the cluster.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "flavor",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecpostgresssl",
    "default": {
      "enabled": true
    },
    "description": "This section allows to use SSL when connecting to Postgres",
    "name": "ssl",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.postgres.extensions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgres)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The name of the extension to deploy.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "The id of the publisher of the extension to deploy. If not specified `com.ongres` will be used by default.",
    "name": "publisher",
    "required": false,
    "type": "string"
  },
  {
    "description": "The repository base URL from where to obtain the extension to deploy.\n\n**This section is filled by the operator.**",
    "name": "repository",
    "required": false,
    "type": "string"
  },
  {
    "description": "The version of the extension to deploy. If not specified version of `stable` channel will be used by default.",
    "name": "version",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.postgres.ssl
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgres)</sup></sup>

This section allows to use SSL when connecting to Postgres

**Example:**


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
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



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecpostgressslcertificatesecretkeyselector",
    "description": "Secret key selector for the certificate or certificate chain used for SSL connections.",
    "name": "certificateSecretKeySelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies the duration of the auto-generated certificate.\n\nIf not specified the default duration will be of 13 months.\n\nThe certificate will be re-generated 1 day or, if more, 1/12th of its duration before it expires.",
    "name": "duration",
    "required": false,
    "type": "string"
  },
  {
    "default": true,
    "description": "Allows enabling SSL for connections to Postgres. By default is `true`.\n\nIf `true` certificate and private key will be auto-generated unless fields `certificateSecretKeySelector` and `privateKeySecretKeySelector` are specified.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecpostgressslprivatekeysecretkeyselector",
    "description": "Secret key selector for the private key used for SSL connections.",
    "name": "privateKeySecretKeySelector",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgres.ssl.certificateSecretKeySelector
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresssl)</sup></sup>

Secret key selector for the certificate or certificate chain used for SSL connections.


{{< crd-properties >}}
[
  {
    "description": "The key of Secret that contains the certificate or certificate chain for SSL connections",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "The name of Secret that contains the certificate or certificate chain for SSL connections",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgres.ssl.privateKeySecretKeySelector
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresssl)</sup></sup>

Secret key selector for the private key used for SSL connections.


{{< crd-properties >}}
[
  {
    "description": "The key of Secret that contains the private key for SSL connections",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "The name of Secret that contains the private key for SSL connections",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.configurations
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

Sharded cluster custom configurations.

**Example:**


```
 yaml
apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  configurations:
    backups:
    - sgObjectStorage: 'backupconf'

```



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationsbackupsindex",
    "description": "List of sharded backups configurations for this SGShardedCluster",
    "name": "backups",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationsbinding",
    "description": "This section allows specifying the properties of [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).\n If not specified, then some default will be used.\n\nFor more information see https://servicebinding.io/spec/core/1.0.0/",
    "name": "binding",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscitus",
    "description": "Configuration specific to the citus sharding technology. It is ignored for other sharding types.",
    "name": "citus",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentials",
    "description": "Allows specifying custom credentials for Postgres users and Patroni REST API\n\n**Changing this field may require a restart.**",
    "name": "credentials",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationsobservability",
    "description": "Allows specifying Observability configuration (related to logs, metrics and traces)",
    "name": "observability",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationspostgresexporter",
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.",
    "name": "postgresExporter",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.configurations.backups[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurations)</sup></sup>

Sharded backup configuration for this SGShardedCluster


{{< crd-properties >}}
[
  {
    "description": "Name of the [SGObjectStorage](https://stackgres.io/doc/latest/reference/crd/sgobjectstorage) to use for the cluster. It defines the location in which the backups will be stored.",
    "name": "sgObjectStorage",
    "required": true,
    "type": "string"
  },
  {
    "description": "Specifies the backup compression algorithm. Possible options are: lz4, lzma, zstd, brotli. The default method is `lz4`. LZ4 is the fastest method, but compression ratio is the worst. LZMA is way slower, but it compresses backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio, being about 3 times better than LZ4.",
    "enum": [
      "lz4",
      "lzma",
      "zstd",
      "brotli"
    ],
    "name": "compression",
    "required": false,
    "type": "enum"
  },
  {
    "description": "Continuous Archiving backups are composed of periodic *base backups* and all the WAL segments produced in between those base backups for the coordinator and each worker. This parameter specifies at what time and with what frequency to start performing a new base backup.\n\nUse cron syntax (`m h dom mon dow`) for this parameter, i.e., 5 values separated by spaces:\n*  `m`: minute, 0 to 59.\n*  `h`: hour, 0 to 23.\n*  `dom`: day of month, 1 to 31 (recommended not to set it higher than 28).\n*  `mon`: month, 1 to 12.\n*  `dow`: day of week, 0 to 7 (0 and 7 both represent Sunday).\n\nAlso ranges of values (`start-end`), the symbol `*` (meaning `first-last`) or even `*/N`, where `N` is a number, meaning \"\"every `N`, may be used. All times are UTC. It is recommended to avoid 00:00 as base backup time, to avoid overlapping with any other external operations happening at this time.\n\nIf not set, full backups are never performed automatically.",
    "name": "cronSchedule",
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified SGBackup will create a backup forcing a fast start (by setting parameter `fast` to `true` when calling `pg_backup_start`) that will reduce the time the backups may take at the expense of more IO usage.\n\nSee also https://www.postgresql.org/docs/current/continuous-archiving.html#BACKUP-LOWLEVEL-BASE-BACKUP",
    "name": "fastVolumeSnapshot",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The maximum number of retries the backup operation is allowed to do after a failure.\n\nA value of `0` (zero) means no retries are made. Defaults to: `3`.",
    "name": "maxRetries",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The paths where the backups of coordinator and workers are stored. If not set this field is filled up by the operator.\n\nWhen provided will indicate where the backups and WAL files will be stored.\n\nThe first path indicate the coordinator path and the other paths indicate the workers paths\n\n\u003e **WARNING**: Most users should leave this field empty since having it manually set could be dangerous. If the value is repeated due to re-creating an SGShardedCluster or\n re-using the same value in another SGShardedCluster and you may get a mixed WAL history with unrecoverable backups.",
    "name": "paths",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationsbackupsindexperformance",
    "description": "Configuration that affects the backup network and disk usage performance.",
    "name": "performance",
    "required": false,
    "type": "object"
  },
  {
    "description": "The paths where the backups of the query routers are stored. If not set this field is filled up by the operator.\n\nWhen provided will indicate where the backups and WAL files will be stored.\n\n\u003e **WARNING**: Most users should leave this field empty since having it manually set could be dangerous. If the value is repeated due to re-creating an SGShardedCluster or\n re-using the same value in another SGShardedCluster and you may get a mixed WAL history with unrecoverable backups.",
    "name": "queryRouterPaths",
    "required": false,
    "type": "[]string"
  },
  {
    "default": 300,
    "description": "Allows setting a timeout for the reconciliation process that take place after the backup.\n\nIf not set defaults to 300 (5 minutes). If set to 0 it will disable timeout.\n\nFailure of reconciliation will not make the backup fail and will be re-tried the next time a SGBackup\n or shecduled backup Job take place.",
    "name": "reconciliationTimeout",
    "required": false,
    "type": "integer"
  },
  {
    "description": "If specified, WAL created after any unmanaged lifecycle backups will be retained.",
    "name": "retainWalsForUnmanagedLifecycle",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When an automatic retention policy is defined to delete old base backups, this parameter specifies the number of base backups to keep, in a sliding window.\n\nConsequently, the time range covered by backups is `periodicity*retention`, where `periodicity` is the separation between backups as specified by the `cronSchedule` property.\n\nDefault is 5.",
    "minimum": 1,
    "name": "retention",
    "required": false,
    "type": "integer"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies the initial delay used by the exponential backoff applied when a command run by the backup script is retried and in general by unbounded retries and in some cases without exponential backoff.\n\nDefaults to: `PT1S`.",
    "name": "retryDelay",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum number of times a command run by the backup script is retried before failing.\n\nDefaults to: `10`.",
    "name": "retryLimit",
    "required": false,
    "type": "integer"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies the maximum delay the exponential backoff is allowed to reach when a command run by the backup script is retried.\n\nDefaults to: `PT1M`.",
    "name": "retryMaxDelay",
    "required": false,
    "type": "string"
  },
  {
    "description": "Allows setting a timeout for the backup creation.\n\nIf not set it will be disabled and the backup operation will continue until the backup completes or fail. If set to 0 is the same as not being set.\n\nMake sure to set a reasonable high value in order to allow for any unexpected delays during backup creation (network low bandwidth, disk low throughput and so forth).",
    "name": "timeout",
    "required": false,
    "type": "integer"
  },
  {
    "description": "If specified SGBackup will use VolumeSnapshot to create backups.\n\nThis functionality still require to store WAL files in an SGObjectStorage but could result in much faster backups and restore of those backups.\n\nSee also https://kubernetes.io/docs/concepts/storage/volume-snapshots/",
    "name": "useVolumeSnapshot",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The name of the VolumeSnapshotClass to use to create the VolumeSnapshot for backups.\n\nSee also https://kubernetes.io/docs/concepts/storage/volume-snapshots/",
    "name": "volumeSnapshotClass",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.backups[index].performance
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationsbackupsindex)</sup></sup>

Configuration that affects the backup network and disk usage performance.


{{< crd-properties >}}
[
  {
    "description": "Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of files to read and 10.",
    "minimum": 1,
    "name": "downloadConcurrency",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum disk read I/O when performing a backup. In bytes (per second).",
    "name": "maxDiskBandwidth",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum storage upload bandwidth used when storing a backup. In bytes (per second).",
    "name": "maxNetworkBandwidth",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Backup storage may use several concurrent streams to store the data. This parameter configures the number of parallel streams to use. By default, it's set to 16.",
    "minimum": 1,
    "name": "uploadConcurrency",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Backup storage may use several concurrent streams to store the data. This parameter configures the number of parallel streams to use to reading from disk. By default, it's set to 1.",
    "minimum": 1,
    "name": "uploadDiskConcurrency",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.configurations.binding
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurations)</sup></sup>

This section allows specifying the properties of [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).
 If not specified, then some default will be used.

For more information see https://servicebinding.io/spec/core/1.0.0/


{{< crd-properties >}}
[
  {
    "description": "Allows specifying the database name. If not specified, then the default value is `postgres`",
    "name": "database",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationsbindingpassword",
    "description": "Allows referencing a Secret that contains the user's password. If not specified, then the superuser password will be used.",
    "name": "password",
    "required": false,
    "type": "object"
  },
  {
    "description": "It's the reference of custom provider name. If not specified, then the default value will be `stackgres`",
    "name": "provider",
    "required": false,
    "type": "string"
  },
  {
    "description": "Allows specifying the username. If not specified, then the superuser username will be used.",
    "name": "username",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.binding.password
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationsbinding)</sup></sup>

Allows referencing a Secret that contains the user's password. If not specified, then the superuser password will be used.

{{< crd-properties >}}
[
  {
    "description": "The key of the Secret",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "The name of the Secret",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.configurations.citus
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurations)</sup></sup>

Configuration specific to the citus sharding technology. It is ignored for other sharding types.


{{< crd-properties >}}
[
  {
    "default": false,
    "description": "When `true` the coordinator replicates the reference tables to the active nodes that miss any of them (like a query router added after the reference tables were created) using `replicate_reference_tables('block_writes')`, at the interval set by `updateNodeInterval`. The writes to every reference table are blocked while they are copied.\n\nWhen `false` (the default) the reference tables are not replicated to a node added after they were created until a reference table is created or `SELECT replicate_reference_tables()` is executed on the coordinator primary. In the meantime the queries of that node that use a reference table are executed on the nodes that hold it.",
    "name": "autoReplicateReferenceTables",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When `true` (the default) Citus connects to each node through the connection pooler ([PgBouncer](https://www.pgbouncer.org/)) running in its Pod instead of connecting directly to Postgres: the coordinator maintains `pg_dist_poolinfo` on the coordinator and on every worker and query router so that the port `6432` of PgBouncer (or the Envoy entry port `7432` when Envoy is enabled) is used instead of the Postgres port registered in `pg_dist_node`. Only the port is set, so the host keeps following the one that Patroni updates in `pg_dist_node` after a failover. Citus ignores `pg_dist_poolinfo` for the connections that can not go through a pooler (like the ones of the shard rebalancer).\n\nThe nodes whose connection pooling is disabled (`.spec.coordinator.pods.disableConnectionPooling`, `.spec.workers.pods.disableConnectionPooling` and `.spec.workers.overrides[].pods.disableConnectionPooling`) have no PgBouncer and are not added to `pg_dist_poolinfo`, so Citus connects directly to their Postgres.\n\nWhen `false` the coordinator removes from `pg_dist_poolinfo` the entries it created.",
    "name": "connectToPooler",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "When `true` the coordinator removes from `pg_dist_node` the nodes of the groups that are no longer part of the sharded cluster (after decreasing `.spec.workers.clusters` or `.spec.coordinator.queryRouterClusters`), as long as they hold no shard of a distributed table and can be reached.\n\nPatroni only maintains the Citus groups that exist, so the nodes of a removed group would otherwise stay in `pg_dist_node` and, once they can not be reached, make Citus reject any node addition and distributed DDL.\n\nRegardless of this setting, the SGCluster of a removed worker or query router is kept running while its group is registered in `pg_dist_node` (Citus can only remove a primary node that can be reached) and is scaled down to 0 instances once its group has been removed from `pg_dist_node`.",
    "name": "enableNodeAutoRemoval",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The interval at which the coordinator updates the nodes registered in `pg_dist_node`: it registers the query routers (with `shouldhaveshards` set to `false`, before their Patroni is started), fixes the `shouldhaveshards` flag of the query routers, activates the registered query routers, replicates the reference tables to the active nodes that miss them if `autoReplicateReferenceTables` is `true` and, if `enableNodeAutoRemoval` is `true`, removes the nodes of the removed groups.\n\nAn ISO 8601 duration in the format `PnDTnHnMn.nS`, truncated to seconds if lower than a minute, to minutes if lower than an hour and to hours otherwise (up to 23 hours). An interval that does not divide its unit restarts at the beginning of each minute, hour or day.\n\nDefaults to: `PT10S`.",
    "name": "updateNodeInterval",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.configurations.credentials
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurations)</sup></sup>

Allows specifying custom credentials for Postgres users and Patroni REST API

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialspatroni",
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials for patroni REST API.\n\nWhen the referenced Secret, or its content, changes, the operator applies the new password to Patroni at the next reconciliation cycle (usually within a minute),\n  without restarting it.",
    "name": "patroni",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusers",
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the users.\n\nWhen the referenced Secrets, or their content, change, the operator applies the changes at the next reconciliation cycle (usually within a minute),\n  without any manual step:\n\n* The new passwords are set to the database users.\n* When the username of a user changes, the user with the new username is created. The user with the previous username is not removed.\n* Patroni uses the new credentials of the superuser and of the replication user, without restarting it, once they are set to the database users.\n* The Postgres exporter uses the new username of the monitor user, without restarting the Pods.\n* PgBouncer uses the new username of the authenticator user to authenticate the users that connect through it, without restarting the Pods.",
    "name": "users",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentials)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials for patroni REST API.

When the referenced Secret, or its content, changes, the operator applies the new password to Patroni at the next reconciliation cycle (usually within a minute),
  without restarting it.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialspatronirestapipassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password for the patroni REST API.",
    "name": "restApiPassword",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.patroni.restApiPassword
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialspatroni)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password for the patroni REST API.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "requireRestart": true,
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentials)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the users.

When the referenced Secrets, or their content, change, the operator applies the changes at the next reconciliation cycle (usually within a minute),
  without any manual step:

* The new passwords are set to the database users.
* When the username of a user changes, the user with the new username is created. The user with the previous username is not removed.
* Patroni uses the new credentials of the superuser and of the replication user, without restarting it, once they are set to the database users.
* The Postgres exporter uses the new username of the monitor user, without restarting the Pods.
* PgBouncer uses the new username of the authenticator user to authenticate the users that connect through it, without restarting the Pods.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersauthenticator",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.",
    "name": "authenticator",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersreplication",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.",
    "name": "replication",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsuserssuperuser",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).",
    "name": "superuser",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.authenticator
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersauthenticatorpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersauthenticatorusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.authenticator.password
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "requireRestart": true,
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.authenticator.username
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "requireRestart": true,
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.replication
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersreplicationpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersreplicationusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.replication.password
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "requireRestart": true,
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.replication.username
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "requireRestart": true,
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.superuser
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsuserssuperuserpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsuserssuperuserusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.superuser.password
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "requireRestart": true,
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.superuser.username
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "requireRestart": true,
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.configurations.observability
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurations)</sup></sup>

Allows specifying Observability configuration (related to logs, metrics and traces)

{{< crd-properties >}}
[
  {
    "default": false,
    "description": "If set to `true`, avoids creating the Prometheus exporter sidecar. Recommended when there's no intention to use internal monitoring.\n\n**Changing this field may require a restart.**",
    "name": "disableMetrics",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "If set to `true`, a PodMonitor is created for each Prometheus instance as specified in the SGConfig.spec.collector.prometheusOperator.monitors section.",
    "name": "prometheusAutobind",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Indicate the receiver in the configuration for the collector scraper (if not specified, will default to prometheus).",
    "name": "receiver",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.configurations.postgresExporter
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurations)</sup></sup>

Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.

{{< crd-properties >}}
[
  {
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) queries.yaml configuration.\n\nThe queries defined here will overwrite those created by the operator.\n\n**WARNING**: Changing this may break some of the functionality that depend on the query overwritten.",
    "name": "queries",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.distributedLogs
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

StackGres features a functionality for all pods to send Postgres, Patroni and PgBouncer logs to a central (distributed) location, which is in turn another Postgres database. Logs can then be accessed via SQL interface or from the web UI. This section controls whether to enable this feature or not. If not enabled, logs are sent to the pod's standard output.

**Example:**


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  distributedLogs:
    sgDistributedLogs: distributedlogs

```



{{< crd-properties >}}
[
  {
    "description": "Define a retention window with the syntax `\u003cinteger\u003e (minutes|hours|days|months)` in which log entries are kept.\n  Log entries will be removed when they get older more than the double of the specified retention window.\n\nWhen this field is changed the retention will be applied only to log entries that are newer than the end of\n  the retention window previously specified. If no retention window was previously specified it is considered\n  to be of 7 days. This means that if previous retention window is of `7 days` new retention configuration will\n  apply after UTC timestamp calculated with: `SELECT date_trunc('days', now() at time zone 'UTC') - INTERVAL '7 days'`.",
    "name": "retention",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the [SGDistributedLogs](https://stackgres.io/doc/latest/reference/crd/sgdistributedlogs/) to use for this cluster. It must exist.",
    "name": "sgDistributedLogs",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.initialData
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

Sharded cluster initialization data options. Sharded cluster may be initialized empty, or from a sharded backup restoration.

**This field can only be set on creation.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecinitialdatarestore",
    "description": "This section allows to restore a sharded cluster from an existing copy of the metadata and data.",
    "immutable": true,
    "name": "restore",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.initialData.restore
<sup><sup>[↩ Parent](#sgshardedclusterspecinitialdata)</sup></sup>

This section allows to restore a sharded cluster from an existing copy of the metadata and data.


{{< crd-properties >}}
[
  {
    "description": "The backup fetch process may fetch several streams in parallel. Parallel fetching is enabled when set to a value larger than one.\n\nIf not specified it will be interpreted as latest.",
    "immutable": true,
    "minimum": 1,
    "name": "downloadDiskConcurrency",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecinitialdatarestorefrombackup",
    "description": "From which sharded backup to restore and how the process is configured",
    "immutable": true,
    "name": "fromBackup",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.initialData.restore.fromBackup
<sup><sup>[↩ Parent](#sgshardedclusterspecinitialdatarestore)</sup></sup>

From which sharded backup to restore and how the process is configured

**Example:**


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  initialData:
    restore:
      fromBackup:
        name: stackgres-backup
      downloadDiskConcurrency: 1

```



{{< crd-properties >}}
[
  {
    "description": "When set to the name of an existing [SGShardedBackup](https://stackgres.io/doc/latest/reference/crd/sgshardedbackup), the sharded cluster is initialized by restoring the\n  backup data to it. If not set, the sharded cluster is initialized empty. The selected sharded backup must be in the same namespace.",
    "immutable": true,
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecinitialdatarestorefrombackuppointintimerecovery",
    "description": "It is possible to restore the database to its state at any time since your backup was taken using Point-in-Time Recovery (PITR) as long as another\n  backup newer than the PITR requested restoration date does not exist.\n\nPoint In Time Recovery (PITR). PITR allows restoring the database state to an arbitrary point of time in the past, as long as you specify a backup\n  older than the PITR requested restoration date and does not exist a backup newer than the same restoration date.\n\nSee also: https://www.postgresql.org/docs/current/continuous-archiving.html",
    "immutable": true,
    "name": "pointInTimeRecovery",
    "required": false,
    "type": "object"
  },
  {
    "description": "Specify the [recovery_target_inclusive](https://postgresqlco.nf/doc/en/param/recovery_target_timeline/) to stop recovery just after the specified\n  recovery target (true), or just before the recovery target (false). Applies when targetLsn, pointInTimeRecovery, or targetXid is specified. This\n  setting controls whether transactions having exactly the target WAL location (LSN), commit time, or transaction ID, respectively, will be included\n  in the recovery. Default is true.",
    "immutable": true,
    "name": "targetInclusive",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.initialData.restore.fromBackup.pointInTimeRecovery
<sup><sup>[↩ Parent](#sgshardedclusterspecinitialdatarestorefrombackup)</sup></sup>

It is possible to restore the database to its state at any time since your backup was taken using Point-in-Time Recovery (PITR) as long as another
  backup newer than the PITR requested restoration date does not exist.

Point In Time Recovery (PITR). PITR allows restoring the database state to an arbitrary point of time in the past, as long as you specify a backup
  older than the PITR requested restoration date and does not exist a backup newer than the same restoration date.

See also: https://www.postgresql.org/docs/current/continuous-archiving.html


{{< crd-properties >}}
[
  {
    "description": "An ISO 8601 date, that holds UTC date indicating at which point-in-time the database have to be restored.",
    "immutable": true,
    "name": "restoreToTimestamp",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.metadata
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

Metadata information from any cluster created resources.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecmetadataannotations",
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecmetadatalabels",
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.",
    "name": "labels",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.metadata.annotations
<sup><sup>[↩ Parent](#sgshardedclusterspecmetadata)</sup></sup>

Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.

**Example:**


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  metadata:
    annotations:
      clusterPods:
        customAnnotations: customAnnotationValue
      coordinatorPrimaryService:
        customAnnotations: customAnnotationValue
      coordinatorAnyService:
        customAnnotations: customAnnotationValue
      workersPrimariesService:
        customAnnotations: customAnnotationValue

```



{{< crd-properties >}}
[
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "coordinatorAnyService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "coordinatorPrimaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "queryRoutersPrimariesService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "serviceAccount",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "**Deprecated** use `workersPrimariesService` instead.\n\nCustom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "shardsPrimariesService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "workersPrimariesService",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.metadata.labels
<sup><sup>[↩ Parent](#sgshardedclusterspecmetadata)</sup></sup>

Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.

**Example:**


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
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
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "coordinatorAnyService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "coordinatorPrimaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "queryRoutersPrimariesService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "serviceAccount",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "**Deprecated** use `workersPrimariesService` instead.\n\nCustom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "shardsPrimariesService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "workersPrimariesService",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.nonProductionOptions
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "It is a best practice, on non-containerized environments, when running production workloads, to run each database server on a different server (virtual or physical), i.e., not to co-locate more than one database server per host.\n\nThe same best practice applies to databases on containers. By default, StackGres will not allow running more than one StackGres pod on a given Kubernetes node. Set this property to true to allow more than one StackGres pod per node.\n\nThis property default value may be changed depending on the value of field `.spec.profile`.\n\nThis property default value may be changed depending on the value of field `.spec.profile`.",
    "name": "disableClusterPodAntiAffinity",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "It is a best practice, on containerized environments, when running production workloads, to enforce container's resources requirements.\n\nBy default, StackGres will configure resource requirements for all the containers. Set this property to true to prevent StackGres from setting container's resources requirements (except for patroni container, see `disablePatroniResourceRequirements`).\n\nThis property default value may be changed depending on the value of field `.spec.profile`.",
    "name": "disableClusterResourceRequirements",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "It is a best practice, on containerized environments, when running production workloads, to enforce container's resources requirements.\n\nThe same best practice applies to databases on containers. By default, StackGres will configure resource requirements for patroni container. Set this property to true to prevent StackGres from setting patroni container's resources requirement.\n\nThis property default value may be changed depending on the value of field `.spec.profile`.",
    "name": "disablePatroniResourceRequirements",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allows achieving the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less cpu than it requires. It also allows setting [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.\n\nBy default, StackGres will configure cpu requirements to have the same limit and request for all the containers. Set this property to true to make StackGres set container's cpu requirements request equals to the limit (except for patroni container, see `enableSetPatroniCpuRequests`)\n  when `.spec.requests.containers.\u003ccontainer name\u003e.cpu` `.spec.requests.initContainers.\u003ccontainer name\u003e.cpu` is configured in the referenced `SGInstanceProfile`.",
    "name": "enableSetClusterCpuRequests",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allows achieving the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less memory than it requires.\n\nBy default, StackGres will configure memory requirements to have the same limit and request for all the containers. Set this property to true to make StackGres set container's memory requirements request equals to the limit (except for patroni container, see `enableSetPatroniMemoryRequests`)\n  when `.spec.requests.containers.\u003ccontainer name\u003e.memory` `.spec.requests.initContainers.\u003ccontainer name\u003e.memory` is configured in the referenced `SGInstanceProfile`.",
    "name": "enableSetClusterMemoryRequests",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allows achieving the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less cpu than it requires. It also allows setting [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.\n\nBy default, StackGres will configure cpu requirements to have the same limit and request for the patroni container. Set this property to true to make StackGres set patroni container's cpu requirements request equals to the limit\n  when `.spec.requests.cpu` is configured in the referenced `SGInstanceProfile`.",
    "name": "enableSetPatroniCpuRequests",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allows achieving the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less memory than it requires.\n\nBy default, StackGres will configure memory requirements to have the same limit and request for the patroni container. Set this property to true to make StackGres set patroni container's memory requirements request equals to the limit\n  when `.spec.requests.memory` is configured in the referenced `SGInstanceProfile`.",
    "name": "enableSetPatroniMemoryRequests",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "A list of StackGres feature gates to enable (not suitable for a production environment).\n\nAvailable feature gates are:\n* `babelfish-flavor`: Allows using the `babelfish` flavor.",
    "name": "enabledFeatureGates",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.postgresServices
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

Kubernetes [services](https://kubernetes.io/docs/concepts/services-networking/service/) created or managed by StackGres.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecpostgresservicescoordinator",
    "description": "Configuration for the coordinator services",
    "name": "coordinator",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecpostgresservicesshards",
    "description": "**Deprecated** use `workers` instead.\n\nConfiguration for the workers services",
    "name": "shards",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecpostgresservicesworkers",
    "description": "Configuration for the workers services",
    "name": "workers",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.postgresServices.coordinator
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservices)</sup></sup>

Configuration for the coordinator services


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorany",
    "description": "Configure the coordinator service to any instance of the coordinator with the same name as the SGShardedCluster plus the `-reads` suffix.\n\nIf the sharding type is `shardingsphere` then the name of the service will be the same name as the SGShardedCluster.\n\nIt provides a stable connection (regardless of node failures) to any Postgres server of the coordinator cluster. Servers are load-balanced via this service.\n\nSee also https://kubernetes.io/docs/concepts/services-networking/service/",
    "name": "any",
    "required": false,
    "type": "object"
  },
  {
    "description": "The list of custom ports that will be exposed by the coordinator services.\n\nThe names of custom ports will be prefixed with the string `custom-` so they do not\n  conflict with ports defined for the coordinator services.\n\nThe names of target ports will be prefixed with the string `custom-` so that the ports\n  that can be referenced in this section will be only those defined under\n  .spec.pods.customContainers[].ports sections where names are also prepended with the same\n  prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#serviceport-v1-core",
    "name": "customPorts",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorprimary",
    "description": "Configure the coordinator service to the primary of the coordinator with the name as the SGShardedCluster.\n\nIf the sharding type is `shardingsphere` then this service will be disabled.\n\nIt provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the coordinator cluster.\n\nSee also https://kubernetes.io/docs/concepts/services-networking/service/",
    "name": "primary",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorqueryrouters",
    "description": "Configure the query routers service to any primary in the query router wokrers with the name as the SGShardedCluster plus the `-routers` suffix.\n\nIt provides a stable connection to read-write Postgres servers of any query router worker SGCluster. Read-write servers are load-balanced via this service.\n\nSee also `.spec.coordinator.queryRouterClusters`\n\nSee also https://kubernetes.io/docs/concepts/services-networking/service/",
    "name": "queryRouters",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.coordinator.any
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinator)</sup></sup>

Configure the coordinator service to any instance of the coordinator with the same name as the SGShardedCluster plus the `-reads` suffix.

If the sharding type is `shardingsphere` then the name of the service will be the same name as the SGShardedCluster.

It provides a stable connection (regardless of node failures) to any Postgres server of the coordinator cluster. Servers are load-balanced via this service.

See also https://kubernetes.io/docs/concepts/services-networking/service/


{{< crd-properties >}}
[
  {
    "description": "allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is \"true\". It may be set to \"false\" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.",
    "name": "allocateLoadBalancerNodePorts",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Specify if the service should be created or not.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#allocateloadbalancernodeports-v1-core",
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
    "anchor": "sgshardedclusterspecpostgresservicescoordinatoranynodeports",
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
    "anchor": "sgshardedclusterspecpostgresservicescoordinatoranysessionaffinityconfig",
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
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

##### SGShardedCluster.spec.postgresServices.coordinator.any.nodePorts
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatorany)</sup></sup>

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

##### SGShardedCluster.spec.postgresServices.coordinator.any.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatorany)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecpostgresservicescoordinatoranysessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.coordinator.any.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatoranysessionaffinityconfig)</sup></sup>

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

##### SGShardedCluster.spec.postgresServices.coordinator.primary
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinator)</sup></sup>

Configure the coordinator service to the primary of the coordinator with the name as the SGShardedCluster.

If the sharding type is `shardingsphere` then this service will be disabled.

It provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the coordinator cluster.

See also https://kubernetes.io/docs/concepts/services-networking/service/


{{< crd-properties >}}
[
  {
    "description": "allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is \"true\". It may be set to \"false\" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.",
    "name": "allocateLoadBalancerNodePorts",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Specify if the service should be created or not.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#allocateloadbalancernodeports-v1-core",
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
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorprimarynodeports",
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
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorprimarysessionaffinityconfig",
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
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

##### SGShardedCluster.spec.postgresServices.coordinator.primary.nodePorts
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatorprimary)</sup></sup>

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

##### SGShardedCluster.spec.postgresServices.coordinator.primary.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatorprimary)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorprimarysessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.coordinator.primary.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatorprimarysessionaffinityconfig)</sup></sup>

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

##### SGShardedCluster.spec.postgresServices.coordinator.queryRouters
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinator)</sup></sup>

Configure the query routers service to any primary in the query router wokrers with the name as the SGShardedCluster plus the `-routers` suffix.

It provides a stable connection to read-write Postgres servers of any query router worker SGCluster. Read-write servers are load-balanced via this service.

See also `.spec.coordinator.queryRouterClusters`

See also https://kubernetes.io/docs/concepts/services-networking/service/


{{< crd-properties >}}
[
  {
    "description": "allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is \"true\". It may be set to \"false\" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.",
    "name": "allocateLoadBalancerNodePorts",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Specify if the service should be created or not.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#allocateloadbalancernodeports-v1-core",
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
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorqueryroutersnodeports",
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
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorqueryrouterssessionaffinityconfig",
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
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

##### SGShardedCluster.spec.postgresServices.coordinator.queryRouters.nodePorts
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatorqueryrouters)</sup></sup>

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

##### SGShardedCluster.spec.postgresServices.coordinator.queryRouters.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatorqueryrouters)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecpostgresservicescoordinatorqueryrouterssessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.coordinator.queryRouters.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicescoordinatorqueryrouterssessionaffinityconfig)</sup></sup>

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

#### SGShardedCluster.spec.postgresServices.shards
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservices)</sup></sup>

**Deprecated** use `workers` instead.

Configuration for the workers services


{{< crd-properties >}}
[
  {
    "description": "The list of custom ports that will be exposed by the workers services.\n\nThe names of custom ports will be prefixed with the string `custom-` so they do not\n  conflict with ports defined for the workers services.\n\nThe names of target ports will be prefixed with the string `custom-` so that the ports\n  that can be referenced in this section will be only those defined under\n  .spec.pods.customContainers[].ports sections where names are also prepended with the same\n  prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#serviceport-v1-core",
    "name": "customPorts",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecpostgresservicesshardsprimaries",
    "description": "Configure the workers service to any primary in the workers with the name as the SGShardedCluster plus the `-workers` suffix.\n\nIt provides a stable connection (regardless of primary failures or switchovers) to read-write Postgres servers of any worker SGCluster. Read-write servers are load-balanced via this service.\n\nSee also https://kubernetes.io/docs/concepts/services-networking/service/",
    "name": "primaries",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.shards.primaries
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesshards)</sup></sup>

Configure the workers service to any primary in the workers with the name as the SGShardedCluster plus the `-workers` suffix.

It provides a stable connection (regardless of primary failures or switchovers) to read-write Postgres servers of any worker SGCluster. Read-write servers are load-balanced via this service.

See also https://kubernetes.io/docs/concepts/services-networking/service/


{{< crd-properties >}}
[
  {
    "description": "allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is \"true\". It may be set to \"false\" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.",
    "name": "allocateLoadBalancerNodePorts",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Specify if the service should be created or not.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#allocateloadbalancernodeports-v1-core",
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
    "anchor": "sgshardedclusterspecpostgresservicesshardsprimariesnodeports",
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
    "anchor": "sgshardedclusterspecpostgresservicesshardsprimariessessionaffinityconfig",
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
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

##### SGShardedCluster.spec.postgresServices.shards.primaries.nodePorts
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesshardsprimaries)</sup></sup>

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

##### SGShardedCluster.spec.postgresServices.shards.primaries.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesshardsprimaries)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecpostgresservicesshardsprimariessessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.shards.primaries.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesshardsprimariessessionaffinityconfig)</sup></sup>

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

#### SGShardedCluster.spec.postgresServices.workers
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservices)</sup></sup>

Configuration for the workers services


{{< crd-properties >}}
[
  {
    "description": "The list of custom ports that will be exposed by the workers services.\n\nThe names of custom ports will be prefixed with the string `custom-` so they do not\n  conflict with ports defined for the workers services.\n\nThe names of target ports will be prefixed with the string `custom-` so that the ports\n  that can be referenced in this section will be only those defined under\n  .spec.pods.customContainers[].ports sections where names are also prepended with the same\n  prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#serviceport-v1-core",
    "name": "customPorts",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecpostgresservicesworkersprimaries",
    "description": "Configure the workers service to any primary in the workers with the name as the SGShardedCluster plus the `-workers` suffix.\n\nIt provides a stable connection (regardless of primary failures or switchovers) to read-write Postgres servers of any worker SGCluster. Read-write servers are load-balanced via this service.\n\nSee also https://kubernetes.io/docs/concepts/services-networking/service/",
    "name": "primaries",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.workers.primaries
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesworkers)</sup></sup>

Configure the workers service to any primary in the workers with the name as the SGShardedCluster plus the `-workers` suffix.

It provides a stable connection (regardless of primary failures or switchovers) to read-write Postgres servers of any worker SGCluster. Read-write servers are load-balanced via this service.

See also https://kubernetes.io/docs/concepts/services-networking/service/


{{< crd-properties >}}
[
  {
    "description": "allocateLoadBalancerNodePorts defines if NodePorts will be automatically allocated for services with type LoadBalancer.  Default is \"true\". It may be set to \"false\" if the cluster load-balancer does not rely on NodePorts.  If the caller requests specific NodePorts (by specifying a value), those requests will be respected, regardless of this field. This field may only be set for services with type LoadBalancer and will be cleared if the type is changed to any other type.",
    "name": "allocateLoadBalancerNodePorts",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Specify if the service should be created or not.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "externalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service.  These IPs are not managed by Kubernetes.  The user is responsible for ensuring that traffic arrives at a node with this IP.  A common example is external load-balancers that are not part of the Kubernetes system.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#allocateloadbalancernodeports-v1-core",
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
    "anchor": "sgshardedclusterspecpostgresservicesworkersprimariesnodeports",
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
    "anchor": "sgshardedclusterspecpostgresservicesworkersprimariessessionaffinityconfig",
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
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

##### SGShardedCluster.spec.postgresServices.workers.primaries.nodePorts
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesworkersprimaries)</sup></sup>

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

##### SGShardedCluster.spec.postgresServices.workers.primaries.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesworkersprimaries)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecpostgresservicesworkersprimariessessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.workers.primaries.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesworkersprimariessessionaffinityconfig)</sup></sup>

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

### SGShardedCluster.spec.replicateFrom
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

Make the sharded cluster a read-only standby replica allowing replication from another sharded cluster and acting as a relay.

Changing this section is allowed to fix issues or to change the replication source.

Removing this section convert the sharded cluster in a normal sharded cluster where the standby leader of each SGCluster is converted into a primary instance.

**Example:**

From SGShardedCluster instance:


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  replicateFrom:
    instance:
      sgShardedCluster: my-cluster

```

> **Note:** The above example allows replicating from another SGShardedCluster instance that in the same namespace and the same K8s cluster.
> 
> This option cannot be combined with external sharded cluster instance, storage and users.

From external sharded cluster instance:


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  replicateFrom:
    instance:
      external:
        hosts: ["${COORDINATOR_HOST_IP}","${SHARD_0_HOST_IP}","${SHARD_1_HOST_IP}"]
        ports: [5433,5433,5433]
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


> **Note:** Replace the ${COORDINATOR_HOST_IP}, ${SHARD_0_HOST_IP} and ${SHARD_1_HOST_IP} with the actual IPs of the external sharded cluster instance.

From Storage:


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  initialData:
    restore:
      fromBackup:
        name: backup-name
  replicateFrom:
    storage:
      paths: ["${PG_ORIGIN_COORDINATOR_BACKUP_PATH}","${PG_ORIGIN_SHARD_0_BACKUP_PATH}","${PG_ORIGIN_SHARD_1_BACKUP_PATH}"]
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
> Replace the ${PG_ORIGIN_COORDINATOR_BACKUP_PATH}, ${PG_ORIGIN_SHARD_0_BACKUP_PATH}, ${PG_ORIGIN_SHARD_1_BACKUP_PATH} with the actual paths in the object storage where the backups are stored.

From external instance and storage:


```yaml

apiVersion: stackgres.io/v1beta1
kind: SGShardedCluster
metadata:
  name: stackgres
spec:
  replicateFrom:
    instance:
      external:
        hosts: ["${COORDINATOR_HOST_IP}","${SHARD_0_HOST_IP}","${SHARD_1_HOST_IP}"]
        ports: [5433,5433,5433]
    storage:
      paths: ["${PG_ORIGIN_COORDINATOR_BACKUP_PATH}","${PG_ORIGIN_SHARD_0_BACKUP_PATH}","${PG_ORIGIN_SHARD_1_BACKUP_PATH}"]
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


> **Note:** Replace the ${COORDINATOR_HOST_IP}, ${SHARD_0_HOST_IP} and ${SHARD_1_HOST_IP} with the actual IPs of the external sharded cluster instance.
> 
> Replace the ${PG_ORIGIN_COORDINATOR_BACKUP_PATH}, ${PG_ORIGIN_SHARD_0_BACKUP_PATH}, ${PG_ORIGIN_SHARD_1_BACKUP_PATH} with the actual paths in the object storage where the backups are stored.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefrominstance",
    "description": "Configure replication from a PostgreSQL sharded cluster instance.",
    "name": "instance",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromstorage",
    "description": "Configure replication from an SGObjectStorage using WAL shipping.\n\nThe file structure of the object storage must follow the\n  [WAL-G](https://github.com/wal-g/wal-g) file structure.",
    "name": "storage",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromusers",
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the users.",
    "name": "users",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.replicateFrom.instance
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefrom)</sup></sup>

Configure replication from a PostgreSQL sharded cluster instance.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefrominstanceexternal",
    "description": "Configure replication from an external PostgreSQL instance.",
    "name": "external",
    "required": false,
    "type": "object"
  },
  {
    "description": "Configure replication from an SGShardedCluster.",
    "name": "sgShardedCluster",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.instance.external
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefrominstance)</sup></sup>

Configure replication from an external PostgreSQL instance.


{{< crd-properties >}}
[
  {
    "description": "The hosts of the PostgreSQL instances to replicate from.",
    "name": "hosts",
    "required": true,
    "type": "[]string"
  },
  {
    "description": "The ports of the PostgreSQL instances to replicate from.",
    "name": "ports",
    "required": true,
    "type": "[]integer"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefrominstanceexternalcustomrestoremethodsindex",
    "description": "Custom restore methods. See https://patroni.readthedocs.io/en/latest/replica_bootstrap.html#custom-replica-creation",
    "name": "customRestoreMethods",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.instance.external.customRestoreMethods[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefrominstanceexternal)</sup></sup>

Custom restore method. See https://patroni.readthedocs.io/en/latest/replica_bootstrap.html#custom-replica-creation

{{< crd-properties >}}
[
  {
    "description": "A command to be run to restore the leader replica. Mutually exclusive with `script` field.",
    "name": "command",
    "required": false,
    "type": "string"
  },
  {
    "description": "If set to `true`, will instruct Patroni to not clean PGDATA folder before calling restore.",
    "name": "keep_data",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If set to `true`, will instruct Patroni to not remove the existing `recovery.conf` file if it exists (PostgreSQL \u003c= 11).\n Similarly, in that case Patroni will not remove the existing `recovery.signal` or `standby.signal` if either exists,\n nor will it override the configured recovery settings (PostgreSQL \u003e= 12). This is useful when bootstrapping from a\n backup with tools like pgBackRest that generate the appropriate recovery configuration for you.",
    "name": "keep_existing_recovery_conf",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If set to `true` allows Patroni to call the replica creation method even if there is no running leader or replicas.\n In that case, an empty string will be passed in a connection string. This is useful for restoring the formerly\n running cluster from the binary backup.",
    "name": "no_leader",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If set to `true`, restricts passing parameters to custom command or script.",
    "name": "no_params",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "A list of optional parameters to pass to the command or script.",
    "name": "parameters",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "When set, Patroni will generate a `recovery.conf` before starting the newly bootstrapped instance (or set the recovery\n settings on Postgres configuration if running PostgreSQL \u003e= 12). Typically, such recovery configuration should contain\n at least one of the `recovery_target_*` parameters, together with the `recovery_target_action` set to promote.",
    "name": "recovery_conf",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "The path to a shell script to be run to restore the leader replica. Mutually exclusive with `command` field.",
    "name": "script",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.replicateFrom.storage
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefrom)</sup></sup>

Configure replication from an SGObjectStorage using WAL shipping.

The file structure of the object storage must follow the
  [WAL-G](https://github.com/wal-g/wal-g) file structure.


{{< crd-properties >}}
[
  {
    "description": "The paths in the SGObjectStorage to replicate from.",
    "name": "paths",
    "required": true,
    "type": "[]string"
  },
  {
    "description": "The SGObjectStorage name to replicate from.",
    "name": "sgObjectStorage",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromstorageperformance",
    "description": "Configuration that affects the backup network and disk usage performance during recovery.",
    "name": "performance",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.storage.performance
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromstorage)</sup></sup>

Configuration that affects the backup network and disk usage performance during recovery.


{{< crd-properties >}}
[
  {
    "description": "Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of files to read and 10.",
    "minimum": 1,
    "name": "downloadConcurrency",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum disk read I/O when performing a backup. In bytes (per second).",
    "name": "maxDiskBandwidth",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum storage upload bandwidth used when storing a backup. In bytes (per second).",
    "name": "maxNetworkBandwidth",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.replicateFrom.users
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefrom)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the users.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefromusersauthenticator",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.",
    "name": "authenticator",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromusersreplication",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.",
    "name": "replication",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromuserssuperuser",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).",
    "name": "superuser",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.authenticator
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefromusersauthenticatorpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromusersauthenticatorusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.authenticator.password
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.authenticator.username
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.replication
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefromusersreplicationpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromusersreplicationusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.replication.password
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.replication.username
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.superuser
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefromuserssuperuserpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromuserssuperuserusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.superuser.password
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the password of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.superuser.username
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) that contains the username of the user.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.replication
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

This section allows to configure the global Postgres replication mode.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicationinitialization",
    "description": "Allows specifying how the replicas are initialized.",
    "name": "initialization",
    "required": false,
    "type": "object"
  },
  {
    "default": "async",
    "description": "The replication mode applied to the whole cluster.\nPossible values are:\n* `async` (default)\n* `sync`\n* `strict-sync`\n* `sync-all`\n* `strict-sync-all`\n\n**async**\n\nWhen in asynchronous mode the cluster is allowed to lose some committed transactions.\n  When the primary server fails or becomes unavailable for any other reason a sufficiently healthy standby\n  will automatically be promoted to primary. Any transactions that have not been replicated to that standby\n  remain in a \"forked timeline\" on the primary, and are effectively unrecoverable (the data is still there,\n  but recovering it requires a manual recovery effort by data recovery specialists).\n\n**sync**\n\nWhen in synchronous mode a standby will not be promoted unless it is certain that the standby contains all\n  transactions that may have returned a successful commit status to client (clients can change the behavior\n  per transaction using PostgreSQL’s `synchronous_commit` setting. Transactions with `synchronous_commit`\n  values of `off` and `local` may be lost on fail over, but will not be blocked by replication delays). This\n  means that the system may be unavailable for writes even though some servers are available. System\n  administrators can still use manual failover commands to promote a standby even if it results in transaction\n  loss.\n\nSynchronous mode does not guarantee multi node durability of commits under all circumstances. When no suitable\n  standby is available, primary server will still accept writes, but does not guarantee their replication. When\n  the primary fails in this mode no standby will be promoted. When the host that used to be the primary comes\n  back it will get promoted automatically, unless system administrator performed a manual failover. This behavior\n  makes synchronous mode usable with 2 node clusters.\n\nWhen synchronous mode is used and a standby crashes, commits will block until the primary is switched to standalone\n  mode. Manually shutting down or restarting a standby will not cause a commit service interruption. Standby will\n  signal the primary to release itself from synchronous standby duties before PostgreSQL shutdown is initiated.\n\n**strict-sync**\n\nWhen it is absolutely necessary to guarantee that each write is stored durably on at least two nodes, use the strict\n  synchronous mode. This mode prevents synchronous replication to be switched off on the primary when no synchronous\n  standby candidates are available. As a downside, the primary will not be available for writes (unless the Postgres\n  transaction explicitly turns off `synchronous_mode` parameter), blocking all client write requests until at least one\n  synchronous replica comes up.\n\n**Note**: Because of the way synchronous replication is implemented in PostgreSQL it is still possible to lose\n  transactions even when using strict synchronous mode. If the PostgreSQL backend is cancelled while waiting to acknowledge\n  replication (as a result of packet cancellation due to client timeout or backend failure) transaction changes become\n  visible for other backends. Such changes are not yet replicated and may be lost in case of standby promotion.\n\n**sync-all**\n\nThe same as `sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number\n  of instances less one.\n\n**strict-sync-all**\n\nThe same as `strict-sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number\n  of instances less one.",
    "name": "mode",
    "required": false,
    "type": "string"
  },
  {
    "description": "Number of synchronous standby instances. Must be less than the total number of instances. It is set to 1 by default.\n  Only setteable if mode is `sync` or `strict-sync`.",
    "minimum": 1,
    "name": "syncInstances",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.replication.initialization
<sup><sup>[↩ Parent](#sgshardedclusterspecreplication)</sup></sup>

Allows specifying how the replicas are initialized.


{{< crd-properties >}}
[
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how old an SGBackup have to be in order to be seleceted\n to initialize a replica.\n\nWhen `FromExistingBackup` mode is set this field restrict the selection of SGBackup to be used for recovery newer than the\n specified value. \n\nWhen `FromNewlyCreatedBackup` mode is set this field skip the creation SGBackup to be used for recovery if one newer than\n the specified value exists.",
    "name": "backupNewerThan",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecreplicationinitializationbackuprestoreperformance",
    "description": "Configuration that affects the backup network and disk usage performance during recovery.",
    "name": "backupRestorePerformance",
    "required": false,
    "type": "object"
  },
  {
    "default": "FromExistingBackup",
    "description": "Allows specifying how the replicas are initialized.\n\nPossible values are:\n\n* `FromPrimary`: When this mode is used replicas will be always created from the primary using `pg_basebackup`.\n* `FromReplica`: When this mode is used replicas will be created from another existing replica using\n `pg_basebackup`. Fallsback to `FromPrimary` if there's no replica or it fails.\n* `FromExistingBackup`: When this mode is used replicas will be created from an existing SGBackup. If `backupNewerThan` is set\n the SGBackup must be newer than its value. When this mode fails to restore an SGBackup it will try with a previous one (if exists).\n Fallsback to `FromReplica` if there's no backup left or it fails.\n* `FromNewlyCreatedBackup`: When this mode is used replicas will be created from a newly created SGBackup.\n Fallsback to `FromExistingBackup` if `backupNewerThan` is set and exists a recent backup newer than its value or it fails.",
    "name": "mode",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replication.initialization.backupRestorePerformance
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicationinitialization)</sup></sup>

Configuration that affects the backup network and disk usage performance during recovery.


{{< crd-properties >}}
[
  {
    "description": "Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of file to read and 10.",
    "minimum": 1,
    "name": "downloadConcurrency",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum disk read I/O when performing a backup. In bytes (per second).",
    "name": "maxDiskBandwidth",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum storage upload bandwidth used when storing a backup. In bytes (per second).",
    "name": "maxNetworkBandwidth",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.workers
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

The workers are a group of StackGres clusters where the partitioned data chunks are stored.

When referring to the cluster in the descriptions below it applies to any worker's StackGres cluster.


{{< crd-properties >}}
[
  {
    "description": "Number of worker's StackGres clusters",
    "minimum": 0,
    "name": "clusters",
    "required": true,
    "type": "integer"
  },
  {
    "description": "Number of StackGres instances per worker's StackGres cluster. Each instance contains one Postgres server.\n  Out of all of the Postgres servers, one is elected as the primary, the rest remain as read-only replicas.",
    "minimum": 0,
    "name": "instancesPerCluster",
    "required": true,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecworkerspods",
    "description": "Cluster pod's configuration.",
    "name": "pods",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersautoscaling",
    "description": "This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.\n\nVertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.\nVertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.",
    "name": "autoscaling",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name template of worker's StackGres clusters.\n\nThe SGCluster name will be created using the name template and the index (starting at 0) as a suffix.\nFor instance with a `clusterNameTemplate` set to `myworker` the first SGCluster name will be `myworker0`.\n\nBy default the query router cluster name template is `\u003ccluster name\u003e-worker`.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "clusterNameTemplate",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkersconfigurations",
    "description": "Shards custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersmanagedsql",
    "description": "This section allows to reference SQL scripts that will be applied to the cluster live.",
    "name": "managedSql",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersmetadata",
    "description": "Metadata information from workers cluster created resources.",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindex",
    "description": "Any worker can be overridden by this section.\n\nAn entry only overrides the SGClusters of its `type` (see field `type`), and the\n`index` (or `indexes`) is always relative to that type. An entry of type `Worker`\nand an entry of type `QueryRouter` may therefore use the same index without\noverlapping.\n\nAny field that is not set in an entry is inherited from the section the overridden\nSGCluster is generated from: `.spec.workers` for workers and `.spec.coordinator`\nfor query routers. In particular a query router with no override uses\n`.spec.coordinator.sgInstanceProfile` and `.spec.coordinator.configurations`, and\n**not** the ones of `.spec.workers`.",
    "name": "overrides",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecworkersreplication",
    "description": "This section allows to configure the global Postgres replication mode.\n\nThe main replication group is implicit and contains the total number of instances less the sum of all\n  instances in other replication groups.\n\nThe total number of instances is always specified by `.spec.instances`.",
    "name": "replication",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGInstanceProfile](https://stackgres.io/doc/latest/reference/crd/sginstanceprofile/).\n\nA SGInstanceProfile defines CPU and memory limits. Must exist before creating a cluster.\n\nWhen no profile is set, a default (1 core, 2 GiB RAM) one is used.\n\n**Changing this field may require a restart.**",
    "name": "sgInstanceProfile",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.workers.pods
<sup><sup>[↩ Parent](#sgshardedclusterspecworkers)</sup></sup>

Cluster pod's configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkerspodspersistentvolume",
    "description": "Pod's persistent volume configuration.",
    "name": "persistentVolume",
    "required": true,
    "type": "object"
  },
  {
    "description": "A list of custom application containers that run within the workers cluster's Pods.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodscustomenvfromkeyindex",
    "description": "Custom Pod environment variables from a source for the specified container.",
    "name": "customEnvFrom",
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom application init containers that run within the coordinator cluster's Pods. The\n  custom init containers will run following the defined sequence at the end of\n  cluster's Pods init containers.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volumes that may be used along with any container defined in\n  customInitContainers or customContainers sections for the workers.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the customInitContainers or customContainers sections the name used\n  has to be prepended with the same prefix.\n\nOnly the following volume types are allowed: configMap, downwardAPI, emptyDir,\n  gitRepo, glusterfs, hostPath, nfs, projected and secret\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "customVolumes",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating a connection pooling (using [PgBouncer](https://www.pgbouncer.org/)) sidecar.\n\nIf sharding type is `citus` and `.spec.configurations.citus.connectToPooler` is `true` (the default) the nodes with this field set to `true` are not added to `pg_dist_poolinfo`, so Citus connects directly to their Postgres.\n\n**Changing this field may require a restart.**",
    "name": "disableConnectionPooling",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "If set to `true`, avoids creating the `envoy` sidecar. This sidecar is used as the endge proxy for the cluster's Pods providing extra metrics to the monitoring layer.\n\n**Changing this field may require a restart.**",
    "name": "disableEnvoy",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** use `.spec.configurations.observability.disableMetrics` instead.",
    "name": "disableMetricsExporter",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating the `postgres-util` sidecar. This sidecar contains usual Postgres administration utilities *that are not present in the main (`patroni`) container*, like `psql`. Only disable if you know what you are doing.\n\n**Changing this field may require a restart.**",
    "name": "disablePostgresUtil",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsdnsconfig",
    "description": "PodDNSConfig defines the DNS parameters of a pod in addition to those generated from DNSPolicy.",
    "name": "dnsConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Set DNS policy for the pod. Defaults to \"ClusterFirst\". Valid values are 'ClusterFirstWithHostNet', 'ClusterFirst', 'Default' or 'None'. DNS parameters given in DNSConfig will be merged with the policy selected with DNSPolicy. To have DNS options set along with hostNetwork, you have to specify DNS policy explicitly to 'ClusterFirstWithHostNet'.",
    "name": "dnsPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "Host networking requested for this pod. Use the host's network namespace. When using HostNetwork you should specify ports so the scheduler is aware. When `hostNetwork` is true, specified `hostPort` fields in port definitions must match `containerPort`, and unspecified `hostPort` fields in port definitions are defaulted to match `containerPort`. Default to false.",
    "name": "hostNetwork",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodslivenessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "livenessProbe",
    "required": false,
    "type": "object"
  },
  {
    "description": "managementPolicy controls how pods are created during initial scale up, when replacing pods\n  on nodes, or when scaling down. The default policy is `OrderedReady`, where pods are created\n  in increasing order (pod-0, then pod-1, etc) and the controller will wait until each pod is\n  ready before continuing. When scaling down, the pods are removed in the opposite order.\n  The alternative policy is `Parallel` which will create pods in parallel to match the desired\n  scale without waiting, and on scale down will delete all pods at once.",
    "name": "managementPolicy",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsreadinessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "readinessProbe",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsresources",
    "description": "Pod custom resources configuration.",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsscheduling",
    "description": "Pod custom scheduling, affinity and topology spread constratins configuration.\n\n**Changing this field may require a restart.**",
    "name": "scheduling",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "If true the pod's hostname will be configured as the pod's FQDN, rather than the leaf name (the default). In Linux containers, this means setting the FQDN in the hostname field of the kernel (the nodename field of struct utsname). In Windows containers, this means setting the registry value of hostname for the registry key HKEY_LOCAL_MACHINE\\\\SYSTEM\\\\CurrentControlSet\\\\Services\\\\Tcpip\\\\Parameters to FQDN. If a pod does not have FQDN, this has no effect. Default to false.",
    "name": "setHostnameAsFQDN",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsstartupprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "startupProbe",
    "required": false,
    "type": "object"
  },
  {
    "description": "serviceName is the name of the service that governs this StatefulSet. This service must exist before the StatefulSet, and is responsible for the network identity of the set. Pods get DNS/hostnames that follow the pattern: pod-specific-string.serviceName.default.svc.cluster.local where \"pod-specific-string\" is managed by the StatefulSet controller.",
    "name": "statefulSetServiceName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully. May be decreased in delete request. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). If this value is nil, the default grace period will be used instead. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. Defaults to 30 seconds.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsupdatestrategy",
    "default": {
      "type": "OnlyDbOps"
    },
    "description": "This section indicates the strategy that the SGCluster controller will use to perform updates.\n\nIt includes any additional parameters necessary to perform the update for the indicated strategy.\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.",
    "name": "updateStrategy",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.persistentVolume
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

Pod's persistent volume configuration.

{{< crd-properties >}}
[
  {
    "description": "Size of the PersistentVolume set for each instance of the cluster. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).",
    "name": "size",
    "required": true,
    "type": "string"
  },
  {
    "description": "fsGroupChangePolicy defines behavior of changing ownership and permission of the volume before being exposed inside Pod. This field will only apply to volume types which support fsGroup based ownership(and permissions). It will have no effect on ephemeral volume types such as: secret, configmaps and emptydir. Valid values are \"OnRootMismatch\" and \"Always\". If not specified, \"Always\" is used. Note that this field cannot be set when spec.os.name is windows.",
    "name": "fsGroupChangePolicy",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodspersistentvolumeiolimits",
    "description": "This section allows to define per-volume I/O limits for nodes using [cgroup v2 `io.max` features](https://docs.kernel.org/admin-guide/cgroup-v2.html).\n\n\u003e **NOTE**: This is an **alpha** feature.\n\nWhen multiple PostgreSQL clusters share the same local storage device on a Kubernetes node, a single cluster running a heavy workload\n (for example large COPY, aggressive VACUUM, or an unoptimized query) can saturate the device's I/O bandwidth and degrade performance\n for every other cluster on that node. StackGres provides this mechanism to prevent this class of noisy-neighbor problem.\nThe mechanism operates at the cgroup + block-device layer, not at the PVC abstraction. Since each pod has its own cgroup, limits are\n enforced independently across pods even when they share the same physical device — this is the core of the noisy-neighbor protection.\nHowever, if multiple volumes within the same pod share the same backing device, their limits target a single entry in that pod's cgroup\n and cannot be enforced independently of each other.\n\nThis feature works on top of existing CSI drivers such as TopoLVM or OpenEBS LocalPV. The primary target is on-premises deployments where\n multiple StackGres clusters share local NVMe storage on the same node.\n\n\u003e **IMPORTANT**: Setting any of the value above will require to create an init container named `setup-io-limits` in each SGCluster's Pods\n\u003e  running as root with only the `CHOWN` cabability set. Also both the `setup-io-limits` init container and the `cluster-controller`\n\u003e  container will require to mount the host path `/sys/fs/cgroup` in order to be able to set owner of and write to the `io.max` file that\n\u003e  will allows to set the io limits.\n\u003e  For this reason this feature must be enabled globally by the operator administrator by adding the `io-limits` feature gate under\n\u003e  `.spec.featureGates` of the SGConfig. Setting any of the value above without the `io-limits` feature gate enabled will be rejected.",
    "name": "ioLimits",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.",
    "name": "storageClass",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.\n\nSee https://kubernetes.io/docs/concepts/storage/volume-attributes-classes/",
    "name": "volumeAttributesClassName",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.persistentVolume.ioLimits
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodspersistentvolume)</sup></sup>

This section allows to define per-volume I/O limits for nodes using [cgroup v2 `io.max` features](https://docs.kernel.org/admin-guide/cgroup-v2.html).

> **NOTE**: This is an **alpha** feature.

When multiple PostgreSQL clusters share the same local storage device on a Kubernetes node, a single cluster running a heavy workload
 (for example large COPY, aggressive VACUUM, or an unoptimized query) can saturate the device's I/O bandwidth and degrade performance
 for every other cluster on that node. StackGres provides this mechanism to prevent this class of noisy-neighbor problem.
The mechanism operates at the cgroup + block-device layer, not at the PVC abstraction. Since each pod has its own cgroup, limits are
 enforced independently across pods even when they share the same physical device — this is the core of the noisy-neighbor protection.
However, if multiple volumes within the same pod share the same backing device, their limits target a single entry in that pod's cgroup
 and cannot be enforced independently of each other.

This feature works on top of existing CSI drivers such as TopoLVM or OpenEBS LocalPV. The primary target is on-premises deployments where
 multiple StackGres clusters share local NVMe storage on the same node.

> **IMPORTANT**: Setting any of the value above will require to create an init container named `setup-io-limits` in each SGCluster's Pods
>  running as root with only the `CHOWN` cabability set. Also both the `setup-io-limits` init container and the `cluster-controller`
>  container will require to mount the host path `/sys/fs/cgroup` in order to be able to set owner of and write to the `io.max` file that
>  will allows to set the io limits.
>  For this reason this feature must be enabled globally by the operator administrator by adding the `io-limits` feature gate under
>  `.spec.featureGates` of the SGConfig. Setting any of the value above without the `io-limits` feature gate enabled will be rejected.


{{< crd-properties >}}
[
  {
    "description": "Max read IO operations per second. Leave empty to remove this limit.",
    "name": "readIops",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max read MiB per second. Leave empty to remove this limit.",
    "name": "readMiBps",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max write IO operations per second. Leave empty to remove this limit.",
    "name": "writeIops",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max write MiB per second. Leave empty to remove this limit.",
    "name": "writeMiBps",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.customEnvFrom[key][index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

EnvFromSource represents the source of a set of ConfigMaps or Secrets

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#envfromsource-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkerspodscustomenvfromkeyindexconfigmapref",
    "description": "ConfigMapEnvSource selects a ConfigMap to populate the environment variables with.\n\nThe contents of the target ConfigMap's Data field will represent the key-value pairs as environment variables.",
    "name": "configMapRef",
    "required": false,
    "type": "object"
  },
  {
    "description": "Optional text to prepend to the name of each environment variable. May consist of any printable ASCII characters except '='.",
    "name": "prefix",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodscustomenvfromkeyindexsecretref",
    "description": "SecretEnvSource selects a Secret to populate the environment variables with.\n\nThe contents of the target Secret's Data field will represent the key-value pairs as environment variables.",
    "name": "secretRef",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.customEnvFrom[key][index].configMapRef
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodscustomenvfromkeyindex)</sup></sup>

ConfigMapEnvSource selects a ConfigMap to populate the environment variables with.

The contents of the target ConfigMap's Data field will represent the key-value pairs as environment variables.

{{< crd-properties >}}
[
  {
    "description": "Name of the referent. This field is effectively required, but due to backwards compatibility is allowed to be empty. Instances of this type with an empty value here are almost certainly wrong. More info: https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "Specify whether the ConfigMap must be defined",
    "name": "optional",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.customEnvFrom[key][index].secretRef
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodscustomenvfromkeyindex)</sup></sup>

SecretEnvSource selects a Secret to populate the environment variables with.

The contents of the target Secret's Data field will represent the key-value pairs as environment variables.

{{< crd-properties >}}
[
  {
    "description": "Name of the referent. This field is effectively required, but due to backwards compatibility is allowed to be empty. Instances of this type with an empty value here are almost certainly wrong. More info: https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "Specify whether the Secret must be defined",
    "name": "optional",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.dnsConfig
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

PodDNSConfig defines the DNS parameters of a pod in addition to those generated from DNSPolicy.

{{< crd-properties >}}
[
  {
    "description": "A list of DNS name server IP addresses. This will be appended to the base nameservers generated from DNSPolicy. Duplicated nameservers will be removed.",
    "name": "nameservers",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsdnsconfigoptionsindex",
    "description": "A list of DNS resolver options. This will be merged with the base options generated from DNSPolicy. Duplicated entries will be removed. Resolution options given in Options will override those that appear in the base DNSPolicy.",
    "name": "options",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of DNS search domains for host-name lookup. This will be appended to the base search paths generated from DNSPolicy. Duplicated search paths will be removed.",
    "name": "searches",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.dnsConfig.options[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodsdnsconfig)</sup></sup>

PodDNSConfigOption defines DNS resolver options of a pod.

{{< crd-properties >}}
[
  {
    "description": "Name is this DNS resolver option's name. Required.",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "Value is this DNS resolver option's value.",
    "name": "value",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.livenessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.readinessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.resources
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

Pod custom resources configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkerspodsresourcescontainerskey",
    "description": "Resources configuration to be merged with the specified container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "containers",
    "required": false,
    "type": "map[string]object"
  },
  {
    "description": "When set to `true` the resources requests values in fields `SGInstanceProfile.spec.requests.cpu` and `SGInstanceProfile.spec.requests.memory` will represent the resources\n requests of the patroni container and the total resources requests calculated by adding the resources requests of all the containers (including the patroni container).\n\n**Changing this field may require a restart.**",
    "name": "disableResourcesRequestsSplitFromTotal",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When enabled resource limits for containers other than the patroni container will be set just like for patroni container as specified in the SGInstanceProfile.\n\n**Changing this field may require a restart.**",
    "name": "enableClusterLimitsRequirements",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When set to `true` the reconciliation of the cluster will fail if `disableResourcesRequestsSplitFromTotal` is not set or set to `false` and the sum of the CPU or memory\n of all the containers except patroni is equals or higher than the total specified in `SGInstanceProfile.spec.requests.cpu` or `SGInstanceProfile.spec.requests.memory`.\n\nWhen `false` (the default) and `disableResourcesRequestsSplitFromTotal` is not set or set to `false` and the sum of the CPU or memory\n of all the containers except patroni is equals or higher than the total specified in `SGInstanceProfile.spec.requests.cpu` or `SGInstanceProfile.spec.requests.memory`\n then the patroni container resources will be set to 0.",
    "name": "failWhenTotalIsHigher",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsresourcesinitcontainerskey",
    "description": "Resources configuration to be merged with the specified init container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "initContainers",
    "required": false,
    "type": "map[string]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.resources.containers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkerspodsresourcescontainerskeyclaimsindex",
    "description": "Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.\n\nThis field depends on the DynamicResourceAllocation feature gate.\n\nThis field is immutable. It can only be set for containers.",
    "name": "claims",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "limits",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "requests",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.resources.containers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodsresourcescontainerskey)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

{{< crd-properties >}}
[
  {
    "description": "Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.",
    "name": "request",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.resources.initContainers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkerspodsresourcesinitcontainerskeyclaimsindex",
    "description": "Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.\n\nThis field depends on the DynamicResourceAllocation feature gate.\n\nThis field is immutable. It can only be set for containers.",
    "name": "claims",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "limits",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "requests",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.resources.initContainers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodsresourcesinitcontainerskey)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

{{< crd-properties >}}
[
  {
    "description": "Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.",
    "name": "request",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.scheduling
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

Pod custom scheduling, affinity and topology spread constratins configuration.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkerspodsschedulingbackup",
    "description": "Backup Pod custom scheduling and affinity configuration.",
    "name": "backup",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core",
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
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podantiaffinity-v1-core",
    "name": "podAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "PreemptionPolicy is the Policy for preempting pods with lower priority. One of Never, PreemptLowerPriority. Defaults to PreemptLowerPriority if unset.",
    "name": "preemptionPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.",
    "name": "priorityClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "RuntimeClassName refers to a RuntimeClass object in the node.k8s.io group, which should be used to run this pod.  If no RuntimeClass resource matches the named class, the pod will not be run. If unset or empty, the \"legacy\" RuntimeClass will be used, which is an implicit class with an empty definition that uses the default runtime handler. More info: https://git.k8s.io/enhancements/keps/sig-node/585-runtime-class",
    "name": "runtimeClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod will be dispatched by specified scheduler. If not specified, the pod will be dispatched by default scheduler.",
    "name": "schedulerName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#topologyspreadconstraint-v1-core",
    "name": "topologySpreadConstraints",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.scheduling.backup
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodsscheduling)</sup></sup>

Backup Pod custom scheduling and affinity configuration.

{{< crd-properties >}}
[
  {
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core",
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
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podantiaffinity-v1-core",
    "name": "podAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "PreemptionPolicy is the Policy for preempting pods with lower priority. One of Never, PreemptLowerPriority. Defaults to PreemptLowerPriority if unset.",
    "name": "preemptionPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.",
    "name": "priorityClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "RuntimeClassName refers to a RuntimeClass object in the node.k8s.io group, which should be used to run this pod.  If no RuntimeClass resource matches the named class, the pod will not be run. If unset or empty, the \"legacy\" RuntimeClass will be used, which is an implicit class with an empty definition that uses the default runtime handler. More info: https://git.k8s.io/enhancements/keps/sig-node/585-runtime-class",
    "name": "runtimeClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod will be dispatched by specified scheduler. If not specified, the pod will be dispatched by default scheduler.",
    "name": "schedulerName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.startupProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.updateStrategy
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspods)</sup></sup>

This section indicates the strategy that the SGCluster controller will use to perform updates.

It includes any additional parameters necessary to perform the update for the indicated strategy.

When a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of
 the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the
 primary is demoted instead of being restarted.

The Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters
 `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is
 decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,
 see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover
 to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is
 performed.


{{< crd-properties >}}
[
  {
    "default": "InPlace",
    "description": "Indicates the method of the update strategy. Default is `InPlace`.\n\n* `InPlace`: update will be performed on the existing instances.\n* `ReducedImpact`: before an update is performed on the existing instances a new instance if created to reduce the impact on read-only replicas.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "default": "PT5M",
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS` indicating the delay that has to pass between the restart of a\n Postgres instance performed by the SGCluster controller and the next one. Default is `PT5M`.",
    "name": "restartDelay",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkerspodsupdatestrategyscheduleindex",
    "description": "Indicates a list of windows of time where an update can be performed.",
    "name": "schedule",
    "required": false,
    "type": "[]object"
  },
  {
    "default": "OnlyDbOps",
    "description": "Indicates the type of the update strategy. Default is `OnlyDbOps`.\n\n* `Always`: update will be performed as soon as possible.\n* `Schedule`: update will be performed as specified in the schedule section where you can configure windows of time where the update can be performed.\n* `OnlyDbOps`: update will be performed only when an SGDbOps of type restart, securityUpgrade or minorVersionUpgrade targeting the SGCluster is started.\n* `Never`: update will never be performed (even if annotation `stackgres.io/rollout` is present with a different value) unless the Pods are deleted manually.\n\nAlternatively to this configuration you may specify the following annotations:\n\n* `stackgres.io/rollout=always`: Same behavior as `Always`.\n* `stackgres.io/rollout=schedule`: Same behavior as `Schedule`.\n* `stackgres.io/rollout=never`: Same behavior as `Never` (even if type has a different value).\n\nUnless `Never` (or the annotation `stackgres.io/rollout=never`) is specified an `SGDbOps` of type restart, securityUpgrade or minorVersionUpgrade targeting the SGCluster\n will trigger an update of the Pods.\n\nThe update of the Pods will be performed only if Pods are pending any change. Changes in the configuration may not require restarting the Pod and, in such cases, only\n the Postgres instance will be restarted.",
    "name": "type",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.pods.updateStrategy.schedule[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkerspodsupdatestrategy)</sup></sup>

Indicates a window of time where an update can be performed.

{{< crd-properties >}}
[
  {
    "description": "A UNIX cron expression indicating the start of the window of time where the update can be performed.",
    "name": "cron",
    "required": false,
    "type": "string"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that, together with the cron expression, indicates the end of the window of time where the update can be performed.",
    "name": "duration",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.workers.autoscaling
<sup><sup>[↩ Parent](#sgshardedclusterspecworkers)</sup></sup>

This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.

Vertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.
Vertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersautoscalinghorizontal",
    "description": "Section to configure horizontal Pod autoscaling aspects.",
    "name": "horizontal",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersautoscalingmaxallowed",
    "description": "Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "maxAllowed",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersautoscalingminallowed",
    "description": "Defines the lower bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "minAllowed",
    "required": false,
    "type": "object"
  },
  {
    "default": "vertical",
    "description": "Allows enabling or disabling any of horizontal and vertical Pod autoscaling.\n\nPossible values are:\n* `vertical`: only vertical Pod autoscaling will be enabled (default)\n* `none`: all autoscaling will be disabled",
    "enum": [
      "vertical",
      "none"
    ],
    "name": "mode",
    "required": false,
    "type": "enum"
  },
  {
    "anchor": "sgshardedclusterspecworkersautoscalingvertical",
    "description": "Section to configure vertical Pod autoscaling aspects.",
    "name": "vertical",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.horizontal
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscaling)</sup></sup>

Section to configure horizontal Pod autoscaling aspects.


{{< crd-properties >}}
[
  {
    "default": 300,
    "description": "The period in seconds before the downscale of replica instances can be triggered.",
    "name": "cooldownPeriod",
    "required": false,
    "type": "integer"
  },
  {
    "default": 30,
    "description": "The interval in seconds to check if the scaleup or scaledown have to be triggered.",
    "name": "pollingInterval",
    "required": false,
    "type": "integer"
  },
  {
    "default": "AverageValue",
    "description": "The metric type for connections used metric. See https://keda.sh/docs/latest/concepts/scaling-deployments/#triggers",
    "name": "replicasConnectionsUsageMetricType",
    "required": false,
    "type": "string"
  },
  {
    "default": "0.8",
    "description": "The target value for replicas connections used in order to trigger the upscale of replica instances.",
    "name": "replicasConnectionsUsageTarget",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.maxAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscaling)</sup></sup>

Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersautoscalingmaxallowedenvoy",
    "description": "Defines the higher bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersautoscalingmaxallowedpatroni",
    "description": "Defines the higher bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersautoscalingmaxallowedpgbouncer",
    "description": "Defines the higher bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.maxAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of envoy container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the envoy container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the envoy container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.maxAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of patroni container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the patroni container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the patroni container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.maxAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of pgbouncer container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the pgbouncer container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the pgbouncer container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.minAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscaling)</sup></sup>

Defines the lower bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersautoscalingminallowedenvoy",
    "description": "Defines the lower bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersautoscalingminallowedpatroni",
    "description": "Defines the lower bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersautoscalingminallowedpgbouncer",
    "description": "Defines the lower bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.minAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of envoy container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the envoy container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the envoy container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.minAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of patroni container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the patroni container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the patroni container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.minAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of pgbouncer container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the pgbouncer container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the pgbouncer container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.autoscaling.vertical
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersautoscaling)</sup></sup>

Section to configure vertical Pod autoscaling aspects.


{{< crd-properties >}}
[
  {
    "description": "Recommender responsible for generating recommendation for vertical Pod autoscaling. If not specified the default one will be used.",
    "name": "recommender",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.workers.configurations
<sup><sup>[↩ Parent](#sgshardedclusterspecworkers)</sup></sup>

Shards custom configurations.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersconfigurationspatroni",
    "description": "Allows specifying Patroni configuration that will extend the generated one\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersconfigurationspooling",
    "description": "",
    "name": "pooling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersconfigurationspostgres",
    "description": "Section for postgres configuration.",
    "name": "postgres",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersconfigurationspostgresexporter",
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.",
    "name": "postgresExporter",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig) used for this cluster. Each pod contains a sidecar with a connection pooler (currently: [PgBouncer](https://www.pgbouncer.org/)). The connection pooler is implemented as a sidecar.\n\nIf not set, a default configuration will be used. Disabling connection pooling altogether is possible if the disableConnectionPooling property of the pods object is set to true.\n\n**Changing this field may require a restart.**",
    "name": "sgPoolingConfig",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig) used for the cluster. It must exist. When not set, a default Postgres config, for the major version selected, is used.\n\n**Changing this field may require a restart.**",
    "name": "sgPostgresConfig",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.configurations.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersconfigurations)</sup></sup>

Allows specifying Patroni configuration that will extend the generated one

If sharding type is `shardingsphere` then this section is ignored.


{{< crd-properties >}}
[
  {
    "description": "Configure Patroni to use FQDN instead of the Pod's assigned IPs to connect between Patroni and Postgres instances.",
    "name": "connectUsingFqdn",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Allows specifying Patroni dynamic configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/dynamic_configuration.html\n\nThe following configuration fields will be ignored:\n\n* synchronous_mode\n* synchronous_mode_strict\n* postgresql\n* standby_cluster\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "dynamicConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Allows specifying Patroni configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/yaml_configuration.html\n\nThe following configuration fields will be ignored:\n\n* name\n* namespace\n* log\n* bootstrap\n* citus\n* postgresql # with the exception of postgresql.callbacks, postgresql.pre_promote, postgresql.before_stop and postgresql.pg_ctl_timeout\n* restapi\n* ctl\n* watchdog\n* tags\n\nIf sharding type is `shardingsphere` then this section is ignored.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "initialConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Annotations that must be set on the generated SGClusters before Patroni is started. See `SGCluster.spec.configurations.patroni.startGateAnnotations`.\n\nIf sharding type is `citus` the operator also adds the annotation required to start the query routers only once they have been registered by the coordinator.",
    "name": "startGateAnnotations",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.configurations.pooling
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersconfigurations)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersconfigurationspoolingpgbouncer",
    "description": "",
    "name": "pgBouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.configurations.pooling.pgBouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersconfigurationspooling)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersconfigurationspoolingpgbouncerpgbouncerini",
    "description": "The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).\n\n**Changing this field may require a restart.**",
    "name": "pgbouncer.ini",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.configurations.pooling.pgBouncer.pgbouncer.ini
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersconfigurationspoolingpgbouncer)</sup></sup>

The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "description": "The `pgbouncer.ini` (Section [databases]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-databases) for more information about supported parameters.",
    "name": "databases",
    "requireRestart": true,
    "required": false,
    "type": "map[string]map[string]string"
  },
  {
    "description": "The `pgbouncer.ini` (Section [pgbouncer]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#generic-settings) for more information about supported parameters",
    "name": "pgbouncer",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "The `pgbouncer.ini` (Section [users]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-users) for more information about supported parameters.",
    "name": "users",
    "requireRestart": true,
    "required": false,
    "type": "map[string]map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.configurations.postgres
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersconfigurations)</sup></sup>

Section for postgres configuration.

{{< crd-properties >}}
[
  {
    "description": "The `postgresql.conf` parameters that overwrite those specified in the referenced [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig).\n\n**Changing this field may require a restart.**",
    "name": "postgresql.conf",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.configurations.postgresExporter
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersconfigurations)</sup></sup>

Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.

{{< crd-properties >}}
[
  {
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) queries.yaml configuration.\n\nThe queries defined here will overwrite those created by the operator.\n\n**WARNING**: Changing this may beak some of the functionality that depend on the query overwritten.",
    "name": "queries",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.workers.managedSql
<sup><sup>[↩ Parent](#sgshardedclusterspecworkers)</sup></sup>

This section allows to reference SQL scripts that will be applied to the cluster live.


{{< crd-properties >}}
[
  {
    "description": "If true, when any entry of any `SGScript` fail will not prevent subsequent `SGScript` from being executed. By default is `false`.",
    "name": "continueOnSGScriptError",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkersmanagedsqlscriptsindex",
    "description": "A list of script references that will be executed in sequence.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersmanagedsql)</sup></sup>

A script reference. Each version of each entry of the script referenced will be executed exactly once following the sequence defined
  in the referenced script and skipping any script entry that have already been executed.


{{< crd-properties >}}
[
  {
    "description": "The id is immutable and must be unique across all the `SGScript` entries. It is replaced by the operator and is used to identify the `SGScript` entry.",
    "name": "id",
    "required": false,
    "type": "integer"
  },
  {
    "description": "A reference to an `SGScript`",
    "name": "sgScript",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.workers.metadata
<sup><sup>[↩ Parent](#sgshardedclusterspecworkers)</sup></sup>

Metadata information from workers cluster created resources.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersmetadataannotations",
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersmetadatalabels",
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.",
    "name": "labels",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.metadata.annotations
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersmetadata)</sup></sup>

Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.

{{< crd-properties >}}
[
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "serviceAccount",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.metadata.labels
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersmetadata)</sup></sup>

Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.

{{< crd-properties >}}
[
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "serviceAccount",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.workers.overrides[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkers)</sup></sup>

Any worker can be overridden by this section.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscaling",
    "description": "This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.\n\nVertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.\nVertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.",
    "name": "autoscaling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexconfigurations",
    "description": "Shards custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "description": "Identifier of the SGCluster worker to override (starting from 0).\n\nThe identifier is relative to the `type` of the entry. When `type` is\n`QueryRouter` it is the zero-based identifier of the query router (i.e. `0`\nselects the first query router) and not the Citus group identifier, that is\noffset by `.spec.coordinator.queryRouterIndexOffset`.\n\nThis field is mutually exclusive with `indexes` and one of the two must be set.",
    "minimum": 0,
    "name": "index",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Identifiers of the SGCluster workers to override (starting from 0).\n\nEach entry can be a single identifier (e.g. `3`), a range specified by\nan initial and a final identifier (both included) separated by a dash\ncharacter (e.g. `1-2` will override clusters with index 1 and 2) or `all` to\nspecify all the identifiers. Type of the entry is IntOrString.\n\nThe identifiers are relative to the `type` of the entry, as described in field\n`index`.\n\nThis field is mutually exclusive with `index` and one of the two must be set.",
    "name": "indexes",
    "required": false,
    "type": "[]int or string"
  },
  {
    "description": "Number of StackGres instances per worker's StackGres cluster. Each instance contains one Postgres server.\n  Out of all of the Postgres servers, one is elected as the primary, the rest remain as read-only replicas.",
    "minimum": 0,
    "name": "instancesPerCluster",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexmanagedsql",
    "description": "This section allows to reference SQL scripts that will be applied to the cluster live.",
    "name": "managedSql",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexmetadata",
    "description": "Metadata information from workers cluster created resources.",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpods",
    "description": "Cluster pod's configuration.",
    "name": "pods",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexreplication",
    "description": "This section allows to configure the global Postgres replication mode.\n\nThe main replication group is implicit and contains the total number of instances less the sum of all\n  instances in other replication groups.\n\nThe total number of instances is always specified by `.spec.instances`.",
    "name": "replication",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGInstanceProfile](https://stackgres.io/doc/latest/reference/crd/sginstanceprofile/). A SGInstanceProfile defines CPU and memory limits. Must exist before creating a cluster. When no profile is set, a default (currently: 1 core, 2 GiB RAM) one is used.",
    "name": "sgInstanceProfile",
    "required": false,
    "type": "string"
  },
  {
    "description": "Specifies the type of workers to override. Possible values are:\n\n* `Worker`: SGClusters that store the data of sharded tables. See also `.spec.workers`.\n* `QueryRouter`: SGClusters that do not store data but route read/write queries. See also `.spec.coordinator.queryRouterClusters`.\n\nDefaults to `Worker` when not specified.\n\nThe fields that are not set in an entry of type `Worker` are inherited from\n`.spec.workers`, while those of an entry of type `QueryRouter` are inherited\nfrom `.spec.coordinator`, since query routers are generated from the\ncoordinator section.",
    "name": "type",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindex)</sup></sup>

This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.

Vertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.
Vertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalinghorizontal",
    "description": "Section to configure horizontal Pod autoscaling aspects.",
    "name": "horizontal",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingmaxallowed",
    "description": "Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "maxAllowed",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingminallowed",
    "description": "Defines the lower bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "minAllowed",
    "required": false,
    "type": "object"
  },
  {
    "default": "vertical",
    "description": "Allows enabling or disabling any of horizontal and vertical Pod autoscaling.\n\nPossible values are:\n* `vertical`: only vertical Pod autoscaling will be enabled (default)\n* `none`: all autoscaling will be disabled",
    "enum": [
      "vertical",
      "none"
    ],
    "name": "mode",
    "required": false,
    "type": "enum"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingvertical",
    "description": "Section to configure vertical Pod autoscaling aspects.",
    "name": "vertical",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.horizontal
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscaling)</sup></sup>

Section to configure horizontal Pod autoscaling aspects.


{{< crd-properties >}}
[
  {
    "default": 300,
    "description": "The period in seconds before the downscale of replica instances can be triggered.",
    "name": "cooldownPeriod",
    "required": false,
    "type": "integer"
  },
  {
    "default": 30,
    "description": "The interval in seconds to check if the scaleup or scaledown have to be triggered.",
    "name": "pollingInterval",
    "required": false,
    "type": "integer"
  },
  {
    "default": "AverageValue",
    "description": "The metric type for connections used metric. See https://keda.sh/docs/latest/concepts/scaling-deployments/#triggers",
    "name": "replicasConnectionsUsageMetricType",
    "required": false,
    "type": "string"
  },
  {
    "default": "0.8",
    "description": "The target value for replicas connections used in order to trigger the upscale of replica instances.",
    "name": "replicasConnectionsUsageTarget",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.maxAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscaling)</sup></sup>

Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingmaxallowedenvoy",
    "description": "Defines the higher bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingmaxallowedpatroni",
    "description": "Defines the higher bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingmaxallowedpgbouncer",
    "description": "Defines the higher bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.maxAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of envoy container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the envoy container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the envoy container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.maxAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of patroni container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the patroni container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the patroni container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.maxAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscalingmaxallowed)</sup></sup>

Defines the higher bound for Pod resources of pgbouncer container


{{< crd-properties >}}
[
  {
    "description": "The maximum allowed CPU for the pgbouncer container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum allowed memory for the pgbouncer container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.minAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscaling)</sup></sup>

Defines the lower bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingminallowedenvoy",
    "description": "Defines the lower bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingminallowedpatroni",
    "description": "Defines the lower bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexautoscalingminallowedpgbouncer",
    "description": "Defines the lower bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.minAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of envoy container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the envoy container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the envoy container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.minAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of patroni container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the patroni container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the patroni container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.minAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscalingminallowed)</sup></sup>

Defines the lower bound for Pod resources of pgbouncer container


{{< crd-properties >}}
[
  {
    "description": "The minimum allowed CPU for the pgbouncer container",
    "name": "cpu",
    "required": false,
    "type": "string"
  },
  {
    "description": "The minimum allowed memory for the pgbouncer container",
    "name": "memory",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].autoscaling.vertical
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexautoscaling)</sup></sup>

Section to configure vertical Pod autoscaling aspects.


{{< crd-properties >}}
[
  {
    "description": "Recommender responsible for generating recommendation for vertical Pod autoscaling. If not specified the default one will be used.",
    "name": "recommender",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].configurations
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindex)</sup></sup>

Shards custom configurations.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexconfigurationspatroni",
    "description": "Allows specifying Patroni configuration that will extend the generated one\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexconfigurationspooling",
    "description": "",
    "name": "pooling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexconfigurationspostgres",
    "description": "Section for postgres configuration.",
    "name": "postgres",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexconfigurationspostgresexporter",
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.",
    "name": "postgresExporter",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig) used for this cluster. Each pod contains a sidecar with a connection pooler (currently: [PgBouncer](https://www.pgbouncer.org/)). The connection pooler is implemented as a sidecar.\n\nIf not set, a default configuration will be used. Disabling connection pooling altogether is possible if the disableConnectionPooling property of the pods object is set to true.",
    "name": "sgPoolingConfig",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig) used for the cluster. It must exist. When not set, a default Postgres config, for the major version selected, is used.",
    "name": "sgPostgresConfig",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].configurations.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexconfigurations)</sup></sup>

Allows specifying Patroni configuration that will extend the generated one

If sharding type is `shardingsphere` then this section is ignored.


{{< crd-properties >}}
[
  {
    "description": "Configure Patroni to use FQDN instead of the Pod's assigned IPs to connect between Patroni and Postgres instances.",
    "name": "connectUsingFqdn",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Allows specifying Patroni dynamic configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/dynamic_configuration.html\n\nThe following configuration fields will be ignored:\n\n* synchronous_mode\n* synchronous_mode_strict\n* postgresql\n* standby_cluster\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "dynamicConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Allows specifying Patroni configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/yaml_configuration.html\n\nThe following configuration fields will be ignored:\n\n* name\n* namespace\n* log\n* bootstrap\n* citus\n* postgresql # with the exception of postgresql.callbacks, postgresql.pre_promote, postgresql.before_stop and postgresql.pg_ctl_timeout\n* restapi\n* ctl\n* watchdog\n* tags\n\nIf sharding type is `shardingsphere` then this section is ignored.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "initialConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Annotations that must be set on the generated SGClusters before Patroni is started. See `SGCluster.spec.configurations.patroni.startGateAnnotations`.\n\nIf sharding type is `citus` the operator also adds the annotation required to start the query routers only once they have been registered by the coordinator.",
    "name": "startGateAnnotations",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].configurations.pooling
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexconfigurations)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexconfigurationspoolingpgbouncer",
    "description": "",
    "name": "pgBouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].configurations.pooling.pgBouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexconfigurationspooling)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexconfigurationspoolingpgbouncerpgbouncerini",
    "description": "The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).\n\n**Changing this field may require a restart.**",
    "name": "pgbouncer.ini",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].configurations.pooling.pgBouncer.pgbouncer.ini
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexconfigurationspoolingpgbouncer)</sup></sup>

The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "description": "The `pgbouncer.ini` (Section [databases]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-databases) for more information about supported parameters.",
    "name": "databases",
    "requireRestart": true,
    "required": false,
    "type": "map[string]map[string]string"
  },
  {
    "description": "The `pgbouncer.ini` (Section [pgbouncer]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#generic-settings) for more information about supported parameters",
    "name": "pgbouncer",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "The `pgbouncer.ini` (Section [users]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.\n\nCheck [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-users) for more information about supported parameters.",
    "name": "users",
    "requireRestart": true,
    "required": false,
    "type": "map[string]map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].configurations.postgres
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexconfigurations)</sup></sup>

Section for postgres configuration.

{{< crd-properties >}}
[
  {
    "description": "The `postgresql.conf` parameters that overwrite those specified in the referenced [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig).\n\n**Changing this field may require a restart.**",
    "name": "postgresql.conf",
    "requireRestart": true,
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].configurations.postgresExporter
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexconfigurations)</sup></sup>

Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.

{{< crd-properties >}}
[
  {
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) queries.yaml configuration.\n\nThe queries defined here will overwrite those created by the operator.\n\n**WARNING**: Changing this may beak some of the functionality that depend on the query overwritten.",
    "name": "queries",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].managedSql
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindex)</sup></sup>

This section allows to reference SQL scripts that will be applied to the cluster live.


{{< crd-properties >}}
[
  {
    "description": "If true, when any entry of any `SGScript` fail will not prevent subsequent `SGScript` from being executed. By default is `false`.",
    "name": "continueOnSGScriptError",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexmanagedsqlscriptsindex",
    "description": "A list of script references that will be executed in sequence.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexmanagedsql)</sup></sup>

A script reference. Each version of each entry of the script referenced will be executed exactly once following the sequence defined
  in the referenced script and skipping any script entry that have already been executed.


{{< crd-properties >}}
[
  {
    "description": "The id is immutable and must be unique across all the `SGScript` entries. It is replaced by the operator and is used to identify the `SGScript` entry.",
    "name": "id",
    "required": false,
    "type": "integer"
  },
  {
    "description": "A reference to an `SGScript`",
    "name": "sgScript",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].metadata
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindex)</sup></sup>

Metadata information from workers cluster created resources.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexmetadataannotations",
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexmetadatalabels",
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.",
    "name": "labels",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].metadata.annotations
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexmetadata)</sup></sup>

Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.

{{< crd-properties >}}
[
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "serviceAccount",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].metadata.labels
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexmetadata)</sup></sup>

Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.

{{< crd-properties >}}
[
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "allResources",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "clusterPods",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "primaryService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "replicasService",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "serviceAccount",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "services",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindex)</sup></sup>

Cluster pod's configuration.

{{< crd-properties >}}
[
  {
    "description": "A list of custom application containers that run within the workers cluster's Pods.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom application init containers that run within the coordinator cluster's Pods. The\n  custom init containers will run following the defined sequence at the end of\n  cluster's Pods init containers.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customInitVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "customVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volumes that may be used along with any container defined in\n  customInitContainers or customContainers sections for the workers.\n\nThe name used in this section will be prefixed with the string `custom-` so that when\n  referencing them in the customInitContainers or customContainers sections the name used\n  has to be prepended with the same prefix.\n\nOnly the following volume types are allowed: configMap, downwardAPI, emptyDir,\n  gitRepo, glusterfs, hostPath, nfs, projected and secret\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "customVolumes",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating a connection pooling (using [PgBouncer](https://www.pgbouncer.org/)) sidecar.\n\nIf sharding type is `citus` and `.spec.configurations.citus.connectToPooler` is `true` (the default) the nodes with this field set to `true` are not added to `pg_dist_poolinfo`, so Citus connects directly to their Postgres.\n\n**Changing this field may require a restart.**",
    "name": "disableConnectionPooling",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "If set to `true`, avoids creating the `envoy` sidecar. This sidecar is used as the endge proxy for the cluster's Pods providing extra metrics to the monitoring layer.\n\n**Changing this field may require a restart.**",
    "name": "disableEnvoy",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** use `.spec.configurations.observability.disableMetrics` instead.",
    "name": "disableMetricsExporter",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating the `postgres-util` sidecar. This sidecar contains usual Postgres administration utilities *that are not present in the main (`patroni`) container*, like `psql`. Only disable if you know what you are doing.\n\n**Changing this field may require a restart.**",
    "name": "disablePostgresUtil",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsdnsconfig",
    "description": "PodDNSConfig defines the DNS parameters of a pod in addition to those generated from DNSPolicy.",
    "name": "dnsConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Set DNS policy for the pod. Defaults to \"ClusterFirst\". Valid values are 'ClusterFirstWithHostNet', 'ClusterFirst', 'Default' or 'None'. DNS parameters given in DNSConfig will be merged with the policy selected with DNSPolicy. To have DNS options set along with hostNetwork, you have to specify DNS policy explicitly to 'ClusterFirstWithHostNet'.",
    "name": "dnsPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "Host networking requested for this pod. Use the host's network namespace. When using HostNetwork you should specify ports so the scheduler is aware. When `hostNetwork` is true, specified `hostPort` fields in port definitions must match `containerPort`, and unspecified `hostPort` fields in port definitions are defaulted to match `containerPort`. Default to false.",
    "name": "hostNetwork",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodslivenessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "livenessProbe",
    "required": false,
    "type": "object"
  },
  {
    "description": "managementPolicy controls how pods are created during initial scale up, when replacing pods\n  on nodes, or when scaling down. The default policy is `OrderedReady`, where pods are created\n  in increasing order (pod-0, then pod-1, etc) and the controller will wait until each pod is\n  ready before continuing. When scaling down, the pods are removed in the opposite order.\n  The alternative policy is `Parallel` which will create pods in parallel to match the desired\n  scale without waiting, and on scale down will delete all pods at once.",
    "name": "managementPolicy",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodspersistentvolume",
    "description": "Pod's persistent volume configuration.",
    "name": "persistentVolume",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsreadinessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "readinessProbe",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsresources",
    "description": "Pod custom resources configuration.",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsscheduling",
    "description": "Pod custom scheduling, affinity and topology spread constratins configuration.\n\n**Changing this field may require a restart.**",
    "name": "scheduling",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "If true the pod's hostname will be configured as the pod's FQDN, rather than the leaf name (the default). In Linux containers, this means setting the FQDN in the hostname field of the kernel (the nodename field of struct utsname). In Windows containers, this means setting the registry value of hostname for the registry key HKEY_LOCAL_MACHINE\\\\SYSTEM\\\\CurrentControlSet\\\\Services\\\\Tcpip\\\\Parameters to FQDN. If a pod does not have FQDN, this has no effect. Default to false.",
    "name": "setHostnameAsFQDN",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsstartupprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "startupProbe",
    "required": false,
    "type": "object"
  },
  {
    "description": "serviceName is the name of the service that governs this StatefulSet. This service must exist before the StatefulSet, and is responsible for the network identity of the set. Pods get DNS/hostnames that follow the pattern: pod-specific-string.serviceName.default.svc.cluster.local where \"pod-specific-string\" is managed by the StatefulSet controller.",
    "name": "statefulSetServiceName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully. May be decreased in delete request. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). If this value is nil, the default grace period will be used instead. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. Defaults to 30 seconds.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsupdatestrategy",
    "default": {
      "type": "OnlyDbOps"
    },
    "description": "This section indicates the strategy that the SGCluster controller will use to perform updates.\n\nIt includes any additional parameters necessary to perform the update for the indicated strategy.\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.",
    "name": "updateStrategy",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.dnsConfig
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpods)</sup></sup>

PodDNSConfig defines the DNS parameters of a pod in addition to those generated from DNSPolicy.

{{< crd-properties >}}
[
  {
    "description": "A list of DNS name server IP addresses. This will be appended to the base nameservers generated from DNSPolicy. Duplicated nameservers will be removed.",
    "name": "nameservers",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsdnsconfigoptionsindex",
    "description": "A list of DNS resolver options. This will be merged with the base options generated from DNSPolicy. Duplicated entries will be removed. Resolution options given in Options will override those that appear in the base DNSPolicy.",
    "name": "options",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of DNS search domains for host-name lookup. This will be appended to the base search paths generated from DNSPolicy. Duplicated search paths will be removed.",
    "name": "searches",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.dnsConfig.options[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpodsdnsconfig)</sup></sup>

PodDNSConfigOption defines DNS resolver options of a pod.

{{< crd-properties >}}
[
  {
    "description": "Name is this DNS resolver option's name. Required.",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "Value is this DNS resolver option's value.",
    "name": "value",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.livenessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.persistentVolume
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpods)</sup></sup>

Pod's persistent volume configuration.

{{< crd-properties >}}
[
  {
    "description": "Size of the PersistentVolume set for each instance of the cluster. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).",
    "name": "size",
    "required": true,
    "type": "string"
  },
  {
    "description": "fsGroupChangePolicy defines behavior of changing ownership and permission of the volume before being exposed inside Pod. This field will only apply to volume types which support fsGroup based ownership(and permissions). It will have no effect on ephemeral volume types such as: secret, configmaps and emptydir. Valid values are \"OnRootMismatch\" and \"Always\". If not specified, \"Always\" is used. Note that this field cannot be set when spec.os.name is windows.",
    "name": "fsGroupChangePolicy",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodspersistentvolumeiolimits",
    "description": "This section allows to define per-volume I/O limits for nodes using [cgroup v2 `io.max` features](https://docs.kernel.org/admin-guide/cgroup-v2.html).\n\n\u003e **NOTE**: This is an **alpha** feature.\n\nWhen multiple PostgreSQL clusters share the same local storage device on a Kubernetes node, a single cluster running a heavy workload\n (for example large COPY, aggressive VACUUM, or an unoptimized query) can saturate the device's I/O bandwidth and degrade performance\n for every other cluster on that node. StackGres provides this mechanism to prevent this class of noisy-neighbor problem.\nThe mechanism operates at the cgroup + block-device layer, not at the PVC abstraction. Since each pod has its own cgroup, limits are\n enforced independently across pods even when they share the same physical device — this is the core of the noisy-neighbor protection.\nHowever, if multiple volumes within the same pod share the same backing device, their limits target a single entry in that pod's cgroup\n and cannot be enforced independently of each other.\n\nThis feature works on top of existing CSI drivers such as TopoLVM or OpenEBS LocalPV. The primary target is on-premises deployments where\n multiple StackGres clusters share local NVMe storage on the same node.\n\n\u003e **IMPORTANT**: Setting any of the value above will require to create an init container named `setup-io-limits` in each SGCluster's Pods\n\u003e  running as root with only the `CHOWN` cabability set. Also both the `setup-io-limits` init container and the `cluster-controller`\n\u003e  container will require to mount the host path `/sys/fs/cgroup` in order to be able to set owner of and write to the `io.max` file that\n\u003e  will allows to set the io limits.\n\u003e  For this reason this feature must be enabled globally by the operator administrator by adding the `io-limits` feature gate under\n\u003e  `.spec.featureGates` of the SGConfig. Setting any of the value above without the `io-limits` feature gate enabled will be rejected.",
    "name": "ioLimits",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.",
    "name": "storageClass",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.\n\nSee https://kubernetes.io/docs/concepts/storage/volume-attributes-classes/",
    "name": "volumeAttributesClassName",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.persistentVolume.ioLimits
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpodspersistentvolume)</sup></sup>

This section allows to define per-volume I/O limits for nodes using [cgroup v2 `io.max` features](https://docs.kernel.org/admin-guide/cgroup-v2.html).

> **NOTE**: This is an **alpha** feature.

When multiple PostgreSQL clusters share the same local storage device on a Kubernetes node, a single cluster running a heavy workload
 (for example large COPY, aggressive VACUUM, or an unoptimized query) can saturate the device's I/O bandwidth and degrade performance
 for every other cluster on that node. StackGres provides this mechanism to prevent this class of noisy-neighbor problem.
The mechanism operates at the cgroup + block-device layer, not at the PVC abstraction. Since each pod has its own cgroup, limits are
 enforced independently across pods even when they share the same physical device — this is the core of the noisy-neighbor protection.
However, if multiple volumes within the same pod share the same backing device, their limits target a single entry in that pod's cgroup
 and cannot be enforced independently of each other.

This feature works on top of existing CSI drivers such as TopoLVM or OpenEBS LocalPV. The primary target is on-premises deployments where
 multiple StackGres clusters share local NVMe storage on the same node.

> **IMPORTANT**: Setting any of the value above will require to create an init container named `setup-io-limits` in each SGCluster's Pods
>  running as root with only the `CHOWN` cabability set. Also both the `setup-io-limits` init container and the `cluster-controller`
>  container will require to mount the host path `/sys/fs/cgroup` in order to be able to set owner of and write to the `io.max` file that
>  will allows to set the io limits.
>  For this reason this feature must be enabled globally by the operator administrator by adding the `io-limits` feature gate under
>  `.spec.featureGates` of the SGConfig. Setting any of the value above without the `io-limits` feature gate enabled will be rejected.


{{< crd-properties >}}
[
  {
    "description": "Max read IO operations per second. Leave empty to remove this limit.",
    "name": "readIops",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max read MiB per second. Leave empty to remove this limit.",
    "name": "readMiBps",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max write IO operations per second. Leave empty to remove this limit.",
    "name": "writeIops",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Max write MiB per second. Leave empty to remove this limit.",
    "name": "writeMiBps",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.readinessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.resources
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpods)</sup></sup>

Pod custom resources configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsresourcescontainerskey",
    "description": "Resources configuration to be merged with the specified container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "containers",
    "required": false,
    "type": "map[string]object"
  },
  {
    "description": "When set to `true` the resources requests values in fields `SGInstanceProfile.spec.requests.cpu` and `SGInstanceProfile.spec.requests.memory` will represent the resources\n requests of the patroni container and the total resources requests calculated by adding the resources requests of all the containers (including the patroni container).\n\n**Changing this field may require a restart.**",
    "name": "disableResourcesRequestsSplitFromTotal",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When enabled resource limits for containers other than the patroni container will be set just like for patroni container as specified in the SGInstanceProfile.\n\n**Changing this field may require a restart.**",
    "name": "enableClusterLimitsRequirements",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When set to `true` the reconciliation of the cluster will fail if `disableResourcesRequestsSplitFromTotal` is not set or set to `false` and the sum of the CPU or memory\n of all the containers except patroni is equals or higher than the total specified in `SGInstanceProfile.spec.requests.cpu` or `SGInstanceProfile.spec.requests.memory`.\n\nWhen `false` (the default) and `disableResourcesRequestsSplitFromTotal` is not set or set to `false` and the sum of the CPU or memory\n of all the containers except patroni is equals or higher than the total specified in `SGInstanceProfile.spec.requests.cpu` or `SGInstanceProfile.spec.requests.memory`\n then the patroni container resources will be set to 0.",
    "name": "failWhenTotalIsHigher",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsresourcesinitcontainerskey",
    "description": "Resources configuration to be merged with the specified init container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "initContainers",
    "required": false,
    "type": "map[string]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.resources.containers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsresourcescontainerskeyclaimsindex",
    "description": "Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.\n\nThis field depends on the DynamicResourceAllocation feature gate.\n\nThis field is immutable. It can only be set for containers.",
    "name": "claims",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "limits",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "requests",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.resources.containers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpodsresourcescontainerskey)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

{{< crd-properties >}}
[
  {
    "description": "Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.",
    "name": "request",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.resources.initContainers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsresourcesinitcontainerskeyclaimsindex",
    "description": "Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.\n\nThis field depends on the DynamicResourceAllocation feature gate.\n\nThis field is immutable. It can only be set for containers.",
    "name": "claims",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "limits",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/",
    "name": "requests",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.resources.initContainers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpodsresourcesinitcontainerskey)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

{{< crd-properties >}}
[
  {
    "description": "Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.",
    "name": "request",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.scheduling
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpods)</sup></sup>

Pod custom scheduling, affinity and topology spread constratins configuration.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsschedulingbackup",
    "description": "Backup Pod custom scheduling and affinity configuration.",
    "name": "backup",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core",
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
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podantiaffinity-v1-core",
    "name": "podAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "PreemptionPolicy is the Policy for preempting pods with lower priority. One of Never, PreemptLowerPriority. Defaults to PreemptLowerPriority if unset.",
    "name": "preemptionPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.",
    "name": "priorityClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "RuntimeClassName refers to a RuntimeClass object in the node.k8s.io group, which should be used to run this pod.  If no RuntimeClass resource matches the named class, the pod will not be run. If unset or empty, the \"legacy\" RuntimeClass will be used, which is an implicit class with an empty definition that uses the default runtime handler. More info: https://git.k8s.io/enhancements/keps/sig-node/585-runtime-class",
    "name": "runtimeClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod will be dispatched by specified scheduler. If not specified, the pod will be dispatched by default scheduler.",
    "name": "schedulerName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#topologyspreadconstraint-v1-core",
    "name": "topologySpreadConstraints",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.scheduling.backup
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpodsscheduling)</sup></sup>

Backup Pod custom scheduling and affinity configuration.

{{< crd-properties >}}
[
  {
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core",
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
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podantiaffinity-v1-core",
    "name": "podAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "PreemptionPolicy is the Policy for preempting pods with lower priority. One of Never, PreemptLowerPriority. Defaults to PreemptLowerPriority if unset.",
    "name": "preemptionPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.",
    "name": "priorityClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "RuntimeClassName refers to a RuntimeClass object in the node.k8s.io group, which should be used to run this pod.  If no RuntimeClass resource matches the named class, the pod will not be run. If unset or empty, the \"legacy\" RuntimeClass will be used, which is an implicit class with an empty definition that uses the default runtime handler. More info: https://git.k8s.io/enhancements/keps/sig-node/585-runtime-class",
    "name": "runtimeClassName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod will be dispatched by specified scheduler. If not specified, the pod will be dispatched by default scheduler.",
    "name": "schedulerName",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.startupProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpods)</sup></sup>

Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.

{{< crd-properties >}}
[
  {
    "description": "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1.",
    "format": "int32",
    "name": "failureThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after the container has started before liveness probes are initiated. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "initialDelaySeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.",
    "format": "int32",
    "name": "periodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness and startup. Minimum value is 1.",
    "format": "int32",
    "name": "successThreshold",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully upon probe failure. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. If this value is nil, the pod's terminationGracePeriodSeconds will be used. Otherwise, this value overrides the value provided by the pod spec. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). This is a beta field and requires enabling ProbeTerminationGracePeriod feature gate. Minimum value is 1. spec.terminationGracePeriodSeconds is used if unset.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes",
    "format": "int32",
    "name": "timeoutSeconds",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.updateStrategy
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpods)</sup></sup>

This section indicates the strategy that the SGCluster controller will use to perform updates.

It includes any additional parameters necessary to perform the update for the indicated strategy.

When a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of
 the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the
 primary is demoted instead of being restarted.

The Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters
 `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is
 decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,
 see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover
 to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is
 performed.


{{< crd-properties >}}
[
  {
    "default": "InPlace",
    "description": "Indicates the method of the update strategy. Default is `InPlace`.\n\n* `InPlace`: update will be performed on the existing instances.\n* `ReducedImpact`: before an update is performed on the existing instances a new instance if created to reduce the impact on read-only replicas.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "default": "PT5M",
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS` indicating the delay that has to pass between the restart of a\n Postgres instance performed by the SGCluster controller and the next one. Default is `PT5M`.",
    "name": "restartDelay",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexpodsupdatestrategyscheduleindex",
    "description": "Indicates a list of windows of time where an update can be performed.",
    "name": "schedule",
    "required": false,
    "type": "[]object"
  },
  {
    "default": "OnlyDbOps",
    "description": "Indicates the type of the update strategy. Default is `OnlyDbOps`.\n\n* `Always`: update will be performed as soon as possible.\n* `Schedule`: update will be performed as specified in the schedule section where you can configure windows of time where the update can be performed.\n* `OnlyDbOps`: update will be performed only when an SGDbOps of type restart, securityUpgrade or minorVersionUpgrade targeting the SGCluster is started.\n* `Never`: update will never be performed (even if annotation `stackgres.io/rollout` is present with a different value) unless the Pods are deleted manually.\n\nAlternatively to this configuration you may specify the following annotations:\n\n* `stackgres.io/rollout=always`: Same behavior as `Always`.\n* `stackgres.io/rollout=schedule`: Same behavior as `Schedule`.\n* `stackgres.io/rollout=never`: Same behavior as `Never` (even if type has a different value).\n\nUnless `Never` (or the annotation `stackgres.io/rollout=never`) is specified an `SGDbOps` of type restart, securityUpgrade or minorVersionUpgrade targeting the SGCluster\n will trigger an update of the Pods.\n\nThe update of the Pods will be performed only if Pods are pending any change. Changes in the configuration may not require restarting the Pod and, in such cases, only\n the Postgres instance will be restarted.",
    "name": "type",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].pods.updateStrategy.schedule[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexpodsupdatestrategy)</sup></sup>

Indicates a window of time where an update can be performed.

{{< crd-properties >}}
[
  {
    "description": "A UNIX cron expression indicating the start of the window of time where the update can be performed.",
    "name": "cron",
    "required": false,
    "type": "string"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that, together with the cron expression, indicates the end of the window of time where the update can be performed.",
    "name": "duration",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].replication
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindex)</sup></sup>

This section allows to configure the global Postgres replication mode.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexreplicationinitialization",
    "description": "Allows specifying how the replicas are initialized.",
    "name": "initialization",
    "required": false,
    "type": "object"
  },
  {
    "default": "async",
    "description": "The replication mode applied to the whole cluster.\nPossible values are:\n* `async` (default)\n* `sync`\n* `strict-sync`\n* `sync-all`\n* `strict-sync-all`\n\n**async**\n\nWhen in asynchronous mode the cluster is allowed to lose some committed transactions.\n  When the primary server fails or becomes unavailable for any other reason a sufficiently healthy standby\n  will automatically be promoted to primary. Any transactions that have not been replicated to that standby\n  remain in a \"forked timeline\" on the primary, and are effectively unrecoverable (the data is still there,\n  but recovering it requires a manual recovery effort by data recovery specialists).\n\n**sync**\n\nWhen in synchronous mode a standby will not be promoted unless it is certain that the standby contains all\n  transactions that may have returned a successful commit status to client (clients can change the behavior\n  per transaction using PostgreSQL’s `synchronous_commit` setting. Transactions with `synchronous_commit`\n  values of `off` and `local` may be lost on fail over, but will not be blocked by replication delays). This\n  means that the system may be unavailable for writes even though some servers are available. System\n  administrators can still use manual failover commands to promote a standby even if it results in transaction\n  loss.\n\nSynchronous mode does not guarantee multi node durability of commits under all circumstances. When no suitable\n  standby is available, primary server will still accept writes, but does not guarantee their replication. When\n  the primary fails in this mode no standby will be promoted. When the host that used to be the primary comes\n  back it will get promoted automatically, unless system administrator performed a manual failover. This behavior\n  makes synchronous mode usable with 2 node clusters.\n\nWhen synchronous mode is used and a standby crashes, commits will block until the primary is switched to standalone\n  mode. Manually shutting down or restarting a standby will not cause a commit service interruption. Standby will\n  signal the primary to release itself from synchronous standby duties before PostgreSQL shutdown is initiated.\n\n**strict-sync**\n\nWhen it is absolutely necessary to guarantee that each write is stored durably on at least two nodes, use the strict\n  synchronous mode. This mode prevents synchronous replication to be switched off on the primary when no synchronous\n  standby candidates are available. As a downside, the primary will not be available for writes (unless the Postgres\n  transaction explicitly turns off `synchronous_mode` parameter), blocking all client write requests until at least one\n  synchronous replica comes up.\n\n**Note**: Because of the way synchronous replication is implemented in PostgreSQL it is still possible to lose\n  transactions even when using strict synchronous mode. If the PostgreSQL backend is cancelled while waiting to acknowledge\n  replication (as a result of packet cancellation due to client timeout or backend failure) transaction changes become\n  visible for other backends. Such changes are not yet replicated and may be lost in case of standby promotion.\n\n**sync-all**\n\nThe same as `sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number\n  of instances less one.\n\n**strict-sync-all**\n\nThe same as `strict-sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number\n  of instances less one.",
    "name": "mode",
    "required": false,
    "type": "string"
  },
  {
    "description": "Number of synchronous standby instances. Must be less than the total number of instances. It is set to 1 by default.\n  Only setteable if mode is `sync` or `strict-sync`.",
    "minimum": 1,
    "name": "syncInstances",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].replication.initialization
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexreplication)</sup></sup>

Allows specifying how the replicas are initialized.


{{< crd-properties >}}
[
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how old an SGBackup have to be in order to be seleceted\n to initialize a replica.\n\nWhen `FromExistingBackup` mode is set this field restrict the selection of SGBackup to be used for recovery newer than the\n specified value. \n\nWhen `FromNewlyCreatedBackup` mode is set this field skip the creation SGBackup to be used for recovery if one newer than\n the specified value exists.",
    "name": "backupNewerThan",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkersoverridesindexreplicationinitializationbackuprestoreperformance",
    "description": "Configuration that affects the backup network and disk usage performance during recovery.",
    "name": "backupRestorePerformance",
    "required": false,
    "type": "object"
  },
  {
    "default": "FromExistingBackup",
    "description": "Allows specifying how the replicas are initialized.\n\nPossible values are:\n\n* `FromPrimary`: When this mode is used replicas will be always created from the primary using `pg_basebackup`.\n* `FromReplica`: When this mode is used replicas will be created from another existing replica using\n `pg_basebackup`. Fallsback to `FromPrimary` if there's no replica or it fails.\n* `FromExistingBackup`: When this mode is used replicas will be created from an existing SGBackup. If `backupNewerThan` is set\n the SGBackup must be newer than its value. When this mode fails to restore an SGBackup it will try with a previous one (if exists).\n Fallsback to `FromReplica` if there's no backup left or it fails.\n* `FromNewlyCreatedBackup`: When this mode is used replicas will be created from a newly created SGBackup.\n Fallsback to `FromExistingBackup` if `backupNewerThan` is set and exists a recent backup newer than its value or it fails.",
    "name": "mode",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.overrides[index].replication.initialization.backupRestorePerformance
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersoverridesindexreplicationinitialization)</sup></sup>

Configuration that affects the backup network and disk usage performance during recovery.


{{< crd-properties >}}
[
  {
    "description": "Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of file to read and 10.",
    "minimum": 1,
    "name": "downloadConcurrency",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum disk read I/O when performing a backup. In bytes (per second).",
    "name": "maxDiskBandwidth",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum storage upload bandwidth used when storing a backup. In bytes (per second).",
    "name": "maxNetworkBandwidth",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.spec.workers.replication
<sup><sup>[↩ Parent](#sgshardedclusterspecworkers)</sup></sup>

This section allows to configure the global Postgres replication mode.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecworkersreplicationinitialization",
    "description": "Allows specifying how the replicas are initialized.",
    "name": "initialization",
    "required": false,
    "type": "object"
  },
  {
    "default": "async",
    "description": "The replication mode applied to the whole cluster.\nPossible values are:\n* `async` (default)\n* `sync`\n* `strict-sync`\n* `sync-all`\n* `strict-sync-all`\n\n**async**\n\nWhen in asynchronous mode the cluster is allowed to lose some committed transactions.\n  When the primary server fails or becomes unavailable for any other reason a sufficiently healthy standby\n  will automatically be promoted to primary. Any transactions that have not been replicated to that standby\n  remain in a \"forked timeline\" on the primary, and are effectively unrecoverable (the data is still there,\n  but recovering it requires a manual recovery effort by data recovery specialists).\n\n**sync**\n\nWhen in synchronous mode a standby will not be promoted unless it is certain that the standby contains all\n  transactions that may have returned a successful commit status to client (clients can change the behavior\n  per transaction using PostgreSQL’s `synchronous_commit` setting. Transactions with `synchronous_commit`\n  values of `off` and `local` may be lost on fail over, but will not be blocked by replication delays). This\n  means that the system may be unavailable for writes even though some servers are available. System\n  administrators can still use manual failover commands to promote a standby even if it results in transaction\n  loss.\n\nSynchronous mode does not guarantee multi node durability of commits under all circumstances. When no suitable\n  standby is available, primary server will still accept writes, but does not guarantee their replication. When\n  the primary fails in this mode no standby will be promoted. When the host that used to be the primary comes\n  back it will get promoted automatically, unless system administrator performed a manual failover. This behavior\n  makes synchronous mode usable with 2 node clusters.\n\nWhen synchronous mode is used and a standby crashes, commits will block until the primary is switched to standalone\n  mode. Manually shutting down or restarting a standby will not cause a commit service interruption. Standby will\n  signal the primary to release itself from synchronous standby duties before PostgreSQL shutdown is initiated.\n\n**strict-sync**\n\nWhen it is absolutely necessary to guarantee that each write is stored durably on at least two nodes, use the strict\n  synchronous mode. This mode prevents synchronous replication to be switched off on the primary when no synchronous\n  standby candidates are available. As a downside, the primary will not be available for writes (unless the Postgres\n  transaction explicitly turns off `synchronous_mode` parameter), blocking all client write requests until at least one\n  synchronous replica comes up.\n\n**Note**: Because of the way synchronous replication is implemented in PostgreSQL it is still possible to lose\n  transactions even when using strict synchronous mode. If the PostgreSQL backend is cancelled while waiting to acknowledge\n  replication (as a result of packet cancellation due to client timeout or backend failure) transaction changes become\n  visible for other backends. Such changes are not yet replicated and may be lost in case of standby promotion.\n\n**sync-all**\n\nThe same as `sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number\n  of instances less one.\n\n**strict-sync-all**\n\nThe same as `strict-sync` but `syncInstances` is ignored and the number of synchronous instances is equals to the total number\n  of instances less one.",
    "name": "mode",
    "required": false,
    "type": "string"
  },
  {
    "description": "Number of synchronous standby instances. Must be less than the total number of instances. It is set to 1 by default.\n  Only setteable if mode is `sync` or `strict-sync`.",
    "minimum": 1,
    "name": "syncInstances",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.replication.initialization
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersreplication)</sup></sup>

Allows specifying how the replicas are initialized.


{{< crd-properties >}}
[
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how old an SGBackup have to be in order to be seleceted\n to initialize a replica.\n\nWhen `FromExistingBackup` mode is set this field restrict the selection of SGBackup to be used for recovery newer than the\n specified value. \n\nWhen `FromNewlyCreatedBackup` mode is set this field skip the creation SGBackup to be used for recovery if one newer than\n the specified value exists.",
    "name": "backupNewerThan",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecworkersreplicationinitializationbackuprestoreperformance",
    "description": "Configuration that affects the backup network and disk usage performance during recovery.",
    "name": "backupRestorePerformance",
    "required": false,
    "type": "object"
  },
  {
    "default": "FromExistingBackup",
    "description": "Allows specifying how the replicas are initialized.\n\nPossible values are:\n\n* `FromPrimary`: When this mode is used replicas will be always created from the primary using `pg_basebackup`.\n* `FromReplica`: When this mode is used replicas will be created from another existing replica using\n `pg_basebackup`. Fallsback to `FromPrimary` if there's no replica or it fails.\n* `FromExistingBackup`: When this mode is used replicas will be created from an existing SGBackup. If `backupNewerThan` is set\n the SGBackup must be newer than its value. When this mode fails to restore an SGBackup it will try with a previous one (if exists).\n Fallsback to `FromReplica` if there's no backup left or it fails.\n* `FromNewlyCreatedBackup`: When this mode is used replicas will be created from a newly created SGBackup.\n Fallsback to `FromExistingBackup` if `backupNewerThan` is set and exists a recent backup newer than its value or it fails.",
    "name": "mode",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.workers.replication.initialization.backupRestorePerformance
<sup><sup>[↩ Parent](#sgshardedclusterspecworkersreplicationinitialization)</sup></sup>

Configuration that affects the backup network and disk usage performance during recovery.


{{< crd-properties >}}
[
  {
    "description": "Backup storage may use several concurrent streams to read the data. This parameter configures the number of parallel streams to use. By default, it's set to the minimum between the number of file to read and 10.",
    "minimum": 1,
    "name": "downloadConcurrency",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum disk read I/O when performing a backup. In bytes (per second).",
    "name": "maxDiskBandwidth",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Maximum storage upload bandwidth used when storing a backup. In bytes (per second).",
    "name": "maxNetworkBandwidth",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

## SGShardedCluster.status
<sup><sup>[↩ Parent](#sgshardedcluster)</sup></sup>

Current status of a StackGres sharded cluster.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterstatusbinding",
    "description": "This section follow the schema specified in [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).\n\nFor more information see https://servicebinding.io/spec/core/1.0.0/",
    "name": "binding",
    "required": false,
    "type": "object"
  },
  {
    "description": "The Postgres build version used to generate the SGShardedCluster's SGClusters",
    "name": "buildVersion",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterstatusclusterstatusesindex",
    "description": "The list of cluster statuses.",
    "name": "clusterStatuses",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterstatusconditionsindex",
    "description": "",
    "name": "conditions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterstatusdbops",
    "description": "Used by some [SGShardedDbOps](https://stackgres.io/doc/latest/reference/crd/sgshardeddbops) to indicate the operation configuration and status to the operator.",
    "name": "dbOps",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterstatusextensionsindex",
    "description": "The list of Postgres extensions to install.\n\n**This section is filled by the operator.**",
    "name": "extensions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "The Postgres version used to generate the SGShardedCluster's SGClusters",
    "name": "postgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "The list of SGBackups that compose the SGShardedBackup used to restore the sharded cluster.",
    "name": "sgBackups",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardedclusterstatustoinstallpostgresextensionsindex",
    "description": "**Deprecated** replaced by `extensions`.\n\nThe list of Postgres extensions to install",
    "name": "toInstallPostgresExtensions",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.status.binding
<sup><sup>[↩ Parent](#sgshardedclusterstatus)</sup></sup>

This section follow the schema specified in [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).

For more information see https://servicebinding.io/spec/core/1.0.0/


{{< crd-properties >}}
[
  {
    "description": "The name of the Secret as specified in [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.status.clusterStatuses[index]
<sup><sup>[↩ Parent](#sgshardedclusterstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The name of the cluster.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "Indicates if the cluster requires restart",
    "name": "pendingRestart",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.status.conditions[index]
<sup><sup>[↩ Parent](#sgshardedclusterstatus)</sup></sup>



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
    "description": "The .metadata.generation the condition was computed against.",
    "format": "int64",
    "name": "observedGeneration",
    "required": false,
    "type": "integer"
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

### SGShardedCluster.status.dbOps
<sup><sup>[↩ Parent](#sgshardedclusterstatus)</sup></sup>

Used by some [SGShardedDbOps](https://stackgres.io/doc/latest/reference/crd/sgshardeddbops) to indicate the operation configuration and status to the operator.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterstatusdbopsmajorversionupgrade",
    "description": "The major version upgrade configuration and status",
    "name": "majorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterstatusdbopsminorversionupgrade",
    "description": "The minor version upgrade configuration and status",
    "name": "minorVersionUpgrade",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.status.dbOps.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgshardedclusterstatusdbops)</sup></sup>

The major version upgrade configuration and status


{{< crd-properties >}}
[
  {
    "description": "The list of SGDbOps created by the SGShardedDbOps to perform the major version upgrade.\n\nThe entries follow the same ordering convention as `SGShardedBackup.status.sgBackups`: the\nfirst entry targets the coordinator, followed by the workers and then the query routers.\n\nWhile this section is present the operator stops reconciling the child SGClusters so that\nthe SGDbOps are in full control of the major version upgrade of each SGCluster.",
    "name": "sgDbOps",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The source PostgreSQL version",
    "name": "sourcePostgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "The SGPostgresConfig used before the major version upgrade",
    "name": "sourceSgPostgresConfig",
    "required": false,
    "type": "string"
  },
  {
    "description": "The target PostgreSQL version",
    "name": "targetPostgresVersion",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedCluster.status.dbOps.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgshardedclusterstatusdbops)</sup></sup>

The minor version upgrade configuration and status


{{< crd-properties >}}
[
  {
    "description": "Postgres version that is currently running on the cluster",
    "name": "sourcePostgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "The desired Postgres version for the cluster",
    "name": "targetPostgresVersion",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.status.extensions[index]
<sup><sup>[↩ Parent](#sgshardedclusterstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The name of the extension to install.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "The postgres major version of the extension to install.",
    "name": "postgresVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "The id of the publisher of the extension to install.",
    "name": "publisher",
    "required": true,
    "type": "string"
  },
  {
    "description": "The repository base URL from where the extension will be installed from.",
    "name": "repository",
    "required": true,
    "type": "string"
  },
  {
    "description": "The version of the extension to install.",
    "name": "version",
    "required": true,
    "type": "string"
  },
  {
    "description": "The build version of the extension to install.",
    "name": "build",
    "required": false,
    "type": "string"
  },
  {
    "description": "The extra mounts of the extension to install.",
    "name": "extraMounts",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.status.toInstallPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgshardedclusterstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The name of the extension to install.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "The postgres major version of the extension to install.",
    "name": "postgresVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "The id of the publisher of the extension to install.",
    "name": "publisher",
    "required": true,
    "type": "string"
  },
  {
    "description": "The repository base URL from where the extension will be installed from.",
    "name": "repository",
    "required": true,
    "type": "string"
  },
  {
    "description": "The version of the extension to install.",
    "name": "version",
    "required": true,
    "type": "string"
  },
  {
    "description": "The build version of the extension to install.",
    "name": "build",
    "required": false,
    "type": "string"
  },
  {
    "description": "The extra mounts of the extension to install.",
    "name": "extraMounts",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}
