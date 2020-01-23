---
title: Create your first cluster
weight: 3
---

# Installation with kubectl

We ship some kubernetes resources files in order to allow installation of the StackGres cluster
 including backup configuration for demostration purpose. Assuming you have already installed the
 [kubectl CLI](https://kubernetes.io/docs/tasks/tools/install-kubectl/) you can proceed by
 installing a StackGres cluster using the following command:

```
kubectl apply -f https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/demo-simple-config.yml
kubectl apply -f https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/demo-simple-cluster.yml
```

To clean up the resources created by the demo just run:

```
kubectl delete --ignore-not-found -f https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/demo-simple-cluster.yml
kubectl delete --ignore-not-found -f https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/demo-simple-config.yml
```

# Installation with helm

You can also install a StackGres cluster using [helm vesion 2.x](https://github.com/helm/helm/releases)
 with the following command:

```
helm install --dep-up --name simple \
  https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm-cluster.tgz
```

To clean up the resources created by the demo just run:

```
(helm get manifest simple; helm get hooks simple) | kubectl delete --ignore-not-found -f -
helm delete --purge simple
```

# Check cluster

A cluster called `simple` will be deployed in the default namespace
 that is configured in your environment (normally this is the namespace `default`).

```
watch kubectl get pod -o wide
```

```
NAMESPACE   NAME                            READY   STATUS            RESTARTS   AGE     IP           NODE                 NOMINATED NODE
default     simple-0                        5/5     Running           0          97s     10.244.2.5   kind-worker2         <none>
default     simple-1                        0/5     PodInitializing   0          41s     10.244.1.7   kind-worker          <none>
default     simple-minio-7dfd746f88-7ndmq   1/1     Running           0          99s     10.244.1.5   kind-worker          <none>
```

# Open a psql console

To open a psql console and manage the PostgreSQL cluster you may connect to the postgres-util
 container of primary instance (with label `role: master`):

```
kubectl exec -ti "$(kubectl get pod --selector app=StackGres,cluster=true,role=master -o name)" -c postgres-util -- psql -U postgres -p 5435
```

```
psql (11.6 OnGres Inc.)
Type "help" for help.

postgres=# CREATE USER app WITH PASSWORD 'test';
CREATE ROLE
postgres=# CREATE DATABASE app WITH OWNER app;
CREATE DATABASE
```

# Manage the status of the PostgreSQL cluster

You can also open a shell in any instance to use patronictl and control the status of the cluster:

```
kubectl exec -ti "$(kubectl get pod --selector app=StackGres,cluster=true -o name | hean -n 1)" -c patroni -- psql -U postgres -p 5435
```

```
sh-4.4$ patronictl list
+---------+----------+-----------------+--------+---------+----+-----------+
| Cluster |  Member  |       Host      |  Role  |  State  | TL | Lag in MB |
+---------+----------+-----------------+--------+---------+----+-----------+
| default | simple-0 | 10.244.2.5:5433 |        | running |  2 |       0.0 |
| default | simple-1 | 10.244.1.7:5433 | Leader | running |  2 |           |
+---------+----------+-----------------+--------+---------+----+-----------+
```

# Connect from an application

You will be able to connect to the cluster primary instance using service DNS `simple-primary` from a pod in the same namespace.
