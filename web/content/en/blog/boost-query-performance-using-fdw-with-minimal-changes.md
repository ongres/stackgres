+++
title = "Boost query performance using Foreign Data Wrapper with minimal changes"
date = 2021-09-29T13:30:00+02:00
author = ["3manuek","asotolongo","gerardo"]
tags = ["postgresql", "FDW","performance"]
planet = "asotolongo"
draft = false
readtime = "10 min"
thumbnail = "img/blog/fdw_queries.jpg"
description = "Boost query performance on external Postgres data with postgres_fdw: how Foreign Data Wrappers outperform dblink with minimal application changes."
+++




## Foreign Data Wrapper

Nowadays  it is becoming more frequent for systems and applications to require querying data from outside the main database. PostgreSQL supports querying external postgres data using two core extensions [dblink](https://www.postgresql.org/docs/current/dblink.html) and  [postgres-fdw](https://www.postgresql.org/docs/current/postgres-fdw.html) , the last one is a Foreign Data Wrapper (FDW), that is an implementation of [SQL/MED standard](https://en.wikipedia.org/wiki/SQL/MED), which is part of ANSI SQL 2003 standard specification. FDW is widely recommended to be used in PostgreSQL for this activity instead of dblink,   because it provides standards-compliant syntax for accessing remote tables/data, and can give us better performance in some cases. 

Executing queries that need external data can sometimes be slow but PostgreSQL’s planner can apply some optimizations for this, such as: running some activity in the remote server to try to reduce the data transferred from there or if it is possible execute remote JOIN operations to take advantage of remote server resources. In addition, it is possible to make adjustments to help the planner choose better decisions and help the executor take advantage of this. This blog will show the reader some simple tips, examples, and explanations about how to increase performance of queries using FDW in PostgreSQL.



## Improving from the definition

Normally when working with FDW it is necessary to define three types of objects, such as: a **SERVER** to establish the connections to the remote host, the **USER** that defines what user will access  the external host and finally the **FOREIGN TABLE** to map the tables in the foreign server. All of  these objects mentioned previously have some options that can help to improve the performance of queries.


### 1: Fetch_size

The FDW retrieves data from the remote server using row bunches and the size of these bunches is defined by the fetch_size option (default 100). This option can be specified for a **SERVER or FOREIGN TABLE**, and the values defined for the last one have priority.


**Example :**

```sql
CREATE SERVER pgserver FOREIGN DATA WRAPPER postgres_fdw  OPTIONS(host 'localhost', port '5432',dbname 'db2');
  CREATE USER MAPPING FOR current_user  SERVER pgserver   OPTIONS (user 'postgres',password 'my_pass');
CREATE FOREIGN TABLE f_tables.customers (
	customerid int4  ,
	firstname varchar(50) ,
	lastname varchar(50) ,
	username varchar(50) ,
	age int2 ,
	gender varchar(1)
) SERVER pgserver OPTIONS (schema_name 'public', table_name 'customers');
```


And if executes **EXPLAIN** command to see what is happening with the query is possible see how the executor implements **Remote SQL**: 

```sql
EXPLAIN (analyze,verbose) select o.*,ftc.firstname from orders o join f_tables.customers as ftc using (customerid)

Hash Join  (cost=470.00..509.92 rows=748 width=148) (actual time=5.960..55.366 rows=12000 loops=1)
  Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount, ftc.firstname
  Hash Cond: (ftc.customerid = o.customerid)
  ->  Foreign Scan on f_tables.customers ftc  (cost=100.00..126.83 rows=561 width=122) (actual time=0.528..41.536 rows=20000 loops=1)
        Output: ftc.customerid, ftc.firstname, …
        Remote SQL: SELECT customerid, firstname FROM public.customers
  ->  Hash  (cost=220.00..220.00 rows=12000 width=30) (actual time=5.408..5.409 rows=12000 loops=1)
        Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
        Buckets: 16384  Batches: 1  Memory Usage: 889kB
        ->  Seq Scan on public.orders o  (cost=0.00..220.00 rows=12000 width=30) (actual time=0.014..2.167 rows=12000 loops=1)
              Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
Planning Time: 0.197 ms
Execution Time: 56.162 ms
```
With logging properly configured (by *[log_statement](https://postgresqlco.nf/doc/en/param/log_statement/) = ‘all’*) it will be possible to see what is happening behind the scenes:


```sql
0- [18580] postgres@db1 LOG:  execute <unnamed>: explain (analyze,verbose) select o.*,ftc.firstname from orders o join f_tables.customers as ftc using (customerid)
1- [22696] postgres@db2 LOG:  statement: START TRANSACTION ISOLATION LEVEL REPEATABLE READ
2- [22696] postgres@db2 LOG:  execute <unnamed>: DECLARE c1 CURSOR FOR
	SELECT customerid, firstname FROM public.customers
3- [22696] postgres@db2 LOG:  statement: FETCH 100 FROM c1
[22696] postgres@db2 LOG:  statement: FETCH 100 FROM c1
… until 20000 rows
4- [22696] postgres@db2 LOG:  statement: CLOSE c1
5- [22696] postgres@db2 LOG:  statement: COMMIT TRANSACTION
```

What has happened?
1. A transaction began with **ISOLATION LEVEL REPEATABLE READ**, in a connections to db2 database-
2. A cursor was defined with the query to try to fetch the data.
3. **The data was fetched using 100 row bunches**.
4. The cursor was closed.
5. The transaction was committed.

Therefore a possible optimization would be to increase the values of **fetch_size** to try to reduce the number of fetch activity.

```sql
ALTER FOREIGN TABLE  f_tables.customers OPTIONS ( fetch_size '10000' );

---the new plan
Hash Join  (cost=470.00..509.92 rows=748 width=148) (actual time=11.943..33.022 rows=12000 loops=1)
  Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount, ftc.firstname
  Hash Cond: (ftc.customerid = o.customerid)
  ->  Foreign Scan on f_tables.customers ftc  (cost=100.00..126.83 rows=561 width=122) (actual time=9.173..22.924 rows=20000 loops=1)
        Output: ftc.customerid, ftc.firstname…
        Remote SQL: SELECT customerid, firstname FROM public.customers
  ->  Hash  (cost=220.00..220.00 rows=12000 width=30) (actual time=2.752..2.752 rows=12000 loops=1)
        Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
        Buckets: 16384  Batches: 1  Memory Usage: 889kB
        ->  Seq Scan on public.orders o  (cost=0.00..220.00 rows=12000 width=30) (actual time=0.012..1.180 rows=12000 loops=1)
              Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
Planning Time: 0.131 ms
Execution Time: 33.794 ms
```
And the logs:

```sql
[18580] postgres@db1 LOG:  execute <unnamed>: explain (analyze,verbose) select o.*,ftc.firstname from orders o join f_tables.customers as ftc using (customerid)
[22696] postgres@db2 LOG:  statement: START TRANSACTION ISOLATION LEVEL REPEATABLE READ
[22696] postgres@db2 LOG:  execute <unnamed>: DECLARE c1 CURSOR FOR 	SELECT customerid, firstname FROM public.customers
[22696] postgres@db2 LOG:  statement: FETCH 10000 FROM c1
...until 20000
[22696] postgres@db2 LOG:  statement: CLOSE c1
[22696] postgres@db2 LOG:  statement: COMMIT TRANSACTION

```
Obviously, the transaction will require fewer **Fetch** operations to get all data, this will translate in less time executing the query, as the **EXPLAIN** output shows us. So if is necessary to process a large number of rows from FDW please take this option  into consideration.

### 2: Extensions

The PostgreSQL planner can decide if any **WHERE** operations could be executed on a remote host as long as it is safe to do so, in this case only **WHERE** clauses using built-in functions are considered safe, but the extensions option can help us to define our own  functions and operators as safe (packaged in extension form) and this will indicate to the PostgreSQL’s planner that it is safe to execute **WHERE** operations on the remote host and this will translate into a better performance. For this, it is necessary to package the functions in an extension way and set them as **IMMUTABLE**. This option can only be specified for **SERVER** objects.


For example: 

```sql
  EXPLAIN (analyze,verbose) select o.*,ftc.firstname from orders o join f_tables.customers as ftc using (customerid)  where super_age(ftc.age)

    Hash Join  (cost=470.00..642.84 rows=249 width=148) (actual time=18.702..59.381 rows=525 loops=1)
  Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount, ftc.firstname
  Hash Cond: (ftc.customerid = o.customerid)
  ->  Foreign Scan on f_tables.customers ftc  (cost=100.00..268.48 rows=187 width=122) (actual time=14.233..54.635 rows=842 loops=1)
        Output: ftc.customerid, ftc.firstname, ftc.lastname…
        Filter: super_age((ftc.age)::integer)
        Rows Removed by Filter: 19158
        Remote SQL: SELECT customerid, firstname, age FROM public.customers
  ->  Hash  (cost=220.00..220.00 rows=12000 width=30) (actual time=4.282..4.282 rows=12000 loops=1)
        Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
        Buckets: 16384  Batches: 1  Memory Usage: 889kB
        ->  Seq Scan on public.orders o  (cost=0.00..220.00 rows=12000 width=30) (actual time=0.022..1.659 rows=12000 loops=1)
              Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
Planning Time: 0.197 ms
Execution Time: 60.013 ms

```


In this case, if the function **super_age** is packaged as an [extension](https://github.com/asotolongo/fdw_functions) and sets the name to the extension option the result changes

```sql
CREATE EXTENSION fdw_functions; -- the extension must be loaded in both server (origin and remote)

ALTER SERVER pgserver  OPTIONS (extensions 'fdw_functions');
    Hash Join  (cost=470.00..635.36 rows=249 width=148) (actual time=39.761..39.985 rows=525 loops=1)
  Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount, ftc.firstname
  Hash Cond: (ftc.customerid = o.customerid)
  ->  Foreign Scan on f_tables.customers ftc  (cost=100.00..261.00 rows=187 width=122) (actual time=37.855..37.891 rows=842 loops=1)
        Output: ftc.customerid, ftc.firstname, ftc.lastname…
        Remote SQL: SELECT customerid, firstname FROM public.customers WHERE (public.super_age(age))
  ->  Hash  (cost=220.00..220.00 rows=12000 width=30) (actual time=1.891..1.892 rows=12000 loops=1)
        Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
        Buckets: 16384  Batches: 1  Memory Usage: 889kB
        ->  Seq Scan on public.orders o  (cost=0.00..220.00 rows=12000 width=30) (actual time=0.009..0.842 rows=12000 loops=1)
              Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
Planning Time: 0.162 ms
Execution Time: 40.364 ms

```

Therefore if it is necessary to filter some data in the **WHERE** clause using its own function, bear in mind this option provided by FDW.

### 3: Use_remote_estimate

PostgreSQL’s planner decides which is the better strategy to execute the queries, this decision is based on the table’s statistics, so, if the statistics are outdated the planner will choose a bad execution strategy. Autovacuum process will help to keep statistics updated, but does not execute **ANALYZE** commands on foreign tables, hence to keep statistics updated on  foreign tables it is necessary to run manually **ANALYZE**  on those tables. To avoid outdated statistics, the FDW has an option to get the statistics from the remote server on the fly, this option is precisely  **use_remote_estimate** and can be specified for a foreign table or a foreign server.


Example :

```sql
EXPLAIN (analyze,verbose) select o.*,ftc.firstname from orders o join f_tables.customers as ftc using (customerid)    where gender ='F'
   
Hash Join  (cost=470.00..1212.33 rows=133 width=148) (actual time=38.572..43.333 rows=5947 loops=1)
  Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount, ftc.firstname
  Hash Cond: (ftc.customerid = o.customerid)
  ->  Foreign Scan on f_tables.customers ftc  (cost=100.00..840.00 rows=100 width=122) (actual time=34.704..35.651 rows=10010 loops=1)
        Output: ftc.customerid, ftc.firstname, ftc.lastname…
        Remote SQL: SELECT customerid, firstname FROM public.customers WHERE ((gender = 'F'::text))
  ->  Hash  (cost=220.00..220.00 rows=12000 width=30) (actual time=3.846..3.847 rows=12000 loops=1)
        Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
        Buckets: 16384  Batches: 1  Memory Usage: 889kB
        ->  Seq Scan on public.orders o  (cost=0.00..220.00 rows=12000 width=30) (actual time=0.017..1.606 rows=12000 loops=1)
              Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
Planning Time: 0.440 ms
Execution Time: 44.203 ms
```

Defining **use_remote_estimate** to on the plan will change: 

```SQL
 ALTER FOREIGN TABLE  f_tables.customers  OPTIONS ( use_remote_estimate 'on' );

Hash Join  (cost=1163.33..1488.38 rows=6006 width=37) (actual time=13.584..17.607 rows=5947 loops=1)
  Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount, ftc.firstname
  Hash Cond: (o.customerid = ftc.customerid)
  ->  Seq Scan on public.orders o  (cost=0.00..220.00 rows=12000 width=30) (actual time=0.034..1.070 rows=12000 loops=1)
        Output: o.orderid, o.orderdate, o.customerid, o.netamount, o.tax, o.totalamount
  ->  Hash  (cost=1038.20..1038.20 rows=10010 width=11) (actual time=13.507..13.508 rows=10010 loops=1)
        Output: ftc.firstname, ftc.customerid
        Buckets: 16384  Batches: 1  Memory Usage: 559kB
        ->  Foreign Scan on f_tables.customers ftc  (cost=100.00..1038.20 rows=10010 width=11) (actual time=11.719..12.516 rows=10010 loops=1)
              Output: ftc.firstname, ftc.customerid
              Remote SQL: SELECT customerid, firstname FROM public.customers WHERE ((gender = 'F'::text))
Planning Time: 1.173 ms
Execution Time: 17.974 ms
```
And the logs:

```sql
[18580] postgres@db1 LOG:  execute <unnamed>: explain (analyze,verbose) select o.*,ftc.firstname from orders o join f_tables.customers as ftc using (customerid)   where gender ='F'
[22696] postgres@db2 LOG:  statement: START TRANSACTION ISOLATION LEVEL REPEATABLE READ
[22696] postgres@db2 LOG:  statement: EXPLAIN SELECT customerid, firstname FROM public.customers WHERE ((gender = 'F'::text))
[22696] postgres@db2 LOG:  statement: EXPLAIN SELECT customerid, firstname FROM public.customers WHERE ((gender = 'F'::text)) ORDER BY customerid ASC NULLS LAST
[22696] postgres@db2 LOG:  statement: EXPLAIN SELECT customerid, firstname FROM public.customers WHERE ((((SELECT null::integer)::integer) = customerid)) AND ((gender = 'F'::text))
[22696] postgres@db2 LOG:  execute <unnamed>: DECLARE c1 CURSOR FOR 	SELECT customerid, firstname FROM public.customers WHERE ((gender = 'F'::text))
[22696] postgres@db2 LOG:  statement: FETCH 10000 FROM c1
[22696] postgres@db2 LOG:  statement: FETCH 10000 FROM c1
[22696] postgres@db2 LOG:  statement: CLOSE c1
[22696] postgres@db2 LOG:  statement: COMMIT TRANSACTION
```

As shown in the logs, many **EXPLAIN** commands are thrown to the remote server and  obviously the planning time will increase, but the planner gets better statistics to decide which is the better execution plan,  which translates into better execution times.

## Pool

Another improvement could be using a pooling solution, like pgbouncer, with static connections for FDWs. This would reduce the overhead of establishing new connections each time is required. Benchmarks about the benefits of using pgBouncer can be found: [here](https://www.commandprompt.com/blog/connection-initiation-overhead-pgbouncer/) and [there](https://www.commandprompt.com/blog/connection-initiation-overhead-pgbouncer/). If you want to test FDW with pgbouncer  by yourself you can use [this labs](https://gitlab.com/ongresinc/labs/fwdpool) or you can check the results of this test [here](https://gitlab.com/ongresinc/labs/fwdpool/-/blob/master/run.log), where it is possible to see the positive impact of using a pgbouncer for pooling the FDW connections .

## Conclusions 

The tips and examples shown above have shown us that sometimes with minimum changes if we are using  FDW  in our queries in PostgreSQL we can get some reasonable  performance benefits. Of course you must analyze if each tip fixes your data scenery. In new PostgreSQL's releases, it is possible that new options will appear to boost our queries with FDW. If you know any other tips about FDW performance,  please feel free to share them with us. 
