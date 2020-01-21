---
title: Connection pooling configuration
weight: 3
---

# Session mode

By default PgBouncer is configured in session mode with 1000 maximum client connections limit.
 This is not necessarily a bad configuration but tuning this aspect really depends on the needs of
 your application. In case the application already implement a pool of connection you could rely on
 that pool but if you later scale up the application it will necessarily need to update the
 configuration to avoid saturation of connection by decreasing the pool size proportionally to the
 number of application instances. This limits the application to grow not more than the PostgreSQL
 maximum connections limit (by default this is 100) and increasing this value should be avoided in
 order to not saturate database with parallel connections. So the session mode pool allow to scale
 beyond the limit of PostgreSQL maximum connections.

Main drawback of session mode is that you must configure application to actually close the connection
 in order to allow other connection that where waiting in the queue to start sending command to
 PostgreSQL.

# Transaction mode

PgBouncer can be configured in transaction mode that allow to scale and parallelize connections
 beyond the limit of PostgreSQL maximum connections and without the hassle of configuring
 application to actually close the connection. This mode make it possible by managing a waiting
 queue of transactions, so that multiple connection's transactions will be sent to a single
 PostgreSQL connection. This mode enable optimal usage of PostgreSQL connection by multiplexing
 transactions among a multiple connections.

The drawback of transaction mode is that application will not be able to use session object
 between transactions since two subsequent transactions could be sent to different PostgreSQL
 connections that has different session objects states.