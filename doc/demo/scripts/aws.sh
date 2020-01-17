#!/bin/bash
## Export variablesble
export namecluster=<Your cluster's name >
export region=<You region  of your project>
export version=<version cluster Kubernetes>
export nodetype=<node type>
export nodes=<number of nodes>
export minnode=<Minimum nodes>1
export maxnode=<Maximum nodes>


## Create cluster
eksctl create cluster \
--name $namecluster \
--version $version \
--region $region \
--nodegroup-name standard-workers \
--node-type $nodetype \
--nodes $nodes \
--nodes-min $minnode \
--nodes-max $maxnode \
--node-ami  auto

## Create your kubeconfig file with the AWS CLI

aws eks --region $region update-kubeconfig --name $namecluster
