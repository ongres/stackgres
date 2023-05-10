---
title: Installation via Helm
weight: 2
url: install/helm
aliases: [ /tutorial/stackgres-installation ]
description: Details about how to install the StackGres operator using Helm.
showToc: true
---

The StackGres operator can be installed using [Helm](https://helm.sh/) version >= `3.1.1`.
As you may expect, a production environment will require you to install and set up additional components alongside your StackGres operator and cluster resources.

In this page, we are going through all the necessary steps to set up a production-grade StackGres environment using Helm.

## Set Up StackGres Helm Repository

Add the StackGres Helm repository:

```
helm repo add stackgres-charts https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/
```

## StackGres Operator Installation

Install the operator: 

```
helm install --create-namespace --namespace stackgres stackgres-operator stackgres-charts/stackgres-operator
```

> You can specify the version adding `--version <version, e.g. 1.0.0>` to the Helm command. 

For more installation options have a look at the [Operator Parameters]({{% relref "04-administration-guide/01-production-installation/02-installation-via-helm/01-operator-parameters" %}}) section for more information.

If you want to integrate Prometheus and Grafana into StackGres, please read the next section. 

### Installation With Monitoring

It's also possible to install the StackGres operator with an integration of an existing Prometheus/Grafana monitoring stack.

For this, the StackGres operator is pointed to the existing monitoring resources:

```
helm install --create-namespace --namespace stackgres stackgres-operator \
 --set grafana.autoEmbed=true \
 --set-string grafana.webHost=prometheus-grafana.monitoring \
 --set-string grafana.secretNamespace=monitoring \
 --set-string grafana.secretName=prometheus-grafana \
 --set-string grafana.secretUserKey=admin-user \
 --set-string grafana.secretPasswordKey=admin-password \
 stackgres-charts/stackgres-operator
```

> **Important:** This example only works if you already have a running monitoring setup (here running in namespace `monitoring`), otherwise the StackGres installation will fail.
> The example above is based on the official Prometheus/Grafana Helm chart.
> To install the full setup, have a look at the [Monitoring]({{% relref "04-administration-guide/06-monitoring" %}}) guide.

This example is based on the official Prometheus/Grafana Helm chart, which you can install following the instructions under [Monitoring]({{% relref "04-administration-guide/06-monitoring" %}}).

<!-- TODO what will this do?
The installation with monitoring will integrate the monitoring dashboards into ... webui? also cluster?
-->

## Waiting for Operator Startup

Use the following command to wait until the operator is ready to use:

```
kubectl wait -n stackgres deployment -l group=stackgres.io --for=condition=Available
```

Once it's ready, you will see that the operator pods are `Running`:

```
$ kubectl get pods -n stackgres -l group=stackgres.io
NAME                                  READY   STATUS    RESTARTS   AGE
stackgres-operator-78d57d4f55-pm8r2   1/1     Running   0          3m34s
stackgres-restapi-6ffd694fd5-hcpgp    2/2     Running   0          3m30s

```

Now we can continue with [creating a StackGres cluster]({{% relref "04-administration-guide/01-production-installation/03-creating-a-cluster" %}}).