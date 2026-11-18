---
title: Monitoring
weight: 6
url: /administration/monitoring
aliases: [ /monitoring/metrics , /install/prerequisites/monitoring ]
description: Details about monitoring and metrics automatically stored on Prometheus.
showToc: true
---

StackGres makes various Postgres-related metrics available, and integrates with Postgres and Grafana.
The metrics are shown in the monitoring dashboards (Prometheus and Grafana), as well as in the admin UI.

At the moment, StackGres only provides an integration for Prometheus.

This section shows how to enable observability, especially monitoring and alerting with StackGres, using Prometheus and Grafana.


## Observability: Monitoring and Alerting

Prometheus natively includes the following services:

- Prometheus Server: The core service
- Alert Manager: Handle events and send notifications to your preferred on-call platform


## Installing the Community Prometheus Stack

For a full Prometheus Stack (State Metrics, Node Exporter and Grafana), there is a community Helm chart available at [kube-prometheus-stack installation instructions](https://github.com/prometheus-community/helm-charts/blob/main/charts/kube-prometheus-stack/README.md).

First, add the Prometheus Community repositories:

```
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://charts.helm.sh/stable
helm repo update
```

Install the [Kube Prometheus Stack](https://github.com/prometheus-community/helm-charts/blob/main/charts/kube-prometheus-stack/):

```
helm install --create-namespace --namespace monitoring \
 --set grafana.enabled=true \
 prometheus prometheus-community/kube-prometheus-stack
```

> StackGres provides advanced options for monitoring installation, see [Operator installation with Helm]({{% relref "04-administration-guide/01-installation/02-installation-via-helm/#stackgres-operator-installation" %}}) in the [Production installation section]({{% relref "04-administration-guide/01-installation/#monitoring" %}}).

Once the operator is installed, you can retrieve the generated credentials. By default, they are user `admin` and password `prom-operator`.

```
kubectl get secret prometheus-grafana \
 --namespace monitoring \
 --template '{{ printf "user = %s\npassword = %s\n" (index .data "admin-user" | base64decode) (index .data "admin-password" | base64decode) }}'
```

Grafana's hostname also can be queried:

```
kubectl get --namespace monitoring deployments prometheus-grafana -o json | jq -r '.metadata.name'
```


### Re-Routing Services to Different Ports

In a production setup, it is very likely that you will be installing all the resources in a remote location, so you'll need to route the services through specific interfaces and ports.

> For the sake of simplicity, we will port-forward to the pods on all local interfaces (`0.0.0.0`).
> This is only for testing purposes, and we strongly recommend to only expose through secure or internal network interfaces when dealing with production workloads.


### Exposing the Grafana UI

To access Grafana's dashboard locally, you can forward the pod's port, so that it will be available at `localhost:9999`:

```
GRAFANA_POD=$(kubectl get pods --namespace monitoring -l "app.kubernetes.io/name=grafana" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward "$GRAFANA_POD" 9999:3000 --namespace monitoring
```


### Exposing the Prometheus Server UI

You can also access the Prometheus server, by forwarding the port of the Prometheus pod:

```
POD_NAME=$(kubectl get pods --namespace monitoring -l "app=prometheus" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME 9090
```

This will be available at `localhost:9090`.

Inside the cluster, the Prometheus server is available via the `prometheus-operator-server.monitoring` DNS name.


### Exposing the Alert Manager

You can also access the Prometheus alert manager, by forwarding the following port:

```
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app=alertmanager" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9093
```

Inside the cluster, the Prometheus alert manager can be accessed via `prometheus-operator-server.monitoring`.


## Pre-Existing Grafana Integration and Pre-Requisites

### Integrating Grafana

If you already have a Grafana installation in your system, you can embed it automatically in the StackGres UI by setting the property `grafana.autoEmbed=true`:

```
helm install --create-namespace --namespace stackgres stackgres-operator \
   --set grafana.autoEmbed=true \
   https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz
```

This method requires the installation process to authenticate using Grafana's username and password (see [installation via helm]({{% relref "/04-administration-guide/01-installation/02-installation-via-helm" %}}) for more options related to automatic embedding of Grafana).


### Manual Integration

Some manual steps are required in order to manually integrate Grafana.

1. Create the PostgreSQL Grafana dashboard:

   **Using the UI:** Click on Grafana > Create > Import > Grafana.com Dashboard 9628

   Check the [PostgreSQL dashboard](https://grafana.com/grafana/dashboards/9628) for more details.

2. Copy Grafana's dashboard URL for the Postgres exporter:

   **Using the UI:** Click on Grafana > Dashboard > Manage > Select Postgres exporter dashboard > Copy URL

3. Create a Grafana API token:

   **Using the UI:** Grafana > Configuration > API Keys > Add API key (for viewer) > Copy key value


## Installing Grafana and Creating Basic Dashboards

If you already installed the `prometheus-community/kube-prometheus-stack`, you can skip this section.

Add the Grafana charts' source repository:

```
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

And install the chart:

```
helm install --namespace monitoring grafana grafana/grafana
```

Get the `admin` credential:

```
kubectl get secret --namespace monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

Expose your Grafana service (`grafana.monitoring`) in your cluster setup or port-forward to port `3000` locally:

```
POD_NAME=$(kubectl get pods --namespace monitoring -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=grafana" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 3000
```

You will need the admin credential to log into the Web Console (at `localhost:3000` if you're using port forwarding).

> NOTE: take note of the Grafana's URL `grafana.monitoring`, which will be used when configuring StackGres Operator.

The following script will create a basic PostgreSQL dashboard using Grafana's API (you can change the `grafana_host` to point to your remote location):

```
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

The resulting URL will point to the dashboard that displays your PostgreSQL metrics.


### Monitoring Setup Validation

At this point, you should have the following pods:

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

To allow the StackGres operator to discover available [Prometheus](https://github.com/prometheus-operator/prometheus-operator/blob/primary/Documentation/api.md#prometheus) instances, to create required [ServiceMonitors](https://github.com/prometheus-operator/prometheus-operator/blob/primary/Documentation/api.md#servicemonitor), to store StackGres stats in existing Prometheus instances (only for those that are created through the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator)), you have to set the field `.spec.prometheusAutobind` to `true` in your [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}):

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
  instances: 1
  postgres:
    version: 'latest'
  pods:
    persistentVolume:
      size: '5Gi'
  configurations:
    observability:
      prometheusAutobind: true
```
