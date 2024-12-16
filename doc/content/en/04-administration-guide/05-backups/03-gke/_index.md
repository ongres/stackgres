---
title: GKE
weight: 3
url: /administration/backups/gke
aliases: [ /install/prerequisites/backups/gke ]
description: Details about how to set up and configure the backups on Google Cloud Storage.
showToc: true
---

## Google Cloud Bucket Setup

This section shows how to set up backups using Google Cloud Storage.
You will need to have [gsutil](https://cloud.google.com/storage/docs/gsutil_install) installed, to create the bucket on Google Cloud.

Create the bucket with following characteristics (that you may change):

* Project: `stackgres-project`
* Zone: `us-west1`
* Bucket name: `my-stackgres-bucket`
* Service account: `stackgres-backup-user` 

```
gsutil mb \
 -p stackgres-project \
 -b on \
 -l us-west1 \
 "gs://my-stackgres-bucket/"

gcloud iam service-accounts create stackgres-backup-user --project stackgres-project

## grant access to the bucket
gsutil iam ch \
 serviceAccount:stackgres-backup-user@stackgres-project.iam.gserviceaccount.com:roles/storage.objectAdmin \
 "gs://my-stackgres-bucket/"
```

## Secret and SGObjectStorage

Create a Kubernetes namespace, a serviceaccount, the required access, and a Kubernetes secret containing the credentials.

```
gcloud iam service-accounts keys \
 create my-creds.json --iam-account stackgres-backup-user@stackgres-project.iam.gserviceaccount.com

## create secret
kubectl --namespace stackgres create secret \
 generic gcs-backup-secret \
 --from-file="my-creds.json"

rm -rfv my-creds.json
```

Having the resources created, we now need to create the object storage configuration and to set the backup configuration.
The object storage configuration it is governed by the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD.
This CRD allows to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: objectstorage
spec:
  type: "gcs"
  gcs:
    bucket: my-stackgres-bucket
    gcpCredentials:
      secretKeySelectors:
        serviceAccountJSON: 
          name: gcs-backup-secret
          key: my-creds.json
```
