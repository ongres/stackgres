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
  shards:
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
    "description": "stackgres.io/v1alpha1",
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
    "description": "The coordinator is a StackGres cluster responsible for coordinating data storage and access from the shards.",
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
    "anchor": "sgshardedclusterspecshards",
    "description": "The shards are a group of StackGres clusters where the partitioned data chunks are stored.\n\nWhen referring to the cluster in the descriptions below it applies to any shard's StackGres cluster.",
    "name": "shards",
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
    "description": "The sharding technology that will be used for the sharded cluster.\n\nAvailable technologies are:\n\n* `citus`\n* `ddp`\n* `shardingsphere`\n\n**Citus**\n\nCitus is a PostgreSQL extension that transforms Postgres into a distributed database—so you can achieve high performance at any scale.\n\nSee also https://github.com/citusdata/citus\n\n**DDP**\n\nDDP (Distributed Data Partitioning) allows you to distribute data across different physical nodes to improve the query performance of high data volumes, taking advantage of distinct nodes’ resources. Using the entry point named coordinator in charge of sending/distributing the queries to different nodes named shards.\n\n**ShardingSphere**\n\nApache ShardingSphere is an ecosystem to transform any database into a distributed database system, and enhance it with sharding, elastic scaling, encryption features \u0026 more.\n\nStackGres implementation of ShardingSphere as a sharding technology uses the [ShardingSphere Proxy](https://shardingsphere.apache.org/document/current/en/quick-start/shardingsphere-proxy-quick-start/) as an entry point to distribute SQL traffic among the shards.\n\nThis implementation requires the [ShardingSphere Operator](https://shardingsphere.apache.org/oncloud/current/en/user-manual/cn-sn-operator/) to be installed and will create a ComputeNode",
    "name": "type",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedCluster.spec.coordinator
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

The coordinator is a StackGres cluster responsible for coordinating data storage and access from the shards.


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
    "anchor": "sgshardedclusterspeccoordinatorconfigurations",
    "description": "Coordinator custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatormanagedsql",
    "description": "This section allows referencing SQL scripts that will be applied to the cluster live.\n\nIf sharding type is `shardingsphere` then this section will be applied to the first cluster shard.\n In this case the database postgres will also provide a foreign server called `shardingsphere` and\n the superuser user mappings that will allow running DistQL queries using command like the following:\n\n\n```\n\nSELECT * FROM dblink('shardingsphere', 'SHOW STORAGE UNITS')\n  AS _(name text, type text, host text, port int, db text,\n    connection_timeout_milliseconds int, idle_timeout_milliseconds int,\n    max_lifetime_milliseconds int, max_pool_size int, min_pool_size int,\n    read_only boolean, other_attributes text);\n\n```\n\n\nSee https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-proxy/distsql",
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
    "description": "A list of custom application containers that run within the coordinator cluster's Pods.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customContainers",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom application init containers that run within the shards cluster's Pods. The\n  custom init containers will run following the defined sequence at the end of\n  cluster's Pods init containers.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volumes that may be used along with any container defined in\n  customInitContainers or customContainers sections for the coordinator.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the customInitContainers or customContainers sections the name used\n  has to be prepended with the same prefix.\n\nOnly the following volume types are allowed: configMap, downwardAPI, emptyDir,\n  gitRepo, glusterfs, hostPath, nfs, projected and secret\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#volume-v1-core",
    "name": "customVolumes",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating a connection pooling (using [PgBouncer](https://www.pgbouncer.org/)) sidecar.\n\nIf sharding type is `shardingsphere` then this field is ignored.\n\n**Changing this field may require a restart.**",
    "name": "disableConnectionPooling",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If set to `false`, creates the `envoy` sidecar. This sidecar is used as the edge proxy for the cluster's Pods providing extra metrics to the monitoring layer.\n\n**Changing this field may require a restart.**",
    "name": "disableEnvoy",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** use instead .spec.configurations.observability.disableMetrics.",
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
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.\n\nIf sharding type is `shardingsphere` then this field is ignored.",
    "name": "storageClass",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinity",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinity",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingtolerationsindex",
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingtopologyspreadconstraintsindex",
    "description": "TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#topologyspreadconstraint-v1-core",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinity",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "nodeAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselector",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "nodeSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinity",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerations",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselector)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselector)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackup)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackup)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuptolerations)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuptolerations)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsscheduling)</sup></sup>

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

##### SGShardedCluster.spec.coordinator.pods.scheduling.topologySpreadConstraints[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsscheduling)</sup></sup>

TopologySpreadConstraint specifies how to spread matching pods among the given topology.

