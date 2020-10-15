---
title: Installation via Helm
weight: 2
url: install/helm/install
---

StackGres operator and clusters can be installed using [Helm](https://helm.sh/) version >= `3.1.1`.
However, installing StackGres is not only the Operator and the Cluster, if you want you have other options
to modify, add or remove in your installation.

In this page, we are going through all the necessary steps to setup a Production like environment using Helm repositories and workflow.

Firstly, create `stackgres` namespace if doesn't exists already.
Also, you could include the namespace for Prometheus and enable it alongside the StackGres Operator installation.

```bash
kubectl create namespace stackgres
kubectl create namespace prometheus
kubectl create namespace my-cluster
```

Now that you have created the Prometheus namespace you can install the Prometheus operator, 
this will install also a Grafana instance and it will be
 embed with the StackGres UI automatically using the `grafana.autoEmbed=true` as shown later in the SG operator install.

Helm will take care of the necessary steps in Prometheus Operator deployment.

```bash
helm install --namespace prometheus prometheus-operator stable/prometheus-operator
```

Or by executing following script:

``` sh
grafana_host=http://localhost:3000
grafana_credentials=admin:prom-operator
grafana_prometheus_datasource_name=Prometheus
curl_grafana_api() {
  curl -sk -H "Accept: application/json" -H "Content-Type: application/json" -u "$grafana_credentials" "$@"
}
dashboard_id=9628
dashboard_json="$(cat << EOF
{
  "dashboard": $(curl_grafana_api "$grafana_host/api/gnet/dashboards/$dashboard_id" | jq .json),
  "overwrite": true,
  "inputs": [{
    "name": "DS_PROMETHEUS",
    "type": "datasource",
    "pluginId": "prometheus",
    "value": "$grafana_prometheus_datasource_name"
  }]
}
EOF
)"
grafana_dashboard_url="$(curl_grafana_api -X POST -d "$dashboard_json" "$grafana_host/api/dashboards/import" | jq -r .importedUrl)"
echo "$grafana_dashboard_url"

Then install the StackGres Operator

```bash
helm install --namespace stackgres stackgres-operator \
        --set grafana.autoEmbed=true \
        --set-string grafana.webHost=prometheus-operator-grafana.prometheus \
        --set-string grafana.user=admin \
        --set-string grafana.password=prom-operator \
        --set-string adminui.service.type=LoadBalancer \
        {{< download-url >}}/helm-operator.tgz
```

In the previous example StackGres have included several options to the installation, including the needed options to enable
the monitoring. Follow the [Cluster Parameters](install/cluster/parameters) section for a described list.

> The `grafana.webHost` value may change if the installation is not Prometheus' default, as well as `grafana.user` and `grafana.password`.

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

But not only the Instance Profile, you can instruct StackGres to changes PostgreSQL configuration using the CR [SGPostgresConfig](/reference/crd/tuning/postgres/) or the PGBouncer setting with [SGPoolingConfig](/reference/crd/tuning/pool/) and more, like the backup specification using [SGBackupConfig](/reference/backups/#configuration)

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

The last step and the longest for this demonstration is the backup CR

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

Alternatively StackGres could be instructed to use [Google Cloud Storage](https://cloud.google.com/storage/)

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

Finally create the SGDistributedLogs CR to enable a [distributed log cluster](/reference/distributedlogs/) 

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
The order of the CR creation have no relevance for the Cluster creation, the order in the previous steps 
is only a coincidence.

But that is not all, StackGres lets you include several `initialData` script to perform any operation in the cluster before start.
In the given example, we are creating an user to perform some queries using the k8s secret capabilities.

```bash
kubectl -n demo create secret generic pgbench-user-password-secret --from-literal=pgbench-create-user-sql="create user admin password 'admin123'"
```
As you can see, has been created a secret key and its value which will be used in the StackGres cluster creation.
All the necessary steps were performed to create your first StackGres Cluster, lets do it

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
Another important entry to highlight in the yaml is [prometheusAutobind: true](/install/cluster/parameters/#configuration-cluster-parameters). 
It is not enough to have the Prometheus operator installed to have monitoring, we need to enable this parameter to have monitoring as documentation indicates.

Awesome, now you can relax and wait for the SGCluster spinning up.

Meanwhile, you can monitor what StackGres is doing online running

```
some commando to monitor what happen
```

