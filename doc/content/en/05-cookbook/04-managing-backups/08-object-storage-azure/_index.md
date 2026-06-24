---
title: Backup storage on Azure Blob
weight: 8
url: /cookbook/managing-backups/object-storage-azure
description: Define an SGObjectStorage backed by Azure Blob Storage.
showToc: true
---

## What it does

Creates an [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) custom
resource that points StackGres at an Azure Blob Storage container for storing base backups
and WAL segments. Setting `spec.type: azureBlob` and supplying the container name together
with a reference to a Kubernetes Secret that holds the storage account name and access key
is all that is required.

## When to use it

- You run workloads on Azure (AKS or self-managed Kubernetes) and want backups stored in
  Azure Blob Storage.
- You are setting up a new cluster and need to provide a backup storage target before
  enabling automated backups on an SGCluster.
- You want to update the credentials or container of an existing backup target without
  re-creating the cluster.

## How to do it

### 1. Create the credentials Secret

Store the Azure Storage Account name and its access key in a Kubernetes Secret:

```bash
kubectl create secret generic azure-backup-credentials \
  --namespace my-cluster \
  --from-literal=storage-account=mystorageaccount \
  --from-literal=access-key='base64encodedAccessKey=='
```

### 2. Apply the SGObjectStorage

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: my-cluster     # same namespace as the SGCluster
  name: azure-backup-storage
spec:
  type: azureBlob           # selects the Azure Blob backend
  azureBlob:
    bucket: my-backups      # Azure Blob container name
    azureCredentials:
      secretKeySelectors:
        storageAccount:     # references the storage account name in the Secret
          name: azure-backup-credentials
          key: storage-account
        accessKey:          # references the access key in the Secret
          name: azure-backup-credentials
          key: access-key
```

```bash
kubectl apply -f sgobjectstorage-azure.yaml
```

### 3. Reference it from the SGCluster

Point the cluster's backup configuration at the new storage object:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  # ...other fields unchanged
  configurations:
    backups:
    - sgObjectStorage: azure-backup-storage
      cronSchedule: "0 5 * * *"
      retention: 5
```

## How it works

The SGObjectStorage resource is purely declarative: it holds the backend type, the container
name, and references to Kubernetes Secrets — it does not start any process by itself. When
the operator reconciles an SGCluster that references the SGObjectStorage via
`spec.configurations.backups[].sgObjectStorage`, it injects the Azure credentials into the
backup sidecar (pgBackRest) and configures the repository path inside the named container.
Changes to the SGObjectStorage (for example rotating the access key) are picked up on the
next reconciliation loop without restarting the cluster.

## What to expect

- After applying, verify the resource was accepted:

  ```bash
  kubectl get sgobjectstorage -n my-cluster azure-backup-storage
  ```

- Once the SGCluster references it and a scheduled backup runs, backups appear under a path
  derived from the cluster namespace and name inside the specified container.
- Restoring or cloning a cluster from this storage follows the standard restore flow
  described in the administration guide.

## Pitfalls

- **Storage account name and access key are both required.** The
  `secretKeySelectors.storageAccount` and `secretKeySelectors.accessKey` fields are both
  required; omitting either causes the operator to reject the resource.
- **Secret must be in the same namespace.** The Secret referenced by `secretKeySelectors`
  must exist in the same namespace as the SGObjectStorage. A Secret in a different namespace
  is not resolvable and will cause reconciliation errors.
- **Container must exist before first backup.** StackGres does not create the Azure Blob
  container. Create it in your Azure subscription before running the first backup job.
- **Access key rotation.** Rotating the key in Azure requires updating the Secret value and
  then patching the SGObjectStorage (or its referencing SGCluster) to trigger
  re-reconciliation. Until reconciliation completes, in-flight backups may fail.
- **Key format.** Azure access keys are base64-encoded strings. Store the raw value from the
  Azure portal directly in the Secret — do not double-encode it.
