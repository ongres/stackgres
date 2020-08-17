---
title: Create your first cluster
weight: 3
---

# Installation with kubectl

To create your first StackGres cluster you have to create a simple custom resource that reflect
 the cluster configuration. Assuming you have already installed the
 [kubectl CLI](https://kubernetes.io/docs/tasks/tools/install-kubectl/) you can proceed by
 installing a StackGres cluster using the following command:

```shell
cat << 'EOF' | kubectl create -f -
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  name: simple
spec:
  instances: 2
  postgresVersion: 'latest'
  pods:
    persistentVolume: 
      size: '5Gi'
EOF
```

## Enable backups

This will create a cluster using latest available PostgreSQL version with 2 nodes each with a disk
 of 5Gi using the default storage class and a set of default configurations for PostgreSQL,
 connection pooling and resource profile.

By default backup are not enabled. To enable them you have to provide a storage configuration (AWS S3,
 Google Cloud Storage or Azure Blob Storage). We ship a kubernetes resources file in order to allow
 easy installation of minio service that is compatible with AWS S3.

Clean up the previously created cluster:

```shell
kubectl delete sgcluster simple
```

Create the minio service and the backup configuration with default parameters:

```shell
kubectl create -f {{< download-url >}}/demo-minio.yml

cat << 'EOF' | kubectl create -f -
apiVersion: stackgres.io/v1beta1
kind: SGBackupConfig
metadata:
  name: simple
spec:
  storage:
    type: s3Compatible
    s3Compatible:
      bucket: stackgres
      region: k8s
      enablePathStyleAddressing: true
      endpoint: http://minio:9000
      awsCredentials:
        secretKeySelectors:
          accessKeyId:
            key: accesskey
            name: minio
          secretAccessKey:
            key: secretkey
            name: minio
EOF
```

Then create the StackGres cluster indicating the previously created backup configuration:

```shell
cat << 'EOF' | kubectl create -f -
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  name: simple
spec:
  instances: 2
  postgresVersion: 'latest'
  pods:
    persistentVolume:
      size: '5Gi'
  configurations:
    sgBackupConfig: simple
EOF
```

To clean up the resources created by this demo just run:

```
kubectl delete sgcluster simple
kubectl delete sgbackupconfig simple
kubectl delete -f {{< download-url >}}/demo-minio.yml
```

# Installation with helm

You can also install a StackGres cluster using [helm vesion 3.x](https://github.com/helm/helm/releases)
 with the following command:

```
helm install simple \
  {{< download-url >}}/demo-helm-cluster.tgz
```

To clean up the resources created by the demo run:

```
helm uninstall --keep-history simple
helm get hooks simple | kubectl delete --ignore-not-found -f -
helm uninstall simple
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
kubectl exec -ti "$(kubectl get pod --selector app=StackGresCluster,cluster=true,role=master -o name)" -c postgres-util -- psql
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
kubectl exec -ti "$(kubectl get pod --selector app=StackGresCluster,cluster=true -o name | head -n 1)" -c patroni -- patronictl list
```

```
+ Cluster: simple (6858282512984477821) ---------+----+-----------+
|  Member  |       Host       |  Role  |  State  | TL | Lag in MB |
+----------+------------------+--------+---------+----+-----------+
| simple-0 | 10.244.0.28:7433 | Leader | running |  1 |           |
+----------+------------------+--------+---------+----+-----------+
```

# Connect from an application

You will be able to connect to the cluster primary instance using service DNS `simple-primary` from a pod in the same namespace.
