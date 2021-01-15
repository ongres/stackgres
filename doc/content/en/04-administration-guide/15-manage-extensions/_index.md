---
title: Database Extensions
weight: 15
url: administration/extensions
---

All extensions can be created with a regular `CREATE EXTENSION` command. To create then, connect on the database through `psql` then run:

```sql
postgres=# CREATE EXTENSION pg_stat_statements;
CREATE EXTENSION
```
> Check [here for more details]({{% relref "04-administration-guide/02-Connecting-to-the-cluster/03-postgres-util" %}}) about how to connect using kubectl.

## Checking available extensions

Besides the [standard PostgreSQL extensions](https://www.postgresql.org/docs/current/contrib.html), StackGres ships with the following extensions:

* [PostGIS](https://github.com/postgis/postgis)
* [TimescaleDB](https://github.com/timescale/timescaledb)
* [pgbouncer_fdw](https://github.com/CrunchyData/pgbouncer_fdw)
* [pgsphere](https://github.com/akorotkov/pgsphere)
* [pg_repack](https://github.com/reorg/pg_repack)
* [pg_healpix](https://gitlab.com/ongresinc/pg_healpix)
* [pg_q3c](https://github.com/segasai/q3c)

Check the current [version on the releases]({{% relref "/01-introduction/06-versions#additional-extensions-included-on-stackgres" %}}) page.

To list all available extensions, use the view [`pg_avaiable_extensions`](https://www.postgresql.org/docs/current/view-pg-available-extensions.html), like below:

```sql
postgres=# select * from pg_available_extensions ;
        name        | default_version | installed_version |                               comment                                
--------------------+-----------------+-------------------+----------------------------------------------------------------------
 ltree_plpython3u   | 1.0             |                   | transform between ltree and plpython3u
 jsonb_plpythonu    | 1.0             |                   | transform between jsonb and plpythonu
 xml2               | 1.1             |                   | XPath querying and XSLT
 citext             | 1.6             |                   | data type for case-insensitive character strings
 dict_int           | 1.0             |                   | text search dictionary template for integers
 amcheck            | 1.2             |                   | functions for verifying relation integrity
 plpgsql            | 1.0             | 1.0               | PL/pgSQL procedural language
 hstore_plpython2u  | 1.0             |                   | transform between hstore and plpython2u
 pg_trgm            | 1.4             |                   | text similarity measurement and index searching based on trigrams
 moddatetime        | 1.0             |                   | functions for tracking last modification time
 tsm_system_time    | 1.0             |                   | TABLESAMPLE method which accepts time in milliseconds as a limit
 hstore_plpython3u  | 1.0             |                   | transform between hstore and plpython3u
 hstore             | 1.6             |                   | data type for storing sets of (key, value) pairs
 pageinspect        | 1.7             |                   | inspect the contents of database pages at a low level
 hstore_plpythonu   | 1.0             |                   | transform between hstore and plpythonu
 btree_gin          | 1.3             |                   | support for indexing common datatypes in GIN
 pg_buffercache     | 1.3             |                   | examine the shared buffer cache
 seg                | 1.3             |                   | data type for representing line segments or floating-point intervals
 file_fdw           | 1.0             |                   | foreign-data wrapper for flat file access
 ltree_plpythonu    | 1.0             |                   | transform between ltree and plpythonu
 pgcrypto           | 1.3             |                   | cryptographic functions
 earthdistance      | 1.1             |                   | calculate great-circle distances on the surface of the Earth
 lo                 | 1.1             |                   | Large Object maintenance
 pgstattuple        | 1.5             |                   | show tuple-level statistics
 postgres_fdw       | 1.0             |                   | foreign-data wrapper for remote PostgreSQL servers
 bloom              | 1.0             |                   | bloom access method - signature file based index
 jsonb_plpython3u   | 1.0             |                   | transform between jsonb and plpython3u
 pg_stat_statements | 1.7             | 1.7               | track execution statistics of all SQL statements executed
 autoinc            | 1.0             |                   | functions for autoincrementing fields
 fuzzystrmatch      | 1.1             |                   | determine similarities and distance between strings
 adminpack          | 2.0             |                   | administrative functions for PostgreSQL
 pg_visibility      | 1.2             |                   | examine the visibility map (VM) and page-level visibility info
 uuid-ossp          | 1.1             |                   | generate universally unique identifiers (UUIDs)
 jsonb_plpython2u   | 1.0             |                   | transform between jsonb and plpython2u
 dict_xsyn          | 1.0             |                   | text search dictionary template for extended synonym processing
 unaccent           | 1.1             |                   | text search dictionary that removes accents
 intarray           | 1.2             |                   | functions, operators, and index support for 1-D arrays of integers
 ltree              | 1.1             |                   | data type for hierarchical tree-like structures
 isn                | 1.2             |                   | data types for international product numbering standards
 tablefunc          | 1.0             |                   | functions that manipulate whole tables, including crosstab
 intagg             | 1.1             |                   | integer aggregator and enumerator (obsolete)
 dblink             | 1.2             |                   | connect to other PostgreSQL databases from within a database
 insert_username    | 1.0             |                   | functions for tracking who changed a table
 tsm_system_rows    | 1.0             |                   | TABLESAMPLE method which accepts number of rows as a limit
 cube               | 1.4             |                   | data type for multidimensional cubes
 pgrowlocks         | 1.2             |                   | show row-level locking information
 ltree_plpython2u   | 1.0             |                   | transform between ltree and plpython2u
 refint             | 1.0             |                   | functions for implementing referential integrity (obsolete)
 plpython3u         | 1.0             |                   | PL/Python3U untrusted procedural language
 tcn                | 1.0             |                   | Triggered change notifications
 btree_gist         | 1.5             |                   | support for indexing common datatypes in GiST
 pg_freespacemap    | 1.2             |                   | examine the free space map (FSM)
 pg_prewarm         | 1.2             |                   | prewarm relation data
(53 rows)
```
> 


### Checking installed extensions

Execute the metacommand `\dxi` to list the installed extensions:

```bash
postgres=# \dxi
                                     List of installed extensions
        Name        | Version |   Schema   |                        Description                        
--------------------+---------+------------+-----------------------------------------------------------
 pg_stat_statements | 1.7     | public     | track execution statistics of all SQL statements executed
 plpgsql     
```

### Custom `shared_preload_libraries` for extensions

Both `timescaledb` and `pg_stat_statements` need a custom configuration. To fix that is necessary to add a new configuration that contains the custom value on `shared_preload_libraries`, like the example below:

```bash
cat << 'EOF' | kubectl create -f -

apiVersion: stackgres.io/v1beta1
kind: SGPostgresConfig
metadata:
  name: custom-conf
spec:
  postgresVersion: "12"
  postgresql.conf:
    shared_preload_libraries: pg_stat_statements,timescaledb

---
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  name: extensions
spec:
  instances: 2
  postgresVersion: 'latest'
  pods:
    persistentVolume: 
      size: '1Gi'
  configurations:
    sgPostgresConfig: custom-conf
EOF
```

Once the config is done and the cluster is ready, you can create the extension without errors:

```sql
postgres=# CREATE EXTENSION timescaledb CASCADE;
WARNING:  
WELCOME TO
 _____ _                               _     ____________  
|_   _(_)                             | |    |  _  \ ___ \ 
  | |  _ _ __ ___   ___  ___  ___ __ _| | ___| | | | |_/ / 
  | | | |  _ ` _ \ / _ \/ __|/ __/ _` | |/ _ \ | | | ___ \ 
  | | | | | | | | |  __/\__ \ (_| (_| | |  __/ |/ /| |_/ /
  |_| |_|_| |_| |_|\___||___/\___\__,_|_|\___|___/ \____/
               Running version 1.7.1
For more information on TimescaleDB, please visit the following links:

 1. Getting started: https://docs.timescale.com/getting-started
 2. API reference documentation: https://docs.timescale.com/api
 3. How TimescaleDB is designed: https://docs.timescale.com/introduction/architecture

Note: TimescaleDB collects anonymous reports to better understand and assist our users.
For more information and how to disable, please see our docs https://docs.timescaledb.com/using-timescaledb/telemetry.

CREATE EXTENSION
```