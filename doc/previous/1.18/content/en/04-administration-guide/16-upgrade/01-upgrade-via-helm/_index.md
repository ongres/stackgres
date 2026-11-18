---
title: "Upgrade via Helm"
weight: 1
url: /administration/upgrade/helm
description: Details how to use Helm to upgrade the operator.
showToc: true
---

This section shows how to upgrade the StackGres operator using Helm.

## Pre-upgrade Checks

Before proceeding with any upgrade, perform the following checks:

### Verify No Pending Upgrades

All SGClusters and SGShardedClusters must have been updated to the latest version with security upgrade SGDbOps or SGShardedDbOps:

```shell
kubectl get sgcluster -A -o json \
  | jq -r '.items[]|.metadata.namespace + " " + .metadata.name' \
  | while read NAMESPACE NAME
    do
      echo "$NAMESPACE"
      kubectl wait --timeout 0 -n "$NAMESPACE" sgcluster/"$NAME" \
        --for=condition=PendingUpgrade=false
    done
```

### Version Compatibility

The new version must be maximum 2 minor versions newer than the installed version. If that is not the case, upgrade hopping is required.

> **Example:** To upgrade from version 1.12.0 to 1.16.1, first upgrade from version 1.12.0 to version 1.14.3, and then to version 1.16.1.

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

After upgrading the operator have a look at the [following steps]({{% relref "16-upgrade#following-steps-after-operator-upgrade" %}}).

## Upgrade Process Details

When you upgrade the operator Helm chart, the following happens:

1. The SGConfig CRD is updated first since the operator may require new default values shipped with the new version
2. The operator image is upgraded and all other CRDs are updated
3. All existing custom resources are patched to add any defaults introduced in the new version

**Important:** The upgrade process does NOT touch any running SGCluster's Pods to avoid any service disruption. Users must then proceed to update all existing SGClusters by creating an SGDbOps security upgrade operation.

## Security Upgrade After Operator Upgrade

After upgrading the operator, you need to perform a security upgrade on each SGCluster to enable new functionalities and apply bugfixes:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: cluster-security-upgrade
  namespace: my-namespace
spec:
  sgCluster: my-cluster
  op: securityUpgrade
  securityUpgrade:
    method: InPlace
```

For SGShardedClusters, use SGShardedDbOps instead:

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: sharded-security-upgrade
  namespace: my-namespace
spec:
  sgShardedCluster: my-sharded-cluster
  op: securityUpgrade
  securityUpgrade:
    method: InPlace
```

The security upgrade operation is similar to a restart operation but ensures the SGCluster's Pods are brought to the latest version, effectively enabling any new functionality that requires Pod upgrades.
