---
title: Istio
weight: 1
url: install/prerequisites/services-mesh-integration/istio
description: |
  Details about how to work in a K8s cluster with Istio
showToc: true
---

StackGres already uses an Envoy sidecar container.
The sidecar injected by Istio (`istio-proxy`) is not compatible with StackGres pods at the moment.
In a Kubernetes cluster with Istio installed, you need to annotate the StackGres cluster to avoid Istio's sidecar injection.

## Annotate StackGres Pods

Before you create a StackGres cluster, make sure you add the annotation `sidecar.istio.io/inject: 'false'` to the pods, as shown below:

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
  postgres:
    version: '12.3'
  instances: 3
```

This will avoid that your pods enter a `CrashLoopBackOff` state.
