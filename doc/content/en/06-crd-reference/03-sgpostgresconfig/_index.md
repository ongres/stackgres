---
title: SGPostgresConfig
weight: 3
url: reference/crd/sgpgconfig
description: Details about SGPostgresConfig configurations
---

A PostgreSQL configuration CR represent the configuration of a specific PostgreSQL major
 version.

Have a look at [postgresqlco.nf](https://postgresqlco.nf) to help you tune and optimize your
 PostgreSQL configuration.

___

**Kind:** SGPostgresConfig

**listKind:** SGPostgresConfigList

**plural:** sgpgconfigs

**singular:** sgpgconfig
___

**Spec**

| Property        | Required | Updatable | Type   | Default   | Description |
|:----------------|----------|-----------|:-------|:----------|:------------|
| postgresVersion |          |           | string | 12        | {{< crd-field-description SGPostgresConfig.spec.postgresVersion >}} |
| postgresql.conf |          | ✓         | object | see below | {{< crd-field-description "SGPostgresConfig.spec.postgresql\.conf" >}} |

Default value of `postgresql.conf` property:

```yaml
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

Example:

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

To guarantee a functional postgres configuration some of the parameters specified in
 [postgres configuration documentation](https://www.postgresql.org/docs/latest/runtime-config.html)
 have been added to the blocklisted and, if specified in the configuration, an error will be returned.
 The parameters that will not be allowed in the configuration are:

| blocklisted parameters   |
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
