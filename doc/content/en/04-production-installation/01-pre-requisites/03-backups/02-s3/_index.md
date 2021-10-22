---
title: S3
weight: 2
url: install/prerequisites/backups/eks
description: Details about how to setup and configure the backups on AWS S3.
showToc: true
---

## AWS S3 Setup

This section will illustrate how to configure backups on StackGres using AWS S3.
To do so, you will need to have the [AWS CLI](https://aws.amazon.com/cli) installed to create the right permissions and the bucket on AWS S3.


Create the right permissions and the user with following characteristics (that you may change):

* Zone: us-west-2
* Bucket name: backup-demo-of-stackgres-io
* IAM username: stackgres-demo-k8s-sa-user
* Secret Credentials: eks-backup-bucket-secret

```bash
aws iam create-user --region us-west-2 --user-name stackgres-demo-k8s-sa-user
```
```bash
aws iam put-user-policy --region us-west-2 --user-name stackgres-demo-k8s-sa-user --policy-name stackgres-demo-k8s-user-policy --policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["s3:ListBucket","s3:GetBucketLocation"],"Resource":["arn:aws:s3:::backup-demo-of-stackgres-io"]},{"Effect":"Allow","Action":["s3:PutObject","s3:GetObject","s3:DeleteObject"],"Resource":["arn:aws:s3:::backup-demo-of-stackgres-io/*"]}]}'
```

Now we need to create the access key to be used on backup creation. As output a file `access_key.json` will be generated:
```bash
aws --output json iam create-access-key --region us-west-2 --user-name stackgres-demo-k8s-sa-user | tee access_keys.json
```

Finally to create the bucket:
```bash
aws s3 mb s3://backup-demo-of-stackgres-io --region us-west-2
```

## Kubernetes Setup

To proceed, a Kubernetes `Secret` with the folling shape needs to be created:

```bash
kubectl create secret generic eks-backup-bucket-secret --from-literal="accessKeyId=<YOUR_ACCESS_KEY_HERE>"   --from-literal="secretAccessKey=<YOUR_SECRET_KEY_HERE>"

secret/sg-demo-jira-arm-secret created
```

Having the credentials secret created, we just need to create now a backup configuration. It is governed by the CRD
[SGBackupConfig]({{% relref "06-crd-reference/05-sgbackupconfig" %}}). This CRD allows to specify, among others, the
retention window for the automated backups, when base backups are performed, performance parameters of the backup
process, the object storage technology and parameters required and a reference to the above secret.

Create the file `sgbackupconfig-backupconfig1.yaml`:

```yaml
apiVersion: stackgres.io/v1
kind: SGBackupConfig
metadata:
  name: backup-config-stackgres-demo
spec:
  baseBackups:
    cronSchedule: "*/5 * * * *"
    retention: 3
  storage:
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

and deploy to Kubernetes:

```bash
kubectl apply -f sgbackupconfig-backupconfig1.yaml
```

Note that for this tutorial and demo purposes, backups are created every 5 minutes. Modify the
`.spec.baseBackups.cronSchedule` parameter above to adjust to your own needs.
