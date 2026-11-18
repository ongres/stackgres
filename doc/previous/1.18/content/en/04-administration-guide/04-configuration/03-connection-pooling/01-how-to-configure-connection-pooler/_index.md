---
title: Configuration of the Connection Pooler Modes
weight: 1
url: /administration/cluster/pool/custom/config
description: Details about how to configure the connection pooler modes
showToc: true
---

## Session Mode

This is the most stable and default mode. In order for the pool to be effectively used it requires the clients to close the connection when a session is no longer used. Here is an example of `SGPoolingConfig` that uses session mode:

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  name: poolconfig
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        pool_mode: session
        max_client_conn: '100'
        default_pool_size: '80'
```

## Transaction Mode

This configuration is recommended for most efficient pool allocations but requires the application to be restricted in order to not use session objects. A session object is any object that can be created during a connection session with the database (see [Postgres Architectural Fundamentals](https://www.postgresql.org/docs/current/tutorial-arch.html)) like session settings, temporary tables, prepared statements, etc. (prepared statements can be used in some cases, see the [Pgbouncer FAQ](https://www.pgbouncer.org/faq.html#how-to-use-prepared-statements-with-transaction-pooling)). Here is an example of `SGPoolingConfig` that uses transaction mode:

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  name: poolconfig
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        pool_mode: transaction
        max_client_conn: '1000'
        default_pool_size: '80'
```

## Session Mode with Connection release through timeouts

This configuration requires more insights and specifications to be known from the application used
 against the cluster. What it is intended here, is to release connections that are
 _idle in transaction_.

You'll notice that the below is ordered from variables that affect client-side to the server-side,
 incrementally. If your application sets a client timeout when connection is idle, you may not need
 to do this, although several production clusters may be source for not only one, but many
 applications within different connection handlings.


```
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  name: poolconfig
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        pool_mode: session
        max_client_conn: '1000'
        default_pool_size: '80'
        client_idle_timeout: '30s'
        idle_transaction_timeout: '60s'
        server_idle_timeout: '120s'
        server_lifetime: '240s'
        server_fast_close: '300s'
EOF
```

When the server pool is fulfilled, incoming client connection will be queued in `wait` state by PgBouncer. This is why it is important to ensure that server connections are released properly, especially if they are kept during long periods of time.
