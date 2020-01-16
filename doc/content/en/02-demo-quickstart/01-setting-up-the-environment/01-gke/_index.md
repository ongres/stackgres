---
title: "GKE"
weight: 1
---

To create a Google Kubernetes Engine you have to do so in a [Google Cloud Project](https://cloud.google.com/resource-manager/docs/creating-managing-projects).
 Assuming you have already do so and that you have installed the [gcloud CLI](https://cloud.google.com/sdk/gcloud/)
 you can proceed by creating the kubernetes cluster with following characteristics (that you may change):

 * Project: my-project
 * Cluster name: stackgres
 * GKE version: 1.13.12-gke.17
 * Zone: us-west1-a
 * Node locations: us-west1-a,us-west1-b,us-west1-c
 * Machine type: n1-standard-1
 * # of nodes: 3
 * Disk size 20GB
 * Node auto upgrade disabled

```
gcloud -q beta container \
  --project my-project \
  clusters create stackgres \
  --cluster-version 1.13.12-gke.17 \
  --zone us-west1-a \
  --node-locations us-west1-a,us-west1-b,us-west1-c \
  --machine-type n1-standard-1 \
  --disk-size "20" \
  --num-nodes 3 \
  --no-enable-autoupgrade
```

To cleanup the kubernetes cluster you may issue following command:

```
gcloud -q beta container \
  --project my-project \
  clusters delete stackgres \
  --zone us-west1-a \
```