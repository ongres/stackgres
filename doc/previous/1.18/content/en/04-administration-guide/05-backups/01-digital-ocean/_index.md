---
title: DigitalOcean
weight: 1
url: /administration/backups/do
aliases: [ /install/prerequisites/backups/do ]
description: Details about how to set up and configure the backups on DigitalOcean Spaces.
showToc: true
---

## DigitalOcean Setup

This section shows how to set up backups using DigitalOcean Spaces.
You will need to have [s3Cmd](https://s3tools.org/download) installed.
You need to configure `s3cmd` following the [instructions in the official docs](https://docs.digitalocean.com/products/spaces/resources/s3cmd/).

Go to the [API page](https://cloud.digitalocean.com/settings/api/tokens) and create a spaces key.

Let's create the bucket with the following characteristics (that you may change):

* Bucket name: `my-stackgres-bucket`

```
s3cmd mb s3://my-stackgres-bucket
```

## Secret and SGObjectStorage

Create a Kubernetes secret with the following contents:

```
ACCESS_KEY="**********" ## fix me
SECRET_KEY="**********" ## fix me
kubectl create secret generic \
  do-backup-secret \
  --from-literal=accessKeyId=${ACCESS_KEY} \
  --from-literal=secretAccessKey=${SECRET_KEY}
```

Having the credentials secret created, we now need to create the object storage configuration and set the backup configuration.
The object storage configuration is governed by the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD.
This CRD allows you to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: objectstorage
spec:
  type: s3Compatible
  s3Compatible:
    bucket: my-stackgres-bucket
    endpoint: https://nyc3.digitaloceanspaces.com
    awsCredentials:
      secretKeySelectors:
        accessKeyId: {name: 'do-backup-secret', key: 'accessKeyId'}
        secretAccessKey: {name: 'do-backup-secret', key: 'secretAccessKey'}
```
