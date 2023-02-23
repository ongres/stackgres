---
title: S3
weight: 2
url: install/prerequisites/backups/eks
description: Details about how to set up and configure backups on AWS S3.
showToc: true
---

## AWS S3 Setup

This section shows how to configure backups on StackGres using AWS S3.
You will need to have the [AWS CLI](https://aws.amazon.com/cli) installed, to create the required permissions and the bucket on AWS S3.

Create the required permissions and the user with the following characteristics (that you may change):

* Zone: `us-west-2`
* Bucket name: `backup-demo-of-stackgres-io`
* IAM username: `stackgres-demo-k8s-sa-user`
* Secret Credentials: `eks-backup-bucket-secret`

```bash
aws iam create-user --region us-west-2 --user-name stackgres-demo-k8s-sa-user
```
```bash
aws iam put-user-policy --region us-west-2 --user-name stackgres-demo-k8s-sa-user --policy-name stackgres-demo-k8s-user-policy --policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["s3:ListBucket","s3:GetBucketLocation"],"Resource":["arn:aws:s3:::backup-demo-of-stackgres-io"]},{"Effect":"Allow","Action":["s3:PutObject","s3:GetObject","s3:DeleteObject"],"Resource":["arn:aws:s3:::backup-demo-of-stackgres-io/*"]}]}'
```

Now we need to create the access key that is used for the backup creation.
The following creates a key and saves it to a file `access_key.json`:

```bash
aws --output json iam create-access-key --region us-west-2 --user-name stackgres-demo-k8s-sa-user | tee access_keys.json
```

Finally, create the bucket:

```bash
aws s3 mb s3://backup-demo-of-stackgres-io --region us-west-2
```

## Kubernetes Setup

Create a Kubernetes secret with the following contents:

```bash
kubectl create secret generic eks-backup-bucket-secret \
 --from-literal="accessKeyId=<YOUR_ACCESS_KEY_HERE>" \
 --from-literal="secretAccessKey=<YOUR_SECRET_KEY_HERE>"
```

Having the credentials secret created, we now need to create the object storage configuration and to set the backup configuration.
The object storage configuration it is governed by the [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRD.
This CRD allows to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

Create the file `sgobjectstorage-backupconfig1.yaml`:

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

```bash
kubectl apply -f sgobjectstorage-backupconfig1.yaml
```

The backup configuration can be set under the section `.spec.configurations.backups` of the [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) CRD.
Here we define the retention window for the automated backups and when base backups are performed.
Additionally, you can define performance-related configuration of the backup process.

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

For this tutorial, backups are created every 5 minutes.
Change the `.spec.backups[0].cronSchedule` parameter according to your own needs.

The above configuration will be applied when the SGCluster resource is created.
