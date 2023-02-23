---
title: Backup Configuration
weight: 4
url: tutorial/complete-cluster/backup-configuration
description: Details about how to create custom backup configurations.
---

# Backups

StackGres supports automated backups, based on Postgres continuous archiving, that is base backups plus WAL archiving, as well as backup lifecycle management.
To achieve maximum durability, backups are stored on cloud/object storage.
S3, GCP, Azure Blob, and S3-compatible object storages are supported.


## AWS S3 Configuration


First, let's create the IAM policy that would allow the appropriate level of access to the S3 bucket:

```bash
export S3_BACKUP_BUCKET=YOUR_BUCKET_NAME

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
```

Let's then create an IAM user and attach the above policy:

```bash
export AWS_PROFILE=     # optional
export AWS_REGION= #YOUR_REGION
export S3_BACKUP_BUCKET_USER=stackgres-s3-user

aws iam create-user --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER

aws iam put-user-policy --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER \
	--policy-name ${S3_BACKUP_BUCKET_USER}-policy --policy-document $policy
```

Then, let's create an access key that will be used to access the bucket.
The following command will create and output the credentials, which we save in a file `credentials.json`:

```bash
aws --output json iam create-access-key --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER > credentials.json
```

Finally, create the bucket:

```bash
aws s3 mb s3://$S3_BACKUP_BUCKET --region $AWS_REGION
```

Now we create a secret with the contents of our credentials:

```bash
export CLUSTER_NAMESPACE=demo
export CREDENTIALS_FILE=credentials.json    # your credentials file
accessKeyId=$(jq -r '.AccessKey.AccessKeyId' "$CREDENTIALS_FILE")
secretAccessKey=$(jq -r '.AccessKey.SecretAccessKey' "$CREDENTIALS_FILE")

kubectl --namespace $CLUSTER_NAMESPACE create secret generic s3-backup-bucket-secret \
        --from-literal="accessKeyId=$accessKeyId" \
        --from-literal="secretAccessKey=$secretAccessKey"
```


## StackGres Backup Configuration

Having the credentials secret created, we just need to create the object storage configuration and set the backup configuration.
The object storage configuration it is governed by the [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRD.
This CRD allows to specify the object storage technology and required parameters, as well as a reference to the above secret.

Create the file `sgobjectstorage-backupconfig1.yaml`:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: demo
  name: backupconfig1
spec:
  type: 's3'
  s3:
    bucket: 'YOUR_BUCKET_NAME'
    awsCredentials:
      secretKeySelectors:
        accessKeyId: {name: 's3-backup-bucket-secret', key: 'accessKeyId'}
        secretAccessKey: {name: 's3-backup-bucket-secret', key: 'secretAccessKey'}
```

and deploy it to Kubernetes:

```bash
kubectl apply -f sgobjectstorage-backupconfig1.yaml
```

The backup configuration can be set under the section `.spec.configurations.backups` of the [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) CRD.
Among others, we can specify the retention window, when base backups are performed, and performance parameters of the backup process.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
spec:
  configurations:
    backups:
    - sgObjectStorage: backupconfig1
      cronSchedule: '*/5 * * * *'
      retention: 6
```

Note that for this tutorial and demo purposes, backups are created every 5 minutes.
Modify the `.spec.backups[0].cronSchedule` parameter above to adjust to your own needs.

The above configuration will be applied when creating the SGCluster resource.
