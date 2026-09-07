+++
title = "The fastest way to copy data between Postgres tables"
date = 2024-07-24T12:00:00+01:00
author = ["asotolongo"]
tags = ["postgresql", "tables","copy"]
planet = "asotolongo"
draft = false
readtime = "6 min"
thumbnail = "img/blog/pg_copy_table.png"
description = "Benchmarking the fastest way to copy data between Postgres tables: INSERT SELECT, CREATE TABLE AS, COPY and other cloning techniques compared."
+++

## Introduction

Data cloning from one table to another in relational databases is a commonly used process to copy data from an existing table to a new or pre-existing table definition within the same database. This process can be performed for various reasons, such as data backup, information replication, and testing, among other purposes. Postgres and other third-party tools offer several techniques to clone data from one table to another. 

The purpose of this blog is to try to find the fastest way to clone data in Postgres to be prepared for this type of request in the future. To do this, several examples and the results will be shown. 

For the sake of the example, the following table definitions will be used, To simplify the testing activity, the tables will not have indexes or triggers:

 ```sql
postgres=# CREATE TABLE origin_table (i bigint, d date, j int, t character varying (10));
CREATE TABLE
postgres=# INSERT INTO origin_table SELECT i,'2024-02-15', i/10, md5(i::text)::character varying(10) FROM generate_series(1,20000000) as i; 
INSERT 0 20000000
postgres=# ANALYZE origin_table ;
ANALYZE
postgres=# CREATE TABLE target_table (i bigint, d date, j int, t character varying (10));
CREATE TABLE

```


## Tests

### INSERT INTO SELECT (IIS)

One of the most common ways to clone data using almost standard SQL. An IIS statement is used to select data from the original table and then insert it into the target table.

```sql
postgres=# INSERT INTO target_table SELECT * FROM origin_table ;
INSERT 0 20000000
Duración: 12795,623 ms (00:12,796)
--query plan
postgres=# EXPLAIN (ANALYZE,VERBOSE) INSERT INTO target_table SELECT * FROM origin_table ;
                                                                QUERY PLAN                                                           
------------------------------------------------------------------------------------
 Insert on public.target_table  (cost=0.00..347059.24 rows=0 width=0) (actual time=12757.949..12767.355 rows=0 loops=1)
   ->  Seq Scan on public.origin_table  (cost=0.00..347059.00 rows=20000000 width=27) (actual time=0.026..2271.678 rows=20000000 loops=1)
         Output: origin_table.i, origin_table.d, origin_table.j, origin_table.t
 Planning Time: 2.000 ms
 Execution Time: 12779.611 ms
(5 filas)

```

As shown in the EXPLAIN command, IIS generates two type of nodes, INSERT and SEQSCAN in the query plan.

