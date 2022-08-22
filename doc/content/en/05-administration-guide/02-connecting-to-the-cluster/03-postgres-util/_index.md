---
title: Local connection with the postgres-util sidecar
weight: 3
url: administration/cluster/connection/pgutil
description: Describes how to connect on the cluster using kubectl and the postgres-util sidecar container.
showToc: true
---

Local Connection to the database has to be through the `postgres-utils` sidecar. This sidecar has all PostgreSQL binaries that are not present in the main container called `patroni` like the `psql` command.

This main container only have the required binaries and utilities to be able to configure the postgres cluster and the HA configuration.


## Access to postgres-util sidecar

First we'll check the if the container is present in the pods, for these example we have a cluster named stackgres, composed of three pods and installed in the default namespace:

```bash
kubectl get pods -n default -l app=StackGresCluster,stackgres.io/cluster=true
```

output:

```
NAME          READY   STATUS    RESTARTS   AGE
stackgres-0   5/5     Running   0          12m
stackgres-1   5/5     Running   0          12m
stackgres-2   5/5     Running   0          11m
```

As you can see in the list we have `5/5` containers (sidecars) ready. To check the list of these containers we can run the next command:

```bash
kubectl get pods stackgres-0 -n default -o jsonpath='{.spec.containers[*].name}*'
```

output:

```
patroni envoy pgbouncer postgres-util prometheus-postgres-exporter
```

At this point we already checked that sidecar `postgres-util` is up and running. Now to access the postgres instance through this sidecar you have two options:

1. **Access directly from the** `kubectl` **commmand**

    `kubectl exec -it stackgres-0 -c postgres-util -- psql`

    Then you will be into the postgresql console. You can access through the port `5432` this will connect via unix socket directly to postgres instances and will not required a password or you can use the port `6432` and the connection will  be through the conection pooling tool (pgbouncer) and you will be ask for the password to connect.

    ```
    psql (11.6 OnGres Inc.)
    Type "help" for help.

    postgres=#
    ```

2. **Access the sidecar console**

    To access the sidecar console run the next command:

    ```bash
    ➜ kubectl exec -it simple-0 -c postgres-util -- bash
    bash-4.4$ 
    ```

    > **Note:** You will be able to run any linux command and have access to all the PostgreSQL binaries.

    Connect to postgres console:

    ```bash
    bash-4.4$ psql
    psql (12.3 OnGres Inc.)
    Type "help" for help.

    postgres=# 

    ```