---
title: Configuration
weight: 4
url: /administration/configuration
aliases: [ /administration/custom/postgres/config ]
description: Details about default and custom configurations.
showToc: true
---

StackGres clusters can be configured in various ways. In this section we will detail how to configure the following aspects:

* the resources requirements of the Pods
* the PostgreSQL configuration
* the connection pooling (PgBouncer) configuration
* the cluster profile configuration

The first three configurations listed above (i.e. excluding the profile configuration) are defined in Kubernetes as custom resource definitions (CRDs): `SGInstanceProfile`, `SGPostgresConfig`, and `SGPoolingConfig`. The cluster profile configuration is defined as the `SGCluster.spec.profile` field.

## Cluster Profile

By default an SGCluster is created with the `production` profile that will enforce some best practices suited for a production environment.

Since the `production` profile enforces strict rules that may be inconvenient in a testing or development environment, you may choose any of the other existing profiles.

The available profiles are:

* `production`: enforces resources requests and limits and add Pod anti-affinity rules to prevent a Postgres instance to run in the same Kubernetes node.
* `testing`: enforces resources limits but disables resources requests and removes the Pod anti-affinity rules that prevent Postgres instances from running in the same Kubernetes node.
* `development`: disables resources requests and limits and removes the Pod anti-affinity rules that prevent Postgres instances from running in the same Kubernetes node.

The above rules may still be affected by other configurations. Details can be found in the [CRD reference about SGCluster profile]({{% relref "06-crd-reference/01-sgcluster#sgclusterspec" %}}).

## Default Configuration

The StackGres operator creates default configurations in the same namespace as the cluster, if no custom configuration has been specified using a unique name.

The operator will merge the fields of a custom configuration resource with its default configuration.

Here is the list of default configuration resources that will be created in the same namespace as the cluster:

| Name                                                                  | Kind                  |
|:----------------------------------------------------------------------|:----------------------|
| `postgres-<major-version>-generated-from-default-<timestamp>` | `SGPostgresConfig`  |
| `generated-from-default-<timestamp>`                             | `SGInstanceProfile` |
| `generated-from-default-<timestamp>`                             | `SGPoolingConfig`   |

You can query the default configuration as follows:

```
kubectl describe sginstanceprofile
kubectl describe sgpgconfig
kubectl describe sgpoolconfig
```

## Custom Configuration

For creating your custom configuration, check out the following guides:

{{% children style="li" depth="1" description="true" %}}