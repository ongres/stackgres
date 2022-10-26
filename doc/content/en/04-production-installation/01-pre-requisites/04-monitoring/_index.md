---
title: Monitoring
weight: 4
url: install/prerequisites/monitoring
description: Details about how to setup and configure prometheus.
showToc: true
---

As early indicated in [Component of the Stack]({{% relref "01-introduction/04-components-of-the-stack/#monitoring" %}}) StackGres, at the moment, only supports Prometheus integration.

## Monitoring, Observability and Alerting with Prometheus and Grafana

Prometheus natively includes the following services:

- Prometheus Server: The core service
- Alert Manager: Handle events and send notifications to your preferred on-call platform


## Installing Community Prometheus Stack

If the user is willing to install a full Prometheus Stack (State Metrics, Node Exporter and Grafana), there is a community chart that provides this at [kube-prometheus-stack installation instructions](https://github.com/prometheus-community/helm-charts/blob/main/charts/kube-prometheus-stack/README.md).


First, add the Prometheus Community repositories:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://charts.helm.sh/stable
helm repo update
```

Install the [Prometheus Server Operator](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus):

```bash
helm install --create-namespace --namespace monitoring prometheus prometheus-community/kube-prometheus-stack --set grafana.enabled=true --version 12.10.6
```

> StackGres provides more and advanced options for monitoring installation, see [Operator installation with Helm]({{% relref "04-production-installation/02-installation-via-helm/#stackgres-operator-installation" %}}) in the [Production installation session]({{% relref "04-production-installation/#monitoring" %}}).

Once the operator is installed, take note of the generated secrets as you they are need to be specified at StackGres operator installation. By default are `user=admin` and `password=prom-operator`:

```bash
kubectl get secret prometheus-grafana \
    --namespace monitoring \
    --template '{{ printf "user = %s\npassword = %s\n" (index .data "admin-user" | base64decode) (index .data "admin-password" | base64decode) }}'
```

Grafana's hostname also can be queried as:

```
kubectl get --namespace monitoring deployments prometheus-grafana -o json | jq -r '.metadata.name'
```

### Re-routing services to different ports

In a production setup, is very likely that you will be installing all the resources in a remote location, so you'll need to route the services through specific interfaces and ports.

> For sake of simplicity, we will port-forward to all interfaces (0.0.0.0), although we
> strongly recommend to only expose through internal network interfaces when dealing on production.

### Exposing the Grafana UI

To access Grafana's dashboard remotely, it can be done through the following step (it will be available at `<your server ip>:9999`):

```bash
GRAFANA_POD=$(kubectl get pods --namespace monitoring -l "app.kubernetes.io/name=grafana" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward "$GRAFANA_POD" --address 0.0.0.0 9999:3000 --namespace monitoring
```

### Exposing the Prometheus Server UI

```bash
POD_NAME=$(kubectl get pods --namespace monitoring -l "app=prometheus" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9090
```

The Prometheus server serves through port 80 under `prometheus-operator-server.monitoring` DNS name.

### Exposing Alert Manager

Over port 80, Prometheus alertmanager can be accessed through `prometheus-operator-alertmanager.monitoring` DNS name.

```
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app=alertmanager" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9093
```

## Pre-existing Grafana Integration and Pre-requisites

### Integrating Grafana

If you already have a Grafana installation in your system you can embed it automatically in the
 StackGres UI by setting the property `grafana.autoEmbed=true`:

```
helm install --namespace stackgres stackgres-operator {{< download-url >}}/helm/stackgres-operator.tgz \
  --set grafana.autoEmbed=true
```

This method requires the installation process to be able to authenticate using administrative username and password to the Grafana's API (see [installation via helm]({{% relref "/04-production-installation/02-installation-via-helm" %}}) for more options related to automatic embedding of Grafana).

### Manual integration

Some manual steps are required in order to achieve such integration.

1. Create Grafana dashboard for Postgres exporter and copy/paste share URL:

    **Using the UI:** Click on Grafana > Create > Import > Grafana.com Dashboard 9628

    Check [the dashboard](https://grafana.com/grafana/dashboards/9628) for more details.

2. Copy/paste Grafana's dashboard URL for the Postgres exporter:

    **Using the UI:** Click on Grafana > Dashboard > Manage > Select Postgres exporter dashboard > Copy URL

3. Create and copy/paste Grafana API token:

    **Using the UI:** Grafana > Configuration > API Keys > Add API key (for viewer) > Copy key value

## Installing Grafana and create basic dashboards

If you already installed the `prometheus-community/kube-prometheus-stack` you can skip this session. It was  Get the source repository for the Grafana charts:

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

And install the chart:

```bash
helm install --namespace monitoring grafana grafana/grafana
```

Get the `admin` credential:

```bash
kubectl get secret --namespace monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

Expose your Grafana service at `grafana.monitoring` (port 80) through your interfaces and port 3000 to login remotely (using above secret):

```bash
POD_NAME=$(kubectl get pods --namespace monitoring -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=grafana" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 3000
```

> NOTE: take note of the Grafana's URL `grafana.monitoring`, which will be used when configuring StackGres Operator.

The following script, will create a basic PostgreSQL dashboard against Grafana's API (you can change grafana_host to point to the remote location):

```bash
grafana_host=http://localhost:3000
grafana_admin_cred=$(kubectl get secret --namespace monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo)
grafana_credentials=admin:${grafana_admin_cred}
grafana_prometheus_datasource_name=Prometheus
curl_grafana_api() {
  curl -sk -H "Accept: application/json" -H "Content-Type: application/json" -u "$grafana_credentials" "$@"
}
get_admin_settings() {
  # Not executed in the script, but useful to keep this
  curl_grafana_api -X GET  ${grafana_host}/api/admin/settings | jq .
}
dashboard_id=9628
dashboard_json="$(cat << EOF
{
  "dashboard": $(curl_grafana_api "$grafana_host/api/gnet/dashboards/$dashboard_id" | jq .json),
  "overwrite": true,
  "inputs": [{
    "name": "DS_PROMETHEUS",
    "type": "datasource",
    "pluginId": "prometheus",
    "value": "$grafana_prometheus_datasource_name"
  }]
}
EOF
)"
grafana_dashboard_url="$(curl_grafana_api -X POST -d "$dashboard_json" "$grafana_host/api/dashboards/import" | jq -r .importedUrl)"
echo ${grafana_host}${grafana_dashboard_url}
```

The resulting URL will be the dashboard whether your PostgreSQL metrics will be show up.

### Monitoring Setup validation

At this point, you should have ended with the following pods:

```
# kubectl get pods -n monitoring
NAME                                                      READY   STATUS    RESTARTS   AGE
alertmanager-prometheus-kube-prometheus-alertmanager-0    2/2     Running   0          20m
grafana-7575c4b7b5-2cbvw                                  1/1     Running   0          14m
prometheus-grafana-5b458bf78c-tpqrl                       2/2     Running   0          20m
prometheus-kube-prometheus-operator-576f4bf45b-w5j9m      2/2     Running   0          20m
prometheus-kube-state-metrics-c65b87574-tsx24             1/1     Running   0          20m
prometheus-operator-alertmanager-655b8bc7bf-hc6fd         2/2     Running   0          79m
prometheus-operator-kube-state-metrics-69fcc8d48c-tmn8j   1/1     Running   0          79m
prometheus-operator-node-exporter-28qz9                   1/1     Running   0          79m
prometheus-operator-pushgateway-888f886ff-bxxtw           1/1     Running   0          79m
prometheus-operator-server-7686fc69bd-mlvsx               2/2     Running   0          79m
prometheus-prometheus-kube-prometheus-prometheus-0        3/3     Running   1          20m
prometheus-prometheus-node-exporter-jbsm2                 0/1     Pending   0          20m
```

## Enable Prometheus Auto Binding in Cluster

To allow the operator discover available [Prometheus](https://github.com/prometheus-operator/prometheus-operator/blob/master/Documentation/api.md#prometheus)
 and create required [ServiceMonitors](https://github.com/prometheus-operator/prometheus-operator/blob/master/Documentation/api.md#servicemonitor)
 to store StackGres stats in existing instances of prometheus (only for those that are created through the
 [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator)) you have to set to `true` field `.spec.prometheusAutobind` in
 your [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}):

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: simple
spec:
  instances: 2
  postgres:
    version: 'latest'
  pods:
    persistentVolume:
      size: '5Gi'
  prometheusAutobind: true
```