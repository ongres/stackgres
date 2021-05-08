---
title: AWS S3
weight: 1
url: tutorial/complete-cluster/backup-configuration/aws-s3
description: Details about how to create custom backup configuration using the AWS S3 service.
---

To proceed, a Kubernetes `Secret` with the folling shape needs to be created:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: aws-creds-secret
type: Opaque
data:
  accessKeyId: ${accessKey}
  secretAccessKey: ${secretKey}
```

If you have created the S3 bucket following the instructions in the
[AWS S3 Prerequisites]({{% relref "03-tutorial/01-pre-requisites/04-object-storage/01-aws-s3" %}}), you should already
have the generated bucket credentials stored on a local file in JSON format. In this case, we can even script the
creation of the above secret:

```bash
export CLUSTER_NAMESPACE=demo
export CREDENTIALS_FILE=    # your credentials file
accessKeyId=$(jq -r '.AccessKey.AccessKeyId' "$CREDENTIALS_FILE")
secretAccessKey=$(jq -r '.AccessKey.SecretAccessKey' "$CREDENTIALS_FILE")

kubectl --namespace $CLUSTER_NAMESPACE create secret generic s3-backup-bucket-secret \
        --from-literal="accessKeyId=$accessKeyId" \
        --from-literal="secretAccessKey=$secretAccessKey"
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
  namespace: demo
  name: backupconfig1
spec:
  baseBackups:
    cronSchedule: '*/5 * * * *'
    retention: 6
  storage:
    type: 's3'
    s3:
      bucket: 'YOUR_BUCKET_NAME'
      awsCredentials:
        secretKeySelectors:
          accessKeyId: {name: 's3-backup-bucket-secret', key: 'accessKeyId'}
          secretAccessKey: {name: 's3-backup-bucket-secret', key: 'secretAccessKey'}
```

and deploy to Kubernetes:

```bash
kubectl apply -f sgbackupconfig-backupconfig1.yaml
```

Note that for this tutorial and demo purposes, backups are created every 5 minutes. Modify the
`.spec.baseBackups.cronSchedule` parameter above to adjust to your own needs.

