+++
title = "Debugging Postgres WAL events with pg_walinspect"
date = 2022-12-09T14:00:00+01:00
author = ["asotolongo"]
tags = ["postgresql", "wal", "extension"]
planet = "asotolongo"
draft = false
readtime = "6 min"
thumbnail = "img/blog/pgwalinspect-v1.jpg"
description = "Debug Postgres write-ahead-log activity from SQL with pg_walinspect, the extension that displays WAL records in human-readable form."
+++



## An introduction to pg_walinspect 

The Write Ahead Log (“WAL” in short) is a core component of Postgres which stores all the transactions and operations that are written, in order to make them durable and persistent at restore time. It is stored in a binary format, which makes it hard to read for the naked eye if you need to debug the writing activity. Fortunately, the first effort to display the context of WAL records in a  “human-readable” format  appeared in version 9.3 and it has been evolving since then: using the [pgxlogdump/pg_waldump](https://pgpedia.info/p/pg_waldump.html) server utility.


Actually, I had never paid attention to this server utility until the most recent version of Postgres ([15](https://www.postgresql.org/about/news/postgresql-15-released-2526/)), which released a new way to see what happens inside the WAL using plain SQL language through the [pg_walinspect](https://www.postgresql.org/docs/15/pgwalinspect.html) extension.  I would like to thank the author (Bharath Rupireddy) for making life a bit easier for Postgres users.

The extension name is self-explanatory and it allows inspecting WAL records, either for debugging, reporting, or even for exploratory purposes about how Postgres works.


## Hands-on with pg_walinspect

The first step, is to install the extension, which does not differ from any other extension: 

```sql
postgres=# CREATE EXTENSION pg_walinspect;
```

This extension allows us to examine the records between two valid WAL [Log Sequence Number](https://www.postgresql.org/docs/current/wal-internals.html) (LSN in short): _**start_lsn**_ and  _**end_lsn**_. Those LSN can be obtained through the _**pg_current_wal_lsn**_ function.

The _**start_lsn**_ can be obtained as follows:


```sql
postgres=# SELECT pg_current_wal_lsn(),now();
 pg_current_wal_lsn |              now              
--------------------+-------------------------------
 0/157BA88          | 2022-10-23 15:51:21.532482-03
(1 row)

```

It is needed to generate some write activity for the example. Let’s do some straightforward `CREATE TABLE-INSERT-DELETE` operations:

```sql
postgres=# create table my_tab(i int, j text);
CREATE TABLE
postgres=# INSERT INTO my_tab VALUES(1,'1'),(2,'2');
INSERT 0 2
postgres=# DELETE FROM my_tab where i=1;
DELETE 1
```

Take the _**end_lsn**_ in the same way as the _**start_lsn**_ :

```sql
postgres=# SELECT pg_current_wal_lsn(),now();
 pg_current_wal_lsn |              now              
--------------------+-------------------------------
 0/157E768          | 2022-10-23 15:51:37.255149-03
(1 row)

```

Using the _**pg_get_wal_records_info**_ function with our extracted LSNs(_**0/157BA88**_ and  _**0/157E768**_) it will prompt a similar output as the following:


```sql
postgres=# select start_lsn, end_lsn,  xid, resource_manager, record_type, record_length, main_data_length, fpi_length, description::varchar(30) from pg_get_wal_records_info('0/157BA88', '0/157E768');

--- the whole output will be at the end in a linked file
start_lsn |  end_lsn  | xid | resource_manager |  record_type  | record_length | main_data_length | fpi_length |          description           
-----------+-----------+-----+------------------+---------------+---------------+------------------+------------+--------------------------------
 0/157BA88 | 0/157BAB8 |   0 | Storage          | CREATE        |            42 |               16 |          0 | base/5/16415
 0/157BAB8 | 0/157BB90 | 747 | Heap             | INSERT        |           211 |                3 |          0 | off 16 flags 0x00
 0/157BB90 | 0/157BBD0 | 747 | Btree            | INSERT_LEAF   |            64 |                2 |          0 | off 252
 ...
 ...
 ...
 0/157E6E0 | 0/157E708 | 748 | Transaction      | COMMIT        |            34 |                8 |          0 | 2022-10-23 15:51:29.170343-03
 0/157E708 | 0/157E740 | 749 | Heap             | DELETE        |            54 |                8 |          0 | off 1 flags 0x00 KEYS_UPDATED 
 0/157E740 | 0/157E768 | 749 | Transaction      | COMMIT        |            34 |                8 |          0 | 2022-10-23 15:51:34.056308-03
(100 rows)

```
The whole output is [this file](https://pastebin.com/raw/8pna2thm)


## WAL verbosity

The verbosity of the output will depend on your [wal_level](https://postgresqlco.nf/doc/en/param/wal_level/15/) setting, from the lowest (minimal) to the highest (_**logical**_), in this example we are using the default value (replica). The first impression is the high number of wal records: just for a few SQL operations 100 records were generated. If  the _**wal_level**_ is set to _**logical**_, the number of records increases (and of course decreases if it is set  to _**minimal**_). 

Another observation is that not all records have the same length. This is the reason  why not all activities have the same weight or impact on the database.


## General Stats

It is possible to see the general stats during the LSN intervals using the _**pg_get_wal_stats**_ function

```sql
postgres=# select * from pg_get_wal_stats('0/157BA88', '0/157E768', true) order by count_percentage desc;
 resource_manager/record_type | count | count_percentage | record_size | record_size_percentage | fpi_size | fpi_size_percentage | combined_size | combined_size_percentage 
------------------------------+-------+------------------+-------------+------------------------+----------+---------------------+---------------+--------------------------
 Btree/INSERT_LEAF            |    65 |               65 |        4456 |     39.725416778104666 |        0 |                   0 |          4456 |       39.416187527642634
 Heap2/MULTI_INSERT           |    10 |               10 |        3181 |     28.358741196398324 |        0 |                   0 |          3181 |        28.13799203892083
 Heap/INSERT                  |     7 |                7 |        1299 |     11.580636533832575 |        0 |                   0 |          1299 |        11.49049093321539
 Transaction/COMMIT           |     3 |                3 |        1161 |     10.350361059106714 |        0 |                   0 |          1161 |       10.269792127377267
 Storage/CREATE               |     3 |                3 |         126 |      1.123294998662744 |        0 |                   0 |           126 |       1.1145510835913313
 Heap/INPLACE                 |     2 |                2 |         376 |      3.352054916644379 |        0 |                   0 |           376 |       3.3259619637328615
 Heap2/PRUNE                  |     2 |                2 |         180 |     1.6047071409467772 |        0 |                   0 |           180 |        1.592215833701902
 Standby/LOCK                 |     2 |                2 |          84 |     0.7488633324418293 |        0 |                   0 |            84 |       0.7430340557275542
 Btree/DELETE                 |     1 |                1 |          60 |     0.5349023803155925 |        0 |                   0 |            60 |       0.5307386112339673
 Standby/RUNNING_XACTS        |     1 |                1 |          50 |      0.445751983596327 |        0 |                   0 |            50 |      0.44228217602830605
 Heap/DELETE                  |     1 |                1 |          54 |     0.4814121422840332 |        0 |                   0 |            54 |       0.4776647501105705
 Heap/HOT_UPDATE              |     1 |                1 |          80 |     0.7132031737541232 |        0 |                   0 |            80 |       0.7076514816452897
 Heap/INSERT+INIT             |     1 |                1 |          61 |      0.543817419987519 |        0 |                   0 |            61 |       0.5395842547545334
 XLOG/FPI                     |     1 |                1 |          49 |     0.4368369439244005 |       88 |                 100 |           137 |       1.2118531623175586
(14 rows)

```

In this case statistics grouped by _**resource_manager and  record_type**_ are shown. Also since the _**Btree**_ index activity is the item that most uses wal record size and  without having defined any btree index in the SQL statements example, possibly these activities are related to _**pg_class'**_ indexes. In addition, this helps to remind us how the index maintenance generates workload in the database,  and if the unused indexes are removed from the database this will help to improve the performance by transitivity. The output of the function will be useful to understand what is the activity that most generates the record wal and detects some anomalies or explains the server behavior.

## CREATE record operations

Let's focus now on the _**resource_manager and record_type columns**_, which show a kind of classification of the wal type, and the specific wal record type respectively. More details about the meaning of each one in the: [rmgrlist.h](https://github.com/postgres/postgres/blob/REL_15_STABLE/src/include/access/rmgrlist.h) and [rmgrdesc](https://github.com/postgres/postgres/tree/REL_15_STABLE/src/backend/access/rmgrdesc) files header files at the Postgres source code.

_**Storage/CREATE**_ combination means that an object was created, but in this case there are three occurrences of these combinations and our test only wrote one. How is this possible?

If we check out the description column, it is possible to see the path and OID of the created objects and if we analyze these paths we can see the following: 


```sql
SELECT
	relname,
	CASE 
		when relkind = 'r' then 'tab'
		when relkind = 'i' then 'idx'
		when relkind = 'S' then 'seq'
		when relkind = 't' then 'toast'
		when relkind = 'v' then 'view'
		when relkind = 'm' then 'matview'
		when relkind = 'c' then 'composite'
		when relkind = 'f' then 'F tab'
		when relkind = 'p' then 'part tab'
		when relkind = 'I' then 'part idx'
	END as object_type
   FROM
		pg_class
	WHERE
		oid IN ('16415', '16418', '16419');

       relname        | object_type 
----------------------+-------------
 pg_toast_16415       | toast
 pg_toast_16415_index | idx
 my_tab               | tab
(3 rows)

```

_Note: relkind values can be found in pg_class documentation_



The table and the TOAST table with the corresponding index were created, so keep in mind that behind the scenes it could happen that the creation of many objects required some extra data for persisting. This applies for instance to indexes, TOAST objects, etc., so be aware that some objects can generate other implicit objects and therefore some extra workload.

## Numbers of transactions 

Let's take a look at the column xid, which represents the transaction's number. It is possible to see three transaction numbers _**747, 748 and 749**_. This reminds us how Postgres encapsulates a simple SQL sentence in a transaction if we do not specify the BEGIN and _**COMMIT/ROLLBACK**_ block of the transaction.

_Note: It is helpful to be aware, whenever possible, of including a transaction block (BEGIN…COMMIT/ROLLBACK) when executing some related SQL statements. Including a transaction's block avoids wasting the transaction's id which will eventually help reach the wraparound threshold limit and force an aggressive autovacuum. This type of autovacuum could impact directly on the database performance._


In addition at the end of each transaction, it is possible to see a _**Transaction/COMMIT**_ combination and the timestamp of this transaction end.


## Conclusions 

The new extension provides helpful information about the all the activities behind the scene to make possible the the right work inside Postgres, and will help us to take X-rays and understand a bit more how Postgres works. In particular, it allows us just by using SQL sentences to show us some expected behaviors and others not so well known, at least for me,  such as the weight of indexes maintenance activity. Meanwhile, we can continue learning more details about the use of this extension and Postgres's behavior, and maybe in the future the extension will let us audit and analyze the workflow of the database server and generate some interesting and beautiful reports. 
