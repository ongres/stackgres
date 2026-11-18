---
title: Cluster with Istio
weight: 1
url: /administration/cluster-creation/services-mesh-integration/istio
aliases: [ /install/prerequisites/services-mesh-integration/istio, /install/services-mesh-integration/istio ]
description: Details about how to work in a K8s cluster with Istio
---

StackGres already uses an Envoy sidecar container.
The sidecar injected by Istio (`istio-proxy`) is not compatible with StackGres pods at the moment.
In a Kubernetes cluster with Istio installed, you need to annotate the StackGres cluster to avoid Istio's sidecar injection.

## Annotate StackGres Pods

Before you create a StackGres cluster, make sure you add the annotation `sidecar.istio.io/inject: 'false'` to the pods, as shown below:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
  metadata:
    annotations:
      clusterPods:
        sidecar.istio.io/inject: 'false'
# [...]
```

This will prevent your pods from entering a `CrashLoopBackOff` state.