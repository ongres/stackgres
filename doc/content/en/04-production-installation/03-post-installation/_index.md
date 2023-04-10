---
title: Post-installation
weight: 3
url: install/post/install
description: Details about how to connect to the web UI.
showToc: true
---

StackGres includes a web UI which offers to display and modify all StackGres resources.
The web UI is available inside the cluster at the service `stackgres-operator.stackgres`.

## Exposing the UI

To expose the web UI to a local environment, we can forward a local port to the `stackgres-restapi` pod.
This is only for local test purposes.
It is not recommended to expose the web UI to the public internet without protecting it with additional security measure.

You can forward the port using the following command:

```
POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward ${POD_NAME} --address 0.0.0.0 8443:9443 --namespace stackgres
```

## Connecting to the UI

You can connect to `https://localhost:8443/admin/` and log in with the StackGres credentials:

```
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\n" (.data.k8sUsername | base64decode) }}'
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "password = %s\n" (.data.clearPassword | base64decode) }}'
```
