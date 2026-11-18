---
title: SGPoolingConfig
weight: 4
url: /reference/crd/sgpoolingconfig
description: Details about SGPoolingConfig
shotToc: true
---

___

**Kind:** SGPoolingConfig

**listKind:** SGPoolingConfigList

**plural:** sgpoolconfigs

**singular:** sgpoolconfig

**shortNames** sgpoc
___

The `SGPoolingConfig` custom resource represents the configuration of the connection pooling, in particular of PgBouncer.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  name: pgbouncerconf
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        max_client_conn: '2000'
        default_pool_size: '50'
      databases:
        foodb:
          max_db_connections: 1000
          pool_size: 20
          dbname: 'bardb'
          reserve_pool: 5
      users:
        user1:
          pool_mode: transaction
          max_user_connections: 50
        user2:
          pool_mode: session
          max_user_connections: '100'
```

See also [Coonection Pooling Configuration section]({{%  relref "04-administration-guide/04-configuration/03-connection-pooling" %}}).

{{% include "generated/SGPoolingConfig.md" %}}

## Default Values

These are the default values of the `pgbouncer.ini` property:

```
  admin_users: "postgres",
  application_name_add_host: "1",
  auth_query: "SELECT usename, passwd FROM pg_shadow WHERE usename=$1",
  auth_type: "md5",
  auth_user: "authenticator",
  default_pool_size: "50",
  ignore_startup_parameters: "extra_float_digits",
  listen_addr: "127.0.0.1",
  max_client_conn: "200",
  max_db_connections: "0",
  max_user_connections: "0",
  pool_mode: "transaction",
  stats_users: "postgres"
```

## Parameter Denylist / Blocklist

To guarantee a functional PgBouncer configuration, most of the parameters specified in the [PgBouncer configuration documentation](https://www.pgbouncer.org/config.html#generic-settings) for section `pgbouncer` are not allowed to be changed.
If these are specified in the CR configuration, they will be ignored and the default values will be used instead.
The immutable PgBouncer parameters are:

| Immutable Parameters       |
|:---------------------------|
| admin_users                |
| application_name_add_host  |
| auth_file                  |
| auth_hba_file              |
| auth_query                 |
| auth_type                  |
| auth_user                  |
| client_tls_ca_file         |
| client_tls_cert_file       |
| client_tls_ciphers         |
| client_tls_dheparams       |
| client_tls_ecdhcurve       |
| client_tls_key_file        |
| client_tls_protocols       |
| client_tls_sslmode         |
| conffile                   |
| disable_pqexec             |
| dns_max_ttl                |
| dns_nxdomain_ttl           |
| dns_zone_check_period      |
| listen_addr                |
| listen_backlog             |
| listen_port                |
| logfile                    |
| pidfile                    |
| server_check_delay         |
| server_check_query         |
| server_fast_close          |
| server_lifetime            |
| server_round_robin         |
| server_tls_ca_file         |
| server_tls_cert_file       |
| server_tls_ciphers         |
| server_tls_key_file        |
| server_tls_protocols       |
| server_tls_sslmode         |
| stats_period               |
| stats_users                |
| syslog                     |
| syslog_facility            |
| syslog_ident               |
| tcp_defer_accept           |
| tcp_keepalive              |
| tcp_keepcnt                |
| tcp_keepidle               |
| tcp_keepintvl              |
| tcp_socket_buffer          |
| unix_socket_dir            |
| unix_socket_group          |
| unix_socket_mode           |
| user                       |
| verbose                    |
