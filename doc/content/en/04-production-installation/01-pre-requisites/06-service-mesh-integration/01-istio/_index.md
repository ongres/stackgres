---
title: Istio
weight: 1
url: install/prerequisites/services-mesh-integration/istio
description: |
  Details about how to work in a k8s cluster with Istio
showToc: true
---

StackGres already has an implementation of Envoy, the sidecar injected by Istio (istio-proxy) it's not compatible at the moment.
In a k8s cluster with Istio installed you just only need to Annotate the StackGres cluster to avoid the sidecar injection from Istio.

## Annotate StackGres pods

Before you create a StackGres cluster make sure you add the annotation `sidecar.istio.io/inject: 'false'` to the pods as is shown below:

```
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: gitlab-db
  name: gitlab-db
spec:
  metadata:
    annotations:
      pods:
        sidecar.istio.io/inject: 'false'
  postgresVersion: '12.3'
  instances: 3
```

This will avoid your pods enter in a `CrashLoopBackOff` state.

