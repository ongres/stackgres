---
title: GKE
weight: 3
url: install/prerequisites/backups/gke
description: Details about how to setup and configure the backups on Google Cloud Storage.
showToc: true
---

## Google Cloud Bucket Setup

This section will illustrate how to setup backup using Google Cloud Storage. To do so, you will need to have the [gsutil](https://cloud.google.com/storage/docs/gsutil_install) installed to create the bucket on Google Cloud.

Create the bucket with following characteristics (that you may change):

* Project: my-project
* Zone: us-west1
* Bucket name: backup-demo-of-stackgres-io

```
gsutil mb \
    -p my-project \
    -b on \
    -l us-west1 \
    "gs://backup-demo-of-stackgres-io/"
```

## Kubernetes Setup

To proceed, a Kubernetes `Secret` with the folling shape needs to be created:

* Project: my-project
* Zone: us-west1
* Bucket name: backup-demo-of-stackgres-io
* K8s Service Account: stackgres-demo-k8s-sa-user
* K8s Bucket Secret Credentials: gcp-backup-bucket-secret
* Cluster namespace: stackgres

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

Having the credentials secret created, we just need to create the object storage configuration and set the backup configuration.
 The object storage configuration it is governed by the CRD
 [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}). This CRD allows to specify the object storage technology
 and parameters required and a reference to the above secret.

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

and deploy to Kubernetes:

```
kubectl apply -f sgobjectstorage-backupconfig1.yaml
```

The backup configuration can be set unser the section `.spec.configurations.backups` of the CRD
 [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}), among others, the retention window for the automated backups,
 when base backups are performed and performance parameters of the backup process.

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

Note that for this tutorial and demo purposes, backups are created every 5 minutes. Modify the
`.spec.backups[0].cronSchedule` parameter above to adjust to your own needs.

The above configuration will be applied when creating the SGCluster resource.