{{< crd-properties >}}
[
  {
    "description": "MaxSkew describes the degree to which pods may be unevenly distributed. When `whenUnsatisfiable=DoNotSchedule`, it is the maximum permitted difference between the number of matching pods in the target topology and the global minimum. The global minimum is the minimum number of matching pods in an eligible domain or zero if the number of eligible domains is less than MinDomains. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 2/2/1: In this case, the global minimum is 1. | zone1 | zone2 | zone3 | |  P P  |  P P  |   P   | - if MaxSkew is 1, incoming pod can only be scheduled to zone3 to become 2/2/2; scheduling it onto zone1(zone2) would make the ActualSkew(3-1) on zone1(zone2) violate MaxSkew(1). - if MaxSkew is 2, incoming pod can be scheduled onto any zone. When `whenUnsatisfiable=ScheduleAnyway`, it is used to give higher precedence to topologies that satisfy it. It's a required field. Default value is 1 and 0 is not allowed.",
    "format": "int32",
    "name": "maxSkew",
    "requireRestart": true,
    "required": true,
    "type": "integer"
  },
  {
    "description": "TopologyKey is the key of node labels. Nodes that have a label with this key and identical values are considered to be in the same topology. We consider each \u003ckey, value\u003e as a \"bucket\", and try to put balanced number of pods into each bucket. We define a domain as a particular instance of a topology. Also, we define an eligible domain as a domain whose nodes meet the requirements of nodeAffinityPolicy and nodeTaintsPolicy. e.g. If TopologyKey is \"kubernetes.io/hostname\", each Node is a domain of that topology. And, if TopologyKey is \"topology.kubernetes.io/zone\", each zone is a domain of that topology. It's a required field.",
    "name": "topologyKey",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "WhenUnsatisfiable indicates how to deal with a pod if it doesn't satisfy the spread constraint. - DoNotSchedule (default) tells the scheduler not to schedule it. - ScheduleAnyway tells the scheduler to schedule the pod in any location,\n  but giving higher precedence to topologies that would help reduce the\n  skew.\nA constraint is considered \"Unsatisfiable\" for an incoming pod if and only if every possible node assignment for that pod would violate \"MaxSkew\" on some topology. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 3/1/1: | zone1 | zone2 | zone3 | | P P P |   P   |   P   | If WhenUnsatisfiable is set to DoNotSchedule, incoming pod can only be scheduled to zone2(zone3) to become 3/2/1(3/1/2) as ActualSkew(2-1) on zone2(zone3) satisfies MaxSkew(1). In other words, the cluster can still be imbalanced, but scheduler won't make it *more* imbalanced. It's a required field.",
    "name": "whenUnsatisfiable",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingtopologyspreadconstraintsindexlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select the pods over which spreading will be calculated. The keys are used to lookup values from the incoming pod labels, those key-value labels are ANDed with labelSelector to select the group of existing pods over which spreading will be calculated for the incoming pod. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. MatchLabelKeys cannot be set when LabelSelector isn't set. Keys that don't exist in the incoming pod labels will be ignored. A null or empty list means only match against labelSelector.\n\nThis is a beta field and requires the MatchLabelKeysInPodTopologySpread feature gate to be enabled (enabled by default).",
    "name": "matchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MinDomains indicates a minimum number of eligible domains. When the number of eligible domains with matching topology keys is less than minDomains, Pod Topology Spread treats \"global minimum\" as 0, and then the calculation of Skew is performed. And when the number of eligible domains with matching topology keys equals or greater than minDomains, this value has no effect on scheduling. As a result, when the number of eligible domains is less than minDomains, scheduler won't schedule more than maxSkew Pods to those domains. If value is nil, the constraint behaves as if MinDomains is equal to 1. Valid values are integers greater than 0. When value is not nil, WhenUnsatisfiable must be DoNotSchedule.\n\nFor example, in a 3-zone cluster, MaxSkew is set to 2, MinDomains is set to 5 and pods with the same labelSelector spread as 2/2/2: | zone1 | zone2 | zone3 | |  P P  |  P P  |  P P  | The number of domains is less than 5(MinDomains), so \"global minimum\" is treated as 0. In this situation, new pod with the same labelSelector cannot be scheduled, because computed skew will be 3(3 - 0) if new Pod is scheduled to any of the three zones, it will violate MaxSkew.",
    "format": "int32",
    "name": "minDomains",
    "requireRestart": true,
    "required": false,
    "type": "integer"
  },
  {
    "description": "NodeAffinityPolicy indicates how we will treat Pod's nodeAffinity/nodeSelector when calculating pod topology spread skew. Options are: - Honor: only nodes matching nodeAffinity/nodeSelector are included in the calculations. - Ignore: nodeAffinity/nodeSelector are ignored. All nodes are included in the calculations.\n\nIf this value is nil, the behavior is equivalent to the Honor policy.",
    "name": "nodeAffinityPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "NodeTaintsPolicy indicates how we will treat node taints when calculating pod topology spread skew. Options are: - Honor: nodes without taints, along with tainted nodes for which the incoming pod has a toleration, are included. - Ignore: node taints are ignored. All nodes are included.\n\nIf this value is nil, the behavior is equivalent to the Ignore policy.",
    "name": "nodeTaintsPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.pods.scheduling.topologySpreadConstraints[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingtopologyspreadconstraintsindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspeccoordinatorpodsschedulingtopologyspreadconstraintsindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.coordinator.pods.scheduling.topologySpreadConstraints[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorpodsschedulingtopologyspreadconstraintsindexlabelselector)</sup></sup>

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
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthorityusersindexuser",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "user",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.coordinator.configurations.shardingSphere.authority.users[index].password
<sup><sup>[↩ Parent](#sgshardedclusterspeccoordinatorconfigurationsshardingsphereauthorityusersindex)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.


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

If sharding type is `shardingsphere` then this section will be applied to the first cluster shard.
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
    "description": "StackGres support deploy of extensions at runtime by simply adding an entry to this array. A deployed extension still\nrequires the creation in a database using the [`CREATE EXTENSION`](https://www.postgresql.org/docs/current/sql-createextension.html)\nstatement. After an extension is deployed correctly it will be present until removed and the cluster restarted.\n\nA cluster restart is required for:\n* Extensions that requires to add an entry to [`shared_preload_libraries`](https://postgresqlco.nf/en/doc/param/shared_preload_libraries/) configuration parameter.\n* Upgrading extensions that overwrite any file that is not the extension''s control file or extension''s script file.\n* Removing extensions. Until the cluster is not restarted a removed extension will still be available.\n* Install of extensions that require extra mount. After installed the cluster will require to be restarted.\n\n**Exmaple:**\n\n\n```\n yaml\napiVersion: stackgres.io/v1alpha1\nkind: SGShardedCluster\nmetadata:\n  name: stackgres\nspec:\n  postgres:\n    extensions:\n      - {name: 'timescaledb', version: '2.3.1'}\n\n```",
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

apiVersion: stackgres.io/v1alpha1
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

### SGShardedCluster.spec.shards
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

The shards are a group of StackGres clusters where the partitioned data chunks are stored.

When referring to the cluster in the descriptions below it applies to any shard's StackGres cluster.


{{< crd-properties >}}
[
  {
    "description": "Number of shard's StackGres clusters",
    "minimum": 0,
    "name": "clusters",
    "required": true,
    "type": "integer"
  },
  {
    "description": "Number of StackGres instances per shard's StackGres cluster. Each instance contains one Postgres server.\n  Out of all of the Postgres servers, one is elected as the primary, the rest remain as read-only replicas.",
    "minimum": 0,
    "name": "instancesPerCluster",
    "required": true,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecshardspods",
    "description": "Cluster pod's configuration.",
    "name": "pods",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsautoscaling",
    "description": "This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.\n\nVertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.\nVertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.",
    "name": "autoscaling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsconfigurations",
    "description": "Shards custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsmanagedsql",
    "description": "This section allows to reference SQL scripts that will be applied to the cluster live.",
    "name": "managedSql",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsmetadata",
    "description": "Metadata information from shards cluster created resources.",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindex",
    "description": "Any shard can be overridden by this section.",
    "name": "overrides",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsreplication",
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

#### SGShardedCluster.spec.shards.pods
<sup><sup>[↩ Parent](#sgshardedclusterspecshards)</sup></sup>

Cluster pod's configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodspersistentvolume",
    "description": "Pod's persistent volume configuration.",
    "name": "persistentVolume",
    "required": true,
    "type": "object"
  },
  {
    "description": "A list of custom application containers that run within the shards cluster's Pods.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodscustomenvfromkeyindex",
    "description": "Custom Pod environment variables from a source for the specified container.",
    "name": "customEnvFrom",
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom application init containers that run within the coordinator cluster's Pods. The\n  custom init containers will run following the defined sequence at the end of\n  cluster's Pods init containers.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volumes that may be used along with any container defined in\n  customInitContainers or customContainers sections for the shards.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the customInitContainers or customContainers sections the name used\n  has to be prepended with the same prefix.\n\nOnly the following volume types are allowed: configMap, downwardAPI, emptyDir,\n  gitRepo, glusterfs, hostPath, nfs, projected and secret\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#volume-v1-core",
    "name": "customVolumes",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating a connection pooling (using [PgBouncer](https://www.pgbouncer.org/)) sidecar.\n\n**Changing this field may require a restart.**",
    "name": "disableConnectionPooling",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If set to `true`, avoids creating the `envoy` sidecar. This sidecar is used as the endge proxy for the cluster's Pods providing extra metrics to the monitoring layer.\n\n**Changing this field may require a restart.**",
    "name": "disableEnvoy",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** use instead .spec.configurations.observability.disableMetrics.",
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
    "anchor": "sgshardedclusterspecshardspodslivenessprobe",
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
    "anchor": "sgshardedclusterspecshardspodsreadinessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "readinessProbe",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsresources",
    "description": "Pod custom resources configuration.",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsscheduling",
    "description": "Pod custom scheduling, affinity and topology spread constratins configuration.\n\n**Changing this field may require a restart.**",
    "name": "scheduling",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully. May be decreased in delete request. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). If this value is nil, the default grace period will be used instead. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. Defaults to 30 seconds.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsupdatestrategy",
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

##### SGShardedCluster.spec.shards.pods.persistentVolume
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspods)</sup></sup>

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
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.",
    "name": "storageClass",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.customEnvFrom[key][index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspods)</sup></sup>

EnvFromSource represents the source of a set of ConfigMaps or Secrets

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#envfromsource-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodscustomenvfromkeyindexconfigmapref",
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
    "anchor": "sgshardedclusterspecshardspodscustomenvfromkeyindexsecretref",
    "description": "SecretEnvSource selects a Secret to populate the environment variables with.\n\nThe contents of the target Secret's Data field will represent the key-value pairs as environment variables.",
    "name": "secretRef",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.customEnvFrom[key][index].configMapRef
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodscustomenvfromkeyindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.customEnvFrom[key][index].secretRef
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodscustomenvfromkeyindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.livenessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspods)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.readinessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspods)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.resources
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspods)</sup></sup>

Pod custom resources configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsresourcescontainerskey",
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
    "description": "When enabled resource limits for containers other than the patroni container wil be set just like for patroni contianer as specified in the SGInstanceProfile.\n\n**Changing this field may require a restart.**",
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
    "anchor": "sgshardedclusterspecshardspodsresourcesinitcontainerskey",
    "description": "Resources configuration to be merged with the specified init container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "initContainers",
    "required": false,
    "type": "map[string]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.resources.containers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsresourcescontainerskeyclaimsindex",
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

##### SGShardedCluster.spec.shards.pods.resources.containers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsresourcescontainerskey)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.resources.initContainers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsresourcesinitcontainerskeyclaimsindex",
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

##### SGShardedCluster.spec.shards.pods.resources.initContainers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsresourcesinitcontainerskey)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspods)</sup></sup>

Pod custom scheduling, affinity and topology spread constratins configuration.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackup",
    "description": "Backup Pod custom scheduling and affinity configuration.",
    "name": "backup",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinity",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinity",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingtolerationsindex",
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingtopologyspreadconstraintsindex",
    "description": "TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#topologyspreadconstraint-v1-core",
    "name": "topologySpreadConstraints",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsscheduling)</sup></sup>

Backup Pod custom scheduling and affinity configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinity",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "nodeAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselector",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "nodeSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinity",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerations",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeselector)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeselector)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackup)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackup)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuptolerations)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuptolerations)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsscheduling)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.scheduling.topologySpreadConstraints[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsscheduling)</sup></sup>

