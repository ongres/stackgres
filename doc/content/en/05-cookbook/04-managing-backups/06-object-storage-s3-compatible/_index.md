---
title: Backup storage on S3-compatible storage
weight: 6
url: /cookbook/managing-backups/object-storage-s3-compatible
description: Define an SGObjectStorage for MinIO, DigitalOcean Spaces, and other S3-compatible backends.
showToc: true
---

## What it does

Creates an [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) with
`spec.type: s3Compatible`, pointing the operator at any S3-compatible object store — MinIO,
DigitalOcean Spaces, Ceph RGW, Cloudflare R2, and similar. The resource is referenced by an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to store base backups and WAL
segments.

## When to use it

- Your cluster runs in an environment without direct access to AWS S3 but with an on-premises
  or cloud-provider S3-compatible service.
- You are using MinIO inside or alongside the Kubernetes cluster.
- Your S3-compatible endpoint uses a self-signed certificate and requires a custom CA.
- You want a single reusable storage resource that multiple clusters in the same namespace can
  reference.

## How to do it

### 1. Create the credentials Secret

Store the access key ID and secret access key (and, if needed, the CA certificate) in a
Kubernetes Secret before creating the `SGObjectStorage`:

```bash
kubectl create secret generic s3c-backup-credentials \
  --namespace my-cluster \
  --from-literal=accessKeyId=EXAMPLEACCESSKEYID \
  --from-literal=secretAccessKey=EXAMPLESECRETACCESSKEY
```

For a self-signed endpoint, add the CA certificate to the same or a separate Secret:

```bash
kubectl create secret generic s3c-ca \
  --namespace my-cluster \
  --from-file=ca.crt=/path/to/ca.crt
```

### 2. Apply the SGObjectStorage

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: my-cluster
  name: my-object-storage
spec:
  type: s3Compatible
  s3Compatible:
    bucket: postgres-backups          # required — bucket name on the S3-compatible service
    endpoint: http://minio:9000       # required for non-AWS — full URL of the service
    enablePathStyleAddressing: true   # set true when the backend lacks virtual-hosted-style URLs
    region: us-east-1                 # optional — some backends require a region string
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: s3c-backup-credentials
          key: accessKeyId
        secretAccessKey:
          name: s3c-backup-credentials
          key: secretAccessKey
        caCertificate:                # optional — omit when the endpoint uses a trusted cert
          name: s3c-ca
          key: ca.crt
```

```bash
kubectl apply -f sgobjectstorage.yaml
```

### 3. Reference it from the SGCluster

Add a `backups` entry to `spec.configurations` of the target cluster (see the "Configuring
automated backups" recipe for the full cluster patch):

```yaml
spec:
  configurations:
    backups:
    - sgObjectStorage: my-object-storage
      cronSchedule: "0 2 * * *"
      retention: 7
```

## How it works

`SGObjectStorage` is a namespace-scoped resource that holds storage-backend configuration
independently of any cluster. The operator reads its `spec` and passes the resolved
credentials and endpoint parameters to WAL-G on every pod that performs archiving or
restore operations.

When `type` is `s3Compatible`, the operator selects the `spec.s3Compatible` block and
ignores `s3`, `gcs`, and `azureBlob`. `awsCredentials.secretKeySelectors` resolves the
referenced Secret keys at runtime; the Secret values are never stored in the CRD itself.

`enablePathStyleAddressing` changes whether WAL-G constructs URLs as
`http://endpoint/bucket/key` (path-style, `true`) or `http://bucket.endpoint/key`
(virtual-hosted-style, `false`). Most self-hosted backends require path-style.

See the full field reference at
[SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}).

## What to expect

After applying the manifest the resource is visible immediately:

```bash
kubectl get sgobjectstorage -n my-cluster my-object-storage
```

The operator validates the resource via the admission webhook. If the referenced Secret or
key does not exist the webhook rejects the `SGObjectStorage`. Once an `SGCluster` references
it, scheduled backup jobs use the configured endpoint and credentials. Confirm the first
backup completes:

```bash
kubectl get sgbackup -n my-cluster -w
```

A successful backup shows `status.process.status: Completed`.

## Pitfalls

- **Most S3-compatible backends require `enablePathStyleAddressing: true`.** Without it,
  WAL-G constructs virtual-hosted-style URLs (e.g. `http://mybucket.minio:9000/`) that
  MinIO and most self-hosted services do not route correctly. The backup job fails with a
  DNS or connection error that can look like a credential problem.
- **`endpoint` must be set; there is no default for non-AWS.**  Omitting it causes WAL-G to
  target `s3.amazonaws.com`, which will reject credentials issued by a private service.
- **Self-signed endpoints need `caCertificate`.**  If the endpoint uses HTTPS with a
  certificate not trusted by the system CA bundle, WAL-G will fail TLS verification. Store
  the PEM-encoded CA in a Secret and reference it via
  `awsCredentials.secretKeySelectors.caCertificate`.
- **The Secret must exist in the same namespace before the `SGObjectStorage` is created.**
  The admission webhook resolves Secret references at admission time and rejects the resource
  if any referenced Secret or key is missing.
- **Changing `bucket` or `endpoint` after backups have been written does not migrate data.**
  Existing `SGBackup` resources keep metadata pointing to the old location. Update these
  fields only when you intend to start a fresh backup set or have already migrated the
  objects manually.
