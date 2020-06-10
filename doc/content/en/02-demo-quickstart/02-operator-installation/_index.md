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
kubectl apply -f {{< download-url >}}/demo-operator.yml
```

To clean up the resources created by the demo just run:

```
kubectl delete --ignore-not-found -f {{< download-url >}}/demo-operator.yml
```

# Installation with helm

You can also install the StackGres operator using [helm vesion 3.1.x](https://github.com/helm/helm/releases)
 with the following command:

```
kubectl create namespace stackgres

helm install stackgres-operator \
  {{< download-url >}}/helm-operator.tgz
```

To clean up the resources created by the demo just run:

```
(helm get manifest simple; helm get hooks simple) | kubectl delete --ignore-not-found -f -
helm delete --purge simple
```

# Wait for the operator to become ready

You must wait for the operator to become ready in order to allow installation of a StackGres cluster:

```
until kubectl describe pod --selector=app=stackgres-operator | grep '  Ready\s\+True'
do
  sleep 1
done
```

# Connect to the UI

To connect to the Web UI of the operator you may forward port 443 of the operator pod:

```
kubectl port-forward "$(kubectl get pod --selector=app=stackgres-operator -o name)" 8443:9443
```

Then open the browser at following address `https://localhost:8443/stackgres`

The UI will ask for a username and a password. By default those are `admin` and `st4ckgr3s`
 respectively. See [installation via helm]({{% relref "/03-production-installation/02-installation-via-helm" %}})
 section in order to change those.