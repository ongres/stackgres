---
title: GKE
weight: 3
url: install/prerequisites/backups/gke
description: Details about how to set up and configure the backups on Google Cloud Storage.
showToc: true
---

## Google Cloud Bucket Setup

This section shows how to set up backups using Google Cloud Storage.
You will need to have [gsutil](https://cloud.google.com/storage/docs/gsutil_install) installed, to create the bucket on Google Cloud.

Create the bucket with following characteristics (that you may change):

* Project: `my-project`
* Zone: `us-west1`
* Bucket name: `backup-demo-of-stackgres-io`

```
gsutil mb \
 -p my-project \
 -b on \
 -l us-west1 \
 "gs://backup-demo-of-stackgres-io/"
```

## Kubernetes Setup

Create a Kubernetes namespace, a serviceaccount, the required access, and a Kubernetes secret containing the credentials.
We use the following information:

* K8s namespace: `stackgres`
* K8s service account: `stackgres-demo-k8s-sa-user`
* K8s bucket secret credentials: `gcp-backup-bucket-secret`

```
kubectl create namespace stackgres

kubectl create serviceaccount --namespace stackgres stackgres-demo-k8s-sa-user

gcloud iam service-accounts create stackgres-demo-k8s-sa-user --project my-project

## grant access to the bucket
gsutil iam ch \
 serviceAccount:stackgres-demo-k8s-sa-user@my-project.iam.gserviceaccount.com:roles/storage.objectAdmin \
 "gs://backup-demo-of-stackgres-io/"

gcloud iam service-accounts keys \
 create my-creds.json --iam-account stackgres-demo-k8s-sa-user@my-project.iam.gserviceaccount.com

## create secret
kubectl --namespace stackgres create secret \
 generic gcp-backup-bucket-secret \
 --from-file="my-creds.json"

rm -rfv my-creds.json
```

Having the resources created, we now need to create the object storage configuration and to set the backup configuration.
The object storage configuration it is governed by the [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRD.
This CRD allows to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

Create a file `sgobjectstorage-backupconfig1.yaml` with the following contents:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: demo
  name: backupconfig-gcp
spec:
  type: "gcs"
  gcs:
    bucket: backup-demo-of-stackgres-io
    gcpCredentials:
      secretKeySelectors:
        serviceAccountJSON: 
          name: gcp-backup-bucket-secret
          key: my-creds.json
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
