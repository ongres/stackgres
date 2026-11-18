---
title: Creating a Cluster
weight: 2
url: /administration/cluster-creation
aliases: [ /administration/install/cluster-creation , /tutorial/simple-cluster , /tutorial/complete-cluster, /tutorial/complete-cluster/create-cluster ]
description: Details about how to create a production StackGres cluster.
showToc: true
---

This page will guide you through the creation of a production-ready StackGres cluster using your custom configuration.

## Understanding SGCluster

An SGCluster is a custom resource that represents a Postgres cluster in StackGres. It is important not to confuse this with the PostgreSQL term "database cluster", which refers to a single Postgres instance (a collection of databases managed by a single Postgres server process). In StackGres, an SGCluster represents a high-availability cluster composed of multiple Postgres instances.

When you create an SGCluster, the operator creates N Pods (where N is defined by `.spec.instances`). One of these Pods is elected by [Patroni](https://patroni.readthedocs.io/en/latest/) to be the primary, which receives all read/write queries. The remaining Pods become replicas that use PostgreSQL streaming replication (and/or WAL shipping if backups are configured) to stay synchronized with the primary.

StackGres creates Services to route traffic to the appropriate Pods:

- The main Service (named after the cluster) points to the primary Pod for read/write operations
- The `-replicas` Service distributes read-only queries across the replica Pods (useful for queries that are resilient to slightly out-of-date data)

## Minimal SGCluster Specification

The simplest SGCluster you can create requires only a few fields:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 1
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 10Gi
```

When you apply this minimal specification, the StackGres operator automatically adds default values for many fields, including default configurations for PostgreSQL, connection pooling, resource profiles, and other settings required for a functional cluster.

When you specify `latest` for the Postgres version, the operator materializes this to the actual latest available Postgres version. Each Pod is attached to a PersistentVolume of the specified size using the default StorageClass when one is not specified.

## Pod Architecture

Each Pod in an SGCluster contains several containers that work together to provide a fully functional Postgres instance:

**Init Container:**

- `setup-filesystem`: Creates the postgres user based on the UID provided by the Kubernetes cluster (important for OpenShift) and copies the filesystem inside the persistent volume for the extensions subsystem and major version upgrade mechanism

**Main Container:**

- `patroni`: Runs Patroni, which is responsible for high availability and controls the Postgres start/stop lifecycle and manages the primary/replica role assignment. The Postgres process runs in the same container as Patroni.

**Controller Sidecar:**

- `cluster-controller`: Initializes aspects of the patroni container, reconciles configurations, updates SGCluster status, and manages extension installation

**Optional Sidecars:**

- `envoy`: Edge proxy for connection routing (may be deprecated in future versions)
- `pgbouncer`: Connection pooling for improved connection scalability (port 5432)
- `prometheus-postgres-exporter`: Exports Postgres metrics for Prometheus monitoring
- `postgres-util`: Debugging and manual operations container (no active process, waits for user connection)
- `fluent-bit`: Sends logs to configured SGDistributedLogs instance when distributed logs are configured

## Cluster Profiles

StackGres provides three cluster profiles that control Pod scheduling and resource constraints. You can set the profile using `.spec.profile`:

**production (default):**

The production profile enforces strict operational requirements:
- Pod anti-affinity rules prevent Pods from running on the same Kubernetes node
- Resource requests are enforced for all containers
- Resource limits are enforced for the `patroni` container

**testing:**

The testing profile relaxes some restrictions for non-production environments:
- Pod anti-affinity restrictions are relaxed, allowing Pods on the same node
- Resource limits are still enforced but not resource requests

**development:**

The development profile removes all restrictions for local development:
- No Pod anti-affinity requirements
- No mandatory resource requests or limits

Example configuration:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  profile: development
  instances: 1
  postgres:
    version: latest
  pods:
    persistentVolume:
      size: 10Gi
```

## Customizing Your Postgres Clusters

The following shows examples of StackGres versatile configuration options.
In general, these steps are optional, but we do recommend to consider these features for production setups.

### Configuring an Instance Profile

You can create your cluster with different resources requirements using an [SGInstanceProfile]({{% relref "06-crd-reference/02-sginstanceprofile" %}}) custom resource (CR) as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  namespace: my-cluster
  name: size-small
spec:
  cpu: "2"
  memory: "4Gi"
```

By default the resources requests will be applied as the sum of the resources requests of all the containers of a cluster's Pod. Instead the resources limits will be applied for the `patroni` container that will run the Postgres process. For more advanced understanding see the [Instance Profile Configuration section]({{% relref "06-crd-reference/02-sginstanceprofile" %}}).

### Configuring Postgres and PGBouncer

You can also change Postgres configuration using an [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) CR, or the PGBouncer settings using [SGPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}}), the backup storage specification using [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}), and more.

The next code snippets will show you how to use these CRs.

Let's start with a custom PostgreSQL configuration, using `SGPostgresConfig`:

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  namespace: my-cluster
  name: pgconfig1
spec:
  postgresVersion: "12"
  postgresql.conf:
    shared_buffers: '512MB'
    random_page_cost: '1.5'
    password_encryption: 'scram-sha-256'
    log_checkpoints: 'on'
```

You can configure the variables supported by StackGres.

The connection pooler (currently PgBouncer) is an important part of a Postgres cluster, as it provides connection scaling capabilities.
We'll cover all the details about this in the [Customizing Pooling configuration section]({{% relref "04-administration-guide/04-configuration/03-connection-pooling" %}}).

For improved performance and stability, it is recommended to set the `pool_mode` to `transaction`.

> **IMPORTANT**: setting the `pool_mode` to `transaction` may require some changes in how the application
>  use the database. In particular the application will not be able to use session object. For more
>  information see the [PgBouncer official documentation](https://www.pgbouncer.org). In order to enable prepared statements in this
>  mode see [PgBouncer FAQ](https://www.pgbouncer.org/faq.html#how-to-use-prepared-statements-with-transaction-pooling).

The following command shows an example pooling configuration:

```yaml
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  namespace: my-cluster
  name: poolconfig1
spec:
  pgBouncer:
    pgbouncer.ini:
      pgbouncer:
        pool_mode: transaction
        max_client_conn: '1000'
        default_pool_size: '80'
```

### Configuring Backups

The [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRs are used to configure how backups are being taken.

The following command shows an example configuration using [Google Cloud Storage](https://cloud.google.com/storage/):

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: my-cluster
  name: backupconfig1
spec:
  type: "gcs"
  gcs:
    bucket: backup-my-cluster-of-stackgres-io
    gcpCredentials:
      secretKeySelectors:
        serviceAccountJSON: 
          name: gcp-backup-bucket-secret
          key: my-creds.json
```

Or alternatively, for [AWS S3](https://aws.amazon.com/s3/):

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: my-cluster
  name: backupconfig1
spec:
  type: 's3'
  s3:
    bucket: 'backup.my-cluster.stackgres.io'
    awsCredentials:
      secretKeySelectors:
        accessKeyId: {name: 'aws-creds-secret', key: 'accessKeyId'}
        secretAccessKey: {name: 'aws-creds-secret', key: 'secretAccessKey'}
```

You will need to perform additional steps in order to configure backups in your cloud environment.
Have a look at the section [Backups]({{% relref "04-administration-guide/05-backups" %}}) for full examples using AWS S3, Google Cloud Storage, Digital Ocean Spaces, and more.

### Configuring Distributed Logs

You can create an [SGDistributedLogs]({{% relref "06-crd-reference/07-sgdistributedlogs" %}}) CR to create a distributed log cluster that will receive the logs from the SGCluster configured to do so and to be able to view logs directly from the [Admin UI]({{% relref "04-administration-guide/13-admin-ui" %}}):

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  namespace: my-cluster
  name: distributedlogs
spec:
  persistentVolume:
    size: 50Gi
```

### Configuring Scripts

Last but not least, StackGres lets you include several `managedSql` scripts, to perform cluster operations at startup or on demand.

In this example, we're creating the `pgbench` user, using a Kubernetes secret:

```
kubectl -n my-cluster create secret generic pgbench-user-password-secret \
  --from-literal=pgbench-create-user-sql="CREATE USER pgbench WITH PASSWORD 'admin123'"
```

Then we reference the secret in a [SGScript]({{% relref "06-crd-reference/10-sgscript" %}}) that contains
 an inline script to create the `pgbench` database using the previously created user `pgbench` as the
 owner:

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
  - name: create-pgbench-database
    script: |
      CREATE DATABASE pgbench OWNER pgbench;
```

The SGScript will be referenced in the `managedSql` definition of the cluster, shown below.

Note that we could equally well define the SQL script in a ConfigMap, however, since the password
 represents a credential, we're using a Secret instead.

## Creating the Cluster

All the required steps were performed in order to allow creating our production ready SGCluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  postgres:
    version: '12.3'
  instances: 3
  sgInstanceProfile: 'size-small'
  pods:
    persistentVolume:
      size: '10Gi'
  configurations:
    sgPostgresConfig: 'pgconfig1'
    sgPoolingConfig: 'poolconfig1'
    backups:
    - sgObjectStorage: 'backupconfig1'
      cronSchedule: '*/5 * * * *'
      retention: 6
    observability:
      prometheusAutobind: true
  managedSql:
    scripts:
    - sgScript: cluster-scripts
  distributedLogs:
    sgDistributedLogs: 'distributedlogs'
```

Notice that each resource has been defined with its own name, and is referenced in the SGCluster definition.
The order of the CR creation is relevant to successfully create a cluster, that is you create all resources, secrets, and permissions necessary before creating dependent resources.

Another helpful configuration is the [`prometheusAutobind`]({{% relref "04-administration-guide/01-installation/02-installation-via-helm/01-operator-parameters" %}}) set to `true`.
This parameter automatically enables monitoring for our cluster by integrating with the Prometheus operator.
The StackGres operator will create the necessary PodMonitor to scrape the cluster's Pods.

Awesome, now you can sit back and relax while the SGCluster's Pods are spinning up.

Have a look at [Connecting to the Cluster]({{% relref "04-administration-guide/03-connecting-to-the-cluster" %}}), to see how to connect to the created Postgres cluster.
