---
Title: Install K3S and StackGres locally
weight: 7
url: /runbooks/k3s-stackgres
description: How to install locally K3S and f Stackgres
showToc: true
---

This runbook will show you how to install (K3s)[https://www.k3s.io] a simplified and secure version of Kubernetes which will require lower resources to create an environment, where we will deploy StackGres. 


In the first part of the runbook it will demonstrate how to install a single-node setup, in the second part it will approach a multi-node installation.


We recommend to setup the var `INSTALL_K3S_VERSION`=v1.27.6+k3s1 to a fixed Kubernetes version that is among those supported by StackGres (https://stackgres.io/doc/1.5/install/prerequisites/k8s/), and that relative version on the K3s releases page (see https://github.com/k3s-io/k3s/releases) that will match teh major Kubernetes version supported by StackGres. Currently, the version that should be on that Runbook should be v1.27.6+k3s1, as it is the latest Kubernetes version is v1.27 available from K3s.


** Install a single-node setup steps:**

To proceed with the installation please execute:

**1 - Install K3s:** 

```
curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION=v1.25.9+k3s1 sh -
```

**2 -  Add the config to kube config**

If you already have an existent Kubernetes configuration, please follow the (documentation)[https://able8.medium.com/how-to-merge-multiple-kubeconfig-files-into-one-36fc987c2e2f] for the correct management.


If you do not have any Kubernetes config please execute: 
```
sudo k3s kubectl config view --raw >> ~/.kube/config 
```

**3 - Check the status of the nodes:**

```
kubectl get nodes
```
Output: 
```
NAME      STATUS   ROLES                  AGE    VERSION
jose-pc   Ready    control-plane,master   125d   v1.25.9+k3s1

```

For the next steps to install StackGres, please follow the steps on the [Install with Helm](https://stackgres.io/doc/1.5/install/helm/).


** Install a multi-node setup steps:**

**1 - collect the token from the primary**

Execute the command on the primary: 
```
sudo cat /var/lib/rancher/k3s/server/node-token
```

Output example:
```
K10292d9a0b77f4cf2918312272c393b856789e91426029dba51728e4a196fb989e::server:e68f8c6edd588820a765a68b0c41d666
```

**2 - Execute the setup with the variables on the additional nodes**

Execute the command to install K3s on the additional node:

```
curl -sfL https://get.k3s.io | K3S_URL=https://<IP_SERVER_PRIMARY>:6443 K3S_TOKEN=<MY_TOKEN_PRIMARY> sh -
```

**3 - Check the status of the nodes:**

Execute the following command on the primary to check the status of the cluster: 

```
kubectl get nodes
```
Output: 
```
NAME     STATUS   ROLES                  AGE   VERSION
node-1   Ready    control-plane,master   45m   v1.27.5+k3s1
node-2   Ready    <none>                 63s   v1.27.5+k3s1
```


**To deinstall K3S you should execute:** 

```
sudo /usr/local/bin/k3s-uninstall.sh
```