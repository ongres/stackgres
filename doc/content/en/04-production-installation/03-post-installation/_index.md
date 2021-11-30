---
title: Post-installation
weight: 3
url: install/post/install
description: Details about how to expose and connect to the UI.
showToc: true
---

## Exposing the UI

StackGres publish a Web UI that can be accessed by pointing to port 443 with DNS
`stackgres-operator.stackgres`. It is not recommended to expose this Web UI to public
internet without protecting it with some secure access bridge. 

You can expose the UI using the command below:

```
POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward ${POD_NAME} --address 0.0.0.0 8443:9443 --namespace stackgres
```

## Connect to the UI

Connect to `https://<your-host>:8443/admin/` and get your UI credentials:

```bash
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\n" (.data.k8sUsername | base64decode) }}'
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "password = %s\n" (.data.clearPassword | base64decode) }}'
```