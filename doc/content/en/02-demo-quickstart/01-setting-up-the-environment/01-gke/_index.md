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
 * Number of nodes: 3
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

```
WARNING: Currently VPC-native is not the default mode during cluster creation. In the future, this will become the default mode and can be disabled using `--no-enable-ip-alias` flag. Use `--[no-]enable-ip-alias` flag to suppress this warning.
WARNING: Starting in 1.12, default node pools in new clusters will have their legacy Compute Engine instance metadata endpoints disabled by default. To create a cluster with legacy instance metadata endpoints disabled in the default node pool, run `clusters create` with the flag `--metadata disable-legacy-endpoints=true`.
WARNING: Your Pod address range (`--cluster-ipv4-cidr`) can accommodate at most 1008 node(s). 
This will enable the autorepair feature for nodes. Please see https://cloud.google.com/kubernetes-engine/docs/node-auto-repair for more information on node autorepairs.
Creating cluster stackgres in us-west1-a... Cluster is being health-checked (master is healthy)...done.
Created [https://container.googleapis.com/v1beta1/projects/my-project/zones/us-west1-a/clusters/stackgres].
To inspect the contents of your cluster, go to: https://console.cloud.google.com/kubernetes/workload_/gcloud/us-west1-a/stackgres?project=my-project
kubeconfig entry generated for stackgres.
NAME       LOCATION    MASTER_VERSION  MASTER_IP       MACHINE_TYPE   NODE_VERSION    NUM_NODES  STATUS
stackgres  us-west1-a  1.13.12-gke.17  35.233.185.142  n1-standard-1  1.13.12-gke.17  9          RUNNING
```

To cleanup the kubernetes cluster you may issue following command:

```
gcloud -q beta container \
  --project my-project \
  clusters delete stackgres \
  --zone us-west1-a \
```

You may also want to cleanup compute disks used by persistence volumes that may have been created:

```shell
gcloud -q compute disks list --project my-project --filter "zone:us-west1-a" | tail -n+2 | cut -d ' ' -f 1 \
    | grep '^gke-stackgres-[0-9a-f]\{4\}-pvc-[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}$' \
    | xargs -r -n 1 -I % gcloud -q compute disks delete --project my-project --zone us-west1-a %
```