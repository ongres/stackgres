---
title: DigitalOcean
weight: 1
url: install/prerequisites/backups/do
description: Details about how to setup and configure the backups on DigitalOcean Spaces.
showToc: true
---

## DigitalOcean Setup

This section will illustrate how to setup backup using DigitalOcean Spaces. you will also need to have installed the [s3Cmd](https://s3tools.org/download) installed. Once installed, configure it following the [instructions in the oficial docs](https://docs.digitalocean.com/products/spaces/resources/s3cmd/).

Go the [API page](https://cloud.digitalocean.com/settings/api/tokens) and create a spaces key.

Create the bucket with following characteristics (that you may change):

```bash
export DO_SPACES_BACKUP_BUCKET=stackgres-tutorial
s3cmd mb s3://${DO_SPACES_BACKUP_BUCKET}
```

## Kubernetes Setup

To proceed, a Kubernetes `Secret` with the folling shape needs to be created:

```bash
ACCESS_KEY="**********" ## fix me
SECRET_KEY="**********" ## fix me
CLUSTER_NAMESPACE=demo
kubectl create secret generic \
  --namespace ${CLUSTER_NAMESPACE} \
  do-creds-secret \
  --from-literal=accessKeyId=${ACCESS_KEY} \
  --from-literal=secretAccessKey=${SECRET_KEY}
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
    type: s3Compatible
    s3Compatible:
      bucket: 'stackgres-tutorial' ## change me if needed
      endpoint: https://nyc3.digitaloceanspaces.com
      awsCredentials:
        secretKeySelectors:
          accessKeyId: {name: 'do-creds-secret', key: 'accessKeyId'}
          secretAccessKey: {name: 'do-creds-secret', key: 'secretAccessKey'}
```

and deploy to Kubernetes:

```bash
kubectl apply -f sgbackupconfig-backupconfig1.yaml
```

Note that for this tutorial and demo purposes, backups are created every 5 minutes. Modify the
`.spec.baseBackups.cronSchedule` parameter above to adjust to your own needs.
