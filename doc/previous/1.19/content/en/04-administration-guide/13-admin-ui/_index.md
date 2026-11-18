---
title: Admin UI
weight: 10
url: /administration/adminui
description: This page contains details about how to connect on the StackGres admin UI.
---

StackGres includes a web UI which offers to display and modify all StackGres resources.
The web UI is available inside the cluster via the service `stackgres-restapi.stackgres`.

## Exposing the UI

To expose the web UI to a local environment, we can forward a local port to the `stackgres-restapi` pod.
This is only for local test purposes.
It is not recommended to expose the web UI to the public internet without protecting it with additional security measures.

You can forward the port using the following command:

```
POD_NAME=$(kubectl get pods --namespace stackgres -l "stackgres.io/restapi=true" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward ${POD_NAME} --address 0.0.0.0 8443:9443 --namespace stackgres
```

## Connecting to the UI

You can connect to `https://localhost:8443/admin/` and log in with the StackGres credentials:

```
kubectl get secret -n stackgres stackgres-restapi-admin --template '{{ printf "username = %s\n" (.data.k8sUsername | base64decode) }}'
kubectl get secret -n stackgres stackgres-restapi-admin --template '{{ printf "password = %s\n" (.data.clearPassword | base64decode) }}'
```

After login, you should see something like the following screenshots:

![StackGres Web Console](web-console-1.png)

![StackGres Web Console](web-console-2.png)

## Connecting via LoadBalancer

If StackGres has been installed with a configured `LoadBalancer` (e.g. via the Helm installation via parameter `--set-string adminui.service.type=LoadBalancer`), you can query the URL and use it to connect to the admin UI directly:

```
kubectl -n stackgres get svc --field-selector metadata.name=stackgres-restapi

NAME                TYPE           CLUSTER-IP      EXTERNAL-IP                                                              PORT(S)         AGE
stackgres-restapi   LoadBalancer   10.100.165.13   aa372eefc1630469f95e64d384caa004-833850176.eu-west-1.elb.amazonaws.com   443:32194/TCP   47m
```

Open the external URL in the web browser prefixing it with `https://` , as in:

```
https://aa372eefc1630469f95e64d384caa004-833850176.eu-west-1.elb.amazonaws.com
```
