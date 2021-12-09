---
title: initialData Scripts
weight: 1
url: tutorial/using-initialdata/scripts
description: "Details about how use scripts in the initialData section"
---

With this option you can provide the script or many script as you required in three different ways:

1. **Raw format:**

Create the file `sgcluster-with-raw-script.yaml` and apply the following YAML file:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    scripts:
    - name: create-my-database
      script: |
        create database my_db owner postgres;
```

and deploy to Kubernetes:

```bash
kubectl apply -f sgcluster-with-raw-script.yaml
```

> **Note:** Avoid this method to create sensitive data like user and passwords.

1. **From a Secret:**

Using this method you need to create the secret first:

```bash
kubectl create secret generic database-user \
--from-literal=create-user.sql="create user demo password 'demo'"
```

Then add the reference in the initialData section:

Create the YAML file `sgcluster-with-secret-script.yaml`:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    scripts:
    - name: create-database-user
      scriptFrom:
        secretKeyRef:
          name: database-user
          key: create-user.sql
```

and deploy to Kubernetes:

```bash
kubectl apply -f sgcluster-with-secret-script.yaml
```

1. **From a ConfigMap:**

First create a configmap:

```bash
kubectl create configmap init-tables \
--from-literal=create-init-tables.sql="create table company(id integer, name char(50));"
```

> **Note:** To load more complex or larger queries [create the configmap from your sql files](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#create-configmaps-from-files).

Create the YAML file sgcluster-with-script-from-configmap.yaml

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    scripts:
    - name: create-database-user
      scriptFrom:
        configMapKeyRef:
          name: init-tables
          key: create-init-tables.sql
```

and deploy to Kubernetes:

```bash
kubectl apply -f sgcluster-with-script-from-configmap.yaml
```