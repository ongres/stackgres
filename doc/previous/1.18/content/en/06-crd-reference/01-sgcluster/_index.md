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
```

See also [Cluster Creation section]({{% relref "04-administration-guide/02-cluster-creation" %}}).





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
    "description": "SGCluster",
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
    "anchor": "sgclusterspec",
    "description": "Specification of the desired behavior of a StackGres cluster.",
    "name": "spec",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterstatus",
    "description": "Current status of a StackGres cluster.",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGCluster.spec
<sup><sup>[↩ Parent](#sgcluster)</sup></sup>

Specification of the desired behavior of a StackGres cluster.


{{< crd-properties >}}
[
  {
    "description": "Number of instances for the StackGres cluster. Each instance is a Pod containing one Postgres server.\n  Out of all of the Postgres servers, one is elected as the primary, the rest remain as read-only replicas.",
    "minimum": 0,
    "name": "instances",
    "required": true,
    "type": "integer"
  },
  {
    "anchor": "sgclusterspecpods",
    "description": "Cluster pod's configuration.",
    "name": "pods",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecpostgres",
    "description": "This section allows to configure Postgres features",
    "name": "postgres",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecautoscaling",
    "description": "This section allows to configure horizontal and vertical Pod autoscaling for the SGCluster's Pods.\n\nHorizontal Pod Autoscaling will use replicas connections usage (active connections / max connections) as the metric to control the upscale or downscale of the replicas.\nHorizontal Pod Autoscaling require the [KEDA operator](https://github.com/kedacore/keda) to be installed in the Kubernetes cluster.\n\nVertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.\nVertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.",
    "name": "autoscaling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurations",
    "description": "Cluster custom configurations.",
    "name": "configurations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecdistributedlogs",
    "description": "StackGres features a functionality for all pods to send Postgres, Patroni and PgBouncer logs to a central (distributed) location, which is in turn another Postgres database. Logs can then be accessed via SQL interface or from the web UI. This section controls whether to enable this feature or not. If not enabled, logs are send to the pod's standard output.",
    "name": "distributedLogs",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecinitialdata",
    "description": "Cluster initialization data options. Cluster may be initialized empty, or from a backup restoration.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "initialData",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecmanagedsql",
    "description": "This section allows to reference SQL scripts that will be applied to the cluster live.",
    "name": "managedSql",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecmetadata",
    "description": "Metadata information for cluster created resources.",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecnonproductionoptions",
    "description": "",
    "name": "nonProductionOptions",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecpostgresservices",
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
    "anchor": "sgclusterspecreplicatefrom",
    "description": "Make the cluster a read-only standby replica allowing to replicate from another PostgreSQL instance and acting as a rely.\n\nChanging this section is allowed to fix issues or to change the replication source.\n\nRemoving this section convert the cluster in a normal cluster where the standby leader is converted into a primary instance.",
    "name": "replicateFrom",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplication",
    "description": "This section allows to configure Postgres replication mode and HA roles groups.\n\nThe main replication group is implicit and contains the total number of instances less the sum of all\n  instances in other replication groups.\n\nThe total number of instances is always specified by `.spec.instances`.",
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
  },
  {
    "anchor": "sgclusterspectoinstallpostgresextensionsindex",
    "description": "The list of Postgres extensions to install.\n\n**This section is filled by the operator.**",
    "name": "toInstallPostgresExtensions",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

### SGCluster.spec.pods
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Cluster pod's configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodspersistentvolume",
    "description": "Pod's persistent volume configuration.",
    "name": "persistentVolume",
    "required": true,
    "type": "object"
  },
  {
    "description": "A list of custom application containers that run within the cluster's Pods.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  have to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
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
    "description": "A list of custom application init containers that run within the cluster's Pods. The\n  custom init containers will run following the defined sequence as the end of\n  cluster's Pods init containers.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the .spec.containers section of SGInstanceProfile the name used\n  have to be prepended with the same prefix.\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#container-v1-core",
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
    "description": "A list of custom volumes that may be used along with any container defined in\n  customInitContainers or customContainers sections.\n\nThe name used in this section will be prefixed with the string `c-` so that when\n  referencing them in the customInitContainers or customContainers sections the name used\n  have to be prepended with the same prefix.\n\nOnly the following volume types are allowed: configMap, downwardAPI, emptyDir,\n  gitRepo, glusterfs, hostPath, nfs, projected and secret\n\n**Changing this field may require a restart.**\n  \nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#volume-v1-core",
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
    "default": true,
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
    "description": "If set to `true`, avoids creating the `postgres-util` sidecar. This sidecar contains usual Postgres administration utilities *that are not present in the main (`patroni`) container*, like `psql`. Only disable if you know what you are doing.\n\n**Changing this field may require a restart.**",
    "name": "disablePostgresUtil",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgclusterspecpodslivenessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "livenessProbe",
    "required": false,
    "type": "object"
  },
  {
    "default": "OrderedReady",
    "description": "managementPolicy controls how pods are created during initial scale up, when replacing pods\n  on nodes, or when scaling down. The default policy is `OrderedReady`, where pods are created\n  in increasing order (pod-0, then pod-1, etc) and the controller will wait until each pod is\n  ready before continuing. When scaling down, the pods are removed in the opposite order.\n  The alternative policy is `Parallel` which will create pods in parallel to match the desired\n  scale without waiting, and on scale down will delete all pods at once.",
    "name": "managementPolicy",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecpodsreadinessprobe",
    "description": "Probe describes a health check to be performed against a container to determine whether it is alive or ready to receive traffic.",
    "name": "readinessProbe",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecpodsresources",
    "description": "Pod custom resources configuration.",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecpodsscheduling",
    "description": "Pod custom scheduling, affinity and topology spread constraints configuration.\n\n**Changing this field may require a restart.**",
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
    "anchor": "sgclusterspecpodsupdatestrategy",
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

#### SGCluster.spec.pods.livenessProbe
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

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

#### SGCluster.spec.pods.readinessProbe
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

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

#### SGCluster.spec.pods.resources
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

Pod custom resources configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsresourcescontainerskey",
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
    "default": false,
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
    "anchor": "sgclusterspecpodsresourcesinitcontainerskey",
    "description": "Resources configuration to be merged with the specified init container on top of SGInstanceProfile referenced by `sgInstanceProfile` field if specified.",
    "name": "initContainers",
    "required": false,
    "type": "map[string]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.resources.containers[key]
<sup><sup>[↩ Parent](#sgclusterspecpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsresourcescontainerskeyclaimsindex",
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

##### SGCluster.spec.pods.resources.containers[key].claims[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsresourcescontainerskey)</sup></sup>

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

##### SGCluster.spec.pods.resources.initContainers[key]
<sup><sup>[↩ Parent](#sgclusterspecpodsresources)</sup></sup>

ResourceRequirements describes the compute resource requirements.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsresourcesinitcontainerskeyclaimsindex",
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

##### SGCluster.spec.pods.resources.initContainers[key].claims[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsresourcesinitcontainerskey)</sup></sup>

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

#### SGCluster.spec.pods.scheduling
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

Pod custom scheduling, affinity and topology spread constraints configuration.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackup",
    "description": "Backup Pod custom scheduling and affinity configuration.",
    "name": "backup",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinity",
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
    "anchor": "sgclusterspecpodsschedulingpodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingpodantiaffinity",
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
    "anchor": "sgclusterspecpodsschedulingtolerationsindex",
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingtopologyspreadconstraintsindex",
    "description": "TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#topologyspreadconstraint-v1-core",
    "name": "topologySpreadConstraints",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.backup
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

Backup Pod custom scheduling and affinity configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinity",
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
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinity",
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
    "anchor": "sgclusterspecpodsschedulingbackuptolerationsindex",
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#toleration-v1-core",
    "name": "tolerations",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackup)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackupnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.podAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackup)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinity)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.backup.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackup)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinity)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.backup.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackuppodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.backup.tolerations[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingbackup)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "requireRestart": true,
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinity)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinity)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

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

##### SGCluster.spec.pods.scheduling.topologySpreadConstraints[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsscheduling)</sup></sup>

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
    "anchor": "sgclusterspecpodsschedulingtopologyspreadconstraintsindexlabelselector",
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

##### SGCluster.spec.pods.scheduling.topologySpreadConstraints[index].labelSelector
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingtopologyspreadconstraintsindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpodsschedulingtopologyspreadconstraintsindexlabelselectormatchexpressionsindex",
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

##### SGCluster.spec.pods.scheduling.topologySpreadConstraints[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsschedulingtopologyspreadconstraintsindexlabelselector)</sup></sup>

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

#### SGCluster.spec.pods.updateStrategy
<sup><sup>[↩ Parent](#sgclusterspecpods)</sup></sup>

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
    "anchor": "sgclusterspecpodsupdatestrategyscheduleindex",
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

##### SGCluster.spec.pods.updateStrategy.schedule[index]
<sup><sup>[↩ Parent](#sgclusterspecpodsupdatestrategy)</sup></sup>

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

### SGCluster.spec.postgres
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

This section allows to configure Postgres features


{{< crd-properties >}}
[
  {
    "description": "Postgres version used on the cluster. It is either of:\n\n*  The string 'latest', which automatically targets the latest major.minor Postgres version.\n*  A major version, like '14' or '13', which targets that major version and the latest minor version.\n*  A specific major.minor version, like '14.4'.\n\nChanging this value will trigger an SGCluster's Pods restart with the following conditions:\n\n* When a restart SGDbOps targeting this SGCluster is created the Pods will be restarted when the SGDbOps is started.\n* When SGCluster is annotated with `stackgres.io/rollout` with value `always` the Pods will be restarted ASAP.\n* When SGCluster is annotated with `stackgres.io/schedule` with value syntax `\u003ccron  expression\u003e[:\u003cduration\u003e][,...]` the Pods will be restarted in the time windows specified by the cron expressions and durations values defined.\n* When SGCluster is annotated with `stackgres.io/rollout` with value `never` the Pods will never be restarted (even creating an SGDbOps).",
    "name": "version",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecpostgresextensionsindex",
    "description": "StackGres support deploy of extensions at runtime by simply adding an entry to this array. A deployed extension still\nrequires the creation in a database using the [`CREATE EXTENSION`](https://www.postgresql.org/docs/current/sql-createextension.html)\nstatement. After an extension is deployed correctly it will be present until removed and the cluster restarted.\n\nA cluster restart is required for:\n\n* Extensions that requires to add an entry to [`shared_preload_libraries`](https://postgresqlco.nf/en/doc/param/shared_preload_libraries/) configuration parameter.\n* Upgrading extensions that overwrite any file that is not the extension''s control file or extension''s script file.\n* Removing extensions. Until the cluster is not restarted a removed extension will still be available.\n* Install of extensions that require extra mount. After installed the cluster will require to be restarted.",
    "name": "extensions",
    "required": false,
    "type": "[]object"
  },
  {
    "default": "vanilla",
    "description": "Postgres flavor used on the cluster. It is either of:\n\n  * `vanilla` will use the [Official Postgres](https://www.postgresql.org/)\n  * `babelfish` will use the [Babelfish for Postgres](https://babelfish-for-postgresql.github.io/babelfish-for-postgresql/).\n\nIf not specified then the vanilla Postgres will be used for the cluster.\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "flavor",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecpostgresssl",
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

#### SGCluster.spec.postgres.extensions[index]
<sup><sup>[↩ Parent](#sgclusterspecpostgres)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The name of the extension to deploy.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "default": "com.ongres",
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
    "description": "The version of the extension to deploy. If not specified latest version will be used by default. If latest version can not be determined (if extension uses non standard versioning) a version will be picked up randomly.",
    "name": "version",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

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



{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpostgressslcertificatesecretkeyselector",
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
    "description": "Allow to enable SSL for connections to Postgres. By default is `true`.\n\nIf `true` certificate and private key will be auto-generated unless fields `certificateSecretKeySelector` and `privateKeySecretKeySelector` are specified.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgclusterspecpostgressslprivatekeysecretkeyselector",
    "description": "Secret key selector for the private key used for SSL connections.",
    "name": "privateKeySecretKeySelector",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.postgres.ssl.certificateSecretKeySelector
<sup><sup>[↩ Parent](#sgclusterspecpostgresssl)</sup></sup>

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

##### SGCluster.spec.postgres.ssl.privateKeySecretKeySelector
<sup><sup>[↩ Parent](#sgclusterspecpostgresssl)</sup></sup>

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

### SGCluster.spec.autoscaling
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

This section allows to configure horizontal and vertical Pod autoscaling for the SGCluster's Pods.

Horizontal Pod Autoscaling will use replicas connections usage (active connections / max connections) as the metric to control the upscale or downscale of the replicas.
Horizontal Pod Autoscaling require the [KEDA operator](https://github.com/kedacore/keda) to be installed in the Kubernetes cluster.

Vertical Pod Autoscaling will use cpu and memory usage as the metric to control the upscale or downscale of the Pod requests and limits resources.
Vertical Pod Autoscaling requires the [Vertical Pod Autoscaler operator](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler) to be installed in the Kubernetes cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecautoscalinghorizontal",
    "description": "Section to configure horizontal Pod autoscaling aspects.",
    "name": "horizontal",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecautoscalingmaxallowed",
    "description": "Allow to define the higher bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "maxAllowed",
    "required": false,
    "type": "object"
  },
  {
    "description": "The total maximum number of instances that the SGCluster will have (including the primary instance).\n\nThis field is ignored when horizontal Pod autoscaling is disabled.",
    "minimum": 2,
    "name": "maxInstances",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgclusterspecautoscalingminallowed",
    "description": "Allow to define the lower bound for Pod resources of patroni, pgbouncer and envoy containers",
    "name": "minAllowed",
    "required": false,
    "type": "object"
  },
  {
    "description": "The total minimum number of instances that the SGCluster will have (including the primary instance).\n\nThis field is ignored when horizontal Pod autoscaling is disabled.",
    "minimum": 2,
    "name": "minInstances",
    "required": false,
    "type": "integer"
  },
  {
    "default": "all",
    "description": "Allow to enable or disable any of horizontal and vertical Pod autoscaling.\n\nPossible values are:\n* `all`: both horizontal and vertical Pod autoscaling will be enabled (default)\n* `horizontal`: only horizontal Pod autoscaling will be enabled\n* `vertical`: only vertical Pod autoscaling will be enabled\n* `none`: all autoscaling will be disabled",
    "name": "mode",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecautoscalingvertical",
    "description": "Section to configure vertical Pod autoscaling aspects.",
    "name": "vertical",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.autoscaling.horizontal
<sup><sup>[↩ Parent](#sgclusterspecautoscaling)</sup></sup>

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

#### SGCluster.spec.autoscaling.maxAllowed
<sup><sup>[↩ Parent](#sgclusterspecautoscaling)</sup></sup>

Allow to define the higher bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecautoscalingmaxallowedenvoy",
    "description": "Allow to define the higher bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecautoscalingmaxallowedpatroni",
    "description": "Allow to define the higher bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecautoscalingmaxallowedpgbouncer",
    "description": "Allow to define the higher bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.autoscaling.maxAllowed.envoy
<sup><sup>[↩ Parent](#sgclusterspecautoscalingmaxallowed)</sup></sup>

Allow to define the higher bound for Pod resources of envoy container


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

##### SGCluster.spec.autoscaling.maxAllowed.patroni
<sup><sup>[↩ Parent](#sgclusterspecautoscalingmaxallowed)</sup></sup>

Allow to define the higher bound for Pod resources of patroni container


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

##### SGCluster.spec.autoscaling.maxAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgclusterspecautoscalingmaxallowed)</sup></sup>

Allow to define the higher bound for Pod resources of pgbouncer container


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

#### SGCluster.spec.autoscaling.minAllowed
<sup><sup>[↩ Parent](#sgclusterspecautoscaling)</sup></sup>

Allow to define the lower bound for Pod resources of patroni, pgbouncer and envoy containers


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecautoscalingminallowedenvoy",
    "description": "Allow to define the lower bound for Pod resources of envoy container",
    "name": "envoy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecautoscalingminallowedpatroni",
    "description": "Allow to define the lower bound for Pod resources of patroni container",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecautoscalingminallowedpgbouncer",
    "description": "Allow to define the lower bound for Pod resources of pgbouncer container",
    "name": "pgbouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.autoscaling.minAllowed.envoy
<sup><sup>[↩ Parent](#sgclusterspecautoscalingminallowed)</sup></sup>

Allow to define the lower bound for Pod resources of envoy container


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

##### SGCluster.spec.autoscaling.minAllowed.patroni
<sup><sup>[↩ Parent](#sgclusterspecautoscalingminallowed)</sup></sup>

Allow to define the lower bound for Pod resources of patroni container


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

##### SGCluster.spec.autoscaling.minAllowed.pgbouncer
<sup><sup>[↩ Parent](#sgclusterspecautoscalingminallowed)</sup></sup>

Allow to define the lower bound for Pod resources of pgbouncer container


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

#### SGCluster.spec.autoscaling.vertical
<sup><sup>[↩ Parent](#sgclusterspecautoscaling)</sup></sup>

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

### SGCluster.spec.configurations
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Cluster custom configurations.

**Example:**


```yaml

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



