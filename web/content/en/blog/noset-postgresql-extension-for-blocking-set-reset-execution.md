+++
title = "Noset: PostgreSQL extension for blocking  SET and RESET execution"
date = 2021-08-11T12:30:00+02:00
author = ["3manuek","asotolongo"]
tags = ["postgresql", "extension","set","reset"]
planet = "asotolongo"
draft = false
readtime = "6 min"
thumbnail = "img/blog/noset-v1.jpg"
description = "noset is a PostgreSQL extension that blocks SET and RESET of user-context parameters, preventing sessions from overriding configured settings."
+++




## Introduction

Each PostgreSQL [parameter](https://postgresqlco.nf/doc/en/param/) has its own functionality and an associated context which defines the context level on top of which a  parameter can be changed. The possible context  values  are (from the lowest to the highest level): internal, postmaster, sighup, backend, superuser-backend, superuser and user ( more details can be found in the [documentation](https://www.postgresql.org/docs/current/view-pg-settings.html)).This blog will focus on those settings with the user context, that is all the settings that can be changed by any user in their sessions.  

 

This option can be pretty useful, above all if the user needs to modify the behavior of his session changing some specific parameters to obtain  benefit from it, but it can be a double-edged sword too,  because some "smart" users can change some parameters recklessly and “steal” resources for himself. In this way, it can impede other processes/users from using it and  sometimes they can destroy postgres service, for example causing an OOM issue. Some example cases:  setting a super high work_mem, decreasing the memory available for other processes, or taking more  CPU  resources for himself changing in his session the max_parallel_workers_per_gather, among other examples.



So far PostgreSQL has no way of avoiding this situation, but taking advantage of  PostgreSQL extensibility, we developed an extension using PostgreSQL's hook mechanism to avoid  reckless non-superusers to change a parameter via SET command.
practical manner.


## noset extension

The [noset extension](https://gitlab.com/ongresinc/extensions/noset/-/tree/main) was developed by the StackGres team and it is a loadable module via shared_preload_libraries that blocks the `SET/RESET` command for users’ sessions. The extension has support for version 12 and 13, and is planned to support the newer versions.



### Installation and setting

For installation the [devel PostgreSQL's packages](https://www.postgresql.org/download/linux/ubuntu/) and download [code’s extension](https://gitlab.com/ongresinc/extensions/noset/-/tree/main) is required.

```bash
make 
make install
```

The noset module must be loaded by adding it to the shared_preload_libraries and requires a service restart.

```bash
shared_preload_libraries = 'noset'
```
The extension has a variable named noset.enabled, that defines if the role/user is prohibited from running SET/RESET command and changing a parameter in his session (default false)

### Example

Creating user and setting the `noset.enabled` option

```sql
psql -d db -U postgres
db=# create user appuser password 'mypass';
CREATE ROLE
db=# alter user appuser set noset.enabled = true;
ALTER ROLE
```
Using the user created previously and trying to change a parameter via `SET` command


```sql
psql -d db -U appuser
--login as appuser
db=> select current_user;
 current_user 
--------------
 Appuser
(1 row)
db=> show noset.enabled ;
 noset.enabled 
---------------
 On
(1 row)
db=> set work_mem ='1GB';
ERROR:  permission denied to set/reset parameter 'set work_mem = '1GB';
```
It is possible to verify the users who are prohibited from changing parameters through SET by consulting PostgreSQL's catalog.

```sql
db=# select usename, useconfig from pg_user where useconfig is not null and array['noset.enabled=on']  <@ useconfig;
 usename |     useconfig      
---------+--------------------
 appuser | {noset.enabled=on}
(1 row)
```

## Conclusions 

Using the noset extension it is possible to provide an easy way to avoid the “smart” users from changing parameters in their session indiscriminately, and shown once again the excellent  extensibility capabilities of PostgreSQL.
