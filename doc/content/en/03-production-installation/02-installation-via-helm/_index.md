---
title: Installation via Helm
weight: 2
url: install/helm/install
---

StackGres operator and clusters can be installed using [Helm](https://helm.sh/) version >= `3.1.1`.
As you may expect, a Production environment will require to install and setup additional components alongside your StackGres Operator and Cluster resources.

In this page, we are going through all the necessary steps to setup a Production like environment using Helm repositories and workflow.

## Setting up namespaces

Each component of your infrastructure needs to be isolated in different namespaces as a standard practice for reusability and security. For a minimal setup, three namespaces will be created:

```bash
kubectl create namespace stackgres
kubectl create namespace monitoring
kubectl create namespace my-cluster
```

`stackgres` will be the StackGres' **Operator** namespace, and `my-cluster` will be the namespace for the node resources that will contain the data and working backend.

The `monitoring` namespace was created to deploy the Prometheus Operator, which will result in a running Grafana instance.

### Installing Prometheus Server

First, add the Prometheus Community repositories:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://charts.helm.sh/stable
helm repo update
```

Install the [Prometheus Server Operator](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus):

```bash
helm install --namespace monitoring prometheus-operator prometheus-community/prometheus
```

> StackGres provides more and advanced options for monitoring installation, see [Create a more advanced cluster]({{% relref "04-administration-guide/07-create-a-more-advanced-cluster" %}}) in the [Administration Guide]({{% relref "04-administration-guide" %}}).

## StackGres Operator installation

Now that we have configured a Backup storage place, as indicated in the pre-requisites, 
and a monitoring system already in place for proper observability, 
we can proceed to the StackGres Operator itself!

```bash
helm install --namespace stackgres stackgres-operator \
        --set grafana.autoEmbed=true \
        --set-string grafana.webHost=prometheus-operator-grafana.monitoring \
        --set-string grafana.user=admin \
        --set-string grafana.password=prom-operator \
        --set-string adminui.service.type=LoadBalancer \
        {{< download-url >}}/helm/stackgres-operator.tgz
```

In the previous example StackGres have included several options to the installation, including the needed options to enable
the monitoring. Follow the [Cluster Parameters]({{% relref "03-production-installation/06-cluster-parameters" %}}) section for a described list.

> The `grafana.webHost` value may change if the installation is not Prometheus' default, as well as `grafana.user` and `grafana.password`.

## Creating and customizing your Postgres Clusters 

The next step is an optional one, but it will show you how to play with the StackGres versatility.

You can instruct StackGres to create your cluster with different hardware specification using the [Custom Resource](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/) (AKA CR) [SGInstanceProfile](https://stackgres.io/doc/latest/04-postgres-cluster-management/03-instance-profiles/) as follow


```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGInstanceProfile
metadata:
  namespace: my-cluster
  name: size-small
spec:
  cpu: "2"
  memory: "4Gi"
EOF
```

But not only the Instance Profile, you can instruct StackGres to changes PostgreSQL configuration using the CR [SGPostgresConfig]({{% relref "05-crd-reference/04-postgres-configuration" %}}) or the PGBouncer setting with [SGPoolingConfig]({{% relref "05-crd-reference/07-connection-pooling-configuration" %}}) and more, like the backup specification using [SGBackupConfig]({{% relref "05-crd-reference/03-backup-config" %}})

The next code snippets will show you how to play with these CRs.

Start with PostgreSQL configuration using th `SGPostgresConfig` as follow

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
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

You can easily declare the StackGres supported variables and setup your specific configuration.

Lets move forward and create our pooling CR

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGPoolingConfig
metadata:
  namespace: my-cluster
  name: poolconfig1
spec:
  pgBouncer:
    pgbouncer.ini:
      pool_mode: transaction
      max_client_conn: '200'
      default_pool_size: '200'
EOF
```

The longest step for this demonstration is the backup CR

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGBackupConfig
metadata:
  namespace: my-cluster
  name: backupconfig1
spec:
  baseBackups:
    cronSchedule: "*/5 * * * *"
    retention: 6
  storage:
    type: "gcs"
    gcs:
EOF
```

Alternatively, StackGres could be instructed to use [Google Cloud Storage](https://cloud.google.com/storage/)

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGBackupConfig
metadata:
  namespace: my-cluster
  name: backupconfig1
spec:
  baseBackups:
    cronSchedule: "*/5 * * * *"
    retention: 6
  storage:
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

Or [AWS S3](https://aws.amazon.com/s3/) if you want

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGBackupConfig
metadata:
  namespace: my-cluster
  name: backupconfig1
spec:
  baseBackups:
    cronSchedule: '*/5 * * * *'
    retention: 6
  storage:
    type: 's3'
    s3:
      bucket: 'backup.my-cluster.stackgres.io'
      awsCredentials:
        secretKeySelectors:
          accessKeyId: {name: 's3-backup-bucket-secret', key: 'accessKeyId'}
          secretAccessKey: {name: 's3-backup-bucket-secret', key: 'secretAccessKey'}
EOF
```

On AWS you will need to define some parameters if already you don't have defined it.
As bottom here is some variables and the needed permissions on S3

```bash
S3_BACKUP_BUCKET=backup.my-cluster.stackgres.io

S3_BACKUP_BUCKET_POLICY_NAME=s3_backup_bucket_iam_policy

S3_BACKUP_BUCKET_USER=s3_backup_bucket_iam_user

S3_BACKUP_CREDENTIALS_K8S_SECRET=s3-backup-bucket-secret

CLUSTER_NAMESPACE=my-cluster

# May be empty
export AWS_PROFILE=

# Include the region as you like
AWS_REGION=

aws=aws
[ ">"${AWS_PROFILE}"<" != "><" ] && aws="aws --profile ${AWS_PROFILE}"
```

Is necessary perform the policies generation, access keys and credentials.

```bash
#!/bin/bash

source ./variables

tempdir=/tmp/.$RANDOM-$RANDOM
mkdir $tempdir

cat << EOF > "$tempdir/policy.json"
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::${S3_BACKUP_BUCKET}/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": [
        "arn:aws:s3:::${S3_BACKUP_BUCKET}"
      ]
    }
  ]
}
EOF

{
	aws iam create-user --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER > /dev/null
	
	aws iam put-user-policy --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER \
		--policy-name $S3_BACKUP_BUCKET_POLICY_NAME \
		--policy-document "file://$tempdir/policy.json" > /dev/null
	
	aws iam create-access-key --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER \
		> $tempdir/credentials.json

	aws s3 mb s3://$S3_BACKUP_BUCKET --region $AWS_REGION
} &> /dev/null

accessKeyId=$(jq -r '.AccessKey.AccessKeyId' "$tempdir/credentials.json")
secretAccessKey=$(jq -r '.AccessKey.SecretAccessKey' "$tempdir/credentials.json")

echo accessKeyId=$accessKeyId
echo secretAccessKey=$secretAccessKey
echo kubectl --namespace $CLUSTER_NAMESPACE create secret generic $S3_BACKUP_CREDENTIALS_K8S_SECRET \
	--from-literal="accessKeyId=$accessKeyId" \
	--from-literal="secretAccessKey=$secretAccessKey"

rm $tempdir/policy.json
rm $tempdir/credentials.json
rmdir $tempdir
```

Now StackGres is able to use the keys accordingly.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: aws-creds-secret
type: Opaque
data:
  accessKey: ${accessKey}
  secretKey: ${secretKey}
EOF
```

Finally create the SGDistributedLogs CR to enable a [distributed log cluster]({{% relref "05-crd-reference/06-distributed-logs" %}}) 

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGDistributedLogs
metadata:
  namespace: my-cluster
  name: distributedlogs
spec:
  persistentVolume:
    size: 50Gi
EOF
```

Notice that each CR was assigned with its own `name:` which you would keep to define in the cluster creation
and aware StackGres about it.

The order of the CR creation have some relevance for the Cluster creation, i.e you need perform the access and secrets keys before create the SGDistributedLogs CR.

But that is not all, StackGres lets you include several `initialData` script to perform any operation in the cluster before start.

In the given example, we are creating an user to perform some queries using the k8s secret capabilities.

```bash
kubectl -n demo create secret generic pgbench-user-password-secret --from-literal=pgbench-create-user-sql="create user admin password 'admin123'"
```

As you can see, has been created a secret key and its value which will be used in the StackGres cluster creation.

All the necessary steps were performed to create your first StackGres Cluster, lets do it.

```yaml
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  postgresVersion: '12.3'
  instances: 3
  sgInstanceProfile: 'size-small'
  pods:
    persistentVolume:
      size: '10Gi'
  configurations:
    sgPostgresConfig: 'pgconfig1'
    sgPoolingConfig: 'poolconfig1'
    sgBackupConfig: 'backupconfig1'
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
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
EOF
```

Look up to the yaml into the here doc above, every CR previously being included in the right place in the SGCluster CR creation.

And there is in place the script created through the secret, but StackGres includes an extra example for you, the second script
show you how to run a SQL instruction directly into the yaml. 

Another important entry to highlight in the yaml is [prometheusAutobind: true]({{% relref "03-production-installation/06-cluster-parameters" %}}). 
It is not enough to have the Prometheus operator installed to have monitoring, we need to enable this parameter to have monitoring as documentation indicates.

Awesome, now you can relax and wait for the SGCluster spinning up.

## Accessing the cluster

Once the cluster is up and running, we need to expose the main entrypoint port for being accessed remotely:

```
kubectl port-forward test-0 --address 0.0.0.0 7777:5432
```

In the namespace of the cluster, you should be able to see a set of secrets, we'll get the main superuser password:

```
kubectl get secrets  test -o jsonpath='{.data.superuser-password}' | base64 -d
```

You can connect within the following command:

```
psql -h <the ip of the cluster> -p 7777 -U postgres
```
