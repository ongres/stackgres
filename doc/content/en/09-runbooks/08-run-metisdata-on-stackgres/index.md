---
title: Run MetisData on top of StackGres
weight: 7
url: runbooks/metisdata-stackgres
description: How to run MetisData on top of StackGres
showToc: true
---

This runbook will show you how to install [MetisData](https://metisdata.io/) on Kubernetes, with a production-grade database provided by StackGres. MetisData  guardrail enables a proactive approach to database code, empowering devs to better understand, control, troubleshoot, fix, and own the entire data layer.improving their experiences and preventing code from breaking production.


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


MetisData will use one (or more) databases in Postgres, and expects it to be created and owned by a given user. Since we don't want to use Postgres superuser for this, we will create a specific user, with a specific password (randomly generated, as per this runbook) and one database for MetisData. To do this, we will leverage StackGres' [SGScript](https://stackgres.io/doc/latest/reference/crd/sgscript/) facility, that allows us to create and maintain SQL scripts that will be automatically managed and applied in the database by StackGres.

Let's start by creating a `Secret` that contains the SQL command to create the user with a random password.

```sh
#!/bin/sh

PASSWORD="$(dd if=/dev/urandom bs=1 count=8 status=none | base64 | tr / 0)"

kubectl -n metisdata create secret generic createuser \
  --from-literal=sql="create user metisdata with password '"${PASSWORD}"'"
```

Please add the privilege to execute the file :
```
sudo chmod +x 02-createuser_secret.sh
```

```sh
./02-createuser_secret.sh
```

We can now create the `SGScript`, that will contain two scripts: one to create the user with the password, by reading the SQL literal from this `Secret`; and another one to create the database, owned by this user, and with the proper encoding and locale configuration that are required by MetisData:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: createuserdb
  namespace: metisdata
spec:
  scripts:
  - name: create-user
    scriptFrom:
      secretKeyRef:
        name: createuser
        key: sql
  - name: create-database
    script: |
            create database metisdata owner metisdata encoding 'UTF8' locale 'en_US.UTF-8' template template0;
```

```sh
kubectl apply -f 03-sgscript.yaml
```

We are now ready to create the Postgres cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: metisdata
  name: postgres
spec:
  postgres:
    version: '15'
  instances: 1
  pods:
    persistentVolume:
      size: '5Gi'
  configurations:
    sgPoolingConfig: sgpoolingconfig1
  managedSql:
    scripts:
      - sgScript: createuserdb
```

```sh
kubectl apply -f 04-sgcluster.yaml
```

After some seconds to a few minutes, the cluster should be brought up:

```sh
kubectl -n metisdata get pods
NAME                           READY   STATUS    RESTARTS   AGE
postgres-0                     6/6     Running   0          16m
```

And the database `metisdata` should exist and being owned by the user with the same name:

```sh
kubectl -n metisdata exec -it postgres-0 -c postgres-util -- psql -l metisdata
                                                 List of databases
   Name    |  Owner   | Encoding |   Collate   |    Ctype    | ICU Locale | Locale Provider |   Access privileges   
-----------+----------+----------+-------------+-------------+------------+-----------------+-----------------------
 metisdata  | metisdata | UTF8     | en_US.UTF-8 | en_US.UTF-8 |            | libc            | 
  ...
```


## Deploy MetisData

Add the Metis Helm repository to your local Helm installation and update the Helm repository to ensure that you have the latest version:

```
helm repo add metis-data https://metis-data.github.io/helm-charts/
helm repo update
```

Create helm chart with specific api-key and pg connection on your relevant namespace:

```
helm install metis-mmc metis-data/metis-md-collector \
  --set METIS_API_KEY=*****1 \
  --set DB_CONNECTION_STRINGS=*****2://postgres:postgres@postgres.metisdata.svc:5432/postgres;
```

Where the:
*****1 - Represents the API token from Metisdata, that you could find in the web console at:
![Metisdata-API](metisdata-api.png)


*****2 - Represents the StackGres password, that could be obtained by the command: 
```
kubectl get secrets -n demo stackgres -o jsonpath='{.data.superuser-password}' | base64 -d
```

## Cleanup

Deleting the namespace should clean all used resources:

```sh
kubectl delete namespace metisdata
```
