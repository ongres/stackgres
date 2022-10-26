---
title: Setting up the enviroment
weight: 1
url: tutorial/setting-up-env
description: Details about setting up a Kubernetes cluster and some StackGres dependencies.
---

# Setting up a Kubernetes Cluster

You obviously need a Kubernetes cluster to run this tutorial. In general, any Kubernetes-compliant cluster from version 1.18 to 1.23 should work. Some Kubernetes clusters require some specific adjustments. Please see [StackGres documentation on K8s environments](https://stackgres.io/doc/latest/install/prerequisites/k8s/) for specific notes on RKE.

The lab demo will be performed on an [Amazon EKS](https://aws.amazon.com/eks/) cluster. If you wish to create also an EKS environment, you may run the steps detailed below (you will need to have installed [awscli](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) and [eksctl](https://github.com/weaveworks/eksctl/releases)):

```
export AWS_REGION= #your preferred region
export K8S_CLUSTER_NAME=	# EKS cluster name

eksctl --region $AWS_REGION create cluster --name $K8S_CLUSTER_NAME \
        --node-type m5a.2xlarge --node-volume-size 100 --nodes 3 \
        --zones ${AWS_REGION}a,${AWS_REGION}b,${AWS_REGION}c \
        --version 1.20
```

This operation takes a bit more than 15 minutes.


## Installing StackGres dependencies

While this is an optional step, it is recommended and it will be followed for the lab. The purpose is to install Prometheus and Grafana (along with AlertManager), so that they can be integrated automatically with StackGres.


And install the Prometheus stack with Helm:

```
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

helm install --create-namespace --namespace monitoring prometheus-operator prometheus-community/kube-prometheus-stack
```

After some seconds / a minute you should have several pods including Prometheus, Grafana and AlertManager in the `monitoring` namespace.

You may use a custom Prometheus and Grafana installation if you wish. In this case note the credentials used for accesing them, as will be required when installing StackGres.

