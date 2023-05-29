---
title: SGObjectStorage
weight: 10
url: reference/crd/sgobjectstorage
description: Details about SGObjectStorage
showToc: true
---

The `SGObjectStorage` custom resource allows to configure where backups are going to be stored.
The object storage represents a persistence location.

___

**Kind:** SGObjectStorage

**listKind:** SGObjectStorageList

**plural:** sgobjectstorages

**singular:** sgobjectstorage
___

**Spec**

## Storage Type Configuration

| <div style="width:8rem">Property</div>                                | <div style="width:12rem">Required</div> | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:----------------------------------------------------------------------|-----------------------------------------|-----------|:-------|:--------|:------------|
| type                                                                  | ✓                                       | ✓         | string |         | {{< crd-field-description SGObjectStorage.spec.type >}} |
| [s3](#s3---amazon-web-services-s3-configuration)                      | if type = s3                            | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3 >}} |
| [s3Compatible](#s3---amazon-web-services-s3-configuration)            | if type = s3Compatible                  | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible >}} |
| [gcs](#gsc---google-cloud-storage-configuration)                      | if type = gcs                           | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.gcs >}} |
| [azureBlob](#azure---azure-blob-storage-configuration)                | if type = azureblob                     | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.azureBlob >}} |

## S3

### S3 - Amazon Web Services S3 configuration

| <div style="width:8rem">Property</div>             | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                                             | ✓        | ✓         | string  |         | {{< crd-field-description SGObjectStorage.spec.s3.bucket >}} |
| [awsCredentials](#amazon-web-services-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGObjectStorage.spec.s3.awsCredentials >}} |
| region                                             |          | ✓         | string  |         | {{< crd-field-description SGObjectStorage.spec.s3.region >}} |
| storageClass                                       |          | ✓         | string  |         | {{< crd-field-description SGObjectStorage.spec.s3.storageClass >}} |

### S3 - Amazon Web Services S3 Compatible configuration

| <div style="width:14rem">Property</div>            | Required | Updatable | <div style="width:5rem">Type</div> | Default | Description |
|:---------------------------------------------------|----------|-----------|:--------|:--------|:------------|
| bucket                                             | ✓        | ✓         | string  |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.bucket >}} |
| [awsCredentials](#amazon-web-services-compatible-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.awsCredentials >}} |
| region                                             |          | ✓         | string  |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.region >}} |
| storageClass                                       |          | ✓         | string  |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.storageClass >}} |
| endpoint                                           |          | ✓         | string  |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.endpoint >}} |
| enablePathStyleAddressing                          |          | ✓         | boolean |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.enablePathStyleAddressing >}} |

### Amazon Web Services Credentials

| <div style="width:10rem">Property</div>                        | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#amazon-web-services-s3-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors >}} |

#### Amazon Web Services Secret Key Selector

| Property                                                                                                          | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [accessKeyId](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)     | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId >}} |
| [secretAccessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey >}} |

### Amazon Web Services S3 Compatible Credentials

| <div style="width:10rem">Property</div>                        | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:---------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#amazon-web-services-s3-compatible-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors >}} |

#### Amazon Web Services S3 Compatible Secret Key Selector

| Property                                                                                                          | Required | Updatable | Type   | Default | Description |
|:------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [accessKeyId](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)     | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId >}} |
| [secretAccessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey >}} |
| [caCertificate](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)   |          | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.caCertificate >}} |

## GSC - Google Cloud Storage configuration

| Property                           | Required | Updatable | Type   | Default | Description |
|:-----------------------------------|----------|-----------|:-------|:--------|:------------|
| bucket                             | ✓        | ✓         | string |         | {{< crd-field-description SGObjectStorage.spec.gcs.bucket >}} |
| [gcpCredentials](#gcp-credentials) | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.gcs.gcpCredentials >}} |

### GCP Credentials

| Property                                       | Required | Updatable | Type   | Default | Description |
|:-----------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#gcp-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.gcs.gcpCredentials.secretKeySelectors >}} |

#### GCP Secret Key Selector

| Property                                                                                                             | Required | Updatable | Type   | Default | Description |
|:---------------------------------------------------------------------------------------------------------------------|----------|:----------|:-------|:--------|:------------|
| [serviceAccountJSON](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core) | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON >}} |


## AZURE - Azure Blob Storage configuration

| Property                               | Required | Updatable | Type    | Default | Description |
|:---------------------------------------|----------|-----------|:--------|:--------|:-------------|
| bucket                                 | ✓        | ✓         | string  |         | {{< crd-field-description SGObjectStorage.spec.azureBlob.bucket >}} |
| [azureCredentials](#azure-credentials) | ✓        | ✓         | object  |         | {{< crd-field-description SGObjectStorage.spec.azureBlob.azureCredentials >}} |

### Azure Credentials

| <div style="width:10rem">Property</div>          | Required | Updatable | <div style="width:4rem">Type</div> | Default | Description |
|:-------------------------------------------------|----------|-----------|:-------|:--------|:------------|
| [secretKeySelectors](#azure-secret-key-selector) | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors >}} |

### Azure Secret Key Selector

| Property                                                                                                           | Required | Updatable | Type   | Default | Description |
|:-------------------------------------------------------------------------------------------------------------------|----------|-----------|:-------|:--------|:-------------|
| [storageAccount](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)   | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount >}} |
| [accessKey](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#secretkeyselector-v1-core)        | ✓        | ✓         | object |         | {{< crd-field-description SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors.accessKey >}} |

## Example

Example for an AWS S3-compatible storage configuration:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: objectstorage
spec:
  type: s3Compatible
  s3Compatible:
    bucket: stackgres
    region: k8s
    enablePathStyleAddressing: true
    endpoint: http://my-cluster-minio:9000
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          key: accesskey
          name: my-cluster-minio
        secretAccessKey:
          key: secretkey
          name: my-cluster-minio
```
