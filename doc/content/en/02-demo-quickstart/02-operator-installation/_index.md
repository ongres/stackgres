---
title: Operator installation
weight: 2
url: demo/operator/install
---

## Installation with kubectl

We ship some kubernetes resources files in order to allow installation of the StackGres operator
 for demostration purpose. Assuming you have already installed the the
 [kubectl CLI](https://kubernetes.io/docs/tasks/tools/install-kubectl/) you can install the
 operator with the following command:

```
kubectl apply -f {{< download-url >}}/demo-operator.yml
```

> The `demo-operator.yml` will expose the UI as with a LoadBalancer. Note that enabling this feature
> will probably incurr in some fee that depend on the host of the kubernetes cluster (for example
> this is true for EKS, GKE and AKS).

To clean up the resources created by the demo just run:

```
kubectl delete --ignore-not-found -f {{< download-url >}}/demo-operator.yml
```

## Installation with helm

You can also install the StackGres operator using [helm vesion 3.1.x](https://github.com/helm/helm/releases)
 with the following command:

```
kubectl create namespace stackgres

helm install --namespace stackgres stackgres-operator \
  {{< download-url >}}/helm-operator.tgz \
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
while [ $(kubectl get pods -n stackgres | grep -E 'stackgres\-(operator|restapi)' | grep -E '0/1|1/1|2/2' | grep -E 'Running|Completed' | wc -l) -ne 3 ] ; do
  echo not ready...
  sleep 3
done
```

Once it's ready you will see that the two pods are `Running` and the create certificate job is `Complete`:

```bash
➜ kubectl get pods -n stackgres   
NAME                                          READY   STATUS      RESTARTS   AGE
stackgres-operator-7bfcb56dc7-c2hfs           1/1     Running     0          18m
stackgres-operator-create-certificate-2fltp   0/1     Completed   0          18m
stackgres-restapi-66db44f45f-l5gz4            2/2     Running     0          18m
```

## Connect to the UI

To connect to the Web UI of the operator you may forward port 443 of the operator pod:

```
POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward "$POD_NAME" 8443:9443 --namespace stackgres
```

Then open the browser at following address [`localhost:8443/admin/`]](`https://localhost:8443/admin/`)

The UI will ask for a username and a password. By default those are `admin` and a randomly generated password. You can run the command bellow to get the user and password auto-generated:

```bash
kubectl get secret -n stackgres stackgres-restapi --template 'username = {{ printf "%s\n" (.data.k8sUsername | base64decode) }}password = {{ printf "%s\n" ( .data.clearPassword | base64decode) }}'
```

## Changing the UI password

You can use the command bellow to change the password:

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

> See [installation via helm]({{% relref "/03-production-installation/02-installation-via-helm" %}}) section in order to change those.