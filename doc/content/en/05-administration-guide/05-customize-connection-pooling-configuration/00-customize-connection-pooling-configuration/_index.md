---
title: Customize Connection Pooling Configuration
weight: 2
url: administration/cluster/pool/custom/config
draft: true
showToc: true
---

## Disabling Pooling

Certain set of applications, particularly those for reporting or OLAP, may not need a pooling
 middleware to issue large queries and low amount of connections. It is possible to disable
 pooling by setting `disableConnectionPooling` to `true` at the Cluster configuration (for more
 information, see [CRD Cluster Pods configuration]({{% relref "06-crd-reference/01-sgcluster/" %}}) ).

```bash
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    disableConnectionPooling: false
...
```

Either way, if your application does internal pooling or it already has a pooling middleware, you
 can consider disabling internal pooling mechanisms. Although, we encourage the user to keep pooling
 enabled internally, as it serves as a contention barrier for unexpected connection spikes that may
 occur, bringing more stability to the cluster.

## Transaction Mode

This configuration is recommended for most efficient pool allocations:

```bash
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  namespace: my-cluster
  name: poolconfig1
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        pool_mode: transaction
        max_client_conn: '1000'
        default_pool_size: '80'
EOF
```

## Session Mode with Connection release through timeouts

This configuration requires more insights and specifications to be known from the application used
 against the cluster. What it is intended here, is to release connections that are
 _idle in transaction_.

You'll notice that the bellow is ordered from variables that affect client-side to the server-side,
 incrementally. If your application sets a client timeout when connection is idle, you may not need
 to do this, although several production clusters may be source for not only one, but many
 applications within different connection handlings.


```bash
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  namespace: my-cluster
  name: poolconfig-session-prod
spec:
  pgBouncer:
    pgbouncer.ini:
      pgboucner:
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

When the server pool is fulfilled, incoming client connection stablish requests will be queued set
 in `wait` state by PgBouncer. This is why it is important to ensure that server connections are
 released properly, specially if they are keep during long periods of time.
