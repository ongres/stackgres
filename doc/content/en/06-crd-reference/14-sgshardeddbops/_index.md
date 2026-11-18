---
title: SGShardedDbOps
weight: 14
url: /reference/crd/sgshardeddbops
description: Details about SGShardedDbOps
showToc: true
---

___
**Kind:** SGShardedDbOps

**listKind:** SGShardedDbOpsList

**plural:** sgshardeddbops

**singular:** sgshardeddbops

**shortNames** sgsdo
___

The `SGShardedDbOps` custom resource represents database operations that are performed on a Postgres sharded cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: restart
spec:
  sgShardedCluster: my-cluster
  op: restart
  maxRetries: 1
  restart:
    method: InPlace
```





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
    "description": "SGShardedDbOps",
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
    "anchor": "sgshardeddbopsspec",
    "description": "",
    "name": "spec",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgshardeddbopsstatus",
    "description": "",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGShardedDbOps.spec
<sup><sup>[↩ Parent](#sgshardeddbops)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The kind of operation that will be performed on the SGShardedCluster. Available operations are:\n\n* `resharding`: perform a resharding of the cluster.\n* `restart`: perform a restart of the cluster.\n* `securityUpgrade`: perform a security upgrade of the cluster.\n* `majorVersionUpgrade`: perform a major version upgrade of PostgreSQL.\n* `minorVersionUpgrade`: perform a minor version upgrade of PostgreSQL.",
    "name": "op",
    "required": true,
    "type": "string"
  },
  {
    "description": "The name of SGShardedCluster on which the operation will be performed.",
    "name": "sgShardedCluster",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsspecmajorversionupgrade",
    "description": "Configuration of major version upgrade (see also [`pg_upgrade`](https://www.postgresql.org/docs/current/pgupgrade.html) command)",
    "name": "majorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "description": "The maximum number of retries the operation is allowed to do after a failure.\n\nA value of `0` (zero) means no retries are made. Defaults to: `0`.",
    "name": "maxRetries",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardeddbopsspecminorversionupgrade",
    "description": "Configuration of minor version upgrade\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.\n\nThe delay between the restart of a Postgres instance and the next one is configured in the\n `SGShardedCluster.spec.coordinator.pods.updateStrategy.restartDelay` and\n `SGShardedCluster.spec.workers.pods.updateStrategy.restartDelay` fields.",
    "name": "minorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardeddbopsspecresharding",
    "description": "Configuration for resharding. Resharding a sharded cluster is the operation that moves the data among workers in order to try to\n balance the disk space used in each worker. See also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start",
    "name": "resharding",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardeddbopsspecrestart",
    "description": "Configuration of restart\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.\n\nThe delay between the restart of a Postgres instance and the next one is configured in the\n `SGShardedCluster.spec.coordinator.pods.updateStrategy.restartDelay` and\n `SGShardedCluster.spec.workers.pods.updateStrategy.restartDelay` fields.",
    "name": "restart",
    "required": false,
    "type": "object"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies the initial delay used by the exponential backoff applied when a command run by the operation script is retried and in general by unbounded retries and in some cases without exponential backoff.\n\nDefaults to: `PT1S`.",
    "name": "retryDelay",
    "required": false,
    "type": "string"
  },
  {
    "description": "The maximum number of times a command run by the operation script is retried before failing.\n\nDefaults to: `10`.",
    "name": "retryLimit",
    "required": false,
    "type": "integer"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies the maximum delay the exponential backoff is allowed to reach when a command run by the operation script is retried.\n\nDefaults to: `PT1M`.",
    "name": "retryMaxDelay",
    "required": false,
    "type": "string"
  },
  {
    "description": "An ISO 8601 date, that holds UTC scheduled date of the operation execution.\n\nIf not specified or if the date is in the past, it will be interpreted ASAP.",
    "name": "runAt",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsspecscheduling",
    "description": "Pod custom node scheduling and affinity configuration",
    "name": "scheduling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardeddbopsspecsecurityupgrade",
    "description": "Configuration of security upgrade\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.\n\nThe delay between the restart of a Postgres instance and the next one is configured in the\n `SGShardedCluster.spec.coordinator.pods.updateStrategy.restartDelay` and\n `SGShardedCluster.spec.workers.pods.updateStrategy.restartDelay` fields.",
    "name": "securityUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies a timeout after which the operation execution will be canceled.\n\nIf the operation cannot be performed due to timeout expiration, the condition `Failed` will have a status of `True` and the reason will be `OperationTimedOut`.\n\nIf not specified the operation will never fail for timeout expiration.",
    "name": "timeout",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.spec.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Configuration of major version upgrade (see also [`pg_upgrade`](https://www.postgresql.org/docs/current/pgupgrade.html) command)


{{< crd-properties >}}
[
  {
    "description": "The paths were the backups for the coordinator and each shard are stored. If not set this field is filled up by the operator.\n\nWhen provided will indicate were the backups and WAL files will be stored.\n\nThe path should be different from the current `.spec.configurations.backupPath` value for the target `SGCluster`\n  in order to avoid mixing WAL files of two distinct major versions of postgres.",
    "name": "backupPaths",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "If true does some checks to see if the cluster can perform a major version upgrade without changing any data. Defaults to: `false`.",
    "name": "check",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true use efficient file cloning (also known as \"reflinks\" on some systems) instead of copying files to the new cluster.\nThis can result in near-instantaneous copying of the data files, giving the speed advantages of `link` while leaving the old\n  cluster untouched. This option is mutually exclusive with `link`. Defaults to: `false`.\n\nFile cloning is only supported on some operating systems and file systems. If it is selected but not supported, the pg_upgrade\n  run will error. At present, it is supported on Linux (kernel 4.5 or later) with Btrfs and XFS (on file systems created with\n  reflink support), and on macOS with APFS.",
    "name": "clone",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true use hard links instead of copying files to the new cluster. This option is mutually exclusive with `clone`. Defaults to: `false`.",
    "name": "link",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "When set to `true` the SGShardedDbOps waits for a rollback decision to be set on\n its own `.status.majorVersionUpgrade.rollback` whenever one of the child SGDbOps\n it created pauses (it always creates the child SGDbOps with `manualRollback: true`).\n The operator raises the `WaitingRollback` condition while waiting. The decision is\n then propagated to the waiting child SGDbOps.\n\nWhen `false` the decision for each child is taken automatically: a failed child is\n rolled back and a succeeded child is cleaned up.",
    "name": "manualRollback",
    "required": false,
    "type": "boolean"
  },
  {
    "default": 0,
    "description": "Propagated to each child SGDbOps. See the `maxErrorsAfterContinueOnFailure` field of\n SGDbOps major version upgrade.",
    "name": "maxErrorsAfterContinueOnFailure",
    "required": false,
    "type": "integer"
  },
  {
    "default": 6,
    "description": "Indicates the number of errors that the operation can tolerate after the upgrade\n is performed in order to wait for the Pod to become ready and set the operation\n as completed.",
    "name": "maxErrorsAfterUpgrade",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgshardeddbopsspecmajorversionupgradepostgresextensionsindex",
    "description": "A major version upgrade can not be performed if a required extension is not present for the target major version of the upgrade.\nIn those cases you will have to provide the target extension version of the extension for the target major version of postgres.\nBeware that in some cases it is not possible to upgrade an extension alongside postgres. This is the case for PostGIS or timescaledb.\n In such cases you will have to upgrade the extension before or after the major version upgrade. Please make sure you read the\n documentation of each extension in order to understand if it is possible to upgrade it during a major version upgrade of postgres.",
    "name": "postgresExtensions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "The target postgres version that must have the same major version of the target SGShardedCluster.",
    "name": "postgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "The postgres config that must have the same major version of the target postgres version.",
    "name": "sgPostgresConfig",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsspecmajorversionupgradetoinstallpostgresextensionsindex",
    "description": "The list of Postgres extensions to install.\n\n**This section is filled by the operator.**",
    "name": "toInstallPostgresExtensions",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGShardedDbOps.spec.majorVersionUpgrade.postgresExtensions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecmajorversionupgrade)</sup></sup>



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
    "description": "The version of the extension to deploy. If not specified version of `stable` channel will be used by default and if only a version is available that one will be used.",
    "name": "version",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGShardedDbOps.spec.majorVersionUpgrade.toInstallPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecmajorversionupgrade)</sup></sup>



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

### SGShardedDbOps.spec.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Configuration of minor version upgrade

When a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of
 the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the
 primary is demoted instead of being restarted.

The Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters
 `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is
 decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,
 see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover
 to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is
 performed.

The delay between the restart of a Postgres instance and the next one is configured in the
 `SGShardedCluster.spec.coordinator.pods.updateStrategy.restartDelay` and
 `SGShardedCluster.spec.workers.pods.updateStrategy.restartDelay` fields.


{{< crd-properties >}}
[
  {
    "description": "The method used to perform the minor version upgrade operation. Available methods are:\n\n* `InPlace`: the in-place method does not require more resources than those that are available.\n  In case only an instance of the StackGres cluster is present this means the service disruption will\n  last longer so we encourage using the reduced impact restart and especially for a production environment.\n* `ReducedImpact`: this procedure is the same as the in-place method but requires additional\n  resources in order to spawn a new updated replica that will be removed when the procedure completes.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "description": "The target postgres version that must have the same major version of the target SGShardedCluster.",
    "name": "postgresVersion",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.spec.resharding
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Configuration for resharding. Resharding a sharded cluster is the operation that moves the data among workers in order to try to
 balance the disk space used in each worker. See also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start


{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecreshardingcitus",
    "description": "Citus specific resharding parameters",
    "name": "citus",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGShardedDbOps.spec.resharding.citus
<sup><sup>[↩ Parent](#sgshardeddbopsspecresharding)</sup></sup>

Citus specific resharding parameters


{{< crd-properties >}}
[
  {
    "description": "When set to `true`, shard groups will be moved off nodes that have `shouldhaveshards` set to false. When set to `false` (the default), all shard groups will be balanced across nodes.\nSee also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start",
    "name": "drainOnly",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The name of a strategy in Rebalancer strategy table. Will pick a default one if not specified\nSee also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start",
    "name": "rebalanceStrategy",
    "required": false,
    "type": "string"
  },
  {
    "description": "A float number between 0.0 and 1.0 which indicates the maximum difference ratio of node utilization from average utilization.\nSee also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start",
    "name": "threshold",
    "required": false,
    "type": "number"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.spec.restart
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Configuration of restart

When a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of
 the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the
 primary is demoted instead of being restarted.

The Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters
 `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is
 decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,
 see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover
 to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is
 performed.

The delay between the restart of a Postgres instance and the next one is configured in the
 `SGShardedCluster.spec.coordinator.pods.updateStrategy.restartDelay` and
 `SGShardedCluster.spec.workers.pods.updateStrategy.restartDelay` fields.


{{< crd-properties >}}
[
  {
    "description": "The method used to perform the restart operation. Available methods are:\n\n* `InPlace`: the in-place method does not require more resources than those that are available.\n  In case only an instance of the StackGres cluster for the coordinator or any shard is present\n  this means the service disruption will last longer so we encourage using the reduced impact restart\n   and especially for a production environment.\n* `ReducedImpact`: this procedure is the same as the in-place method but requires additional\n  resources in order to spawn a new updated replica that will be removed when the procedure completes.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "description": "By default all Pods are restarted. Setting this option to `true` allows restarting only those Pods which\n  are in pending restart state as detected by the operation. Defaults to: `false`.",
    "name": "onlyPendingRestart",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.spec.scheduling
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Pod custom node scheduling and affinity configuration

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinity",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core",
    "name": "nodeAffinity",
    "required": false,
    "type": "object"
  },
  {
    "description": "NodeSelector is a selector which must be true for the pod to fit on a node. Selector which must match a node's labels for the pod to be scheduled on that node. More info: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/",
    "name": "nodeSelector",
    "required": false,
    "type": "map[string]string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podaffinity-v1-core",
    "name": "podAffinity",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinity",
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podantiaffinity-v1-core",
    "name": "podAntiAffinity",
    "required": false,
    "type": "object"
  },
  {
    "description": "If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.",
    "name": "priorityClassName",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingtolerationsindex",
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGShardedDbOps.spec.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgshardeddbopsspecscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
    "description": "A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.",
    "name": "preference",
    "required": true,
    "type": "object"
  },
  {
    "description": "Weight associated with matching the corresponding nodeSelectorTerm, in the range 1-100.",
    "format": "int32",
    "name": "weight",
    "required": true,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "The label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "The label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "The label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "The label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

#### SGShardedDbOps.spec.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgshardeddbopsspecscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
    "description": "Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key \u003ctopologyKey\u003e matches that of any node on which a pod of the set of pods is running",
    "name": "podAffinityTerm",
    "required": true,
    "type": "object"
  },
  {
    "description": "weight associated with matching the corresponding podAffinityTerm, in the range 1-100.",
    "format": "int32",
    "name": "weight",
    "required": true,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

{{< crd-properties >}}
[
  {
    "description": "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.",
    "name": "topologyKey",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "matchLabelKeys",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "mismatchLabelKeys",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "namespaceSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means \"this pod's namespace\".",
    "name": "namespaces",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

{{< crd-properties >}}
[
  {
    "description": "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.",
    "name": "topologyKey",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "matchLabelKeys",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "mismatchLabelKeys",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "namespaceSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means \"this pod's namespace\".",
    "name": "namespaces",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

#### SGShardedDbOps.spec.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgshardeddbopsspecscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
    "description": "Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key \u003ctopologyKey\u003e matches that of any node on which a pod of the set of pods is running",
    "name": "podAffinityTerm",
    "required": true,
    "type": "object"
  },
  {
    "description": "weight associated with matching the corresponding podAffinityTerm, in the range 1-100.",
    "format": "int32",
    "name": "weight",
    "required": true,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

{{< crd-properties >}}
[
  {
    "description": "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.",
    "name": "topologyKey",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "matchLabelKeys",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "mismatchLabelKeys",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "namespaceSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means \"this pod's namespace\".",
    "name": "namespaces",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

{{< crd-properties >}}
[
  {
    "description": "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.",
    "name": "topologyKey",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "labelSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "matchLabelKeys",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.",
    "name": "mismatchLabelKeys",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
    "description": "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.",
    "name": "namespaceSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means \"this pod's namespace\".",
    "name": "namespaces",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
    "description": "matchExpressions is a list of label selector requirements. The requirements are ANDed.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is \"key\", the operator is \"In\", and the values array contains only \"value\". The requirements are ANDed.",
    "name": "matchLabels",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

{{< crd-properties >}}
[
  {
    "description": "key is the label key that the selector applies to.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.",
    "name": "operator",
    "required": true,
    "type": "string"
  },
  {
    "description": "values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.",
    "name": "values",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

#### SGShardedDbOps.spec.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecscheduling)</sup></sup>

The pod this Toleration is attached to tolerates any taint that matches the triple <key,value,effect> using the matching operator <operator>.

{{< crd-properties >}}
[
  {
    "description": "Effect indicates the taint effect to match. Empty means match all taint effects. When specified, allowed values are NoSchedule, PreferNoSchedule and NoExecute.",
    "name": "effect",
    "required": false,
    "type": "string"
  },
  {
    "description": "Key is the taint key that the toleration applies to. Empty means match all taint keys. If the key is empty, operator must be Exists; this combination means to match all values and all keys.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator represents a key's relationship to the value. Valid operators are Exists, Equal, Lt, and Gt. Defaults to Equal. Exists is equivalent to wildcard for value, so that a pod can tolerate all taints of a particular category. Lt and Gt perform numeric comparisons (requires feature gate TaintTolerationComparisonOperators).",
    "name": "operator",
    "required": false,
    "type": "string"
  },
  {
    "description": "TolerationSeconds represents the period of time the toleration (which must be of effect NoExecute, otherwise this field is ignored) tolerates the taint. By default, it is not set, which means tolerate the taint forever (do not evict). Zero and negative values will be treated as 0 (evict immediately) by the system.",
    "format": "int64",
    "name": "tolerationSeconds",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Value is the taint value the toleration matches to. If the operator is Exists, the value should be empty, otherwise just a regular string.",
    "name": "value",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.spec.securityUpgrade
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Configuration of security upgrade

When a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of
 the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the
 primary is demoted instead of being restarted.

The Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters
 `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is
 decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,
 see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover
 to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is
 performed.

The delay between the restart of a Postgres instance and the next one is configured in the
 `SGShardedCluster.spec.coordinator.pods.updateStrategy.restartDelay` and
 `SGShardedCluster.spec.workers.pods.updateStrategy.restartDelay` fields.


{{< crd-properties >}}
[
  {
    "description": "The method used to perform the security upgrade operation. Available methods are:\n\n* `InPlace`: the in-place method does not require more resources than those that are available.\n  In case only an instance of the StackGres cluster is present this means the service disruption will\n  last longer so we encourage using the reduced impact restart and especially for a production environment.\n* `ReducedImpact`: this procedure is the same as the in-place method but requires additional\n  resources in order to spawn a new updated replica that will be removed when the procedure completes.",
    "name": "method",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

## SGShardedDbOps.status
<sup><sup>[↩ Parent](#sgshardeddbops)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgshardeddbopsstatusconditionsindex",
    "description": "Possible conditions are:\n\n* Running: to indicate when the operation is actually running\n* Completed: to indicate when the operation has completed successfully\n* Failed: to indicate when the operation has failed",
    "name": "conditions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgshardeddbopsstatusmajorversionupgrade",
    "description": "The results of a major version upgrade",
    "name": "majorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardeddbopsstatusminorversionupgrade",
    "description": "The results of a minor version upgrade",
    "name": "minorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "description": "The number of retries performed by the operation",
    "name": "opRetries",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The ISO 8601 timestamp of when the operation started running",
    "name": "opStarted",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgshardeddbopsstatusrestart",
    "description": "The results of a restart",
    "name": "restart",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgshardeddbopsstatussecurityupgrade",
    "description": "The results of a security upgrade",
    "name": "securityUpgrade",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.status.conditions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Last time the condition transitioned from one status to another.",
    "name": "lastTransitionTime",
    "required": false,
    "type": "string"
  },
  {
    "description": "A human-readable message indicating details about the transition.",
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
    "description": "The reason for the condition last transition.",
    "name": "reason",
    "required": false,
    "type": "string"
  },
  {
    "description": "Status of the condition, one of `True`, `False` or `Unknown`.",
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

### SGShardedDbOps.status.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgshardeddbopsstatus)</sup></sup>

The results of a major version upgrade


{{< crd-properties >}}
[
  {
    "description": "A failure message (when available)",
    "name": "failure",
    "required": false,
    "type": "string"
  },
  {
    "description": "The SGClusters that are pending to be restarted",
    "name": "pendingToRestartSgClusters",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The SGClusters that have been restarted",
    "name": "restartedSgClusters",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "Set when `manualRollback` is `true` to resolve a paused SGShardedDbOps. The value is\n propagated to the waiting child SGDbOps `.status.majorVersionUpgrade.rollback`: `false`\n continues (assuming the upgrade was performed manually), `true` performs a rollback.",
    "name": "rollback",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The postgres version currently used by the primary instance",
    "name": "sourcePostgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "Set when `manualRollback` is `true` and the SGShardedDbOps is waiting for a rollback\n decision because one of its child SGDbOps paused. Indicates whether that child had\n `failed` or `succeeded`.",
    "name": "status",
    "required": false,
    "type": "string"
  },
  {
    "description": "The postgres version that the cluster will be upgraded to",
    "name": "targetPostgresVersion",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.status.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgshardeddbopsstatus)</sup></sup>

The results of a minor version upgrade


{{< crd-properties >}}
[
  {
    "description": "A failure message (when available)",
    "name": "failure",
    "required": false,
    "type": "string"
  },
  {
    "description": "The SGClusters that are pending to be restarted",
    "name": "pendingToRestartSgClusters",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The SGClusters that have been restarted",
    "name": "restartedSgClusters",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The postgres version currently used by the primary instance",
    "name": "sourcePostgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "description": "The postgres version that the cluster will be upgraded (or downgraded) to",
    "name": "targetPostgresVersion",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.status.restart
<sup><sup>[↩ Parent](#sgshardeddbopsstatus)</sup></sup>

The results of a restart


{{< crd-properties >}}
[
  {
    "description": "A failure message (when available)",
    "name": "failure",
    "required": false,
    "type": "string"
  },
  {
    "description": "The SGClusters that are pending to be restarted",
    "name": "pendingToRestartSgClusters",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The SGClusters that have been restarted",
    "name": "restartedSgClusters",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGShardedDbOps.status.securityUpgrade
<sup><sup>[↩ Parent](#sgshardeddbopsstatus)</sup></sup>

The results of a security upgrade


{{< crd-properties >}}
[
  {
    "description": "A failure message (when available)",
    "name": "failure",
    "required": false,
    "type": "string"
  },
  {
    "description": "The SGClusters that are pending to be restarted",
    "name": "pendingToRestartSgClusters",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The SGClusters that have been restarted",
    "name": "restartedSgClusters",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}
