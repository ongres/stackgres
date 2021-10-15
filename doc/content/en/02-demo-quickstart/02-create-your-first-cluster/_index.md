---
title: Create your first cluster
weight: 3
url: demo/cluster/create
description: Details about the how to create the first StackGres cluster.
showToc: true
---

## Installation with kubectl

To create your first StackGres cluster you have to create a simple custom resource that reflect
 the cluster configuration. Assuming you have already installed the
 [kubectl CLI](https://kubernetes.io/docs/tasks/tools/install-kubectl/) you can proceed by
 installing a StackGres cluster using the following command:

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

This will create a cluster using latest available PostgreSQL version with 2 nodes each with a disk
 of 5Gi using the default storage class and a set of default configurations for PostgreSQL,
 connection pooling and resource profile.

## Check cluster

A cluster called `simple` will be deployed in the default namespace
 that is configured in your environment (normally this is the namespace `default`).

```bash
kubectl get pods --watch
```

```
NAME       READY   STATUS    RESTARTS   AGE
simple-0   6/6     Running   0          82s
simple-1   6/6     Running   0          36s

```

## Accessing Postgres(psql)

To open a psql console and manage the PostgreSQL cluster you may connect to the `postgres-util` container of primary instance (with label `role: master`):

```bash
kubectl exec -ti "$(kubectl get pod --selector app=StackGresCluster,cluster=true,role=master -o name)" -c postgres-util -- psql
```
> **IMPORTANT:** Connecting directly trough the `postgres-util` sidecar will grant you access with the postgres user. It will work similar to `sudo -i postgres -c psql`.

Please check [about the postgres-util side car]({{% relref "05-administration-guide/02-Connecting-to-the-cluster/03-postgres-util" %}}) and [how to connect to the postgres cluster]({{% relref "05-administration-guide/02-Connecting-to-the-cluster" %}}) for more details.

Each `SGCluster` will create a service for both the primary and the replicas. They will be create as `${CLUSTER-NAME}-primary` and `${CLUSTER-NAME}-replicas`.

You will be able to connect to the cluster primary instance using service DNS `simple-primary` from any pod in the same namespace.

For example:

```bash
➜ kubectl run -it psql-connect --rm --image=postgres:12 -- psql -U postgres -h simple-primary                    
If you don't see a command prompt, try pressing enter.

psql (12.4 (Debian 12.4-1.pgdg100+1), server 12.3 OnGres Inc.)
Type "help" for help.

postgres=# \q
Session ended, resume using 'kubectl attach psql-connect -c psql-connect -i -t' command when the pod is running
pod "psql-connect" deleted
```

Check [how to connect to the cluster]({{% relref "/05-administration-guide/02-connecting-to-the-cluster#dns-resolution" %}}) for more details.

## Cluster Status and Automated Failover

Now that the cluster is up and running, you can also open a shell in any instance to use patronictl and control the status of the cluster:

```bash
kubectl exec -ti "$(kubectl get pod --selector app=StackGresCluster,cluster=true -o name | head -n 1)" -c patroni -- patronictl list
```

The result should be something similar to this:
```bash
+ Cluster: simple (6868989109118287945) ---------+----+-----------+
|  Member  |       Host       |  Role  |  State  | TL | Lag in MB |
+----------+------------------+--------+---------+----+-----------+
| simple-0 | 10.244.0.9:7433  | Leader | running |  1 |           |
| simple-1 | 10.244.0.11:7433 |        | running |  1 |         0 |
+----------+------------------+--------+---------+----+-----------+
```

To test the automated cluster failover, let's simulate a disaster killing the leader:
```bash
kubectl delete pod simple-0
```

You can observe the failover process in action by running the command:
```bash
kubectl exec -ti "$(kubectl get pod --selector app=StackGresCluster,cluster=true -o name | head -n 1)" -c patroni -- patronictl list
```

The final state of the failover will result as the `simple-1` node as the leader:
```bash
+ Cluster: simple (6868989109118287945) ---------+----+-----------+
|  Member  |       Host       |  Role  |  State  | TL | Lag in MB |
+----------+------------------+--------+---------+----+-----------+
| simple-0 | 10.244.0.9:7433  |        | running |  1 |           |
| simple-1 | 10.244.0.11:7433 | Leader | running |  1 |         0 |
+----------+------------------+--------+---------+----+-----------+
```

Please check [about the patroni-management]({{% relref "05-administration-guide/16-patroni-management" %}}) for more details.