---
title: Post-installation
weight: 3
url: install/post/install
---

## Exposing the UI

StackGres publish a Web UI that can be accessed by pointing to port 443 with DNS
`stackgres-operator.stackgres.svc`. It is not recommended to expose this Web UI to public
internet without protecting it with some secure access bridge. 

A good fit for this purpose would be using the [ingress nginx project](https://github.com/kubernetes/ingress-nginx/) by configuring an [external OAUTH authentication](https://kubernetes.github.io/ingress-nginx/examples/auth/oauth-external-auth/).

You can expose the UI using the bellow command:

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