+++
title = "Dealing and preventing PL/pgSQL runtime errors"
date = 2023-07-20T12:00:00+01:00
author = ["asotolongo"]
tags = ["postgresql", "plpgsql"]
planet = "asotolongo"
draft = false
readtime = "6 min"
thumbnail = "img/blog/errors_at_execution_time.jpg"
description = "Detect and prevent PL/pgSQL runtime errors in Postgres with plpgsql_check: catching the semantic mistakes that syntax validation cannot see."
+++



## Introduction


When PL/pgSQL code is written within functions/procedures in Postgres and does not contain any syntax errors, the code is created successfully. However, this does not guarantee that the code is free from suboptimal or incorrect semantics, which can lead to runtime errors. For example, a missing RETURN in a function. 

The majority of these semantic errors are human mistakes whenever writing PL/pgSQL, or the function became deprecated due to schema changes that break its functionality. A Database Versioning Control (DBVC) can help in the second case. A DBVC has the ability to interoperate between different versions of the schema and expose which to use, and also allow you to  reproduce it and detect mistakes related to changes before committing. 

 A recommended Postgres tool to facilitate the detection of these types of errors or warnings is **[plpgsql_check][1]** extension (developed by Pavel Stehule), which will help you address potential errors related to the errors we mentioned and eventually enhance your development experience on Postgres functions and procedures. 

The  **[plpgsql_check][1]** extension does:

* Check fields of referenced database objects and types inside the embedded SQL
* Validate Typing of the function arguments.
* Identifies unused variables and function arguments, unmodified OUT arguments.
* Partial detection of dead code (code after an unqualified RETURN command).
* Missing RETURN statements in a function (common after exception handlers, complex logic, etc).
* Ability to detect potential SQL injection vulnerability in EXECUTE statement.

Despite the help of this great extension, there are some errors related to semantic mistakes that you can not detect. In this blog, we'll show you some corner case errors that you may get when executing PL/pgSQL functions and how to avoid them. This point can also benefit those who usually use other RDBMS.



## Non-closed cursors 

