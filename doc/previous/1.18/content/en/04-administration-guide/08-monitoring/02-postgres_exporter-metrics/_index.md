---
title: postgres_exporter
weight: 2
url: /administration/monitoring/postgres_exporter
aliases: [ /monitoring/metrics/postgres_exporter ]
description: Contains details about the metrics collected by the postgres_exporter.
showToc: true
---

The next tables contain details about the metrics collected by the postgres_exporter.

## Postgres cluster metrics

|item| metric group | metric | type        | description |
|----|--------------|--------|-------------|-------------|
| 1 | pg_postmaster | start_time_seconds | GAUGE | Time at which postmaster started |
| 2 | pg_replication|        |             |    |
|   |              | lag      | GAUGE  | Replication lag behind master in seconds |
|   |              | is_replica | GAUGE    | Indicates if this host is a slave |
| 3 | pg_stat_user_tables |        |    |         |
|   |              | datname | LABEL| Name of current database |
|   |              | schemaname | LABEL | Name of the schema that this table is in |
|   |              | relname | LABEL | Name of this table |
|   |              | seq_scan | COUNTER   | Number of sequential scans initiated on this table |
|   |              | seq_tup_read | COUNTER | Number of live rows fetched by sequential scans |
|   |              | idx_scan | COUNTER | Number of index scans initiated on this table|
|   |              | idx_tup_fetch | COUNTER | Number of live rows fetched by index scans |
|   |              | n_tup_ins | COUNTER | Number of rows inserted |
|   |              | n_tup_upd | COUNTER | Number of rows updated |
|   |              | n_tup_del | COUNTER | Number of rows deleted |
|   |              | n_tup_hot_upd | COUNTER | Number of rows HOT updated (i.e., with no separate index update required) |
|   |              | n_live_tup | GAUGE | Estimated number of live rows |
|   |              | n_dead_tup | GAUGE | Estimated number of dead rows |
|   |              | n_mod_since_analyze | GAUGE | Estimated number of rows changed since last analyze |
|   |              | last_vacuum | GAUGE | Last time at which this table was manually vacuumed (not counting VACUUM FULL) |
|   |              | last_autovacuum | GAUGE | Last time at which this table was vacuumed by the autovacuum daemon |
|   |              | last_analyze | GAUGE | Last time at which this table was manually analyzed |
|   |              | last_autoanalyze | GAUGE | Last time at which this table was analyzed by the autovacuum daemon |
|   |              | vacuum_count | COUNTER | Number of times this table has been manually vacuumed (not counting VACUUM FULL) |
|   |              | autovacuum_count | COUNTER | Number of times this table has been vacuumed by the autovacuum daemon |
|   |              | analyze_count | COUNTER | Number of times this table has been manually analyzed |
|   |              | autoanalyze_count | COUNTER | Number of times this table has been analyzed by the autovacuum daemon |
| 4 | pg_statio_user_tables |  | | |
|   |              | datname | LABEL | Name of current database |
|   |              | schemaname | LABEL | Name of the schema that this table is in |
|   |              | relname | LABEL | Name of this table |
|   |              | heap_blks_read | COUNTER | Number of disk blocks read from this table |
|   |              | heap_blks_hit | COUNTER | Number of buffer hits in this table |
|   |              | idx_blks_read | COUNTER | Number of disk blocks read from all indexes on this table |
|   |              | idx_blks_hit | COUNTER | Number of buffer hits in all indexes on this table |
|   |              | toast_blks_read | COUNTER | Number of disk blocks read from this table's TOAST table (if any) |
|   |              | toast_blks_hit  | COUNTER | Number of buffer hits in this table's TOAST table (if any) |
|   |              | tidx_blks_read | COUNTER | Number of disk blocks read from this table's TOAST table indexes (if any) |
|   |              | tidx_blks_hit | COUNTER | Number of buffer hits in this table's TOAST table indexes (if any) |
| 5 | pg_database  |  | | |
|   |              | datname | LABEL | Name of the database |
|   |              | size_bytes | GAUGE | Disk space used by the database |
| 6 | pg_archiver  |  | | |
|   |              | pending_wal_count | GAUGE | No. of pending WAL files to be archived |
| 7 | pg_stat_user_indexes |  | | |
|   |              | schemaname | LABEL | Name of the schema that this table is in |
|   |              | relname | LABEL | Name of the table for this index |
|   |              | indexrelname | LABEL | Name of this index |
|   |              | idx_scan | COUNTER | Number of index scans initiated on this index |
|   |              | idx_tup_read | COUNTER | Number of index entries returned by scans on this index |
|   |              | idx_tup_fetch | COUNTER | Number of live table rows fetched by simple index scans using this index |
| 8 | pg_statio_user_indexes |  | | |
|   |              | schemaname | LABEL | Name of the schema that this table is in |
|   |              | relname | LABEL | Name of the table for this index |
|   |              | indexrelname | LABEL | Name of this index |
|   |              | idx_blks_read | COUNTER | Number of disk blocks read from this index |
|   |              | idx_blks_hit | COUNTER | Number of buffer hits in this index |
| 9 | pg_total_relation_size |  | | |
|   |              | datname | LABEL | Database name |
|   |              | schemaname | LABEL | Name of the schema that this table is in |
|   |              | relname | LABEL | Name of this table |
|   |              | bytes | GAUGE | total disk space usage for the specified table and associated indexes |
| 10| pg_blocked   |  | | |
|   |              | type | LABEL | The lock type |
|   |              | datname | LABEL | Database name |
|   |              | schemaname | LABEL | The schema on which a query is blocked |
|   |              | reltype | LABEL | The type of relation |
|   |              | relname | LABEL | The relation on which a query is blocked |
|   |              | queries | GAUGE | The current number of blocked queries |
| 11| pg_oldest_blocked |  | | |
|   |              | age_seconds | GAUGE | Largest number of seconds any transaction is currently waiting on a lock |
|   |              | datname | LABEL | Database name |
| 12| pg_slow      |  | | |
|   |              | queries | GAUGE | Current number of slow queries |
|   |              | datname | LABEL | Database name |
| 13| pg_long_running_transactions |  | | |
|   |              | datname | LABEL | Database name |
|   |              | queries | GAUGE | Current number of long running transactions |
|   |              | age_in_seconds | GAUGE | The current maximum transaction age in seconds |
| 14| pg_vacuum    |  | | |
|   |              | datname | LABEL | Database name |
|   |              | queries | GAUGE | The current number of VACUUM queries |
|   |              | age_in_seconds | GAUGE | The current maximum VACUUM query age in seconds |
| 15| pg_vacuum_analyze |  | | |
|   |              | datname | LABEL | Database name |
|   |              | queries | GAUGE | The current number of VACUUM ANALYZE queries |
|   |              | age_in_seconds | GAUGE | The current maximum VACUUM ANALYZE query age in seconds |
| 16| pg_stuck_idle_in_transaction | | | |
|   |              | datname | LABEL | Database name |
|   |              | queries | GAUGE | Current number of queries that are stuck being idle in transactions |
| 17| pg_txid             |  | | |
|   |              | current | COUNTER | Current 64-bit transaction id of the query used to collect this metric (truncated to low 52 bits) |
|   |              | xmin | COUNTER | Oldest transaction id of a transaction still in progress, i.e. not known committed or aborted (truncated to low 52 bits) |
|   |              | xmin_age | GAUGE | Age of oldest transaction still not committed or aborted measured in transaction ids |
| 18| pg_database_datfrozenxid |  | | |
|   |              | datname | LABEL | Database name |
|   |              | age | GAUGE | Age of the oldest transaction that has not been frozen |
| 19| pg_wal_position             |  | | |
|   |              | bytes | COUNTER | Postgres LSN (log sequence number) being generated on the primary instance or replayed on a replica (truncated to low 52 bits) |
| 20| pg_replication_slots             |  | | |
|   |              | slot_name | LABEL | Slot Name |
|   |              | slot_type | LABEL | Slot Type |
|   |              | active | GAUGE | Boolean flag indicating whether this slot has a consumer streaming from it |
|   |              | xmin_age |GAUGE | Age of oldest transaction that cannot be vacuumed due to this replica |
|   |              | catalog_xmin_age | GAUGE | Age of oldest transaction that cannot be vacuumed from catalogs due to this replica (used by logical replication) |
|   |              | restart_lsn_bytes | GAUGE | Amount of data in the xlog that this replica may need to complete recovery |
|   |              | confirmed_flush_lsn_bytes | GAUGE | Amount of data in the xlog that this replica has not yet received |
| 21| pg_stat_ssl  |  | | |
|   |              | pid | LABEL | Process ID of a backend or WAL sender process |
|   |              | active | GAUGE | Boolean flag indicating if SSL is used on this connection |
|   |              | bits | GAUGE | Number of bits in the encryption algorithm in use |
|   |              | compression | GAUGE | Boolean flag indicating if SSL compression is in use |
| 22| pg_table_bloat  |  | | |
|   |              | datname | LABEL | Database name |
|   |              | schemaname | LABEL | Schema name |
|   |              | tablename | LABEL | Table name |
|   |              | real_size | GAUGE | Table real size |
|   |              | extra_size | GAUGE | Estimated extra size not used/needed in the table. This extra size is composed by the fillfactor, bloat and alignment padding spaces |
|   |              | extra_ratio | GAUGE | Estimated ratio of the real size used by extra_size |
|   |              | fillfactor | GAUGE | Table fillfactor |
|   |              | bloat_size | GAUGE | Estimated size of the bloat without the extra space kept for the fillfactor |
|   |              | bloat_ratio | GAUGE | Estimated ratio of the real size used by bloat_size |
|   |              | is_na | GAUGE | Estimation not applicable, If true, do not trust the stats |
| 23| pg_index  |  | | |
|   |              | datname | LABEL | Database name |
|   |              | schema_name | LABEL | Schema name |
|   |              | tblname | LABEL | Table name |
|   |              | idxname | LABEL | Index Name |
|   |              | real_size | GAUGE | Index size |
|   |              | extra_size | GAUGE | Index extra size |
|   |              | extra_ratio | GAUGE | Index extra ratio |
|   |              | fillfactor | GAUGE | Fillfactor |
|   |              | bloat_size | GAUGE | Estimate index bloat size |
|   |              | bloat_ratio | GAUGE | Estimate index bloat size ratio |
|   |              | is_na | GAUGE | Estimate not applicable, bad statistic |
| 24| pg_replication_status  |  | | |
|   |              | application_name | LABEL | Application or node name |
|   |              | client_addr | LABEL | Client ip address |
|   |              | state | LABEL | Client replication state |
|   |              | lag_size_bytes | GAUGE | Replication lag size in bytes |


