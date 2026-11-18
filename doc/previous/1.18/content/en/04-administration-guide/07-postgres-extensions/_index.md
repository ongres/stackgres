---
title: Postgres Extensions
weight: 5
url: /administration/extensions
description: Details about how to set up and install Postgres extensions.
showToc: true
---

StackGres has developed an innovative mechanism to load Postgres extensions on-demand, onto the otherwise immutable container images.
Required extensions can be specified declaratively in the cluster definition.

Out of the box, a StackGres cluster comes stripped of all the Postgres core extensions.
Only some basic extensions are installed by default, like `plpgsql`.

## List Extensions

We can list the available extensions by connecting to the cluster and executing the following SQL:

```
postgres=# SELECT * FROM pg_available_extensions ORDER BY name LIMIT 10;
```

> **Note**: The list of available extensions depends on your PostgreSQL version and the extensions you have configured in your cluster spec.

## Simple Extension Installation

To install other extensions, StackGres provides a mechanism to make them available on the fly by declaring them in the `SGCluster`.
At first, we'll have a look at a simple example, with the `ltree` extension.

Edit your cluster's YAML file and add the following section inside the `postgres` spec:

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

Note that installing some extensions may require a cluster restart.
A more complex example is shown below.


## Complex Extension Installation

For a more complex example, we'll add the `timescaledb` and `postgis` extensions:

```yaml
---
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
  # [...]
spec:
  # [..]
  postgres:
    extensions:
    - name: postgis
    - name: timescaledb
```

After applying the YAML change to the cluster, the extensions will be downloaded and will become available:

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

As usual, to use an extension on a database use the `create extension` command like below:

```
postgres=# create extension postgis;
CREATE EXTENSION
```

Some extensions need extra files to be installed and configured before they can be used.
This varies depending on the extension and, in some cases, requires the cluster to be configured and restarted:

* Extensions that require adding an entry to [`shared_preload_libraries`](https://postgresqlco.nf/en/doc/param/shared_preload_libraries/) configuration parameter.
* Upgrading extensions that overwrite any file that is not the extension's control file or extension's script file.
* Removing extensions. Until the cluster is restarted, a removed extension will still be available.
* Installation of extensions that require extra mounts. After installation, the cluster will need to be restarted.

### Update the Configuration for the New Extensions

Some extensions, such as `timescaledb`, need to update some configuration to work, as shown in the error below:

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

To fix that, it is necessary to find the configuration used in the `SGCluster`, update it with the new values, then reload the cluster.

### Editing the `PostgresConfig`

Assuming that my cluster is named `cluster`, execute the command below to find its current postgres configuration:

```
$ kubectl get sgcluster/cluster -o jsonpath="{ .spec.configurations.sgPostgresConfig }"
postgres-12-generated-from-default-1622494739858
```

Once you find the config, edit it, adding the extra configs:

```
kubectl edit sgPgConfig/postgres-12-generated-from-default-1622494739858
```

Then add the extra param in the `shared_preload_libraries`, under the `postgresql.conf`:

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  name: postgres-12-generated-from-default-1622494739858
  # ...
spec:
  # ...
  postgresql.conf:
    # ...
    shared_preload_libraries: pg_stat_statements,auto_explain,timescaledb
```

> Please note that the `pg_stat_statements` and the `auto_explain` are automatically set up by the
>  cluster creation, they are used for the monitoring and will break the node creation if removed.


### Reloading and Testing

Once the configuration is updated, it is necessary to reload the cluster to apply the changes. To 
 do so, a `restart` `SGDbOps` can be created:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart-1622494739858
spec:
  sgCluster: cluster
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

## Available Extensions

Check the [Extensions page]({{% relref "01-introduction/08-Extensions" %}}) for the complete list of available extensions.
