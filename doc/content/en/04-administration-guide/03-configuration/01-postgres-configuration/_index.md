---
title: Postgres Configuration
weight: 1
url: administration/configuration/postgres
aliases: [ administration/custom/postgres/config ]
description: Details about how the PostgresSQL configuration works and how to customize it.
---

The PostgreSQL configuration is specified in the [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) CRD.
If no custom configuration is specified at cluster creation, StackGres will create a default configuration, which you can see [here]({{% relref "04-administration-guide/03-configuration" %}}).

StackGres already ships with an expertly tuned Postgres configuration (aka `postgresql.conf`) by default.
However, it's absolutely possible to specify your own configuration.
If you need guidance regarding configuration, consider using the [postgresqlCONF](https://postgresqlco.nf) service, which gives you detailed parameter information in several langauges, recommendations, a tuning guide, and even a facility to store and manage your Postgres configurations online.

The [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) CRD allows you to specify and manage your Postgres configurations.
A Postgres configurations can be either created (and/or modified) per cluster, or reused in multiple clusters.
There's no need to repeat the configuration in every cluster.

The `SGPostgresConfig` is referenced from one or more Postgres clusters.

This is an example config definition:

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  namespace: demo
  name: pgconfig1
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

This definition is created in Kubernetes (e.g. using `kubectl apply`) and can be inspected (`kubectl describe sgpgconfig pgconfig1`) like any other Kubernetes resource.

StackGres clusters can reference this configuration as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: demo
  name: cluster
spec:
# [...]
  configurations:
    sgPostgresConfig: 'pgconfig1'
```