---
title: AWS S3
weight: 2
url: administration/backups/eks
aliases: [ /install/prerequisites/backups/eks ]
description: Details about how to set up and configure backups on AWS S3.
showToc: true
---

## AWS S3 Setup

This section shows how to configure backups on StackGres using AWS S3.
You will need to have the [AWS CLI](https://aws.amazon.com/cli) installed, to create the required permissions and the bucket on AWS S3.

First, let's create the required permissions and the user with the following characteristics (that you may change):

* Zone: `us-west-2`
* Bucket name: `backup-demo-of-stackgres-io`
* IAM username: `stackgres-demo-k8s-sa-user`
* Secret Credentials: `eks-backup-bucket-secret`

```
export S3_BACKUP_BUCKET=backup-demo-of-stackgres-io

read -d '' policy <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [ "s3:ListBucket", "s3:GetBucketLocation" ],
      "Resource": [ "arn:aws:s3:::${S3_BACKUP_BUCKET}" ]
    },
    {
      "Effect": "Allow",
      "Action": [ "s3:PutObject", "s3:GetObject", "s3:DeleteObject" ],
      "Resource": [ "arn:aws:s3:::${S3_BACKUP_BUCKET}/*" ]
    }
  ]
}
EOF

export AWS_PROFILE=# optional
export AWS_REGION=us-west-2
export S3_BACKUP_BUCKET_USER=stackgres-demo-k8s-sa-user

aws iam create-user --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER

aws iam put-user-policy --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER \
	--policy-name ${S3_BACKUP_BUCKET_USER}-policy --policy-document $policy
```

Now, we need to create the access key that is used for the backup creation.
The following creates a key and saves it to a file `credentials.json`:

```
aws --output json iam create-access-key --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER > credentials.json
```

Finally, create the bucket (`mb` stands for 'make bucket'):

```
aws s3 mb s3://$S3_BACKUP_BUCKET --region $AWS_REGION
```

## Kubernetes Setup

Create a Kubernetes secret with the contents of our credentials:

```
export CLUSTER_NAMESPACE=demo
export CREDENTIALS_FILE=credentials.json # your credentials file

accessKeyId=$(jq -r '.AccessKey.AccessKeyId' "$CREDENTIALS_FILE")
secretAccessKey=$(jq -r '.AccessKey.SecretAccessKey' "$CREDENTIALS_FILE")

kubectl -n $CLUSTER_NAMESPACE create secret generic s3-backup-bucket-secret \
        --from-literal="accessKeyId=$accessKeyId" \
        --from-literal="secretAccessKey=$secretAccessKey"
```

## StackGres Object Storage

Having the credentials secret created, we now need to create the object storage configuration and to set the backup configuration.
The object storage configuration it is governed by the [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRD.
This CRD allows to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

Create a file `sgobjectstorage-backupconfig1.yaml` with the following contents:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: backup-config-stackgres-demo
spec:
  type: s3
  s3:
    bucket: backup-demo-of-stackgres-io
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: eks-backup-bucket-secret
          key: accessKeyId
        secretAccessKey:
          name: eks-backup-bucket-secret
          key: secretAccessKey
```

and deploy it to Kubernetes:

```
kubectl apply -f sgobjectstorage-backupconfig1.yaml
```

The backup configuration can be set under the section `.spec.configurations.backups` of the [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) CRD.
Here we define the retention window for the automated backups and when base backups are performed.
Additionally, you can define performance-related configuration of the backup process.

An example cluster configuration looks as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
# [...]
spec:
  configurations:
    backups:
    - sgObjectStorage: backupconfig1
      cronSchedule: '*/5 * * * *'
      retention: 6
```

For this tutorial, backups are created every 5 minutes.
Change the `.spec.backups[0].cronSchedule` parameter according to your own needs.

The above configuration will be applied when the SGCluster resource is created.