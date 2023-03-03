---
title: Storage Classes
weight: 1
url: install/prerequisites/storage/storageclass
description: "Details about how to set up and configure the storage classes. Storage classes are used by the database clusters and will impact the performance and availability of the cluster."
---

When setting up a K8s environment, by default, the storage class is not set up to not expand the volumes when they're filling up.
It is recommended, however, to allow for expansion, which is configured with the parameter `allowVolumeExpansion: true`.

We recommend creating a new storage class with these parameters:

- `reclaimPolicy: Retain`
- `volumeBindingMode: WaitForFirstConsumer`
- `allowVolumeExpansion: true`

Here is an example for an AWS environment:

```
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: io1
provisioner: kubernetes.io/aws-ebs
parameters:
  type: io1
  iopsPerGB: "50"
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

and if you're using GKE:

```
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ssd
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-ssd
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

Check the [storage class documentation](https://kubernetes.io/docs/concepts/storage/storage-classes/) for more details and other providers.

Don't forget to use your custom storage class when you create a cluster.
You can check the required parameters in [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}).

**Important note:**
Make sure you include these parameters in order to avoid the following errors:

- Autoscaler not working as expected:
`cluster-autoscaler  pod didn't trigger scale-up (it wouldn't fit if a new node is added)`

- Volumes not assigned:
`N node(s) had no available volume zone`

- Losing data by accidentally removing a volume:
`reclaimPolicy: Retain` will guarantee the volume is not deleted when a claim no longer exists.

