---
title: Connection pooling configuration
weight: 3
---

The connection pooling CR represent the configuration of PgBouncer.

___

**Kind:** SGPoolingConfig

**listKind:** SGPoolingConfigList

**plural:** sgpoolconfigs

**singular:** sgpoolconfig
___

**Spec**

| Property      | Required | Updatable | Type    | Default   | Description |
|:--------------|----------|-----------|:--------|:----------|:------------|
| pgbouncer.ini |          | ✓         | object  | see below | Section [pgbouncer] of pgbouncer.ini configuration |


Default value of `pgbouncer.ini` property:

```yaml
pool_mode: session
max_client_conn: "1000"
```

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGPoolingConfig
metadata:
  name: pgbouncerconf
spec:
  pgbouncer.ini:
    pool_mode: 'transaction'
    max_client_conn: '2000'
```

To guarantee a functional pgbouncer configuration most of the parameters specified in
 [pgbouncer configuration documentation](https://www.pgbouncer.org/config.html#generic-settings)
 for section `[pgbouncer]` have been blacklisted and will be ignored. The only parameters that can
 be changed are:

| Whitelisted parameter |
|:----------------------|
| pool_mode             |
| max_client_conn       |

For reference this is the list of parameters that will be ignored:

| Blacklisted parameter     |
|:--------------------------|
| logfile                   |
| pidfile                   |
| listen_addr               |
| listen_port               |
| unix_socket_dir           |
| unix_socket_mode          |
| unix_socket_group         |
| user                      |
| auth_file                 |
| auth_hba_file             |
| auth_type                 |
| auth_query                |
| auth_user                 |
| default_pool_size         |
| min_pool_size             |
| reserve_pool_size         |
| reserve_pool_timeout      |
| max_db_connections        |
| max_user_connections      |
| server_round_robin        |
| ignore_startup_parameters |
| disable_pqexec            |
| application_name_add_host |
| conffile                  |
| service_name              |
| job_name                  |
| stats_period              |
| syslog                    |
| syslog_ident              |
| syslog_facility           |
| log_connections           |
| log_disconnections        |
| log_pooler_errors         |
| log_stats                 |
| verbose                   |
| admin_users               |
| stats_users               |
| server_check_delay        |
| server_check_query        |
| server_fast_close         |
| server_lifetime           |
| server_idle_timeout       |
| server_connect_timeout    |
| server_login_retry        |
| client_login_timeout      |
| autodb_idle_timeout       |
| dns_max_ttl               |
| dns_nxdomain_ttl          |
| dns_zone_check_period     |
| client_tls_sslmode        |
| client_tls_key_file       |
| client_tls_cert_file      |
| client_tls_ca_file        |
| client_tls_protocols      |
| client_tls_ciphers        |
| client_tls_ecdhcurve      |
| client_tls_dheparams      |
| server_tls_sslmode        |
| server_tls_ca_file        |
| server_tls_key_file       |
| server_tls_cert_file      |
| server_tls_protocols      |
| server_tls_ciphers        |
| query_timeout             |
| query_wait_timeout        |
| client_idle_timeout       |
| idle_transaction_timeout  |
| pkt_buf                   |
| max_packet_size           |
| listen_backlog            |
| sbuf_loopcnt              |
| suspend_timeout           |
| tcp_defer_accept          |
| tcp_socket_buffer         |
| tcp_keepalive             |
| tcp_keepcnt               |
| tcp_keepidle              |
| tcp_keepintvl             |

