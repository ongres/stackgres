---
title: DigitalOcean
weight: 1
url: tutorial/complete-cluster/backup-configuration/do-spaces
description: Details about how to create custom backup configuration using the DigitalOcean's Spaces service.
---

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