TopologySpreadConstraint specifies how to spread matching pods among the given topology.

{{< crd-properties >}}
[
  {
    "description": "MaxSkew describes the degree to which pods may be unevenly distributed. When `whenUnsatisfiable=DoNotSchedule`, it is the maximum permitted difference between the number of matching pods in the target topology and the global minimum. The global minimum is the minimum number of matching pods in an eligible domain or zero if the number of eligible domains is less than MinDomains. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 2/2/1: In this case, the global minimum is 1. | zone1 | zone2 | zone3 | |  P P  |  P P  |   P   | - if MaxSkew is 1, incoming pod can only be scheduled to zone3 to become 2/2/2; scheduling it onto zone1(zone2) would make the ActualSkew(3-1) on zone1(zone2) violate MaxSkew(1). - if MaxSkew is 2, incoming pod can be scheduled onto any zone. When `whenUnsatisfiable=ScheduleAnyway`, it is used to give higher precedence to topologies that satisfy it. It's a required field. Default value is 1 and 0 is not allowed.",
    "format": "int32",
    "name": "maxSkew",
    "requireRestart": true,
    "required": true,
    "type": "integer"
  },
  {
    "description": "TopologyKey is the key of node labels. Nodes that have a label with this key and identical values are considered to be in the same topology. We consider each \u003ckey, value\u003e as a \"bucket\", and try to put balanced number of pods into each bucket. We define a domain as a particular instance of a topology. Also, we define an eligible domain as a domain whose nodes meet the requirements of nodeAffinityPolicy and nodeTaintsPolicy. e.g. If TopologyKey is \"kubernetes.io/hostname\", each Node is a domain of that topology. And, if TopologyKey is \"topology.kubernetes.io/zone\", each zone is a domain of that topology. It's a required field.",
    "name": "topologyKey",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "WhenUnsatisfiable indicates how to deal with a pod if it doesn't satisfy the spread constraint. - DoNotSchedule (default) tells the scheduler not to schedule it. - ScheduleAnyway tells the scheduler to schedule the pod in any location,\n  but giving higher precedence to topologies that would help reduce the\n  skew.\nA constraint is considered \"Unsatisfiable\" for an incoming pod if and only if every possible node assignment for that pod would violate \"MaxSkew\" on some topology. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 3/1/1: | zone1 | zone2 | zone3 | | P P P |   P   |   P   | If WhenUnsatisfiable is set to DoNotSchedule, incoming pod can only be scheduled to zone2(zone3) to become 3/2/1(3/1/2) as ActualSkew(2-1) on zone2(zone3) satisfies MaxSkew(1). In other words, the cluster can still be imbalanced, but scheduler won't make it *more* imbalanced. It's a required field.",
    "name": "whenUnsatisfiable",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingtopologyspreadconstraintsindexlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select the pods over which spreading will be calculated. The keys are used to lookup values from the incoming pod labels, those key-value labels are ANDed with labelSelector to select the group of existing pods over which spreading will be calculated for the incoming pod. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. MatchLabelKeys cannot be set when LabelSelector isn't set. Keys that don't exist in the incoming pod labels will be ignored. A null or empty list means only match against labelSelector.\n\nThis is a beta field and requires the MatchLabelKeysInPodTopologySpread feature gate to be enabled (enabled by default).",
    "name": "matchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MinDomains indicates a minimum number of eligible domains. When the number of eligible domains with matching topology keys is less than minDomains, Pod Topology Spread treats \"global minimum\" as 0, and then the calculation of Skew is performed. And when the number of eligible domains with matching topology keys equals or greater than minDomains, this value has no effect on scheduling. As a result, when the number of eligible domains is less than minDomains, scheduler won't schedule more than maxSkew Pods to those domains. If value is nil, the constraint behaves as if MinDomains is equal to 1. Valid values are integers greater than 0. When value is not nil, WhenUnsatisfiable must be DoNotSchedule.\n\nFor example, in a 3-zone cluster, MaxSkew is set to 2, MinDomains is set to 5 and pods with the same labelSelector spread as 2/2/2: | zone1 | zone2 | zone3 | |  P P  |  P P  |  P P  | The number of domains is less than 5(MinDomains), so \"global minimum\" is treated as 0. In this situation, new pod with the same labelSelector cannot be scheduled, because computed skew will be 3(3 - 0) if new Pod is scheduled to any of the three zones, it will violate MaxSkew.",
    "format": "int32",
    "name": "minDomains",
    "requireRestart": true,
    "required": false,
    "type": "integer"
  },
  {
    "description": "NodeAffinityPolicy indicates how we will treat Pod's nodeAffinity/nodeSelector when calculating pod topology spread skew. Options are: - Honor: only nodes matching nodeAffinity/nodeSelector are included in the calculations. - Ignore: nodeAffinity/nodeSelector are ignored. All nodes are included in the calculations.\n\nIf this value is nil, the behavior is equivalent to the Honor policy.",
    "name": "nodeAffinityPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "NodeTaintsPolicy indicates how we will treat node taints when calculating pod topology spread skew. Options are: - Honor: nodes without taints, along with tainted nodes for which the incoming pod has a toleration, are included. - Ignore: node taints are ignored. All nodes are included.\n\nIf this value is nil, the behavior is equivalent to the Ignore policy.",
    "name": "nodeTaintsPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.pods.scheduling.topologySpreadConstraints[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingtopologyspreadconstraintsindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardspodsschedulingtopologyspreadconstraintsindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.pods.scheduling.topologySpreadConstraints[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsschedulingtopologyspreadconstraintsindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.pods.updateStrategy
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspods)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardspodsupdatestrategyscheduleindex",
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

##### SGShardedCluster.spec.shards.pods.updateStrategy.schedule[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardspodsupdatestrategy)</sup></sup>

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

#### SGShardedCluster.spec.shards.autoscaling
<sup><sup>[↩ Parent](#sgshardedclusterspecshards)</sup></sup>

This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.

Vertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.
Vertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsautoscalinghorizontal",
    "description": "Section to configure horizontal Pod autoscaling aspects.",
    "name": "horizontal",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsautoscalingmaxallowed",
    "description": "Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "maxAllowed",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsautoscalingminallowed",
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
    "anchor": "sgshardedclusterspecshardsautoscalingvertical",
    "description": "Section to configure vertical Pod autoscaling aspects.",
    "name": "vertical",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.autoscaling.horizontal
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscaling)</sup></sup>

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

##### SGShardedCluster.spec.shards.autoscaling.maxAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscaling)</sup></sup>

Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsautoscalingmaxallowedenvoy",
    "description": "Defines the higher bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsautoscalingmaxallowedpatroni",
    "description": "Defines the higher bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsautoscalingmaxallowedpgbouncer",
    "description": "Defines the higher bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.autoscaling.maxAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscalingmaxallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.autoscaling.maxAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscalingmaxallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.autoscaling.maxAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscalingmaxallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.autoscaling.minAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscaling)</sup></sup>

Defines the lower bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsautoscalingminallowedenvoy",
    "description": "Defines the lower bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsautoscalingminallowedpatroni",
    "description": "Defines the lower bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsautoscalingminallowedpgbouncer",
    "description": "Defines the lower bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.autoscaling.minAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscalingminallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.autoscaling.minAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscalingminallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.autoscaling.minAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscalingminallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.autoscaling.vertical
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsautoscaling)</sup></sup>

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

