---
title: "Upgrade via Helm"
weight: 5
url: install/helm/upgrade
description: Details about how to use helm to upgrade the operator.
showToc: true
---

## Upgrade Operator

Upgrade the operator with the following command:

```bash
helm upgrade --namespace stackgres stackgres-operator \
  --values my-operator-values.yml \
  stackgres-charts/stackgres-operator
```

>**Important note:** Do not use the `--reuse-values` option from Helm, this prevent the new operator resources and the new values needed to be set. Pass your installation params using the values file or setting the values directly in the command using `--set-string` option.

The main recommendation is to pass the same installation values in the upgrade command or using a values.yaml.

Upgrade of an operator can serve two purpose:

* Configuration change
* Operator upgrade

### Operator upgrade

After the upgrade completes any new cluster that will be created, will be created with the new
 updated components.
For existing clusters, there are two mechanisms in order to update components: in-place restart
 and reduced-impact restart. Both procedures are essentially the same but reduced-impact restart
 allow to restart a cluster with minimal throughput reduction for read-only connections (we will
 not apply draining here) or for read-write connections when a single node clusters is used.

For more details please see the [cluster restart section]({{% relref "05-administration-guide/20-cluster-restart" %}})