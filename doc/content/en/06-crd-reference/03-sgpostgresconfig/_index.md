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
checkpoint_completion_target: "0.9"
checkpoint_timeout: 15min
default_statistics_target: "250"
extra_float_digits: "1"
huge_pages: "off"
lc_messages: C
random_page_cost: "2.0"
shared_preload_libraries: pg_stat_statements
track_activity_query_size: "2048"
track_functions: pl
track_io_timing: "on"
wal_compression: "on"
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
 [postgres configuration documentation](https://www.postgresql.org/docs/12/runtime-config.html)
 have been blacklisted and will be ignored. The parameters that will be ignored are:

| Blacklisted parameter   |
|:------------------------|
| listen_addresses        |
| port                    |
| cluster_name            |
| hot_standby             |
| fsync                   |
| full_page_writes        |
| log_destination         |
| logging_collector       |
| max_replication_slots   |
| max_wal_senders         |
| wal_keep_segments       |
| wal_level               |
| wal_log_hints           |
| archive_mode            |
| archive_command         |
