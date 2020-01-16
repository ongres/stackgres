---
title: Operator installation
weight: 2
---

# Installation with kubectl

We ship some kubernetes resources files in order to allow installation of the StackGres operator
 for demostration purpose. Assuming you have already installed the the
 [kubectl CLI](https://kubernetes.io/docs/tasks/tools/install-kubectl/) you can install the
 operator with the following command:

```
kubectl apply -f https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/demo-operator.yml
```

To clean up the resources created by the demo just run:

```
kubectl delete --ignore-not-found -f https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/demo-operator.yml
```

# Installation with helm

You can also install the StackGres operator using [helm vesion 2.x](https://github.com/helm/helm/releases)
 with the following command:

```
helm install --namespace stackgres --name stackgres-operator \
  https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm-operator.tgz
```

To clean up the resources created by the demo just run:

```
(helm get manifest simple; helm get hooks simple) | kubectl delete --ignore-not-found -f -
helm delete --purge simple
```

# Wait for the operator to become ready

You must wait for the operator to become ready in order to allow installation of a StackGres cluster:

```
until kubectl describe deployment -n stackgres stackgres-operator | grep -q '  Ready: true'
do
  sleep 1
done
```
