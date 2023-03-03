---
title: Setting Up the Environment
weight: 1
url: tutorial/setting-up-env
description: Details about setting up a Kubernetes cluster and some StackGres dependencies.
---

## Setting Up a Kubernetes Cluster

You obviously need a Kubernetes cluster to run this tutorial. In general, any Kubernetes-compliant cluster from version 1.18 to 1.25 should work. Some Kubernetes clusters require some specific adjustments. Please see the [StackGres documentation on K8s environments](https://stackgres.io/doc/latest/install/prerequisites/k8s/) for specific notes on RKE.

The lab demo will be performed on an [Amazon EKS](https://aws.amazon.com/eks/) cluster. If you wish to create an EKS environment, you may run the steps shown below (you will need to have [awscli](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) and [eksctl](https://github.com/weaveworks/eksctl/releases) installed):

```
export AWS_REGION= #your preferred region
export K8S_CLUSTER_NAME=	# EKS cluster name

eksctl --region $AWS_REGION create cluster --name $K8S_CLUSTER_NAME \
        --node-type m5a.2xlarge --node-volume-size 100 --nodes 3 \
        --zones ${AWS_REGION}a,${AWS_REGION}b,${AWS_REGION}c \
        --version 1.20
```

This operation takes a bit more than 15 minutes.


## Installing StackGres Dependencies

While being an optional step, it is recommended to install Prometheus and Grafana (along with AlertManager). These will be integrated automatically with StackGres.

You can install this monitoring stack with Helm:

```
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

helm install --create-namespace --namespace monitoring prometheus-operator prometheus-community/kube-prometheus-stack
```

After some moments, you should have several pods including Prometheus, Grafana and AlertManager in the `monitoring` namespace.

If you like, you may use a custom Prometheus and Grafana installation instead. In this case, you'll need the Prometheus and Grafana credentials at StackGres installation time.