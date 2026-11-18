---
title: Installation via Helm
weight: 2
url: /install/helm
aliases: [ /tutorial/stackgres-installation ]
description: Details about how to install the StackGres operator using Helm.
showToc: true
---

The StackGres operator can be installed using [Helm](https://helm.sh/) version >= `{{% helm-min-version %}}`.
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

> You can specify the version adding `--version <version>` to the Helm command. 

For more installation options, have a look at the [Operator Parameters]({{% relref "04-administration-guide/01-installation/02-installation-via-helm/01-operator-parameters" %}}) section.

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

Now we can continue with [creating a StackGres cluster]({{% relref "04-administration-guide/02-cluster-creation" %}}).

## Operator Architecture

The operator Helm chart creates the following components:

- A Deployment called `stackgres-operator` with 1 Pod in the `stackgres` namespace. This is the main operator component that manages all StackGres resources.
- Custom Resource Definitions (CRDs) that extend Kubernetes functionalities by providing custom resources like SGCluster to create Postgres clusters.
- Mutating and validating webhooks that provide functionalities like defaults and custom validations on the new custom resources.
- A Deployment called `stackgres-restapi` that provides the Web Console component, allowing you to interact with StackGres custom resources using a web interface.

When SGClusters are created with monitoring capabilities, a Deployment called `stackgres-collector` is created to collect metrics. The metrics are discarded if not sent to any metric storage. StackGres offers an integration with the Prometheus operator so that metrics can be collected by the Prometheus resource installed in your Kubernetes cluster.

## Upgrading the Operator

Upgrading the operator Helm chart is needed whenever any setting is changed or when you need to upgrade the operator version.

```
helm upgrade --namespace stackgres stackgres-operator stackgres-charts/stackgres-operator --version <version> -f values.yaml
```

> **Best Practice:** It is recommended to always fix the version in your `values.yaml` or installation command to ensure reproducible deployments.

For more information see the [upgrade section]({{% relref "04-administration-guide/16-upgrade" %}}).

## Configuration with Helmfile

For a more DevOps-oriented experience, the installation may be managed by tools like [Helmfile](https://github.com/helmfile/helmfile) that wraps the Helm CLI, allowing you to set even the command parameters as a configuration file. Helmfile also allows separating environments using a Go templating engine similar to the one used for Helm charts.

Example `helmfile.yaml`:

```yaml
environments:
  training:
---

repositories:
  - name: stackgres-charts
    url: https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/

releases:
- name: stackgres-operator
  namespace: stackgres
  version: 1.16.1
  chart: stackgres-charts/stackgres-operator
  # Helmfile allows to specify a set of environments and to bind a Helm chart
  # to a specific values.yaml file based on the environment name by using Go templating
  values:
    - values/stackgres-{{ .Environment.Name }}-values.yaml

# Helmfile allows to specify other Helm command options
helmDefaults:
  wait: true
  timeout: 120
  createNamespace: true
  cleanupOnFail: true
```

To apply and update the above configuration for the `training` environment:

```
helmfile -e training -f helmfile.yaml apply
```

## SGConfig Custom Resource

Helm chart values are (mostly) mapped to the SGConfig custom resource that is stored during the installation/upgrade of the Helm chart. For detailed configuration options, see the [SGConfig reference]({{% relref "06-crd-reference/12-sgconfig" %}}).

> **Tip:** Users of the operator should not create an SGConfig directly. Instead, modify it to change some of the configuration (configuration that cannot be changed by editing the SGConfig is specified in the documentation). In general, it is better to always use the Helm chart `values.yaml` to configure the operator in order for the changes to not be overwritten during upgrades.