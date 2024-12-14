---
title: Configuration
weight: 4
url: /administration/configuration
aliases: [ /administration/custom/postgres/config ]
description: Details about default and custom configurations.
showToc: true
---

StackGres clusters can be configured in various ways, targeting for example the PostgreSQL configuration, the sizing of the instances, or the connection pooling.
The configuration is made in Kubernetes resources such as `SGPostgresConfig`, `SGInstanceProfile`, or `SGPoolingConfig`.

## Default Configuration

The StackGres operator creates default configurations in the same namespace as the cluster, if no custom configuration has been specified.
These created configuration resources are independent of other clusters configurations, and can be modified by the user without affecting other clusters.

The operator will merge the fields of a custom configuration resource with its default configuration.

If an SGCluster is created without specifying custom PostgreSQL configuration (`SGPostgresConfig`), PgBouncer configuration (`SGPoolingConfig`) or resource profile configuration (`SGInstanceProfile`), the operator applies the default configuration.

Here is the list of default configuration resources that will be created in the same namespace as the cluster:

| Name                                                          | Kind                |
|:--------------------------------------------------------------|:--------------------|
| `postgres-<major-version>-generated-from-default-<timestamp>` | `SGPostgresConfig`  |
| `generated-from-default-<timestamp>`                          | `SGInstanceProfile` |
| `generated-from-default-<timestamp>`                          | `SGPoolingConfig`   |

You can query the default configuration as follows (for a cluster created in `demo`):

```
kubectl -n demo describe sgpgconfig
kubectl -n demo describe sginstanceprofile
kubectl -n demo describe sgpoolconfig
```

This is an excerpt from the `SGPostgresConfig` default configuration:

```
kubectl -n demo describe sgpgconfig
Name:         postgres-14-generated-from-default-1681459078209
Namespace:    demo
Labels:       <none>
Annotations:  stackgres.io/operatorVersion: 1.5.0-SNAPSHOT
API Version:  stackgres.io/v1
Kind:         SGPostgresConfig
Metadata:
  Creation Timestamp:  2023-04-14T07:57:58Z
  Generation:          1
  Managed Fields:
    API Version:  stackgres.io/v1
    Fields Type:  FieldsV1
[...]
Spec:
  Postgres Version:  14
  postgresql.conf:
    autovacuum_max_workers:            3
    autovacuum_vacuum_cost_delay:      2
    autovacuum_work_mem:               512MB
[...]
Status:
  Default Parameters:
    archive_command:                   /bin/true
    archive_mode:                      on
    autovacuum_max_workers:            3
    autovacuum_vacuum_cost_delay:      2
    autovacuum_work_mem:               512MB
    checkpoint_completion_target:      0.9
    checkpoint_timeout:                15min
[...]
```

## Cluster Profile

By default an SGCluster is created with the `production` profile that will enforce some best practices suited for a production environment.

This may be inconvenient in a testing or development environment. In such case you may chose any of the other existing profile.

The available profiles are:

* `production`: enforces resources requests and limits and add a Pod anti-affinity rules to prevent a Postgres instance to run in the same Kubernetes node.
* `testing`: enforces resources limits but disables resources requests and removes the Pod anti-affinity rules that prevents a Postgres instance to run in the same Kubernetes node.
* `development`: disables resources requests and limits and removes the Pod anti-affinity rules that prevents a Postgres instance to run in the same Kubernetes node.

The above rules may still be affected by other configurations. Details will can be found in the [CRD reference about SGCluster profile]({{% relref "06-crd-reference/01-sgcluster#sgclusterspec" %}}).

## Custom Configuration

For creating your custom configuration, check out the following guides:

{{% children style="li" depth="1" description="true" %}}