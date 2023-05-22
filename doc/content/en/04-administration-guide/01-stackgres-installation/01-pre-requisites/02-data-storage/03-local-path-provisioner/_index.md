---
title: Local Path Provisioner
weight: 3
url: install/prerequisites/storage/local-path-provisioner
description: "Details about how to install Local Path Provisioner"
---

## Installing Local Path Provisioner

Most managed Kubernetes cluster and Kubernetes distributions comes with a StorageClass that allows to create a `PersistentVolume` on demand whenever a `PersistentVolumeClaim` requires it.

Some Kubernetes distributions do not provide such facility so that a simple way to overcome this lack is to install the [Local Path Provisioner](https://github.com/rancher/local-path-provisioner) that will provide a StorageClass that is backed by the node local disk. To install you may use the following command:

```
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/v0.0.24/deploy/local-path-storage.yaml
```