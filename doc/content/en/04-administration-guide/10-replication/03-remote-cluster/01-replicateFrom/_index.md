---
title: Creating an external cascade replica cluster
weight: 1
url: /administration/replication/remote/replicatefrom
description: Is time to create our Stackgres cascading replication cluster and start touching the details.
---

# replicateFrom

The `replicateFrom` feature is explained in the [SGCluster CRD](https://stackgres.io/doc/latest/reference/crd/sgcluster/#sgclusterspecreplicatefrom) but here is a practical guide to accomplish the setup

Since `replicateFrom` works through the Patroni (*Standby Cluster* concept)[https://patroni.readthedocs.io/en/latest/standby_cluster.html], access from the new cluster replica to the Main Cluster leader member is required. Based on the DC architecture or k8s Cloud provider, enabling connections to the WAN must be done. Beforehand, consider that the k8s service should be ready to expose the cluster service.

Once access is granted, the next command can be used to test the connection:

```sh
psql -U <USER> -p 5433 -h <HOST> -d <database>
```

Then, the new Stackgres Cluster will require the credentials for the user that will connect to the main Cluster, the classical way is by using a Postgres user named "replication" or similar. Since credentials are being present here, it should be saved into `secrets`.


It the new remote Stackgres deployment, where a new Stackgres Cluster will be created as Standby Leader, equal CRDs are needed after creating it. Create them accordingly as follows:

- Namespace
- StorageClass - Setting up the same storage or better performance is strongly recommended.
- SGInstanceProfile
- SGPostgresConfig
- SGPoolingConfig
- Service
- SGBackupConfig (if any)
- SGScript (if any)
- Secrets

Now, the environment is ready to apply the SGCluster. The next example contains extra entries to give a wider view of the options included in a production-like system. Beware of review and complete fields as backups (if you will take backups from your Standby Cluster), the number of instances among others, and the port number exposed in the main cluster.

```yml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-db
  namespace: my-namespace
spec:
  configurations:
    backups:
    - compression: lz4
      cronSchedule: 0 0 1 * *
      performance:
        uploadDiskConcurrency: 1
      reconciliationTimeout: 300
      retention: 1
      sgObjectStorage: backupconfig
    sgPoolingConfig: poolconfig
    sgPostgresConfig: pgconfig
  initialData: {}
  instances: 2
  managedSql:
    continueOnSGScriptError: false
    scripts:
    - id: 0
      sgScript: my-db-default
    - id: 1
      sgScript: my-db-inital-data
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
    disableClusterResourceRequirements: true
    enableSetClusterCpuRequests: false
    enableSetClusterMemoryRequests: false
    enableSetPatroniCpuRequests: false
    enableSetPatroniMemoryRequests: false
  pods:
    disableConnectionPooling: false
    disableMetricsExporter: false
    disablePostgresUtil: false
    managementPolicy: OrderedReady
    persistentVolume:
      size: 1Ti
      storageClass: gp2-sg
    resources:
      disableResourcesRequestsSplitFromTotal: true
      enableClusterLimitsRequirements: false
  postgres:
    extensions:
    - name: pg_repack
      publisher: com.ongres
      version: 1.4.8
    flavor: vanilla
    version: "16.1"
  postgresServices:
    primary:
      enabled: true
      type: ClusterIP
    replicas:
      enabled: true
      type: ClusterIP
  profile: production
  prometheusAutobind: true
  replication:
    mode: async
    role: ha-read
    syncInstances: 2
  sgInstanceProfile: my-size
  toInstallPostgresExtensions:
  - build: "6.20"
    name: pg_repack
    postgresVersion: "15"
    publisher: com.ongres
    repository: https://extensions.stackgres.io/postgres/repository
    version: 1.4.8
  replicateFrom:
    instance:
      external:
        host: 1.2.3.4
        port: 30001
    users:
      superuser:
        username:
          name: mysecrets-db
          key: superuser-username
        password:
          name: mysecrets-db
          key: superuser-password
      replication:
        username:
          name: mysecrets-db
          key: replication-username
        password:
          name: mysecrets-db
          key: replication-password
      authenticator:
        username:
          name: mysecrets-db
          key: authenticator-username
        password:
          name: mysecrets-db
          key: authenticator-password
```

If no errors, the new pods must be created, but the patroni container won't be ready until the replica catch up with the Leader. Take into account that depending on the data size and the network bandwith it could take several hours. When the replica is ready, we should look an output as follow:

```sh
$ kubectl -n my-namespace exec -it my-db-0 -c patroni -- patronictl list 
+ Cluster: my-db (7202191435613375243) ------+-----------+----+-----------+
| Member       | Host            | Role           | State     | TL | Lag in MB |
+--------------+-----------------+----------------+-----------+----+-----------+
| my-db-0 | 1.2.3.4:7433 | Standby Leader | streaming | 1 |           |
+--------------+-----------------+----------------+-----------+----+-----------+
```

