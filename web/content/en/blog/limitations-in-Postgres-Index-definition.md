+++
title = "Limitations in Postgres Index definition "
date = 2023-05-16T12:00:00+01:00
author = ["asotolongo"]
tags = ["postgresql", "indexes"]
planet = "asotolongo"
draft = false
readtime = "6 min"
thumbnail = "img/blog/create_index_limitations.jpg"
description = "A tour of the limitations in Postgres index definitions: expression, partial and covering indexes, and the cases where they cannot be used."
+++



## Introduction


Indexes are a core RDBMS component and Postgres ain’t an exception. Furthermore, Postgres indexing capabilities aren’t tied to the available native-supported mechanisms. Aside from the available [6 types of native indexes](https://www.postgresql.org/docs/current/indexes-types.html) (B-tree, Hash, GiST, SP-GiST, GIN, BRIN) it is even possible to extend index capabilities using the [Access Method Extensions Interface](https://www.postgresql.org/docs/current/xindex.html) for customizing and building your own.

There is a subcategory of index types that are classified according to their [expressions and definitions](https://www.postgresql.org/docs/current/indexes.html), and they can use custom operators to extend the usage of more advanced data types. Those subcategories can be Multicolumn, Unique, Expressional, Partial, and Covering Indexes.

_Note: It is possible to implement other types of indexes and add them to PostgreSQL as extensions for example:_ https://www.postgresql.org/docs/current/bloom.html or https://github.com/postgrespro/rum 

Nevertheless,  under certain circumstances, there are certain limitations on index definitions. This is precisely the objective of the current blog: to show under what circumstances you can not create indexes.

## Index functions must be IMMUTABLE 

The following error is very likely to be the most common one of this type of restriction, and it is in response to the restriction that when you use a function in an index expression, this function must be IMMUTABLE, this is one of “Function Volatility Categories”, please read the [documentation](https://www.postgresql.org/docs/current/xfunc-volatility.html) to understand the differences between categories.


```sql
mydatabase=>CREATE INDEX CONCURRENTLY idx_test on  table_test  (test_id, to_char(create_time, 'YYYY-MM-DD') );
ERROR:  functions in index expression must be marked IMMUTABLE

```
If it describes the _**to_char**_ function is possible to see that this function is marked as _**STABLE**_




```sql
mydatabase=>\df+ to_char
Schema              | pg_catalog
Name                | to_char
Result data type    | text
Argument data types | timestamp with time zone, text
Type                | func
Volatility          | stable
Parallel            | safe
Owner               | postgres
Security            | invoker
Access privileges   | 
Language            | internal
Source code         | timestamptz_to_char
Description         | format timestamp with time zone to text

```

Sometimes, to avoid this issue, you can wrap the function inside your own function marked as _**IMMUTABLE**_, such as in [this example](https://stackoverflow.com/questions/5973030/error-functions-in-index-expression-must-be-marked-immutable-in-postgres), but be aware that not always you can use this solution because the functions are “black boxes” and must contain safe code inside them.

Also, you won't always see this error in an explicit definition, sometimes the non-immutable  functions are masked, for example in a cast operation:


```sql
mydatabase=>CREATE INDEX CONCURRENTLY idx_test_xml ON xml_table  ((((xpath('/book/title/text()'::text, info::xml))[1])::text));
ERROR:  functions in index expression must be marked IMMUTABLE

```

In this case, the error coming from the cast of _**info::xml**_, due to the function used to  _**cast(xml)**_ is marked as _**STABLE**_

```sql
mydatabase=> \df+ xml
Schema              | pg_catalog
Name                | xml
Result data type    | xml
Argument data types | text
Type                | func
Volatility          | stable
Parallel            | safe
Owner               | postgres
Security            | invoker
Access privileges   | 
Language            | internal
Source code         | texttoxml
Description         | perform a non-validating parse of a character string to produce an XML value

```

Everything described in this section reminds us that if a function/expression is used in an index definition, it must be marked _**IMMUTABLE**_.


## Index creation on system columns

Every table in PostgreSQL has some system columns as described in [the documentation](https://www.postgresql.org/docs/current/ddl-system-columns.html), and none of those columns can be indexed, otherwise, you will get the following error:

```sql
mydatabase=> CREATE INDEX CONCURRENTLY idx_system_column on categories (ctid);
ERROR:  index creation on system columns is not supported
```

If you are thinking of using these system columns to do some operations in your database, please keep in mind that these operations will not use an index and can cause some performance issues. Therefore, avoid using these columns in your queries that manage a lot of data, This advice is especially for those who come from using other RDBMS that sometimes use these types of columns to do some operations in the production database.


## Index over catalogs tables 

PostgreSQL has a rich catalog system tables that provides a good option for getting information (definition, statistics, etc ) about the database itself. For more information, check [here](https://www.postgresql.org/docs/current/catalogs.html). As these are system tables, it is not possible to modify them to avoid inconsistencies in the database schema. If you try to index some columns of these tables, you get the following error:

```sql
mydatabase=> CREATE INDEX CONCURRENTLY idx_kind on pg_class (relkind);
ERROR:  permission denied: "pg_class" is a system catalog
```
So, if you have a lot of data in these types of tables and you will use them frequently, try querying using the columns that have been indexed by definition already.

Although, indexing catalog tables can be bypassed by using the [allow_system_table_mods](https://postgresqlco.nf/doc/en/param/allow_system_table_mods/) GUC parameter.

> Thanks to our friend [Fabrizio Mello](https://twitter.com/fabriziomello/status/1658549916132691978) for the reminder!


## Index on FDW tables

Another excellent feature in PostgreSQL is the [Foreign Data Wrapper](https://wiki.postgresql.org/wiki/Foreign_data_wrappers) (FDW) concept, which allows querying data from another PostgreSQL database, you must define and link a local table with a remote table, for more detail in the [documentation](https://www.postgresql.org/docs/current/postgres-fdw.html). Since you use the table concept, you may be tempted to create indexes on the FDW table to try to improve query performance, but since the data is remote, it is not possible to create indexes, if you try to define an index on the FDW table, you will get the following error:

```sql
mydatabase=> CREATE INDEX CONCURRENTLY idx_fdw_table on foreign_table (id);
ERROR:  cannot create index on foreign table "foreign_table"
```
To improve the performance of a query that uses an FDW table, you must create the indexes on a remote server or apply some changes described [here](https://stackgres.io/blog/boost-query-performance-using-fdw-with-minimal-changes/)




## Conclusions 

As you have been able to appreciate, PostgreSQL has some restrictions when it comes to creating some indexes over some types of tables/columns, and we recommend being aware of this because it can avoid some misunderstandings and some performance issues due to a bad database design or some specific query. Please, if you know of any other index restrictions, share them with us.