Postgres provides `CURSOR` capability to point to a SELECT statement *Result Set*. One of the reasons on using CURSOR is avoid memory exhaustation in large Result Sets, by fetching few row batches at a time in a single transaction -- see [cursor operations](https://www.postgresql.org/docs/current/plpgsql-cursors.html). The CLOSE operation is an important operation which closes the cursor, frees up resources, and recycles the cursor variable. Even though it will be closed automatically when the transaction closes, one golden rule is to always close the cursor if you have finished processing the data. If the transaction has not finished, and you need to open the same cursor inside the same transaction, you can obtain an error similar to **SQL Error [42P03]:** `ERROR: cursor "X" already in use`. This is a reproducible test case: 



```sql
test=# CREATE or REPLACE FUNCTION inc_quantity_by_date (p_orderdate date, p_quantity smallint)    RETURNS SETOF public.orderlines
    AS $$
DECLARE
    c1 CURSOR for SELECT * from public.orderlines  where orderdate = $1;
    res public.orderlines;
BEGIN
    OPEN c1;
    LOOP
        FETCH c1 INTO res;
        EXIT  WHEN NOT found;
        UPDATE  public.orderlines  set  quantity = quantity - $2  where  orderid = res.orderid  AND prod_id = res.prod_id;
        RETURN NEXT res;
    END LOOP;
    --never close the cursor c1;
    RETURN;
END;
$$ LANGUAGE plpgsql;
CREATE FUNCTION
test=# BEGIN;
test=# SELECT * FROM inc_quantity_by_date('2004-01-27',1::smallint);
test=# SELECT * FROM inc_quantity_by_date('2004-01-14',2::smallint);
ERROR:  cursor "c1" already in use
CONTEXT:  PL/pgSQL function inc_quantity_by_date(date,smallint) line 6 at OPEN

```
To avoid this kind of error, always close the cursor `c1` using the clause `CLOSE c1`; before `RETURN` inside the function, and make a habit of doing this.


## Deleted types 

Postgres allows User-defined Data Types using simple clauses such as [CREATE DOMAIN](https://www.postgresql.org/docs/current/sql-createdomain.html) and [CREATE TYPE](https://www.postgresql.org/docs/current/sql-createtype.html), which is really convenient to customize or extend primitive data types, and of course, these User-defined Data Types can be used inside functions and procedures, for example: 

```sql
test=# CREATE TYPE public.price_type AS ENUM ('EXPENSIVE', 'CHEAP') ;
test=# CREATE or REPLACE  FUNCTION type_example (pid int)  RETURNS  text
    AS $$
DECLARE
    result public.price_type ;
BEGIN
        result:= (SELECT CASE WHEN p.price > 20 THEN 'EXPENSIVE' ELSE 'CHEAP' END  FROM public.products p WHERE p.prod_id=$1);
        RETURN result;
END;
$$ LANGUAGE plpgsql; 
CREATE FUNCTION
```

In this case, if the data type `public.price_type` is deleted, Postgres will allow it because it does not store the dependent relationships between the use of data type inside the code and the data type itself. And this situation will cause an error in execution time, For example:

```sql
test=# select oid, typname from pg_type where typname='price_type';
  oid   |  typname
--------+------------
 120872 | price_type
(1 row)
test=# drop type public.price_type ;
DROP TYPE
test=# select type_example(81);
ERROR: cache lookup failed for type 120872
```

To prevent issues when deleting a custom data type, **be sure to update the corresponding functions or recreate the data type as needed**. Another example related to this scenario is discussed in the following section.


## Delete tables with values in TOAST 

Postgres uses **The Oversized-Attribute Storage Technique([TOAST](https://www.postgresql.org/docs/current/storage-toast.html))** to store very large field values. This means that a large field is split into multiple physical rows in another into a special table, only on those that are “TOASTable” datatypes -- for more details check the [documentation](https://www.postgresql.org/docs/current/storage-toast.html). 

All this activity occurs behind the scenes and is transparent to the user. Inside the function code, the **values stored in the TOAST are not immediately fetched from the TOAST when assigned to a variable**, which, if you do not know this, can cause an error in the execution time if the table that contains the data is deleted, for example: 

```sql
test=# CREATE OR REPLACE  FUNCTION fetch_toast_data_example() RETURNS text as $$
DECLARE
  txt text;
BEGIN
  CREATE TABLE tab1 (i int, j text);
  -- To force to use store in toast without compresion:
  ALTER TABLE tab1 ALTER COLUMN j SET STORAGE EXTERNAL; 
  INSERT INTO tab1 SELECT i, repeat('textvalue ',10000) from generate_series (1,1000 ) AS i; 
  RAISE NOTICE 'data about tables and toast: %', 
        (SELECT 'Normal table ' ||relname||' -> OID='||oid||' - Toast table ' || 
          (SELECT relname FROM pg_class WHERE oid=c.reltoastrelid)  ||'-> OID=' || 
          (SELECT oid FROM pg_class WHERE oid=c.reltoastrelid)  
        FROM pg_class c WHERE relname = 'tab1' );
  
  -- Fetch the data from toast table:
  txt:= (SELECT j  FROM tab1 WHERE i = 20); 
  
  -- Drop tab1 table before return data:
  DROP TABLE tab1; 

  RETURN 'text: '||txt;
END;
$$ LANGUAGE plpgsql;
CREATE FUNCTION
test=# SELECT fetch_toast_data_example();
--you will get error due to delete the table before return the result
NOTICE:  Info about tables and toast: Normal table tab1 -> OID=128975 - Toast table pg_toast_128975-> OID=128978
ERROR:  could not open relation with OID 128978 --this is the OID of Toast table
CONTEXT:  PL/pgSQL function fetch_toast_data_example() line 11 at RETURN
```

To avoid this error, don't DELETE the main table before RETURN clause. Take into account that the TRUNCATE clause also will fail because truncate destroys the object and creates again with another relfilenode.  

A way to workround this case is to implement the above function as a  PROCEDURE, due that they are non-atomic context, for more details please check the following [thread](https://www.postgresql.org/message-id/flat/17669-2238da15dce5c53c%40postgresql.org).  

```sql
CREATE OR REPLACE PROCEDURE fetch_toast_data_example()  as $$
DECLARE
  txt text;
BEGIN
  CREATE TABLE tab1 (i int, j text);
  -- Force to use store in toast without compresion:
  ALTER TABLE tab1 ALTER COLUMN j SET STORAGE EXTERNAL; 
  INSERT INTO tab1 SELECT i, repeat('textvalue ',10000) from generate_series (1,1000 ) AS i; 
  RAISE NOTICE 'data about tables and toast: %', 
        (SELECT 'Normal table ' ||relname||' -> OID='||oid||' - Toast table ' || 
          (SELECT relname FROM pg_class WHERE oid=c.reltoastrelid)  ||'-> OID=' || 
          (SELECT oid FROM pg_class WHERE oid=c.reltoastrelid)  
        FROM pg_class c WHERE relname = 'tab1' );

  -- Fetch the data from toast table :
  txt:= (SELECT j  FROM tab1 WHERE i = 20); 
  -- Drop tab1 table before return data:
  DROP TABLE tab1; 
  RAISE NOTICE 'text: %', txt;
  -- No return needed
END;
$$ LANGUAGE plpgsql;
```

Another way to handle this is by using temporal tables that automatically get deleted once the transaction ends:

```sql
  ...
  CREATE TEMP TABLE tab1 (i int, j text);
  ...
  RETURN 'text: '||txt;
  ...
```

## Conclusions 

As you have been able to appreciate above, even when a function may have been defined correctly, some errors can appear at runtime. The examples that have been shown here may be corner cases, but despite these scenarios being unlikely, they have all been real cases. You need to be aware of this, and in addition, using the extension  [`plpgsql_check`][1] can help to avoid some headaches. 

Please, if you know of any other semantic mistakes that can affect in runtime, share them with us,  using Twitter tag us @ongresinc or @AnthonySotolong

[1]: https://github.com/okbob/plpgsql_check 