{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationsbackupsindex",
    "description": "List of backups configurations for this SGCluster",
    "name": "backups",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecconfigurationsbinding",
    "description": "This section allows to specify the properties of [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).\n If not specified, then some default will be used.\n\nFor more information see https://servicebinding.io/spec/core/1.0.0/",
    "name": "binding",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentials",
    "description": "Allow to specify custom credentials for Postgres users and Patroni REST API",
    "name": "credentials",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationsobservability",
    "description": "Allow to specify Observability configuration (related to logs, metrics and traces)",
    "name": "observability",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationspatroni",
    "description": "Allow to specify Patroni configuration that will extend the generated one",
    "name": "patroni",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationspooling",
    "description": "",
    "name": "pooling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationspostgres",
    "description": "Section for postgres configuration.",
    "name": "postgres",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationspostgresexporter",
    "description": "Section to configure the [postgres_exporter](https://github.com/prometheus-community/postgres_exporter) component.",
    "name": "postgresExporter",
    "required": false,
    "type": "object"
  },
  {
    "description": "Name of the [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig) used for this cluster.\n\nEach pod contains a sidecar with a connection pooler (currently: [PgBouncer](https://www.pgbouncer.org/)). The connection pooler is implemented as a sidecar.\n\nIf not set, a default configuration will be used. Disabling connection pooling altogether is possible if the disableConnectionPooling property of the pods object is set to true.\n\n**Changing this field may require a restart.**",
    "name": "sgPoolingConfig",
    "requireRestart": true,
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the [SGPostgresConfig](https://stackgres.io/doc/latest/reference/crd/sgpgconfig) used for the cluster.\n\nIt must exist. When not set, a default Postgres config, for the major version selected, is used.\n\n**Changing this field may require a restart.**",
    "name": "sgPostgresConfig",
    "requireRestart": true,
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.configurations.backups[index]
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Backup configuration for this SGCluster


{{< crd-properties >}}
[
  {
    "description": "Name of the [SGObjectStorage](https://stackgres.io/doc/latest/reference/crd/sgobjectstorage) to use for the cluster.\n\nIt defines the location in which the backups will be stored.",
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
    "description": "Continuous Archiving backups are composed of periodic *base backups* and all the WAL segments produced in between those base backups. This parameter specifies at what time and with what frequency to start performing a new base backup.\n\nUse cron syntax (`m h dom mon dow`) for this parameter, i.e., 5 values separated by spaces:\n*  `m`: minute, 0 to 59.\n*  `h`: hour, 0 to 23.\n*  `dom`: day of month, 1 to 31 (recommended not to set it higher than 28).\n*  `mon`: month, 1 to 12.\n*  `dow`: day of week, 0 to 7 (0 and 7 both represent Sunday).\n\nAlso ranges of values (`start-end`), the symbol `*` (meaning `first-last`) or even `*/N`, where `N` is a number, meaning \"\"every `N`, may be used. All times are UTC. It is recommended to avoid 00:00 as base backup time, to avoid overlapping with any other external operations happening at this time.\n\nIf not set, full backups are never performed automatically.",
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
    "description": "The path where the backup is stored. If not set this field is filled up by the operator.\n\nWhen provided will indicate where the backups and WAL files will be stored.\n\n\u003e **WARNING**: Most users should leave this field empty since having it manually set could be dangerous. If the value is repeated due to re-creating an SGCluster or\n re-using the same value in another SGCluster and you may get a mixed WAL history with unrecoverable backups.",
    "name": "path",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecconfigurationsbackupsindexperformance",
    "description": "Configuration that affects the backup network and disk usage performance.",
    "name": "performance",
    "required": false,
    "type": "object"
  },
  {
    "default": 300,
    "description": "Allow to set a timeout for the reconciliation process that take place after the backup.\n\nIf not set defaults to 300 (5 minutes). If set to 0 it will disable timeout.\n\nFailure of reconciliation will not make the backup fail and will be re-tried the next time a SGBackup\n or shecduled backup Job take place.",
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
    "default": 5,
    "description": "When an automatic retention policy is defined to delete old base backups, this parameter specifies the number of base backups to keep, in a sliding window.\n\nConsequently, the time range covered by backups is `periodicity*retention`, where `periodicity` is the separation between backups as specified by the `cronSchedule` property.\n\nDefault is 5.",
    "minimum": 1,
    "name": "retention",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Allow to set a timeout for the backup creation.\n\nIf not set it will be disabled and the backup operation will continue until the backup completes or fail. If set to 0 is the same as not being set.\n\nMake sure to set a reasonable high value in order to allow for any unexpected delays during backup creation (network low bandwidth, disk low throughput and so forth).",
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

##### SGCluster.spec.configurations.backups[index].performance
<sup><sup>[↩ Parent](#sgclusterspecconfigurationsbackupsindex)</sup></sup>

Configuration that affects the backup network and disk usage performance.


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

#### SGCluster.spec.configurations.binding
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

This section allows to specify the properties of [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).
 If not specified, then some default will be used.

For more information see https://servicebinding.io/spec/core/1.0.0/


{{< crd-properties >}}
[
  {
    "description": "Allow to specify the database name. If not specified, then the default value is `postgres`",
    "name": "database",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecconfigurationsbindingpassword",
    "description": "Allow to reference Secret that contains the user's password. If not specified, then the superuser password will be used.",
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
    "description": "Allow to specify the username. If not specified, then the superuser username will be used.",
    "name": "username",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.binding.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationsbinding)</sup></sup>

Allow to reference Secret that contains the user's password. If not specified, then the superuser password will be used.

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

#### SGCluster.spec.configurations.credentials
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Allow to specify custom credentials for Postgres users and Patroni REST API


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationscredentialspatroni",
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials for patroni REST API.\n\n**Changing this field may require a restart.**",
    "name": "patroni",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentialsusers",
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the users.\n\n**Changing this field may require a manual modification of the database users to reflect the new values specified.**\n\nIn particular you may have to create those users if username is changed or alter password if it is changed. Here are the SQL commands to perform such operation (replace\n  default usernames with the new ones and `***` with their respective passwords):\n\n* Superuser username changed:\n\n```\n\nCREATE ROLE postgres;\n\n```\n\n* Superuser password changed:\n\n```\n\nALTER ROLE postgres WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS PASSWORD '***';\n\n```\n\n* Replication username changed:\n\n```\n\nCREATE ROLE replicator;\n\n```\n\n* Replication password changed:\n\n```\n\nALTER ROLE replicator WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN REPLICATION NOBYPASSRLS PASSWORD '***';\n\n```\n\n* Authenticator username changed:\n\n```\n\nCREATE ROLE authenticator;\n\n```\n\n* Authenticator password changed:\n\n```\n\nALTER ROLE authenticator WITH SUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '***';\n\n```\n\n\n**Changing this field may require a restart.**",
    "name": "users",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.credentials.patroni
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentials)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials for patroni REST API.

**Changing this field may require a restart.**


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationscredentialspatronirestapipassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password for the patroni REST API.",
    "name": "restApiPassword",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.credentials.patroni.restApiPassword
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialspatroni)</sup></sup>

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

##### SGCluster.spec.configurations.credentials.users
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentials)</sup></sup>

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
    "anchor": "sgclusterspecconfigurationscredentialsusersauthenticator",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.",
    "name": "authenticator",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentialsusersmonitor",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the monitor user used by the Prometheus Postgres exporter to collect metrics.",
    "name": "monitor",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentialsusersreplication",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.",
    "name": "replication",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentialsuserssuperuser",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).",
    "name": "superuser",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.credentials.users.authenticator
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationscredentialsusersauthenticatorpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentialsusersauthenticatorusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.credentials.users.authenticator.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersauthenticator)</sup></sup>

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

##### SGCluster.spec.configurations.credentials.users.authenticator.username
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersauthenticator)</sup></sup>

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

##### SGCluster.spec.configurations.credentials.users.monitor
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the monitor user used by the Prometheus Postgres exporter to collect metrics.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationscredentialsusersmonitorpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentialsusersmonitorusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.credentials.users.monitor.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersmonitor)</sup></sup>

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

