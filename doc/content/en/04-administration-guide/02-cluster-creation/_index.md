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

The following shows examples of StackGres' versatile configuration options.
In general, these steps are optional, but we do recommend to consider these features for production setups.

The next step is an optional one, but it will show you how to play with the StackGres versatility.

You can instruct StackGres to create your cluster with different hardware specification using the [Custom Resource](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/) (AKA CR) [SGInstanceProfile](https://stackgres.io/doc/latest/04-postgres-cluster-management/03-instance-profiles/) as follow


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

But not only the Instance Profile, you can instruct StackGres to changes PostgreSQL configuration using the CR [SGPostgresConfig]({{% relref "06-crd-reference/03-sgpostgresconfig" %}})
 or the PGBouncer setting with [SGPoolingConfig]({{% relref "06-crd-reference/04-sgpoolingconfig" %}})
 and more, like the backup storage specification using [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}})

The next code snippets will show you how to play with these CRs.

Start with PostgreSQL configuration using th `SGPostgresConfig` as follow

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

You can easily declare the StackGres supported variables and setup your specific configuration.

The connection pooler (currently PgBouncer) is an important part of a Postgres cluster, as it provides connection scaling capabilities.
We'll cover all more details about this in the [Customizing Pooling configuration section]({{% relref "04-administration-guide/03-configuration/03-connection-pooling" %}}).

For better performance and stability, it is recommended to use `pool_mode` in `transaction`. An example configuration would be like this:

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

The longest step for this demonstration is the backup storage CR.
 For example, [Google Cloud Storage](https://cloud.google.com/storage/) could be used:

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

Or [AWS S3](https://aws.amazon.com/s3/) if you want to:

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
Have a look at the section [Backups]({{% relref "04-administration-guide/04-backups" %}}) for full examples using S3, GKE, Digital Ocean, and more.

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

Finally create the SGDistributedLogs CR to enable a [distributed log cluster]({{% relref "06-crd-reference/07-sgdistributedlogs" %}}):

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

Notice that each CR was assigned with its own `name:` which you would keep to define in the cluster creation
and aware StackGres about it.

The order of the CR creation have some relevance for the Cluster creation, i.e you need perform the access and secrets keys before create the SGDistributedLogs CR.

But that is not all, StackGres lets you include several `initialData` script to perform any operation in the cluster before start.

In the given example, we are creating an user to perform some queries using the k8s secret capabilities.

```
kubectl -n my-cluster create secret generic pgbench-user-password-secret \
  --from-literal=pgbench-create-user-sql="create user pgbench password 'admin123'"
```

As you can see, has been created a secret key and its value which will be used in the StackGres cluster creation.

Note that we could equally well define the SQL script in a config map, however, since the password represents a credential, we're using a secret.


## Creating the Cluster

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

Look up to the yaml into the here doc above, every CR previously being included in the right place in the SGCluster CR creation.

And there is in place the script created through the secret, but StackGres includes an extra example for you, the second script
show you how to run a SQL instruction directly into the yaml. 

Another helpful configuration is the [prometheusAutobind: true]({{% relref "04-administration-guide/01-stackgres-installation/02-installation-via-helm/01-operator-parameters" %}}) definition.
This parameter automatically enables monitoring for our cluster.
We can use this since we've installed the Prometheus operator on our Kubernetes environment.

Awesome, now you can relax and wait for the SGCluster spinning up.

While the cluster is being created, you may notice a blip in the distributed logs server, where a container is restarted.
This behavior is caused by a re-configuration which requires a container restart, and only temporarily pauses the log collection.
No logs are lost, since they are buffered on the source pods.

Have a look at [Connecting to the Cluster]({{% relref "04-administration-guide/02-connecting-to-the-cluster" %}}), to see how to connect to the created Postgres cluster.
