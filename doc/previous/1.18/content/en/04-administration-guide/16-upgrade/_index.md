---
title: "Upgrade"
weight: 16
url: /administration/helm/upgrade
aliases: [ /install/helm/upgrade, /administration/helm/upgrade ]
description: Details how to use Helm to upgrade the operator.
showToc: true
---

This section shows how to upgrade the StackGres operator.

## Operator upgrade

StackGres recommended upgrade is performed from the published Helm chart or through OperatorHub (this second option is only available if [OLM](https://olm.operatorframework.io/) is installed).

{{% children style="li" depth="1" description="true" %}}

## Following steps after operator upgrade 

### Upgrading Resources

After the upgrade completes, any new SGCluster, SGShardedCluster or SGDistributedLogs will be created with the updated components.

Existing SGClusters, SGShardedClusters and SGDistributedLogs will work using the previous version of the operator.

In order for the existing SGClusters, SGShardedClusters and SGDistributedLogs to make use of all the new functionalities and bugfixes available in the updated operator version, an [SGDbOps security upgrade]({{% relref "06-crd-reference/08-sgdbops#security-upgrade" %}}) for all existing SGCluster and SGDistributedLogs or an [SGShardedDbOps security upgrade]({{% relref "06-crd-reference/14-sgshardeddbops#security-upgrade" %}}) for all existing SGShardedCluster has to be performed. Since this operation require restarting the primary Postgres instance it should be performed during a maintenance window.

The security upgrade operation will change the version of the SGCluster, SGShardedCluster or SGDistributedLogs resources to the latest one and will perform a restart of the all the Pods by re-creating them.
There are two methods to perform such a security upgrade: *InPlace* and *ReducedImpact*. While both methods are similar in what they accomplish, they differ in the impact they have on the throughput of the read-only connections.
The *InPlace* method restarts one pod at a time without increasing the total number of running Pods.
The *ReducedImpact* method creates one additional temporary Pod during operation (*n + 1*), so that the impact on read-only throughput is reduced.

### Upgrade custom resources for external YAMLs

After the upgrade, the YAMLs files of StackGres custom resources that are hosted externally (such as local files, Git, Helm) of the Kubernetes cluster may require some changes.
Please read carefully the following sections that includes the changes provided by each operator release that affect the same minor version of StackGres custom resources and update the YAMLs stored outside the Kubernetes API accordingly.

> **NOTE**: the operator upgrades automatically the StackGres custom resources that are created into the Kubernetes cluster, such as adding new default sections, renaming a field, migrate from one section to another, or even create new resources.
> In some cases, the StackGres resources that are stored as YAML files and updated using `kubectl apply` may lead to errors if the resource updates weren't reflected in the YAML files, accordingly. This behavior is mitigated if the version of kubectl and Kubernetes you are using supports [server-side apply](https://kubernetes.io/docs/reference/using-api/server-side-apply/).

**1.15**

* stackgres.io/v1/SGDistributedLogs:
    * `.spec.metadata.annotations.pods` has been replaced by `.spec.metadata.annotations.clusterPods`
* stackgres.io/v1/SGScript:
    * `.spec.scripts` array now requires a unique `id` field to be specified when using server-side apply to update the custom resource.

**1.14**

* stackgres.io/v1/SGConfig:
    * `.spec.prometheus.allowAutobind` has been replaced by `.spec.collector.prometheusOperator.allowDiscovery`.
* stackgres.io/v1/SGCluster:
    * `.spec.prometheusAutobind` has been replaced by `.spec.configurations.observability.prometheusAutobind`.
* stackgres.io/v1alpha1/SGShardedCluster:
    * `.spec.prometheusAutobind` has been replaced by `.spec.configurations.observability.prometheusAutobind`.

**1.3**

* stackgres.io/v1/SGCluster:
    * `.spec.configurations.sgBackupConfig` has been deprecated and is replaced by `.spec.configurations.backups[0].sgObjectStorage`.
    * `.spec.configurations.backupPath` has been deprecated and is replaced by `.spec.configurations.backups[0].path`.
    * `.spec.initialData.scripts` has been deprecated and is replaced by `.spec.managedSql` and stackgres.io/v1/SGScript.
* stackgres.io/v1/SGBackupConfig has been deprecated and is replaced by stackgres.io/v1beta1/SGObjectStorage and the section `.spec.configurations.backups[0]` under stackgres.io/v1/SGCluster
* stackgres.io/v1/SGInstanceProfile
    * `.spec.containers` and `.spec.initContainers` section with default memory and CPU resources requests and limits restrictions were added. Those resources requests and limits
     are applied by default to the cluster's Pods for new stackgres.io/v1/SGCluster. For existing stackgres.io/v1/SGCluster the field `.spec.nonProductionOptions.disableClusterResourceRequirements`
     is set to `true` in order to maintain the previous memory and CPU resources requests and limits restrictions that are only enforced on the patroni container.

**1.2**

* stackgres.io/v1/SGBackup
    * `.status.sgBackupConfig.baseBackups.performance.maxDiskBandwitdh` has been deprecated and is replaced by `.status.sgBackupConfig.baseBackups.performance.maxDiskBandwidth`
    * `.status.sgBackupConfig.baseBackups.performance.maxNetworkBandwitdh` has been deprecated and is replaced by `.status.sgBackupConfig.baseBackups.performance.maxNetworkBandwidth`
* stackgres.io/v1/SGBackupConfig
    * `.spec.baseBackups.performance.maxDiskBandwitdh` has been deprecated and is replaced by `.spec.baseBackups.performance.maxDiskBandwidth`
    * `.spec.baseBackups.performance.maxNetworkBandwitdh` has been deprecated and is replaced by `.spec.baseBackups.performance.maxNetworkBandwidth`
* stackgres.io/v1/SGCluster:
    * `.status.labelPrefix` with default value of an empty string.
* stackgres.io/v1/SGDistributedLogs:
    * `.status.labelPrefix` with default value of an empty string.

**1.1**

* stackgres.io/v1/SGCluster:
    * `.spec.flavor` with default value of `vanilla`.
