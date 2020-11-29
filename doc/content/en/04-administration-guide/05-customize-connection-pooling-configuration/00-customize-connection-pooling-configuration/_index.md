---
title: Customize Connection Pooling Configuration
weight: 2
url: administration/cluster/pool/custom/config
draft: true
---

## Transaction Mode

This configuration is recommended for most efficient pool allocations:

```bash
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGPoolingConfig
metadata:
  namespace: my-cluster
  name: poolconfig1
spec:
  pgBouncer:
    pgbouncer.ini:
      pool_mode: transaction
      max_client_conn: '1000'
      default_pool_size: '80'
EOF
```

## Session Mode emulating Postgres behavior

Even tho the main purpose of a pooling tool is to queue connections, sometimes your application may not be handling connection handling properly, which requires a more strict configuration as follows:

```bash
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGPoolingConfig
metadata:
  namespace: my-cluster
  name: poolconfig-session-hardlimit
spec:
  pgBouncer:
    pgbouncer.ini:
      pool_mode: session
      max_client_conn: '80'
      default_pool_size: '80'
EOF
```

Within this, you can limit the connections to the pooling layer and the server side, simulating what Postgres does when `max_connections` is overpassed. In this case, `max_client_conn` specifies the amount of allowed client connections and the `default_pool_side`, the number of server-side connections.

## Session Mode with Connection release through timeouts

This configuration requires more insights and specifications to be known from the application used against the cluster. What it is intended here, is to release connections that are idle are idle in transaction.

You'll notice that the bellow is ordered from variables that affect client-side to the server-side, incrementally. If your application sets a client timeout when connection is idle, you may not need to do this, although several production clusters may be source for not only one, but many applications within different connection handlings.


```bash
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGPoolingConfig
metadata:
  namespace: my-cluster
  name: poolconfig-session-prod
spec:
  pgBouncer:
    pgbouncer.ini:
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

When the server pool is fulfilled, incoming client connection stablish requests will be queued set in `wait` state by PgBouncer. This is why it is important to ensure that server connections are released properly, specially if they are keep during long periods of time.