What about if the target table is [unlogged](https://www.postgresql.org/docs/current/sql-createtable.html#SQL-CREATETABLE-UNLOGGED)? which allows you to bypass the WAL and perform a quick write to the table, Although it has some disadvantages (no durability, no replication), it is worth testing to see if it can help speed up the cloning process.  Going to a logged table at the end of the cloning process will make this data durable and replicable, but, how long will this process take?


```sql
postgres=# TRUNCATE target_table ;
TRUNCATE TABLE
postgres=# ALTER TABLE target_table SET UNLOGGED ;
ALTER TABLE
postgres=# \timing
El despliegue de duración está activado.
postgres=# INSERT INTO target_table SELECT * FROM origin_table ;
INSERT 0 20000000
Duración: 6498,045 ms (00:06,498)
postgres=# ALTER TABLE target_table SET LOGGED ;
ALTER TABLE
Duración: 7723,691 ms (00:07,724)

```

It seems that changing from an UNLOGGED table to a LOGGED is expensive and does not represent an improvement in the data table cloning process. The whole process (INSERT into unlogged table + SET table logged) took 14.2 seconds. 

### CREATE TABLE AS (CTAS)

This command conforms to the SQL standard, creates a new table, and fills it with data from the query result in one command. CTAS is also one of the most common ways to clone data in all databases. Bear in mind that if you have constraints, indexes, or triggers, these components will not be copied.

```sql
postgres=# EXPLAIN (ANALYZE,VERBOSE) CREATE TABLE target_table AS SELECT * FROM origin_table ;
                                                             QUERY PLAN                                                             
------------------------------------------------------------------------------------
 Seq Scan on public.origin_table  (cost=0.00..347059.00 rows=20000000 width=27) (actual time=0.291..1773.890 rows=20000000 loops=1)
   Output: i, d, j, t
 Planning Time: 2.180 ms
 Execution Time: 11900.089 ms
(4 filas)

```

As shown in the EXPLAIN command, CTAS generates only one node (SEQSCAN) in the query plan, It seems there are some small optimizations in CTAS related to IIS.

Note: The SELECT INTO SQL command works just as well as CTAS, you can see the code in the [analyze.c](https://github.com/postgres/postgres/blob/REL_16_STABLE/src/backend/parser/analyze.c#L265-L272) file.

### COPY STDOUT/STDIN

The COPY command in Postgres is a powerful option to import and export data between files and tables within the database. It provides an efficient way to move large amounts of data in and out of the database, especially when dealing with files in CSV format. In addition, it has options named STDIN and STDOUT, when used correctly, you can use to clone data, Of course, the connection operation through psql and pipe operations can generate certain levels of latency. For example:

```sql
time psql  -d postgres -c "copy origin_table to stdout " | psql  -d postgres -c "copy target_table from stdin"
COPY 20000000
real	0m20,937s
user	0m1,906s
sys	    0m0,511s

```

### pg_bulkload

[pg_bulkload](https://github.com/ossc-db/pg_bulkload) is a tool designed to load a huge amount of data into a database, which includes a certain level of optimization to load data in Postgres, and can be used in one specific type to clone a table. In this case, using the type **FUNCTION** allows you to load a result set from a function. For example:

According to the documentation, **WRITER** option has different values, for example:

* **DIRECT**: Load data directly to the table. Bypass the shared buffers and skip WAL logging, but you need your recovery procedure. This is the default and original older version's mode.
* **BUFFERED**: Load data to tables via shared buffers. Use shared buffers, write WALs, and use the original PostgreSQL WAL recovery.

##### Extension and function to clone data:

```sql
postgres=# CREATE EXTENSION pg_bulkload ;
CREATE EXTENSION
postgres=# CREATE FUNCTION select_function() RETURNS SETOF origin_table
postgres-#     AS $$ SELECT * FROM origin_table $$
postgres-#     LANGUAGE SQL;
CREATE FUNCTION

```

##### Using DIRECT value for WRITER option

* Control file for pg_bulkload 
```
#cloning.ctl
TABLE = target_table
TYPE = FUNCTION
WRITER = DIRECT
INPUT = select_function()

```
* Executing pg_bulkload

```
time pg_bulkload cloning.ctl 
NOTICE: BULK LOAD START
NOTICE: BULK LOAD END
	0 Rows skipped.
	20000000 Rows successfully loaded.
	0 Rows not loaded due to parse errors.
	0 Rows not loaded due to duplicate errors.
	0 Rows replaced with new rows.
real	0m6.912s
user	0m0.004s
sys	0m0.006s

```

It is pretty fast for the pg_bulkload tool to load data using **DIRECT** value, In addition, it is a fact that the table is not loaded into the cache:

```sql
postgres=# SELECT n.nspname, c.relname, count(*) AS buffers
             FROM pg_buffercache b JOIN pg_class c
             ON b.relfilenode = pg_relation_filenode(c.oid) AND
                b.reldatabase IN (0, (SELECT oid FROM pg_database
                                      WHERE datname = current_database()))
             JOIN pg_namespace n ON n.oid = c.relnamespace
             GROUP BY n.nspname, c.relname ORDER BY 3 DESC  LIMIT 10;
  nspname   |             relname             | buffers 
------------+---------------------------------+---------
 public     | origin_table                    |      32
 pg_catalog | pg_attribute                    |      29
 pg_catalog | pg_proc                         |      20
 pg_catalog | pg_class                        |      13
 pg_catalog | pg_proc_proname_args_nsp_index  |       9
 pg_catalog | pg_proc_oid_index               |       9
 pg_catalog | pg_attribute_relid_attnum_index |       8
 pg_catalog | pg_amproc                       |       5
 pg_catalog | pg_operator_oprname_l_r_n_index |       5
 pg_catalog | pg_statistic                    |       5
(10 filas)

```

The data is not replicated, as stated in the [documentation](https://ossc-db.github.io/pg_bulkload/pg_bulkload.html#restrictions):

* PITR/Replication: 
  Because of bypassing WAL, archive recovery by PITR is not available. This does not mean that it can be done PITR without loaded table data. If you would like to use PITR, take a full backup of the database after loading via pg_bulkload. If you are using streaming replication, you need to re-create your standby based on the backup set which is taken after pg_bulkload.

```sql
--primary server
postgres=# select count(*) from target_table;
  Count
----------
 20000000
(1 fila)

--replica server
postgres=# select count(*) from target_table;
 count 
-------
   136
(1 fila)
```
##### Using BUFFERED value for WRITER option

* Control file for pg_bulkload 
```
#cloning_buffer.ctl
TABLE = target_table
TYPE = FUNCTION
WRITER = BUFFERED
INPUT = select_function()

```
* Executing pg_bulkload

```
time pg_bulkload clonning_buffer.ctl
NOTICE: BULK LOAD START
NOTICE: BULK LOAD END
	0 Rows skipped.
	20000000 Rows successfully loaded.
	0 Rows not loaded due to parse errors.
	0 Rows not loaded due to duplicate errors.
	0 Rows replaced with new rows.
real	0m12.901s
user	0m0.004s
sys	0m0.006s

```

As it is possible to see above, using the **BUFFERED** value in **WRITER** option, the times are similar to IIS and CTAS

### pg_filedump

[pg_filedump](https://wiki.postgresql.org/wiki/Pg_filedump) is a tool designed for inspecting or analyzing the contents of Postgres table data files directly. However, working directly with Postgres's data files is not typically recommended or supported, so use it with discretion. However, we can test if this tool can help with the data cloning task.

```
#installing pg_filedump
git clone git://git.postgresql.org/git/pg_filedump.git
cd pg_filedump
make

#getting the path of the origin table file 
-bash-4.2$ psql
psql (14.10)
postgres=# SELECT  current_setting('data_directory')||'/'||pg_relation_filepath(oid)as table_file FROM pg_class WHERE relname='origin_table';
               table_file             
----------------------------------------
/var/lib/pgsql/14/data/base/14486/36758
(1 row)

#cloning data, command two times, because the data file exceeds 1GB 
#https://www.postgresql.org/docs/current/storage-file-layout.html
-bash-4.2$ time ./pg_filedump -o -t  -D bigint,date,int,varchar /var/lib/pgsql/14/data/base/14486/36758 | grep "COPY:" | sed 's/COPY: //' | psql  -d postgres -c "copy target_table from stdin" 
COPY 17825792
real	0m27.446s
user	0m12.574s
sys	0m2.778s

-bash-4.2$ time ./pg_filedump -o -t  -D bigint,date,int,varchar /var/lib/pgsql/14/data/base/14486/36758.1 | grep "COPY:" | sed 's/COPY: //' | psql  -d postgres -c "copy target_table from stdin"
COPY 2174208
real	0m3.164s
user	0m1.445s
sys	0m0.325s

```
## Result and Conclusions 

![cloning_data_time](../img/data_cloning_results.png)

As we can see above, the **pg_bulkload** tool using the **DIRECT** option is the fastest way to clone a data table in this test. This tool has excellent optimization to perform this type of activity, but has some drawbacks,  In addition, the **IIS, CTAS**, and **pg_bulkload** using the **BUFFERED** work similarly, with a slightly better **CTAS**. Now we have different options for data cloning for a table in Postgres, and you can use the option most appropriate to your use case.  Please, if you know of any other ways/techniques to table data cloning, share them with us, using Twitter Tag us @ongresinc or @AnthonySotolong


## Bonus tips

If the tables to clone have indexes or triggers, it is important to analyze if it is possible to carry out some tuning in the target table definition. The following [blog entry](https://www.cybertec-postgresql.com/en/postgresql-bulk-loading-huge-amounts-of-data/) has good tips related to: Drop the indexes, triggers and optimizing column order. In addition, it would be good to analyze, if possible, whether to disable the autovacuum for the target table and enable it at the end of the process.

```sql
postgres=# ALTER TABLE target_table SET (autovacuum_enabled = off);
ALTER TABLE
--cloning data activity
postgres=# ALTER TABLE target_table RESET (autovacuum_enabled );
ALTER TABLE
```