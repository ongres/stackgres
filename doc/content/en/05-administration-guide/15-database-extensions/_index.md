---
title: Database Extensions
weight: 15
url: administration/extensions
description: Details about how to set up and install the database extensions.
showToc: true
---

A StackGres cluster comes stripped of all his core extensions. Only some basic extensions are installed
 by default, like `plpgsql` that is always installed and created in every database. To install other
 extensions StackGres provide a mechanism to make them available on the fly by simply declaring them in
 the `SGCluster` like in the following example:

```yaml
---
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-db-cluster
  # ...
spec:
  # ..
  postgres:
    extensions:
    - name: postgis
    - name: timescaledb
```

After adding an extension this way it will be downloaded and will become available:

```
# select * from pg_available_extensions;
        name        | default_version | installed_version |                               comment                               
--------------------+-----------------+-------------------+---------------------------------------------------------------------
 postgis            | 3.0.1           | 3.0.1             | PostGIS geometry, geography, and raster spatial types and functions
 timescaledb        | 2.2.0           | 2.2.0             | Enables scalable inserts and complex queries for time-series data
 pg_stat_statements | 1.6             |                   | track execution statistics of all SQL statements executed
 plpgsql            | 1.0             | 1.0               | PL/pgSQL procedural language
 dblink             | 1.2             | 1.2               | connect to other PostgreSQL databases from within a database
 plpython3u         | 1.0             | 1.0               | PL/Python3U untrusted procedural language
(5 rows)
```

As usual, to use an extension on a database use the [`CREATE EXTENSION`](https://www.postgresql.org/docs/current/sql-createextension.html)
 command like below:

```
postgres=# create extension postgis;
CREATE EXTENSION
```

> Check [here for more details]({{% relref "05-administration-guide/02-connecting-to-the-cluster/03-postgres-util" %}}) about how to connect using kubectl.

Some extensions needs extra files to be installed and configured before they can be used. This
 vary depending on the extension and, in some cases, requires the cluster to be configured and restarted:

* Extensions that requires to add an entry to [`shared_preload_libraries`](https://postgresqlco.nf/en/doc/param/shared_preload_libraries/) configuration parameter.
* Upgrading extensions that overwrite any file that is not the extension''s control file or extension''s script file.
* Removing extensions. Until the cluster is not restarted a removed extension will still be available.
* Install of extensions that require extra mount. After installed the cluster will require to be restarted.

### Update the configuration for the new extensions

Some extensions, such as `timescale` needs to update some configuration to work, as shown in the error
 below:

```
postgres=# create extension timescaledb;
FATAL:  extension "timescaledb" must be preloaded
HINT:  Please preload the timescaledb library via shared_preload_libraries.

This can be done by editing the config file at: /var/lib/postgresql/data/postgresql.conf
and adding 'timescaledb' to the list in the shared_preload_libraries config.
        # Modify postgresql.conf:
        shared_preload_libraries = 'timescaledb'

Another way to do this, if not preloading other libraries, is with the command:
        echo "shared_preload_libraries = 'timescaledb'" >> /var/lib/postgresql/data/postgresql.conf 

(Will require a database restart.)

If you REALLY know what you are doing and would like to load the library without preloading, you can disable this check with: 
        SET timescaledb.allow_install_without_preload = 'on';
server closed the connection unexpectedly
        This probably means the server terminated abnormally
        before or while processing the request.
The connection to the server was lost. Attempting reset: Succeeded.
```

To fix that is necessary to find the configuration used in the `SGCluster`, update it with the new
 values, then reload the cluster.

#### Finding and editing the `PostgresConfig` of the cluster

Assuming that my cluster name is named `my-db-cluster`, execute the command below to find its current
 postgres configuration:

```
$ kubectl get sgcluster/my-db-cluster -o jsonpath="{ .spec.configurations.sgPostgresConfig }"
postgres-12-generated-from-default-1622494739858
```

Once found the config, edit it, adding the extra configs:

```
kubectl edit sgPgConfig/postgres-12-generated-from-default-1622494739858
```

Then add the extra param in the `shared_preload_libraries`, under the `postgresql.conf`:

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  name: postgres-12-generated-from-default-1622494739858
  namespace: default
  # ...
spec:
  # ...
  postgresql.conf:
    # ...
    shared_preload_libraries: pg_stat_statements,auto_explain,timescaledb
```

> Please note that the `pg_stat_statements` and the `auto_explain` are automatically set up by the
>  cluster creation, they are used for the monitoring and will break the node creation if removed.


#### Reloading and testing

Once updated the configuration is necessary to reload the cluster to update the configuration. To 
 do so, a `restart` `SGDbOps` can be created:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-1622494739858
  namespace: default
spec:
  sgCluster: my-db-cluster
  op: restart
```

You can wait for the completion of the task using `kubectl wait` command:

```
$ kubectl wait sgdbops/restart-1622494739858 --for=condition=Completed 
sgdbops.stackgres.io/restart-1622494739858 condition met
```

Connect in the `psql` and run the following commands:

```
postgres=# show shared_preload_libraries ;
          shared_preload_libraries           
---------------------------------------------
 pg_stat_statements,auto_explain,timescaledb
(1 row)

postgres=# create extension timescaledb;
WARNING:  
WELCOME TO
 _____ _                               _     ____________  
|_   _(_)                             | |    |  _  \ ___ \ 
  | |  _ _ __ ___   ___  ___  ___ __ _| | ___| | | | |_/ / 
  | | | |  _ ` _ \ / _ \/ __|/ __/ _` | |/ _ \ | | | ___ \ 
  | | | | | | | | |  __/\__ \ (_| (_| | |  __/ |/ /| |_/ /
  |_| |_|_| |_| |_|\___||___/\___\__,_|_|\___|___/ \____/
               Running version 1.7.4
For more information on TimescaleDB, please visit the following links:

 1. Getting started: https://docs.timescale.com/getting-started
 2. API reference documentation: https://docs.timescale.com/api
 3. How TimescaleDB is designed: https://docs.timescale.com/introduction/architecture

Note: TimescaleDB collects anonymous reports to better understand and assist our users.
For more information and how to disable, please see our docs https://docs.timescaledb.com/using-timescaledb/telemetry.

CREATE EXTENSION
```

## Checking available extensions

Check the [Extensions page]({{% relref "01-introduction/08-Extensions" %}}) for the complete list of
 available extensions.
