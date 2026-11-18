---
title: Creating an external cascade replica cluster
weight: 1
url: /administration/replication/remote/replicatefrom
description: This section details the cluster cascading replication.
---

The standby cluster feature is explained in the [SGCluster CRD]({{% relref "06-crd-reference/01-sgcluster/#sgclusterspecreplicatefrom" %}}) but here is a practical guide to accomplish the setup.

Since the standby cluster feature works through the Patroni [*Standby Cluster* concept](https://patroni.readthedocs.io/en/latest/standby_cluster.html), when using streaming replication, it is required that the main cluster leader member or a simple stand alone Postgres server, is accessible from the new cluster replica. Based on the DC architecture or k8s Cloud provider, enabling connections to the WAN must be done. Beforehand, consider that in k8s a service should be ready to expose the cluster service.

StackGres requires to setup 3 users in the `replicateFrom` spec using the specific keys `superuser`, `replication`, and `authenticator` (that may be the same user in the source server) in order to function properly. The 3 (or 2 or 1) users must exist in the main cluster that is being replicated. To create each of those users you can follow the next command examples:

* Superuser username:
```
CREATE ROLE postgres;
```
* Superuser password:
```
ALTER ROLE postgres WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS PASSWORD '***';
```
* Replication username:
```
CREATE ROLE replicator;
```
* Replication password:
```
ALTER ROLE replicator WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN REPLICATION NOBYPASSRLS PASSWORD '***';
```
* Authenticator username:
```
CREATE ROLE authenticator;
```
* Authenticator password:
```
ALTER ROLE authenticator WITH SUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '***';
```

> More details can be found in the [CRD reference]({{% relref "06-crd-reference/01-sgcluster/#sgclusterspecconfigurationscredentialsusers"%}})

Once access is granted, the next command can be used to test the connection:

```sh
psql -U <USER> -p 5433 -h <HOST> -d <database>
```

Then, the new StackGres Cluster will require the credentials for the users that will connect to the main Cluster. Since credentials are present here, they should be saved in a `Secret`.
The next example helps to understand how to create it, using the same names from the example above:

<!-- doc-check:skip -->
```yaml
apiVersion: v1
kind: Secret
metadata:
  labels:
    app: StackGresCluster
    stackgres.io/cluster-name: my-db
  name: mysecrets-db
  namespace: my-namespace
type: Opaque
data:
  authenticator-password: ***
  authenticator-username: authenticator
  replication-password: ***
  replication-username: replicator
  superuser-password: ***
  superuser-username: postgres
```

In the new remote StackGres deployment, where a new StackGres Cluster will be created as Standby Leader, equivalent CRDs are required before proceeding. 
The same steps should be applied, refer to the [Installation section]({{% relref "04-administration-guide/01-installation/"%}}) for details.

> Note: Currently, it is required to create the `postgresql.conf` and the `pg_hba.conf` files in the source data directory Postgres server if these files don't exist. There is an issue created about this bug, please see and follow instruction in https://gitlab.com/ongresinc/stackgres/-/issues/2821

Now, the environment is ready for the SGCluster to be created. The next example contains extra entries to give a wider view of the options included in a production-like system. Beware of review and complete fields as backups (if you will take backups from your Standby Cluster), the number of instances, and the port number exposed in the main cluster among others.

```yml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-db
  namespace: my-namespace
spec:
  configurations:
    observability:
      prometheusAutobind: true
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
      sgScript: my-db-initial-data
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
  replication:
    mode: async
    role: ha-read
    syncInstances: 2
  sgInstanceProfile: my-size
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

If there are no errors, the new pods should be created, but the patroni container will not be ready until the replica catch up with the leader. Take into account that depending on the data size and the network bandwidth it could take several hours. When the replica is ready, we should look the output of the following command:

```sh
$ kubectl -n my-namespace exec -it my-db-0 -c patroni -- patronictl list 
+ Cluster: my-db (7202191435613375243) ------+-----------+----+-----------+
| Member       | Host            | Role           | State     | TL | Lag in MB |
+--------------+-----------------+----------------+-----------+----+-----------+
| my-db-0 | 1.2.3.4:7433 | Standby Leader | streaming | 1 |           |
+--------------+-----------------+----------------+-----------+----+-----------+
```

## Using a custom restore method

By default, when bootstrapping a replica from an external instance StackGres relies on a standard Postgres base backup to fetch the initial copy of the data. In some scenarios this is not desirable, for example when the source data is very large, when a faster snapshot-based transfer is available, or when a custom restore tool must be used to seed the replica data directory.

For these cases the optional `spec.replicateFrom.instance.external.customRestoreMethod` object lets you define a Patroni [custom replica creation method](https://patroni.readthedocs.io/en/latest/replica_bootstrap.html#building-replicas), which is added to Patroni's `create_replica_methods`. It controls how the replica data is fetched from the external source instead of performing a standard base backup, allowing you to invoke a custom command or script (e.g. a snapshot restore or a third-party restore tool).

The same object is also available on `SGShardedCluster` under `spec.replicateFrom.external.customRestoreMethod` and is applied to every cluster of the sharded cluster.

The object supports the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `command` | string | The command to run in order to fetch the replica data. Mutually exclusive with `script`. |
| `script` | string | A script to run in order to fetch the replica data. Mutually exclusive with `command`. |
| `no_leader` | boolean | When `true`, the method is allowed to run without a leader being present. |
| `keep_data` | boolean | When `true`, the existing data directory is kept instead of being removed before the method runs. |
| `no_params` | boolean | When `true`, the standard Patroni parameters are not passed to the method. |
| `parameters` | map<string,string> | Extra parameters passed to the method. |
| `keep_existing_recovery_conf` | boolean | When `true`, an existing `recovery.conf` is preserved. |
| `recovery_conf` | map<string,string> | The `recovery.conf` settings to apply after the method has run. |

The following example replicates from an external instance using a custom restore method that runs a script to seed the replica data directory:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-db
  namespace: my-namespace
spec:
  instances: 2
  replicateFrom:
    instance:
      external:
        host: 1.2.3.4
        port: 30001
        customRestoreMethod:
          script: /path/to/my-restore-tool.sh
          no_leader: true
          keep_data: false
          no_params: false
          parameters:
            source: s3://my-bucket/my-db
            threads: "4"
          keep_existing_recovery_conf: false
          recovery_conf:
            restore_command: my-restore-tool fetch-wal %f %p
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

> Note: `command` and `script` are mutually exclusive, set only one of them.