##### SGCluster.spec.configurations.credentials.users.monitor.username
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersmonitor)</sup></sup>

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

##### SGCluster.spec.configurations.credentials.users.replication
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationscredentialsusersreplicationpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentialsusersreplicationusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.credentials.users.replication.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersreplication)</sup></sup>

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

##### SGCluster.spec.configurations.credentials.users.replication.username
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusersreplication)</sup></sup>

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

##### SGCluster.spec.configurations.credentials.users.superuser
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationscredentialsuserssuperuserpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "requireRestart": true,
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecconfigurationscredentialsuserssuperuserusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.credentials.users.superuser.password
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsuserssuperuser)</sup></sup>

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

##### SGCluster.spec.configurations.credentials.users.superuser.username
<sup><sup>[↩ Parent](#sgclusterspecconfigurationscredentialsuserssuperuser)</sup></sup>

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

#### SGCluster.spec.configurations.observability
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Allow to specify Observability configuration (related to logs, metrics and traces)

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
    "description": "Indicate the receiver name (for type prometheus) in the configuration for the collector scraper (if not specified the default empty name will be used).",
    "name": "receiver",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.configurations.patroni
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

Allow to specify Patroni configuration that will extend the generated one

{{< crd-properties >}}
[
  {
    "description": "Allow to specify Patroni dynamic configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/dynamic_configuration.html\n\nThe following configuration fields will be ignored:\n\n* synchronous_mode\n* synchronous_mode_strict\n* postgresql (except pg_hba section)\n* standby_cluster\n\n\u003e NOTE: pg_hba section will be appended with some rules required to make the cluster working as expected.",
    "name": "dynamicConfig",
    "required": false,
    "type": "object"
  },
  {
    "description": "Allow to specify Patroni configuration that will overwrite the generated one. See https://patroni.readthedocs.io/en/latest/yaml_configuration.html\n\nThe following configuration fields will be ignored:\n\n* name\n* namespace\n* log\n* bootstrap\n* citus\n* postgresql # with the exception of postgresql.callbacks, postgresql.pre_promote, postgresql.before_stop and postgresql.pg_ctl_timeout\n* restapi\n* ctl\n* watchdog\n* tags\n\n**This field can only be set on creation.**",
    "immutable": true,
    "name": "initialConfig",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.configurations.pooling
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationspoolingpgbouncer",
    "description": "",
    "name": "pgBouncer",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.pooling.pgBouncer
<sup><sup>[↩ Parent](#sgclusterspecconfigurationspooling)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecconfigurationspoolingpgbouncerpgbouncerini",
    "description": "The `pgbouncer.ini` parameters that overwrite those specified in the referenced [SGPoolingConfig](https://stackgres.io/doc/latest/reference/crd/sgpoolconfig).\n\n**Changing this field may require a restart.**",
    "name": "pgbouncer.ini",
    "requireRestart": true,
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.configurations.pooling.pgBouncer.pgbouncer.ini
<sup><sup>[↩ Parent](#sgclusterspecconfigurationspoolingpgbouncer)</sup></sup>

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

#### SGCluster.spec.configurations.postgres
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

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

#### SGCluster.spec.configurations.postgresExporter
<sup><sup>[↩ Parent](#sgclusterspecconfigurations)</sup></sup>

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

### SGCluster.spec.initialData
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Cluster initialization data options. Cluster may be initialized empty, or from a backup restoration.

**This field can only be set on creation.**


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecinitialdatarestore",
    "description": "This section allows to restore a cluster from an existing copy of the metadata and data.",
    "immutable": true,
    "name": "restore",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecinitialdatascriptsindex",
    "description": "**Deprecated** use instead .spec.managedSql with SGScript.\n\nA list of SQL scripts executed in sequence, exactly once, when the database is bootstrap and/or after restore is completed.",
    "immutable": true,
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.initialData.restore
<sup><sup>[↩ Parent](#sgclusterspecinitialdata)</sup></sup>

This section allows to restore a cluster from an existing copy of the metadata and data.


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
    "anchor": "sgclusterspecinitialdatarestorefrombackup",
    "description": "From which backup to restore and how the process is configured",
    "immutable": true,
    "name": "fromBackup",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

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



{{< crd-properties >}}
[
  {
    "description": "When set to the name of an existing [SGBackup](https://stackgres.io/doc/latest/reference/crd/sgbackup), the cluster is initialized by restoring the\n  backup data to it. If not set, the cluster is initialized empty. The selected backup must be in the same namespace.",
    "immutable": true,
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecinitialdatarestorefrombackuppointintimerecovery",
    "description": "Using Point-in-Time Recovery (PITR) it is possible to restore the database to its state at any moment in the past by setting `restoreToTimestamp`\n  to a value between the timestamps at which your chosen SGBackup and the subsequent one were taken. If the chosen SGBackup is the latest one, the\n  `restoreToTimestamp` value can be between the timestamps at which that last SGBackup was taken and the current one.\n\nSee also: https://www.postgresql.org/docs/current/continuous-archiving.html",
    "immutable": true,
    "name": "pointInTimeRecovery",
    "required": false,
    "type": "object"
  },
  {
    "description": "Specify the [recovery_target](https://postgresqlco.nf/doc/en/param/recovery_target/) that specifies that recovery should end as soon as a consistent\n  state is reached, i.e., as early as possible. When restoring from an online backup, this means the point where taking the backup ended.\n\n  Technically, this is a string parameter, but 'immediate' is currently the only allowed value.",
    "immutable": true,
    "name": "target",
    "required": false,
    "type": "string"
  },
  {
    "description": "Specify the [recovery_target_inclusive](https://postgresqlco.nf/doc/en/param/recovery_target_timeline/) to stop recovery just after the specified\n  recovery target (true), or just before the recovery target (false). Applies when targetLsn, pointInTimeRecovery, or targetXid is specified. This\n  setting controls whether transactions having exactly the target WAL location (LSN), commit time, or transaction ID, respectively, will be included\n  in the recovery. Default is true.",
    "immutable": true,
    "name": "targetInclusive",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "[recovery_target_lsn](https://postgresqlco.nf/doc/en/param/recovery_target_lsn/) specifies the LSN of the write-ahead log location up to which\n  recovery will proceed. The precise stopping point is also influenced by targetInclusive. This parameter is parsed using the system data type\n  pg_lsn.",
    "immutable": true,
    "name": "targetLsn",
    "required": false,
    "type": "string"
  },
  {
    "description": "[recovery_target_name](https://postgresqlco.nf/doc/en/param/recovery_target_name/) specifies the named restore point\n  (created with pg_create_restore_point()) to which recovery will proceed.",
    "immutable": true,
    "name": "targetName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Specify the [recovery_target_timeline](https://postgresqlco.nf/doc/en/param/recovery_target_timeline/) to recover into a particular timeline.\n  The default is to recover along the same timeline that was current when the base backup was taken. Setting this to latest recovers to the latest\n  timeline found in the archive, which is useful in a standby server. Other than that you only need to set this parameter in complex re-recovery\n  situations, where you need to return to a state that itself was reached after a point-in-time recovery.",
    "immutable": true,
    "name": "targetTimeline",
    "required": false,
    "type": "string"
  },
  {
    "description": "[recovery_target_xid](https://postgresqlco.nf/doc/en/param/recovery_target_xid/) specifies the transaction ID up to which recovery will proceed.\n  Keep in mind that while transaction IDs are assigned sequentially at transaction start, transactions can complete in a different numeric order.\n  The transactions that will be recovered are those that committed before (and optionally including) the specified one. The precise stopping point\n  is also influenced by targetInclusive.",
    "immutable": true,
    "name": "targetXid",
    "required": false,
    "type": "string"
  },
  {
    "description": "When set to the UID of an existing [SGBackup](https://stackgres.io/doc/latest/reference/crd/sgbackup), the cluster is initialized by restoring the\n  backup data to it. If not set, the cluster is initialized empty. This field is deprecated.",
    "immutable": true,
    "name": "uid",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.initialData.restore.fromBackup.pointInTimeRecovery
<sup><sup>[↩ Parent](#sgclusterspecinitialdatarestorefrombackup)</sup></sup>

Using Point-in-Time Recovery (PITR) it is possible to restore the database to its state at any moment in the past by setting `restoreToTimestamp`
  to a value between the timestamps at which your chosen SGBackup and the subsequent one were taken. If the chosen SGBackup is the latest one, the
  `restoreToTimestamp` value can be between the timestamps at which that last SGBackup was taken and the current one.

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

#### SGCluster.spec.initialData.scripts[index]
<sup><sup>[↩ Parent](#sgclusterspecinitialdata)</sup></sup>

**Deprecated** use instead .spec.managedSql with SGScript.

Scripts are executed in auto-commit mode with the user `postgres` in the specified database (or in database `postgres` if not specified).

Fields `script` and `scriptFrom` are mutually exclusive and only one of them is required.


{{< crd-properties >}}
[
  {
    "description": "Database where the script is executed. Defaults to the `postgres` database, if not specified.",
    "immutable": true,
    "name": "database",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the script. Must be unique across this SGCluster.",
    "immutable": true,
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "Raw SQL script to execute. This field is mutually exclusive with `scriptFrom` field.",
    "immutable": true,
    "name": "script",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecinitialdatascriptsindexscriptfrom",
    "description": "Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.\n\nFields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.",
    "immutable": true,
    "name": "scriptFrom",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.initialData.scripts[index].scriptFrom
<sup><sup>[↩ Parent](#sgclusterspecinitialdatascriptsindex)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecinitialdatascriptsindexscriptfromconfigmapkeyref",
    "description": "A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.",
    "immutable": true,
    "name": "configMapKeyRef",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecinitialdatascriptsindexscriptfromsecretkeyref",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.",
    "immutable": true,
    "name": "secretKeyRef",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.initialData.scripts[index].scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgclusterspecinitialdatascriptsindexscriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.


{{< crd-properties >}}
[
  {
    "description": "The key name within the ConfigMap that contains the SQL script to execute.",
    "immutable": true,
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "The name of the ConfigMap that contains the SQL script to execute.",
    "immutable": true,
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.initialData.scripts[index].scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgclusterspecinitialdatascriptsindexscriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "immutable": true,
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "immutable": true,
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGCluster.spec.managedSql
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

This section allows to reference SQL scripts that will be applied to the cluster live.


{{< crd-properties >}}
[
  {
    "default": false,
    "description": "If true, when any entry of any `SGScript` fail will not prevent subsequent `SGScript` from being executed. By default is `false`.",
    "name": "continueOnSGScriptError",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgclusterspecmanagedsqlscriptsindex",
    "description": "A list of script references that will be executed in sequence.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgclusterspecmanagedsql)</sup></sup>

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

### SGCluster.spec.metadata
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Metadata information for cluster created resources.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecmetadataannotations",
    "description": "Custom Kubernetes [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to be passed to resources created and managed by StackGres.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecmetadatalabels",
    "description": "Custom Kubernetes [labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) to be passed to resources created and managed by StackGres.",
    "name": "labels",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

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

### SGCluster.spec.nonProductionOptions
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "It is a best practice, on non-containerized environments, when running production workloads, to run each database server on a different server (virtual or physical), i.e., not to co-locate more than one database server per host.\n\nThe same best practice applies to databases on containers. By default, StackGres will not allow to run more than one StackGres pod on a given Kubernetes node. Set this property to true to allow more than one StackGres pod per node.\n\nThis property default value may be changed depending on the value of field `.spec.profile`.\n\n**Changing this field may require a restart.**",
    "name": "disableClusterPodAntiAffinity",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "It is a best practice, on containerized environments, when running production workloads, to enforce container's resources requirements.\n\nBy default, StackGres will configure resource requirements for all the containers. Set this property to true to prevent StackGres from setting container's resources requirements (except for patroni container, see `disablePatroniResourceRequirements`).\n\nThis property default value may be changed depending on the value of field `.spec.profile`.\n\n**Changing this field may require a restart.**",
    "name": "disableClusterResourceRequirements",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "It is a best practice, on containerized environments, when running production workloads, to enforce container's resources requirements.\n\nThe same best practice applies to databases on containers. By default, StackGres will configure resource requirements for patroni container. Set this property to true to prevent StackGres from setting patroni container's resources requirement.\n\nThis property default value may be changed depending on the value of field `.spec.profile`.\n\n**Changing this field may require a restart.**",
    "name": "disablePatroniResourceRequirements",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less cpu than it requires. It also allow to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.\n\nBy default, StackGres will configure cpu requirements to have the same limit and request for all the containers. Set this property to true to prevent StackGres from setting container's cpu requirements request equals to the limit (except for patroni container, see `enablePatroniCpuRequests`)\n  when `.spec.requests.containers.\u003ccontainer name\u003e.cpu` `.spec.requests.initContainers.\u003ccontainer name\u003e.cpu` is configured in the referenced `SGInstanceProfile`.\n\n**Changing this field may require a restart.**",
    "name": "enableSetClusterCpuRequests",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less memory than it requires.\n\nBy default, StackGres will configure memory requirements to have the same limit and request for all the containers. Set this property to true to prevent StackGres from setting container's memory requirements request equals to the limit (except for patroni container, see `enablePatroniCpuRequests`)\n  when `.spec.requests.containers.\u003ccontainer name\u003e.memory` `.spec.requests.initContainers.\u003ccontainer name\u003e.memory` is configured in the referenced `SGInstanceProfile`.\n\n**Changing this field may require a restart.**",
    "name": "enableSetClusterMemoryRequests",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's cpu requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less cpu than it requires. It also allow to set [static CPU management policy](https://kubernetes.io/docs/tasks/administer-cluster/cpu-management-policies/#static-policy) that allows to guarantee a pod the usage exclusive CPUs on the node.\n\nBy default, StackGres will configure cpu requirements to have the same limit and request for the patroni container. Set this property to true to prevent StackGres from setting patroni container's cpu requirements request equals to the limit\n  when `.spec.requests.cpu` is configured in the referenced `SGInstanceProfile`.\n\n**Changing this field may require a restart.**",
    "name": "enableSetPatroniCpuRequests",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "**Deprecated** this value is ignored and you can consider it as always `true`.\n\nOn containerized environments, when running production workloads, enforcing container's memory requirements request to be equals to the limit allow to achieve the highest level of performance. Doing so, reduces the chances of leaving\n  the workload with less memory than it requires.\n\nBy default, StackGres will configure memory requirements to have the same limit and request for the patroni container. Set this property to true to prevent StackGres from setting patroni container's memory requirements request equals to the limit\n  when `.spec.requests.memory` is configured in the referenced `SGInstanceProfile`.\n\n**Changing this field may require a restart.**",
    "name": "enableSetPatroniMemoryRequests",
    "requireRestart": true,
    "required": false,
    "type": "boolean"
  },
  {
    "description": "A list of StackGres feature gates to enable (not suitable for a production environment).\n\nAvailable feature gates are:\n* `babelfish-flavor`: Allow to use `babelfish` flavor.",
    "name": "enabledFeatureGates",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGCluster.spec.postgresServices
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Kubernetes [services](https://kubernetes.io/docs/concepts/services-networking/service/) created or managed by StackGres.

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpostgresservicesprimary",
    "description": "Configure the service to the primary with the same name as the SGCluster. A legacy service \n\nProvides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the cluster.\n\nSee also https://kubernetes.io/docs/concepts/services-networking/service/",
    "name": "primary",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecpostgresservicesreplicas",
    "description": "Configure the service to any replica with the name as the SGCluster plus the `-replicas` suffix.\n\nIt provides a stable connection (regardless of replica node failures) to any read-only Postgres server of the cluster. Read-only servers are load-balanced via this service.\n\nSee also https://kubernetes.io/docs/concepts/services-networking/service/",
    "name": "replicas",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.postgresServices.primary
<sup><sup>[↩ Parent](#sgclusterspecpostgresservices)</sup></sup>

Configure the service to the primary with the same name as the SGCluster. A legacy service 

Provides a stable connection (regardless of primary failures or switchovers) to the read-write Postgres server of the cluster.

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
    "description": "The list of custom ports that will be exposed by the service.\n\nThe names of custom ports will be prefixed with the string `c-` so they do not\n  conflict with ports defined for the service.\n\nThe names of target ports will be prefixed with the string `c-` so that the ports\n  that can be referenced in this section will be only those defined under\n  .spec.pods.customContainers[].ports sections were names are also prepended with the same\n  prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#serviceport-v1-core",
    "name": "customPorts",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "default": true,
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
    "anchor": "sgclusterspecpostgresservicesprimarynodeports",
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
    "anchor": "sgclusterspecpostgresservicesprimarysessionaffinityconfig",
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

##### SGCluster.spec.postgresServices.primary.nodePorts
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesprimary)</sup></sup>

nodePorts is a list of ports for exposing a cluster services to the outside world

{{< crd-properties >}}
[
  {
    "description": "the node port that will be exposed to connect to Babelfish instance using SQL Server wire-protocol and T-SQL",
    "name": "babelfish",
    "required": false,
    "type": "integer"
  },
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

##### SGCluster.spec.postgresServices.primary.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesprimary)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpostgresservicesprimarysessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.postgresServices.primary.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesprimarysessionaffinityconfig)</sup></sup>

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

#### SGCluster.spec.postgresServices.replicas
<sup><sup>[↩ Parent](#sgclusterspecpostgresservices)</sup></sup>

Configure the service to any replica with the name as the SGCluster plus the `-replicas` suffix.

It provides a stable connection (regardless of replica node failures) to any read-only Postgres server of the cluster. Read-only servers are load-balanced via this service.

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
    "description": "The list of custom ports that will be exposed by the service.\n\nThe names of custom ports will be prefixed with the string `c-` so they do not\n  conflict with ports defined for the service.\n\nThe names of target ports will be prefixed with the string `c-` so that the ports\n  that can be referenced in this section will be only those defined under\n  .spec.pods.customContainers[].ports sections were names are also prepended with the same\n  prefix.\n\n**Changing this field may require a restart.**\n\nSee: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#serviceport-v1-core",
    "name": "customPorts",
    "requireRestart": true,
    "required": false,
    "type": "[]object"
  },
  {
    "default": true,
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
    "anchor": "sgclusterspecpostgresservicesreplicasnodeports",
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
    "anchor": "sgclusterspecpostgresservicesreplicassessionaffinityconfig",
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

##### SGCluster.spec.postgresServices.replicas.nodePorts
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesreplicas)</sup></sup>

nodePorts is a list of ports for exposing a cluster services to the outside world

{{< crd-properties >}}
[
  {
    "description": "the node port that will be exposed to connect to Babelfish instance using SQL Server wire-protocol and T-SQL",
    "name": "babelfish",
    "required": false,
    "type": "integer"
  },
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

##### SGCluster.spec.postgresServices.replicas.sessionAffinityConfig
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesreplicas)</sup></sup>

SessionAffinityConfig represents the configurations of session affinity.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#sessionaffinityconfig-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecpostgresservicesreplicassessionaffinityconfigclientip",
    "description": "ClientIPConfig represents the configurations of Client IP based session affinity.",
    "name": "clientIP",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.postgresServices.replicas.sessionAffinityConfig.clientIP
<sup><sup>[↩ Parent](#sgclusterspecpostgresservicesreplicassessionaffinityconfig)</sup></sup>

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

### SGCluster.spec.replicateFrom
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

Make the cluster a read-only standby replica allowing to replicate from another PostgreSQL instance and acting as a rely.

Changing this section is allowed to fix issues or to change the replication source.

Removing this section convert the cluster in a normal cluster where the standby leader is converted into a primary instance.

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


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecreplicatefrominstance",
    "description": "Configure replication from a PostgreSQL instance.",
    "name": "instance",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromstorage",
    "description": "Configure replication from an SGObjectStorage using WAL shipping.\n\nThe file structure of the object storage must follow the\n  [WAL-G](https://github.com/wal-g/wal-g) file structure.",
    "name": "storage",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromusers",
    "description": "Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the users.",
    "name": "users",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.replicateFrom.instance
<sup><sup>[↩ Parent](#sgclusterspecreplicatefrom)</sup></sup>

Configure replication from a PostgreSQL instance.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecreplicatefrominstanceexternal",
    "description": "Configure replication from an external PostgreSQL instance.",
    "name": "external",
    "required": false,
    "type": "object"
  },
  {
    "description": "Configure replication from an SGCluster.",
    "name": "sgCluster",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.replicateFrom.instance.external
<sup><sup>[↩ Parent](#sgclusterspecreplicatefrominstance)</sup></sup>

Configure replication from an external PostgreSQL instance.


{{< crd-properties >}}
[
  {
    "description": "The host of the PostgreSQL to replicate from.",
    "name": "host",
    "required": true,
    "type": "string"
  },
  {
    "description": "The port of the PostgreSQL to replicate from.",
    "name": "port",
    "required": true,
    "type": "integer"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.replicateFrom.storage
<sup><sup>[↩ Parent](#sgclusterspecreplicatefrom)</sup></sup>

Configure replication from an SGObjectStorage using WAL shipping.

The file structure of the object storage must follow the
  [WAL-G](https://github.com/wal-g/wal-g) file structure.


{{< crd-properties >}}
[
  {
    "description": "The path in the SGObjectStorage to replicate from.",
    "name": "path",
    "required": true,
    "type": "string"
  },
  {
    "description": "The SGObjectStorage name to replicate from.",
    "name": "sgObjectStorage",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecreplicatefromstorageperformance",
    "description": "Configuration that affects the backup network and disk usage performance during recovery.",
    "name": "performance",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.replicateFrom.storage.performance
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromstorage)</sup></sup>

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

#### SGCluster.spec.replicateFrom.users
<sup><sup>[↩ Parent](#sgclusterspecreplicatefrom)</sup></sup>

Kubernetes [SecretKeySelectors](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the users.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecreplicatefromusersauthenticator",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.",
    "name": "authenticator",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromusersreplication",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.",
    "name": "replication",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromuserssuperuser",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).",
    "name": "superuser",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromusersmonitor",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the monitor user used by the Prometheus Postgres exporter to collect metrics.",
    "name": "monitor",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.replicateFrom.users.authenticator
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the authenticator user used by pgbouncer to authenticate other users.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecreplicatefromusersauthenticatorpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromusersauthenticatorusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.replicateFrom.users.authenticator.password
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersauthenticator)</sup></sup>

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

##### SGCluster.spec.replicateFrom.users.authenticator.username
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersauthenticator)</sup></sup>

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

##### SGCluster.spec.replicateFrom.users.replication
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the replication user used to replicate from the primary cluster and from replicas of this cluster.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecreplicatefromusersreplicationpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromusersreplicationusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.replicateFrom.users.replication.password
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersreplication)</sup></sup>

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

##### SGCluster.spec.replicateFrom.users.replication.username
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersreplication)</sup></sup>

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

##### SGCluster.spec.replicateFrom.users.superuser
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the superuser (usually the postgres user).


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecreplicatefromuserssuperuserpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromuserssuperuserusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.replicateFrom.users.superuser.password
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromuserssuperuser)</sup></sup>

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

##### SGCluster.spec.replicateFrom.users.superuser.username
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromuserssuperuser)</sup></sup>

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

##### SGCluster.spec.replicateFrom.users.monitor
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusers)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the credentials of the monitor user used by the Prometheus Postgres exporter to collect metrics.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecreplicatefromusersmonitorpassword",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the password of the user.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgclusterspecreplicatefromusersmonitorusername",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the username of the user.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.replicateFrom.users.monitor.password
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersmonitor)</sup></sup>

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

##### SGCluster.spec.replicateFrom.users.monitor.username
<sup><sup>[↩ Parent](#sgclusterspecreplicatefromusersmonitor)</sup></sup>

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

### SGCluster.spec.replication
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>

This section allows to configure Postgres replication mode and HA roles groups.

The main replication group is implicit and contains the total number of instances less the sum of all
  instances in other replication groups.

The total number of instances is always specified by `.spec.instances`.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterspecreplicationgroupsindex",
    "description": "StackGres support replication groups where a replication group of a specified number of instances could have different\n  replication role. The main replication group is implicit and contains the total number of instances less the sum of all\n  instances in other replication groups.",
    "name": "groups",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterspecreplicationinitialization",
    "description": "Allow to specify how the replicas are initialized.",
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
    "default": "ha-read",
    "description": "This role is applied to the instances of the implicit replication group that is composed by `.spec.instances` number of instances.\nPossible values are:\n* `ha-read` (default)\n* `ha`\nThe primary instance will be elected among all the replication groups that are either `ha` or `ha-read`.\nOnly if the role is set to `ha-read` instances of main replication group will be exposed via the replicas service.",
    "name": "role",
    "required": false,
    "type": "string"
  },
  {
    "description": "Number of synchronous standby instances. Must be less than the total number of instances. It is set to 1 by default.\n  Only settable if mode is `sync` or `strict-sync`.",
    "minimum": 1,
    "name": "syncInstances",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.replication.groups[index]
<sup><sup>[↩ Parent](#sgclusterspecreplication)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Number of StackGres instances for this replication group.\n\nThe total number of instance of a cluster is always `.spec.instances`. The sum of the instances in all the replication groups must be\n  less than the total number of instances.",
    "minimum": 1,
    "name": "instances",
    "required": true,
    "type": "integer"
  },
  {
    "description": "This role is applied to the instances of this replication group.\nPossible values are:\n* `ha-read`\n* `ha`\n* `readonly`\n* `none`\nThe primary instance will be elected among all the replication groups that are either `ha` or `ha-read`.\nOnly if the role is set to `readonly` or `ha-read` instances of such replication group will be exposed via the replicas service.",
    "name": "role",
    "required": true,
    "type": "string"
  },
  {
    "description": "Minimum number of StackGres instances for this replication group. It is ignored when horizontal Pod autoscaling is disabled (see `.spec.autoscaling`)\n\nThe total minimum number of instance of a cluster is always `.spec.autoscaling.minInstances`. The sum of the minimum instances in all the replication groups must be\n  less than the total minimum number of instances.\n\nWhen this field is set the instances value that is provided by the user it is overwritten using the following formula to calculate it:\n\n\n```\n\n\u003cinstances\u003e = max(\u003cminInstances\u003e, \u003cminInstances\u003e * \u003ccluster instances\u003e / \u003ccluster autoscaling minInstances\u003e)\n\n```",
    "minimum": 1,
    "name": "minInstances",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The name of the replication group. If not set will default to the `group-\u003cindex\u003e`.",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGCluster.spec.replication.initialization
<sup><sup>[↩ Parent](#sgclusterspecreplication)</sup></sup>

Allow to specify how the replicas are initialized.


{{< crd-properties >}}
[
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how old an SGBackup has to be in order to be selected\n to initialize a replica.\n\nWhen `FromExistingBackup` mode is set this field restrict the selection of SGBackup to be used for recovery newer than the\n specified value. \n\nWhen `FromNewlyCreatedBackup` mode is set this field skip the creation SGBackup to be used for recovery if one newer than\n the specified value exists.",
    "name": "backupNewerThan",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterspecreplicationinitializationbackuprestoreperformance",
    "description": "Configuration that affects the backup network and disk usage performance during recovery.",
    "name": "backupRestorePerformance",
    "required": false,
    "type": "object"
  },
  {
    "default": "FromExistingBackup",
    "description": "Allow to specify how the replicas are initialized.\n\nPossible values are:\n\n* `FromPrimary`: When this mode is used replicas will be always created from the primary using `pg_basebackup`.\n* `FromReplica`: When this mode is used replicas will be created from another existing replica using\n `pg_basebackup`. Fallsback to `FromPrimary` if there's no replica or it fails.\n* `FromExistingBackup`: When this mode is used replicas will be created from an existing SGBackup. If `backupNewerThan` is set\n the SGBackup must be newer than its value. When this mode fails to restore an SGBackup it will try with a previous one (if exists).\n Fallsback to `FromReplica` if there's no backup left or it fails.\n* `FromNewlyCreatedBackup`: When this mode is used replicas will be created from a newly created SGBackup.\n Fallsback to `FromExistingBackup` if `backupNewerThan` is set and exists a recent backup newer than its value or it fails.",
    "name": "mode",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGCluster.spec.replication.initialization.backupRestorePerformance
<sup><sup>[↩ Parent](#sgclusterspecreplicationinitialization)</sup></sup>

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

### SGCluster.spec.toInstallPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgclusterspec)</sup></sup>



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

## SGCluster.status
<sup><sup>[↩ Parent](#sgcluster)</sup></sup>

Current status of a StackGres cluster.

{{< crd-properties >}}
[
  {
    "description": "The architecture on which the cluster has been initialized.",
    "name": "arch",
    "required": false,
    "type": "string"
  },
  {
    "description": "The backup paths used",
    "name": "backupPaths",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgclusterstatusbinding",
    "description": "This section follow the schema specified in [Service Binding spec for provisioned service](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).\n\nFor more information see https://servicebinding.io/spec/core/1.0.0/",
    "name": "binding",
    "required": false,
    "type": "object"
  },
  {
    "description": "The Postgres build version used to generate the SGCluster's Pods",
    "name": "buildVersion",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterstatusconditionsindex",
    "description": "",
    "name": "conditions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgclusterstatusdbops",
    "description": "Used by some [SGDbOps](https://stackgres.io/doc/latest/reference/crd/sgdbops) to indicate the operation configuration and status to the operator.",
    "name": "dbOps",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterstatusextensionsindex",
    "description": "The list of Postgres extensions to install.\n\n**This section is filled by the operator.**",
    "name": "extensions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Actual number of instances for the StackGres cluster. Each instance is a Pod containing one Postgres server.",
    "name": "instances",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The custom prefix that is prepended to all labels.",
    "name": "labelPrefix",
    "required": false,
    "type": "string"
  },
  {
    "description": "Actual label selector for instances for the StackGres cluster's Pods to be used by autoscaling.",
    "name": "labelSelector",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterstatusmanagedsql",
    "description": "This section stores the state of referenced SQL scripts that are applied to the cluster live.",
    "name": "managedSql",
    "required": false,
    "type": "object"
  },
  {
    "description": "The operative system on which the cluster has been initialized.",
    "name": "os",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterstatuspodstatusesindex",
    "description": "The list of pod statuses.",
    "name": "podStatuses",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "The Postgres version used to generate the SGCluster's Pods",
    "name": "postgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "Indicates the latest failed backup for the replication initialization.",
    "name": "replicationInitializationFailedSGBackup",
    "required": false,
    "type": "string"
  },
  {
    "description": "The Postgres configuration that is being used by the SGCluster's Pods",
    "name": "sgPostgresConfig",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGCluster.status.binding
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>

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

### SGCluster.status.conditions[index]
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>



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

### SGCluster.status.dbOps
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>

Used by some [SGDbOps](https://stackgres.io/doc/latest/reference/crd/sgdbops) to indicate the operation configuration and status to the operator.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterstatusdbopsmajorversionupgrade",
    "description": "The major version upgrade configuration and status",
    "name": "majorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterstatusdbopsminorversionupgrade",
    "description": "The minor version upgrade configuration and status",
    "name": "minorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "description": "the name of the SGDbOps resource",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterstatusdbopsrestart",
    "description": "The minor version upgrade configuration and status",
    "name": "restart",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgclusterstatusdbopssecurityupgrade",
    "description": "The minor version upgrade configuration and status",
    "name": "securityUpgrade",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGCluster.status.dbOps.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgclusterstatusdbops)</sup></sup>

The major version upgrade configuration and status


{{< crd-properties >}}
[
  {
    "description": "Run `pg_upgrade` with check option instead of performing the real upgrade",
    "name": "check",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Use `--clone` option when running `pg_upgrade`",
    "name": "clone",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Indicates if PostgreSQL data checksum is enabled",
    "name": "dataChecksum",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The PostgreSQL encoding",
    "name": "encoding",
    "required": false,
    "type": "string"
  },
  {
    "description": "The instances that this operation is targetting",
    "name": "initialInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "Use `--link` option when running `pg_upgrade`",
    "name": "link",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The PostgreSQL locale",
    "name": "locale",
    "required": false,
    "type": "string"
  },
  {
    "description": "The primary instance that this operation is targetting",
    "name": "primaryInstance",
    "required": false,
    "type": "string"
  },
  {
    "description": "Indicates to rollback from a previous major version upgrade",
    "name": "rollback",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The source backup path",
    "name": "sourceBackupPath",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgclusterstatusdbopsmajorversionupgradesourcepostgresextensionsindex",
    "description": "The source PostgreSQL extensions",
    "name": "sourcePostgresExtensions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "The source PostgreSQL version",
    "name": "sourcePostgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "The source replication mode",
    "name": "sourceReplicationMode",
    "required": false,
    "type": "string"
  },
  {
    "description": "The source SGPostgresConfig reference",
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

##### SGCluster.status.dbOps.majorVersionUpgrade.sourcePostgresExtensions[index]
<sup><sup>[↩ Parent](#sgclusterstatusdbopsmajorversionupgrade)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The name of the extension to deploy.",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "default": "com.ongres",
    "description": "The id of the publisher of the extension to deploy. If not specified `com.ongres` will be used by default.",
    "name": "publisher",
    "required": false,
    "type": "string"
  },
  {
    "description": "The repository base URL from where to obtain the extension to deploy.",
    "name": "repository",
    "required": false,
    "type": "string"
  },
  {
    "description": "The version of the extension to deploy. If not specified version of `stable` channel will be used by default and if only a version is available that one will be used.",
    "name": "version",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGCluster.status.dbOps.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgclusterstatusdbops)</sup></sup>

The minor version upgrade configuration and status


{{< crd-properties >}}
[
  {
    "description": "The instances that this operation is targetting",
    "name": "initialInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The primary instance that this operation is targetting",
    "name": "primaryInstance",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGCluster.status.dbOps.restart
<sup><sup>[↩ Parent](#sgclusterstatusdbops)</sup></sup>

The minor version upgrade configuration and status


{{< crd-properties >}}
[
  {
    "description": "The instances that this operation is targetting",
    "name": "initialInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The primary instance that this operation is targetting",
    "name": "primaryInstance",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGCluster.status.dbOps.securityUpgrade
<sup><sup>[↩ Parent](#sgclusterstatusdbops)</sup></sup>

The minor version upgrade configuration and status


{{< crd-properties >}}
[
  {
    "description": "The instances that this operation is targetting",
    "name": "initialInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The primary instance that this operation is targetting",
    "name": "primaryInstance",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGCluster.status.extensions[index]
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>



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

### SGCluster.status.managedSql
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>

This section stores the state of referenced SQL scripts that are applied to the cluster live.


{{< crd-properties >}}
[
  {
    "anchor": "sgclusterstatusmanagedsqlscriptsindex",
    "description": "A list of statuses for script references.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGCluster.status.managedSql.scripts[index]
<sup><sup>[↩ Parent](#sgclusterstatusmanagedsql)</sup></sup>

The status of a script reference.

{{< crd-properties >}}
[
  {
    "description": "ISO-8601 datetime of when the script execution had completed (mutually exclusive with `failedAt`).",
    "name": "completedAt",
    "required": false,
    "type": "string"
  },
  {
    "description": "ISO-8601 datetime of when the script execution had failed (mutually exclusive with `completedAt`).",
    "name": "failedAt",
    "required": false,
    "type": "string"
  },
  {
    "description": "Identify the associated `SGScript` entry with the same value in the `id` field.",
    "name": "id",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgclusterstatusmanagedsqlscriptsindexscriptsindex",
    "description": "A list of statuses for script entries of referenced script.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "ISO-8601 datetime of when the script execution has been started.",
    "name": "startedAt",
    "required": false,
    "type": "string"
  },
  {
    "description": "ISO-8601 datetime of when the last script execution occurred. Will be reset each time the referenced `SGScripts` entry will be applied.",
    "name": "updatedAt",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGCluster.status.managedSql.scripts[index].scripts[index]
<sup><sup>[↩ Parent](#sgclusterstatusmanagedsqlscriptsindex)</sup></sup>

The status of a script entry of a referenced script.

{{< crd-properties >}}
[
  {
    "description": "If failed, a message of the failure",
    "name": "failure",
    "required": false,
    "type": "string"
  },
  {
    "description": "If failed, the error code of the failure. See also https://www.postgresql.org/docs/current/errcodes-appendix.html",
    "name": "failureCode",
    "required": false,
    "type": "string"
  },
  {
    "description": "Identify the associated script entry with the same value in the `id` field.",
    "name": "id",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Indicates the number of intents or failures occurred",
    "name": "intents",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The latest version applied",
    "name": "version",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

### SGCluster.status.podStatuses[index]
<sup><sup>[↩ Parent](#sgclusterstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The name of the pod.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgclusterstatuspodstatusesindexinstalledpostgresextensionsindex",
    "description": "The list of Postgres extensions currently installed.",
    "name": "installedPostgresExtensions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "The node name where the pod is running.",
    "name": "nodeName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Indicates if the pod requires restart",
    "name": "pendingRestart",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Indicates if the pod is the elected primary",
    "name": "primary",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Indicates the replication group this Pod belongs to.",
    "name": "replicationGroup",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

#### SGCluster.status.podStatuses[index].installedPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgclusterstatuspodstatusesindex)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The name of the installed extension.",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "The postgres major version of the installed extension.",
    "name": "postgresVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "The id of the publisher of the installed extension.",
    "name": "publisher",
    "required": true,
    "type": "string"
  },
  {
    "description": "The repository base URL from where the extension was installed from.",
    "name": "repository",
    "required": true,
    "type": "string"
  },
  {
    "description": "The version of the installed extension.",
    "name": "version",
    "required": true,
    "type": "string"
  },
  {
    "description": "The build version of the installed extension.",
    "name": "build",
    "required": false,
    "type": "string"
  },
  {
    "description": "The extra mounts of the installed extension.",
    "name": "extraMounts",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

## Sidecar Containers

A sidecar container is a container that adds functionality to PostgreSQL or to the cluster infrastructure.
Currently StackGres implement following sidecar containers:

* `cluster-controller`: this container is always present, and it is not possible to disable it.
 It serves to reconcile local configurations, collects Pod status, and performs local actions (like extensions installation, execution of SGScript entries, etc.).
* `envoy`: this container is disabled by default and will be removed in future versions, you may still enable it.
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
    disableEnvoy: true
```
