---
title: Restoring a Backup
weight: 3
url: /runbooks/restore-backup
description: Details about how to restore a StackGres cluster backup.
showToc: true
---

This runbook will show you how to restore a StackGres cluster backup.
All the steps explained here are also possible from the StackGres web console.

## Checking the Database Size

Check the databases and sizes of your StackGres cluster.

The demo cluster `ongres-db` has one database:

```
$ kubectl exec -it --namespace ongres-db ongres-db -c postgres-util -- psql -c '\l'
                              List of databases
   Name    |  Owner   | Encoding | Collate |  Ctype  |   Access privileges   |  Size   | Tablespace |                Description
-----------+----------+----------+---------+---------+-----------------------+---------+------------+--------------------------------------------
 demo_db   | postgres | UTF8     | C.UTF-8 | C.UTF-8 |                       | 20 MB   | pg_default |
 postgres  | postgres | UTF8     | C.UTF-8 | C.UTF-8 |                       | 7977 kB | pg_default | default administrative connection database
 template0 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +| 7793 kB | pg_default | unmodifiable empty database
           |          |          |         |         | postgres=CTc/postgres |         |            |
 template1 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +| 7793 kB | pg_default | default template for new databases
           |          |          |         |         | postgres=CTc/postgres |         |            |
(4 rows)
```

## Retrieving the Backup List

Get the backup list:

```
$ kubectl get sgbackups --namespace ongres-db

NAME            AGE
backup-demo-1   3h33m
backup-demo-2   3h11m
backup-demo-3   55s
```

## Configuring the Instance Profile

The restore operation includes creating a new [cluster](https://stackgres.io/doc/latest/reference/crd/sgcluster/) from a previously performed backup.
You're able to specify any of the cluster params.
If you do not specify an [SGInstanceProfile](https://stackgres.io/doc/latest/reference/crd/sginstanceprofile/), it will use the default profile with `1` CPU and `2Gi` of RAM.

Create an instance profile specific to the restore cluster.
Change the resources according to your requirements.

Create a YAML file with the following content and apply it to Kubernetes:

```
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  namespace: ongres-db
  name: size-s
spec:
  cpu: "500m"
  memory: "256Mi"
```

> Note: The restore process needs to be performed in the same namespace as the cluster which you want to backup.

## Restoring the Backup

To restore the backup you need to create a new `SGCluster` specifying the `initialData` section setting the param `fromBackup` with the backup resource name.

Create a YAML file similar to the following content and apply it to Kubernetes:

```
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: demo-restore
  namespace: ongres-db
spec:
  instances: 1
  postgres:
    version: '12'
  sgInstanceProfile: 'size-s'
  pods:
    persistentVolume:
      size: '10Gi'
  initialData:
    restore:
      fromBackup:
        name: backup-demo-3
```

Now, you should have a new cluster called `demo-restore` with all the data restored:

```
$ kubectl exec -it -n ongres-db demo-restore-0 -c postgres-util -- psql -c '\l+'
                                                                List of databases
   Name    |  Owner   | Encoding | Collate |  Ctype  |   Access privileges   |  Size   | Tablespace |                Description
-----------+----------+----------+---------+---------+-----------------------+---------+------------+--------------------------------------------
 demo_db   | postgres | UTF8     | C.UTF-8 | C.UTF-8 |                       | 20 MB   | pg_default |
 postgres  | postgres | UTF8     | C.UTF-8 | C.UTF-8 |                       | 7977 kB | pg_default | default administrative connection database
 template0 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +| 7793 kB | pg_default | unmodifiable empty database
           |          |          |         |         | postgres=CTc/postgres |         |            |
 template1 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +| 7793 kB | pg_default | default template for new databases
           |          |          |         |         | postgres=CTc/postgres |         |            |
(4 rows)
```

