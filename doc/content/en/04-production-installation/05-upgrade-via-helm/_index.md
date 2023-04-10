---
title: "Upgrade via Helm"
weight: 5
url: install/helm/upgrade
description: Details how to use Helm to upgrade the operator.
showToc: true
---

This section shows how to upgrade the StackGres operator using Helm.

## Upgrading the StackGres Helm Repository

Upgrade the Helm repository:

```
helm repo update stackgres-charts
```

## Upgrading the StackGres Operator

Upgrade the StackGres operator:

```
helm upgrade --namespace stackgres stackgres-operator \
  --values my-operator-values.yml \
  stackgres-charts/stackgres-operator
```

Adapt the values to your specific namespace, values, and chart name.

> **Important:** Do not use the `--reuse-values` option of Helm since this prevents the operator Helm chart from adding new default values.
> Pass your installation params using the values file, or set the values directly in the command using the `--set-string` or `--set` options.

It's recommended to pass the same values or the same `value.yaml` file at upgrade time that have been used at installation time.

Upgrading an operator serves two purposes:

* Configuration change: to enable or disable features or to change any parameter of the current installation
* Operator upgrade: to upgrade to another version of the operator

### Upgrading Clusters

After the upgrade completes, any new cluster will be created with the updated components.
Existing clusters will work using the previous version of the operator.
They will be able to use some or all the features available in the new operator version and will receive bugfixes (this may require a [cluster restart]({{% relref "06-crd-reference/09-sgdbops#restart" %}})).

To make use of all the functionalities available in the updated operator version, a [cluster security upgrade]({{% relref "06-crd-reference/09-sgdbops#security-upgrade" %}}) has to be performed.
There are two methods to perform such a security upgrade: *in-place* and *reduced-impact*.
While both methods are similar in what they accomplish, they differ in the impact they have on the throughput.
The *in-place* upgrade restarts one pod at a time, and with this the total number of running pods is (roughly) constant at all times.
The *reduced-impact* update performs the update with one additional temporary pod during the duration of the update (*n+1*), so that the impact on throughput is reduced.

### Upgrading Clusters YAMLs

After the upgrade, the StackGres custom resource YAMLs may require some change.
Each release note describes these changes (see below).
In general, there is no need to perform any action in order to maintain compatibility with the previous version of StackGres custom resource YAMLs that are maintained outside the Kubernetes API (such as local files, Git, Helm, or ArgoCD).
If any unexpected behavior appears, please read the following sections that includes the changes provided by each operator release that affect the same version of StackGres custom resources and update the YAMLs stored outside the Kubernetes API accordingly.

> Also be aware of that an operator upgrade may modify the existing StackGres resources, such as adding new default sections, renaming a field, migrate from one section to another, or even create new resources.
> In some cases, the StackGres resources that are stored as YAML files and updated using `kubectl apply` may lead to errors if the resource updates weren't reflected in the YAML files, accordingly.

**1.3.0**

* stackgres.io/v1/SGCluster:
    * `.spec.configurations.sgBackupConfig` has been deprecated and is replaced by `.spec.configurations.backups[0].sgObjectStorage`.
    * `.spec.configurations.backupPath` has been deprecated and is replaced by `.spec.configurations.backups[0].path`.
    * `.spec.initialData.scripts` has been deprecated and is replaced by `.spec.managedSql` and stackgres.io/v1/SGScript.
* stackgres.io/v1/SGBackupConfig has been deprecated and is replaced by stackgres.io/v1beta1/SGObjectStorage and the section `.spec.configurations.backups[0]` under stackgres.io/v1/SGCluster
* stackgres.io/v1/SGInstanceProfile
    * `.spec.containers` and `.spec.initContainers` section with default memory and CPU resources requests and limits restrictions were added. Those resources requests and limits
     are applied by default to the cluster's Pods for new stackgres.io/v1/SGCluster. For existing stackgres.io/v1/SGCluster the field `.spec.nonProductionOptions.disableClusterResourceRequirements`
     is set to `true` in order to maintain the previous memory and CPU resources requests and limits restrictions that are only enforced on the patroni container.

**1.2.0**

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

**1.1.0**

* stackgres.io/v1/SGCluster:
    * `.spec.flavor` with default value of `vanilla`.
