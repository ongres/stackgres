---
title: "Upgrade via Helm"
weight: 1
url: /administration/upgrade/helm
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

After upgrading the operator have a look at the [following steps]({{% relref "16-upgrade#following-steps-after-operator-upgrade" %}}).
