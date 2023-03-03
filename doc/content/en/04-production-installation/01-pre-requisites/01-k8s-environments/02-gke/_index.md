---
title: "GKE"
weight: 2
url: install/prerequisites/k8s/gke
description: Google Kubernetes Engine (GKE) is a managed, production-ready environment for running containerized applications.
---

You can create a [Google Kubernetes Engine](https://cloud.google.com/kubernetes-engine/) cluster in your [Google Cloud Project](https://cloud.google.com/resource-manager/docs/creating-managing-projects).
After you have created a project and installed the [gcloud CLI](https://cloud.google.com/sdk/gcloud/), you can proceed by creating the Kubernetes cluster.
We use the following characteristics which you might change:

> We recommend to disable auto upgrades

 * Project: `my-project`
 * Cluster name: `stackgres`
 * Default GKE version
 * Zone: `us-west1`
 * Node locations: `us-west1-a`, `us-west1-b`, `us-west1-c`
 * Machine type: `e2-standard-4`
 * Number of nodes: 3
 * Disk size: 100 GB
 * Node auto upgrade/repair disabled

```bash
gcloud container clusters create "stackgres" \
 --project "my-project" \
 --region "us-west1" \
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
```plain
Note: Your Pod address range (`--cluster-ipv4-cidr`) can accommodate at most 1008 node(s).
Creating cluster stackgres in us-west1... Cluster is being health-checked (master is healthy)...done.                                                                                                              
Created [https://container.googleapis.com/v1beta1/projects/my-project/zones/us-west1/clusters/stackgres].
To inspect the contents of your cluster, go to: https://console.cloud.google.com/kubernetes/workload_/gcloud/us-west1/stackgres?project=my-project
kubeconfig entry generated for stackgres.
NAME       LOCATION  MASTER_VERSION  MASTER_IP     MACHINE_TYPE   NODE_VERSION    NUM_NODES  STATUS
stackgres  us-west1  1.25.6-gke.200  34.105.42.91  e2-standard-4  1.25.6-gke.200  9          RUNNING

...
```

To access your cluster via `kubectl`, you can conveniently configure your kubeconfig via the following command:

```bash
gcloud container clusters get-credentials --region us-west1 --project my-project stackgres

# if your gcloud / GKE version expects to use the GKE gcloud auth plugin, you might need to set this env variable:
export USE_GKE_GCLOUD_AUTH_PLUGIN=True; gcloud container clusters get-credentials --region us-west1 --project my-project stackgres
```

Then you should be able to access your cluster via `kubectl`:

```bash
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

```bash
gcloud container clusters delete stackgres \
 --project my-project \
 --region us-west1 \
 --quiet
```

You may also want to clean up compute disks used by persistence volumes that may have been created:

```bash
gcloud compute disks list --project my-project --filter "zone:us-west1" --quiet | tail -n+2 | sed 's/ \+/|/g' | cut -d '|' -f 1-2 \
  | grep '^gke-stackgres-[0-9a-f]\{4\}-pvc-[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}|' \
  | xargs -r -n 1 -I % sh -ec "gcloud -q compute disks delete --project my-project --zone \"\$(echo '%' | cut -d '|' -f 2)\" \"\$(echo '%' | cut -d '|' -f 1)\""
```