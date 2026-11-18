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





<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr><tr>
      <td><b>apiVersion</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>stackgres.io/v1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGPoolingConfig</td>
      </tr>
      <tr>
      <td><b><a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta">metadata</a></b></td>
      <td>✓</td>
      <td>✓</td>
      <td></td>
      <td>object</td>
      <td>Refer to the Kubernetes API documentation for the fields of the `metadata` field.</td>
      </tr>
      <tr>
        <td><b><a href="#sgpoolingconfigspec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgpoolingconfigstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

## SGPoolingConfig.spec
<sup><sup>[↩ Parent](#sgpoolingconfig)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgpoolingconfigspecpgbouncer">pgBouncer</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Connection pooling configuration based on PgBouncer.
<br/>
        </td>
      </tr></tbody>
</table>

### SGPoolingConfig.spec.pgBouncer
<sup><sup>[↩ Parent](#sgpoolingconfigspec)</sup></sup>

Connection pooling configuration based on PgBouncer.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgpoolingconfigspecpgbouncerpgbouncerini">pgbouncer.ini</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The `pgbouncer.ini` parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.

Check [pgbouncer configuration](https://www.pgbouncer.org/config.html#generic-settings) for more information about supported parameters.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGPoolingConfig.spec.pgBouncer.pgbouncer.ini
<sup><sup>[↩ Parent](#sgpoolingconfigspecpgbouncer)</sup></sup>

The `pgbouncer.ini` parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.

Check [pgbouncer configuration](https://www.pgbouncer.org/config.html#generic-settings) for more information about supported parameters.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>databases</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]string</td>
        <td>
          The `pgbouncer.ini` (Section [databases]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.

Check [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-databases) for more information about supported parameters.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pgbouncer</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          The `pgbouncer.ini` (Section [pgbouncer]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.

Check [pgbouncer configuration](https://www.pgbouncer.org/config.html#generic-settings) for more information about supported parameters
<br/>
        </td>
      </tr>
      <tr>
        <td><b>users</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]string</td>
        <td>
          The `pgbouncer.ini` (Section [users]) parameters the configuration contains, represented as an object where the keys are valid names for the `pgbouncer.ini` configuration file parameters.

Check [pgbouncer configuration](https://www.pgbouncer.org/config.html#section-users) for more information about supported parameters.
<br/>
        </td>
      </tr></tbody>
</table>

## SGPoolingConfig.status
<sup><sup>[↩ Parent](#sgpoolingconfig)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgpoolingconfigstatuspgbouncer">pgBouncer</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Connection pooling configuration status based on PgBouncer.
<br/>
        </td>
      </tr></tbody>
</table>

### SGPoolingConfig.status.pgBouncer
<sup><sup>[↩ Parent](#sgpoolingconfigstatus)</sup></sup>

Connection pooling configuration status based on PgBouncer.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>defaultParameters</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          The `pgbouncer.ini` default parameters parameters which are used if not set.
<br/>
        </td>
      </tr></tbody>
</table>

## Default Values

These are the default values of the `pgbouncer.ini` property:

```yaml
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
