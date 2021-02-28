---
title: AWS EKS
weight: 2
url: tutorial/prerequisites/kubernetes-environment/aws-eks
---

You will need to have installed the [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html)
installed and configured, with the appropriate credentials to be able to create EKS clusters and create S3 buckets and
policies. You will also need to have installed [eksctl](https://eksctl.io/introduction/#installation).

To create a cluster, run the following commands, making any necessary adjustment to the variables:

```bash
export AWS_PROFILE=    # optional
export AWS_REGION=us-east-2
export K8S_CLUSTER_NAME=stackgres
eksctl --region $AWS_REGION create cluster \
	--name $K8S_CLUSTER_NAME \
	--node-type m5a.2xlarge --node-volume-size 100 --nodes 3 \
	--zones ${AWS_REGION}a,${AWS_REGION}b,${AWS_REGION}c \
	--version 1.17
```

This process takes around 20 minutes. The output should be similar to:

```plain
2021-02-28 20:26:56 [ℹ]  eksctl version 0.39.0                                                         
2021-02-28 20:26:56 [ℹ]  using region us-east-2
2021-02-28 20:26:56 [ℹ]  subnets for us-east-2a - public:192.168.0.0/19 private:192.168.96.0/19
2021-02-28 20:26:56 [ℹ]  subnets for us-east-2b - public:192.168.32.0/19 private:192.168.128.0/19                                                                                                              
2021-02-28 20:26:56 [ℹ]  subnets for us-east-2c - public:192.168.64.0/19 private:192.168.160.0/19
2021-02-28 20:26:57 [ℹ]  nodegroup "ng-092df631" will use "ami-0ba2dda6dd6a9e644" [AmazonLinux2/1.17]
2021-02-28 20:26:58 [ℹ]  using Kubernetes version 1.17        
2021-02-28 20:26:58 [ℹ]  creating EKS cluster "stackgres" in "us-east-2" region with un-managed nodes
2021-02-28 20:26:58 [ℹ]  will create 2 separate CloudFormation stacks for cluster itself and the initial nodegroup
2021-02-28 20:26:58 [ℹ]  if you encounter any issues, check CloudFormation console or try 'eksctl utils describe-stacks --region=us-east-2 --cluster=stackgres'
2021-02-28 20:26:58 [ℹ]  CloudWatch logging will not be enabled for cluster "stackgres" in "us-east-2"                                                                                                         
2021-02-28 20:26:58 [ℹ]  you can enable it with 'eksctl utils update-cluster-logging --enable-types={SPECIFY-YOUR-LOG-TYPES-HERE (e.g. all)} --region=us-east-2 --cluster=stackgres'
2021-02-28 20:26:58 [ℹ]  Kubernetes API endpoint access will use default of {publicAccess=true, privateAccess=false} for cluster "stackgres" in "us-east-2"
2021-02-28 20:26:58 [ℹ]  2 sequential tasks: { create cluster control plane "stackgres", 2 sequential sub-tasks: { wait for control plane to become ready, create nodegroup "ng-092df631" } }
2021-02-28 20:26:58 [ℹ]  building cluster stack "eksctl-stackgres-cluster"
2021-02-28 20:26:59 [ℹ]  deploying stack "eksctl-stackgres-cluster"
2021-02-28 20:39:53 [ℹ]  waiting for CloudFormation stack "eksctl-stackgres-cluster"
2021-02-28 20:39:56 [ℹ]  building nodegroup stack "eksctl-stackgres-nodegroup-ng-092df631"
2021-02-28 20:39:56 [ℹ]  --nodes-min=3 was set automatically for nodegroup ng-092df631
2021-02-28 20:39:56 [ℹ]  --nodes-max=3 was set automatically for nodegroup ng-092df631
2021-02-28 20:39:57 [ℹ]  deploying stack "eksctl-stackgres-nodegroup-ng-092df631"
2021-02-28 20:44:24 [ℹ]  waiting for CloudFormation stack "eksctl-stackgres-nodegroup-ng-092df631"
2021-02-28 20:44:25 [ℹ]  waiting for the control plane availability...
2021-02-28 20:44:25 [✔]  saved kubeconfig as "/home/aht/.kube/config"
2021-02-28 20:44:25 [ℹ]  no tasks
2021-02-28 20:44:25 [✔]  all EKS cluster resources for "stackgres" have been created
2021-02-28 20:44:26 [ℹ]  adding identity "arn:aws:iam::292778140943:role/eksctl-stackgres-nodegroup-ng-092-NodeInstanceRole-1ITJQEMJMGD8" to auth ConfigMap
2021-02-28 20:44:26 [ℹ]  nodegroup "ng-092df631" has 0 node(s)
2021-02-28 20:44:26 [ℹ]  waiting for at least 3 node(s) to become ready in "ng-092df631"
2021-02-28 20:44:52 [ℹ]  nodegroup "ng-092df631" has 3 node(s)
2021-02-28 20:44:52 [ℹ]  node "ip-192-168-13-0.us-east-2.compute.internal" is ready
2021-02-28 20:44:52 [ℹ]  node "ip-192-168-53-249.us-east-2.compute.internal" is ready
2021-02-28 20:44:52 [ℹ]  node "ip-192-168-94-213.us-east-2.compute.internal" is ready
2021-02-28 20:44:53 [✔]  EKS cluster "stackgres" in "us-east-2" region is ready
```

Once your EKS cluster is created, you should have your `~/.kube/config` populated, being able to run:

```bash
kubectl cluster-info
```

and get an output similar to:

```plain
Kubernetes control plane is running at https://6E48B1E2BBDE5960F174FD2D04C1F554.gr7.us-east-2.eks.amazonaws.com
CoreDNS is running at https://6E48B1E2BBDE5960F174FD2D04C1F554.gr7.us-east-2.eks.amazonaws.com/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```
