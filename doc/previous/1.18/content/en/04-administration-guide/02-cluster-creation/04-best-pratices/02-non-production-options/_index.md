---
title: Cluster Non-Production Options
weight: 2
url: /administration/cluster-creation/nonproduction
aliases: [ /install/prerequisites/nonproduction, /install/helm/nonproduction ]
description: Important notes for non-production options in the production environment.
---

There are certain [non-production options]({{% relref "06-crd-reference/01-sgcluster" %}}#non-production-options) supported in StackGres, that we recommend to disable in a production environment.
To disable all of these options, create a YAML values file to include in the helm installation (`-f` or `--values` parameters) of the StackGres operator similar to the following:

```yaml
nonProductionOptions: {}
```

For reference, you can see a list of all of these [non-production options]({{% relref "06-crd-reference/01-sgcluster" %}}#non-production-options).

## Scaling with limited resources

By default StackGres enforces some rules and resource requirements and limitations in order to be production ready by default. In case you are testing StackGres functionality it is possible to configure StackGres so that it does not prevent Pods from being scheduled in a Kubernetes cluster with insufficient resources.

Normally StackGres requires each Pod of a Postgres cluster to be scheduled on a separate node using a Pod anti affinity rule. To disable such rule you may set the following options:

```yaml
spec:
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
```

Another aspect that may prevent Postgres cluster Pods from being scheduled are the resource requests and limits requirements. To disable such requirements you may set the following options:

```yaml
spec:
  nonProductionOptions:
    disablePatroniResourceRequirements: true
    disableClusterResourceRequirements: true
```

After setting those options you will have to restart the SGCluster's Pods by running a [restart SGDbOps]({{% relref "06-crd-reference/08-sgdbops#restart" %}}):

```bash
kubectl delete pod -l app=StackGresCluster,stackgres.io/cluster-name=simple
```
