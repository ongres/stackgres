---
title: Operator installation
weight: 2
url: demo/operator/install
description: Details about the how to install the operator.
showToc: true
---

## Installation with kubectl

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

To clean up the resources created by the demo just run:

```
kubectl delete --ignore-not-found -f {{< download-url >}}/stackgres-operator-demo.yml
```

## Installation with helm

You can also install the StackGres operator using [helm version 3.1.x](https://github.com/helm/helm/releases)
 with the following command:

```
kubectl create namespace stackgres

helm repo add stackgres-charts https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/

helm install --namespace stackgres stackgres-operator \
  stackgres-charts/stackgres-operator \
  --set-string adminui.service.type=LoadBalancer
```

> The `--set-string adminui.service.type=LoadBalancer` will expose the UI as with a LoadBalancer. Note that
> enabling this feature will probably incurr in some fee that depend on the host of the kubernetes cluster
> (for example this is true for EKS, GKE and AKS).

To clean up the resources created by the demo just run:

```
helm uninstall --namespace stackgres stackgres-operator
```

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

## Connect to the UI

To connect to the Web UI of the operator you may forward port 443 of the operator pod:

```
POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward "$POD_NAME" 8443:9443 --namespace stackgres
```

Then open the browser at following address [`localhost:8443/admin/`]](`https://localhost:8443/admin/`)

The UI will ask for a username and a password. By default those are `admin` and a randomly generated password. You can run the command below to get the user and password auto-generated:

```bash
kubectl get secret -n stackgres stackgres-restapi --template 'username = {{ printf "%s\n" (.data.k8sUsername | base64decode) }}password = {{ printf "%s\n" ( .data.clearPassword | base64decode) }}'
```

### Connecting to the UI through the LoadBalancer

Since in the previous installation we set set option `--set-string adminui.service.type=LoadBalancer`, 
 is possible to connect to the UI whitout having to do a port-forward.

To find the hostname you just need to execute and look for the External IP value. 
``` bash
kubectl get services -n stackgres stackgres-restapi
```

For instance, in this output from a EKS cluster the hostname is `aecdec3efe3a542b2b0b40d0072ab338-1132619204.us-west-2.elb.amazonaws.com`
``` bash
NAME                TYPE           CLUSTER-IP       EXTERNAL-IP                                                               PORT(S)         AGE
stackgres-restapi   LoadBalancer   10.100.233.210   aecdec3efe3a542b2b0b40d0072ab338-1132619204.us-west-2.elb.amazonaws.com   443:32674/TCP   10m
```

Therefore to connect the UI, the address would be `https://aecdec3efe3a542b2b0b40d0072ab338-1132619204.us-west-2.elb.amazonaws.com/admin`
## Changing the UI password

You can use the command below to change the password:

```bash
NEW_USER=admin
NEW_PASSWORD=password
kubectl create secret generic -n stackgres stackgres-restapi  --dry-run=client -o json \
  --from-literal=k8sUsername="$NEW_USER" \
  --from-literal=password="$(echo -n "${NEW_USER}${NEW_PASSWORD}"| sha256sum | awk '{ print $1 }' )" > password.patch

kubectl patch secret -n stackgres stackgres-restapi -p "$(cat password.patch)" && rm password.patch
```

Remember to remove the generated password hint from the secret to avoid security flaws:

```bash
kubectl patch secrets --namespace stackgres stackgres-restapi --type json -p '[{"op":"remove","path":"/data/clearPassword"}]'
```

> See [installation via helm]({{% relref "/04-production-installation/02-installation-via-helm" %}}) section in order to change those.