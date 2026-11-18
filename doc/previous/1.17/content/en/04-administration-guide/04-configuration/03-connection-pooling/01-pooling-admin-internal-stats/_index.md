---
title: Pooling Administration and Internal Stats
weight: 1
url: /administration/configuration/pool/admin
aliases: [ /administration/cluster/pool/admin ]
description: Details about how to check the pool configuration.
showToc: true
---

## Accessing the Pooling Admin console

PgBouncer includes an admin database-style connection for getting valuable information about the pool stats, like
counters, aggregations, client and server connection, etc. Those values are critical to be understood for a production
alike environment.

Access the console through container socket and `pgbouncer` (this is not a database user) user:

```
kubectl exec -it  -c postgres-util test-0 -- psql  -p 6432 -d pgbouncer pgbouncer
psql (12.4 OnGres Inc., server 1.13.0/bouncer)
Type "help" for help.

pgbouncer=# \x
Expanded display is on.
```

## Getting valuable pool information


```
pgbouncer=# show stats;
-[ RECORD 1 ]-----+----------
database          | pgbouncer
total_xact_count  | 1
total_query_count | 1
total_received    | 0
total_sent        | 0
total_xact_time   | 0
total_query_time  | 0
total_wait_time   | 0
avg_xact_count    | 0
avg_query_count   | 0
avg_recv          | 0
avg_sent          | 0
avg_xact_time     | 0
avg_query_time    | 0
avg_wait_time     | 0
```

```
pgbouncer=# show pools;
-[ RECORD 1 ]---------
database   | pgbouncer
user       | pgbouncer
cl_active  | 1
cl_waiting | 0
sv_active  | 0
sv_idle    | 0
sv_used    | 0
sv_tested  | 0
sv_login   | 0
maxwait    | 0
maxwait_us | 0
pool_mode  | statement
```

```
pgbouncer=# show clients;
-[ RECORD 1 ]+------------------------
type         | C
user         | pgbouncer
database     | pgbouncer
state        | active
addr         | unix
port         | 6432
local_addr   | unix
local_port   | 6432
connect_time | 2020-10-23 13:19:54 UTC
request_time | 2020-10-23 14:18:23 UTC
wait         | 3445
wait_us      | 617385
close_needed | 0
ptr          | 0x1a5c350
link         | 
remote_pid   | 28349
tls          | 
```

Other useful commands:

- `show servers`
- `show fds`
- `show mem`
- `show stats_totals`
- `show stat_averages `


## Reference

Available commands:

```
        SHOW HELP|CONFIG|DATABASES|POOLS|CLIENTS|SERVERS|USERS|VERSION
        SHOW FDS|SOCKETS|ACTIVE_SOCKETS|LISTS|MEM
        SHOW DNS_HOSTS|DNS_ZONES
        SHOW STATS|STATS_TOTALS|STATS_AVERAGES|TOTALS
        SET key = arg
        RELOAD
        PAUSE [<db>]
        RESUME [<db>]
        DISABLE <db>
        ENABLE <db>
        RECONNECT [<db>]
        KILL <db>
        SUSPEND
        SHUTDOWN
```

