---
title: Custom Postgres Configuration
weight: 2
url: tutorial/complete-cluster/postgres-config
description: Details about how to create custom PostgreSQL configurations.
---

StackGres comes with an expertly tuned Postgres configuration (aka `postgresql.conf`) by default. However, it does not prevent you from using your own specialized Postgres configuration. If you want to create one and need some guidance, consider using the [postgresqlCONF](https://postgresqlco.nf) service, which gives you detailed parameter information in several langauges, recommendations, a tuning guide and even a facility to store and manage your Postgres configurations online.

StackGres hides the implementation details of how Postgres configuration file works, and how to configure it with Patroni. Instead, a specialized CRD is exposed for you to manage Postgres configurations, called [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}})`. Postgres configurations are created (and/or modified) once and then can be used in zero, one or multiple clusters. No need to repeat the configuration in every cluster.

Configurations are assigned a name, which you may reference later from one or more Postgres clusters. Create the file `sgpostgresconfig-config1.yaml`:

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  namespace: demo
  name: pgconfig1
spec:
  postgresVersion: "14"
  postgresql.conf:
    work_mem: '16MB'
    shared_buffers: '2GB'
    random_page_cost: '1.5'
    password_encryption: 'scram-sha-256'
    log_checkpoints: 'on'
    jit: 'off'
```

and deploy to Kubernetes:

```bash
kubectl apply -f sgpostgresconfig-config1.yaml
```

You may use `kubectl describe` on the created resource to inspect the values that are injected (tuned by default), as
well as an indication in the `status` field of which values are using StackGres defaults:

```bash
kubectl -n demo describe sgpgconfig pgconfig1
```

```plain
Name:         pgconfig1
Namespace:    demo
Labels:       <none>
Annotations:  stackgres.io/operatorVersion: 1.3.3
API Version:  stackgres.io/v1
Kind:         SGPostgresConfig
Metadata:
  Creation Timestamp:  2022-10-26T10:30:15Z
  Generation:          1
  Managed Fields:
    API Version:  stackgres.io/v1
    Fields Type:  FieldsV1
    fieldsV1:
      f:spec:
        .:
        f:postgresVersion:
        f:postgresql.conf:
          .:
          f:jit:
          f:log_checkpoints:
          f:password_encryption:
          f:random_page_cost:
          f:shared_buffers:
          f:work_mem:
    Manager:         kubectl
    Operation:       Update
    Time:            2022-10-26T10:30:15Z
  Resource Version:  1891
  UID:               5d0e9cd4-891a-41e6-af45-0875867e62fd
Spec:
  Postgres Version:  14
  postgresql.conf:
    autovacuum_max_workers:            3
    autovacuum_vacuum_cost_delay:      2
    autovacuum_work_mem:               512MB
    checkpoint_completion_target:      0.9
    checkpoint_timeout:                15min
    default_statistics_target:         200
    enable_partitionwise_aggregate:    on
    enable_partitionwise_join:         on
    huge_pages:                        off
    Jit:                               off
    jit_inline_above_cost:             -1
    log_autovacuum_min_duration:       0ms
    log_checkpoints:                   on
    log_connections:                   on
    log_disconnections:                on
    log_line_prefix:                   %t [%p]: db=%d,user=%u,app=%a,client=%h 
    log_lock_waits:                    on
    log_min_duration_statement:        1000
    log_statement:                     none
    log_temp_files:                    0
    maintenance_work_mem:              2GB
    max_locks_per_transaction:         128
    max_pred_locks_per_transaction:    128
    max_prepared_transactions:         32
    max_replication_slots:             20
    max_wal_senders:                   20
    max_wal_size:                      2GB
    min_wal_size:                      1GB
    password_encryption:               scram-sha-256
    pg_stat_statements.track_utility:  off
    random_page_cost:                  1.5
    shared_buffers:                    2GB
    shared_preload_libraries:          pg_stat_statements, auto_explain
    superuser_reserved_connections:    8
    track_activity_query_size:         4096
    track_functions:                   pl
    track_io_timing:                   on
    wal_keep_size:                     1536MB
    work_mem:                          16MB
Status:
  Default Parameters:
    archive_command:                   /bin/true
    archive_mode:                      on
    autovacuum_max_workers:            3
    autovacuum_vacuum_cost_delay:      2
    autovacuum_work_mem:               512MB
    checkpoint_completion_target:      0.9
    checkpoint_timeout:                15min
    default_statistics_target:         200
    enable_partitionwise_aggregate:    on
    enable_partitionwise_join:         on
    Fsync:                             on
    hot_standby:                       on
    huge_pages:                        off
    jit_inline_above_cost:             -1
    lc_messages:                       C
    listen_addresses:                  localhost
    log_autovacuum_min_duration:       0ms
    log_checkpoints:                   on
    log_connections:                   on
    log_destination:                   stderr
    log_directory:                     log
    log_disconnections:                on
    log_filename:                      postgres-%M.log
    log_line_prefix:                   %t [%p]: db=%d,user=%u,app=%a,client=%h 
    log_lock_waits:                    on
    log_min_duration_statement:        1000
    log_rotation_age:                  30min
    log_rotation_size:                 0kB
    log_statement:                     none
    log_temp_files:                    0
    log_truncate_on_rotation:          on
    logging_collector:                 off
    maintenance_work_mem:              2GB
    max_locks_per_transaction:         128
    max_pred_locks_per_transaction:    128
    max_prepared_transactions:         32
    max_replication_slots:             20
    max_wal_senders:                   20
    max_wal_size:                      2GB
    min_wal_size:                      1GB
    pg_stat_statements.track_utility:  off
    random_page_cost:                  1.5
    shared_preload_libraries:          pg_stat_statements, auto_explain
    superuser_reserved_connections:    8
    track_activity_query_size:         4096
    track_commit_timestamp:            on
    track_functions:                   pl
    track_io_timing:                   on
    wal_compression:                   on
    wal_keep_size:                     1536MB
    wal_level:                         logical
    wal_log_hints:                     on
    work_mem:                          10MB
Events:                                <none>
```
