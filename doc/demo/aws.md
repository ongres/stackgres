# Prerequisites
Make sure you have the following prerequisites:

- [Kubectl ](https://kubernetes.io/es/docs/tasks/tools/install-kubectl/)

- [Helm ](https://helm.sh/docs/using_helm/#installing-helm)
>  Currently the only version supported is `helm menor a 3`  for StackGres


- [helm tiller service](https://helm.sh/docs/using_helm/#initialize-helm-and-install-tiller)
  > helm init --client-only

  or
- [tillerless](https://github.com/rimusz/helm-tiller)

- A Kubernetes cluster (Localy using [Minikube](https://kubernetes.io/es/docs/tasks/tools/install-minikube/), [kind](https://github.com/kubernetes-sigs/kind) ,etc. or in the cloud)


For the demo installation will use AWS,  therefore,  you need to install  eksctl for Linux


- [eksctl](https://github.com/weaveworks/eksctl/blob/master/README.md)

> it is necessary to have aws installed with a version> 1.6


- [aws](https://docs.aws.amazon.com/es_es/cli/latest/userguide/install-linux.html)

    *  Create role in console AWS for eks

# Installation and configuration of StackGres
## 1.- Clone StackGres repository

`
git clone https://gitlab.com/ongresinc/stackgres.git
cd stackgres
`

> You can work this configuration in any k8s cluster, we going to use a GCloud cluster.

## 2.- Create the k8s cluster on aws:
#### 2.1.- Export the variables for cluster
```
export namecluster = name of my cluster
export version     =  version you will used for kubernetes
export region      =  region of deployment
export nodetype    = Node Size
export nodes       = Total of nodes
export minnode     = Minimal nodes
export maxnode     = Maxima nodes
```
> For more information about [Node Size ](https://aws.amazon.com/es/ec2/instance-types/)

For example :
```
export namecluster=prod
export region=us-west-2
export version=1.14
export nodetype=t3.medium
export nodes=3
export minnode=1
export maxnode=4
```
> Currently the only version supported is  1.12.0 - 1.16.0 for [kubernetes](https://docs.aws.amazon.com/eks/latest/userguide/kubernetes-versions.html)

#### 2.2.- create cluster
```
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
```

#### 2.3.-   Create your kubeconfig file with the AWS CLI

`aws eks --region $region update-kubeconfig --name $namecluster`


## 3.-  Now, verify cluster information
`kubectl cluster-info`

and

`kubectl get pods`


## 4.- Make sure you have a tiller service running

If you are using [tillerless](https://github.com/rimusz/helm-tiller) configuration run:

`helm tiller start`

or

`helm init --client-only`

## 5.- Helm install operator of prometheus
`helm install --name prometheus-operator stable/prometheus-operator
`
## 6.- Install StackGres Operator
`helm install --name stackgres-operator stackgres-k8s/install/helm/stackgres-operador/`

## 7.- Now, we are going to create the StackGres cluster

### 7.1.- Create cluster
`helm  install --name stackgres-cluster stackgres-k8s/install/helm/stackgres-cluster/`

> If you do not want to use the Cluster by default, you can generate the CRDs one by one, [in this way](cr.md)

#### 7.1.1.- Adding another cluster
`helm upgrade  stackgres-cluster --version 3 stackgres-k8s/install/helm/stackgres-cluster/ --set-string cluster.instances=3`

> Is necessary you have the resources for deployment
### 7.2.- Verify the cluster

`kubectl get pods --all-namespaces -o json | jq '.items' | jq -c '.[] | select (.metadata.labels.app == "StackGres") | select (.metadata.labels.cluster == "true")' | jq '.metadata.name' -r`
### 7.4.- Verify Patroni cluster status

`kubectl exec -it stackgres-0 -c patroni -- patronictl list`

### 7.5.- Connect a PostgreSQL

`kubectl exec -it stackgres-0 -c postgres-util -- psql`

> Note: This container have all the postgres binaries and you can use it to check PostgreSQL functionality  

## 8.-  Create a port-forward to access the web UI

`kubectl port-forward  "$(kubectl get pods --all-namespaces -o json | jq '.items' | jq -c '.[] | select (.metadata.name | contains("stackgres-orator"))' | jq '.metadata.name' -r)" 8883:443
`

### 8.1.- Access the Web UI
[Go to UI](https://127.0.0.1:8443 )

## 9.- To delete this cluster
```
helm tiller stop
eksctl delete cluster --name $namecluster --region $region

```

## 10- END
:stuck_out_tongue:
