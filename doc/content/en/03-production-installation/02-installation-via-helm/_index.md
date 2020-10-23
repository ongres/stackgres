---
title: Installation via Helm
weight: 2
url: install/helm/install
---

StackGres operator and clusters can be installed using [Helm](https://helm.sh/) version >= `3.1.1`.
As you may expect, a Production environment will require to install and setup additional components alongside your StackGres Operator and Cluster resources.

In this page, we are going through all the necessary steps to setup a Production like environment using Helm repositories and workflow.

## Setting up namespaces

Each component of your infrastructure needs to be isolated in different namespaces as an standard practice for reusability and security. For a minimal setup, three namespaces will be created:

```bash
kubectl create namespace stackgres
kubectl create namespace monitoring
kubectl create namespace my-cluster
```

`stackgres` will be the StackGres' **Operator** namespace, and `my-cluster` will be the namespace for the node resources that will contain the data and working backends.

The `monitoring` namespace was created for sitting the Prometheus Operator, which after all the further steps will end up with a running Grafana instance, embedded in the StackGres UI automatically by using the `grafana.autoEmbed=true` property, as shown later.

## Monitoring, Observability and Alerting with Prometheus and Grafana

Prometheus natively includes the following services:

- Prometheus Server: The core service
- Alert Manager: Handle events and send notifications to your preferred on-call platform
- Push Gateway: exposes metrics for ephemeral and batch jobs  

### Installing Prometheus Server

First, add the Prometheus Community repositories:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://kubernetes-charts.storage.googleapis.com/
helm repo update
```

Install the [Prometheus Server Operator](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus):

```bash
helm install --namespace monitoring prometheus-operator prometheus-community/prometheus
```

### [Optional] Re-routing services to different ports 

In a production setup, is very likely that you will be installing all the resources in a remote location, so you'll need to route the services through specific interfaces and ports.

> For sake of simplicity, we port-forward to all interfaces (0.0.0.0), although we
> strongly recommend to only expose through internal network interfaces when dealing on production.

Exposing Prometheus Server UI:

```bash
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app=prometheus,component=server" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9090
```

The Prometheus server serves through port 80 under `prometheus-operator-server.monitoring.svc.cluster.local` DNS name.

Exposing Alert Manager:

Over port 80, Prometheus alertmanager can be accessed through `prometheus-operator-alertmanager.monitoring.svc.cluster.local` DNS name.

```
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app=prometheus,component=alertmanager" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9093
```

Get the PushGateway URL by running these commands in the same shell:

```
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app=prometheus,component=pushgateway" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 9091
```

The Prometheus PushGateway can be accessed via port 9091 on the following DNS name from within your cluster: `prometheus-operator-pushgateway.monitoring.svc.cluster.local`

### Installing Prometheus Stack

[kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)

```
helm install --namespace monitoring prometheus prometheus-community/kube-prometheus-stack
```

```
kubectl --namespace monitoring get pods -l "release=prometheus"
```

### Installing Grafana and create basic dashboards

Get the source repository for the Grafana charts:

```sh
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

And install the chart:

```
helm install --namespace monitoring grafana grafana/grafana
```

Get the `admin` credential:

```
kubectl get secret --namespace monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

Expose your Grafana service at `grafana.monitoring.svc.cluster.local` (port 80) through your interfaces and port 3000 to login remotely (using above secret):

```bash
export POD_NAME=$(kubectl get pods --namespace monitoring -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=grafana" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace monitoring port-forward $POD_NAME --address 0.0.0.0 3000
```

> NOTE: take note of the Grafana's URL `grafana.monitoring.svc.cluster.local`, which will be used when configuring StackGres Operator.

The following script, will create a basic PostgreSQL dashboard against Grafana's API (you can change grafana_host to point to the remote location):

```sh
grafana_host=http://localhost:3000
grafana_admin_cred=$(kubectl get secret --namespace monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo)
grafana_credentials=admin:${grafana_admin_cred}
grafana_prometheus_datasource_name=Prometheus
curl_grafana_api() {
  curl -sk -H "Accept: application/json" -H "Content-Type: application/json" -u "$grafana_credentials" "$@"
}
get_admin_settings() {
  # Not executed in the script, but useful to keep this
  curl_grafana_api -X GET  ${grafana_host}/api/admin/settings | jq .
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
echo ${grafana_host}${grafana_dashboard_url}
```

The resulting URL will be the dashboard whether your PostgreSQL metrics will be show up.


### Monitoring Setup Verification

At this point, you should have ended with the following pods:

```
# kubectl get pods -n monitoring 
NAME                                                      READY   STATUS    RESTARTS   AGE
alertmanager-prometheus-kube-prometheus-alertmanager-0    2/2     Running   0          20m
grafana-7575c4b7b5-2cbvw                                  1/1     Running   0          14m
prometheus-grafana-5b458bf78c-tpqrl                       2/2     Running   0          20m
prometheus-kube-prometheus-operator-576f4bf45b-w5j9m      2/2     Running   0          20m
prometheus-kube-state-metrics-c65b87574-tsx24             1/1     Running   0          20m
prometheus-operator-alertmanager-655b8bc7bf-hc6fd         2/2     Running   0          79m
prometheus-operator-kube-state-metrics-69fcc8d48c-tmn8j   1/1     Running   0          79m
prometheus-operator-node-exporter-28qz9                   1/1     Running   0          79m
prometheus-operator-pushgateway-888f886ff-bxxtw           1/1     Running   0          79m
prometheus-operator-server-7686fc69bd-mlvsx               2/2     Running   0          79m
prometheus-prometheus-kube-prometheus-prometheus-0        3/3     Running   1          20m
prometheus-prometheus-node-exporter-jbsm2                 0/1     Pending   0          20m
```


## StackGres Operator installation

Now that we have configured a Backup storage place and a monitoring system already in place for proper observability, we can proceed
to the StackGres Operator itself!

```bash
grafana_admin_cred=$(kubectl get secret --namespace monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo)

helm install --namespace stackgres stackgres-operator \
        --set grafana.autoEmbed=true \
        --set-string grafana.webHost=grafana.monitoring \
        --set-string grafana.user=admin \
        --set-string grafana.password=${grafana_admin_cred} \
        --set-string adminui.service.type=LoadBalancer \
        {{< download-url >}}/helm-operator.tgz
```

> Notice that we use the short version of the Grafana's URL for the webHost.

In the previous example StackGres have included several options to the installation, including the needed options to enable
the monitoring. Follow the [Cluster Parameters](install/cluster/parameters) section for a described list.

> The `grafana.webHost` value may change if the installation is not Prometheus' default, as well as `grafana.user` and `grafana.password`.

## Exposing the UI

You can expose the UI using the bellow command:

```
POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")
kubectl port-forward ${POD_NAME} --address 0.0.0.0 8443:9443 --namespace stackgres
```

Connect to `https://<your-host>:8443/admin/` and get your UI credentials:

```bash
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\n" (.data.k8sUsername | base64decode) }}'
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "password = %s\n" (.data.clearPassword | base64decode) }}'
```

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