#### SGShardedCluster.spec.shards.configurations
<sup><sup>[↩ Parent](#sgshardedclusterspecshards)</sup></sup>

Shards custom configurations.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsconfigurationspatroni",
    "description": "Allows specifying Patroni configuration that will extend the generated one\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsconfigurationspooling",
    "description": "",
    "name": "pooling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsconfigurationspostgres",
    "description": "Section for postgres configuration.",
    "name": "postgres",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsconfigurationspostgresexporter",
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

##### SGShardedCluster.spec.shards.configurations.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsconfigurations)</sup></sup>

Allows specifying Patroni configuration that will extend the generated one

If sharding type is `shardingsphere` then this section is ignored.


{{< crd-properties >}}
[
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
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.configurations.pooling
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsconfigurations)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsconfigurationspoolingpgbouncer",
    "description": "",
    "name": "pgBouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.configurations.pooling.pgBouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsconfigurationspooling)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsconfigurationspoolingpgbouncerpgbouncerini",
    "description": "The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).\n\n**Changing this field may require a restart.**",
    "name": "pgbouncer.ini",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.configurations.pooling.pgBouncer.pgbouncer.ini
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsconfigurationspoolingpgbouncer)</sup></sup>

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

##### SGShardedCluster.spec.shards.configurations.postgres
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsconfigurations)</sup></sup>

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

##### SGShardedCluster.spec.shards.configurations.postgresExporter
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsconfigurations)</sup></sup>

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

#### SGShardedCluster.spec.shards.managedSql
<sup><sup>[↩ Parent](#sgshardedclusterspecshards)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsmanagedsqlscriptsindex",
    "description": "A list of script references that will be executed in sequence.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsmanagedsql)</sup></sup>

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

#### SGShardedCluster.spec.shards.metadata
<sup><sup>[↩ Parent](#sgshardedclusterspecshards)</sup></sup>

Metadata information from shards cluster created resources.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsmetadataannotations",
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsmetadatalabels",
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.",
    "name": "labels",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.metadata.annotations
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsmetadata)</sup></sup>

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

##### SGShardedCluster.spec.shards.metadata.labels
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsmetadata)</sup></sup>

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

#### SGShardedCluster.spec.shards.overrides[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshards)</sup></sup>

Any shard can be overridden by this section.


