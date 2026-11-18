---
title: Postgres Configuration
weight: 2
url: /administration/configuration/postgres
aliases: [ /administration/custom/postgres/config , /tutorial/complete-cluster/postgres-config ]
description: Details about how the PostgreSQL configuration works and how to customize it.
---

The [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) CRD allows you to specify and manage your Postgres configurations.

If no custom configuration is specified at cluster creation, StackGres will create a default SGPostgresConfig
 that will use the default configuration, which you can see in the [default values table](#default-values).
 This default configuration will also be used for those parameters that are not specified during the
 creation or modification of the resource.

Some of the configuration's parameters are part of a blocklist and specifying them will not be possible and
 will result in an error during the creation or modification of the resource. For the complete list of those
 parameters see the [blocked parameters table](#blocked-parameters).

A Postgres configurations can be either created (and/or modified) per cluster, or reused in multiple clusters.
There's no need to repeat the configuration in every cluster if they share the same exact configuration.

The `SGPostgresConfig` is referenced from one or more Postgres clusters.

This is an example config definition:

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  name: pgconfig
spec:
  postgresVersion: "14"
  postgresql.conf:
    work_mem: '16MB'
    shared_buffers: '2GB'
    random_page_cost: '1.5'
    password_encryption: 'scram-sha-256'
    log_checkpoints: 'on'
    jit: 'off'
```

This definition is created in Kubernetes (e.g. using `kubectl apply`) and can be inspected (`kubectl describe sgpgconfig pgconfig`) like any other Kubernetes resource.

An SGCluster can reference this configuration as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
# [...]
  configurations:
    sgPostgresConfig: 'pgconfig'
```

StackGres already ships with an expertly tuned Postgres configuration (aka `postgresql.conf`) by default.
However, it's absolutely possible to specify your own configuration.
If you need guidance regarding configuration, consider using the [postgresqlCONF](https://postgresqlco.nf) service, which gives you detailed parameter information in several languages, recommendations, a tuning guide, and even a facility to store and manage your Postgres configurations online.

## Apply Configuration changes

Each configuration, once applied, is automatically _reloaded_.

## Blocked Parameters

The list of blocked Postgres parameters:

{{% postgresql-blocklist %}}

## Default Values

The default Postgres parameters (when not specified):

{{% postgresql-default-values %}}
