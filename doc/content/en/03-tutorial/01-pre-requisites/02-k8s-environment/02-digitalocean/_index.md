---
title: DigitalOcean
weight: 2
url: tutorial/prerequisites/kubernetes-environment/digitalocean-k8s
description: Details about the requirements to set up DigitalOcean kubernetes to run the tutorial.
---

You will need to have installed the [doctl](https://docs.digitalocean.com/reference/doctl/how-to/install/)
installed and configured, with the appropriate credentials to be able to create a Kuberentes cluster.

To create a cluster, run the following commands, making any necessary adjustment to the variables:

```bash
export DO_REGION=nyc1
export DO_NODE_SIZE=s-4vcpu-8gb
export DIGITALOCEAN_CONTEXT=default ## change if needed
export DIGITALOCEAN_PROJECT=stackgres-playground ## change me!
export K8S_CLUSTER_NAME=stackgres ## change me!
export K8S_CLUSTER_NODES=3
export K8S_VERSION=1.19.6
time doctl kubernetes cluster create ${K8S_CLUSTER_NAME} \
	--region ${DO_REGION} \
	--size ${DO_NODE_SIZE} \
	--count ${K8S_CLUSTER_NODES} \
	--version ${K8S_VERSION}-do.0 \
	--wait
```

This process takes around 6 minutes. The output should be similar to:

```plain
Notice: Cluster is provisioning, waiting for cluster to be running
..................................................................
Notice: Cluster created, fetching credentials
Notice: Adding cluster credentials to kubeconfig file found in "/home/seba/.kube/config"
Notice: Setting current-context to do-nyc1-stackgres
ID                                      Name         Region    Version         Auto Upgrade    Status     Node Pools
00a86a85-28e8-45f4-a118-e718a1f46609    stackgres    nyc1      1.18.14-do.0    false           running    stackgres-default-pool
```

Once your cluster is created, you should have your `~/.kube/config` populated, being able to run:

```bash
kubectl cluster-info
```

and get an output similar to:

```plain
Kubernetes control plane is running at https://00a86a85-28e8-45f4-a118-e718a1f46609.k8s.ondigitalocean.com
CoreDNS is running at https://00a86a85-28e8-45f4-a118-e718a1f46609.k8s.ondigitalocean.com/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```
