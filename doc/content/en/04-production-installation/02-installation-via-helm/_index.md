---
title: Installation via Helm
weight: 2
url: install/helm
description: Details about how to install the StackGres operator using Helm.
showToc: true
---

The StackGres operator and clusters can be installed using [Helm](https://helm.sh/) version >= `3.1.1`.
As you may expect, a production environment will require you to install and set up additional components alongside your StackGres operator and cluster resources.

In this page, we are going through all the necessary steps to set up a production-grade environment using Helm.

## Set Up StackGres Helm Repository

Add the StackGres Helm repository:

```bash
helm repo add stackgres-charts https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/
```

## StackGres Operator Installation

Install the operator: 

```bash
helm install --create-namespace --namespace stackgres stackgres-operator stackgres-charts/stackgres-operator
```

> You can specify the version adding `--version <version, e.g. 1.0.0>` to the Helm command. 

For more installation options have a look at the [Operator Parameters]({{% relref "04-production-installation/06-operator-parameters" %}}) section for more information.

If you want to integrate Prometheus and Grafana into StackGres, please read the next section. 

## StackGres Operator Installation With Monitoring

Install the operator with the Grafana-specific values:

```bash
helm install --namespace stackgres stackgres-operator \
 --set grafana.autoEmbed=true \
 --set-string grafana.webHost=prometheus-grafana.monitoring \
 --set-string grafana.secretNamespace=monitoring \
 --set-string grafana.secretName=prometheus-grafana \
 --set-string grafana.secretUserKey=admin-user \
 --set-string grafana.secretPasswordKey=admin-password \
 --set-string adminui.service.type=LoadBalancer \
 stackgres-charts/stackgres-operator
```

In this example, we included the required values to enable monitoring.
Follow the [Operator Parameters]({{% relref "04-production-installation/06-operator-parameters" %}}) section for more information.

## Creating and Customizing Your Postgres Clusters 

The following shows some examples of StackGres' versatile configuration options.
These steps are optional.

### Configuring an Instance Profile

You can create your cluster with different hardware specifications using an [SGInstanceProfile](https://stackgres.io/doc/latest/04-postgres-cluster-management/03-instance-profiles/) custom resource (CR) as follows:

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

### Configuring Postgres and PGBouncer

You can also change Postgres' configuration using an [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) CR, or the PGBouncer settings using [SGPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}}), the backup storage specification using [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}), and more.

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
We'll cover all more details about this in the [Customizing Pooling configuration section]({{% relref "05-administration-guide/05-customize-connection-pooling-configuration" %}}).

For improved performance and stability, it is recommended to set the `pool_mode` to `transaction`. An example pooling configuration looks like this:

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

The [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRs are used to configure how backups are being taken.

The following shows and example configuration using [Google Cloud Storage](https://cloud.google.com/storage/):

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
Have a look at the section [Backups]({{% relref "04-production-installation/01-pre-requisites/03-backups" %}}) for full examples using S3, GKE, Digital Ocean, and more.

### Configuring Distributed Logs

You can create an SGDistributedLogs CR to enable a [distributed log cluster]({{% relref "06-crd-reference/07-sgdistributedlogs" %}}):

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

### Configuring Initial Data

Last but not least, StackGres lets you include several `initialData` scripts, to perform cluster operations at startup.

In this example, we're creating a Postgres user, using a Kubernetes secret:

```bash
kubectl -n my-cluster create secret generic pgbench-user-password-secret \
  --from-literal=pgbench-create-user-sql="create user pgbench password 'admin123'"
```

The secret will be referenced in the `initialData` definition of the cluster, shown below.

Note that we could equally well define the SQL script in a config map, however, since the password represents a credential, we're using a secret.


### Creating the Cluster

All the required steps were performed to create our StackGres Cluster.

Create the SGCluster resource:

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
  distributedLogs:
    sgDistributedLogs: 'distributedlogs'
  initialData:
    scripts:
    - name: create-pgbench-user
      scriptFrom:
        secretKeyRef:
          name: pgbench-user-password-secret
          key: pgbench-create-user-sql
    - name: create-pgbench-database
      script: |
        create database pgbench owner pgbench;
  prometheusAutobind: true
EOF
```

Notice that each resource has been defined with its own `name`, and is referenced in the StackGres cluster definition.
The order of the CR creation is relevant to successfully create a cluster, that is you create all resources, secrets, and permissions necessary before creating dependent resources.

The `initialData` scripts are defined both by the secret created before and SQL instructions inline.

Another helpful configuration is the [prometheusAutobind: true]({{% relref "04-production-installation/06-operator-parameters" %}}) definition.
This parameter automatically enables monitoring for our cluster.
We can use this since we've installed the Prometheus operator on our Kubernetes environment.

Awesome, now you can sit back and relax while the SGCluster is spinning up.

Have a look at [Accessing the Cluster]({{% relref "03-tutorial/04-complete-cluster/07-accessing-cluster" %}}), to see how to connect to the created Postgres cluster.
