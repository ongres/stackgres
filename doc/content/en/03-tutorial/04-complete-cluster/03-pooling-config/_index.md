---
title: Custom Connection Pooling Configuration
weight: 3
url: tutorial/complete-cluster/pooling-config
description: Details about how to create custom pool configurations.
---

StackGres will deploy Postgres clusters, by default, with a sidecar with a connection pooler (set the
[SGCluster.pods.disableConnectionPooling]({{% relref "06-crd-reference/01-sgcluster/#pods" %}}) property if you
don't want such a connection pooler). The goal of this connection pooler fronting the database is to allow to control
the incoming connections (fan-in) and keep Postgres operating with a lower number of concurrent connections, while
allowing a higher number of external connections.

A default configuration is provided by StackGres. But you may provide your own, creating an instance of the CRD
[SGPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}}). StackGres currently uses
[PgBouncer](https://www.pgbouncer.org/). To create a custom PgBouncer configuration, create the file
`sgpoolingconfig-pgbouncer1.yaml`:

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  namespace: demo
  name: poolconfig1
spec:
  pgBouncer:
    pgbouncer.ini:
      pool_mode: session
      max_client_conn: '200'
      default_pool_size: '200'
```

and deploy to Kubernetes:

```bash
kubectl apply -f sgpoolingconfig-pgbouncer1.yaml
```

You may inspect the default values provided by StackGres by describing the created CRD:

```bash
kubectl -n demo describe sgpoolconfig poolconfig1
```

```plain
Name:         poolconfig1
Namespace:    demo
Labels:       <none>
Annotations:  stackgres.io/operatorVersion: 1.0.0-alpha1
API Version:  stackgres.io/v1
Kind:         SGPoolingConfig
Metadata:
  Creation Timestamp:  2021-03-01T10:18:20Z
  Generation:          1
  Resource Version:    154323
  Self Link:           /apis/stackgres.io/v1/namespaces/demo/sgpoolconfigs/poolconfig1
  UID:                 2c4f5b08-041c-463d-a5ab-13dc8deabc93
Spec:
  Pg Bouncer:
    pgbouncer.ini:
      admin_users:                postgres
      application_name_add_host:  1
      auth_query:                 SELECT usename, passwd FROM pg_shadow WHERE usename=$1
      auth_type:                  md5
      auth_user:                  authenticator
      default_pool_size:          200
      ignore_startup_parameters:  extra_float_digits
      listen_addr:                127.0.0.1
      max_client_conn:            200
      max_db_connections:         0
      max_user_connections:       0
      pool_mode:                  session
      stats_users:                postgres
Status:
  Pg Bouncer:
    Default Parameters:
      stats_users
      ignore_startup_parameters
      auth_type
      max_db_connections
      pool_mode
      auth_query
      application_name_add_host
      max_user_connections
      auth_user
      listen_addr
      admin_users
Events:  <none>
```
