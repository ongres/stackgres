---
title: PGBadger reports
weight: 1
url: runbooks/pgbadger-reports
description: Details about how to generate a pgbadger report from the distributed logs server.
showToc: true
---

This tutorial expects that you have pgbadger installed on your machine. Check the [installation procedure](http://pgbadger.darold.net/documentation.html#INSTALLATION) to get it running properly.

## Before you start

Before start, be sure that you have a `SGCluster` runing that is using a `SGDistributedLogs` server, like below:

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
> Remember to change the `size` according with your needs.


### PostgreSQL Configuration for PGBadger

To generate a pgbadger report, a few configuration parameters are necessary:

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

Check [pgbadger documentation](http://pgbadger.darold.net/documentation.html#POSTGRESQL-CONFIGURATION) for more tails about the necessary parameters to setup Postgres.

### Cluster configuration

The final `SGCluster` should be something like this:

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

## Exporting the log files into CSV

Execute the command below to locate the pod of the distributed log server:

```
$ kubectl get pods -o name -l distributed-logs-name=my-distributed-logs 
pod/my-distributed-logs-0
```

Connect on the distributed server and export the log into the CSV format:

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
> Add a `WHERE` clause on the `SELECT` to filter the log on the necessary period, like this:
>
> ```
>  --- ...
>  WHERE  log_time > 'begin timestamp' and log_time < 'end timestamp'
> ```

With the csv file, just call pgbadger:

```
pgbadger --format csv --outfile pgbadger_report.html data.csv
```

### All in one script

PGbadger has support to a external command to get the log info, using that is possible to create a all-in-one script to generate the pgbadger report.

```
POD=$(kubectl get pods -o name -l distributed-logs-name=my-distributed-logs)
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
