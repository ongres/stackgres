---
title: Post-installation
weight: 3
---

## Connect to the UI

StackGres publish a Web UI that can be accessed by pointing to port 443 with DNS
 `stackgres-operator.stackgres.svc`. It is not reccomended to expose this Web UI to public
 internet without protecting it with some secure access bridge. A good fit for this purpose would
 be using the [ingress nginx project](https://github.com/kubernetes/ingress-nginx/) by configuring
 an [external OAUTH authentication](https://kubernetes.github.io/ingress-nginx/examples/auth/oauth-external-auth/).