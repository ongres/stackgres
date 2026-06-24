---
title: Backup storage on AWS S3
weight: 5
url: /cookbook/managing-backups/object-storage-s3
description: Define an SGObjectStorage backed by AWS S3.
showToc: true
---

## What it does

Creates an [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) that
points to an AWS S3 bucket. Once created, any SGCluster in the same namespace can
reference it to store base backups and WAL segments. The operator reconciles credential
changes without restarting Pods.

## When to use it

- You are enabling automated or on-demand backups for the first time and AWS S3 is your
  target storage.
- You want to share one storage definition across multiple clusters.
- You are rotating IAM credentials and need the change to take effect without downtime.

## How to do it

Create a Kubernetes Secret that holds the AWS access key pair, then apply the
SGObjectStorage that references it.

```bash
kubectl create secret generic s3-credentials \
  --namespace my-cluster \
  --from-literal=accessKeyId=AKIAIOSFODNN7EXAMPLE \
  --from-literal=secretAccessKey=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: my-cluster
  name: s3-storage
spec:
  # Use the native AWS S3 endpoint (not an S3-compatible service).
  type: s3
  s3:
    bucket: my-cluster-backups      # AWS S3 bucket name; must already exist.
    region: us-east-1               # Optional but avoids an s3:GetBucketLocation call.
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: s3-credentials      # Secret created above.
          key: accessKeyId
        secretAccessKey:
          name: s3-credentials
          key: secretAccessKey
```

```bash
kubectl apply -f sgobjectstorage.yaml
```

To use an IAM role attached to the node or Pod instead of a static key pair, set
`useIAMRole: true` and omit `secretKeySelectors`:

```yaml
spec:
  type: s3
  s3:
    bucket: my-cluster-backups
    region: us-east-1
    awsCredentials:
      useIAMRole: true
```

Once the SGObjectStorage exists, reference it from an SGCluster via the automated
backups recipe by setting
`spec.configurations.backups[].sgObjectStorage: s3-storage`.

## How it works

SGObjectStorage is a standalone custom resource; it does not create any Pods or
connect to S3 at admission time. When an SGCluster that references it triggers a
backup (on schedule or on demand), the operator mounts the credential Secret into the
backup Job and passes the bucket coordinates to the WAL-G archiver. Changes to the
SGObjectStorage are picked up on the next reconciliation loop without restarting
cluster Pods.

## What to expect

- `kubectl get sgobjectstorage -n my-cluster` lists the resource with no external
  status conditions — the operator only validates the credentials when a backup runs.
- The first backup job logs from `wal-g` will surface any S3 permission errors
  immediately.
- Updating `region` or rotating the Secret keys takes effect on the next scheduled or
  on-demand backup without Pod restarts.

## Pitfalls

- **Bucket must exist and be pre-created.** The operator does not create the S3 bucket.
  Ensure it exists in the specified region before running a backup.
- **IAM policy must cover the required actions.** At minimum the IAM principal needs
  `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`, and `s3:ListBucket` on the bucket.
  Missing `s3:GetBucketLocation` only matters when `region` is omitted.
- **`useIAMRole` requires Pod-level IAM binding.** With `useIAMRole: true` the backup
  Job Pods must be able to reach the instance metadata service or have an IRSA
  (IAM Roles for Service Accounts) annotation. Static `secretKeySelectors` are ignored
  when this flag is `true`.
- **Wrong region causes silent failures.** If the bucket is in `eu-west-1` but `region`
  is set to `us-east-1`, the S3 client redirects fail. Always set `region` explicitly
  to match the bucket's region.
- **Cross-namespace use is not supported.** The SGObjectStorage and the SGCluster that
  references it must be in the same namespace. The credential Secret must also live in
  that namespace.
