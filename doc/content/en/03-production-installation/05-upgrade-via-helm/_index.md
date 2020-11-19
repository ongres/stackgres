---
title: "Upgrade via Helm"
weight: 5
url: install/helm/upgrade
---

## Upgrade Operator

Upgrade the operator with the following command:

```bash
helm upgrade --namespace stackgres stackgres-operator \
  --values my-operator-values.yml \
  {{< download-url >}}/helm/stackgres-operator.tgz
```

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

For more details please see the [cluster restart section]({{% relref "03-production-installation/04-cluster-restart" %}})