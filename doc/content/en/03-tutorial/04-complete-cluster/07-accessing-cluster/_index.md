---
title: Accessing the cluster
weight: 7
url: tutorial/complete-cluster/accessing-cluster
description: Details about how to access a database cluster.
---

## Accessing the Postgres Database Via the Services

By default, StackGres creates two Kubernetes services that expose the Postgres cluster: one for reads and writes and one for reads (`-replicas`), which is load balanced across all replicas.
The name of the service for reads and writes is equal to the name of the cluster.
The name of the service for reads only also contains the suffix `-replicas`.

Previously, we connected to the Postgres server by executing `psql` from the `postgres-util` container.
That's fine for administration purposes, but users of the database will likely connect via the Postgres protocol from other pods.
For this, they will use these services as the endpoint to which to connect.
Here you could use any example application that supports Postgres.
For the purpose of this lab, we will again use `psql`, but executed from a separate pod.
For this, we need a container that includes `psql`.
For example, we can create a separate pod from the StackGres `postgres-util` image:

```
$ kubectl -n demo run psql --rm -it --image ongres/postgres-util --restart=Never -- psql -h cluster postgres postgres
```

This time, the `psql` command will ask for a password.
If you let the authentication fail, e.g. by typing an arbitrary password, the command will abort.
This time, we're connecting via Postgres wire protocol, and StackGres configures Postgres to require authentication.
The password for the `postgres` user (superuser in Postgres) is generated randomly when the cluster is created.
You can retrieve it from the appropriate secret, named as the cluster, and obtaining the key `"superuser-password"` (note that the quoting is required since the key includes a dash).

```
$ kubectl -n demo get secret cluster --template '{{ printf "%s" (index .data "superuser-password" | base64decode) }}'
``` 

Now, you should be able to connect via `psql` using the displayed password.
The command above used the service for reads and writes (`cluster`). It's also possible to use the `-replicas` service, by specifying `-h cluster-replicas` in the `psql` connection string.
Now, your applications are ready to use your Postgres database.


## Accessing Postgres and Patroni Logs

Accessing the logs is easy: go to the web console, navigate to the cluster, and click on the `Logs` pane.

But now, let's do it from the CLI.
Because StackGres stores Postgres logs on a (separate) Postgres database, enhanced by the time-series TimescaleDB extension, you are able to connect to the database and query the logs with SQL.
Indeed, the `SGDistributedLogs` resource that we created before led to the creation of a specialized `SGCluster`, used for logs.
So how to connect to this cluster? In the same way that we just connected to the main cluster: through the read and write service.
In this case, the host name equals the name specified in the `SGDistributedLogs` resource, in our case `distributedlogs`.

In the same way as before, we can retrieve the connection password from the `distributedlogs` secret:

```
$ PGPASSWORD=$(kubectl -n demo get secret distributedlogs --template '{{ printf "%s" (index .data "superuser-password" | base64decode) }}')
```

Then, we can connect to our distributed logs cluster via `psql`:

```
$ kubectl -n demo run psql --env $PGPASSWORD --rm -it --image ongres/postgres-util --restart=Never -- psql -h distributedlogs postgres postgres
```

Now that we're in `psql`, we can query the logs with SQL.
The following commands will list the databases, connect to the database for our current cluster, count the Postgres log entries, describe the logs table, and select all logs of type `ERROR`.
There will be one database per every cluster that is sending logs to this distributed logs server, with the naming scheme `<namespace>_<cluster>`.
You can generate `ERROR` logs by typing any SQL error into the SQL console of the source cluster (not this one).
Logs may take a few seconds to propagate.

```
\l+
\c demo_cluster
\dt log_postgres
select count(*) from log_postgres;
select * from log_postgres where error_severity = 'ERROR';
```

> `\l+` Lists the databases (with additional details) \
> `\c <db>` Connects to a database \
> `\dt <table>` Lists all tables (if `<table>` isn't specified) or a specific table


## Adding Postgres Extensions

Let's add some extensions.
Postgres extensions are awesome and possibly one of the most recognized Postgres features.

Let's first connect to the `cluster` cluster, and run a command to list the extensions available:

```
postgres=# select * from pg_available_extensions();
        name        | default_version |                           comment                            
--------------------+-----------------+--------------------------------------------------------------
 dblink             | 1.2             | connect to other PostgreSQL databases from within a database
 plpgsql            | 1.0             | PL/pgSQL procedural language
 pg_stat_statements | 1.7             | track execution statistics of all SQL statements executed
 plpython3u         | 1.0             | PL/Python3U untrusted procedural language
(4 rows)
```

That looks like a very limited set of extensions.
Can we increase this set?
Fortunately, yes.
StackGres has developed an innovative mechanism to load Postgres extensions on-demand, onto the otherwise immutable container images.
Required extensions can be specified declaratively in the cluster definition.
Let's load the `ltree` extension into our cluster.

Edit the cluster's YAML file and add the following section inside the `postgres` spec:

```yaml
kind: SGCluster
# ...
spec:
  postgres:
    extensions:
    - name: 'ltree'
    # ...
  # ...
```

After applying the change to Kubernetes (`kubectl apply`), the extension will be dynamically downloaded onto the container.
After a few seconds, repeating the previous SQL query in the Postgres console will now show the `ltree` extension.

Now, you just need to run `create extension ltree;` on the databases that you want the extension to be installed in, and you're ready to go.

To list the currently available extensions and versions, please check the web console.
Also note that installing some extensions may require a cluster restart.
This restriction will be lifted in future versions.


## Running a benchmark

StackGres supports automating "Day 2 Operations".
These operations are performed via a CRD called `SGDbOps`.
`SGDbOps` support several kind of operations, and more will be added in future versions.
Let's try one of them that runs a benchmark.

Create and apply the following YAML file:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: demo
  name: pgbench1
spec:
  op: benchmark
  sgCluster: cluster
  benchmark:
    type: pgbench
    pgbench:
      databaseSize: 1Gi
      duration: P0DT0H2M
      usePreparedStatements: false
      concurrentClients: 20 
      threads: 8 
```

Upon creating this resource, StackGres will schedule and run a benchmark.
The results of the benchmark will be written in the `.Status` field of the CRD, which you can query with `kubectl describe`.
You may also check them from the web console.
