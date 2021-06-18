---
title: "EKS"
weight: 1
url: demo/setenv/eks
description: Details about how to set up a EKS cluster.
---

This section will illustrate how to create an [AWS Elastic Kubernetes Service](https://aws.amazon.com/eks/).
 Assuming you have already installed the [aws CLI](https://aws.amazon.com/cli/) and the [eksctl CLI](https://github.com/weaveworks/eksctl)
 you can proceed by creating the kubernetes cluster with following characteristics (that you may change):

 * Cluster name: stackgres
 * Kubernetes version: 1.13
 * Zone: us-west-2
 * Machine type: m5.large
 * Number of nodes: 3
 * Disk size 20GB

```bash
eksctl create cluster --name stackgres \
  --region us-west-2 \
  --node-type m5.large \
  --node-volume-size 20 \
  --nodes 3 \
  --version 1.18
```

```bash
[ℹ]  eksctl version 0.13.0
[ℹ]  using region us-west-2
[ℹ]  setting availability zones to [us-west-2a us-west-2c us-west-2b]
[ℹ]  subnets for us-west-2a - public:192.168.0.0/19 private:192.168.96.0/19
[ℹ]  subnets for us-west-2c - public:192.168.32.0/19 private:192.168.128.0/19
[ℹ]  subnets for us-west-2b - public:192.168.64.0/19 private:192.168.160.0/19
[ℹ]  nodegroup "ng-308f6134" will use "ami-09bcf0b1f5b446c5d" [AmazonLinux2/1.13]
[ℹ]  using Kubernetes version 1.13
[ℹ]  creating EKS cluster "stackgres" in "us-west-2" region with un-managed nodes
[ℹ]  will create 2 separate CloudFormation stacks for cluster itself and the initial nodegroup
[ℹ]  if you encounter any issues, check CloudFormation console or try 'eksctl utils describe-stacks --region=us-west-2 --cluster=stackgres'
[ℹ]  CloudWatch logging will not be enabled for cluster "stackgres" in "us-west-2"
[ℹ]  you can enable it with 'eksctl utils update-cluster-logging --region=us-west-2 --cluster=stackgres'
[ℹ]  Kubernetes API endpoint access will use default of {publicAccess=true, privateAccess=false} for cluster "stackgres" in "us-west-2"
[ℹ]  2 sequential tasks: { create cluster control plane "stackgres", create nodegroup "ng-308f6134" }
[ℹ]  building cluster stack "eksctl-stackgres-cluster"
[ℹ]  deploying stack "eksctl-stackgres-cluster"
[ℹ]  building nodegroup stack "eksctl-stackgres-nodegroup-ng-308f6134"
[ℹ]  --nodes-min=3 was set automatically for nodegroup ng-308f6134
[ℹ]  --nodes-max=3 was set automatically for nodegroup ng-308f6134
[ℹ]  deploying stack "eksctl-stackgres-nodegroup-ng-308f6134"
[✔]  all EKS cluster resources for "stackgres" have been created
[✔]  saved kubeconfig as "/home/matteom/.kube/config-aws"
[ℹ]  adding identity "arn:aws:iam::661392101474:role/eksctl-stackgres-nodegroup-ng-NodeInstanceRole-C8R84QGP5UYX" to auth ConfigMap
[ℹ]  nodegroup "ng-308f6134" has 1 node(s)
[ℹ]  node "ip-192-168-66-45.us-west-2.compute.internal" is not ready
[ℹ]  waiting for at least 3 node(s) to become ready in "ng-308f6134"
[ℹ]  nodegroup "ng-308f6134" has 3 node(s)
[ℹ]  node "ip-192-168-2-185.us-west-2.compute.internal" is ready
[ℹ]  node "ip-192-168-58-166.us-west-2.compute.internal" is ready
[ℹ]  node "ip-192-168-66-45.us-west-2.compute.internal" is ready
[ℹ]  kubectl command should work with "/home/matteom/.kube/config-aws", try 'kubectl --kubeconfig=/home/matteom/.kube/config-aws get nodes'
[✔]  EKS cluster "stackgres" in "us-west-2" region is ready
```

To cleanup the kubernetes cluster you may issue following command:

```bash
eksctl delete cluster --name stackgres \
  --region us-west-2 \
  --wait
```

You may also want to cleanup EBS used by persistence volumes that may have been created:

```bash
aws ec2 describe-volumes --region us-west-2 --filters Name=tag-key,Values=kubernetes.io/cluster/stackgres \
  | jq -r '.Volumes[].VolumeId' | xargs -r -n 1 -I % aws ec2 delete-volume --region us-west-2 --volume-id %
```