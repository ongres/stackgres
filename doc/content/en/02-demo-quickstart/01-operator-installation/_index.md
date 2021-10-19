---
title: Operator installation
weight: 2
url: demo/operator/install
description: Details about the how to install the operator.
showToc: true
---

## Operator Installation

We ship some kubernetes resources files in order to allow installation of the StackGres operator
 for demonstration purpose. Assuming you have already installed the the
 [kubectl CLI](https://kubernetes.io/docs/tasks/tools/install-kubectl/) you can install the
 operator with the following command:

```
kubectl apply -f {{< download-url >}}/stackgres-operator-demo.yml
```

> The `stackgres-operator-demo.yml` will expose the UI as with a LoadBalancer. Note that enabling this feature
> will probably incur in some fee that depend on the host of the kubernetes cluster (for example
> this is true for EKS, GKE and AKS).

## Wait for the operator start

Use the command below to be sure when the operation is ready to use:

```bash
kubectl wait -n stackgres deployment -l group=stackgres.io --for=condition=Available
```

Once it's ready you will see that the pods are `Running`:

```bash
➜ kubectl get pods -n stackgres -l group=stackgres.io
NAME                                  READY   STATUS    RESTARTS   AGE
stackgres-operator-78d57d4f55-pm8r2   1/1     Running   0          3m34s
stackgres-restapi-6ffd694fd5-hcpgp    2/2     Running   0          3m30s

```

To clean up the resources created by the demo just run:

```
kubectl delete --ignore-not-found -f {{< download-url >}}/stackgres-operator-demo.yml
```