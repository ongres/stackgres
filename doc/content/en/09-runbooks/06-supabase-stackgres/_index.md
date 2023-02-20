---
title: Run Supabase on top of StackGres
weight: 6
url: runbooks/supabase-stackgres
description: How to run Supabase on top of Stackgres
showToc: true
---

This runbook will show you how to install [Supabase](https://supabase.com/) on Kubernetes, with a production-grade database provided by StackGres.


## Scenario

This runbook we'll assume that you already have a Kubernetes cluster with the StackGres operator installed.
We will create an SGCluster with a configuration that fits Supabase' requirements. 

### SGCluster

We will create an SGCluster in the usual way, with some additional configuration:

```yaml
# file cluster.yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: supabase-db
spec:
  instances: 2
  pods:
    persistentVolume:
      size: '2Gi'
  configurations:
    sgPoolingConfig: supabase-db
  postgres:
    version: '14.4'
    extensions:
      - name: pgsodium
      - name: pg_graphql
      - name: pg_stat_statements
      - name: pgcrypto
      - name: pgjwt
      - name: uuid-ossp
  managedSql:
    scripts:
      - sgScript: supabase-initdb
---
```

With this, we'll be creating a cluster with 2 instances, Postgres `14`, and a few extensions that are required by Supabase.
We're also referencing a custom script and pooling config.
The latter is required by how Supabase connects to our database -- via PgBouncer:

```yaml
# file poolconfig.yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  name: supabase-db
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        ignore_startup_parameters: extra_float_digits,search_path
---
```

The custom SGScript is a bit more complex and includes the initialization scripts that set up the Supabase schemas, as well as a small workaround that is required (as of StackGres `1.4.2`) to not make the PgBouncer `authenticator` role collide with what Supabase will create:

```yaml
# file script.yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: supabase-initdb
spec:
  scripts:
    - name: 00-reset-auth
      script: |
        alter role authenticator rename to sg_authenticator;
    - name: 01-initial-schema
      script: |
        -- Set up realtime
        create schema if not exists realtime;
        
        [...]
    - name: 02-auth-schema
      script: |
        CREATE SCHEMA IF NOT EXISTS auth AUTHORIZATION supabase_admin;
        
        [...]
    - name: 03-storage-schema
      script: |
        CREATE SCHEMA IF NOT EXISTS storage AUTHORIZATION supabase_admin;
        
        [...]
    - name: 04-post-setup
      script: |
        ALTER ROLE postgres SET search_path TO "\$user",public,extensions;
        
        [...]
    - name: 05-reset-auth
      script: |
        alter role authenticator INHERIT;
        alter role authenticator SUPERUSER;
---
```

We omitted the full scripts for readability.
You can find the full version [here](TODO).
The scripts are based on the unofficial Supabase Helm chart.

In order to create the SGCluster, we need to create these three resources:

```
kubectl apply -f poolconfig.yaml
kubectl apply -f script.yaml
kubectl apply -f cluster.yaml
```

We can then check the status of the running Postgres pods:

```
$ kubectl get pods
NAME            READY   STATUS    RESTARTS   AGE
supabase-db-0   6/6     Running   0          82s
supabase-db-1   6/6     Running   0          36s
```

And of course, we can double-check the status with the details:

```
kubectl describe sgcluster supabase-db 
```

After our SGCluster is up-and-running, we continue with the installation of Supabase.

### Supabase

We use the unofficial Supabase Helm chart for creating the Kubernetes resources.
We will need to make some small adjustments, so the easiest is to clone the repository and edit the provided values:

```
git clone https://github.com/supabase-community/supabase-kubernetes
cd supabase-kubernetes/charts/supabase
cp values.example.yaml my-values.yaml
```

We edit the contents of our `my-values.yaml`, to disable the database creation (since we use our own), to change the db host names, and URLs of the Supabase API and Studio.
For a first test it's sufficient to use a local port forwarding, so our example uses `localhost` addresses instead of Kubernetes ingress resources.
You might want to change this depending on your Kubernetes cluster and cloud setup.

You can find the full contents of our example `my-values.yaml` [here](TODO).

Before we can install the Supabase Helm chart, we also need to set up some secrets with the Supabase auth keys, and also our SGCluster credentials.

For this, we first find out the superuser password of our created SGCluster:

```
kubectl get secret supabase-db --template '{{ printf "%s" (index .data "superuser-password" | base64decode) }}'
```

We use this password along the default Supabase credentials to create the secrets:

```
# creates JWT secret
kubectl -n default create secret generic demo-supabase-jwt \
  --from-literal=anonKey='eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ewogICAgInJvbGUiOiAiYW5vbiIsCiAgICAiaXNzIjogInN1cGFiYXNlIiwKICAgICJpYXQiOiAxNjc1NDAwNDAwLAogICAgImV4cCI6IDE4MzMxNjY4MDAKfQ.ztuiBzjaVoFHmoljUXWmnuDN6QU2WgJICeqwyzyZO88' \
  --from-literal=serviceKey='eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ewogICAgInJvbGUiOiAic2VydmljZV9yb2xlIiwKICAgICJpc3MiOiAic3VwYWJhc2UiLAogICAgImlhdCI6IDE2NzU0MDA0MDAsCiAgICAiZXhwIjogMTgzMzE2NjgwMAp9.qNsmXzz4tG7eqJPh1Y58DbtIlJBauwpqx39UF-MwM8k' \
  --from-literal=secret='abcdefghijklmnopqrstuvwxyz123456'

# creates SMTP secret
kubectl -n default create secret generic demo-supabase-smtp \
  --from-literal=username='your-mail@example.com' \
  --from-literal=password='example123456'

# creates DB secret
kubectl -n default create secret generic demo-supabase-db \
  --from-literal=username='postgres' \
  --from-literal=password='<your-superuser-password>' 
```

Then, we can install the Helm chart:

```
helm install demo -f my-values.yaml .
```

After the resources have been created, we can watch the status of the pods:

```
$ kubectl get pods --watch
NAME                                      READY   STATUS    RESTARTS   AGE
demo-supabase-auth-6b8c448b6b-njsh6       1/1     Running   0          2m
demo-supabase-kong-7c5bdfc9bf-cfb5q       1/1     Running   0          2m
demo-supabase-meta-54d7b47b89-sdzcb       1/1     Running   0          2m
demo-supabase-realtime-6c4dc64668-cxxfd   1/1     Running   0          2m
demo-supabase-rest-794594bb5f-tz4bh       1/1     Running   0          2m
demo-supabase-storage-6c766bb9cc-r8psb    1/1     Running   0          2m
demo-supabase-studio-59c86bd59b-k4p5w     1/1     Running   0          2m
supabase-db-0                             6/6     Running   0          3m
supabase-db-1                             6/6     Running   0          2m
```

In order to test the setup, we create a local port forwarding to the Supabase API and Studio:

```
kubectl port-forward svc/demo-supabase-studio 3000

# in a new terminal window
kubectl port-forward svc/demo-supabase-kong 8000
```

Navigating the browser to [http://localhost:3000/project/default/](http://localhost:3000/project/default/) should now show you the default Supabase project.

Congratulations! You're now running Supabase on top of a production-ready Postgres cluster.

### Supabase Hello World

For a quick test, we can use on of the Supabase quickstarts, for example the [React quickstart](https://supabase.com/docs/guides/getting-started/quickstarts/reactjs).


## TODO

```
PASSWORD=$(< /dev/urandom tr -dc '$/;._A-Z-a-z-0-9' | head -c${1:-20};echo;)
kubectl create secret generic supabase-admin-password --from-literal=supabase-admin-password.sql="alter user supabase_admin with password '$PASSWORD';"
```



## Cleanup

To clean up the created Supabase resources and the SGCluster, we issue the following:

```
helm uninstall demo

kubectl delete secret demo-supabase-db
kubectl delete secret demo-supabase-jwt
kubectl delete secret demo-supabase-smtp

kubectl delete sgcluster supabase-db
kubectl delete sgscript supabase-initdb
kubectl delete sgpoolconfig supabase-db
```