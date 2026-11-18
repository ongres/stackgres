---
title: Installation via Helm
weight: 2
url: /install/helm
aliases: [ /tutorial/stackgres-installation ]
description: Details about how to install the StackGres operator using Helm.
showToc: true
---

The StackGres operator can be installed using [Helm](https://helm.sh/) version >= `3.1.1`.
As you may expect, a production environment will require you to install and set up additional components alongside your StackGres operator and cluster resources.

On this page, we are going through all the necessary steps to set up a production-grade StackGres environment using Helm.

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

For more installation options have a look at the [Operator Parameters]({{% relref "04-administration-guide/01-stackgres-installation/02-installation-via-helm/01-operator-parameters" %}}) section for more information.

If you want to integrate Prometheus and Grafana into StackGres, please read the next section. 

### Installation With Monitoring

It's also possible to install the StackGres operator with an integration of an existing Prometheus/Grafana monitoring stack.
For this, it's required to have a Prometheus/Grafana stack already installed on your cluster.
The following examples use the [Kube Prometheus Stack](https://github.com/prometheus-community/helm-charts/blob/main/charts/kube-prometheus-stack/).

To install StackGres with monitoring, the StackGres operator is pointed to the existing monitoring resources:

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

The example above is based on the Kube Prometheus Stack Helm chart.
To install the full setup, run the following installation commands *before* you install StackGres, or have a look at the [Monitoring]({{% relref "04-administration-guide/08-monitoring" %}}) guide.

```
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://charts.helm.sh/stable
helm repo update

helm install --create-namespace --namespace monitoring \
 --set grafana.enabled=true \
 prometheus prometheus-community/kube-prometheus-stack
```

The [Monitoring]({{% relref "04-administration-guide/08-monitoring" %}}) guide explains this in greater detail.

## Waiting for Operator Startup

Use the following command to wait until the StackGres operator is ready to use:

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

Now we can continue with [creating a StackGres cluster]({{% relref "04-administration-guide/03-cluster-creation" %}}).