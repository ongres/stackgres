---
title: Demo / Quickstart
weight: 2
chapter: true
url: demo/quickstart
---

### Chapter 2

# Demo / Quickstart

> **NOTE:** To run this demo you will need a [K8s environment]({{% relref "04-production-installation/01-pre-requisites/01-k8s-environments" %}}) with at least 2 worker nodes. 

## Installing the Operator

We ship some Kubernetes resources files in order to allow installation of the StackGres operator
 for demonstration purpose. Assuming you have already installed the
 [kubectl CLI](https://kubernetes.io/docs/tasks/tools/install-kubectl/), you can install the
 operator with the following command:

```
kubectl apply -f {{< download-url >}}/stackgres-operator-demo.yml
```

This will install all required resources, and add the StackGres operator to a new namespace `stackgres`.

> The `stackgres-operator-demo.yml` will expose the UI with a LoadBalancer. Note that using this feature
> might cause additional cost by your hosting provider (for example, this is the case for EKS, GKE, and AKS).

## Waiting for Operator Startup

Use the command below to wait until the operator is ready to use:

```bash
kubectl wait -n stackgres deployment -l group=stackgres.io --for=condition=Available
```

Once it's ready, you will see that the operator pods are `Running`:

```bash
➜ kubectl get pods -n stackgres -l group=stackgres.io
NAME                                  READY   STATUS    RESTARTS   AGE
stackgres-operator-78d57d4f55-pm8r2   1/1     Running   0          3m34s
stackgres-restapi-6ffd694fd5-hcpgp    2/2     Running   0          3m30s

```

## Creating the Cluster

To create your first StackGres cluster, you have to create a simple custom resource that reflects
 the cluster configuration. The following command does this using the command line:

```shell
cat << 'EOF' | kubectl create -f -
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: simple
spec:
  instances: 2
  postgres:
    version: 'latest'
  pods:
    persistentVolume: 
      size: '5Gi'
EOF
```

This will create a cluster using the latest PostgreSQL version with 2 nodes each, with a disk
 of 5Gi using the default storage class. It uses StackGres' default configuration for PostgreSQL,
 connection pooling, and resource profile.

## Verifying the Created Cluster

A cluster called `simple` will be deployed in the default namespace
 that is configured in your environment (normally this is the namespace `default`).

Follow the creation status:

```bash
kubectl get pods --watch
```

Eventually, you should see something like this:
```
NAME       READY   STATUS    RESTARTS   AGE
simple-0   6/6     Running   0          2m50s
simple-1   6/6     Running   0          1m56s

```

## Accessing Postgres via psql

To open a psql console and manage the PostgreSQL cluster, you may connect to the `postgres-util` container of the primary instance (the pod with the label `role: master`):

```bash
kubectl exec -ti "$(kubectl get pod --selector app=StackGresCluster,stackgres.io/cluster=true,role=master -o name)" -c postgres-util -- psql
```

> **IMPORTANT:** Connecting directly through the `postgres-util` sidecar will grant you access with the postgres user. It works similar to `sudo -i postgres -c psql`.

Please read about the [postgres-util side car]({{% relref "05-administration-guide/02-connecting-to-the-cluster/03-postgres-util" %}}) and [how to connect to the Postgres cluster]({{% relref "05-administration-guide/02-connecting-to-the-cluster" %}}) for more details.

## Cluster Management and Automated Failover

Now that the cluster is up and running, you can also open a shell in any instance to use `patronictl` to control the status of the cluster:

```bash
kubectl exec -ti "$(kubectl get pod --selector app=StackGresCluster,stackgres.io/cluster=true,role=master -o name)" -c patroni -- patronictl list
```

You should see something similar to this:
```bash
+ Cluster: simple (6868989109118287945) ---------+----+-----------+
|  Member  |       Host       |  Role  |  State  | TL | Lag in MB |
+----------+------------------+--------+---------+----+-----------+
| simple-0 | 10.244.0.9:7433  | Leader | running |  1 |           |
| simple-1 | 10.244.0.11:7433 | Replica| running |  1 |         0 |
+----------+------------------+--------+---------+----+-----------+
```

Now, to test the automated failover, let's simulate a disaster by killing the leader `simple-0`:
```bash
kubectl delete pod simple-0
```

After deleted the leader `simple-0`, Patroni should perform the switchover, electing `simple-1` as new leader and replacing the `simple-0` instance with a new container.
After Patroni performs the failover operation, you can check the cluster status again:
```bash
kubectl exec -ti "$(kubectl get pod --selector app=StackGresCluster,stackgres.io/cluster=true,role=master -o name)" -c patroni -- patronictl list
```

The final state of the failover will result with node `simple-1` as the leader and `simple-0` as the replica.
```bash
+ Cluster: simple (6868989109118287945) ---------+----+-----------+
|  Member  |       Host       |  Role  |  State  | TL | Lag in MB |
+----------+------------------+--------+---------+----+-----------+
| simple-0 | 10.244.0.9:7433  | Replica| running |  2 |         0 |
| simple-1 | 10.244.0.11:7433 | Leader | running |  2 |           |
+----------+------------------+--------+---------+----+-----------+
```

Please read about the [Patroni management]({{% relref "05-administration-guide/16-patroni-management" %}}) for more details.

## Connecting to the UI

Now that you know a little more about StackGres, you can easily manage all your clusters from the UI. The UI will ask for a username and a password. By default, those are `admin` and a randomly generated password. You can run the command below to get the user and auto-generated password:

```bash
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\npassword = %s\n" (.data.k8sUsername | base64decode) ( .data.clearPassword | base64decode) }}'
```

With the credentials in hand, let's connect to the operator web UI. For this, you may forward the HTTPS port of the operator pod:

```
POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward "$POD_NAME" 8443:9443 --namespace stackgres
```

Then you can open the browser at the following address [`localhost:8443/admin/`](https://localhost:8443/admin/)

## Cleaning up

To uninstall all resources generated by this demo, you can run:
```bash
kubectl delete --ignore-not-found -f {{< download-url >}}/stackgres-operator-demo.yml
```

Check the [uninstall]({{% relref "/05-administration-guide/999999-uninstall" %}}) section for more details.

Also, see the [installation via helm]({{% relref "/04-production-installation/02-installation-via-helm" %}}) section in order to change those.