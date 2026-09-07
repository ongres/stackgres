+++
title = "Solving PostgreSQL's planner issues due to statistics missing chunks"
date = 2022-05-19T14:30:00+02:00
author = ["asotolongo"]
tags = ["postgresql", "corruption", "statistic"]
planet = "asotolongo"
draft = false
readtime = "5 min"
thumbnail = "img/blog/corruption-statistic.jpg"
description = "Diagnosing and fixing PostgreSQL planner errors caused by missing TOAST chunks in pg_statistic, with a step-by-step recovery procedure."
+++


## A statistic corruption on TOAST chunk case

## Introduction

PostgreSQL stores data on a page that usually has 8KB of size, this means that when there isn't  enough  space to store the data (for example, text or varchar data type), PostgreSQL uses the [TOAST](https://www.postgresql.org/docs/current/storage-toast.html) technique which allows us to store this extra data like chunks using other tables, each chunk is stored as a separate row in the TOAST table belonging to the main table. The users don't need to know if the information is stored on TOAST. PostgreSQL transfers/obtains the data to/from there transparently. Of course not all tables from our database have TOAST, to know what tables have TOAST you can use the following query:
  
```sql
    SELECT
    t1.oid,t1.relname,	t1.relkind,	t1.reltoastrelid, t1.reltoastrelid::regclass
    FROM 	pg_class t1
    INNER JOIN pg_class t2
    On t1.reltoastrelid = t2.oid
    WHERE	t1.relkind = 'r' AND t2.relkind = 't';
```
 

The data corruption in PostgreSQL is not very frequent and generally appears after some failure such as disk failures,  an unexpected power loss, hardware failure, database systems configured using [fsync](https://postgresqlco.nf/doc/en/param/fsync/)=off, or to a lesser extent PostgreSQL bugs. Some months ago was found the following error: 


```
    ERROR: missing chunk number 0 for toast value XXXXXXX in pg_toast_2619
```


This error message is an example that in our database there is data corruption in this TOAST table  pg_toast_2619, this table belongs to the statistics table ([pg_statistic](https://www.postgresql.org/docs/current/catalog-pg-statistic.html), you can check it in the [code source](https://github.com/postgres/postgres/blob/master/src/include/catalog/pg_statistic.h#L29)), and it is used by the planner to take decisions and choose the better plan to execute a query, which can impact directly the database performance, or simply can not run queries due to this error,  in addition, this table is populated and updated by the AUTOVACUUM process or manual [ANALYZE](https://www.postgresql.org/docs/current/sql-analyze.html) command,  and then, How to solve this data corruption to continue to work well in our database? We will see this in the next section.



## Solution

As the information stored in this table is practically temporal and is computed and stored each time there is an [ANALYZE](https://www.postgresql.org/docs/current/sql-analyze.html) or an [AUTO-ANALYZE](https://www.postgresql.org/docs/current/routine-vacuuming.html#VACUUM-FOR-STATISTICS) happens by AUTOVACUUM, it is possible to dispense with their data, because you can regenerate again, so we use the following steps to try to solve this issue. Take into account that the access to the table _**pg_statistic**_ is restricted to superusers

```sql
    BEGIN;
    DELETE FROM pg_statistic; --delete data from pg_statistic
    ANALYZE; --populate the statistics table again
    END;
```

This activity may take a while depending on the dimensions of the database, but if we have identified the rows that have data corruption and to which table they belong to, it is possible to delete only the statistics corresponding to damaged tables and regenerating them and thus we can reduce the recreation time of the new statistics. This can be detected because it only returns the above error with one or a few tables.


```sql
    BEGIN;
    DELETE FROM pg_statistic where ((starelid::oid)::regclass)::text ='your_table'; --delete data from pg_statistic for specific table
    ANALYZE 'your_table'; --populate the statistics table for specific table again
    END;
```


## Conclusions 

As you can see, it is relatively easy to solve this statistics table data corruption problem, and now the planner can work well and make a good decision about execution strategies, also this allows to AUTOVACUUM make his work fine. All temporal or reconstructible data that become corrupt and affect the PostgreSQL planner or executor can be fixed this way, trying to rebuild the object, for example, [pg_statistic_ext_data](https://www.postgresql.org/docs/current/catalog-pg-statistic-ext-data.html) table or a corrupt index can be [REINDEX](https://www.postgresql.org/docs/current/sql-reindex.html), but more than this solution, you will need to check your hardware and analyze its health or verify your configuration about [fsync](https://postgresqlco.nf/doc/en/param/fsync/), to avoid data corruption. 

## Low-level data check tools

PostgreSQL has other options to discover and analyze some data corruption such as: [amcheck](https://www.postgresql.org/docs/current/amcheck.html), [pageinspect](https://www.postgresql.org/docs/current/pageinspect.html) and [pg_visibility](https://www.postgresql.org/docs/current/pgvisibility.html), in the majority of cases, you will need professional support to try to use it and repair it, But remember the data corruption is not frequent in PostgreSQL ;-) 
