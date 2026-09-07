+++
title = "Boost your User-Defined Functions in PostgreSQL"
date = 2021-02-05T15:30:00+02:00
author = ["3manuek","asotolongo"]
tags = ["postgresql", "performance"]
planet = "asotolongo"
draft = false
readtime = "10 min"
description = "Improve the performance of user-defined functions in PostgreSQL by bringing computation to the data: languages, volatility and optimization tips."
+++



## Introduction

Using the RDBMS only to store data is restricting the full potential of the database systems, which were designed for server-side processing and provide other options besides being a data container. Some of these options are stored procedures and functions that allow the user to write server-side code, using the principle of bringing computation to data, avoiding large datasets round trips and taking advantage of server resources. PostgreSQL allows programming inside the database since the beginning, with User Defined Functions (UDFs). These functions can be written in several languages like SQL, PL/pgsql, PL/Python, PL/Perl, and others. But the most common are the first two mentioned: SQL and PL/pgsql. However, there may be "anti-patterns" in your code within functions and they can affect performance. This blog will show the reader some simple tips, examples and explanations about increasing performance in server-side processing with User Defined Functions in PostgreSQL. It is also important to clarify that the intention of this post isn’t to discuss whether Business Logic should be placed, but only how you can take advantage of the resources of the database server.


## Avoid these antipatterns in your PL/pgsql’s code

### 1:  Use PL/pgsql functions for simple SQL statements 
SQL functions, in certain conditions, can have their function bodies inlined into the main query directly. This can be a performance advantage because the function code can be analyzed by the planner, which can apply some optimizations. When can I apply this pattern? When you have a query or group of simple queries that do not require intermediate analysis/process before returning the result. On the other hand, writing these simple SQL sentences in PL/pgsql requires overhead for the PL/pgsql compiler.  Example: 

```sql
1- CREATE OR REPLACE FUNCTION hemisphere_sql (character varying)
    RETURNS character varying     LANGUAGE sql 
    AS $$
    SELECT
        CASE WHEN $1 IN ('UK', 'Germany', 'Japan', 'US', 'China', 'Canada', 'Russia', 'France') THEN
            'North'
        WHEN $1 IN ('South Africa', 'Australia', 'Chile') THEN
            'South'
        ELSE
            'unknown'
        END
$$;

2- CREATE OR REPLACE FUNCTION hemisphere_plpgsql (character varying)
    RETURNS character varying   LANGUAGE plpgsql
    AS $$
DECLARE
    result character varying;
BEGIN
    result:= (
        SELECT
            CASE WHEN $1 IN ('UK', 'Germany', 'Japan', 'US', 'China', 'Canada', 'Russia', 'France') THEN
                'North'
            WHEN $1 IN ('South Africa', 'Australia', 'Chile') THEN
                'South'
            ELSE
                'unknown'
            END);
    RETURN result;
END;
$$;

1- EXPLAIN (ANALYZE,VERBOSE ) SELECT hemisphere_sql(country) FROM customers;

Seq Scan on public.customers  (cost=0.00..963.00 rows=20000 width=32) (actual time=0.039..29.309 rows=20000 loops=1)
   Output: CASE WHEN ((country)::text = ANY ('{UK,Germany,Japan,US,China,Canada,Russia,France}'::text[])) THEN 'North'::text WHEN ((country)::text = ANY ('{"South Africa",Australia,Chile}'::text[])) THEN 'South'::text ELSE 'unknown'::text END
 Planning Time: 0.458 ms
 Execution Time: 32.306 ms

2-EXPLAIN (ANALYZE,VERBOSE ) SELECT hemisphere_plpgsql(country) FROM customers;

 Seq Scan on public.customers  (cost=0.00..5688.00 rows=20000 width=32) (actual time=0.654..174.685 rows=20000 loops=1)
   Output: hemisphere_plpgsql(country)
 Planning Time: 0.082 ms
 Execution Time: 175.972 ms 
```

As we can see in the outputs of the explains commands, on first **EXPLAIN**,  ***output***  tag has the SQL code that belongs to SQL function, which means that this code is inlined into the calling query, rather than call the function, as the second EXPLAIN, this can improve the performance of our queries.


### 2:  Unnecessary usage of SELECT INTO clause  

Inside the PL/pgsql function, it is a bit more costly to assign a value using  `SELECT INTO` than a simple assignment using `:=`. When can I apply this? When the `:=` operator can replace the INTO clause.  Example:

```sql
1- CREATE OR REPLACE FUNCTION simple ()  RETURNS void
    AS $$
DECLARE
    s int DEFAULT 0;
BEGIN
    FOR i IN 1..10000 LOOP
        s := s + 1;
    END LOOP;
END;
$$
LANGUAGE plpgsql;

2- CREATE OR REPLACE FUNCTION using_select ()  RETURNS void
    AS $$
DECLARE
    s int DEFAULT 0;
BEGIN
    FOR i IN 1..10000 LOOP
        SELECT s + 1 INTO s;
    END LOOP;
END;
$$
LANGUAGE plpgsql;

1-SELECT simple();
 Time: 16.980 ms
2-SELECT using_select();
Time: 86.931 ms
```

