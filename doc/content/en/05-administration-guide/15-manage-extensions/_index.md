---
title: Database Extensions
weight: 15
url: administration/extensions
---

Some extensions needs extra software to be installed and configured before they are ready to use. Aiming to fix that problem, it was developed a extra service in the StackGres operator that handles the installation and set up. To use that is necessary to update the SGCluster, adding the extensions that needs to be installed, like below:

```yaml
---
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-db-cluster
  # ...
spec:
  # ..
  postgresExtensions:
  - name: postgis
    publisher: com.ongres
    repository: https://extensions.stackgres.io/postgres/repository
    version: 3.0.1
  - name: timescaledb
    publisher: com.ongres
    repository: https://extensions.stackgres.io/postgres/repository
    version: 1.7.4
```


Once they are installed, is necessary to create the extension, like below:

```sql
postgres=# create extension postgis;
CREATE EXTENSION
```
> Check [here for more details]({{% relref "05-administration-guide/02-Connecting-to-the-cluster/03-postgres-util" %}}) about how to connect using kubectl.


### Update the configuration for the new extensions

Some extensions, such as `timescale` needs to update some configuration to work, as shown in the error below:

```sql
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

To fix that is necessary to find the configuration used in the `SGCluster`, update it with the new values, then reload the cluster.

#### Finding and editing the `PostgresConfig` of the cluster

Assuming that my cluster name is named `my-db-cluster`, execute the command below to find its current postgres configuration:

```bash
➜ kubectl get sgcluster/my-db-cluster -o jsonpath="{ .spec.configurations.sgPostgresConfig }"
postgres-12-generated-from-default-1622494739858
```

Once found the config, edit it, adding the extra configs:

```bash
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
> Please note that the `pg_stat_statements` and the `auto_explain` are automatically set up by the cluster creation, they are used for the monitoring and will break the node creation if removed.


#### Reloading and testing

Once updated the configuration is necessary to reload patroni to update the configuration, like below:

```bash
➜ kubectl exec pod/my-db-cluster-0 -c patroni -it -- patronictl reload my-db-cluster 
+ Cluster: my-db-cluster (6968565955771891961) ---------+----+-----------+
|      Member     |       Host       |  Role  |  State  | TL | Lag in MB |
+-----------------+------------------+--------+---------+----+-----------+
| my-db-cluster-0 | 10.244.0.66:7433 | Leader | running |  1 |           |
+-----------------+------------------+--------+---------+----+-----------+
Are you sure you want to reload members my-db-cluster-0? [y/N]: y
Reload request received for member my-db-cluster-0 and will be processed within 10 seconds
```

Connect in the `psql` and run the following commands:

```sql
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

Check the [Extensions page]({{% relref "01-introduction/08-Extensions" %}}) for the complete list of available extensions.