{{< crd-properties >}}
[
  {
    "description": "Identifier of the shard StackGres cluster to override (starting from 0)",
    "minimum": 0,
    "name": "index",
    "required": true,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscaling",
    "description": "This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.\n\nVertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.\nVertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.",
    "name": "autoscaling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexconfigurations",
    "description": "Shards custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "description": "Number of StackGres instances per shard's StackGres cluster. Each instance contains one Postgres server.\n  Out of all of the Postgres servers, one is elected as the primary, the rest remain as read-only replicas.",
    "minimum": 0,
    "name": "instancesPerCluster",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexmanagedsql",
    "description": "This section allows to reference SQL scripts that will be applied to the cluster live.",
    "name": "managedSql",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexmetadata",
    "description": "Metadata information from shards cluster created resources.",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpods",
    "description": "Cluster pod's configuration.",
    "name": "pods",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexreplication",
    "description": "This section allows to configure the global Postgres replication mode.\n\nThe main replication group is implicit and contains the total number of instances less the sum of all\n  instances in other replication groups.\n\nThe total number of instances is always specified by `.spec.instances`.",
    "name": "replication",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGInstanceProfile](https://stackgres.io/doc/latest/04-postgres-cluster-management/03-resource-profiles/). A SGInstanceProfile defines CPU and memory limits. Must exist before creating a cluster. When no profile is set, a default (currently: 1 core, 2 GiB RAM) one is used.",
    "name": "sgInstanceProfile",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].autoscaling
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindex)</sup></sup>

This section allows configuring vertical Pod autoscaling for the SGCluster's Pods.

Vertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.
Vertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalinghorizontal",
    "description": "Section to configure horizontal Pod autoscaling aspects.",
    "name": "horizontal",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingmaxallowed",
    "description": "Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "maxAllowed",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingminallowed",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingvertical",
    "description": "Section to configure vertical Pod autoscaling aspects.",
    "name": "vertical",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.horizontal
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscaling)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.maxAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscaling)</sup></sup>

Defines the higher bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingmaxallowedenvoy",
    "description": "Defines the higher bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingmaxallowedpatroni",
    "description": "Defines the higher bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingmaxallowedpgbouncer",
    "description": "Defines the higher bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.maxAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscalingmaxallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.maxAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscalingmaxallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.maxAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscalingmaxallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.minAllowed
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscaling)</sup></sup>

Defines the lower bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingminallowedenvoy",
    "description": "Defines the lower bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingminallowedpatroni",
    "description": "Defines the lower bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexautoscalingminallowedpgbouncer",
    "description": "Defines the lower bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.minAllowed.envoy
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscalingminallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.minAllowed.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscalingminallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.minAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscalingminallowed)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].autoscaling.vertical
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexautoscaling)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].configurations
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindex)</sup></sup>

Shards custom configurations.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexconfigurationspatroni",
    "description": "Allows specifying Patroni configuration that will extend the generated one\n\nIf sharding type is `shardingsphere` then this section is ignored.",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexconfigurationspooling",
    "description": "",
    "name": "pooling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexconfigurationspostgres",
    "description": "Section for postgres configuration.",
    "name": "postgres",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexconfigurationspostgresexporter",
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

##### SGShardedCluster.spec.shards.overrides[index].configurations.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexconfigurations)</sup></sup>

Allows specifying Patroni configuration that will extend the generated one

If sharding type is `shardingsphere` then this section is ignored.


{{< crd-properties >}}
[
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
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].configurations.pooling
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexconfigurations)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexconfigurationspoolingpgbouncer",
    "description": "",
    "name": "pgBouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].configurations.pooling.pgBouncer
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexconfigurationspooling)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexconfigurationspoolingpgbouncerpgbouncerini",
    "description": "The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).\n\n**Changing this field may require a restart.**",
    "name": "pgbouncer.ini",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].configurations.pooling.pgBouncer.pgbouncer.ini
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexconfigurationspoolingpgbouncer)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].configurations.postgres
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexconfigurations)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].configurations.postgresExporter
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexconfigurations)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].managedSql
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexmanagedsqlscriptsindex",
    "description": "A list of script references that will be executed in sequence.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexmanagedsql)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].metadata
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindex)</sup></sup>

Metadata information from shards cluster created resources.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexmetadataannotations",
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexmetadatalabels",
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.",
    "name": "labels",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].metadata.annotations
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexmetadata)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].metadata.labels
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexmetadata)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindex)</sup></sup>

Cluster pod's configuration.

{{< crd-properties >}}
[
  {
    "description": "A list of custom application containers that run within the shards cluster's Pods.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom application init containers that run within the coordinator cluster's Pods. The\n  custom init containers will run following the defined sequence at the end of\n  cluster's Pods init containers.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  has to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitContainers",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "description": "A list of custom environment variables for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitEnv",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom environment variables from source for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitEnvFrom",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified init container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customInitVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volume mounts for the specified container.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
    "name": "customVolumeMounts",
    "requireRestart": true,
    "required": false,
    "type": "map[string][]object"
  },
  {
    "description": "A list of custom volumes that may be used along with any container defined in\n  customInitContainers or customContainers sections for the shards.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the customInitContainers or customContainers sections the name used\n  has to be prepended with the same prefix.\n\nOnly the following volume types are allowed: configMap, downwardAPI, emptyDir,\n  gitRepo, glusterfs, hostPath, nfs, projected and secret\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#volume-v1-core",
    "name": "customVolumes",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "default": false,
    "description": "If set to `true`, avoids creating a connection pooling (using [PgBouncer](https://www.pgbouncer.org/)) sidecar.\n\n**Changing this field may require a restart.**",
    "name": "disableConnectionPooling",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If set to `true`, avoids creating the `envoy` sidecar. This sidecar is used as the endge proxy for the cluster's Pods providing extra metrics to the monitoring layer.\n\n**Changing this field may require a restart.**",
    "name": "disableEnvoy",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** use instead .spec.configurations.observability.disableMetrics.",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodslivenessprobe",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodspersistentvolume",
    "description": "Pod's persistent volume configuration.",
    "name": "persistentVolume",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsreadinessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "readinessProbe",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsresources",
    "description": "Pod custom resources configuration.",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsscheduling",
    "description": "Pod custom scheduling, affinity and topology spread constratins configuration.\n\n**Changing this field may require a restart.**",
    "name": "scheduling",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "Optional duration in seconds the pod needs to terminate gracefully. May be decreased in delete request. Value must be non-negative integer. The value zero indicates stop immediately via the kill signal (no opportunity to shut down). If this value is nil, the default grace period will be used instead. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. Defaults to 30 seconds.",
    "format": "int64",
    "name": "terminationGracePeriodSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsupdatestrategy",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.livenessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpods)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.persistentVolume
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpods)</sup></sup>

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
    "description": "Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolumes for the instances of the cluster.",
    "name": "storageClass",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.readinessProbe
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpods)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.resources
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpods)</sup></sup>

