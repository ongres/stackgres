---
title: Exposing Cluster Services
weight: 3
url: /administration/cluster/connection/exposed
description: Describes how to connect on the cluster exposing its services on the internet.
showToc: true
---

To allow access outside the K8s cluster, it is necessary to update the [services that exposes access to the StackGres cluster]({{% relref "/04-administration-guide/03-connecting-to-the-cluster/02-dns/" %}}) to type `NodePort` or `LoadBalancer`.

All following examples assume that there is a cluster named `cluster` in the `default` namespace.

## Updating the Service Configuration

By default, SGCluster services type are `ClusterIP` which means that the SGCluster will not be opened outside the k8s cluster. To change that behavior, it is necessary to update the cluster, changing the service configuration.

### Connecting through NodePort

NodePort is a Kubernetes mechanism to expose a service on a dynamic port on each cluster node.
Update the SGCluster configuration like below:

```yaml
---
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
  namespace: default
spec:
  ## ...
  postgresServices:
    primary:
      type: NodePort
    replicas:
      type: NodePort
```

> Check the [SGCluster reference]({{% relref "06-crd-reference/01-sgcluster" %}}) for more details about the cluster configuration.

Once applied, the service configuration is updated to `NodePort`:

```
kubectl get services -l stackgres.io/cluster=true,stackgres.io/cluster-name=cluster
# NAME               TYPE       CLUSTER-IP       EXTERNAL-IP   PORT(S)                         AGE
# cluster            NodePort   10.101.139.224   <none>        5432:31884/TCP,5433:31998/TCP   35m
# cluster-replicas   NodePort   10.99.44.2       <none>        5432:32106/TCP,5433:31851/TCP   35m
```

Get the node ip address (kind ip address on the example below):

```
kubectl get nodes -o wide
# NAME                 STATUS   ROLES    AGE    VERSION    INTERNAL-IP   EXTERNAL-IP   OS-IMAGE                                     KERNEL-VERSION     CONTAINER-RUNTIME
# kind-control-plane   Ready    primary   115s   v1.17.11   172.18.0.3    <none>        Ubuntu Groovy Gorilla (development branch)   5.8.0-36-generic   containerd://1.4.0
# kind-worker          Ready    <none>   79s    v1.17.11   172.18.0.4    <none>        Ubuntu Groovy Gorilla (development branch)   5.8.0-36-generic   containerd://1.4.0
# kind-worker2         Ready    <none>   79s    v1.17.11   172.18.0.7    <none>        Ubuntu Groovy Gorilla (development branch)   5.8.0-36-generic   containerd://1.4.0
# kind-worker3         Ready    <none>   79s    v1.17.11   172.18.0.5    <none>        Ubuntu Groovy Gorilla (development branch)   5.8.0-36-generic   containerd://1.4.0
# kind-worker4         Ready    <none>   79s    v1.17.11   172.18.0.2    <none>        Ubuntu Groovy Gorilla (development branch)   5.8.0-36-generic   containerd://1.4.0
# kind-worker5         Ready    <none>   85s    v1.17.11   172.18.0.6    <none>        Ubuntu Groovy Gorilla (development branch)   5.8.0-36-generic   containerd://1.4.0
```

Connect on the cluster using `psql` with the `INTERNAL IP` of any node (172.18.0.2 per example) and the service port (`31884` will point to `cluster` on port `5432`):

```
psql -h 172.18.0.2 -U postgres -p 31884
```

### Connecting through a Load Balancer

LoadBalancer is another option to expose cluster access to outside the k8s cluster.
For on-premise environments this option needs an extra configuration on the k8s cluster to install and configure an Ingress Controller that will route the connections to the target service.

The below example is implemented with [kind](https://kind.sigs.k8s.io/), and it uses [MetalLB](https://metallb.universe.tf/) under the hood.
For cloud environments, check your cloud vendor's documentation about the Load Balancer implementation details.

Update the SGCluster configuration like below:

```yaml
---
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
  namespace: default
spec:
  ## ...
  postgresServices:
    primary:
      type: LoadBalancer
    replicas:
      type: LoadBalancer
```

> Check the [SGCluster reference]({{% relref "06-crd-reference/01-sgcluster" %}}) for more details about the cluster configuration.

Once updated, get the service information:

```
kubectl get services -l stackgres.io/cluster=true,stackgres.io/cluster-name=cluster
# NAME               TYPE           CLUSTER-IP      EXTERNAL-IP    PORT(S)                         AGE
# cluster            LoadBalancer   10.108.32.129   172.18.0.102   5432:30219/TCP,5433:30886/TCP   8m13s
# cluster-replicas   LoadBalancer   10.111.30.87    172.18.0.101   5432:31146/TCP,5433:32063/TCP   8m13s
```

> Please note that, since we change both services to `LoadBalancer`, two loadbalancers were created, one for each service. 
> Be aware that additional charges may apply on your cloud provider.

To connect on the database, just use the `EXTERNAL-IP`, like below:

```
psql -h 172.18.0.102 -U postgres
```

#### Internal Load Balancer

By default, the service type `LoadBalancer` create an external IP that is publicly accessible, so it is not a recommended option to expose the database service, but there's an option to create `internal` load balancers that create External IP but only accessible from your private network, so you can take advantage of load balance functionality without risking your database.

To configure this type of LoadBalancer is usually by setting some annotations to the services. The annotations are provided by each cloud provider, check the examples below and make sure you add them to your [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) manifest:


**[GKE](https://cloud.google.com/kubernetes-engine/docs/how-to/internal-load-balancing):**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    annotations:
      primaryService:
        networking.gke.io/load-balancer-type: "Internal"
      replicasService:
        service.beta.kubernetes.io/aws-load-balancer-internal: "true"
```


**[EKS](https://docs.aws.amazon.com/eks/latest/userguide/network-load-balancing.html):**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    annotations:
      primaryService:
        service.beta.kubernetes.io/aws-load-balancer-internal: "true"
      replicasService:
        service.beta.kubernetes.io/aws-load-balancer-internal: "true"
```

**[AKS](https://docs.microsoft.com/en-us/azure/aks/internal-lb):**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  metadata:
    annotations:
      primaryService:
        service.beta.kubernetes.io/azure-load-balancer-internal: "true"
      replicasService:
        service.beta.kubernetes.io/azure-load-balancer-internal: "true"
```


> **Note:** It is not necessary to configure both services you can pick only the one you need.
