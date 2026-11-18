---
title: SGDbOps
weight: 8
url: /reference/crd/sgdbops
description: Details about SGDbOps
showToc: true
---

___
**Kind:** SGDbOps

**listKind:** SGDbOpsList

**plural:** sgdbops

**singular:** sgdbops

**shortNames** sgdo
___

The `SGDbOps` custom resource represents database operations that are performed on a Postgres cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: benchmark
spec:
 sgCluster: my-cluster
 op: benchmark
 maxRetries: 1
 benchmark:
   type: pgbench
   pgbench:
     databaseSize: 1Gi
     duration: P0DT0H10M0S
     concurrentClients: 10
     threads: 10
   connectionType: primary-service
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
    "description": "SGDbOps",
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
    "anchor": "sgdbopsspec",
    "description": "",
    "name": "spec",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgdbopsstatus",
    "description": "",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGDbOps.spec
<sup><sup>[↩ Parent](#sgdbops)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The kind of operation that will be performed on the SGCluster. Available operations are:\n\n* `benchmark`: run a benchmark on the specified SGCluster and report the results in the status.\n* `vacuum`: perform a [vacuum](https://www.postgresql.org/docs/current/sql-vacuum.html) operation on the specified SGCluster.\n* `repack`: run [`pg_repack`](https://github.com/reorg/pg_repack) command on the specified SGCluster.\n* `majorVersionUpgrade`: perform a major version upgrade of PostgreSQL using [`pg_upgrade`](https://www.postgresql.org/docs/current/pgupgrade.html) command.\n* `restart`: perform a restart of the cluster.\n* `minorVersionUpgrade`: perform a minor version upgrade of PostgreSQL.\n* `securityUpgrade`: perform a security upgrade of the cluster.",
    "name": "op",
    "required": true,
    "type": "string"
  },
  {
    "description": "The name of SGCluster on which the operation will be performed.",
    "name": "sgCluster",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgdbopsspecbenchmark",
    "description": "Configuration of the benchmark",
    "name": "benchmark",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecmajorversionupgrade",
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
    "anchor": "sgdbopsspecminorversionupgrade",
    "description": "Configuration of minor version upgrade\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.\n\nThe delay between the restart of a Postgres instance and the next one is configured in the\n `SGCluster.spec.pods.updateStrategy.restartDelay` field.",
    "name": "minorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecrepack",
    "description": "Configuration of [`pg_repack`](https://github.com/reorg/pg_repack) command",
    "name": "repack",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecrestart",
    "description": "Configuration of restart\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.\n\nThe delay between the restart of a Postgres instance and the next one is configured in the\n `SGCluster.spec.pods.updateStrategy.restartDelay` field.",
    "name": "restart",
    "required": false,
    "type": "object"
  },
  {
    "description": "An ISO 8601 date, that holds UTC scheduled date of the operation execution.\n\nIf not specified or if the date it's in the past, it will be interpreted ASAP.",
    "name": "runAt",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdbopsspecscheduling",
    "description": "Pod custom node scheduling and affinity configuration",
    "name": "scheduling",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecsecurityupgrade",
    "description": "Configuration of security upgrade\n\nWhen a change only require the Postgres instance to be restarted, and not the Pod to be re-created, the Postgres instances of\n the replicas are restarted first and a switchover to the ready replica with the least lag is then performed, so that the\n primary is demoted instead of being restarted.\n\nThe Postgres instance of the primary is restarted in place, without performing any switchover, only when any of the parameters\n `max_connections`, `max_prepared_transactions`, `max_locks_per_transaction`, `max_wal_senders` or `max_worker_processes` is\n decreased (a hot standby require those parameters to be set to a value greater than or equal to the value set on the primary,\n see https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN) or when no replica is available to switchover\n to. Patroni only reports the parameters that are pending a change since version 4, when they are not reported a switchover is\n performed.\n\nThe delay between the restart of a Postgres instance and the next one is configured in the\n `SGCluster.spec.pods.updateStrategy.restartDelay` field.",
    "name": "securityUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies a timeout after which the operation execution will be canceled.\n\nIf the operation can not be performed due to timeout expiration, the condition `Failed` will have a status of `True` and the reason will be `OperationTimedOut`.\n\nIf not specified the operation will never fail for timeout expiration.",
    "name": "timeout",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdbopsspecvacuum",
    "description": "Configuration of [vacuum](https://www.postgresql.org/docs/current/sql-vacuum.html) operation",
    "name": "vacuum",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

### SGDbOps.spec.benchmark
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of the benchmark


{{< crd-properties >}}
[
  {
    "description": "The type of benchmark that will be performed on the SGCluster. Available benchmarks are:\n\n* `pgbench`: run [pgbench](https://www.postgresql.org/docs/current/pgbench.html) on the specified SGCluster and report the results in the status.\n* `sampling`: samples real queries and store them in the SGDbOps status in order to be used by a `pgbench` benchmark using `replay` mode.",
    "name": "type",
    "required": true,
    "type": "string"
  },
  {
    "description": "Specify the service where the benchmark will connect to:\n\n* `primary-service`: Connect to the primary service\n* `replicas-service`: Connect to the replicas service",
    "name": "connectionType",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdbopsspecbenchmarkcredentials",
    "description": "The credentials of the user that will be used by the benchmark",
    "name": "credentials",
    "required": false,
    "type": "object"
  },
  {
    "description": "When specified will indicate the database where the benchmark will run upon.\n\nIf not specified a target database with a random name will be created and removed after the benchmark completes.",
    "name": "database",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdbopsspecbenchmarkpgbench",
    "description": "Configuration of [pgbench](https://www.postgresql.org/docs/current/pgbench.html) benchmark",
    "name": "pgbench",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecbenchmarksampling",
    "description": "Configuration of sampling benchmark.",
    "name": "sampling",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGDbOps.spec.benchmark.credentials
<sup><sup>[↩ Parent](#sgdbopsspecbenchmark)</sup></sup>

The credentials of the user that will be used by the benchmark

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecbenchmarkcredentialspassword",
    "description": "The password that will be used by the benchmark\n\nIf not specified the default superuser password will be used.",
    "name": "password",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecbenchmarkcredentialsusername",
    "description": "The username that will be used by the benchmark.\n\nIf not specified the default superuser username (by default postgres) will be used.",
    "name": "username",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.credentials.password
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkcredentials)</sup></sup>

The password that will be used by the benchmark

If not specified the default superuser password will be used.


{{< crd-properties >}}
[
  {
    "description": "The Secret key where the password is stored.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "The Secret name where the password is stored.",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.credentials.username
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkcredentials)</sup></sup>

The username that will be used by the benchmark.

If not specified the default superuser username (by default postgres) will be used.


{{< crd-properties >}}
[
  {
    "description": "The Secret key where the username is stored.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "The Secret name where the username is stored.",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGDbOps.spec.benchmark.pgbench
<sup><sup>[↩ Parent](#sgdbopsspecbenchmark)</sup></sup>

Configuration of [pgbench](https://www.postgresql.org/docs/current/pgbench.html) benchmark


{{< crd-properties >}}
[
  {
    "description": "Size of the database to generate. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).",
    "name": "databaseSize",
    "required": true,
    "type": "string"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how long the benchmark will run.",
    "name": "duration",
    "required": true,
    "type": "string"
  },
  {
    "description": "Number of clients simulated, that is, number of concurrent database sessions. Defaults to: `1`.",
    "name": "concurrentClients",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustom",
    "description": "This section allow to configure custom SQL for initialization and scripts used by pgbench.",
    "name": "custom",
    "required": false,
    "type": "object"
  },
  {
    "description": "Create the pgbench_accounts, pgbench_tellers and pgbench_branches tables with the given fillfactor. Default is 100.",
    "name": "fillfactor",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Create foreign key constraints between the standard tables. (This option only takes effect if `custom.initialization` is not specified).",
    "name": "foreignKeys",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Perform just a selected set of the normal initialization steps. init_steps specifies the initialization steps to be performed, using one character per step. Each step is invoked in the specified order. The default is dtgvp. The available steps are:\n\n* `d` (Drop): Drop any existing pgbench tables.\n* `t` (create Tables): Create the tables used by the standard pgbench scenario, namely pgbench_accounts, pgbench_branches, pgbench_history, and pgbench_tellers.\n* `g` or `G` (Generate data, client-side or server-side): Generate data and load it into the standard tables, replacing any data already present.\n  With `g` (client-side data generation), data is generated in pgbench client and then sent to the server. This uses the client/server bandwidth extensively through a COPY. pgbench uses the FREEZE option with version 14 or later of PostgreSQL to speed up subsequent VACUUM, unless partitions are enabled. Using g causes logging to print one message every 100,000 rows while generating data for the pgbench_accounts table.\n  With `G` (server-side data generation), only small queries are sent from the pgbench client and then data is actually generated in the server. No significant bandwidth is required for this variant, but the server will do more work. Using G causes logging not to print any progress message while generating data.\n  The default initialization behavior uses client-side data generation (equivalent to g).\n* `v` (Vacuum): Invoke VACUUM on the standard tables.\n* `p` (create Primary keys): Create primary key indexes on the standard tables.\n* `f` (create Foreign keys): Create foreign key constraints between the standard tables. (Note that this step is not performed by default.)",
    "name": "initSteps",
    "required": false,
    "type": "string"
  },
  {
    "description": "The pgbench benchmark type:\n\n* `tpcb-like`: The benchmark is inspired by the [TPC-B benchmark](https://www.tpc.org/TPC_Documents_Latest_Versions/TPC-B_v2.0.0.pdf). It is the default mode when `connectionType` is set to `primary-service`.\n* `select-only`: The `tpcb-like` but only using SELECTs commands. It is the default mode when `connectionType` is set to `replicas-service`.\n* `custom`: will use the scripts in the `custom` section to initialize and run commands for the benchmark.\n* `replay`: will replay the sampled queries of a sampling benchmark SGDbOps. If the `custom` section is specified it will be used instead. Queries can be referenced setting `custom.scripts.replay` to the index of the query in the sampling benchmark SGDbOps's status (index start from 0).\n\nSee also https://www.postgresql.org/docs/current/pgbench.html#TRANSACTIONS-AND-SCRIPTS",
    "name": "mode",
    "required": false,
    "type": "string"
  },
  {
    "description": "Perform no vacuuming during initialization. (This option suppresses the `v` initialization step, even if it was specified in `initSteps`.)",
    "name": "noVacuum",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Create a partitioned pgbench_accounts table with the specified method. Expected values are `range` or `hash`. This option requires that partitions is set to non-zero. If unspecified, default is `range`. (This option only takes effect if `custom.initialization` is not specified).",
    "name": "partitionMethod",
    "required": false,
    "type": "string"
  },
  {
    "description": "Create a partitioned pgbench_accounts table with the specified number of partitions of nearly equal size for the scaled number of accounts. Default is 0, meaning no partitioning. (This option only takes effect if `custom.initialization` is not specified).",
    "name": "partitions",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Protocol to use for submitting queries to the server:\n\n* `simple`: use simple query protocol.\n* `extended`: use extended query protocol.\n* `prepared`: use extended query protocol with prepared statements.\n\nIn the prepared mode, pgbench reuses the parse analysis result starting from the second query iteration, so pgbench runs faster than in other modes.\n\nThe default is `simple` query protocol. See also https://www.postgresql.org/docs/current/protocol.html",
    "name": "queryMode",
    "required": false,
    "type": "string"
  },
  {
    "description": "Sampling rate, used when collecting data, to reduce the amount of collected data. If this option is given, only the specified fraction of transactions are collected. 1.0 means all transactions will be logged, 0.05 means only 5% of the transactions will be logged.",
    "name": "samplingRate",
    "required": false,
    "type": "number"
  },
  {
    "description": "benchmark SGDbOps of type sampling that will be used to replay sampled queries.",
    "name": "samplingSGDbOps",
    "required": false,
    "type": "string"
  },
  {
    "description": "Number of worker threads within pgbench. Using more than one thread can be helpful on multi-CPU machines. Clients are distributed as evenly as possible among available threads. Default is `1`.",
    "name": "threads",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Create all tables as unlogged tables, rather than permanent tables. (This option only takes effect if `custom.initialization` is not specified).",
    "name": "unloggedTables",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "**Deprecated** this field is ignored, use `queryMode` instead.\n\nUse extended query protocol with prepared statements. Defaults to: `false`.",
    "name": "usePreparedStatements",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbench)</sup></sup>

This section allow to configure custom SQL for initialization and scripts used by pgbench.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustominitialization",
    "description": "The custom SQL for initialization that will be executed in place of pgbench default initialization.\n\nIf not specified the default pgbench initialization will be performed instead.",
    "name": "initialization",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustomscriptsindex",
    "description": "The custom SQL scripts that will be executed by pgbench during the benchmark instead of default pgbench scripts",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom.initialization
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustom)</sup></sup>

The custom SQL for initialization that will be executed in place of pgbench default initialization.

If not specified the default pgbench initialization will be performed instead.


{{< crd-properties >}}
[
  {
    "description": "Raw SQL script to execute. This field is mutually exclusive with `scriptFrom` field.",
    "name": "script",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustominitializationscriptfrom",
    "description": "Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.\n\nFields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.",
    "name": "scriptFrom",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom.initialization.scriptFrom
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustominitialization)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustominitializationscriptfromconfigmapkeyref",
    "description": "A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.",
    "name": "configMapKeyRef",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustominitializationscriptfromsecretkeyref",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.",
    "name": "secretKeyRef",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom.initialization.scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustominitializationscriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.


{{< crd-properties >}}
[
  {
    "description": "The key name within the ConfigMap that contains the SQL script to execute.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "The name of the ConfigMap that contains the SQL script to execute.",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom.initialization.scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustominitializationscriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom.scripts[index]
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustom)</sup></sup>

A custom SQL script that will be executed by pgbench during the benchmark instead of default pgbench scripts

{{< crd-properties >}}
[
  {
    "description": "The name of the builtin script to use. See https://www.postgresql.org/docs/current/pgbench.html#PGBENCH-OPTION-BUILTIN\n\nWhen specified fields `replay`, `script` and `scriptFrom` must not be set.",
    "name": "builtin",
    "required": false,
    "type": "string"
  },
  {
    "description": "The index of the query in the sampling benchmark SGDbOps's status (index start from 0).\n\nWhen specified fields `builtin`, `script` and `scriptFrom` must not be set.",
    "name": "replay",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Raw SQL script to execute. This field is mutually exclusive with `scriptFrom` field.",
    "name": "script",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfrom",
    "description": "Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.\n\nFields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.",
    "name": "scriptFrom",
    "required": false,
    "type": "object"
  },
  {
    "description": "The weight of this custom SQL script.",
    "name": "weight",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom.scripts[index].scriptFrom
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustomscriptsindex)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfromconfigmapkeyref",
    "description": "A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.",
    "name": "configMapKeyRef",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfromsecretkeyref",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.",
    "name": "secretKeyRef",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom.scripts[index].scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.


{{< crd-properties >}}
[
  {
    "description": "The key name within the ConfigMap that contains the SQL script to execute.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "The name of the ConfigMap that contains the SQL script to execute.",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.benchmark.pgbench.custom.scripts[index].scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGDbOps.spec.benchmark.sampling
<sup><sup>[↩ Parent](#sgdbopsspecbenchmark)</sup></sup>

Configuration of sampling benchmark.


{{< crd-properties >}}
[
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how long will last the sampling of real queries that will be replayed later.",
    "name": "samplingDuration",
    "required": true,
    "type": "string"
  },
  {
    "description": "The target database to be sampled. By default `postgres`.\n\nThe benchmark database will be used to store the sampled queries but user must specify a target database to be sampled in the `sampling` section.",
    "name": "targetDatabase",
    "required": true,
    "type": "string"
  },
  {
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how long to wait before selecting top queries in order to collect enough stats.",
    "name": "topQueriesCollectDuration",
    "required": true,
    "type": "string"
  },
  {
    "description": "The query used to select top queries. Will be ignored if `mode` is not set to `custom`.\n\nThe query must return at most 2 columns:\n\n* First column returned by the query must be a column holding the query identifier, also available in pg_stat_activity (column `query_id`) and pg_stat_statements (column `queryid`).\n* Second column is optional and, if returned, must hold a json object containing only text keys and values that will be used to generate the stats.\n\nSee also:\n\n* https://www.postgresql.org/docs/current/monitoring-stats.html#MONITORING-PG-STAT-ACTIVITY-VIEW\n* https://www.postgresql.org/docs/current/pgstatstatements.html#PGSTATSTATEMENTS-PG-STAT-STATEMENTS",
    "name": "customTopQueriesQuery",
    "required": false,
    "type": "string"
  },
  {
    "description": "The mode used to select the top queries used for sampling:\n\n* `time`: The top queries will be selected among the most slow queries.\n* `calls`: The top queries will be selected among the most called queries.\n* `custom`: The `customTopQueriesQuery` will be used to select top queries.",
    "name": "mode",
    "required": false,
    "type": "string"
  },
  {
    "description": "When `true` omit to include the top queries stats in the SGDbOps status. By default `false`.",
    "name": "omitTopQueriesInStatus",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Number of sampled queries to include in the result. By default `10`.",
    "name": "queries",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Minimum number of microseconds the sampler will wait between each sample is taken. By default `10000` (10 milliseconds).",
    "name": "samplingMinInterval",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Regular expression for filtering representative statements when selecting top queries. Will be ignored if `mode` is set to `custom`. By default is `^ *(with|select) `. See https://www.postgresql.org/docs/current/functions-matching.html#FUNCTIONS-POSIX-REGEXP",
    "name": "topQueriesFilter",
    "required": false,
    "type": "string"
  },
  {
    "description": "Minimum number of queries to consider as part of the top queries. By default `5`.",
    "name": "topQueriesMin",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Percentile of queries to consider as part of the top queries. Will be ignored if `mode` is set to `custom`. By default `95`.",
    "name": "topQueriesPercentile",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

### SGDbOps.spec.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of major version upgrade (see also [`pg_upgrade`](https://www.postgresql.org/docs/current/pgupgrade.html) command)


{{< crd-properties >}}
[
  {
    "description": "The path where the backup is stored. If not set this field is filled up by the operator.\n\nWhen provided will indicate where the backups and WAL files will be stored.\n\nThe path should be different from the current `.spec.configurations.backups[].path` value for the target `SGCluster`\n  in order to avoid mixing WAL files of two distinct major versions of postgres.",
    "name": "backupPath",
    "required": false,
    "type": "string"
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
    "default": 6,
    "description": "Indicates the number of errors that the operation can tolerate after the upgrade\n is performed in order to wait for the Pod to become ready and set the operation\n as completed.",
    "name": "maxErrorsAfterUpgrade",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgdbopsspecmajorversionupgradepostgresextensionsindex",
    "description": "A major version upgrade can not be performed if a required extension is not present for the target major version of the upgrade.\nIn those cases you will have to provide the target extension version of the extension for the target major version of postgres.\nBeware that in some cases it is not possible to upgrade an extension alongside postgres. This is the case for PostGIS or timescaledb.\n In such cases you will have to upgrade the extension before or after the major version upgrade. Please make sure you read the\n documentation of each extension in order to understand if it is possible to upgrade it during a major version upgrade of postgres.",
    "name": "postgresExtensions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "The target postgres version that must have the same major version of the target SGCluster.",
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
    "anchor": "sgdbopsspecmajorversionupgradetoinstallpostgresextensionsindex",
    "description": "The list of Postgres extensions to install.\n\n**This section is filled by the operator.**",
    "name": "toInstallPostgresExtensions",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGDbOps.spec.majorVersionUpgrade.postgresExtensions[index]
<sup><sup>[↩ Parent](#sgdbopsspecmajorversionupgrade)</sup></sup>



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

#### SGDbOps.spec.majorVersionUpgrade.toInstallPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgdbopsspecmajorversionupgrade)</sup></sup>



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

### SGDbOps.spec.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

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
 `SGCluster.spec.pods.updateStrategy.restartDelay` field.


{{< crd-properties >}}
[
  {
    "description": "The method used to perform the minor version upgrade operation. Available methods are:\n\n* `InPlace`: the in-place method does not require more resources than those that are available.\n  In case only an instance of the StackGres cluster is present this mean the service disruption will\n  last longer so we encourage use the reduced impact restart and especially for a production environment.\n* `ReducedImpact`: this procedure is the same as the in-place method but require additional\n  resources in order to spawn a new updated replica that will be removed when the procedure completes.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "description": "The target postgres version that must have the same major version of the target SGCluster.",
    "name": "postgresVersion",
    "required": false,
    "type": "string"
  },
  {
    "default": "PT5S",
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS` indicating the delay that has to pass, since the last update of the status of\n this operation, before the operation can be considered completed. Default is `PT5S`.\n\nPatroni and the StatefulSet controller do not update their status immediately, therefore the operation may look completed while\n some Pod has still to be restarted. The instant of the last update of the status is stored in the field\n `.status.minorVersionUpgrade.lastUpdate` and is changed whenever a condition or any other status field of the SGDbOps is updated. The\n conditions `RolloutCompleted` and `Completed` are not set until such instant plus this delay has passed, giving the time to the\n controllers to update the status and to trigger any other rollout operation.",
    "name": "statusUpdateDelay",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDbOps.spec.repack
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of [`pg_repack`](https://github.com/reorg/pg_repack) command


{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecrepackdatabasesindex",
    "description": "List of databases to vacuum or repack, don't specify to select all databases",
    "name": "databases",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "If true don't repack tables which belong to specific extension. Defaults to: `false`.",
    "name": "excludeExtension",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true don't analyze at end. Defaults to: `false`.",
    "name": "noAnalyze",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true don't kill other backends when timed out. Defaults to: `false`.",
    "name": "noKillBackend",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true do vacuum full instead of cluster. Defaults to: `false`.",
    "name": "noOrder",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If specified, an ISO 8601 duration format `PnDTnHnMn.nS` to set a timeout to cancel other backends on conflict.",
    "name": "waitTimeout",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGDbOps.spec.repack.databases[index]
<sup><sup>[↩ Parent](#sgdbopsspecrepack)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "the name of the database",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "If true don't repack tables which belong to specific extension. Defaults to: `false`.",
    "name": "excludeExtension",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true don't analyze at end. Defaults to: `false`.",
    "name": "noAnalyze",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true don't kill other backends when timed out. Defaults to: `false`.",
    "name": "noKillBackend",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true do vacuum full instead of cluster. Defaults to: `false`.",
    "name": "noOrder",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If specified, an ISO 8601 duration format `PnDTnHnMn.nS` to set a timeout to cancel other backends on conflict.",
    "name": "waitTimeout",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDbOps.spec.restart
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

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
 `SGCluster.spec.pods.updateStrategy.restartDelay` field.


{{< crd-properties >}}
[
  {
    "description": "The method used to perform the restart operation. Available methods are:\n\n* `InPlace`: the in-place method does not require more resources than those that are available.\n  In case only an instance of the StackGres cluster is present this mean the service disruption will\n  last longer so we encourage use the reduced impact restart and especially for a production environment.\n* `ReducedImpact`: this procedure is the same as the in-place method but require additional\n  resources in order to spawn a new updated replica that will be removed when the procedure completes.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "description": "By default all Pods are restarted. Setting this option to `true` allow to restart only those Pods which\n  are in pending restart state as detected by the operation. Defaults to: `false`.",
    "name": "onlyPendingRestart",
    "required": false,
    "type": "boolean"
  },
  {
    "default": "PT5S",
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS` indicating the delay that has to pass, since the last update of the status of\n this operation, before the operation can be considered completed. Default is `PT5S`.\n\nPatroni and the StatefulSet controller do not update their status immediately, therefore the operation may look completed while\n some Pod has still to be restarted. The instant of the last update of the status is stored in the field\n `.status.restart.lastUpdate` and is changed whenever a condition or any other status field of the SGDbOps is updated. The\n conditions `RolloutCompleted` and `Completed` are not set until such instant plus this delay has passed, giving the time to the\n controllers to update the status and to trigger any other rollout operation.",
    "name": "statusUpdateDelay",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDbOps.spec.scheduling
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Pod custom node scheduling and affinity configuration

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingnodeaffinity",
    "description": "Node affinity is a group of node affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core",
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
    "anchor": "sgdbopsspecschedulingpodaffinity",
    "description": "Pod affinity is a group of inter pod affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core",
    "name": "podAffinity",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsspecschedulingpodantiaffinity",
    "description": "Pod anti affinity is a group of inter pod anti affinity scheduling rules.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core",
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
    "anchor": "sgdbopsspecschedulingtolerationsindex",
    "description": "If specified, the pod's tolerations.\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#toleration-v1-core",
    "name": "tolerations",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGDbOps.spec.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgdbopsspecscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#nodeaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution",
    "description": "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference",
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

##### SGDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex",
    "description": "Required. A list of node selector terms. The terms are ORed.",
    "name": "nodeSelectorTerms",
    "required": true,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex",
    "description": "A list of node selector requirements by node's labels.",
    "name": "matchExpressions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex",
    "description": "A list of node selector requirements by node's fields.",
    "name": "matchFields",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

#### SGDbOps.spec.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgdbopsspecscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding \"weight\" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinity)</sup></sup>

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
    "anchor": "sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

#### SGDbOps.spec.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgdbopsspecscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#podantiaffinity-v1-core

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex",
    "description": "The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and subtracting \"weight\" from the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.",
    "name": "preferredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex",
    "description": "If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.",
    "name": "requiredDuringSchedulingIgnoredDuringExecution",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm",
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

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
    "anchor": "sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector",
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
    "anchor": "sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector",
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

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex",
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

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex",
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

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinity)</sup></sup>

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
    "anchor": "sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector",
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
    "anchor": "sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector",
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

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex",
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

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex",
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

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

#### SGDbOps.spec.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgdbopsspecscheduling)</sup></sup>

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

### SGDbOps.spec.securityUpgrade
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

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
 `SGCluster.spec.pods.updateStrategy.restartDelay` field.


{{< crd-properties >}}
[
  {
    "description": "The method used to perform the security upgrade operation. Available methods are:\n\n* `InPlace`: the in-place method does not require more resources than those that are available.\n  In case only an instance of the StackGres cluster is present this mean the service disruption will\n  last longer so we encourage use the reduced impact restart and especially for a production environment.\n* `ReducedImpact`: this procedure is the same as the in-place method but require additional\n  resources in order to spawn a new updated replica that will be removed when the procedure completes.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "default": "PT5S",
    "description": "An ISO 8601 duration in the format `PnDTnHnMn.nS` indicating the delay that has to pass, since the last update of the status of\n this operation, before the operation can be considered completed. Default is `PT5S`.\n\nPatroni and the StatefulSet controller do not update their status immediately, therefore the operation may look completed while\n some Pod has still to be restarted. The instant of the last update of the status is stored in the field\n `.status.securityUpgrade.lastUpdate` and is changed whenever a condition or any other status field of the SGDbOps is updated. The\n conditions `RolloutCompleted` and `Completed` are not set until such instant plus this delay has passed, giving the time to the\n controllers to update the status and to trigger any other rollout operation.",
    "name": "statusUpdateDelay",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDbOps.spec.vacuum
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of [vacuum](https://www.postgresql.org/docs/current/sql-vacuum.html) operation


{{< crd-properties >}}
[
  {
    "description": "If true, updates statistics used by the planner to determine the most efficient way to execute a query. Defaults to: `true`.",
    "name": "analyze",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgdbopsspecvacuumdatabasesindex",
    "description": "List of databases to vacuum or repack, don't specify to select all databases",
    "name": "databases",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Normally, VACUUM will skip pages based on the visibility map. Pages where all tuples are known to be frozen can always be\n  skipped, and those where all tuples are known to be visible to all transactions may be skipped except when performing an\n  aggressive vacuum. Furthermore, except when performing an aggressive vacuum, some pages may be skipped in order to avoid\n  waiting for other sessions to finish using them. This option disables all page-skipping behavior, and is intended to be\n  used only when the contents of the visibility map are suspect, which should happen only if there is a hardware or\n  software issue causing database corruption. Defaults to: `false`.",
    "name": "disablePageSkipping",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true selects aggressive \"freezing\" of tuples. Specifying FREEZE is equivalent to performing VACUUM with the\n  vacuum_freeze_min_age and vacuum_freeze_table_age parameters set to zero. Aggressive freezing is always performed\n  when the table is rewritten, so this option is redundant when FULL is specified. Defaults to: `false`.",
    "name": "freeze",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true selects \"full\" vacuum, which can reclaim more space, but takes much longer and exclusively locks the table.\nThis method also requires extra disk space, since it writes a new copy of the table and doesn't release the old copy\n  until the operation is complete. Usually this should only be used when a significant amount of space needs to be\n  reclaimed from within the table. Defaults to: `false`.",
    "name": "full",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

#### SGDbOps.spec.vacuum.databases[index]
<sup><sup>[↩ Parent](#sgdbopsspecvacuum)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "the name of the database",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "If true, updates statistics used by the planner to determine the most efficient way to execute a query. Defaults to: `true`.",
    "name": "analyze",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Normally, VACUUM will skip pages based on the visibility map. Pages where all tuples are known to be frozen can always be\n  skipped, and those where all tuples are known to be visible to all transactions may be skipped except when performing an\n  aggressive vacuum. Furthermore, except when performing an aggressive vacuum, some pages may be skipped in order to avoid\n  waiting for other sessions to finish using them. This option disables all page-skipping behavior, and is intended to be\n  used only when the contents of the visibility map are suspect, which should happen only if there is a hardware or\n  software issue causing database corruption. Defaults to: `false`.",
    "name": "disablePageSkipping",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true selects aggressive \"freezing\" of tuples. Specifying FREEZE is equivalent to performing VACUUM with the\n  vacuum_freeze_min_age and vacuum_freeze_table_age parameters set to zero. Aggressive freezing is always performed\n  when the table is rewritten, so this option is redundant when FULL is specified. Defaults to: `false`.",
    "name": "freeze",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If true selects \"full\" vacuum, which can reclaim more space, but takes much longer and exclusively locks the table.\nThis method also requires extra disk space, since it writes a new copy of the table and doesn't release the old copy\n  until the operation is complete. Usually this should only be used when a significant amount of space needs to be\n  reclaimed from within the table. Defaults to: `false`.",
    "name": "full",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

## SGDbOps.status
<sup><sup>[↩ Parent](#sgdbops)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsstatusbenchmark",
    "description": "The results of the benchmark",
    "name": "benchmark",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsstatusconditionsindex",
    "description": "Possible conditions are:\n\n* Running: to indicate when the operation is actually running\n* Completed: to indicate when the operation has completed successfully\n* Failed: to indicate when the operation has failed",
    "name": "conditions",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdbopsstatusmajorversionupgrade",
    "description": "The results of a major version upgrade",
    "name": "majorVersionUpgrade",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsstatusminorversionupgrade",
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
    "anchor": "sgdbopsstatusrestart",
    "description": "The results of a restart",
    "name": "restart",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsstatussecurityupgrade",
    "description": "The results of a security upgrade",
    "name": "securityUpgrade",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

### SGDbOps.status.benchmark
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

The results of the benchmark


{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsstatusbenchmarkpgbench",
    "description": "The results of the pgbench benchmark",
    "name": "pgbench",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsstatusbenchmarksampling",
    "description": "The results of the sampling benchmark",
    "name": "sampling",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGDbOps.status.benchmark.pgbench
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmark)</sup></sup>

The results of the pgbench benchmark

{{< crd-properties >}}
[
  {
    "description": "Compressed and base 64 encoded HdrHistogram",
    "name": "hdrHistogram",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgdbopsstatusbenchmarkpgbenchlatency",
    "description": "The latency results of the pgbench benchmark",
    "name": "latency",
    "required": false,
    "type": "object"
  },
  {
    "description": "The scale factor used to run pgbench (`--scale`).",
    "name": "scaleFactor",
    "required": false,
    "type": "number"
  },
  {
    "anchor": "sgdbopsstatusbenchmarkpgbenchstatementsindex",
    "description": "Average per-statement latency (execution time from the perspective of the client) of each command after the benchmark finishes",
    "name": "statements",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdbopsstatusbenchmarkpgbenchtransactionspersecond",
    "description": "All the transactions per second results of the pgbench benchmark",
    "name": "transactionsPerSecond",
    "required": false,
    "type": "object"
  },
  {
    "description": "The number of transactions processed.",
    "name": "transactionsProcessed",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.pgbench.latency
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbench)</sup></sup>

The latency results of the pgbench benchmark


{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsstatusbenchmarkpgbenchlatencyaverage",
    "description": "Average latency of transactions",
    "name": "average",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsstatusbenchmarkpgbenchlatencystandarddeviation",
    "description": "The latency standard deviation of transactions.",
    "name": "standardDeviation",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.pgbench.latency.average
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchlatency)</sup></sup>

Average latency of transactions


{{< crd-properties >}}
[
  {
    "description": "The latency measure unit",
    "name": "unit",
    "required": false,
    "type": "string"
  },
  {
    "description": "The latency average value",
    "name": "value",
    "required": false,
    "type": "number"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.pgbench.latency.standardDeviation
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchlatency)</sup></sup>

The latency standard deviation of transactions.


{{< crd-properties >}}
[
  {
    "description": "The latency measure unit",
    "name": "unit",
    "required": false,
    "type": "string"
  },
  {
    "description": "The latency standard deviation value",
    "name": "value",
    "required": false,
    "type": "number"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.pgbench.statements[index]
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbench)</sup></sup>

Average per-statement latency (execution time from the perspective of the client) of a command after the benchmark finishes

{{< crd-properties >}}
[
  {
    "description": "The command",
    "name": "command",
    "required": false,
    "type": "string"
  },
  {
    "description": "Average latency of the command",
    "name": "latency",
    "required": false,
    "type": "number"
  },
  {
    "description": "The script index (`0` if no custom scripts have been defined)",
    "name": "script",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The average latency measure unit",
    "name": "unit",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.pgbench.transactionsPerSecond
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbench)</sup></sup>

All the transactions per second results of the pgbench benchmark


{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsstatusbenchmarkpgbenchtransactionspersecondexcludingconnectionsestablishing",
    "description": "Number of Transactions Per Second (tps) excluding connection establishing.",
    "name": "excludingConnectionsEstablishing",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsstatusbenchmarkpgbenchtransactionspersecondincludingconnectionsestablishing",
    "description": "Number of Transactions Per Second (tps) including connection establishing.",
    "name": "includingConnectionsEstablishing",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgdbopsstatusbenchmarkpgbenchtransactionspersecondovertime",
    "description": "The Transactions Per Second (tps) values aggregated over unit of time",
    "name": "overTime",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchtransactionspersecond)</sup></sup>

Number of Transactions Per Second (tps) excluding connection establishing.


{{< crd-properties >}}
[
  {
    "description": "Transactions Per Second (tps) measure unit",
    "name": "unit",
    "required": false,
    "type": "string"
  },
  {
    "description": "The Transactions Per Second (tps) excluding connections establishing value",
    "name": "value",
    "required": false,
    "type": "number"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchtransactionspersecond)</sup></sup>

Number of Transactions Per Second (tps) including connection establishing.


{{< crd-properties >}}
[
  {
    "description": "Transactions Per Second (tps) measure unit",
    "name": "unit",
    "required": false,
    "type": "string"
  },
  {
    "description": "The Transactions Per Second (tps) including connections establishing value",
    "name": "value",
    "required": false,
    "type": "number"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.pgbench.transactionsPerSecond.overTime
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchtransactionspersecond)</sup></sup>

The Transactions Per Second (tps) values aggregated over unit of time

{{< crd-properties >}}
[
  {
    "description": "The interval duration used to aggregate the transactions per second.",
    "name": "intervalDuration",
    "required": false,
    "type": "number"
  },
  {
    "description": "The interval duration measure unit",
    "name": "intervalDurationUnit",
    "required": false,
    "type": "string"
  },
  {
    "description": "The Transactions Per Second (tps) values aggregated over unit of time",
    "name": "values",
    "required": false,
    "type": "[]number"
  },
  {
    "description": "The Transactions Per Second (tps) measures unit",
    "name": "valuesUnit",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGDbOps.status.benchmark.sampling
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmark)</sup></sup>

The results of the sampling benchmark

{{< crd-properties >}}
[
  {
    "anchor": "sgdbopsstatusbenchmarksamplingqueriesindex",
    "description": "The queries sampled.",
    "name": "queries",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgdbopsstatusbenchmarksamplingtopqueriesindex",
    "description": "The top queries sampled with the stats from pg_stat_statements. If is omitted if `omitTopQueriesInStatus` is set to `true`.",
    "name": "topQueries",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.sampling.queries[index]
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarksampling)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The query id of the representative statement calculated by Postgres",
    "name": "id",
    "required": false,
    "type": "string"
  },
  {
    "description": "A sampled SQL query",
    "name": "query",
    "required": false,
    "type": "string"
  },
  {
    "description": "The sampled query timestamp",
    "name": "timestamp",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGDbOps.status.benchmark.sampling.topQueries[index]
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarksampling)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The query id of the representative statement calculated by Postgres",
    "name": "id",
    "required": false,
    "type": "string"
  },
  {
    "description": "stats collected by the top queries query",
    "name": "stats",
    "required": false,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

### SGDbOps.status.conditions[index]
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>



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

### SGDbOps.status.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

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
    "description": "The instances present when the operation started",
    "name": "initialInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The instances that are pending to be restarted",
    "name": "pendingToRestartInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The phase the operation is or was executing",
    "name": "phase",
    "required": false,
    "type": "string"
  },
  {
    "description": "The primary instance when the operation started",
    "name": "primaryInstance",
    "required": false,
    "type": "string"
  },
  {
    "description": "The instances that have been restarted",
    "name": "restartedInstances",
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
    "description": "The postgres version that the cluster will be upgraded to",
    "name": "targetPostgresVersion",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDbOps.status.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

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
    "description": "The instances present when the operation started",
    "name": "initialInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "An ISO 8601 date indicating when a condition or any other status field of the SGDbOps has been\n updated for the last time. See the `.spec.minorVersionUpgrade.statusUpdateDelay` field.",
    "name": "lastUpdate",
    "required": false,
    "type": "string"
  },
  {
    "description": "The instances that are pending to be restarted",
    "name": "pendingToRestartInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The primary instance when the operation started",
    "name": "primaryInstance",
    "required": false,
    "type": "string"
  },
  {
    "description": "The instances that have been restarted",
    "name": "restartedInstances",
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
    "description": "An ISO 8601 date indicating if and when the switchover finalized",
    "name": "switchoverFinalized",
    "required": false,
    "type": "string"
  },
  {
    "description": "An ISO 8601 date indicating if and when the switchover initiated",
    "name": "switchoverInitiated",
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

### SGDbOps.status.restart
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

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
    "description": "The instances present when the operation started",
    "name": "initialInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "An ISO 8601 date indicating when a condition or any other status field of the SGDbOps has been\n updated for the last time. See the `.spec.restart.statusUpdateDelay` field.",
    "name": "lastUpdate",
    "required": false,
    "type": "string"
  },
  {
    "description": "The instances that are pending to be restarted",
    "name": "pendingToRestartInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The primary instance when the operation started",
    "name": "primaryInstance",
    "required": false,
    "type": "string"
  },
  {
    "description": "The instances that have been restarted",
    "name": "restartedInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "An ISO 8601 date indicating if and when the switchover finalized",
    "name": "switchoverFinalized",
    "required": false,
    "type": "string"
  },
  {
    "description": "An ISO 8601 date indicating if and when the switchover initiated",
    "name": "switchoverInitiated",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGDbOps.status.securityUpgrade
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

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
    "description": "The instances present when the operation started",
    "name": "initialInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "An ISO 8601 date indicating when a condition or any other status field of the SGDbOps has been\n updated for the last time. See the `.spec.securityUpgrade.statusUpdateDelay` field.",
    "name": "lastUpdate",
    "required": false,
    "type": "string"
  },
  {
    "description": "The instances that are pending to be restarted",
    "name": "pendingToRestartInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The primary instance when the operation started",
    "name": "primaryInstance",
    "required": false,
    "type": "string"
  },
  {
    "description": "The instances that have been restarted",
    "name": "restartedInstances",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "An ISO 8601 date indicating if and when the switchover finalized",
    "name": "switchoverFinalized",
    "required": false,
    "type": "string"
  },
  {
    "description": "An ISO 8601 date indicating if and when the switchover initiated",
    "name": "switchoverInitiated",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}
