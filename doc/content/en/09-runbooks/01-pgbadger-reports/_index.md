---
title: PgBadger Reports
weight: 1
url: /runbooks/pgbadger-reports
description: Details about how to generate a pgBadger report from the distributed logs server.
showToc: true
---

This tutorial expects that you have pgBadger installed on your machine.
Check the [installation procedure](http://pgbadger.darold.net/documentation.html#INSTALLATION) to get it running properly.

## Before You Start

Before you start, be sure that you have an StackGres cluster running that is configured to use a distributed logs server, like below:

```yaml
---
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  namespace: default
  name: my-distributed-logs
spec:
  persistentVolume:
    size: 20Gi
```
> Change the `size` according to your needs.


### PostgreSQL Configuration for PgBadger

To generate a pgBadger report, a few configuration parameters are necessary:

```yaml
---
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  name: my-postgres-config
  namespace: default
spec:
  postgresVersion: "12"
  postgresql.conf:

    # Logging configuration for pgbadger
    log_checkpoints: 'on'
    log_connections: 'on'
    log_disconnections: 'on'
    log_lock_waits: 'on'
    log_temp_files: '0'

    # Adjust the minimum time to collect data
    log_min_duration_statement: '5s'
    log_autovacuum_min_duration: '0'
```

Check the [pgBadger documentation](http://pgbadger.darold.net/documentation.html#POSTGRESQL-CONFIGURATION) for more tails about the necessary parameters to set up Postgres.

### Cluster configuration

The final StackGres cluster should be something like this:

```yaml
---
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-db-cluster
  namespace: default
spec:
# ...
  configurations:
    sgPostgresConfig: my-postgres-config
  distributedLogs: 
    sgDistributedLogs: my-distributed-logs
```

## Exporting the Log Files into CSV

Execute the command below to locate the pod of the distributed log server:

```
$ kubectl get pods -o name -l stackgres.io/distributed-logs-name=my-distributed-logs 
pod/my-distributed-logs-0
```

Connect to the distributed server and export the logs into CSV format:

```
QUERY=$(cat <<EOF
COPY (
  SELECT 
    log_time, 
    user_name,
    database_name,
    process_id,
    connection_from,
    session_id,
    session_line_num,
    command_tag,
    session_start_time,
    virtual_transaction_id,
    transaction_id,
    error_severity,
    sql_state_code,
    message,
    detail,
    hint,
    internal_query,
    internal_query_pos,
    context,
    query,
    query_pos,
    "location",
    application_name 
  FROM log_postgres 
) to STDOUT CSV DELIMITER ',';

EOF
)

kubectl exec -it pod/my-distributed-logs-0 -c patroni -- psql default_my-db-cluster -At -c "${QUERY}" > data.csv
```
> Add a `WHERE` clause on the `SELECT` to filter the logs on the necessary period, like this:
>
> ```
>  --- ...
>  WHERE  log_time > 'begin timestamp' and log_time < 'end timestamp'
> ```

With the CSV file, invoke pgBadger:

```
pgbadger --format csv --outfile pgbadger_report.html data.csv
```

### All-In-One Script

PgBadger supports defining an external command to get the log info.
Using that, it is possible to create an all-in-one script that generates the pgBadger report.

```
POD=$(kubectl get pods -o name -l stackgres.io/distributed-logs-name=my-distributed-logs)
CLUSTER_NAME="my-db-cluster"
QUERY=$(cat <<EOF
COPY (
  SELECT 
    log_time, 
    user_name,
    database_name,
    process_id,
    connection_from,
    session_id,
    session_line_num,
    command_tag,
    session_start_time,
    virtual_transaction_id,
    transaction_id,
    error_severity,
    sql_state_code,
    message,
    detail,
    hint,
    internal_query,
    internal_query_pos,
    context,
    query,
    query_pos,
    "location",
    application_name 
  FROM log_postgres 
) to STDOUT CSV DELIMITER ',';

EOF
)

pgbadger \
  --format csv \
  --outfile pgbadger_report.html \
  --command "kubectl exec -it ${POD} -c patroni -- psql default_${CLUSTER_NAME} -At -c \"${QUERY}\""
```