Pod custom resources configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsresourcescontainerskey",
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
    "description": "When enabled resource limits for containers other than the patroni container wil be set just like for patroni contianer as specified in the SGInstanceProfile.\n\n**Changing this field may require a restart.**",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsresourcesinitcontainerskey",
    "description": "Resources configuration to be merged with the specified init container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "initContainers",
    "required": false,
    "type": "map[string]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.resources.containers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsresourcescontainerskeyclaimsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.resources.containers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsresourcescontainerskey)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.resources.initContainers[key]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsresourcesinitcontainerskeyclaimsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.resources.initContainers[key].claims[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsresourcesinitcontainerskey)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpods)</sup></sup>

Pod custom scheduling, affinity and topology spread constratins configuration.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackup",
    "description": "Backup Pod custom scheduling and affinity configuration.",
    "name": "backup",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinity",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinity",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingtolerationsindex",
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingtopologyspreadconstraintsindex",
    "description": "TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#topologyspreadconstraint-v1-core",
    "name": "topologySpreadConstraints",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsscheduling)</sup></sup>

Backup Pod custom scheduling and affinity configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinity",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "nodeAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselector",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "nodeSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinity",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerations",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselector)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorpreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselector)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.nodeSelector.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackupnodeselectorrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackup)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackup)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerations)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationspreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerations)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.backup.tolerations.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingbackuptolerationsrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinity)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsscheduling)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.topologySpreadConstraints[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsscheduling)</sup></sup>

TopologySpreadConstraint specifies how to spread matching pods among the given topology.

{{< crd-properties >}}
[
  {
    "description": "MaxSkew describes the degree to which pods may be unevenly distributed. When `whenUnsatisfiable=DoNotSchedule`, it is the maximum permitted difference between the number of matching pods in the target topology and the global minimum. The global minimum is the minimum number of matching pods in an eligible domain or zero if the number of eligible domains is less than MinDomains. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 2/2/1: In this case, the global minimum is 1. | zone1 | zone2 | zone3 | |  P P  |  P P  |   P   | - if MaxSkew is 1, incoming pod can only be scheduled to zone3 to become 2/2/2; scheduling it onto zone1(zone2) would make the ActualSkew(3-1) on zone1(zone2) violate MaxSkew(1). - if MaxSkew is 2, incoming pod can be scheduled onto any zone. When `whenUnsatisfiable=ScheduleAnyway`, it is used to give higher precedence to topologies that satisfy it. It's a required field. Default value is 1 and 0 is not allowed.",
    "format": "int32",
    "name": "maxSkew",
    "requireRestart": true,
    "required": true,
    "type": "integer"
  },
  {
    "description": "TopologyKey is the key of node labels. Nodes that have a label with this key and identical values are considered to be in the same topology. We consider each \u003ckey, value\u003e as a \"bucket\", and try to put balanced number of pods into each bucket. We define a domain as a particular instance of a topology. Also, we define an eligible domain as a domain whose nodes meet the requirements of nodeAffinityPolicy and nodeTaintsPolicy. e.g. If TopologyKey is \"kubernetes.io/hostname\", each Node is a domain of that topology. And, if TopologyKey is \"topology.kubernetes.io/zone\", each zone is a domain of that topology. It's a required field.",
    "name": "topologyKey",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "description": "WhenUnsatisfiable indicates how to deal with a pod if it doesn't satisfy the spread constraint. - DoNotSchedule (default) tells the scheduler not to schedule it. - ScheduleAnyway tells the scheduler to schedule the pod in any location,\n  but giving higher precedence to topologies that would help reduce the\n  skew.\nA constraint is considered \"Unsatisfiable\" for an incoming pod if and only if every possible node assignment for that pod would violate \"MaxSkew\" on some topology. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 3/1/1: | zone1 | zone2 | zone3 | | P P P |   P   |   P   | If WhenUnsatisfiable is set to DoNotSchedule, incoming pod can only be scheduled to zone2(zone3) to become 3/2/1(3/1/2) as ActualSkew(2-1) on zone2(zone3) satisfies MaxSkew(1). In other words, the cluster can still be imbalanced, but scheduler won't make it *more* imbalanced. It's a required field.",
    "name": "whenUnsatisfiable",
    "requireRestart": true,
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingtopologyspreadconstraintsindexlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select the pods over which spreading will be calculated. The keys are used to lookup values from the incoming pod labels, those key-value labels are ANDed with labelSelector to select the group of existing pods over which spreading will be calculated for the incoming pod. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. MatchLabelKeys cannot be set when LabelSelector isn't set. Keys that don't exist in the incoming pod labels will be ignored. A null or empty list means only match against labelSelector.\n\nThis is a beta field and requires the MatchLabelKeysInPodTopologySpread feature gate to be enabled (enabled by default).",
    "name": "matchLabelKeys",
    "requireRestart": true,
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MinDomains indicates a minimum number of eligible domains. When the number of eligible domains with matching topology keys is less than minDomains, Pod Topology Spread treats \"global minimum\" as 0, and then the calculation of Skew is performed. And when the number of eligible domains with matching topology keys equals or greater than minDomains, this value has no effect on scheduling. As a result, when the number of eligible domains is less than minDomains, scheduler won't schedule more than maxSkew Pods to those domains. If value is nil, the constraint behaves as if MinDomains is equal to 1. Valid values are integers greater than 0. When value is not nil, WhenUnsatisfiable must be DoNotSchedule.\n\nFor example, in a 3-zone cluster, MaxSkew is set to 2, MinDomains is set to 5 and pods with the same labelSelector spread as 2/2/2: | zone1 | zone2 | zone3 | |  P P  |  P P  |  P P  | The number of domains is less than 5(MinDomains), so \"global minimum\" is treated as 0. In this situation, new pod with the same labelSelector cannot be scheduled, because computed skew will be 3(3 - 0) if new Pod is scheduled to any of the three zones, it will violate MaxSkew.",
    "format": "int32",
    "name": "minDomains",
    "requireRestart": true,
    "required": false,
    "type": "integer"
  },
  {
    "description": "NodeAffinityPolicy indicates how we will treat Pod's nodeAffinity/nodeSelector when calculating pod topology spread skew. Options are: - Honor: only nodes matching nodeAffinity/nodeSelector are included in the calculations. - Ignore: nodeAffinity/nodeSelector are ignored. All nodes are included in the calculations.\n\nIf this value is nil, the behavior is equivalent to the Honor policy.",
    "name": "nodeAffinityPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "NodeTaintsPolicy indicates how we will treat node taints when calculating pod topology spread skew. Options are: - Honor: nodes without taints, along with tainted nodes for which the incoming pod has a toleration, are included. - Ignore: node taints are ignored. All nodes are included.\n\nIf this value is nil, the behavior is equivalent to the Ignore policy.",
    "name": "nodeTaintsPolicy",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.topologySpreadConstraints[index].labelSelector
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingtopologyspreadconstraintsindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsschedulingtopologyspreadconstraintsindexlabelselectormatchexpressionsindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.scheduling.topologySpreadConstraints[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsschedulingtopologyspreadconstraintsindexlabelselector)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].pods.updateStrategy
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpods)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexpodsupdatestrategyscheduleindex",
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

