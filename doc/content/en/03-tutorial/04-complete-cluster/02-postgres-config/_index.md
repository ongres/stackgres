---
title: Custom Postgres Configuration
weight: 2
url: tutorial/complete-cluster/postgres-config
---

While StackGres comes with a carefully tuned, default Postgres configuration, you may want to set your own configuration
and parameters. StackGres uses for this purpose the CRD
[SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}). By using a CRD, coupled with a webhook
validator, instead of a simple ConfigMap, StackGres is able to strongly validate the desired configuration, ensuring
that parameters and values are valid for the major Postgres version, and within bounds.

Configurations are assigned a name, which you may reference later from one or more Postgres clusters. Create the file
`sgpostgresconfig-config1.yaml`:

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  namespace: demo
  name: pgconfig1
spec:
  postgresVersion: "12"
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
Annotations:  stackgres.io/operatorVersion: 1.0.0-alpha1
API Version:  stackgres.io/v1
Kind:         SGPostgresConfig
Metadata:
  Creation Timestamp:  2021-03-01T10:07:10Z
  Generation:          1
  Resource Version:    152394
  Self Link:           /apis/stackgres.io/v1/namespaces/demo/sgpgconfigs/pgconfig1
  UID:                 46d3a5c8-6d96-4082-97f3-ae9a66c25237
Spec:
  Postgres Version:  12
  postgresql.conf:
    autovacuum_max_workers:            3
    autovacuum_vacuum_cost_delay:      2
    checkpoint_completion_target:      0.9
    checkpoint_timeout:                15min
    default_statistics_target:         200
    enable_partitionwise_aggregate:    on
    enable_partitionwise_join:         on
    Jit:                               off
    jit_inline_above_cost:             -1
    log_autovacuum_min_duration:       0
    log_checkpoints:                   on
    log_connections:                   on
    log_disconnections:                on
    log_line_prefix:                   %t [%p]: db=%d,user=%u,app=%a,client=%h 
    log_lock_waits:                    on
    log_min_duration_statement:        1000
    log_statement:                     ddl
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
    wal_keep_segments:                 100
    work_mem:                          16MB
Status:
  Default Parameters:
    enable_partitionwise_aggregate
    min_wal_size
    max_wal_senders
    log_checkpoints
    max_prepared_transactions
    checkpoint_timeout
    autovacuum_max_workers
    jit_inline_above_cost
    track_functions
    wal_keep_segments
    checkpoint_completion_target
    enable_partitionwise_join
    log_autovacuum_min_duration
    superuser_reserved_connections
    log_temp_files
    log_lock_waits
    random_page_cost
    max_locks_per_transaction
    log_disconnections
    maintenance_work_mem
    log_connections
    shared_preload_libraries
    pg_stat_statements.track_utility
    track_activity_query_size
    max_pred_locks_per_transaction
    max_wal_size
    autovacuum_vacuum_cost_delay
    log_min_duration_statement
    log_statement
    max_replication_slots
    default_statistics_target
    log_line_prefix
    track_io_timing
Events:  <none>
```
