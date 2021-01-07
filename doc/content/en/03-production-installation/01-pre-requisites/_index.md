---
title: Pre-requisites
weight: 1
url: install/prerequisites
---

## Environment

As explained in the [Demo section]({{% relref "02-demo-quickstart/01-setting-up-the-environment" %}}), for setting up the Operator and StackGres Cluster, you need to have an
environment on top of which it needs to request the necessary resources.

StackGres is able to run on any Kubernetes installation from 1.11 to 1.17 version, to maintain support for some version, please follow up the open discussion at" [#666](https://gitlab.com/ongresinc/stackgres/-/issues/666).

## Data Storage

When setting up a K8s environment the Storage Class by default is created with one main restriction and this is represented with the parameter `allowVolumeExpansion: false` this will not allow you to expand your disk when these are filling up. It is recommended to create a new Storage Class with at least these next parameters:

- `reclaimPolicy: Retain`
- `volumeBindingMode: WaitForFirstConsumer`
- `allowVolumeExpansion: true`

Here is an example working in a AWS environment:

```
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: io1
provisioner: kubernetes.io/aws-ebs
parameters:
  type: io1
  iopsPerGB: "50"
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

and if you're using GKE:

```
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ssd
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-ssd
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

Check the [Storage Class documentation](https://kubernetes.io/docs/concepts/storage/storage-classes/) for more details and other providers.

Do not forget using your custom Storage Class when you create a cluster, check the required parameters in [Cluster Parameters]({{% relref "03-production-installation/06-cluster-parameters/#pods" %}})

**Important note:**
Make sure you include these parameters in order to avoid some of the next errors:

- Autoscaler not working as expected:
`cluster-autoscaler  pod didn't trigger scale-up (it wouldn't fit if a new node is added)`

- Volumes not assigned:
`N node(s) had no available volume zone`

- Losing data by accidentally removing a volume:
`reclaimPolicy: Retain` will guarantee the volume is not delete when a claim no longer exist.

## Backups

All the configuration for this matter can be found at [Backup Configuration documentation]({{% relref "05-crd-reference/02-backups/#configuration" %}}). By default, backups are scheduled daily (`config.backup.fullSchedule`) at `05:00 UTC` and with a retention policy (`config.backup.retention`) of 5 full-backups removed on rotation. You will have to find out the correct time window and retention policy that fit your needs.

In the next section, you'll be able to see how to done this [via Helm]({{% relref "03-production-installation/02-installation-via-helm" %}}), with more explicit examples.

### Storage

StackGres support Backups with the following storage options:

* AWS S3
* Google CLoud Storage
* Azure Blob Storage


> Examples are using [MinIO](https://min.io/) service as a S3 compatible service for
> quick setups on local Kubernetes Cluster. Although, for production setups, StackGres Team recommends
> emphatically to pick a Storage as a Service for this purpose.

All the related configuration for the storage, is under `configurations.backupconfig.storage` section in your [Stackgres Cluster configuration file](https://gitlab.com/ongresinc/stackgres/-/blob/development/stackgres-k8s/install/helm/stackgres-cluster/values.yaml#L100-148).

```yaml
configurations:
  backupconfig:
    # fill the preferred storage method with
    # specific credentials and configurations
    storage:
      s3: {}
      s3Compatible: {}
      gcs: {}
      azureBlob: {}
```

To extend the CRD for the backups, all the reference can be found at [CRD Reference Documentation]({{% relref "05-crd-reference/02-backups" %}}).

### Restore

StackGres can perform a database restoration from a StackGres backup by just setting the UID of
 the backup CR that represents the backup that we want to restore. Like this:

``` yaml
cluster:
  initialData:
    restore:
      fromBackup: #the backup UID to restore
```

## Monitoring

As early indicated in [Component of the Stack]({{% relref "01-introduction/04-components-of-the-stack/#monitoring" %}}), StackGres at the moment supports Prometheus integration only.


### Installing Community Prometheus Stack

If the user is willing to install a full Prometheus Stack (State Metrics, Node Exporter and Grafana), there is a community chart that provides this at [kube-prometheus-stack installation instructions](https://github.com/prometheus-community/helm-charts/blob/main/charts/kube-prometheus-stack/README.md).


First, add the Prometheus Community repositories:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://charts.helm.sh/stable
helm repo update
```

Create the `monitoring` namespace:

```bash
kubectl create namespace monitoring 
```

You'll need to update your CRDs for this operator or create those if this is the first time creating it. Example CRDs are:

```bash
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_alertmanagers.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_podmonitors.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_probes.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_prometheuses.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_prometheusrules.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_servicemonitors.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_thanosrulers.yaml
```


Install the [Prometheus Server Operator](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus):

```bash
helm install --namespace monitoring prometheus prometheus-community/kube-prometheus-stack --set grafana.enabled=true
```

> StackGres provides more and advanced options for monitoring installation, see [Create a more advanced cluster]({{% relref "04-administration-guide/07-create-a-more-advanced-cluster" %}}) in the [Administration Guide]({{% relref "04-administration-guide" %}}).


Once the operator is installed, take note of the generated secrets as you they are need to be specified at StackGres operator installation. By default is `user=admin` and `password=prom-operator`:

```bash
kubectl get --namespace monitoring secret prometheus-grafana --template '{{ printf "%s\n" ( index .data "admin-password" | base64decode) }}'
kubectl get --namespace monitoring secret prometheus-grafana --template '{{ printf "%s\n" ( index .data "admin-user" | base64decode) }}'
```

Grafana's hostname also can be queried as:

```
kubectl get --namespace monitoring deployments prometheus-grafana -o json | jq -r '.metadata.name'
```

For accessing Grafana's dashboard remotely, it can be done through the following step (it will be available at `<your server ip>:9999`):

```bash
GRAFANA_POD=$(kubectl get pods --namespace monitoring -l "app.kubernetes.io/name=grafana" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward "$GRAFANA_POD" --address 0.0.0.0 9999:3000 --namespace monitoring
```



### Installing Community Prometheus Stack

If the user is willing to install a full Prometheus Stack (State Metrics, Node Exporter and Grafana), there is a community chart that provides this at [kube-prometheus-stack installation instructions](https://github.com/prometheus-community/helm-charts/blob/main/charts/kube-prometheus-stack/README.md).


First, add the Prometheus Community repositories:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://charts.helm.sh/stable
helm repo update
```

Create the `monitoring` namespace:

```bash
kubectl create namespace monitoring 
```

You'll need to update your CRDs for this operator or create those if this is the first time creating it. Example CRDs are:

```bash
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_alertmanagers.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_podmonitors.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_probes.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_prometheuses.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_prometheusrules.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_servicemonitors.yaml
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.42.0/example/prometheus-operator-crd/monitoring.coreos.com_thanosrulers.yaml
```


Install the [Prometheus Server Operator](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus):

```bash
helm install --namespace monitoring prometheus prometheus-community/kube-prometheus-stack --set grafana.enabled=true
```

> StackGres provides more and advanced options for monitoring installation, see [Create a more advanced cluster]({{% relref "04-administration-guide/07-create-a-more-advanced-cluster" %}}) in the [Administration Guide]({{% relref "04-administration-guide" %}}).


Once the operator is installed, take note of the generated secrets as you they are need to be specified at StackGres operator installation. By default is `user=admin` and `password=prom-operator`:

```bash
kubectl get --namespace monitoring secret prometheus-grafana --template '{{ printf "%s\n" ( index .data "admin-password" | base64decode) }}'
kubectl get --namespace monitoring secret prometheus-grafana --template '{{ printf "%s\n" ( index .data "admin-user" | base64decode) }}'
```

Grafana's hostname also can be queried as:

```
kubectl get --namespace monitoring deployments prometheus-grafana -o json | jq -r '.metadata.name'
```

For accessing Grafana's dashboard remotely, it can be done through the following step (it will be available at `<your server ip>:9999`):

```bash
GRAFANA_POD=$(kubectl get pods --namespace monitoring -l "app.kubernetes.io/name=grafana" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward "$GRAFANA_POD" --address 0.0.0.0 9999:3000 --namespace monitoring
```


### Pre-existing Grafana Integration and Pre-requisites

#### Integrating Grafana

If you already have a Grafana installation in your system you can embed it automatically in the
 StackGres UI by setting the property `grafana.autoEmbed=true`:

```
helm install --namespace stackgres stackgres-operator {{< download-url >}}/helm/stackgres-operator.tgz \
  --set grafana.autoEmbed=true
```

This method requires the installation process to be able to authenticate using administrative username and password to the Grafana's API (see [installation via helm]({{% relref "/03-production-installation/02-installation-via-helm" %}}) for more options related to automatic embedding of Grafana).

#### Manual integration

Some manual steps are required in order to achieve such integration.

1. Create Grafana dashboard for Postgres exporter and copy/paste share URL:

    **Using the UI:** Click on Grafana > Create > Import > Grafana.com Dashboard 9628

    Check [the dashboard](https://grafana.com/grafana/dashboards/9628) for more details.

2. Copy/paste Grafana's dashboard URL for the Postgres exporter:

    **Using the UI:** Click on Grafana > Dashboard > Manage > Select Postgres exporter dashboard > Copy URL

3. Create and copy/paste Grafana API token:

    **Using the UI:** Grafana > Configuration > API Keys > Add API key (for viewer) > Copy key value

## Non production options

We recommend to disable all non production options in a production environment. To do so create a
 YAML values file to include in the helm installation (`-f` or `--values` parameters) of the
 StackGres operator similar to the following:

``` yaml
nonProductionOptions: {}
```
