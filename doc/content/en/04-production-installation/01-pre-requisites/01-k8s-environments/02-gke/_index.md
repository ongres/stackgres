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

```bash
gcloud beta container \
 --project "my-project" clusters create "stackgres" \
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

Output will be similar to this:
```plain
WARNING: Starting with version 1.18, clusters will have shielded GKE nodes by default.
WARNING: Your Pod address range (`--cluster-ipv4-cidr`) can accommodate at most 1008 node(s). 
Creating cluster stackgres in us-west1...done.                                                                                                                                                                                       
Created [https://container.googleapis.com/v1beta1/projects/postgresql-support-dev/zones/southamerica-east1/clusters/stackgres].
To inspect the contents of your cluster, go to: https://console.cloud.google.com/kubernetes/workload_/gcloud/southamerica-east1/stackgres?project=postgresql-support-dev
kubeconfig entry generated for stackgres.
NAME       LOCATION            MASTER_VERSION    MASTER_IP      MACHINE_TYPE   NODE_VERSION      NUM_NODES  STATUS
stackgres  southamerica-east1  1.20.10-gke.1600  34.95.157.116  e2-standard-4  1.20.10-gke.1600  9          RUNNING

...
```

To cleanup the kubernetes cluster you may issue following command:
```bash
gcloud -q beta container \
  --project "my-project" \
  clusters delete stackgres \
  --region us-west1
```

You may also want to cleanup compute disks used by persistence volumes that may have been created:

```shell
gcloud -q compute disks list --project my-project --filter "zone:us-west1" | tail -n+2 | sed 's/ \+/|/g' | cut -d '|' -f 1-2 \
  | grep '^gke-stackgres-[0-9a-f]\{4\}-pvc-[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}|' \
  | xargs -r -n 1 -I % sh -ec "gcloud -q compute disks delete --project my-project --zone \"\$(echo '%' | cut -d '|' -f 2)\" \"\$(echo '%' | cut -d '|' -f 1)\""
```