### 3:  Overusing RAISE clause

**RAISE** clause can be useful to debug and show some information about the code, but it carries or has an extra load within the functions. Only use it if necessary in the production environments. Example: 

```sql
1- CREATE OR REPLACE FUNCTION some_sum (val int, cnt int)
    RETURNS int
    AS $$
DECLARE
    i int;
    result int:= 0;
BEGIN
    FOR i IN 1.. $2 LOOP
        result:= result + $1;
    END LOOP;
    RAISE notice 'Final value of result: %', result;
    RETURN result;
END;
$$
LANGUAGE plpgsql;
2- CREATE OR REPLACE FUNCTION some_sum_raise (val int, cnt int)
    RETURNS int
    AS $$
DECLARE
    i int;
    result int:= 0;
BEGIN
    FOR i IN 1.. $2 LOOP
        result:= result + $1;
        RAISE notice 'Temporary value of result: %', result;
    END LOOP;
    RAISE notice 'Final value of result: %', result;
    RETURN result;
END;
$$
LANGUAGE plpgsql;

1- SELECT  some_sum(3,100);
NOTICE:  Final value of result: 300
Time: 1.843 ms

2- SELECT  some_sum_raise(3,100);
NOTICE:  Temporary value of result: 3
...
NOTICE:  Temporary value of result: 300
NOTICE:  Final value of result: 300
Time: 8.578 ms
```

### 4:  Overusing the high-level programming coding style for SQL activities

Even programmers who come from high-level programming are unaware of the benefits of SQL, and ADVANCED SQL a language that can speed up the performance considerably by avoiding unnecessary loops. For example iterating on a `FOR LOOP` and doing a select within can be replaced by a single query using the `LATERAL` clause, which essentially  is like a SQL for each loop

```sql
1- CREATE OR REPLACE  FUNCTION oldest_orders_by_customer (int) RETURNS SETOF t_oldest_orders_by_customer  AS $$
 DECLARE
    c customers;
    result record;
 BEGIN 
	 FOR c IN SELECT * FROM customers c2 WHERE age > $1    loop
	     SELECT c.firstname,o.orderid, o.orderdate , o.totalamount into result
              FROM orders o 
              WHERE o.customerid = c.customerid
              ORDER BY o.orderdate DESC
              LIMIT 1;
         IF result is not null THEN       
             RETURN NEXT result;   
         END IF;   
     END LOOP;
    RETURN;
 END;
 $$
 LANGUAGE plpgsql;


2- CREATE OR REPLACE  FUNCTION oldest_orders_by_customer_lateral (int) RETURNS SETOF t_oldest_orders_by_customer  AS  $$
 BEGIN 
	RETURN QUERY SELECT customer_sub.firstname  , o_sub.*
       FROM (SELECT * FROM customers c2 WHERE age > $1) customer_sub,
       LATERAL (SELECT o.orderid, o.orderdate , o.totalamount 
                FROM orders o 
                WHERE o.customerid = customer_sub.customerid
                ORDER BY o.orderdate DESC
                LIMIT 1) o_sub;
 END;
 $$
 LANGUAGE plpgsql;

 
1- SELECT * FROM oldest_orders_by_customer(80);
Time: 89.296 ms

2- SELECT * FROM oldest_orders_by_customer_lateral(80);
Time: 45.230 ms
```

## Using  functions properties
The definition of functions has  some properties that can help with function performance, for example:

