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

## Images Registry

By default the images of the Pods of a new SGCluster are retrieved from the StackGres images registry
 (`sgcr.dev`) through the StackGres docir REST API, that combines the base image, the Postgres flavor,
 the required addons (patroni, wal-g and hdrhistogram) and the extensions into a single image for the
 patroni container, and the base image, the Postgres flavor and a single addon (pgbouncer,
 postgres-exporter, kubectl, fluent-bit, fluentd or otel-collector) into the images of the sidecar containers.

The feature is controlled by `SGCluster.spec.configurations.registry.enabled` that can only be set on
 creation: it defaults to `true` for new SGClusters and to `false` for SGClusters created with a
 previous version of the operator, that keep using the images bundled with the operator release.
 The URL of the StackGres docir REST API is configured globally in `SGConfig.spec.repository.url`
 (helm value `repository.url`) and can be overridden for a single SGCluster with
 `SGCluster.spec.configurations.registry.url`.

The catalog of the registry (base images, Postgres versions, addons and extensions) is requested for
 the `<major>.<minor>` version of the operator (query parameter `operator-version`) so that only the
 images supporting it are used, and restricted to the published images (query parameter
 `published=true`). Set the environment variable `USE_PUBLISHED_IMAGES` to `false` in the operator
 (it is propagated to the REST API and to the cluster controller) to also use the images not yet
 published, for instance to test them before publication.

The images used by a SGCluster are pinned in its status (`status.base`, `status.baseVersion`,
 `status.baseRevision`, `status.revision`, `status.addons` and `status.repository`) and are only
 resolved again when a rollout is allowed (see [Rollout]({{% relref "04-administration-guide/11-rollout" %}}))
 or when the Postgres version changes. The addons of the sidecar containers are pinned only when
 available in the registry for the same base image (an addon that becomes available later is pinned
 without changing the other pins) and a sidecar container that requires an addon that is not available
 fails only if enabled.

## Custom Configuration

For creating your custom configuration, check out the following guides:

{{% children style="li" depth="1" description="true" %}}