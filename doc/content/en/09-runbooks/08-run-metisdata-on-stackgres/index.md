---
title: Run Metis on top of StackGres
weight: 7
url: runbooks/metisdata-stackgres
description: How to run Metis on top of StackGres
showToc: true
---

This runbook will show you how to install [Metis](https://metisdata.io/) on Kubernetes, with a production-grade database provided by StackGres. MetisData  guardrail enables a proactive approach to database code, empowering devs to better understand, control, troubleshoot, fix, and own the entire data layer.improving their experiences and preventing code from breaking production.


## Scenario

In this runbook we'll assume that you already have a Kubernetes cluster with the StackGres operator installed.
We will create an SGCluster with a configuration that fits MetisData's requirements. 
You can find the example resources in the [apps-on-stackgres GitHub repository](https://github.com/ongres/apps-on-stackgres/tree/main/examples/metisdata). Please clone the repository and change to the `examples/metisdata` directory, where all the referenced files here are present.


## Creating an SGCluster

To properly group all related resources together, let's first create a namespace:

```yaml
kind: Namespace
apiVersion: v1
metadata:
  name: metisdata
```

To apply run from within the `examples/metisdata` folder of the [apps-on-stackgres GitHub repository](https://github.com/ongres/apps-on-stackgres/tree/main/examples/metisdata):

```sh
kubectl apply -f 01-namespace.yaml
```


In this step we are creating the database script for our cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: create-db-script
  namespace: metisdata
spec:
  continueOnError: false
  managedVersions: true
  scripts:
  - id: 0
    name: create-metisdata-database
    script: |
      create database metisdata;
  - id: 1
    database: metisdata
    name: create-extension-pgstoreplans
    script: |
      create extension if not exists pg_store_plans;
```

```sh
kubectl apply -f 02-db-script.yaml
```

We are now ready to create the Postgres cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: metisdata
  name: metisdata
spec:
  postgres:
    version: '15'
    extensions:
    - name: pg_store_plans
  instances: 1
  pods:
    persistentVolume:
      size: '5Gi'
  managedSql:
    scripts:
      - sgScript: create-db-script
```

Please execute:

```sh
kubectl apply -f 03-sgcluster.yaml
```

After some seconds to a few minutes, the cluster should be brought up:

```sh
kubectl -n metisdata get pods
NAME                           READY   STATUS    RESTARTS   AGE
postgres-0                     6/6     Running   0          16m
```





And the database `metisdata` should exist and being owned by the user with the same name:

```sh
kubectl -n metisdata exec -it metisdata-0 -c postgres-util -- psql -l 
                                             List of databases
   Name    |  Owner   | Encoding | Collate |  Ctype  | ICU Locale | Locale Provider |   Access privileges   
-----------+----------+----------+---------+---------+------------+-----------------+-----------------------
 metisdata | postgres | UTF8     | C.UTF-8 | C.UTF-8 |            | libc            | 
 postgres  | postgres | UTF8     | C.UTF-8 | C.UTF-8 |            | libc            | 
 template0 | postgres | UTF8     | C.UTF-8 | C.UTF-8 |            | libc            | =c/postgres          +
           |          |          |         |         |            |                 | postgres=CTc/postgres
 template1 | postgres | UTF8     | C.UTF-8 | C.UTF-8 |            | libc            | =c/postgres          +
           |          |          |         |         |            |                 | postgres=CTc/postgres
```


## Deploy 

Add the [Metis Helm repository](https://github.com/metis-data/helm-charts) to your local Helm installation and update the Helm repository to ensure that you have the latest version:

```
helm repo metis-data https://metis-data.github.io/helm-charts/
helm repo update
```

Create helm chart with specific api-key and pg connection on your relevant namespace:

```
helm install metis-mmc metis-data/metis-md-collector \
  --set apiKey="*****1" \
  --set dbConnectionStrings=postgresql://postgres:*****2@postgres.metisdata.svc:5432/metisdata;
```

Where the:
*****1 - Represents the API token from Metis, that you could find in the web console at:
![Metisdata-API](metisdata-api.png)


*****2 - Represents the StackGres password, that could be obtained by the command: 
```
kubectl get secrets -n metisdata metisdata -o jsonpath='{.data.superuser-password}' | base64 -d
```


The output should be similar with:

```
NAME: metis-mmc
LAST DEPLOYED: Thu Jun  8 09:49:09 2023
NAMESPACE: default
STATUS: deployed
REVISION: 1
TEST SUITE: None
```


## Cleanup

Deleting the namespace should clean all used resources:

```sh
kubectl delete namespace metisdata
```
