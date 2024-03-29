---
title: Run FerretDB on top of StackGres
weight: 7
url: /runbooks/ferretdb-stackgres
description: How to run FerretDB on top of Stackgres
showToc: true
---

This runbook will show you how to install [FerretDB](https://ferretdb.io/) on Kubernetes, with a production-grade database provided by StackGres. FerretDB is a fully open source MongoDB-compatible database, which may use Postgres as a data storage backend. From this perspective, FerretDB is a stateless application that exposes a MongoDB wire-protocol TCP interface.


## Scenario

In this runbook we'll assume that you already have a Kubernetes cluster with the StackGres operator installed.
We will create an SGCluster with a configuration that fits FerretDB's requirements. 
You can find the example resources in the [apps-on-stackgres GitHub repository](https://github.com/ongres/apps-on-stackgres/tree/main/examples/ferretdb). Please clone the repository and change to the `examples/ferretdb` directory, where all the referenced files here are present.


## Creating an SGCluster

To properly group all related resources together, let's first create a namespace:

```yaml
kind: Namespace
apiVersion: v1
metadata:
  name: ferretdb
```

To apply run from within the `examples/ferretdb` folder of the [apps-on-stackgres GitHub repository](https://github.com/ongres/apps-on-stackgres/tree/main/examples/ferretdb):

```sh
kubectl apply -f 01-namespace.yaml
```

FerretDB will try to set the [`search_path`](https://postgresqlco.nf/doc/en/param/search_path/) Postgres configuration parameter at startup. This is fine, but it is not supported by PgBouncer, which is deployed by default by StackGres as a sidecar to Postgres. We can either disable PgBouncer's sidecar (not recommended) or create a customized connection pooling configuration for PgBouncer to ignore this parameter --easy and harmless:

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  name: sgpoolingconfig1
  namespace: ferretdb
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        ignore_startup_parameters: extra_float_digits,search_path
```

Create with:

```sh
kubectl apply -f 02-sgpoolingconfig.yaml
```

FerretDB will use one (or more) databases in Postgres, and expects it to be created and owned by a given user. Since we don't want to use Postgres superuser for this, we will create a specific user, with a specific password (randomly generated, as per this runbook) and one database for FerretDB. To do this, we will leverage StackGres' [SGScript](https://stackgres.io/doc/latest/reference/crd/sgscript/) facility, that allows us to create and maintain SQL scripts that will be automatically managed and applied in the database by StackGres.

Let's start by creating a `Secret` that contains the SQL command to create the user with a random password.

```sh
#!/bin/sh

PASSWORD="$(dd if=/dev/urandom bs=1 count=8 status=none | base64 | tr / 0)"

kubectl -n ferretdb create secret generic createuser \
  --from-literal=sql="create user ferretdb with password '"${PASSWORD}"'"
```

```sh
./03-createuser_secret.sh
```

We can now create the `SGScript`, that will contain two scripts: one to create the user with the password, by reading the SQL literal from this `Secret`; and another one to create the database, owned by this user, and with the proper encoding and locale configuration that are required by FerretDB:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: createuserdb
  namespace: ferretdb
spec:
  scripts:
  - name: create-user
    scriptFrom:
      secretKeyRef:
        name: createuser
        key: sql
  - name: create-database
    script: |
            create database ferretdb owner ferretdb encoding 'UTF8' locale 'en_US.UTF-8' template template0;
```

```sh
kubectl apply -f 04-sgscript.yaml
```

We are now ready to create the Postgres cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: ferretdb
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
kubectl apply -f 05-sgcluster.yaml
```

After some seconds to a few minutes, the cluster should be brought up:

```sh
kubectl -n ferretdb get pods
NAME                           READY   STATUS    RESTARTS   AGE
postgres-0                     6/6     Running   0          16m
```

And the database `ferretdb` should exist and being owned by the user with the same name:

```sh
kubectl -n ferretdb exec -it postgres-0 -c postgres-util -- psql -l ferretdb
                                                 List of databases
   Name    |  Owner   | Encoding |   Collate   |    Ctype    | ICU Locale | Locale Provider |   Access privileges   
-----------+----------+----------+-------------+-------------+------------+-----------------+-----------------------
 ferretdb  | ferretdb | UTF8     | en_US.UTF-8 | en_US.UTF-8 |            | libc            | 
  ...
```


## Deploy FerretDB

FerretDB itself is a stateless application, and as such we can use the usual pattern of a `Deployment` (in case we want to easily scale it up) with a `Service` to deploy it:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ferretdb-dep
  namespace: ferretdb
  labels:
    app: ferretdb
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ferretdb
  template:
    metadata:
      labels:
        app: ferretdb
    spec:
      containers:
        - name: ferretdb
          image: ghcr.io/ferretdb/ferretdb
          ports:
            - containerPort: 27017
          env:
            - name: FERRETDB_POSTGRESQL_URL
              value: postgres://postgres/ferretdb

---

apiVersion: v1
kind: Service
metadata:
  name: ferretdb
  namespace: ferretdb
spec:
  selector:
    app: ferretdb
  ports:
    - name: mongo
      protocol: TCP
      port: 27017
      targetPort: 27017
```

```sh
kubectl apply -f 06-ferretdb.yaml
```

Note the line where we pass the `FERRETDB_POSTGRESQL_URL` environment variable to FerretDB's container, set with the value `postgres://postgres/ferretdb`: the second `postgres` on the string is the `Service` name that StackGres exposes pointing to the primary instance of the created cluster, which is named after the `SGCluster`'s name; and `ferretdb` is the name of the database.

If all goes well, you should see the pod up & running. To test it, we need to run a MongoDB client. For example, we can use `kubectl run` to run a `mongosh` image:

```sh
kubectl -n ferretdb run mongosh --image=rtsp/mongosh --rm -it -- bash
13:01:14 mongosh:/#
```

FerretDB exposes at the MongoDB wire protocol level the same database, username and passwords that exist in the Postgres database. Therefore, we can use the user, password and database that were created before via the `SGScript`. At the terminal prompt, type the command:

```sh
mongosh mongodb://ferretdb:${PASSWORD}@${FERRETDB_SVC}/ferretdb?authMechanism=PLAIN
```

where `${PASSWORD}` is the randomly generated password of the `ferretdb` user in Postgres:

```sh
kubectl -n ferretdb get secret createuser --template '{{ printf "%s\n" (.data.sql | base64decode) }}'
```

and `${FERRETDB_SVC}` is the address exposed by the FerretDB `Service` (`10.43.178.142` in the example below):

```sh
kubectl -n ferretdb get svc ferretdb
NAME       TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)     AGE
ferretdb   ClusterIP   10.43.178.142   <none>        27017/TCP   23m
```


The `mongosh` command should connect. You can now try to insert and query some data:

```js
ferretdb> db.test.insertOne({a:1})
{
  acknowledged: true,
  insertedId: ObjectId("644ef57e201f475adf06c355")
}
ferretdb> db.test.find()
[ { _id: ObjectId("644ef57e201f475adf06c355"), a: 1 } ]
```

If you are curious, you can see how data was materialized on the Postgres database:

```sql
kubectl -n ferretdb exec -it postgres-0 -c postgres-util -- psql ferretdb
psql (15.1 (OnGres 15.1-build-6.18))
Type "help" for help.

ferretdb=# set search_path to ferretdb;
SET
ferretdb=# \dt
                     List of relations
  Schema  |            Name             | Type  |  Owner   
----------+-----------------------------+-------+----------
 ferretdb | _ferretdb_database_metadata | table | ferretdb
 ferretdb | test_afd071e5               | table | ferretdb
(2 rows)

ferretdb=# table test_afd071e5;
                                                           _jsonb                                                            
-----------------------------------------------------------------------------------------------------------------------------
 {"a": 1, "$s": {"p": {"a": {"t": "int"}, "_id": {"t": "objectId"}}, "$k": ["_id", "a"]}, "_id": "644ef57e201f475adf06c355"}
(1 row)
```


## Cleanup

Deleting the namespace should clean all used resources:

```sh
kubectl delete namespace ferretdb
```
