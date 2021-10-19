---
title: Storage Classes
weight: 1
url: install/prerequisites/storage/storageclass
description: "Details about how to setup and configure the storage classes. Storage classes are used by the database clusters and will impact performance and availability of the cluster."
---

When setting up a K8s environment the Storage Class by default is created with one main restriction and this is represented with the parameter `allowVolumeExpansion: false` this will not allow you to expand your disk when these are filling up. It is recommended to create a new Storage Class with at least these next parameters:

- `reclaimPolicy: Retain`
- `volumeBindingMode: WaitForFirstConsumer`
- `allowVolumeExpansion: true`

Here is an example working in a AWS environment:

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

Check the [Storage Class documentation](https://kubernetes.io/docs/concepts/storage/storage-classes/) for more details and other providers.

Do not forget using your custom Storage Class when you create a cluster, check the required parameters in [Cluster Parameters]({{% relref "04-production-installation/06-cluster-parameters/#pods" %}})

**Important note:**
Make sure you include these parameters in order to avoid some of the next errors:

- Autoscaler not working as expected:
`cluster-autoscaler  pod didn't trigger scale-up (it wouldn't fit if a new node is added)`

- Volumes not assigned:
`N node(s) had no available volume zone`

- Losing data by accidentally removing a volume:
`reclaimPolicy: Retain` will guarantee the volume is not deleted when a claim no longer exist.

