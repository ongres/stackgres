---
title: Custom Connection Pooling Configuration
weight: 3
url: tutorial/complete-cluster/pooling-config
description: Details about how to create custom pool configurations.
---

By default, StackGres will deploy Postgres clusters with a sidecar containing a connection pooler.
You can set the [SGCluster.pods.disableConnectionPooling]({{% relref "06-crd-reference/01-sgcluster/#pods" %}}) property if you don't want such a connection pooler.
This connection pooler fronts the database and controls the incoming connections (fan-in).
This keeps Postgres operating with a lower number of concurrent connections, while allowing a higher number of external connections (from the application to the pooler).

StackGres provides production-grade default configuration.
But of course you may provide your own, by creating an instance of the [SGPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}}) CRD.
StackGres currently uses [PgBouncer](https://www.pgbouncer.org/).

To create a custom PgBouncer configuration, create the file `sgpoolingconfig-pgbouncer1.yaml`:

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  namespace: demo
  name: poolconfig1
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        max_client_conn: '200'
        default_pool_size: '200'
        pool_mode: transaction
```

and deploy it to Kubernetes:

```bash
kubectl apply -f sgpoolingconfig-pgbouncer1.yaml
```

You may inspect the default values provided by StackGres by describing the created custom resource:

```bash
kubectl -n demo describe sgpoolconfig poolconfig1
```

```plain
Name:         poolconfig1
Namespace:    demo
Labels:       <none>
Annotations:  stackgres.io/operatorVersion: 1.3.3
API Version:  stackgres.io/v1
Kind:         SGPoolingConfig
Metadata:
  Creation Timestamp:  2022-10-26T10:37:57Z
  Generation:          1
  Managed Fields:
    API Version:  stackgres.io/v1
    Fields Type:  FieldsV1
    fieldsV1:
      f:spec:
        .:
        f:pgBouncer:
          .:
          f:pgbouncer.ini:
            .:
            f:pgbouncer:
              .:
              f:default_pool_size:
              f:max_client_conn:
              f:pool_mode:
    Manager:         kubectl
    Operation:       Update
    Time:            2022-10-26T10:37:57Z
  Resource Version:  2707
  UID:               78d1c69d-281d-4fac-9522-f024c8a2cea7
Spec:
  Pg Bouncer:
    pgbouncer.ini:
      Pgbouncer:
        default_pool_size:          200
        ignore_startup_parameters:  extra_float_digits
        max_client_conn:            200
        max_db_connections:         0
        max_user_connections:       0
        pool_mode:                  transaction
Status:
  Pg Bouncer:
    Default Parameters:
      admin_users:                pgbouncer_admin
      application_name_add_host:  1
      auth_query:                 SELECT usename, passwd FROM pg_shadow WHERE usename=$1
      auth_type:                  md5
      auth_user:                  authenticator
      default_pool_size:          1000
      ignore_startup_parameters:  extra_float_digits
      listen_addr:                127.0.0.1
      max_client_conn:            1000
      max_db_connections:         0
      max_user_connections:       0
      pool_mode:                  session
      stats_users:                pgbouncer_stats
Events:                           <none>
```
