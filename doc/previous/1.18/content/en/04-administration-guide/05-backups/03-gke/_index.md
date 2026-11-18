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
The object storage configuration is governed by the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD.
This CRD allows you to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

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

## Using GKE Workload Identity

For enhanced security on GKE, you can use [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity) instead of service account keys. This eliminates the need to manage and store service account JSON keys.

### Prerequisites

- GKE cluster with Workload Identity enabled
- gcloud CLI installed and configured

### Step 1: Enable Workload Identity on Your Cluster

If not already enabled:

```bash
gcloud container clusters update my-gke-cluster \
  --workload-pool=stackgres-project.svc.id.goog \
  --zone=us-west1-a
```

For new clusters:

```bash
gcloud container clusters create my-gke-cluster \
  --workload-pool=stackgres-project.svc.id.goog \
  --zone=us-west1-a
```

### Step 2: Create GCP Service Account

```bash
gcloud iam service-accounts create stackgres-backup-sa \
  --project=stackgres-project \
  --display-name="StackGres Backup Service Account"
```

### Step 3: Grant Bucket Access

```bash
gsutil iam ch \
  serviceAccount:stackgres-backup-sa@stackgres-project.iam.gserviceaccount.com:roles/storage.objectAdmin \
  "gs://my-stackgres-bucket/"
```

### Step 4: Create Kubernetes Service Account

```bash
kubectl create serviceaccount stackgres-backup-ksa \
  --namespace default
```

### Step 5: Bind Kubernetes SA to GCP SA

Allow the Kubernetes service account to impersonate the GCP service account:

```bash
gcloud iam service-accounts add-iam-policy-binding \
  stackgres-backup-sa@stackgres-project.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:stackgres-project.svc.id.goog[default/stackgres-backup-ksa]"
```

### Step 6: Annotate Kubernetes Service Account

```bash
kubectl annotate serviceaccount stackgres-backup-ksa \
  --namespace default \
  iam.gke.io/gcp-service-account=stackgres-backup-sa@stackgres-project.iam.gserviceaccount.com
```

### Step 7: Configure SGObjectStorage with Workload Identity

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: gcs-workload-identity-storage
spec:
  type: gcs
  gcs:
    bucket: my-stackgres-bucket
    gcpCredentials:
      fetchCredentialsFromMetadataService: true
```

### Step 8: Configure SGCluster

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  # ... other configuration ...
  configurations:
    backups:
      - sgObjectStorage: gcs-workload-identity-storage
        cronSchedule: '0 5 * * *'
        retention: 7
```

### Benefits of Workload Identity

- **No key management**: No service account JSON keys to create, store, or rotate
- **Enhanced security**: Keys never leave Google's infrastructure
- **Fine-grained access**: Each cluster can use different GCP identities
- **Audit logging**: Cloud Audit Logs track all access

## Choosing Between Methods

| Method | Security | Complexity | Use Case |
|--------|----------|------------|----------|
| Service Account JSON | Good | Simple | Non-GKE clusters, quick setup |
| Workload Identity | Best | Moderate | Production GKE deployments |

For production GKE deployments, Workload Identity is the recommended approach as it eliminates the need to manage service account keys.
