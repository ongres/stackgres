---
title: "GKE"
weight: 2
url: install/prerequisites/k8s/gke
description: Google Kubernetes Engine (GKE) is a managed, production-ready environment for running containerized applications.
---

To create a [Google Kubernetes Engine](https://cloud.google.com/kubernetes-engine/) you have to do so in a [Google Cloud Project](https://cloud.google.com/resource-manager/docs/creating-managing-projects).
 Assuming you have already do so and that you have installed the [gcloud CLI](https://cloud.google.com/sdk/gcloud/)
 you can proceed by creating the kubernetes cluster with following characteristics (that you may change):

> We recommend disable auto upgrade

 * Project: my-project
 * Cluster name: stackgres
 * GKE version: 1.21.3-gke.2001
 * Zone: us-west1
 * Node locations: us-west1-a,us-west1-b,us-west1-c
 * Machine type: e2-standard-4
 * Number of nodes: 3
 * Disk size 100GB
 * Node auto upgrade/repair disabled

```
gcloud container clusters create "stackgres" \
 --project "my-project" \
 --region "us-west1" \
 --no-enable-basic-auth \
 --cluster-version "1.21.3-gke.2001" \
 --no-enable-ip-alias \
 --release-channel "None" \
 --machine-type "e2-standard-4" \
 --image-type "COS_CONTAINERD" \
 --disk-type "pd-standard" \
 --disk-size "100" \
 --metadata disable-legacy-endpoints=true \
 --num-nodes "3" \
 --no-enable-autoupgrade \
 --no-enable-autorepair \
 --node-locations "us-west1-a","us-west1-b","us-west1-c" 
```

The output will be similar to the following:
```
Note: Your Pod address range (`--cluster-ipv4-cidr`) can accommodate at most 1008 node(s).
Creating cluster stackgres in us-west1... Cluster is being health-checked (master is healthy)...done.                                                                                                              
Created [https://container.googleapis.com/v1beta1/projects/my-project/zones/us-west1/clusters/stackgres].
To inspect the contents of your cluster, go to: https://console.cloud.google.com/kubernetes/workload_/gcloud/us-west1/stackgres?project=my-project
kubeconfig entry generated for stackgres.
NAME       LOCATION            MASTER_VERSION    MASTER_IP      MACHINE_TYPE   NODE_VERSION      NUM_NODES  STATUS
stackgres  southamerica-east1  1.20.10-gke.1600  34.95.157.116  e2-standard-4  1.20.10-gke.1600  9          RUNNING

...
```

To access your cluster via `kubectl`, you can conveniently configure your kubeconfig via the following command:

```
gcloud container clusters get-credentials --region us-west1 --project my-project stackgres

# if your gcloud / GKE version expects to use the GKE gcloud auth plugin, you might need to set this env variable:
export USE_GKE_GCLOUD_AUTH_PLUGIN=True; gcloud container clusters get-credentials --region us-west1 --project my-project stackgres
```

You may also want to cleanup compute disks used by persistence volumes that may have been created:

```
$ kubectl get nodes
NAME                                       STATUS   ROLES    AGE   VERSION
gke-stackgres-default-pool-2b3329f9-1v10   Ready    <none>   91s   v1.25.6-gke.200
gke-stackgres-default-pool-2b3329f9-4lgg   Ready    <none>   91s   v1.25.6-gke.200
gke-stackgres-default-pool-2b3329f9-8z0h   Ready    <none>   90s   v1.25.6-gke.200
gke-stackgres-default-pool-6efa0dd8-243j   Ready    <none>   91s   v1.25.6-gke.200
gke-stackgres-default-pool-6efa0dd8-mmnn   Ready    <none>   91s   v1.25.6-gke.200
gke-stackgres-default-pool-6efa0dd8-qc56   Ready    <none>   90s   v1.25.6-gke.200
gke-stackgres-default-pool-e04d99f3-79cr   Ready    <none>   92s   v1.25.6-gke.200
gke-stackgres-default-pool-e04d99f3-d4f7   Ready    <none>   91s   v1.25.6-gke.200
gke-stackgres-default-pool-e04d99f3-g1gr   Ready    <none>   91s   v1.25.6-gke.200
```

To clean up the Kubernetes cluster you can run the following command:

```
gcloud container clusters delete stackgres \
 --project my-project \
 --region us-west1 \
 --quiet
```

You may also want to clean up compute disks used by persistence volumes that may have been created:

```
gcloud compute disks list --project my-project --filter "zone:us-west1" --quiet | tail -n+2 | sed 's/ \+/|/g' | cut -d '|' -f 1-2 \
  | grep '^gke-stackgres-[0-9a-f]\{4\}-pvc-[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}|' \
  | xargs -r -n 1 -I % sh -ec "gcloud -q compute disks delete --project my-project --zone \"\$(echo '%' | cut -d '|' -f 2)\" \"\$(echo '%' | cut -d '|' -f 1)\""
```
