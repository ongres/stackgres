---
title: Recover PGDATA from existing volume
weight: 5
url: runbooks/recover-pgdata-from-existing-volume
description: How recover postgres PGDATA from an existing volume
showToc: true
---

This runbook will show you the process to recover postgres data from an existing volume


## Scenario

For the purpose of this runbook we'll asume that you already have an SGCluster running as is shown below.

### SGCluster

```
❯ kubectl get pods -n demo-db
NAME                READY   STATUS    RESTARTS   AGE
demo-db-0           7/7     Running   0          7m52s
distributedlogs-0   3/3     Running   0          10m
```

### Database

```
❯ kubectl exec -it -n demo-db demo-db-0 -c postgres-util -- psql
psql (12.6 OnGres Inc.)
Type "help" for help.

postgres=# \c demo
You are now connected to database "demo" as user "postgres".
demo=# create table person(i integer, name char(50));
CREATE TABLE
demo=# insert into person(1,'Kadaffy Talavera');

demo=# insert into person(i,name) values (1,'Kadaffy Talavera');
INSERT 0 1
demo=# select *from person;
 i |                        name
---+----------------------------------------------------
 1 | Kadaffy Talavera
(1 row)
```

### PersistenVolumen

```
❯ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                                            STORAGECLASS   REASON   AGE
pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            Delete           Bound    demo-db/distributedlogs-data-distributedlogs-0   standard                58s
pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b   50Gi       RWO            Retain           Bound    demo-db/demo-db-data-demo-db-0                   ssd-data                19s
```

## Delete the current SGCluster

```bash
kubectl delete sgcluster -n demo-db demo-db
```

Check that PVC not longer exist:

```bash
❯ kubectl get pvc -n demo-db
NAME                                     STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
distributedlogs-data-distributedlogs-0   Bound    pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            standard       7m12s
```

Check that the PV now is in state `Released`

```bash
❯ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS     CLAIM                                            STORAGECLASS   REASON   AGE
pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            Delete           Bound      demo-db/distributedlogs-data-distributedlogs-0   standard                8m20s
pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b   50Gi       RWO            Retain           Released   demo-db/demo-db-data-demo-db-0                   ssd-data                7m41s

```

## Create a new cluster with replicas=1

```bash
❯ kubectl get pods -n demo-db
NAME                READY   STATUS    RESTARTS   AGE
demo-db-0           7/7     Running   0          87s
distributedlogs-0   3/3     Running   0          31m
```

Check that it is a new cluster:

```bash
❯ kubectl exec -it -n demo-db demo-db-0 -c postgres-util -- psql -c '\l'
                              List of databases
   Name    |  Owner   | Encoding | Collate |  Ctype  |   Access privileges
-----------+----------+----------+---------+---------+-----------------------
 postgres  | postgres | UTF8     | C.UTF-8 | C.UTF-8 |
 template0 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +
           |          |          |         |         | postgres=CTc/postgres
 template1 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +
           |          |          |         |         | postgres=CTc/postgres
(3 rows)
```

Check the new PVC:

```bash
❯ kubectl get pvc -n demo-db
NAME                                     STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
demo-db-data-demo-db-0                   Bound    pvc-13949341-8e30-4100-bdd8-dea148ea6894   50Gi       RWO            ssd-data       2m19s
distributedlogs-data-distributedlogs-0   Bound    pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            standard       11m

```

Check the PV's:

```bash
❯ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS     CLAIM                                            STORAGECLASS   REASON   AGE
pvc-13949341-8e30-4100-bdd8-dea148ea6894   50Gi       RWO            Retain           Bound      demo-db/demo-db-data-demo-db-0                   ssd-data                2m37s
pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            Delete           Bound      demo-db/distributedlogs-data-distributedlogs-0   standard                11m
pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b   50Gi       RWO            Retain           Released   demo-db/demo-db-data-demo-db-0                   ssd-data                10m
```

**At this point you'll have the PV from the previous cluster and the new one.**

## Backup the PVC and PV(The one we need to recover):

```bash
❯ kubectl get pvc -n demo-db demo-db-data-demo-db-0 -o yaml > demo-pvc-backup.yaml
❯ kubectl get pvc -n demo-db demo-db-data-demo-db-0 -o yaml > demo-pvc.yaml
❯ kubectl get pv pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b -o yaml > demo-0-pv-backup.yaml
❯ kubectl get pv pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b -o yaml > demo-0-pv.yaml
```

>Note: For security reason create a copy of each one.

## Stop the reconciliation cycle for sgclusters

```bash
❯ kubectl annotate sgclusters.stackgres.io -n demo-db demo-db stackgres.io/reconciliation-pause="true"
sgcluster.stackgres.io/demo-db annotated
```

This will allow  to scale the Statefulset to 0

```bash
❯ kubectl scale sts -n demo-db --replicas=0 demo-db
statefulset.apps/demo-db scaled
```

Check the pod no longer exist:

```bash
❯ kubectl get pods -n demo-db
NAME                READY   STATUS    RESTARTS   AGE
distributedlogs-0   3/3     Running   0          20m
```

Check the PVC:

```bash
❯ kubectl get pvc -n demo-db
NAME                                     STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
demo-db-data-demo-db-0                   Bound    pvc-13949341-8e30-4100-bdd8-dea148ea6894   50Gi       RWO            ssd-data       12m
distributedlogs-data-distributedlogs-0   Bound    pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            standard       21m
```

