---
title: StackGres Installation
weight: 2
url: tutorial/stackgres-installation
description: Details about the operator installation.
---

The recommended way to install StackGres is to use the official Helm chart. Additional parameters can be passed to the default installation:
* Access to Grafana. StackGres uses this access to install StackGres specific dashboards as well as to embed Grafana into the web console. If you've installed Prometheus as shown in the previous step, the host and credentials are set to the default values (Grafana service: `prometheus-grafana.monitoring`, username: `admin`, password: `prom-operator`).
* How to expose the web console. You can choose `LoadBalancer` if you're using a Kubernetes setup that supports creating load balancers. Otherwise, you can choose `ClusterIP` (the default), or omit this parameter, in which case you will need to create a custom routing to the console, or use mechanisms such as a port forward, in order to access the web console.

Proceed to install StackGres:

- Add the Helm repo:

```
helm repo add stackgres-charts https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/
```

- Install the Operator

> StackGres (the operator and associated components) may be installed on any namespace but we recommended to create a dedicated namespace (`stackgres` in this case).

```bash
helm install --create-namespace --namespace stackgres stackgres-operator \
    --set grafana.autoEmbed=true \
    --set-string grafana.webHost=prometheus-operator-grafana.monitoring \
    --set-string grafana.secretNamespace=monitoring \
    --set-string grafana.secretName=prometheus-operator-grafana \
    --set-string grafana.secretUserKey=admin-user \
    --set-string grafana.secretPasswordKey=admin-password \
    --set-string adminui.service.type=LoadBalancer \
    stackgres-charts/stackgres-operator
```
> You can specify the version to the Helm command. For example you may add `--version 1.0.0` to install verion `1.0.0`.

Note that using `adminui.service.type=LoadBalancer` will create a network load balancer, which may incur in additional costs. You may alternatively use `ClusterIP` if that's your preference.

The StackGres installation may take a few minutes. The output will be similar to:

```plain
NAME: stackgres-operator
LAST DEPLOYED: Mon Oct 1 00:25:10 2021
NAMESPACE: stackgres
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
Release Name: stackgres-operator
StackGres Version: 1.0.0

   _____ _             _     _____
  / ____| |           | |   / ____|
 | (___ | |_ __ _  ___| | _| |  __ _ __ ___  ___
  \___ \| __/ _` |/ __| |/ / | |_ | '__/ _ \/ __|
  ____) | || (_| | (__|   <| |__| | | |  __/\__ \
 |_____/ \__\__,_|\___|_|\_\\_____|_|  \___||___/
                                  by OnGres, Inc.

Check if the operator was successfully deployed and is available:

    kubectl describe deployment -n stackgres stackgres-operator

    kubectl wait -n stackgres deployment/stackgres-operator --for condition=Available

Check if the restapi was successfully deployed and is available:

    kubectl describe deployment -n stackgres stackgres-restapi

    kubectl wait -n stackgres deployment/stackgres-restapi --for condition=Available

To access StackGres Operator UI from localhost, run the below commands:

    POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")

    kubectl port-forward "$POD_NAME" 8443:9443 --namespace stackgres

Read more about port forwarding here: http://kubernetes.io/docs/user-guide/kubectl/kubectl_port-forward/

Now you can access the StackGres Operator UI on:

https://localhost:8443

To get the username, run the command:

    kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\n" (.data.k8sUsername | base64decode) }}'

To get the generated password, run the command:

    kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "password = %s\n" (.data.clearPassword | base64decode) }}'

Remember to remove the generated password hint from the secret to avoid security flaws:

    kubectl patch secrets --namespace stackgres stackgres-restapi --type json -p '[{"op":"remove","path":"/data/clearPassword"}]'
```

## Connecting to the Web Console

Several useful commands are provided as part of the Helm installation output. Let's use them to connect to the StackGres
web console. If the `LoadBalancer` parameter was used, let's query the URL of the created load balancer, by querying the
K8s service created:

```bash
kubectl -n stackgres get svc --field-selector metadata.name=stackgres-restapi

NAME                TYPE           CLUSTER-IP      EXTERNAL-IP                                                              PORT(S)         AGE
stackgres-restapi   LoadBalancer   10.100.165.13   aa372eefc1630469f95e64d384caa004-833850176.eu-west-1.elb.amazonaws.com   443:32194/TCP   47m
```

The web console is exposed as part of the `stackgres-restapi` service. Here we can see it is of type `LoadBalancer`, and exposed at a given URL. Open this URL in the web browser prefixing it with `https://` , as in:

```
https://aa372eefc1630469f95e64d384caa004-833850176.eu-west-1.elb.amazonaws.com
```

If your service is exposed as `ClusterIP`, you can instead use port forwarding to access the web console. Find the name of the `restapi` pod in either of the following ways, at your preference:

```bash
$ POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")
```

And then do the port-forward to your localhost on port 8443, which you can access from the web browser on URL `https://localhost:8443`:

```bash
$ kubectl port-forward "$POD_NAME" 8443:9443 --namespace stackgres
```
Once you open the web console in the browser, you will need to accept the self-signed certificate to continue to the page. This certificate is generated during the installation and can be customized.

It's also possible to access the web console using an ingress controller or other mechanisms.

The default administrator's username is `admin`, and the password is generated automatically and can be obtained via the following command:

```bash
$ kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "%s\n" (.data.clearPassword | base64decode) }}'
```

You should see something like the following screenshot:

![StackGres Web Console](web-console-1.png)

![StackGres Web Console](web-console-2.png)
