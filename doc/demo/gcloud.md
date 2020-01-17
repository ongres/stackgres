# Prerequisites
Make sure you have the following prerequisites:

- [Kubectl ](https://kubernetes.io/es/docs/tasks/tools/install-kubectl/)

- [Helm ](https://helm.sh/docs/using_helm/#installing-helm)
- [helm tiller service](https://helm.sh/docs/using_helm/#initialize-helm-and-install-tiller)
  > helm init --client-only

  or
- [tillerless](https://github.com/rimusz/helm-tiller)

- A Kubernetes cluster (Localy using [Minikube](https://kubernetes.io/es/docs/tasks/tools/install-minikube/), [kind](https://github.com/kubernetes-sigs/kind) ,etc. or in the cloud)


For the demo installation will use GCP,  therefore,  you need to install  gcloud for Linux

[Gcloud](https://cloud.google.com/sdk/docs/quickstart-debian-ubuntu?hl=en-419)

# Installation and configuration of StackGres 0.7 version
## 1.- Clone StackGres repository

`
git clone https://gitlab.com/ongresinc/stackgres.git
cd stackgres
`

> You can work this configuration in any k8s cluster, we going to use a GCloud cluster.

## 2.- Create the k8s cluster on gcloud:
#### 2.1.- Export the variables for cluster
```
export project        = Name you project in GCP
export namecluster    = Name of my cluster
export zone           = Zone of deployment
export nodelocations  = Locations in the zone
export nodetype       = Node Size
export machinetype    = Total of nodes
export disksize       = Minimal nodes
export numnode        = Maxima nodes
export clusterversion = version you will used for kubernetes
```
> For more information about [Node Size ](https://cloud.google.com/compute/docs/machine-types)
> For more information about [Zones](https://cloud.google.com/compute/docs/regions-zones/)

For example :
```
export project=stackgres-demo-256115
export namecluster=stackgres-demo-gke-cluster
export zone=us-west1-a
export nodelocations=us-west1-a,us-west1-b,us-west1-c
export machinetype=n1-standard-2
export disksize=20
export numnodes=1
export clusterversion=1.12.10-gke.17
```
> Currently the only version supported is  1.12.0 - 1.16.0 for [kubernetes](https://docs.aws.amazon.com/eks/latest/userguide/kubernetes-versions.html)

#### 2.2.- create cluster

`
gcloud  container --project $project  clusters create $namecluster --zone $zone --node-locations $nodelocations  --machine-type $machinetype --disk-size $disksize --num-nodes $numnodes --cluster-version $clusterversion --enable-stackdriver-kubernetes --no-enable-ip-alias --no-enable-autoupgrade --metadata disable-legacy-endpoints=true verbosity=none
`

> Check each of this values to make sure is going to work with your GCP project.

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
`helm install --name stackgres-cluster stackgres-k8s/install/helm/stackgres-cluster/`

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

`kubectl port-forward  "$(kubectl get pods --all-namespaces -o json | jq '.items' | jq -c '.[] | select (.metadata.name | contains("stackgres-orator"))' | jq '.metadata.name' -r)" 8883:443
`

### 8.1.- Access the Web UI
[Go to UI](https://127.0.0.1:8443 )

## 9.- To delete this cluster
```
gcloud container clusters delete  $namecluster  --zone $zone
helm tiller stop
```

## 10- END
:stuck_out_tongue:
