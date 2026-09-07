+++
title = "Tracking the refresh Performance of Materialized Views with mv_stat in PostgreSQL"
date = 2021-05-24T13:30:00+02:00
author = ["3manuek","asotolongo"]
tags = ["postgresql", "performance","view","materialized","extension"]
planet = "asotolongo"
draft = false
readtime = "6 min"
thumbnail = "img/blog/mv_stat.jpg"
description = "Track the refresh performance of materialized views in PostgreSQL with the mv_stat extension: audit creation, modification and refresh timings."
+++



## The tracking issue of Materialized views

The addition of the [materialized views](https://www.postgresql.org/docs/current/rules-materializedviews.html) feature in Postgres (available since 9.3 version) represents a substantial benefit on read-queries performance if your application can tolerate stale data. It helps to reduce disk access and complex query computations by flattening a View’s result set into a physical table. 
Auditing the creation, modification, and refresh operations for the materialized views can be time-consuming for Data Ops. It is necessary to combine information from the logs with an adequate configuration of [log_stamement](https://postgresqlco.nf/doc/en/param/log_statement/), some catalogs views, and potentially matching with the information provided by the [pg_stat_statemets](https://www.postgresql.org/docs/current/pgstatstatements.html) extension or by using [Generic File Access Functions](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADMIN-GENFILE). The complexity of this task may enlarge if your system strongly relies on a considerable number of Materialized Views.
In order to facilitate this further, I’ve created a Postgres extension called  [mv_stats](https://gitlab.com/ongresinc/extensions/mv_stats), which allows the Data Ops to track and query this information in a more practical manner.


## Mv_stats Extension

The mv_stats extension tracks the most significant statistics from all materialized views in the database. Taking advantage of a specific type of trigger named Event Triggers (unlike common triggers, event triggers capture DDL events in a specific database) for tracking newly created MVs without user intervention. If you want more information about the supported commands of the Event Triggers, you can check the official [documentation](https://www.postgresql.org/docs/current/event-trigger-matrix.html).

The statistics gathered by the module are available through the mv_stats view. This view contains one row for each distinct materialized view in the database, declared with the following columns:

| Column                |      Type     |  Description |
|-----------------------|---------------|--------------|
| mv_name               |  text         |  Name of MV schema-qualified|
| create_mv             |  timestamp    |  Timestamp of MV creation (`CREATE MATERIALIZED VIEW`), `NULL` means that MV existed before the extension and was loaded when creating the extension |
| mod_mv                |  timestamp    |  Timestamp of MV Modification (`ALTER MATERIALIZED VIEW`)  |
| refresh_mv_last       |  timestamp    |  Timestamp of last time that MV was refreshed (`REFRESH MATERIALIZED VIEW`)|
| refresh_count         |  int          |  Number of times refreshed |
| refresh_mv_time_last  |  interval     |  Duration of last refresh time |
| refresh_mv_time_total |  interval     |  Total refresh time  |
| refresh_mv_time_min   |  interval     |  Min refresh time  |
| refresh_mv_time_max   |  interval     |  Max refresh time  |
| reset_last            |  timestamp    |  Timestamp of last  stats reset  |



The extension also provides the capability to reset this view statistics via mv_activity_reset_stats function, which needs to be executed within Database superuser privileges :

* mv_activity_reset_stats (mview, ...): Reset the statistics collected. The mview parameter default value is *, implying all existent MV, but can be defined as a specific MV passing the name/s of the views using the schema-qualified name.


###  Example of use

In this section it will show how to use the extension, first of all, create the extension, install please check the [documentation](https://gitlab.com/ongresinc/extensions/mv_stats#install):

In your database execute: 
```sql
CREATE EXTENSION mv_stats;
```

Previously created views will be automatically added to the stats on blank, and they’ll update on the next refresh.

```sql

test=# SELECT mv_name,create_mv,mod_mv,refresh_mv_last as refresh_last, refresh_count, refresh_mv_time_last as refresh_time_last , refresh_mv_time_total as refresh_time_total, refresh_mv_time_min as refresh_time_min,refresh_mv_time_max  as refresh_time_max, reset_last FROM mv_stats ;
      mv_name      |         create_mv          | mod_mv |       refresh_last        | refresh_count | refresh_time_last | refresh_time_total | refresh_time_min | refresh_time_max | reset_last 
-------------------+----------------------------+--------+---------------------------+---------------+-------------------+--------------------+------------------+------------------+-------
 public.mv1        |                            |        |                           |             0 |                   | 00:00:00           |                  |                  | 

 ```

Create and refresh  a MATERIALIZED and  can query the view mv_stats to see the statistics: 

```sql
test=# CREATE MATERIALIZED VIEW mv_example AS SELECT * FROM pg_stat_activity ;
test=# REFRESH MATERIALIZED VIEW mv_example ;
test=# SELECT mv_name,create_mv,mod_mv,refresh_mv_last as refresh_last, refresh_count, refresh_mv_time_last as refresh_time_last , refresh_mv_time_total as refresh_time_total, refresh_mv_time_min as refresh_time_min,refresh_mv_time_max  as refresh_time_max, reset_last FROM mv_stats ;
      mv_name      |         create_mv          | mod_mv |       refresh_last        | refresh_count | refresh_time_last | refresh_time_total | refresh_time_min | refresh_time_max | reset_last 
-------------------+----------------------------+--------+---------------------------+---------------+-------------------+--------------------+------------------+------------------+-------
public.mv1        |                            |        |                           |             0 |                   | 00:00:00           | 
public.mv_example | 2021-02-03 15:32:35.826251 |        | 2021-02-03 15:32:45.37572 |             1 | 00:00:00.45811    | 00:00:00.45811     | 00:00:00.45811   | 00:00:00.45811   | 
(2 rows)


```

Also, it is possible to reset the statistics collected using the function mv_activity_reset_stats

```sql
-- for specific MV
test=# SELECT * FROM mv_activity_reset_stats ('public.mv_example');
 mv_activity_reset_stats 
-------------------------
 public.mv_example
(1 row)


test=# SELECT mv_name,create_mv,mod_mv,refresh_mv_last as refresh_last, refresh_count, refresh_mv_time_last as refresh_time_last , refresh_mv_time_total as refresh_time_total, refresh_mv_time_min as refresh_time_min,refresh_mv_time_max  as refresh_time_max, reset_last FROM mv_stats ;
      mv_name      |         create_mv          | mod_mv | refresh_last | refresh_count | refresh_time_last | refresh_time_total | refresh_time_min | refresh_time_max |         reset_last         
-------------------+----------------------------+--------+--------------+---------------+-------------------+--------------------+------------------+------------------+-----------------
 public.mv1        |                            |        |              |             0 |                   | 00:00:00           |                  |                  | 
 public.mv_example | 2021-02-03 15:32:35.826251 |        |              |             0 |                   | 00:00:00           |                  |                  | 2021-02-03 15:38:37.540717
(2 rows)


-- for all views 
test=# SELECT * FROM mv_activity_reset_stats ();
 mv_activity_reset_stats 
-------------------------
 public.mv_example
 public.mv1
(2 rows)

```

On DaaS such as RDS, you may not be able to install this extension through standard method. Although it can be easily created by loading the [mv_stats--0.2.0.sql](https://gitlab.com/ongresinc/extensions/mv_stats/-/blob/master/mv_stats--0.2.0.sql) in your database and enjoy it. For uninstalling and removing all dependant objects, you can execute the follow functions:

```sql
test=# SELECT _mv_drop_objects();

```

## Conclusions 

Using the [mv_stats](https://gitlab.com/ongresinc/extensions/mv_stats) extension it is possible to provide an easy way to collect and query the statistics of materialized view related to the creation, modification, and the time to refresh, for auditing or performance analysis purposes. By using the extensibility capabilities of Postgres, it avoids the process of exhaustive log search and matching data from pg_stat_statements.
