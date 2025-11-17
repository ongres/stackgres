---
title: Creating a Cluster
weight: 2
url: /administration/cluster-creation
aliases: [ /administration/install/cluster-creation , /tutorial/simple-cluster , /tutorial/complete-cluster, /tutorial/complete-cluster/create-cluster ]
description: Details about how to create a production StackGres cluster.
showToc: true
---

This page will guide you though the creation of a production-ready StackGres cluster using your custom configuration.

## Customizing Your Postgres Clusters

The following shows examples of StackGres versatile configuration options.
In general, these steps are optional, but we do recommend to consider these features for production setups.

### Configuring an Instance Profile

You can create your cluster with different resources requirements using an [SGInstanceProfile]({{% relref "06-crd-reference/02-sginstanceprofile" %}}) custom resource (CR) as follows:

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  namespace: my-cluster
  name: size-small
spec:
  cpu: "2"
  memory: "4Gi"
EOF
```

By default the resources requests will be applied as the sum of the resources requests of all the containers of a cluster's Pod. Instead the resources limits will be applied for the `patroni` container that will run the Postgres process. For more advanced understanding see the [Instance Profile Configuration section]({{% relref "06-crd-reference/02-sginstanceprofile" %}}).

### Configuring Postgres and PGBouncer

You can also change Postgres configuration using an [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) CR, or the PGBouncer settings using [SGPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}}), the backup storage specification using [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}), and more.

The next code snippets will show you how to use these CRs.

Let's start with a custom PostgreSQL configuration, using `SGPostgresConfig`:

```yaml
cat << EOF | kubectl apply -f -
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
EOF
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
cat << EOF | kubectl apply -f -
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
EOF
```

### Configuring Backups

The [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRs are used to configure how backups are being taken.

The following command shows and example configuration using [Google Cloud Storage](https://cloud.google.com/storage/):

```yaml
cat << EOF | kubectl apply -f -
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
EOF
```

Or alternatively, for [AWS S3](https://aws.amazon.com/s3/):

```yaml
cat << EOF | kubectl apply -f -
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
EOF
```

You will need to perform additional steps in order to configure backups in your cloud environment.
Have a look at the section [Backups]({{% relref "04-administration-guide/05-backups" %}}) for full examples using AWS S3, Google Cloud Storage, Digital Ocean Spaces, and more.

### Configuring Distributed Logs

You can create an [SGDistributedLogs]({{% relref "06-crd-reference/07-sgdistributedlogs" %}}) CR to create a distributed log cluster that will receive the logs from the SGCluster configured to do so and to be able to view logs directly from the [Admin UI]({{% relref "04-administration-guide/13-admin-ui" %}}):

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  namespace: my-cluster
  name: distributedlogs
spec:
  persistentVolume:
    size: 50Gi
EOF
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
cat << EOF | kubectl apply -f -
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
EOF
```

The SGScript will be referenced in the `managedSql` definition of the cluster, shown below.

Note that we could equally well define the SQL script in a ConfigMap, however, since the password
 represents a credential, we're using a Secret instead.

## Creating the Cluster

All the required steps were performed in order to allow create our production ready SGCluster:

```yaml
cat << EOF | kubectl apply -f -
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
EOF
```

Notice that each resource has been defined with its own name, and is referenced in the SGCluster definition.
The order of the CR creation is relevant to successfully create a cluster, that is you create all resources, secrets, and permissions necessary before creating dependent resources.

Another helpful configuration is the [`prometheusAutobind`]({{% relref "04-administration-guide/01-installation/02-installation-via-helm/01-operator-parameters" %}}) set to `true`.
This parameter automatically enables monitoring for our cluster by integrating with the Prometheus operator.
The StackGres operator will breate the necessary PodMonitor to scrape the cluster's Pods.

Awesome, now you can sit back and relax while the SGCluster's Pods are spinning up.

Have a look at [Connecting to the Cluster]({{% relref "04-administration-guide/03-connecting-to-the-cluster" %}}), to see how to connect to the created Postgres cluster.
