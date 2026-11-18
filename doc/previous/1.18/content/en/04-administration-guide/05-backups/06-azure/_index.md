---
title: Azure Blob Storage
weight: 6
url: /administration/backups/azure
description: Details about how to set up and configure backups on Azure Blob Storage.
showToc: true
---

## Azure Blob Storage Setup

This section shows how to configure backups on StackGres using Microsoft Azure Blob Storage.
You will need the [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) installed to create the required resources.

Let's create the storage account and container with the following characteristics (that you may change):

* Resource Group: `stackgres-rg`
* Location: `eastus`
* Storage Account: `stackgresbackups`
* Container name: `sgbackups`

### Create Resource Group (if needed)

```bash
az group create \
  --name stackgres-rg \
  --location eastus
```

### Create Storage Account

```bash
az storage account create \
  --name stackgresbackups \
  --resource-group stackgres-rg \
  --location eastus \
  --sku Standard_LRS \
  --kind StorageV2
```

### Create Blob Container

```bash
az storage container create \
  --name sgbackups \
  --account-name stackgresbackups
```

### Get Access Key

Retrieve the storage account access key:

```bash
az storage account keys list \
  --account-name stackgresbackups \
  --resource-group stackgres-rg \
  --query '[0].value' \
  --output tsv
```

Save this key securely - you'll need it for the Kubernetes Secret.

## Secret and SGObjectStorage

### Create the Credentials Secret

Create a Kubernetes Secret with the Azure storage account credentials:

```bash
# Set your values
STORAGE_ACCOUNT="stackgresbackups"
ACCESS_KEY="your-access-key-from-previous-step"

kubectl create secret generic azure-backup-secret \
  --from-literal=storageAccount="$STORAGE_ACCOUNT" \
  --from-literal=accessKey="$ACCESS_KEY"
```

Or using a YAML manifest:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: azure-backup-secret
type: Opaque
stringData:
  storageAccount: stackgresbackups
  accessKey: your-storage-account-access-key
```

### Create the SGObjectStorage

Create the object storage configuration using the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: azure-backup-storage
spec:
  type: azureBlob
  azureBlob:
    bucket: sgbackups
    azureCredentials:
      secretKeySelectors:
        storageAccount:
          name: azure-backup-secret
          key: storageAccount
        accessKey:
          name: azure-backup-secret
          key: accessKey
```

Apply the configuration:

```bash
kubectl apply -f sgobjectstorage.yaml
```

## Configuring Cluster Backups

Reference the SGObjectStorage in your cluster configuration:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '10Gi'
  configurations:
    backups:
      - sgObjectStorage: azure-backup-storage
        cronSchedule: '0 5 * * *'
        retention: 7
```

## Using a Subfolder Path

You can specify a path within the container to organize backups:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: azure-backup-storage
spec:
  type: azureBlob
  azureBlob:
    bucket: sgbackups/production/postgres
    azureCredentials:
      secretKeySelectors:
        storageAccount:
          name: azure-backup-secret
          key: storageAccount
        accessKey:
          name: azure-backup-secret
          key: accessKey
```

The bucket field can include path segments after the container name.

## Complete Example

Here's a complete example with all resources:

### 1. Create the Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: azure-backup-secret
  namespace: default
type: Opaque
stringData:
  storageAccount: stackgresbackups
  accessKey: "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx=="
```

### 2. Create the SGObjectStorage

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: azure-backup-storage
  namespace: default
spec:
  type: azureBlob
  azureBlob:
    bucket: sgbackups
    azureCredentials:
      secretKeySelectors:
        storageAccount:
          name: azure-backup-secret
          key: storageAccount
        accessKey:
          name: azure-backup-secret
          key: accessKey
```

### 3. Create the Cluster with Backups

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: production-cluster
  namespace: default
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '50Gi'
  configurations:
    backups:
      - sgObjectStorage: azure-backup-storage
        cronSchedule: '0 */6 * * *'  # Every 6 hours
        retention: 14                  # Keep 14 backups
        path: /production              # Optional subfolder
```

## Manual Backup

To create a manual backup:

```yaml
apiVersion: stackgres.io/v1
kind: SGBackup
metadata:
  name: manual-backup
spec:
  sgCluster: production-cluster
  managedLifecycle: false
```

## Restoring from Azure Backup

To restore a cluster from an Azure backup:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: restored-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '50Gi'
  initialData:
    restore:
      fromBackup:
        name: manual-backup
```

## Azure with Encryption

To add encryption to your Azure backups, see the [Backup Encryption]({{% relref "04-administration-guide/05-backups/05-encryption" %}}) guide:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: encrypted-azure-storage
spec:
  type: azureBlob
  encryption:
    method: sodium
    sodium:
      key:
        name: backup-encryption-key
        key: key
      keyTransform: hex
  azureBlob:
    bucket: sgbackups
    azureCredentials:
      secretKeySelectors:
        storageAccount:
          name: azure-backup-secret
          key: storageAccount
        accessKey:
          name: azure-backup-secret
          key: accessKey
```

## Azure Private Endpoints

For enhanced security, you can configure Azure Storage to use private endpoints. The storage account remains accessible from your AKS cluster via the private network.

1. Create a private endpoint for your storage account in the Azure portal
2. Configure your AKS cluster to use the same VNet or a peered VNet
3. Use the same SGObjectStorage configuration - no changes required

## Azure Storage Tiers

Azure Blob Storage supports different access tiers. StackGres uses the default tier (Hot) for backups. You can configure lifecycle management policies in Azure to move older backups to cooler tiers for cost optimization:

1. Go to the Storage Account in Azure Portal
2. Navigate to "Lifecycle management"
3. Create a rule to move blobs to Cool or Archive tier after a certain number of days

Note: Backups in Archive tier require rehydration before restore, which can take hours.
