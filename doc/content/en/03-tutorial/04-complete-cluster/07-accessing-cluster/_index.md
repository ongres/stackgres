---
title: Accessing the cluster
weight: 7
url: tutorial/complete-cluster/accessing-cluster
description: Details about how to access the database cluster.
---

## Accesing the Postgres database via the services

StackGres creates, by default, two Kubernetes services to expose the Postgres cluster: one for reads and writes (`-primary`) and one for reads (`-replicas`), which is load balanced across all replicas. The name of the services is the name of the cluster plus one of the two suffixes (e.g. `cluster-primary`).

Previously, we connected to the Postgres server by executing `psql` from the `postgres-util` container. That's fine for administration purposes, but users of the database will likely connect via the Postgres protocol from other pods. For this, they will use these services as the endpoint to which to connect. You could use here any application that supports Postgres. For the purpose of this lab, we will again use `psql`, but run from a separate pod that will contain a container that in turn contains `psql`. For example, the very same `postgres-util` container of the StackGres project --but run externally to the StackGres cluster, as a separate pod. Run:

```
$ kubectl -n demo run psql --rm -it --image ongres/postgres-util --restart=Never -- psql -h cluster-primary postgres postgres
```

It will ask for a password. Probably you don't need it, so let it fail (e.g. type any password). It will abort. This time we're connecting via Postgres wire protocol, and StackGres configures Postgres to require authentication. The `postgres` (superuser account in Postgres) password is generated randomly when the cluster is created. You can retrieve it from the appropriate secret, named as the cluster, and obtaining the key `"superuser-password"` (note the quoting, it is required as it is using a dash).

```
$ kubectl -n demo get secret cluster --template '{{ printf "%s" (index .data "superuser-password" | base64decode) }}'
``` 

Now you should be able to connect and operate normally. Also to the `-replicas` service, by using `-h cluster-replicas` in the `psql` connection string. Your applications are now ready to use Postgres!


## Accesing Postgres and Patroni logs

It's easy: go to the Web Console, to the cluster, and click on the `Logs` pane. But now, let's do from the CLI. Because StackGres stores Postgres logs on a (separate!) Postgres database, enhanced by the time-series TimescaleDB extension, you are able to connect to the database and query the logs with SQL! Indeed, the `SGDistributedLogs` CR that we created before led to the creation of a specialized `SGCluster`, used for logs. So how to connect to it? The same way we just connected to the main cluster: through the `-primary` service, where in this case the host is obviously the `distributedlogs` name that we use for the `SGDistributedLogs` CR. Similarly, we can retrieve the connection password from the equivalent secret:

```
$ PGPASSWORD=$(kubectl -n demo get secret distributedlogs --template '{{ printf "%s" (index .data "superuser-password" | base64decode) }}')
```

```
$ kubectl -n demo run psql --env $PGPASSWORD --rm -it --image ongres/postgres-util --restart=Never -- psql -h distributedlogs-primary postgres postgres
```

Now that we're in `psql`, we can query the logs with SQL. Let's do that! The following sequence of commands will allow you to list the databases (there will be one per every cluster that is sending logs to this distributed logs server, named `$namespace_$cluster`), connect to the database for our current cluster, count the Postgres log entries, describe the logs table and select all logs of type `ERROR` (you can generate such logs, if you don't have any, by typing any SQL error into the SQL console --of the source cluster, not this one; bear in mind that logs take a few seconds to propagate, may not appear instantly).

```sql
\l+
\c demo_cluster
\dt log_postgres
select count(*) from log_postgres;
select * from log_postgres where error_severity = 'ERROR';
```


## Let's add some extensions!

Postgres extensions are awesome. Possibly, one of the most appreciated Postgres features.

Let's first connect to the `cluster` cluster, and run a command to list the extensions available:

```sql
postgres=# select * from pg_available_extensions();
        name        | default_version |                           comment                            
--------------------+-----------------+--------------------------------------------------------------
 dblink             | 1.2             | connect to other PostgreSQL databases from within a database
 plpgsql            | 1.0             | PL/pgSQL procedural language
 pg_stat_statements | 1.7             | track execution statistics of all SQL statements executed
 plpython3u         | 1.0             | PL/Python3U untrusted procedural language
(4 rows)
```

That looks like a very limited set of extensions available. Can we do better? Fortunately, yes. StackGres has developed a very innovative mechanism to load Postgres extensions on-demand, onto the (otherwise immutable!) container images. It is quite simple to use. Let's load the `ltree` extension into our cluster. Edit the cluster's YAML file, adding the following section inside postgres spec:

```yaml
    extensions:
      - name: 'ltree'
```

(note the indentation; it's 1 level, 2 spaces). After apply it and a few seconds, the extension would have been dynamically downloaded onto the container, and will now show in the Postgres console if we repeat the previous query.

You just need now to run `create extension ltree;` on those databases that you want the extension to be installed and will be ready to go!

To check the currently available extensions and versions, please check the Web Console. Please also note that installing some extensions may require a cluster restart. This restriction will be lifted in future versions.


## Let's run a benchmark!

StackGres also supports automating "Day 2 Operations". This is performed via a CRD called `SGDbOps`. `SGDbOps` support several kind of operations, with more added in future versions. Let's try one of them, that runs a benchmark. Create and apply the following YAML file:

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

Upon submitting this CR, StackGres will schedule and run a benchmark. The results of the benchmark will be written in the `.Status` field of the CRD, which you can query with `kubectl describe`. You may also check them from the Web Console.
