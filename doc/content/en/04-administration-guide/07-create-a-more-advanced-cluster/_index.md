---
title: Create Advanced Cluster
weight: 7
url: administration/cluster/creation/advanced
---

## Advanced Prometheus Server

### Monitoring, Observability and Alerting with Prometheus and Grafana

Prometheus natively includes the following services:

- Prometheus Server: The core service
- Alert Manager: Handle events and send notifications to your preferred on-call platform
- Push Gateway: exposes metrics for ephemeral and batch jobs  

###  Installation

First, add the Prometheus Community repositories:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://kubernetes-charts.storage.googleapis.com/
helm repo update
```

Install the [Prometheus Server Operator](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus):

```bash
helm install --namespace monitoring prometheus-operator prometheus-community/prometheus
```

### Re-routing services to different ports 

In a production setup, is very likely that you will be installing all the resources in a remote location, so you'll need to route the services through specific interfaces and ports.

> For sake of simplicity, we port-forward to all interfaces (0.0.0.0), although we
> strongly recommend to only expose through internal network interfaces when dealing on production.

Exposing Prometheus Server UI:

```bash
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app=prometheus,component=server" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9090
```

The Prometheus server serves through port 80 under `prometheus-operator-server.monitoring.svc.cluster.local` DNS name.

Exposing Alert Manager:

Over port 80, Prometheus alertmanager can be accessed through `prometheus-operator-alertmanager.monitoring.svc.cluster.local` DNS name.

```
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app=prometheus,component=alertmanager" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9093
```

Get the PushGateway URL by running these commands in the same shell:

```
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app=prometheus,component=pushgateway" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9091
```

The Prometheus PushGateway can be accessed via port 9091 on the following DNS name from within your cluster: `prometheus-operator-pushgateway.monitoring.svc.cluster.local`

### Installing Prometheus Stack

[kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)

```
helm install --namespace monitoring prometheus prometheus-community/kube-prometheus-stack
```

```
kubectl --namespace monitoring get pods -l "release=prometheus"
```

### Installing Grafana and create basic dashboards

Get the source repository for the Grafana charts:

```sh
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

Expose your Grafana service at `grafana.monitoring.svc.cluster.local` (port 80) through your interfaces and port 3000 to login remotely (using above secret):

```bash
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=grafana" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 3000
```

> NOTE: take note of the Grafana's URL `grafana.monitoring.svc.cluster.local`, which will be used when configuring StackGres Operator.

The following script, will create a basic PostgreSQL dashboard against Grafana's API (you can change grafana_host to point to the remote location):

```sh
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

### Monitoring Setup Verification

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

## StackGres Operator installation

Now we can proceed to the StackGres including the new grafana options.

```bash
grafana_admin_cred=$(kubectl get secret --namespace monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo)

helm install --namespace stackgres stackgres-operator \
        --set grafana.autoEmbed=true \
        --set-string grafana.webHost=grafana.monitoring \
        --set-string grafana.user=admin \
        --set-string grafana.password=${grafana_admin_cred} \
        --set-string adminui.service.type=LoadBalancer \
        {{< download-url >}}/helm/stackgres-operator.tgz
```
