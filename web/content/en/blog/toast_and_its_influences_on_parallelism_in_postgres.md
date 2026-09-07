+++
title = "TOAST and its influences on parallelism in PostgreSQL"
date = 2024-02-26T11:00:00+01:00
author = ["asotolongo"]
tags = ["postgresql", "performance","parallel","toast"]
planet = "asotolongo"
draft = false
readtime = "6 min"
thumbnail = "img/blog/toast_parallel.jpg"
url = "/blog/toast_and_its_influences_-on_parallelism_in_postgres/"
description = "How TOASTed data influences parallel query in PostgreSQL: why parallelism may not kick in as expected and the parameters that govern it."
+++

## Introduction

Since the PostgreSQL 9.6 version was released, the feature related to query parallelism has appeared and has become a good option to improve query performance. Since then, the evolution of parallelism has been growing, resulting in better performance in the database. To manage the behavior of parallelism, there are some parameters that you can tune, for example:
 

* [max_parallel_workers_per_gather](https://postgresqlco.nf/doc/en/param/max_parallel_workers_per_gather/): The number of parallel workers to execute a query activity in parallel (default 2), these parallel workers are taken from the pool of processes defined by [max_worker_processes](https://postgresqlco.nf/doc/en/param/max_worker_processes/), limited by [max_parallel_workers](https://postgresqlco.nf/doc/en/param/max_parallel_workers/), worth mentioning there is one special worker named: the leader worker, who coordinates and gathers the result of the scan from each of the parallel workers. This leader worker can or can not participate in scanning and will depend on its load in the coordination activities. Also, it is possible to control the involvement of the leader worker in the scanning using the parameter [parallel_leader_participation](https://postgresqlco.nf/doc/en/param/parallel_leader_participation/).
* [min_parallel_table_scan_size](https://postgresqlco.nf/doc/en/param/min_parallel_table_scan_size/): The minimum amount of table data(size) for a parallel scan to be considered (default 8MB)
* [min_parallel_index_scan_size](https://postgresqlco.nf/doc/en/param/min_parallel_index_scan_size/): the minimum amount of index data(size) for a parallel scan to be considered (default 512kB)
* [parallel_setup_cost](https://postgresqlco.nf/doc/en/param/parallel_setup_cost/) : the cost of starting up worker processes for parallel query (default 1000)
* [parallel_tuple_cost](https://postgresqlco.nf/doc/en/param/parallel_tuple_cost/): the cost of passing each tuple (row) from worker to leader backend (default 0.1)
* [parallel_workers](https://www.postgresql.org/docs/current/sql-createtable.html#RELOPTION-PARALLEL-WORKERS): A storage parameter for tables, that allows change the behavior of number of workers to execute a query activity in parallel, similar to max_parallel_workers_per_gather, but only for a specific table;   `ALTER TABLE tabname SET (parallel_workers = N);`


According to the resources of your server and the workload of your database, you can tune these parameters to take advantage of parallelism. The [min_parallel_table_scan_size](https://postgresqlco.nf/doc/en/param/min_parallel_table_scan_size/) affects the decision of Postgres code about how many workers will be used in a table scan node in a query because the number of workers is computed as a geometric progression with 3 using the table size. You can check the code [here](https://github.com/postgres/postgres/blob/8a300fc3afce4a0fe8a4c55bc22b2aeec092cf23/src/backend/optimizer/path/allpaths.c#L4210).


* < 8MB (3^0 * 8) -> 0 workers 
* < 24MB (3^1 * 8) -> 1 workers
* < 72MB (3^2 * 8) -> 2 workers
* < 216MB (3^3 * 8) -> 3 workers
* …

For example, a table with a 50MB size will use 2 workers to scan the table, but,  what happens if a table has TOAST? Is the size of the TOAST taken into account for the worker number estimation? This point will be analyzed in this blog. 

## Tests and Results 




```sql
CREATE TABLE tab1 (i int, j text);
ALTER TABLE tab1 ALTER COLUMN j SET STORAGE EXTERNAL; -- to force to use store in toast without compression
INSERT INTO tab1 SELECT i, repeat('textvalue ',500) from generate_series (1,900000 ) AS i;
ANALYZE tab1;
postgres=# \dt+ tab1
                                   List of relations
 Schema | Name | Type  |  Owner   | Persistence | Access method |  Size   | Description 
--------+------+-------+----------+-------------+---------------+---------+-------------
 public | tab1 | table | postgres | permanent   | heap          | 5126 MB | 
(1 row)

```

Let's see how the planner generates the execution plan:

```sql
postgres=# EXPLAIN ANALYZE select i, j from tab1 where i%2=0;
                        QUERY PLAN                                                         
---------------------------------------------------------------------------------------------------------------------------
 Gather  (cost=1000.00..12808.00 rows=4500 width=22) (actual time=0.338..54.463 rows=450000 loops=1)
   Workers Planned: 2
   Workers Launched: 2
   ->  Parallel Seq Scan on tab1  (cost=0.00..11358.00 rows=1875 width=22) (actual time=0.038..23.329 rows=150000 loops=3)
         Filter: ((i % 2) = 0)
         Rows Removed by Filter: 150000
 Planning Time: 0.093 ms
 Execution Time: 65.269 ms
(8 rows)
```


The planner uses two parallel workers because `max_parallel_workers_per_gather =2`, value by default, but what will happen if we set `max_parallel_workers_per_gather =3`. Will more workers be used in parallel?

```sql
postgres=# set max_parallel_workers_per_gather = 3;
SET
postgres=# EXPLAIN ANALYZE select i, j from tab1 where i%2=0;
                    QUERY PLAN                                                         
---------------------------------------------------------------------------------------------------------------------------
 Gather  (cost=1000.00..12808.00 rows=4500 width=22) (actual time=0.363..59.643 rows=450000 loops=1)
   Workers Planned: 2
   Workers Launched: 2
   ->  Parallel Seq Scan on tab1  (cost=0.00..11358.00 rows=1875 width=22) (actual time=0.041..24.766 rows=150000 loops=3)
         Filter: ((i % 2) = 0)
         Rows Removed by Filter: 150000
 Planning Time: 0.070 ms
 Execution Time: 71.027 ms
(8 rows)
```
No, the planner only uses two parallel workers, but how is it possible if the table size is 5GB? 

We will use a more specific query to see the table size in detail: 

```sql
postgres=# SELECT
  format(
    $$
      Normal table %s:
        oid: %s
        size: %s 
      Toast table %s:
        oid: %s
        size: %s 
    $$,
    relname,
    oid,
    pg_size_pretty(pg_relation_size(oid)),
    c.reltoastrelid::regclass,
    c.reltoastrelid,
    pg_size_pretty(pg_relation_size(c.reltoastrelid))
  ) as  sizes
FROM pg_class as c
WHERE relname = 'tab1';

                    sizes                     
----------------------------------------------
                                             +
       Normal table tab1:                    +
         oid: 2717534                        +
         size: 45 MB                         +
       Toast table pg_toast.pg_toast_2717534:+
         oid: 2717537                        +
         size: 5022 MB                       +
     
(1 row)
```

The table has only `45MB` of size, and the rest belongs to the toast table (`5GB`), With the default value of `min_parallel_table_scan_size= 8 MB`, the calculation done by the planner will use two workers. This can indicate that the TOAST size is not considered for the planner's and workers' calculations. However, we can modify this parameter to force the planner to compute the different values of the parallel workers and see if this can improve performance. 

```sql
postgres=# set min_parallel_table_scan_size = '4MB';-- this will force to use 3 workers
SET 
postgres=# EXPLAIN ANALYZE select i, j from tab1 where i%2=0;
                    QUERY PLAN                                                         
---------------------------------------------------------------------------------------------------------------------------
 Gather  (cost=1000.00..11537.84 rows=4500 width=22) (actual time=0.186..38.858 rows=450000 loops=1)
   Workers Planned: 3
   Workers Launched: 3
   ->  Parallel Seq Scan on tab1  (cost=0.00..10087.84 rows=1452 width=22) (actual time=0.021..19.218 rows=112500 loops=4)
         Filter: ((i % 2) = 0)
         Rows Removed by Filter: 112500
 Planning Time: 0.035 ms
 Execution Time: 49.475 ms
(8 rows)
```
The cost of the plan decreases, as does the actual time (~ 25%), Of course, this behavior is according to our type of CPUs and disks and is not to be similar in all types of hardware; hence, it is recommended to perform a test like this in your environment. 

 It’s also possible to experiment with the storage parameter `parallel_workers`: 


 ```sql
postgres=# ALTER TABLE tab1 SET (parallel_workers = 4);
postgres=# EXPLAIN ANALYZE select i, j from tab1 where i%2=0;
            QUERY PLAN                                                        
-------------------------------------------------------------------------------------------------------------------------
 Gather  (cost=1000.00..10558.00 rows=4500 width=22) (actual time=0.363..30.522 rows=450000 loops=1)
   Workers Planned: 4
   Workers Launched: 4
   ->  Parallel Seq Scan on tab1  (cost=0.00..9108.00 rows=1125 width=22) (actual time=0.026..13.946 rows=90000 loops=5)
         Filter: ((i % 2) = 0)
         Rows Removed by Filter: 90000
 Planning Time: 0.104 ms
 Execution Time: 44.707 ms
(8 rows)
``` 

## Conclusions 

The result shown above means that toast size does not matter for the parallelism, and then you need to analyze if your table has a significant size in TOAST to modify the planner behavior to get better performance. Only the change of some configuration parameters (`max_parallel_workers_per_gather or min_parallel_table_scan_size`)  during your session or using the storage parameter `parallel_workers` can make a difference in some queries, but you must be aware that more workers can use more resources, and it can become something worse in some cases.  
