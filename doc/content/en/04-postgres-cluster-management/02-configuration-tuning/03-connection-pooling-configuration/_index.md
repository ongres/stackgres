---
title: Connection pooling configuration
weight: 3
---

The connection pooling CR represent the configuration of PgBouncer.

___

**Kind:** StackGresConnectionPoolingConfig

**listKind:** StackGresConnectionPoolingConfigList

**plural:** sgconnectionpoolingconfigs

**singular:** sgconnectionpoolingconfig
___

**Spec**

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| pgbouncer.ini |   | object  | Section [pgbouncer] of pgbouncer.ini configuration | see below |

Default section [pgbouncer] of pgbouncer.ini:

```shell
listen_addr=127.0.0.1
unix_socket_dir=/var/run/postgresql
auth_type=md5
auth_user=authenticator
auth_query=SELECT usename, passwd FROM pg_shadow WHERE usename=$1
admin_users=postgres
stats_users=postgres
user=postgres
pool_mode=session
max_client_conn=1000
max_db_connections=100
max_user_connections=100
default_pool_size=100
ignore_startup_parameters=extra_float_digits
application_name_add_host=1
```

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresConnectionPoolingConfig
metadata:
  name: pgbouncerconf
spec:
  pgbouncer.ini:
    default_pool_size: '200'
    max_client_conn: '200'
    pool_mode: 'transaction'
```
