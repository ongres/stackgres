---
title: Setting up the enviroment
weight: 1
url: tutorial/setting-up-env
description: Details about setting up a Kubernetes cluster and some StackGres dependencies.
---

# Setting up a Kubernetes Cluster

You obviously need a Kubernetes cluster to run this tutorial. In general, any Kubernetes-compliant cluster from version 1.18 to 1.25 should work. Some Kubernetes clusters require some specific adjustments. Please see [StackGres documentation on K8s environments](https://stackgres.io/doc/latest/install/prerequisites/k8s/) for additional notes on specific managed Kubernetes cluster and Kubernetes distributions.

## Amazon Elastic Kubernetes Service (Amazon EKS)

This section has some instructions on how to setup an [Amazon EKS](https://aws.amazon.com/eks/) cluster. If you wish to create an EKS environment, you may run the steps detailed below (you will need to have installed [awscli](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) and [eksctl](https://github.com/weaveworks/eksctl/releases)):

```bash
export AWS_REGION= #your preferred region
export K8S_CLUSTER_NAME=	# EKS cluster name

eksctl --region $AWS_REGION create cluster --name $K8S_CLUSTER_NAME \
        --node-type m5a.2xlarge --node-volume-size 100 --nodes 3 \
        --zones ${AWS_REGION}a,${AWS_REGION}b,${AWS_REGION}c \
        --version 1.20
```

This operation takes a bit more than 15 minutes.

# Installing Local Path Provisioner

Most managed Kubernetes cluster and Kubernetes distributions comes with a StorageClass that allows to create PersisitentVolumes on demand whenever a PersisitentVolumeClaim requires it.

Some Kubernetes distributions do not provide such facility so that a simple way to overcome this lack is to install the [Local Path Provisioner](https://github.com/rancher/local-path-provisioner) that will provide a StorageClass that is backed by the node local disk. To install you may use the following command:

```bash
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/v0.0.24/deploy/local-path-storage.yaml
```

## Local Path Provisioner on Red Hat OpenShift 4.6+

When installing the Local Path Provisioner on OpenShift 4.x you have to create some extra configurations. In particular, since the OpenShift nodes filesystem are enforced with [SELinux](https://es.wikipedia.org/wiki/SELinux) you have to create a special SELinux context for the base folder used by the Local Path Provisioner. This can be achieved by creating a [MachineConfig](https://docs.openshift.com/container-platform/4.6/post_installation_configuration/machine-configuration-tasks.html):

```bash
cat << 'EOF' | kubectl create -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  name: 50-master-local-path-provisioner
  labels:
    machineconfiguration.openshift.io/role: master
spec:
  config:
    ignition:
      version: 2.2.0
    systemd:
      units:
        - name: local-path-provisioner.service
          enabled: true
          contents: |
            [Unit]
            Description=Create and set SELinux contenxt on local-path-provisioner directory
            Before=kubelet.service
            [Service]
            ExecStartPre=/bin/mkdir -p /opt/local-path-provisioner
            ExecStart=/usr/bin/chcon -Rt container_file_t /opt/local-path-provisioner
            [Install]
            WantedBy=multi-user.target
EOF
```

> After applying this command the cluster nodes will be restarted for the configuration to be applied.

The Local Path Provisioner ServiceAccount also requires some privileges that may be granted using the following commands:

```bash
oc adm policy add-scc-to-user privileged -n local-path-storage -z local-path-provisioner-service-account
oc adm policy add-scc-to-user hostmount-anyuid -n local-path-storage -z local-path-provisioner-service-account
```

# Installing StackGres dependencies

While this is an optional step, it is recommended and it will be followed for the lab. The purpose is to install Prometheus and Grafana (along with AlertManager), so that they can be integrated automatically with StackGres.


And install the Prometheus stack with Helm:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

helm install --create-namespace --namespace monitoring prometheus-operator prometheus-community/kube-prometheus-stack
```

After some seconds / a minute you should have several pods including Prometheus, Grafana and AlertManager in the `monitoring` namespace.

You may use a custom Prometheus and Grafana installation if you wish. In this case note the credentials used for accesing them, as will be required when installing StackGres.

## Monitoring on Red Hat OpenShift 4.6+

Starting from Red Hat OpenShift 4.6 the prometheus operator is installed by default in the cluster. You will have to configure to [enable the monitoring for user-defined projects](https://docs.openshift.com/container-platform/4.6/monitoring/enabling-monitoring-for-user-defined-projects.html). This can be achieved by creating creating (or editing if already exist) the cluster-monitoring-config ConfigMap (in namespace openshift-monitoring) and setting the parameter `enableUserWorkload` to `true` in the key `config.yaml` as in the following example:

```yaml
cat << EOF | kubectl create -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    enableUserWorkload: true
EOF
```

> If the ConfigMap already exists edit it in order to change the `enableUserWorkload` field to `true` in case it is not already set so

You will also have to install Grafana Operator separately by creating a Namespace, an OperatorGroup and a Subscription:

```bash
cat << EOF | kubectl create -f -
apiVersion: v1
kind: Namespace
metadata:
  name: grafana
---
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: grafana
  namespace: grafana
spec:
  targetNamespaces:
  - grafana
---
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: grafana-operator
  namespace: grafana
spec:
  channel: v4
  name: grafana-operator
  source: community-operators
  sourceNamespace: openshift-marketplace
  startingCSV: grafana-operator.v4.10.0
  installPlanApproval: Automatic
EOF
```

Also, create a Grafana instance and configure it in order to use the OpenShift user workload Prometheus for monitoring:

```bash
cat << EOF | kubectl create -f -
apiVersion: integreatly.org/v1alpha1
kind: Grafana
metadata:
  name: grafana
  namespace: grafana
EOF
# Wait for Grafana resources to be created
until kubectl get grafana -n grafana -o json | jq '.items[]|.status.message == "success" and .status.phase == "reconciling"' | grep -q true; do sleep 1; done
oc adm policy add-cluster-role-to-user cluster-monitoring-view -n grafana -z grafana-serviceaccount
BEARER_TOKEN="$(oc create token grafana-serviceaccount --duration=8760h -n grafana)"
cat << EOF | kubectl create -f -
apiVersion: integreatly.org/v1alpha1
kind: GrafanaDataSource
metadata:
  name: prometheus-grafanadatasource
  namespace: grafana
spec:
  datasources:
    - access: proxy
      editable: true
      isDefault: true
      jsonData:
        httpHeaderName1: 'Authorization'
        timeInterval: 5s
        tlsSkipVerify: true
      name: Prometheus
      secureJsonData:
        httpHeaderValue1: 'Bearer ${BEARER_TOKEN}'
      type: prometheus
      url: 'https://thanos-querier.openshift-monitoring.svc.cluster.local:9091'
  name: prometheus-grafanadatasource.yaml
EOF
```