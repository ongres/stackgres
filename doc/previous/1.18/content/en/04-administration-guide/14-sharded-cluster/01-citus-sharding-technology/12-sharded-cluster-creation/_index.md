---
title: Creating a Sharded Cluster
weight: 12
url: /administration/sharded-cluster/creation
description: Details about how to create a production StackGres sharded cluster.
showToc: true
---

## Customizing Your Postgres Sharded Clusters

Refer to [Customizing Your Postgres Clusters]({{% relref "04-administration-guide/02-cluster-creation" %}}#customizing-your-postgres-clusters) section for more details on the configuration used
 for the sharded cluster. In particular you will end up creating the following custom resources in the `my-cluster` namespace:

* An [SGInstanceProfile]({{% relref "04-administration-guide/04-configuration/01-instance-profile" %}}) called `size-small`
* An [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) called `pgconfig`
* An [SGPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}}) called `poolconfig`
* An [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) called `backupconfig`
* An [SGDistributedLogs]({{% relref "06-crd-reference/07-sgdistributedlogs" %}}) called `distributedlogs`

## Creating a Citus Sharded Cluster

This section will guide you through the creation of a production-ready StackGres sharded cluster using Citus and your custom configuration.

### Configuring Scripts

Last but not least, StackGres lets you include several `managedSql` scripts, to perform cluster operations at startup.

In this example, we're creating a Postgres user, using a Kubernetes secret and a sharded table using Citus:

```
kubectl -n my-cluster create secret generic pgbench-user-password-secret \
  --from-literal=pgbench-create-user-sql="create user pgbench password 'admin123'"
```

Then we reference the secret in a [SGScript]({{% relref "06-crd-reference/10-sgscript" %}}):

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  namespace: my-cluster
  name: cluster-scripts
spec:
  scripts:
  - name: create-pgbench-user
    scriptFrom:
      secretKeyRef:
        name: pgbench-user-password-secret
        key: pgbench-create-user-sql
  - name: create-pgbench-tables
    database: mydatabase
    user: pgbench
    script: |
      CREATE TABLE pgbench_accounts (
          aid integer NOT NULL,
          bid integer,
          abalance integer,
          filler character(84)
      );
  - name: distribute-pgbench-tables
    database: mydatabase
    user: pgbench
    script: |
      SELECT create_distributed_table('pgbench_history', 'aid');
```

The scripts are defined both by the Secret created before and SQL instructions inline.

The SGScript will be referenced in the `managedSql` definition for the coordinator of the sharded cluster, shown below.

Note that we could equally well define the SQL script in a config map, however, since the password represents a credential, we're using a secret.

### Creating the Citus Sharded Cluster

All the required steps were performed to create our StackGres Cluster.

Create the SGShardedCluster resource:

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGShardedCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  type: citus
  database: mydatabase
  postgres:
    version: '15.3'
  coordinator:
    instances: 2
    sgInstanceProfile: 'size-small'
    pods:
      persistentVolume:
        size: '10Gi'
    configurations:
      sgPostgresConfig: 'pgconfig'
      sgPoolingConfig: 'poolconfig'
    managedSql:
      scripts:
      - sgScript: cluster-scripts
  shards:
    clusters: 3
    instancesPerCluster: 2
    sgInstanceProfile: 'size-small'
    pods:
      persistentVolume:
        size: '10Gi'
    configurations:
      sgPostgresConfig: 'pgconfig'
      sgPoolingConfig: 'poolconfig'
  configurations:
    observability:
      prometheusAutobind: true
    backups:
    - sgObjectStorage: 'backupconfig'
      cronSchedule: '*/5 * * * *'
      retention: 6
  distributedLogs:
    sgDistributedLogs: 'distributedlogs'
```

Notice that each resource has been defined with its own `name`, and is referenced in the StackGres sharded cluster definition.
The order of the CR creation is relevant to successfully create a sharded cluster, that is you create all resources, secrets, and permissions necessary before creating dependent resources.

Another helpful configuration is the [prometheusAutobind: true]({{% relref "04-administration-guide/01-installation/02-installation-via-helm/01-operator-parameters" %}}) definition.
This parameter automatically enables monitoring for our sharded cluster.
We can use this since we've installed the Prometheus operator on our Kubernetes environment.

Awesome, now you can sit back and relax while the SGShardedCluster is spinning up.


While the sharded cluster is being created, you may notice a blip in the distributed logs server, where a container is restarted.
This behavior is caused by a re-configuration which requires a container restart, and only temporarily pauses the log collection.
No logs are lost, since they are buffered on the source pods.

Have a look at [Connecting to the Sharded Cluster]({{% relref "04-administration-guide/14-sharded-cluster/13-connecting-to-the-sharded-cluster" %}}), to see how to connect to the created Postgres sharded cluster.