##### SGShardedCluster.spec.shards.overrides[index].pods.updateStrategy.schedule[index]
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexpodsupdatestrategy)</sup></sup>

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

##### SGShardedCluster.spec.shards.overrides[index].replication
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindex)</sup></sup>

This section allows to configure the global Postgres replication mode.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsoverridesindexreplicationinitialization",
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

##### SGShardedCluster.spec.shards.overrides[index].replication.initialization
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexreplication)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsoverridesindexreplicationinitializationbackuprestoreperformance",
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

##### SGShardedCluster.spec.shards.overrides[index].replication.initialization.backupRestorePerformance
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsoverridesindexreplicationinitialization)</sup></sup>

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

#### SGShardedCluster.spec.shards.replication
<sup><sup>[↩ Parent](#sgshardedclusterspecshards)</sup></sup>

This section allows to configure the global Postgres replication mode.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecshardsreplicationinitialization",
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

##### SGShardedCluster.spec.shards.replication.initialization
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsreplication)</sup></sup>

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
    "anchor": "sgshardedclusterspecshardsreplicationinitializationbackuprestoreperformance",
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

##### SGShardedCluster.spec.shards.replication.initialization.backupRestorePerformance
<sup><sup>[↩ Parent](#sgshardedclusterspecshardsreplicationinitialization)</sup></sup>

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

### SGShardedCluster.spec.configurations
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

Sharded cluster custom configurations.

**Example:**


```
 yaml
apiVersion: stackgres.io/v1alpha1
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
    "description": "Continuous Archiving backups are composed of periodic *base backups* and all the WAL segments produced in between those base backups for the coordinator and each shard. This parameter specifies at what time and with what frequency to start performing a new base backup.\n\nUse cron syntax (`m h dom mon dow`) for this parameter, i.e., 5 values separated by spaces:\n*  `m`: minute, 0 to 59.\n*  `h`: hour, 0 to 23.\n*  `dom`: day of month, 1 to 31 (recommended not to set it higher than 28).\n*  `mon`: month, 1 to 12.\n*  `dow`: day of week, 0 to 7 (0 and 7 both represent Sunday).\n\nAlso ranges of values (`start-end`), the symbol `*` (meaning `first-last`) or even `*/N`, where `N` is a number, meaning \"\"every `N`, may be used. All times are UTC. It is recommended to avoid 00:00 as base backup time, to avoid overlapping with any other external operations happening at this time.\n\nIf not set, full backups are never performed automatically.",
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
    "description": "The paths where the backups are stored. If not set this field is filled up by the operator.\n\nWhen provided will indicate where the backups and WAL files will be stored.\n\nThe first path indicate the coordinator path and the other paths indicate the shards paths\n\n\u003e **WARNING**: Most users should leave this field empty since having it manually set could be dangerous. If the value is repeated due to re-creating an SGShardedCluster or\n re-using the same value in another SGShardedCluster and you may get a mixed WAL history with unrecoverable backups.",
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

#### SGShardedCluster.spec.configurations.credentials
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurations)</sup></sup>

Allows specifying custom credentials for Postgres users and Patroni REST API

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialspatroni",
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials for patroni REST API.\n\n**Changing this field may require a restart.**",
    "name": "patroni",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusers",
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the users.\n\n**Changing this field may require a manual modification of the database users to reflect the new values specified.**\n\nIn particular you may have to create those users if username is changed or alter password if it is changed. Here are the SQL commands to perform such operation (replace\n  default usernames with the new ones and `***` with their respective passwords):\n\n* Superuser username changed:\n\n```\n\nCREATE ROLE postgres;\n\n```\n\n* Superuser password changed:\n\n```\n\nALTER ROLE postgres WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS PASSWORD '***';\n\n```\n\n* Replication username changed:\n\n```\n\nCREATE ROLE replicator;\n\n```\n\n* Replication password changed:\n\n```\n\nALTER ROLE replicator WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN REPLICATION NOBYPASSRLS PASSWORD '***';\n\n```\n\n* Authenticator username changed:\n\n```\n\nCREATE ROLE authenticator;\n\n```\n\n* Authenticator password changed:\n\n```\n\nALTER ROLE authenticator WITH SUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '***';\n\n```\n\n\n**Changing this field may require a restart.**",
    "name": "users",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.patroni
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentials)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials for patroni REST API.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialspatronirestapipassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password for the patroni REST API.",
    "name": "restApiPassword",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.patroni.restApiPassword
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialspatroni)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password for the patroni REST API.


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

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the users.

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


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersauthenticator",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.",
    "name": "authenticator",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersreplication",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.",
    "name": "replication",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsuserssuperuser",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).",
    "name": "superuser",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.authenticator
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersauthenticatorpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersauthenticatorusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.authenticator.password
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersreplicationpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsusersreplicationusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.replication.password
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsuserssuperuserpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecconfigurationscredentialsuserssuperuserusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.configurations.credentials.users.superuser.password
<sup><sup>[↩ Parent](#sgshardedclusterspecconfigurationscredentialsuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.


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

apiVersion: stackgres.io/v1alpha1
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

apiVersion: stackgres.io/v1
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

apiVersion: stackgres.io/v1alpha1
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
      shardsPrimariesService:
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
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/).",
    "name": "shardsPrimariesService",
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

apiVersion: stackgres.io/v1alpha1
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
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).",
    "name": "shardsPrimariesService",
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
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allows achieving the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less cpu than it requires. It also allows setting [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.\n\nBy default, StackGres will configure cpu requirements to have the same limit and request for all the containers. Set this property to true to prevent StackGres from setting container's cpu requirements request equals to the limit (except for patroni container, see `enablePatroniCpuRequests`)\n  when `.spec.requests.containers.\u003ccontainer name\u003e.cpu` `.spec.requests.initContainers.\u003ccontainer name\u003e.cpu` is configured in the referenced `SGInstanceProfile`.",
    "name": "enableSetClusterCpuRequests",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allows achieving the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less memory than it requires.\n\nBy default, StackGres will configure memory requirements to have the same limit and request for all the containers. Set this property to true to prevent StackGres from setting container's memory requirements request equals to the limit (except for patroni container, see `enablePatroniCpuRequests`)\n  when `.spec.requests.containers.\u003ccontainer name\u003e.memory` `.spec.requests.initContainers.\u003ccontainer name\u003e.memory` is configured in the referenced `SGInstanceProfile`.",
    "name": "enableSetClusterMemoryRequests",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allows achieving the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less cpu than it requires. It also allows setting [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.\n\nBy default, StackGres will configure cpu requirements to have the same limit and request for the patroni container. Set this property to true to prevent StackGres from setting patroni container's cpu requirements request equals to the limit\n  when `.spec.requests.cpu` is configured in the referenced `SGInstanceProfile`.",
    "name": "enableSetPatroniCpuRequests",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allows achieving the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less memory than it requires.\n\nBy default, StackGres will configure memory requirements to have the same limit and request for the patroni container. Set this property to true to prevent StackGres from setting patroni container's memory requirements request equals to the limit\n  when `.spec.requests.memory` is configured in the referenced `SGInstanceProfile`.",
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
    "description": "Configuration for the shards services",
    "name": "shards",
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
    "description": "The list of custom ports that will be exposed by the coordinator services.\n\nThe names of custom ports will be prefixed with the string `c-` so they do not\n  conflict with ports defined for the coordinator services.\n\nThe names of target ports will be prefixed with the string `c-` so that the ports\n  that can be referenced in this section will be only those defined under\n  .spec.pods.customContainers[].ports sections where names are also prepended with the same\n  prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#serviceport-v1-core",
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
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "type determines how the Service is exposed. Defaults to ClusterIP. Valid\noptions are ClusterIP, NodePort, LoadBalancer and None. \"ClusterIP\" allocates\na cluster-internal IP address for load-balancing to endpoints.\n\"NodePort\" builds on ClusterIP and allocates a port on every node.\n\"LoadBalancer\" builds on NodePort and creates\nan external load-balancer (if supported in the current cloud).\n\"None\" creates an headless service that can be use in conjunction with `.spec.pods.disableEnvoy`\n set to `true` in order to acces the database using a DNS.\nMore info:\n* https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types\n* https://kubernetes.io/docs/concepts/services-networking/service/#headless-services\n* https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/",
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

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core

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
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "type determines how the Service is exposed. Defaults to ClusterIP. Valid\noptions are ClusterIP, NodePort, LoadBalancer and None. \"ClusterIP\" allocates\na cluster-internal IP address for load-balancing to endpoints.\n\"NodePort\" builds on ClusterIP and allocates a port on every node.\n\"LoadBalancer\" builds on NodePort and creates\nan external load-balancer (if supported in the current cloud).\n\"None\" creates an headless service that can be use in conjunction with `.spec.pods.disableEnvoy`\n set to `true` in order to acces the database using a DNS.\nMore info:\n* https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types\n* https://kubernetes.io/docs/concepts/services-networking/service/#headless-services\n* https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/",
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

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core

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

#### SGShardedCluster.spec.postgresServices.shards
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservices)</sup></sup>

Configuration for the shards services


{{< crd-properties >}}
[
  {
    "description": "The list of custom ports that will be exposed by the shards services.\n\nThe names of custom ports will be prefixed with the string `c-` so they do not\n  conflict with ports defined for the shards services.\n\nThe names of target ports will be prefixed with the string `c-` so that the ports\n  that can be referenced in this section will be only those defined under\n  .spec.pods.customContainers[].ports sections where names are also prepended with the same\n  prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#serviceport-v1-core",
    "name": "customPorts",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardedclusterspecpostgresservicesshardsprimaries",
    "description": "Configure the shards service to any primary in the shards with the name as the SGShardedCluster plus the `-shards` suffix.\n\nIt provides a stable connection (regardless of primary failures or switchovers) to read-write Postgres servers of any shard cluster. Read-write servers are load-balanced via this service.\n\nSee also https://kubernetes.io/docs/concepts/services-networking/service/",
    "name": "primaries",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.postgresServices.shards.primaries
<sup><sup>[↩ Parent](#sgshardedclusterspecpostgresservicesshards)</sup></sup>

Configure the shards service to any primary in the shards with the name as the SGShardedCluster plus the `-shards` suffix.

It provides a stable connection (regardless of primary failures or switchovers) to read-write Postgres servers of any shard cluster. Read-write servers are load-balanced via this service.

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
    "description": "SessionAffinityConfig represents the configurations of session affinity.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core",
    "name": "sessionAffinityConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "type determines how the Service is exposed. Defaults to ClusterIP. Valid\noptions are ClusterIP, NodePort, LoadBalancer and None. \"ClusterIP\" allocates\na cluster-internal IP address for load-balancing to endpoints.\n\"NodePort\" builds on ClusterIP and allocates a port on every node.\n\"LoadBalancer\" builds on NodePort and creates\nan external load-balancer (if supported in the current cloud).\n\"None\" creates an headless service that can be use in conjunction with `.spec.pods.disableEnvoy`\n set to `true` in order to acces the database using a DNS.\nMore info:\n* https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types\n* https://kubernetes.io/docs/concepts/services-networking/service/#headless-services\n* https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/",
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

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core

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

### SGShardedCluster.spec.replicateFrom
<sup><sup>[↩ Parent](#sgshardedclusterspec)</sup></sup>

Make the sharded cluster a read-only standby replica allowing replication from another sharded cluster and acting as a relay.

Changing this section is allowed to fix issues or to change the replication source.

Removing this section convert the sharded cluster in a normal sharded cluster where the standby leader of each SGCluster is converted into a primary instance.

**Example:**

From SGShardedCluster instance:


```yaml

apiVersion: stackgres.io/v1
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

apiVersion: stackgres.io/v1
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

apiVersion: stackgres.io/v1
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

apiVersion: stackgres.io/v1alpha1
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
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the users.",
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

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the users.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefromusersauthenticator",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.",
    "name": "authenticator",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromusersreplication",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.",
    "name": "replication",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromuserssuperuser",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).",
    "name": "superuser",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.authenticator
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefromusersauthenticatorpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromusersauthenticatorusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.authenticator.password
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusersauthenticator)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefromusersreplicationpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromusersreplicationusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.replication.password
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromusersreplication)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).


{{< crd-properties >}}
[
  {
    "anchor": "sgshardedclusterspecreplicatefromuserssuperuserpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardedclusterspecreplicatefromuserssuperuserusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedCluster.spec.replicateFrom.users.superuser.password
<sup><sup>[↩ Parent](#sgshardedclusterspecreplicatefromuserssuperuser)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.


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

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.


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
