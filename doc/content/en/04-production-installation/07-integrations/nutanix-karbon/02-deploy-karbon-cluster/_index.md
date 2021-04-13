---
title: StackGres Cluster Deploy
weight: 2
url: install/integrations/nutanix-karbon/cluster-deploy
---

# StackGres Cluster Deploy

The StackGres Operator and RestApi have been installed with success and the web access is ready, now you can proceed with the StackGres Cluster deployment.

The cluster could be created with default parameters, but to get the most of this, several resources will be created to show the versatility of StackGres. 
You can open and inspect the YAML files to understand the parameters of the resources following the [StackGres Documentation]({{% relref "04-production-installation/01-pre-requisites/03-backups/" %}}).

Lets create the cluster starting with a custom profile for instances.

```sh
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGInstanceProfile
metadata:
  namespace: karbon
  name: size-s
spec:
  cpu: "500m"
  memory: "512Mi"
EOF
```

Create a Postgres custom configuration:

```sh
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGPostgresConfig
metadata:
  namespace: karbon
  name: pgconfig
spec:
  postgresVersion: "12"
  postgresql.conf:
    shared_buffers: '256MB'
    random_page_cost: '1.5'
    password_encryption: 'scram-sha-256'
    checkpoint_timeout: '30'
    max_connections: '100'
    jit: 'off'
EOF
```

Create a specific pooling configuration:

```sh
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGPoolingConfig
metadata:
  namespace: karbon
  name: poolconfig
spec:
  pgBouncer:
    pgbouncer.ini:
      pool_mode: transaction
      max_client_conn: '2000'
      default_pool_size: '50'
      log_connections: '1'
      log_disconnections: '1'
      log_stats: '1'
EOF
```

And create a resource for Distributed logs:

```sh
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGDistributedLogs
metadata:
  name: distributedlogs
  namespace: karbon
spec:
  persistentVolume:
    size: 50Gi
EOF
```

## Backups

StackGres support Backups with the following storage options
    - AWS S3
    - S3 Compatible Storage
    - Google Cloud Storage
    - Azure Blob Storage

Depending on the storage you choose check the StackGres backups Documentation to verify the params according to your choice.
For the purpose an S3 Compatible Storage (Minio) will be configured:

Create minio configuration:

```sh
kubectl apply -f https://gitlab.com/ongresinc/stackgres-tutorial/-/blob/master/sg_demo_karbon/07-minio.yaml
```

Create the backups configuration:

```sh
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGBackupConfig
metadata:
  name: backupconfig
  namespace: karbon
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

Create a k8s secret with the SQL sentence to create the some user

```sh
cat << EOF | kubectl apply -f -
kubectl -n karbon create secret generic admin-user-password --from-literal=admin-create-user-sql="create user admin password 'admin'"
```

All the above configuration resources will be used to create an SGCLuster:

```sh
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  namespace: karbon
  name: karbon-db
spec:
  postgresVersion: '12.3'
  instances: 3
  sgInstanceProfile: 'size-s'
  pods:
    persistentVolume:
      size: '20Gi'
  configurations:
    sgPostgresConfig: 'pgconfig'
    sgPoolingConfig: 'poolconfig'
    sgBackupConfig: 'backupconfig'
  distributedLogs:
    sgDistributedLogs: 'distributedlogs'
  initialData:
    scripts:
    - name: create-admin-user
      scriptFrom:
        secretKeyRef:
          name: admin-user-password
          key: admin-create-user-sql
    - name: create-database
      script: |
        create database admin owner admin;
  prometheusAutobind: true
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
EOF
```

As you can see, we included the initialData section, which give us the option to run our custom scripts, or SQL commands.
Now the PostgreSQL cluster could be inspected and monitored through the web console or the kubectl CLI as you wish.

```sh
# kubectl get pods -n karbon
NAME                READY   STATUS    RESTARTS   AGE
distributedlogs-0   2/2     Running   0          10m
karbon-db-0         6/6     Running   0          2m40s
karbon-db-1         6/6     Running   0          2m7s
karbon-db-2         6/6     Running   0          96s
```

The StackGres Cluster installation could be verified using the next commands. 
It will show the PostgreSQL instances in the cluster and the postgres version installed.

```sh
kubectl exec -it -n demo-karbon karbon-db-0 -c patroni -- patronictl list
kubectl exec -it -n demo-karbon karbon-db-0 -c postgres-util -- psql -c "select version()"
```

## Summary.

StackGres Instllation and Cluster deploy are ready to work on a Nutanix Karbon environment as it was shown with the examples above.
All components from StackGres can be executed, configured and all the features work as expected.