### 1: Use PARALLEL SAFE whenever possible 
The planner cannot determine automatically if a function is parallel safe, but  [under certain conditions](https://www.postgresql.org/docs/current/parallel-safety.html#PARALLEL-LABELING) parallel mode can boost performance significantly if you process a large dataset. The number of workers that the planner will use is limited by the parameters [max_parallel_workers_per_gather](https://postgresqlco.nf/doc/en/param/max_worker_processes/), which in turn are taken from the pool of processes established by [max_worker_processes](https://postgresqlco.nf/doc/en/param/max_worker_processes/), limited by [max_parallel_workers](https://postgresqlco.nf/doc/en/param/max_parallel_workers/), the maximum number of concurrent queries to execute with parallelism  is determined by the following formula, as long as **max_worker_processes<= server cores**: 

**#Q_concurrent_par = max_worker_processes /max_parallel_workers_per_gather (integer division)**

When is it safe to use PARALLEL in a function? As long as your code does not perform the following, you should be ready to use it: 
* Writes to the database.
* Access sequences.
* Change the transaction state.
* Makes persistent changes to settings. 
* Access temporary tables. 
* Use cursors.
* Defines prepared statements

Example: 

```sql

1- CREATE OR REPLACE FUNCTION pair_div_4 (i int) RETURNS boolean
    AS $$
BEGIN
    IF $1%2 = 0 AND $1%4 = 0 THEN
        RETURN TRUE;
    END IF;
    RETURN FALSE;
END;
$$
LANGUAGE plpgsql;

2- CREATE OR REPLACE FUNCTION pair_div_4_ps (i int) RETURNS boolean
    AS $$
BEGIN
    IF $1%2 = 0 AND $1%4 = 0 THEN
        RETURN TRUE;
    END IF;
    RETURN FALSE;
END;
$$
LANGUAGE plpgsql
PARALLEL SAFE;


1- EXPLAIN ANALYZE  SELECT * from trade where pair_div_4 (id);

 Seq Scan on trade  (cost=0.00..448684.86 rows=563520 width=16) (actual time=0.323..2459.553 rows=422640 loops=1)
   Filter: pair_div_4(id)
   Rows Removed by Filter: 1267921
 Planning Time: 0.070 ms
 Execution Time: 2471.796 ms


2- explain analyze  select * from trade where pair_div_4_ps (id);

 Gather  (cost=1000.00..249635.11 rows=563520 width=16) (actual time=0.883..1386.856 rows=422640 loops=1)
   Workers Planned: 2
   Workers Launched: 2
   ->  Parallel Seq Scan on trade  (cost=0.00..192283.11 rows=234800 width=16) (actual time=0.868..1301.220 rows=140880 loops=3)
         Filter: pair_div_4_ps(id)
         Rows Removed by Filter: 422640
 Planning Time: 0.138 ms
 Execution Time: 1405.412 ms
```

As shown in the outputs of the explains, on the second **EXPLAIN**,  ***Workers Launched***  tag has value **2**, this means that this query used **2 workers** to execute, and the first **EXPLAIN** was executed without parallelisms     

### 2: Use IMMUTABLE when it is possible  

The **IMMUTABLE** option informs the query optimizer about the behavior of the function and can apply some optimization. Any call to the function with all-constant arguments can be immediately replaced with the function value. To mark a function as **IMMUTABLE** you need to comply with the following:

* You cannot modify the database (state) and always returns the same result for the same argument values;
* Do not search in the databases or use information that is not directly present in its argument list values.

Example:

```sql
1- CREATE OR REPLACE FUNCTION get_date (date) RETURNS int
    AS $$
DECLARE
    i int;
    result int:= 0;
BEGIN
   RETURN extract (day from $1);
END;
$$
LANGUAGE plpgsql;


2- CREATE OR REPLACE FUNCTION get_date_i (date) RETURNS int
    AS $$
DECLARE
    i int;
    result int:= 0;
BEGIN
   RETURN extract (day from $1);
END;
$$
LANGUAGE plpgsql IMMUTABLE;


1- SELECT 1  from trade where id=get_date(current_date);
 Time: 2557.521 ms (00:02.558)
2- SELECT 1  from trade where id=get_date_i(current_date);;
 Time: 2208.848 ms (00:02.209)
```

## Monitoring  performance of functions

PostgreSQL allows the user to track the  performance of functions in the database. For example, we can see the performance stats using the view [pg_stat_user_functions](https://www.postgresql.org/docs/current/monitoring-stats.html#MONITORING-PG-STAT-USER-FUNCTIONS-VIEW), as long as you configure the parameter named [track_functions](https://postgresqlco.nf/doc/en/param/track_functions/), that allows tracking function call counts and time spent. To simplify the configuration we can leverage the option that gives us [postgresqlcon.nf](https://postgresqlco.nf/) to share a configuration file,  [download it](https://postgresqlco.nf/manage/config/public/8eccd6e6-ecbf-458c-af62-4f9370bba353) and apply it to your server. Specifically, to track function performance, select the download format  **alter_system**,  apply the modification to your server,  and reload the configuration using ***select pg_reload_conf()***. This allows us to detect which functions are working as expected or are slow.

For example, to use this view you can write a query like this: 

```sql
select schemaname||'.'||funcname func_name, calls, total_time, round((total_time/calls)::numeric,2) as mean_time, self_time   from pg_catalog.pg_stat_user_functions;


         func_name         | calls | total_time | mean_time | self_time 
---------------------------+-------+------------+-----------+-----------
 public.f_plpgsql          |     2 |     93.908 |     46.95 |    93.908
 public.auditoria_clientes |  2684 |    593.705 |      0.22 |   593.705
 public.prc_clientes       |     2 |      1.447 |      0.72 |     0.387
 public.max_pro_min        |     3 |      1.589 |      0.53 |     1.589
 public.registro_ddl       |    17 |     39.217 |      2.31 |    39.217
 public.registro_ddl_drop  |     2 |    422.386 |    211.19 |   422.386

```

**calls:** Number of times this function has been called

**total_time:** Time(ms) spent in this function and all other functions called by it inside their code

**mean_time:** AVG Time(ms) spent in this function and all other functions called by it inside their code

**self_time:** Time(ms) spent in this function itself, without including other functions called by it

## Conclusions 
The tips and examples shown above have shown us that sometimes with minimum changes written in our code in PostgreSQL’s functions we can get some performance benefits. These tips are not exclusive, whenever possible these can be combined to achieve an improvement. e.g.: PARALLEL SAFE and avoid overusing the RAISE clause.  If you know any other tips or examples please feel free to share them with us. Also, we can monitor our function’s performance by issuing a  simple change in PostgreSQL’s configuration.
