---
title: SGPostgresConfig
weight: 3
url: /reference/crd/sgpostgresconfig
description: Details about SGPostgresConfig
shotToc: true
---

___

**Kind:** SGPostgresConfig

**listKind:** SGPostgresConfigList

**plural:** sgpgconfigs

**singular:** sgpgconfig

**shortNames** sgpgc
___

The `SGPostgresConfig` custom resource represents the PostgreSQL configuration.

> Have a look at [postgresqlco.nf](https://postgresqlco.nf) to help you tune and optimize your PostgreSQL configuration.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  name: postgresconf
spec:
  postgresVersion: "11"
  postgresql.conf:
    password_encryption: 'scram-sha-256'
    random_page_cost: '1.5'
    shared_buffers: '256MB'
    wal_compression: 'on'
```

See also [Postgres Configuration section]({{%  relref "04-administration-guide/04-configuration/02-postgres-configuration" %}}).





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
    "description": "SGPostgresConfig",
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
    "anchor": "sgpostgresconfigspec",
    "description": "",
    "name": "spec",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgpostgresconfigstatus",
    "description": "",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGPostgresConfig.spec
<sup><sup>[↩ Parent](#sgpostgresconfig)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The **major** Postgres version the configuration is for. Postgres major versions contain one number starting with version 10 (`10`, `11`, `12`, etc), and two numbers separated by a dot for previous versions (`9.6`, `9.5`, etc).\n\nNote that Postgres maintains full compatibility across minor versions, and hence a configuration for a given major version will work for any minor version of that same major version.\n\nCheck [StackGres component versions](https://stackgres.io/doc/latest/intro/versions) to see the Postgres versions supported by this version of StackGres.",
    "name": "postgresVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "The `postgresql.conf` parameters the configuration contains, represented as an object where the keys are valid names for the `postgresql.conf` configuration file parameters of the given `postgresVersion`. You may check [postgresqlco.nf](https://postgresqlco.nf) as a reference on how to tune and find the valid parameters for a given major version.",
    "name": "postgresql.conf",
    "required": true,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

## SGPostgresConfig.status
<sup><sup>[↩ Parent](#sgpostgresconfig)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The `postgresql.conf` default parameters which are used if not set.",
    "name": "defaultParameters",
    "required": true,
    "type": "map[string]string"
  }
]
{{< /crd-properties >}}

## Default Values

These are the default values of the `postgresql.conf` property:

```
listen_addresses: localhost
superuser_reserved_connections: 8
max_prepared_transactions: 32

work_mem: 10MB
maintenance_work_mem: 2GB
huge_pages: off

fsync: on

checkpoint_completion_target: 0.9
checkpoint_timeout: 15min
min_wal_size: 1GB
max_wal_size: 2GB
max_wal_senders: 20
wal_keep_segments: 96 # postgres > 13
# wal_keep_size=1536MB # postgres <= 13

archive_mode: on
archive_command: /bin/true
wal_log_hints: on
wal_compression: on

wal_level: logical
max_replication_slots: 20
hot_standby: on

default_statistics_target: 200
random_page_cost: 1.5
enable_partitionwise_aggregate: on
enable_partitionwise_join: on

max_locks_per_transaction: 128
max_pred_locks_per_transaction: 128

autovacuum_max_workers: 3
autovacuum_vacuum_cost_delay: 2
autovacuum_work_mem: 512MB

logging_collector: off
lc_messages: C
log_destination: stderr
log_directory: log
log_filename: postgres-%M.log
log_rotation_age: 30min
log_rotation_size: 0kB
log_truncate_on_rotation: on
log_min_duration_statement: 1000
log_checkpoints: on
log_connections: on
log_disconnections: on
log_lock_waits: on
log_temp_files: 0
log_autovacuum_min_duration: 0ms
log_line_prefix: %t [%p]: db=%d,user=%u,app=%a,client=%h 
log_statement: none
track_activity_query_size: 4096
track_functions: pl
track_io_timing: on
track_commit_timestamp: on

shared_preload_libraries: pg_stat_statements, auto_explain
pg_stat_statements.track_utility: off

# see https://gitlab.com/ongresinc/stackgres/-/issues/741
jit_inline_above_cost: -1
```

## Parameter Denylist / Blocklist

To guarantee a functional Postgres configuration, some parameters specified in [Postgres configuration documentation](https://www.postgresql.org/docs/latest/runtime-config.html) have been denylisted and cannot be changed from the provided defaults.
If these parameters are specified in the CR configuration, an error will be returned.
The denylisted parameters are:

| Denylisted Parameters    |
|:-------------------------|
| listen_addresses         |
| port                     |
| hot_standby              |
| fsync                    |
| logging_collector        |
| log_destination          |
| log_directory            |
| log_filename             |
| log_rotation_age         |
| log_rotation_size        |
| log_truncate_on_rotation |
| wal_level                |
| track_commit_timestamp   |
| wal_log_hints            |
| archive_mode             |
| archive_command          |
| lc_messages              |
| wal_compression          |
| dynamic_library_path     |
