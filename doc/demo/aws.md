# Prerequisites
Make sure you have the following prerequisites:

- [Kubectl ](https://kubernetes.io/es/docs/tasks/tools/install-kubectl/)

- [Helm ](https://helm.sh/docs/using_helm/#installing-helm)
- [helm tiller service](https://helm.sh/docs/using_helm/#initialize-helm-and-install-tiller)
  > helm init --client-only

  or
- [tillerless](https://github.com/rimusz/helm-tiller)

- A Kubernetes cluster (Localy using [Minikube](https://kubernetes.io/es/docs/tasks/tools/install-minikube/), [kind](https://github.com/kubernetes-sigs/kind) ,etc. or in the cloud)


For the demo installation will use AWS,  therefore,  you need to install  eksctl for Linux


- [eksctl](https://github.com/weaveworks/eksctl/blob/master/README.md)

> it is necessary to have aws installed with a version> 1.6


- [aws](https://docs.aws.amazon.com/es_es/cli/latest/userguide/install-linux.html)

- Create role in console AWS for eks

- Create your kubeconfig file with the AWS CLI

`aws eks --region region update-kubeconfig --name cluster_name`

# Installation and configuration of StackGres 0.7 version
## 1.- Clone StackGres repository

`
git clone https://gitlab.com/ongresinc/stackgres.git
cd stackgres
`

> You can work this configuration in any k8s cluster, we going to use a GCloud cluster.

## 2.- Create the k8s cluster on aws:
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
--node-ami  $nodeami
```


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
### 6.1.- Integration with grafana
#### 6.1.1.- Run grafana to get metrics
```
GRAFANA_POD_ID=$(kubectl get pods --all-namespaces | grep grafana | awk '{print $2}')
GRAFANA_NAME_SPACE=$(kubectl get pods --all-namespaces | grep grafana | cut -d ' ' -f 1)
kubectl port-forward $GRAFANA_POD_ID -n $GRAFANA_NAME_SPACE 3000
```
`
sh integrate-grafana.sh
`
> This script integrate-grafana.sh are in the folder stackgres

#### 8.1.2.- Add previous metrics in the following command and install operator

`helm install --name stackgres-operator operator/install/kubernetes/chart/stackgres-operator --set-string grafana.url='<dashboard url>' --set-string grafana.token='<grafana token>' --set-string grafana.httpHost='prometheus-operator-grafana.default.svc' `


## 7.- Now, we are going to create the StackGres cluster, generating the CRs one by one
  * Create the next yaml files and these must be executed in the same order as shown below:

 1. profiles-crs.yaml
 1. pgconfig-cr.yaml
 1. pgbouncerconfig-cr.yaml
 1. cluster-cr.yaml

## 7.1.- Create each file with the content below:

`profiles-crs.yaml` Custom resources for instances size(memory and cpu):
```
apiVersion: stackgres.io/v1alpha1
kind: StackGresProfile
metadata:
  name: size-xs
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "500m"
  memory: "512Mi"
---
apiVersion: stackgres.io/v1alpha1
kind: StackGresProfile
metadata:
  name: size-s
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "1"
  memory: "2Gi"
---
apiVersion: stackgres.io/v1alpha1
kind: StackGresProfile
metadata:
  name: size-m
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "2"
  memory: "4Gi"
---
apiVersion: stackgres.io/v1alpha1
kind: StackGresProfile
metadata:
  name: size-l
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "4"
  memory: "8Gi"
---
apiVersion: stackgres.io/v1alpha1
kind: StackGresProfile
metadata:
  name: size-xl
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "6"
  memory: "16Gi"
---
apiVersion: stackgres.io/v1alpha1
kind: StackGresProfile
metadata:
  name: size-xxl
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "8"
  memory: "32Gi"

```
`pgconfig-cr.yaml`  Custom resource for PosgreSQL configuration:
```
apiVersion: stackgres.io/v1alpha1
kind: StackGresPostgresConfig
metadata:
  name: postgresconf
#  annotations:
#    "helm.sh/hook": "pre-install"
spec:
  pgVersion: "12"
  postgresql.conf:
      shared_buffers: '256MB'
      random_page_cost: '1.5'
      password_encryption: 'scram-sha-256'
      wal_compression: 'on'

```

`pgbouncerconfig-cr.yaml` Custom resource for pgbouncer configuration:

```
apiVersion: stackgres.io/v1alpha1
kind: StackGresConnectionPoolingConfig
metadata:
  name: pgbouncerconf
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  pgbouncer_version: "1.11.0"
  pgbouncer.ini:
      pool_mode: transaction
      max_client_conn: '200'
      default_pool_size: '200'

```
`cluster-cr.yaml` Custom resource for StackGres cluster
```
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
spec:
  instances: 3
  pgVersion: '12.0'
  pgConfig: 'postgresconf'
  connectionPoolingConfig: 'pgbouncerconf'
  resourceProfile: 'size-xs'
  volumeSize: '5Gi'
  storageClass: 'standard'
  postgresExporterVersion: '0.5.1'
  prometheusAutobind: true
  sidecars:
  - connection-pooling
  - postgres-util
  - prometheus-postgres-exporter

```

### 7.2.- Once you have the files created, apply it to the k8s cluster:

```
kubectel apply -f _your_directory/profiles-crs.yaml
kubectel apply -f _your_directory/pgconfig-cr.yaml
kubectel apply -f _your_directory/pgbouncerconfig-cr.yaml
kubectel apply -f _your_directory/cluster-cr.yaml

```

> Note: This last file will create all the StackGres cluster resources


### 7.3.- Verify the cluster

`kubectl get pods  | grep stackgres`
### 7.4.- Verify Postgres cluster status

`kubectl exec -it stackgres-0 -c patroni -- patronictl list`

### 7.5.- Connect a PostgreSQL

`kubectl exec -it stackgres-0 -c postgres-util -- psql`

> Note: This container have all the postgres binaries and you can use it to check PostgreSQL functionality  

## 8.-  Create a port-forward to access the web UI

`
kubectl port-forward -n stackgres "$(kubectl get pod -n stackgres | grep Running | cut -d ' ' -f 1)" 8443:443
`

### 8.1.- Access the Web UI
[Go to UI](https://127.0.0.1:8443 )

## 9.- To delete this cluster
```
helm tiller stop
gcloud container clusters delete  <your project name>  --zone us-west1-a
```

## 10- END
:stuck_out_tongue:
