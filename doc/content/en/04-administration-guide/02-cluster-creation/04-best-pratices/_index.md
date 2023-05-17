---
title: Best Practices
weight: 4
url: install/best-practices
aliases: [ /install/cluster/best-practices ]
description: Recommendations about how organize all configuration files.
showToc: true
---

A complete StackGres cluster configurations could be organized as a collections of files inside a directory, this files could be distributed according with the environments in your infrastructure.

This example shows a directory structure for two environments, production and staging. 

```
stackgres/
├── production
│   ├── cluster
│   │   ├── PodDisruptionBudget.yaml
│   │   ├── SGCluster.yaml
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
    │   └── SGCluster.yaml
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

The directory structure consists of 4 main sections per environment.

## **Cluster**

Mainly for the `SGCluster` manifest and other resources like Pod disruption budget and alerts definition specifically from that cluster. 

## **Configurations**

All manifest required by the `SGCluster` like postgres configurarion, pooling, instance profile, backups, logs and also `namespaces` and `StorageClasses`.

## **Maintenance jobs** 

All day-2 operations manifest. 

## **Operator**

StackGres operator installation manifest that can include any other operator installation required for for the k8s cluster like the prometheus operator.  


You can see the full manifests definition examples [here.](https://gitlab.com/ongresinc/stackgres/-/tree/main/stackgres-k8s/examples/full_example)