Check the PVs:

```bash
❯ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS     CLAIM                                            STORAGECLASS   REASON   AGE
pvc-13949341-8e30-4100-bdd8-dea148ea6894   50Gi       RWO            Retain           Bound      demo-db/demo-db-data-demo-db-0                   ssd-data                12m
pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            Delete           Bound      demo-db/distributedlogs-data-distributedlogs-0   standard                21m
pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b   50Gi       RWO            Retain           Released   demo-db/demo-db-data-demo-db-0                   ssd-data                20m
```

The main idea is:

recover:
`pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b`

delete:
`pvc-13949341-8e30-4100-bdd8-dea148ea6894`


Delete the PVC with not data:

```bash
❯ kubectl delete pvc -n demo-db demo-db-data-demo-db-0
persistentvolumeclaim "demo-db-data-demo-db-0" deleted
```

Check de PVs, you'll see that both are now in a `Released` state:

```bash
❯ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS     CLAIM                                            STORAGECLASS   REASON   AGE
pvc-13949341-8e30-4100-bdd8-dea148ea6894   50Gi       RWO            Retain           Released   demo-db/demo-db-data-demo-db-0                   ssd-data                18m
pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            Delete           Bound      demo-db/distributedlogs-data-distributedlogs-0   standard                27m
pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b   50Gi       RWO            Retain           Released   demo-db/demo-db-data-demo-db-0                   ssd-data                26m
```

Delete the PV with not data:

```bash
❯ kubectl delete pv pvc-13949341-8e30-4100-bdd8-dea148ea6894
persistentvolume "pvc-13949341-8e30-4100-bdd8-dea148ea6894" deleted
```

Modify the `demo-0-pvc.yaml` file:

- delete the status section
- set `volumenName` to `pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b`
- delete the PVC `uid`  (uid: 13949341-8e30-4100-bdd8-dea148ea6894)
- delete the `resourceVersion: "11353"`
- delete the `creationTimestamp: "2021-11-04T05:33:46Z"`


Create the PVC:

```bash
❯ kubectl create -f demo-0-pvc.yaml
persistentvolumeclaim/demo-db-data-demo-db-0 created
```

Check the state of the PVC:

```bash
❯ kubectl get pvc -n demo-db
NAME                                     STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
demo-db-data-demo-db-0                   Lost     pvc-13949341-8e30-4100-bdd8-dea148ea6894   0                         ssd-data       7s
distributedlogs-data-distributedlogs-0   Bound    pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            standard       39m
```

get the `uid` from the new PVC and update the PV `claimRef` section in `demo-0-pv.yaml` file with only the next parameters:

```yaml
claimRef:
  name: demo-db-data-demo-db-0
  namespace: demo-db
  uid: e4d64ab4-82bd-4f64-ad56-6f61da61275f
```
> **Note:** The uid in the ClaimRef is the PVC's uid.

then replace the PV:

```bash
❯ kubectl replace -f demo-0-pv.yaml
persistentvolume/pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b replaced
```

Now the PV should have the state `Bound`:

```bash
❯ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                                            STORAGECLASS   REASON   AGE
pvc-1b9b4811-c618-4869-ae02-819f5b8c8cd3   50Gi       RWO            Delete           Bound    demo-db/distributedlogs-data-distributedlogs-0   standard                55m
pvc-7528761f-48a3-4aaa-a071-74cb0dd1a65b   50Gi       RWO            Retain           Bound    demo-db/demo-db-data-demo-db-0                   ssd-data                6m43s
```

Remove the annotation `initialize` from endpoint `demo-db-config`:

```bash
❯ kubectl edit endpoints -n demo-db demo-db-config
endpoints/demo-db-config edited
```

## Scale the SGCluster to 1

```bash
❯ kubectl scale sts -n demo-db --replicas=1 demo-db
statefulset.apps/demo-db scaled
```

You should now see your pod running normally:

```bash
❯ kubectl get pods -n demo-db
NAME                READY   STATUS    RESTARTS   AGE
demo-db-0           7/7     Running   0          37s
distributedlogs-0   3/3     Running   0          70m
```

And your data recovered:

```bash
❯ kubectl exec -it -n demo-db demo-db-0 -c postgres-util -- psql -c '\l'
                              List of databases
   Name    |  Owner   | Encoding | Collate |  Ctype  |   Access privileges
-----------+----------+----------+---------+---------+-----------------------
 demo      | postgres | UTF8     | C.UTF-8 | C.UTF-8 |
 postgres  | postgres | UTF8     | C.UTF-8 | C.UTF-8 |
 template0 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +
           |          |          |         |         | postgres=CTc/postgres
 template1 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +
           |          |          |         |         | postgres=CTc/postgres
(4 rows)
```

```bash
❯ kubectl exec -it -n demo-db demo-db-0 -c postgres-util -- psql -d demo -c 'select * from person;'
 i |                        name
---+----------------------------------------------------
 1 | Kadaffy Talavera
(1 row)
```


## Last Steps

1- Update [password](https://stackgres.io/doc/latest/administration/passwords/) from users:

- postgres
- authenticator
- replicator

with the ones in the [secrets](https://stackgres.io/doc/latest/administration/passwords/).

2- Update the sgcluster annotation to `false` to enable the reconciliation cycle for SGClusters.

```bash
❯ kubectl annotate sgclusters.stackgres.io -n demo-db demo-db stackgres.io/reconciliation-pause="false" --overwrite
```
