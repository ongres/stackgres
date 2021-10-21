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

```bash
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

```bash
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
  name: backupconfig-gcp
spec:
  baseBackups:
    cronSchedule: "*/5 * * * *"
    retention: 6
  storage:
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

```bash
kubectl apply -f sgbackupconfig-backupconfig1.yaml
```

Note that for this tutorial and demo purposes, backups are created every 5 minutes. Modify the
`.spec.baseBackups.cronSchedule` parameter above to adjust to your own needs.
