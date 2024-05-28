---
Title: Configure Gateway API 
weight: 11
url: /runbooks/gateway-api
description: How to configure Gateway API
showToc: true
---

Gateway API is an official Kubernetes project focused on L4 and L7 routing in Kubernetes. This project represents the next generation of Kubernetes Ingress, Load Balancing, and Service Mesh APIs. From the outset, it has been designed to be generic, expressive, and role-oriented. ([Official documentation](https://gateway-api.sigs.k8s.io/))

On this runbook we'll show you how to configure the basics to start using Gateway API with StackGres.

>**Important:** In case your Kubernetes cluster, does not have a LoadBalancer implementation, we recommend installing one so the Gateway resource has an Address associated with it. We recommend using [MetalLB](https://metallb.universe.tf/installation/).

## Gateway API components

There are four main components to configure the Gateway API:

1. **Gateway Controller:** A gateway controller is software that manages the infrastructure associated with routing traffic across contexts using Gateway API, analogous to the earlier ingress controller concept. Gateway controllers often, but not always, run in the cluster where they're managing infrastructure.

There are various controller you can use for your infrastructure, check the list [here](https://gateway-api.sigs.k8s.io/implementations/#gateway-controller-implementation-status). 

For the demo purpose we will use the [Envoy Gateway](https://gateway.envoyproxy.io/l).

2. **GatewayClass:** Is a cluster-scoped resource. There must be at least one GatewayClass defined in order to be able to have functional Gateways.

3. **Gateways:** A Gateway describes how traffic can be translated to Services within the cluster. That is, it defines a request for a way to translate traffic from somewhere that does not know about Kubernetes to somewhere that does.

It defines a request for a specific load balancer config that implements the GatewayClass’ configuration and behaviour contract. The resource may be created by an operator directly, or may be created by a controller handling a GatewayClass.

4. **Routes:**  Route resources define protocol-specific rules for mapping requests from a Gateway to Kubernetes Services.

We will show you how to create a TCPRoute to handle Postgres traffic. You can check the different types of Routes [here](https://gateway-api.sigs.k8s.io/concepts/api-overview/#route-resources)



## SGCluster

In order to focus on the Gateway API configuration we will assume that you already have your SGCluster up and running. Check the StackGres [Demo Quickstart]({{% relref "03-demo-quickstart/#" %}}) to create your cluster. The demo cluster used here was created on GKE. 

**Demo SGCluster**:

```bash
❯ kubectl get sgclusters.stackgres.io -n my-db 
NAME    VERSION   INSTANCES   PROFILE   DISK
my-db   16.2      1           size-s    10Gi
```

**SGCluster service:** 

```bash
 kubectl get services -n my-db -l stackgres.io/cluster=true
NAME           TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)             AGE
my-db          ClusterIP   10.63.219.145   <none>        5432/TCP,5433/TCP   61m
my-db-config   ClusterIP   None            <none>        <none>              61m
```

## Configure Gateway API

### 1. Install the Gateway API Controler:

Even though GKE already provides a Gateway API controller, we'll use the Envoy controller.

Install it executing the next command:

```bash
helm install eg oci://docker.io/envoyproxy/gateway-helm --version v0.0.0-latest -n envoy-gateway-system --create-namespace
```

>**Note:** For custom requirements check the [official documentation](https://gateway.envoyproxy.io/)


### 2. Create the GatewayClass

```bash
cat <<EOF | kubectl apply -f -
apiVersion: gateway.networking.k8s.io/v1
kind: GatewayClass
metadata:
  name: eg
spec:
  controllerName: gateway.envoyproxy.io/gatewayclass-controller
EOF
```

Check the object created:

```bash
❯ kubectl get gatewayclasses.gateway.networking.k8s.io 
NAME   CONTROLLER                                      ACCEPTED   AGE
eg     gateway.envoyproxy.io/gatewayclass-controller   True       86m
```

### 3. Create the Gateway

```bash
cat <<EOF | kubectl apply -f -
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: tcp-gateway
  namespace: my-db
spec:
  gatewayClassName: eg
  listeners:
  - name: pg-gateway
    protocol: TCP
    port: 5432
    allowedRoutes:
      kinds:
      - kind: TCPRoute
EOF
```

Check the object created: 

```bash
❯ kubectl get gateways.gateway.networking.k8s.io -n my-db 
NAME          CLASS   ADDRESS        PROGRAMMED   AGE
tcp-gateway   eg      34.89.14.197   True         72m
```

>Note that we are using the namespace `my-db` because only the `GatewayClass` is cluster-scoped.

The IP address might take a few minutes to be assigned. This is the public IP that you need to use as the service endpoint.


### 4. Create the TCPRoute

```bash
cat <<EOF | kubectl apply -f -
apiVersion: gateway.networking.k8s.io/v1alpha2
kind: TCPRoute
metadata:
  name: my-db-route
  namespace: my-db
spec:
  parentRefs:
  - name: tcp-gateway
    sectionName: pg-gateway
  rules:
  - backendRefs:
    - name: my-db
      port: 5432
EOF
```

- Check the `sectionName` field in the `parentRefs`. This correspond directly with the name in the `listeners` in the `Gateway`
- The `backendRefs` correspond to the database service. 


## Check Postgres connection

Once you configured the Gateway API, the last thing is to check if you have connection to your database cluster:

We got the superuser passsword with the next command:

```bash
PASSWORD=$(kubectl get secret -n my-db my-db --template '{{ printf "%s" (index .data "superuser-password" | base64decode) }}')
echo "user: postgres"
echo "password: $PASSWORD"
```

output:

```bash
user: postgres
password: 7a83-4f62-4c71-815
```

Now we can test the connection to the cluster:


```bash
❯ psql -h 34.89.14.197 -p 5432 -U postgres -d postgres
Password for user postgres: 
psql (16.3 (Ubuntu 16.3-1.pgdg22.04+1), server 16.2 (OnGres 16.2-build-6.31))
Type "help" for help.

postgres=# 
```

## Next steps

- The above example was intended to show you the basic configuration of the `Gateway API`. If you want to expose your database, make sure to configure a secure connection to it.  

- Check the security documentation for the Envoy Gateway [here](https://gateway.envoyproxy.io/latest/tasks/security/)
