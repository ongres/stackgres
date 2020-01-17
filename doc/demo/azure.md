# Prerequisites
Make sure you have the following prerequisites:

- [Kubectl ](https://kubernetes.io/es/docs/tasks/tools/install-kubectl/)

- [Helm ](https://helm.sh/docs/using_helm/#installing-helm)
- [helm tiller service](https://helm.sh/docs/using_helm/#initialize-helm-and-install-tiller)
  > helm init --client-only

  or
- [tillerless](https://github.com/rimusz/helm-tiller)

- A Kubernetes cluster (Localy using [Minikube](https://kubernetes.io/es/docs/tasks/tools/install-minikube/), [kind](https://github.com/kubernetes-sigs/kind) ,etc. or in the cloud)


For the demo installation will use `AZURE`,  therefore,  you need to install  `az` for Linux


- [az](https://docs.microsoft.com/es-es/cli/azure/install-azure-cli-apt?view=azure-cli-latest)
- az login



# Installation and configuration of StackGres 0.8 version
## 1.- Clone StackGres repository

`
git clone https://gitlab.com/ongresinc/stackgres.git
cd stackgres
`

> You can work this configuration in any k8s cluster, we going to use a `AZ` cluster


## 2.- Create the k8s cluster on aws:
#### 2.1.- Export the variables for cluster
```
export namecluster = name of my cluster
export location    =  location of deployment
export nodes       = Total of nodes
export namegroup   = Name group for AZ
```
> for more information abount )

For example :
```
export namecluster=prod
export location=eastus
export nodes=3
export namegroup=testgroup
```
> Currently the only version supported is 1.14 for [kubernetes]()

#### 2.2.- Create group
`az group create --name $namegroup --location $location`
#### 2.3.- create cluster
`az aks create --name $namecluster --resource-group $namegroup --location $location --node-count $nodes`
#### 2.4.- Coneccion cluster
```
az aks install-cli
az aks get-credentials --resource-group $namegroup --name $namecluster
```

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
### 7.1.- Create cluster
`helm  --name stackgres-cluter stackgres-k8s/install/helm/stackgres-cluster/`

> If you do not want to use the Cluster by default, you can generate the CRDs one by one, [in this way](cr.md)

#### 7.1.1.- Add  other cluster
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

`kubectl port-forward  "$(kubectl get pods --all-namespaces -o json | jq '.items' | jq -c '.[] | select (.metadata.name | contains("stackgres-orator"))' | jq '.metadata.name' -r)" 8883:443`


### 8.1.- Access the Web UI
[Go to UI](https://127.0.0.1:8443 )

## 9.- To delete this cluster
```
helm tiller stop
az aks delete --name $namecluster --resource-group $namegroup
```

## 10- END
:stuck_out_tongue:
