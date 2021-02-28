---
title: Prometheus and Grafana
weight: 3
url: tutorial/prerequisites/prometheus-grafana
---

Prometheus (or Prometheus-compatible software) and Grafana are optional components, but it is recommended that you will
follow these instructions to install them for the tutorial. Otherwise, monitoring and dashboard integration in the
StackGres Web Console will not be enabled.

Any Prometheus and Grafana installation may work with StackGres. But if you install modifying some defaults, you may
need to make adjustments to the StackGres Helm installation. Use the following commands to run the tutorial without
needing further adjustments.

First, create a namespace for `monitoring` purposes:

```bash
kubectl create namespace monitoring
```

Then install both Prometheus and Grafana with the combined Helm chart:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

helm install --namespace monitoring \
	prometheus prometheus-community/kube-prometheus-stack \
	--version 13.4.0 \
	--set grafana.enabled=true
```

After 1-2 minutes you should get an output similar to:

```plain
NAME: prometheus
LAST DEPLOYED: Sun Feb 28 23:28:21 2021
NAMESPACE: monitoring
STATUS: deployed
REVISION: 1
NOTES:
kube-prometheus-stack has been installed. Check its status by running:
  kubectl --namespace monitoring get pods -l "release=prometheus"

Visit https://github.com/prometheus-operator/kube-prometheus for instructions on how to create & configure Alertmanager and Prometheus instances using the Operator.
```

You may also check the pods running on the `monitoring` namespace:

```bash
kubectl -n monitoring get pods
```

```text
NAME                                                     READY   STATUS    RESTARTS   AGE
alertmanager-prometheus-kube-prometheus-alertmanager-0   2/2     Running   0          4m14s
prometheus-grafana-6b87549fff-djkh6                      2/2     Running   0          4m18s
prometheus-kube-prometheus-operator-8d85d6d94-jjdn6      1/1     Running   0          4m18s
prometheus-kube-state-metrics-6df5d44568-6z9qn           1/1     Running   0          4m18s
prometheus-prometheus-kube-prometheus-prometheus-0       2/2     Running   1          4m14s
prometheus-prometheus-node-exporter-5bc5q                1/1     Running   0          4m18s
prometheus-prometheus-node-exporter-ccbkl                1/1     Running   0          4m18s
prometheus-prometheus-node-exporter-x6mdz                1/1     Running   0          4m18s
```
