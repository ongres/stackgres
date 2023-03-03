---
title: DigitalOcean
weight: 1
url: install/prerequisites/backups/do
description: Details about how to set up and configure the backups on DigitalOcean Spaces.
showToc: true
---

## DigitalOcean Setup

This section shows to set up backups using DigitalOcean Spaces.
You will need to have [s3Cmd](https://s3tools.org/download) installed.
You need to configure `s3cmd` following the [instructions in the official docs](https://docs.digitalocean.com/products/spaces/resources/s3cmd/).

Go to the [API page](https://cloud.digitalocean.com/settings/api/tokens) and create a spaces key.

Create the bucket with the following characteristics (that you may change):

```bash
export DO_SPACES_BACKUP_BUCKET=stackgres-tutorial
s3cmd mb s3://${DO_SPACES_BACKUP_BUCKET}
```

## Kubernetes Setup

Create a Kubernetes secret with the following contents:

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

Having the credentials secret created, we now need to create the object storage configuration and to set the backup configuration.
The object storage configuration it is governed by the [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRD.
This CRD allows to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

Create the file `sgobjectstorage-backupconfig1.yaml`:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: demo
  name: backupconfig1
spec:
  type: s3Compatible
  s3Compatible:
    bucket: 'stackgres-tutorial' ## change me if needed
    endpoint: https://nyc3.digitaloceanspaces.com
    awsCredentials:
      secretKeySelectors:
        accessKeyId: {name: 'do-creds-secret', key: 'accessKeyId'}
        secretAccessKey: {name: 'do-creds-secret', key: 'secretAccessKey'}
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
