---
title: Backup storage on Google Cloud Storage
weight: 7
url: /cookbook/managing-backups/object-storage-gcs
description: Define an SGObjectStorage backed by Google Cloud Storage.
showToc: true
---

## What it does

Creates an [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) that
points to a Google Cloud Storage bucket. Once created, any SGCluster in the same namespace
can reference it to store base backups and WAL segments. The operator reconciles credential
changes without restarting Pods.

## When to use it

- You are enabling automated or on-demand backups for the first time and GCS is your
  target storage.
- Your cluster runs on GKE and you want to authenticate via Workload Identity instead of a
  long-lived JSON key.
- You want to share one storage definition across multiple clusters in the same namespace.

## How to do it

### With a service account JSON key

Create a Kubernetes Secret that holds the GCP service account key (downloaded as JSON from
the GCP Console), then apply the SGObjectStorage that references it.

```bash
kubectl create secret generic gcs-credentials \
  --namespace my-cluster \
  --from-file=service-account-json=/path/to/sa-key.json
```

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: my-cluster
  name: gcs-storage
spec:
  type: gcs
  gcs:
    bucket: my-cluster-backups        # GCS bucket name; must already exist.
    gcpCredentials:
      secretKeySelectors:
        serviceAccountJSON:
          name: gcs-credentials       # Secret created above.
          key: service-account-json
```

```bash
kubectl apply -f sgobjectstorage.yaml
```

### With Workload Identity (no JSON key)

When your GKE nodes use [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity),
the backup Pods can fetch credentials from the GCE metadata service. Set
`fetchCredentialsFromMetadataService: true` and omit `secretKeySelectors`:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: my-cluster
  name: gcs-storage
spec:
  type: gcs
  gcs:
    bucket: my-cluster-backups
    gcpCredentials:
      fetchCredentialsFromMetadataService: true
```

Once the SGObjectStorage exists, reference it from an SGCluster via the automated
backups recipe by setting
`spec.configurations.backups[].sgObjectStorage: gcs-storage`.

## How it works

SGObjectStorage is a standalone custom resource; it does not create any Pods or contact
GCS at admission time. When an SGCluster that references it triggers a backup (on schedule
or on demand), the operator mounts the credential Secret into the backup Job and passes
the bucket coordinates to the WAL-G archiver. When `fetchCredentialsFromMetadataService`
is `true` no Secret is mounted and WAL-G retrieves an OAuth2 token from the instance
metadata endpoint instead. Changes to the SGObjectStorage are picked up on the next
reconciliation loop without restarting cluster Pods.

## What to expect

- `kubectl get sgobjectstorage -n my-cluster` lists the resource with no external status
  conditions — the operator only validates the credentials when a backup runs.
- The first backup job logs from `wal-g` will surface any GCS permission errors immediately.
- Rotating the Secret contents or changing `fetchCredentialsFromMetadataService` takes
  effect on the next scheduled or on-demand backup without Pod restarts.

## Pitfalls

- **Bucket must exist and be pre-created.** The operator does not create the GCS bucket.
  Ensure it exists in the correct GCP project before running a backup.
- **Required GCS permissions.** The service account (or the Workload Identity principal)
  needs at minimum `storage.objects.create`, `storage.objects.get`,
  `storage.objects.delete`, and `storage.buckets.get` on the target bucket. The
  `Storage Object Admin` role is the most common grant.
- **`fetchCredentialsFromMetadataService` requires Workload Identity to be configured.**
  If Workload Identity is not set up on the GKE cluster (or the backup Pod's Kubernetes
  ServiceAccount is not annotated with a GCP service account), credential fetching from
  the metadata service silently returns no identity, causing backup failures. When not
  using Workload Identity, provide a `secretKeySelectors.serviceAccountJSON` reference
  instead.
- **`secretKeySelectors` must be omitted or null with `fetchCredentialsFromMetadataService: true`.**
  Setting both at the same time is not valid according to the CRD spec.
- **Cross-namespace use is not supported.** The SGObjectStorage and the SGCluster that
  references it must be in the same namespace. The credential Secret must also live in
  that namespace.
