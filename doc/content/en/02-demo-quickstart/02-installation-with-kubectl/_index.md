---
title: Installation with kubectl
weight: 2
---

# Installation with kubectl

We ship a kubernetes resources files in order to allow installation of the StackGres operator for demostration purpose.
Just set the environment variable ENV with your kubernetes environment from one of the following:

* simple
* prometheus
* [gke]({{< relref "en/02-demo-quickstart/01-setting-up-the-environment/01-gke/_index.md" >}})
* [eks]({{< relref "en/02-demo-quickstart/01-setting-up-the-environment/02-aws-eks/_index.md" >}})
* [aks]({{< relref "en/02-demo-quickstart/01-setting-up-the-environment/03-azure-containers/_index.md" >}})
* [yandex]({{< relref "en/02-demo-quickstart/01-setting-up-the-environment/04-yandex-cloud/_index.md" >}})

```
ENV=simple
kubectl create -f "https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/demo-${ENV}.yml"
```