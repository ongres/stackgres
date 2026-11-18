---
title: Sharded Cluster Best Practices
weight: 4
url: /administration/sharded-cluster/creation/best-practices
description: Recommendations about how to organize all configuration files.
showToc: true
---

A complete StackGres cluster configuration should be organized as a collection of files inside a directory.
These files can be organized according to your infrastructure's environments.

This example shows a directory structure for two environments, production and staging. 

```
stackgres/
├── production
│   ├── cluster
│   │   ├── PodDisruptionBudget.yaml
│   │   ├── SGShardedCluster.yaml
│   │   └── StackGres-alerts.yaml
│   ├── configurations
│   │   ├── 01-CreateNameSpaces.yaml
│   │   ├── 02-StorageClass.yaml
│   │   ├── 03-SGInstanceProfile.yaml
│   │   ├── 04-SGPostgresConfig.yaml
│   │   ├── 05-SGPoolingConfig.yaml
│   │   ├── 06-SGDistributedLogs.yaml
│   │   └── 07-SGObjectStorage.yaml
│   ├── maintenance-jobs
│   │   └── maintenance-vacuum-freeze.yaml
│   └── operator
│       └── helmfile.yaml
└── staging
    ├── cluster
    │   └── SGShardedCluster.yaml
    ├── configurations
    │   ├── 01-CreateNameSpaces.yaml
    │   ├── 02-StorageClass.yaml
    │   ├── 03-SGInstanceProfile.yaml
    │   ├── 04-SGPostgresConfig.yaml
    │   ├── 05-SGPoolingConfig.yaml
    │   ├── 06-SGDistributedLogs.yaml
    │   └── 07-SGObjectStorage.yaml
    └── operator
        └── helmfile.yaml
```

The directory structure consists of four sections, or directories, per environment.

## Cluster

This directory is mainly for the `SGShardedCluster` manifest and other cluster-related resources such as pod disruption budgets and alert definitions.

## Configurations

This directory contains configuration required by the `SGShardedCluster`, such as Postgres configuration, pooling, instance profiles, backups, and distributed logs, but also Kubernetes namespaces or storage classes.
The YAML files in this directory will be applied before the cluster resources.
The file name numbering helps ensure that the resources are created in the correct order.

## Maintenance jobs 

This directory contains all day-2 operation manifests. 

## Operator

This directory contains StackGres operator installation manifests that can include any other required operator installation configuration, for example the Prometheus operator definitions.

You can see full manifest definition examples [here](https://gitlab.com/ongresinc/stackgres/-/tree/main/stackgres-k8s/examples/full_example).
