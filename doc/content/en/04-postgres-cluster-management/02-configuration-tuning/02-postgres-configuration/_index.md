---
title: Postgres configuration
weight: 2
---

A PostgreSQL configuration CR represent the configuration of a specific PostgreSQL major
 version.

___

**Kind:** StackGresPostgresConfig

**listKind:** StackGresPostgresConfigList

**plural:** sgpgconfigs

**singular:** sgpgconfig
___

**Properties**

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| pgVersion | ✓ | string  | PostgreSQL configuration version (for example 11) | 11 |
| postgresql.conf |   | object  | List of PostgreSQL configuration parameters with their values  | see below |

Default postgresql.conf:

```shell
checkpoint_completion_target=0.9
checkpoint_timeout=15min
default_statistics_target=250
wal_level=logical
wal_compression=on
wal_log_hints=on
lc_messages=C
random_page_cost=2.0
track_activity_query_size=2048
archive_mode=on
huge_pages=off
shared_preload_libraries=pg_stat_statements
track_io_timing=on
track_functions=pl
extra_float_digits=1
```

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresPostgresConfig
metadata:
  name: postgresconf
spec:
  pgVersion: "11"
  postgresql.conf:
    password_encryption: 'scram-sha-256'
    random_page_cost: '1.5'
    shared_buffers: '256MB'
    wal_compression: 'on'
```
