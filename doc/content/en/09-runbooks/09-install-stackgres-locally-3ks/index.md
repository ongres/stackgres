---
Title: Install K3S and StackGres locally
weight: 7
url: runbooks/k3s-stackgres
description: How to install locally K3S and f Stackgres
showToc: true
---

This runbook will show you how to install (K3s)[https://www.k3s.io] a simplified and secure version of Kubernetes which will require lower resources to create an environment, where we will deploy StackGres. 

To proceed with the installation please execute:

**1 - Install K3s:** 

```
curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION=v1.25.9+k3s1 sh -
```

**2 -  Add the config to kube config**

```
sudo k3s kubectl config view --raw >> ~/.kube/config 
```

**3 - Check the status of the nodes:**

```
kubectl get nodes
```
Output: 
```
NAME      STATUS   ROLES                  AGE    VERSION
jose-pc   Ready    control-plane,master   125d   v1.25.9+k3s1

```

**4 - Add repo stackgres-charts and prometheus repo**

```
helm repo add stackgres-charts https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

```

**5 - Install Prometheus Operator**

It is a prerequisite for StackGres monitoring:

```bash
helm install --create-namespace --namespace monitoring  prometheus-operator \
--set grafana.enabled=true \
--set-string grafana.image.tag=9.5.2 \
--set-string grafana.persistence.enabled=true \
--set-string grafana.persistence.size=10Gi \
prometheus-community/kube-prometheus-stack
```



**6 - Install StackGres**
```
helm install  --create-namespace --namespace stackgres stackgres-operator \
--set grafana.autoEmbed=true \
--set-string grafana.webHost=prometheus-operator-grafana.monitoring \
--set-string grafana.secretNamespace=monitoring \
--set-string grafana.secretName=prometheus-operator-grafana \
--set-string grafana.secretUserKey=admin-user \
--set-string grafana.secretPasswordKey=admin-password \
--set-string adminui.service.type=LoadBalancer \
stackgres-charts/stackgres-operator --version 1.4.3
```

Follow the instructions on the terminal for access the UI and credentials.

If you need this report again please execute:

```
helm get notes -n stackgres stackgres-operator
```

**To deinstall K3S you should execute:** 

```
sudo /usr/local/bin/k3s-uninstall.sh
```