## Connection pooling metrics

|item| metric group | metric | type        | description |
|----|--------------|--------|-------------|-------------|
| 1 | pgbouncer_show_clients  |  | | |
|   |              | type | LABEL | C, for client |
|   |              | user | LABEL | Client connected user |
|   |              | database | LABEL | Database name |
|   |              | state | LABEL | State of the client connection, one of active or waiting |
|   |              | addr | LABEL | IP address of client |
|   |              | port | GAUGE | Port client is connected to |
|   |              | local_addr | LABEL | Connection end address on local machine |
|   |              | local_port | GAUGE | Connection end port on local machine |
|   |              | connect_time | LABEL | Timestamp of connect time |
|   |              | request_time | LABEL | Timestamp of latest client request |
|   |              | wait | GAUGE | Current waiting time in seconds |
|   |              | wait_us | GAUGE | Microsecond part of the current waiting time |
|   |              | close_needed | GAUGE | not used for clients |
|   |              | ptr | LABEL | Address of internal object for this connection. Used as unique ID |
|   |              | link | LABEL | Address of server connection the client is paired with |
|   |              | remote_pid | GAUGE | Process ID, in case client connects over Unix socket and OS supports getting it |
|   |              | tls | LABEL | A string with TLS connection information, or empty if not using TLS |
| 2 | pgbouncer_show_pools  |  | | |
|   |              | database | LABEL | Database name |
|   |              | user | LABEL | User name|
|   |              | cl_active | GAUGE | Client connections that are linked to server connection and can process queries |
|   |              | cl_waiting | GAUGE | Client connections that have sent queries but have not yet got a server connection |
|   |              | sv_active | GAUGE | Server connections that are linked to a client |
|   |              | sv_idle | GAUGE | Server connections that are unused and immediately usable for client queries |
|   |              | sv_used | GAUGE | Server connections that have been idle for more than server_check_delay so they need server_check_query to run on them |
|   |              | sv_tested | GAUGE | Server connections that are currently running either server_reset_query or server_check_query |
|   |              | sv_login | GAUGE | Server connections currently in the process of logging in |
|   |              | maxwait | GAUGE | How long the first oldest client in the queue has waited, in seconds |
|   |              | maxwait_us | GAUGE | Microsecond part of the maximum waiting time |
|   |              | pool_mode | LABEL | The pooling mode in use |
| 3 | pgbouncer_show_databases  |  | | |
|   |              | name | LABEL | Name of configured database entry |
|   |              | host | LABEL | Host pgbouncer connects to |
|   |              | port | GAUGE | Port pgbouncer connects to |
|   |              | database | LABEL | Actual database name pgbouncer connects to |
|   |              | force_user | LABEL | When the user is part of the connection string the connection between pgbouncer and PostgreSQL is forced to the given user |
|   |              | pool_size | GAUGE | Maximum number of server connections |
|   |              | reserve_pool | GAUGE | Maximum number of additional connections for this database |
|   |              | pool_mode | LABEL | The database override pool_mode |
|   |              | max_connections | GAUGE | Maximum number of allowed connections for this database |
|   |              | current_connections | GAUGE | Current number of connections for this database |
|   |              | paused | GAUGE | 1 if this database is currently paused, else 0 |
|   |              | disabled | GAUGE | 1 if this database is currently disabled, else 0 |
| 4 | pgbouncer_show_stats_totals  |  | | |
|   |              | database | LABEL | Database name |
|   |              | xact_count | GAUGE | Number of SQL transactions pooled |
|   |              | query_count | GAUGE | Number of SQL queries pooled |
|   |              | bytes_received | GAUGE | Volume in bytes of network traffic received |
|   |              | bytes_sent | GAUGE | Volume in bytes of network traffic sent |
|   |              | xact_time | GAUGE | Number of microseconds spent by pgbouncer when connected to PostgreSQL in a transaction |
|   |              | query_time | GAUGE | Number of microseconds spent by pgbouncer when actively connected to PostgreSQL |
| 5 | pgbouncer_show_stats  |  | | |
|   |              | database | LABEL | Database name |
|   |              | total_xact_count | GAUGE | Total number of SQL transactions pooled |
|   |              | total_query_count | GAUGE | Total number of SQL queries pooled |
|   |              | total_received | GAUGE | Total volume in bytes of network traffic received |
|   |              | total_sent | GAUGE | Total volume in bytes of network traffic sent |
|   |              | total_xact_time | GAUGE | Total number of microseconds spent by pgbouncer when connected to PostgreSQL in a transaction |
|   |              | total_query_time | GAUGE | Total number of microseconds spent by pgbouncer when actively connected to PostgreSQL |
|   |              | total_wait_time | GAUGE | Time spent by clients waiting for a server, in microseconds |
|   |              | avg_xact_count | GAUGE | Average transactions per second in last stat period |
|   |              | avg_query_count | GAUGE | Average queries per second in last stat period |
|   |              | avg_recv | GAUGE | Average received from clients bytes per second |
|   |              | avg_sent | GAUGE | Average sent to clients bytes per second |
|   |              | avg_xact_time | GAUGE | Average transaction duration, in microseconds |
|   |              | avg_query_time | GAUGE | Average query duration, in microseconds |
|   |              | avg_wait_time | GAUGE | Time spent by clients waiting for a server, in microseconds average per second |
