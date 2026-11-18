---
title: Patroni Configuration
weight: 4
url: /administration/patroni/configuration
description: How to customize Patroni configuration in StackGres clusters.
showToc: true
---

[Patroni](https://patroni.readthedocs.io/en/latest/) is the high availability framework used by StackGres to manage PostgreSQL replication, automatic failover, and cluster topology. StackGres generates an optimized Patroni configuration automatically, but you can customize it through the `SGCluster.spec.configurations.patroni` section.

## Configuration Types

Patroni configuration in StackGres is split into two sections:

| Section | Description | Modifiable after creation |
|---------|-------------|--------------------------|
| `dynamicConfig` | Patroni [dynamic configuration](https://patroni.readthedocs.io/en/latest/dynamic_configuration.html) that is stored in the DCS (Kubernetes endpoints/configmaps). Changes are applied at runtime. | Yes |
| `initialConfig` | Patroni [YAML configuration](https://patroni.readthedocs.io/en/latest/yaml_configuration.html) that is set at bootstrap time. | No (creation only) |

## Dynamic Configuration

The `dynamicConfig` section allows you to override Patroni's dynamic configuration. This is useful for tuning failover behavior, timeouts, and adding custom `pg_hba` rules.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  configurations:
    patroni:
      dynamicConfig:
        ttl: 30
        loop_wait: 10
        retry_timeout: 10
        maximum_lag_on_failover: 1048576
        postgresql:
          pg_hba:
          - host all all 10.0.0.0/8 md5
          - host all all 172.16.0.0/12 md5
```

### Ignored Dynamic Configuration Fields

StackGres manages certain Patroni fields internally. The following fields in `dynamicConfig` are ignored:

- `synchronous_mode`
- `synchronous_mode_strict`
- `postgresql` (all sub-fields **except** `pg_hba`)
- `standby_cluster`

> The `pg_hba` section you provide will be appended with rules required for the cluster to function correctly. StackGres will not remove its own required rules.

### Common Dynamic Configuration Scenarios

**Adjusting failover timeouts:**

```yaml
dynamicConfig:
  ttl: 30
  loop_wait: 10
  retry_timeout: 10
  maximum_lag_on_failover: 1048576
```

- `ttl`: The TTL (in seconds) for the leader key. Default is 30.
- `loop_wait`: The number of seconds the main loop sleeps. Default is 10.
- `retry_timeout`: Timeout for DCS and PostgreSQL operation retries. Default is 10.
- `maximum_lag_on_failover`: Maximum WAL lag in bytes for a replica to be eligible for failover.

**Custom pg_hba rules:**

```yaml
dynamicConfig:
  postgresql:
    pg_hba:
    - host all all 10.0.0.0/8 md5
    - host replication replicator 10.0.0.0/8 md5
```

## Initial Configuration

The `initialConfig` section allows you to set Patroni configuration that is applied only at cluster creation time. This is useful for PostgreSQL callbacks and pre/post hooks.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  configurations:
    patroni:
      initialConfig:
        postgresql:
          callbacks:
            on_start: /bin/bash -c 'echo "Cluster started"'
            on_role_change: /bin/bash -c 'echo "Role changed to ${1}"'
          pre_promote: /bin/bash -c 'echo "About to promote"'
          before_stop: /bin/bash -c 'echo "Stopping"'
          pg_ctl_timeout: 120
```

> The `initialConfig` field can only be set at cluster creation time and cannot be modified afterwards.

### Ignored Initial Configuration Fields

The following fields in `initialConfig` are managed by StackGres and are ignored:

- `name`
- `namespace`
- `log`
- `bootstrap`
- `citus`
- `postgresql` (all sub-fields **except** `postgresql.callbacks`, `postgresql.pre_promote`, `postgresql.before_stop`, and `postgresql.pg_ctl_timeout`)
- `restapi`
- `ctl`
- `watchdog`
- `tags`

## Example: Full Patroni Customization

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: production-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  configurations:
    patroni:
      dynamicConfig:
        ttl: 30
        loop_wait: 10
        retry_timeout: 10
        maximum_lag_on_failover: 1048576
        postgresql:
          pg_hba:
          - host all all 10.0.0.0/8 md5
      initialConfig:
        postgresql:
          callbacks:
            on_role_change: /bin/bash -c 'echo "Role changed"'
          pg_ctl_timeout: 120
```

## Related Documentation

- [SGCluster CRD Reference]({{% relref "06-crd-reference/01-sgcluster" %}})
- [Patroni Dynamic Configuration](https://patroni.readthedocs.io/en/latest/dynamic_configuration.html)
- [Patroni YAML Configuration](https://patroni.readthedocs.io/en/latest/yaml_configuration.html)
- [Switchover]({{% relref "04-administration-guide/09-high-availability/01-switchover" %}})
- [Failover]({{% relref "04-administration-guide/09-high-availability/02-failover" %}})
