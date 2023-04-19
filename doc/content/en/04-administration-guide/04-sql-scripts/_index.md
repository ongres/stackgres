---
title: SQL Scripts
weight: 4
url: administration/sql-scripts
aliases: [ tutorial/using-initialdata/scripts, tutorial/using-managed-sql/scripts ]
description: "Details about how manage state and data using SQL scripts"
showToc: true
---

In StackGres, you can define SQL scripts that are executed on the database instances.
The [SGScript]({{% relref "06-crd-reference/11-sgscript" %}}) type is used to define these scripts that are referenced in a StackGres cluster.

There are multiple ways to define scripts, depending on whether you need to define them as custom resource, within a config map, or within a secret.


## StackGres Scripts

The SGScript type can be used to define scripts in the resources inline.

This shows an example SQL script as SGScript:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
 name: stackgres-script
spec:
 scripts:
 - name: create-my-database
   script: |
    create database my_db owner postgres;
```

The script is referenced via its name in the StackGres cluster definition:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
 name: stackgres
spec:
 managedSql:
  scripts:
  - sgScript: stackgres-script
```

> **Note:** For sensitive data like credentials, choose the method via secrets, instead.


## ConfigMaps

It's also possible to define SQL scripts inside config maps.

This shows how to create a config map that contains the SQL script:

```
kubectl create configmap init-tables --from-literal=create-init-tables.sql="create table company(id integer, name char(50));"
```

> **Note:** To load more complex or larger queries, you can [create the configmap from your sql files](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#create-configmaps-from-files).

The config map can be referenced in the StackGres cluster as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres-script
spec:
  scripts:
  - name: create-database-user
    scriptFrom:
      configMapKeyRef:
        name: init-tables
        key: create-init-tables.sql
```


## Secrets

For sensitive data such as credentials, a secret is the preferred way to define SQL scripts.

This shows how to create a secret:

```
kubectl create secret generic database-user --from-literal=create-user.sql="create user demo password 'demo'"
```

You can reference the secret in the StackGres cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: stackgres-script
spec:
  scripts:
  - name: create-database-user
    scriptFrom:
      secretKeyRef:
        name: database-user
        key: create-user.sql
```
