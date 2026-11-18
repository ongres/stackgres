---
title: SGObjectStorage
weight: 9
url: /reference/crd/sgobjectstorage
description: Details about SGObjectStorage
showToc: true
---

___

**Kind:** SGObjectStorage

**listKind:** SGObjectStorageList

**plural:** sgobjectstorages

**singular:** sgobjectstorage

**shortNames** sgobjs
___

The `SGObjectStorage` custom resource allows to configure where backups are going to be stored.
The object storage represents a persistence location.

**Example:**

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

See also [Backup Storage section]({{%  relref "04-administration-guide/05-backups#backup-storage" %}}).





<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr><tr>
      <td><b>apiVersion</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>stackgres.io/v1beta1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGObjectStorage</td>
      </tr>
      <tr>
      <td><b><a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta">metadata</a></b></td>
      <td>✓</td>
      <td>✓</td>
      <td></td>
      <td>object</td>
      <td>Refer to the Kubernetes API documentation for the fields of the `metadata` field.</td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Object Storage configuration
<br/>
        </td>
      </tr></tbody>
</table>

## SGObjectStorage.spec
<sup><sup>[↩ Parent](#sgobjectstorage)</sup></sup>

Object Storage configuration


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>enum</td>
        <td>
          Determine the type of object storage used for storing the base backups and WAL segments.
      Possible values:
      *  `s3`: Amazon Web Services S3 (Simple Storage Service).
      *  `s3Compatible`: non-AWS services that implement a compatibility API with AWS S3.
      *  `gcs`: Google Cloud Storage.
      *  `azureBlob`: Microsoft Azure Blob Storage.
<br/>
          <br/>
            <i>Enum</i>: s3, s3Compatible, gcs, azureBlob<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecazureblob">azureBlob</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Azure Blob Storage configuration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecgcs">gcs</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Google Cloud Storage configuration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3">s3</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Amazon Web Services S3 configuration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3compatible">s3Compatible</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          AWS S3-Compatible API configuration<br/>
        </td>
      </tr></tbody>
</table>

### SGObjectStorage.spec.azureBlob
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

Azure Blob Storage configuration.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecazureblobazurecredentials">azureCredentials</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The credentials to access Azure Blob Storage for writing and reading.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>bucket</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Azure Blob Storage bucket name.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGObjectStorage.spec.azureBlob.azureCredentials
<sup><sup>[↩ Parent](#sgobjectstoragespecazureblob)</sup></sup>

The credentials to access Azure Blob Storage for writing and reading.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecazureblobazurecredentialssecretkeyselectors">secretKeySelectors</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core)(s) to reference the Secret(s) that contain the information about the `azureCredentials`. . Note that you may use the same or different Secrets for the `storageAccount` and the `accessKey`. In the former case, the `keys` that identify each must be, obviously, different.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors
<sup><sup>[↩ Parent](#sgobjectstoragespecazureblobazurecredentials)</sup></sup>

Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core)(s) to reference the Secret(s) that contain the information about the `azureCredentials`. . Note that you may use the same or different Secrets for the `storageAccount` and the `accessKey`. In the former case, the `keys` that identify each must be, obviously, different.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecazureblobazurecredentialssecretkeyselectorsaccesskey">accessKey</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The [storage account access key](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-keys-manage?tabs=azure-portal).
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecazureblobazurecredentialssecretkeyselectorsstorageaccount">storageAccount</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The [Storage Account](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-overview?toc=/azure/storage/blobs/toc.json) that contains the Blob bucket to be used.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors.accessKey
<sup><sup>[↩ Parent](#sgobjectstoragespecazureblobazurecredentialssecretkeyselectors)</sup></sup>

The [storage account access key](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-keys-manage?tabs=azure-portal).


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount
<sup><sup>[↩ Parent](#sgobjectstoragespecazureblobazurecredentialssecretkeyselectors)</sup></sup>

The [Storage Account](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-overview?toc=/azure/storage/blobs/toc.json) that contains the Blob bucket to be used.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).
<br/>
        </td>
      </tr></tbody>
</table>

### SGObjectStorage.spec.gcs
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

Google Cloud Storage configuration.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>bucket</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          GCS bucket name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecgcsgcpcredentials">gcpCredentials</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The credentials to access GCS for writing and reading.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGObjectStorage.spec.gcs.gcpCredentials
<sup><sup>[↩ Parent](#sgobjectstoragespecgcs)</sup></sup>

The credentials to access GCS for writing and reading.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>fetchCredentialsFromMetadataService</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true, the credentials will be fetched from the GCE/GKE metadata service and the field `secretKeySelectors` have to be set to null or omitted.

This is useful when running StackGres inside a GKE cluster using [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity).
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecgcsgcpcredentialssecretkeyselectors">secretKeySelectors</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core) to reference the Secrets that contain the information about the Service Account to access GCS.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.gcs.gcpCredentials.secretKeySelectors
<sup><sup>[↩ Parent](#sgobjectstoragespecgcsgcpcredentials)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core) to reference the Secrets that contain the information about the Service Account to access GCS.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecgcsgcpcredentialssecretkeyselectorsserviceaccountjson">serviceAccountJSON</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A service account key from GCP. In JSON format, as downloaded from the GCP Console.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON
<sup><sup>[↩ Parent](#sgobjectstoragespecgcsgcpcredentialssecretkeyselectors)</sup></sup>

A service account key from GCP. In JSON format, as downloaded from the GCP Console.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).
<br/>
        </td>
      </tr></tbody>
</table>

### SGObjectStorage.spec.s3
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

Amazon Web Services S3 configuration.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3awscredentials">awsCredentials</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The credentials to access AWS S3 for writing and reading.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>bucket</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          AWS S3 bucket name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>region</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The AWS S3 region. The Region may be detected using s3:GetBucketLocation, but if you wish to avoid giving permissions to this API call or forbid it from the applicable IAM policy, you must then specify this property.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>storageClass</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The [Amazon S3 Storage Class](https://aws.amazon.com/s3/storage-classes/) to use for the backup object storage. By default, the `STANDARD` storage class is used. Other supported values include `STANDARD_IA` for Infrequent Access and `REDUCED_REDUNDANCY`.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGObjectStorage.spec.s3.awsCredentials
<sup><sup>[↩ Parent](#sgobjectstoragespecs3)</sup></sup>

The credentials to access AWS S3 for writing and reading.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3awscredentialssecretkeyselectors">secretKeySelectors</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core)(s) to reference the Secrets that contain the information about the `awsCredentials`. Note that you may use the same or different Secrets for the `accessKeyId` and the `secretAccessKey`. In the former case, the `keys` that identify each must be, obviously, different.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.s3.awsCredentials.secretKeySelectors
<sup><sup>[↩ Parent](#sgobjectstoragespecs3awscredentials)</sup></sup>

Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core)(s) to reference the Secrets that contain the information about the `awsCredentials`. Note that you may use the same or different Secrets for the `accessKeyId` and the `secretAccessKey`. In the former case, the `keys` that identify each must be, obviously, different.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3awscredentialssecretkeyselectorsaccesskeyid">accessKeyId</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          AWS [access key ID](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `AKIAIOSFODNN7EXAMPLE`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3awscredentialssecretkeyselectorssecretaccesskey">secretAccessKey</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          AWS [secret access key](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.s3.awsCredentials.secretKeySelectors.accessKeyId
<sup><sup>[↩ Parent](#sgobjectstoragespecs3awscredentialssecretkeyselectors)</sup></sup>

AWS [access key ID](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `AKIAIOSFODNN7EXAMPLE`.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey
<sup><sup>[↩ Parent](#sgobjectstoragespecs3awscredentialssecretkeyselectors)</sup></sup>

AWS [secret access key](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).
<br/>
        </td>
      </tr></tbody>
</table>

### SGObjectStorage.spec.s3Compatible
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

AWS S3-Compatible API configuration

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3compatibleawscredentials">awsCredentials</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The credentials to access AWS S3 for writing and reading.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>bucket</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Bucket name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>enablePathStyleAddressing</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Enable path-style addressing (i.e. `http://s3.amazonaws.com/BUCKET/KEY`) when connecting to an S3-compatible service that lacks support for sub-domain style bucket URLs (i.e. `http://BUCKET.s3.amazonaws.com/KEY`).

Defaults to false.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>endpoint</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Overrides the default url to connect to an S3-compatible service.
For example: `http://s3-like-service:9000`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>region</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The AWS S3 region. The Region may be detected using s3:GetBucketLocation, but if you wish to avoid giving permissions to this API call or forbid it from the applicable IAM policy, you must then specify this property.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>storageClass</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The [Amazon S3 Storage Class](https://aws.amazon.com/s3/storage-classes/) to use for the backup object storage. By default, the `STANDARD` storage class is used. Other supported values include `STANDARD_IA` for Infrequent Access and `REDUCED_REDUNDANCY`.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGObjectStorage.spec.s3Compatible.awsCredentials
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatible)</sup></sup>

The credentials to access AWS S3 for writing and reading.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectors">secretKeySelectors</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core)(s) to reference the Secret(s) that contain the information about the `awsCredentials`. Note that you may use the same or different Secrets for the `accessKeyId` and the `secretAccessKey`. In the former case, the `keys` that identify each must be, obviously, different.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatibleawscredentials)</sup></sup>

Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core)(s) to reference the Secret(s) that contain the information about the `awsCredentials`. Note that you may use the same or different Secrets for the `accessKeyId` and the `secretAccessKey`. In the former case, the `keys` that identify each must be, obviously, different.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectorsaccesskeyid">accessKeyId</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          AWS [access key ID](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `AKIAIOSFODNN7EXAMPLE`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectorssecretaccesskey">secretAccessKey</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          AWS [secret access key](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectorscacertificate">caCertificate</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          CA Certificate file to be used when connecting to the S3 Compatible Service.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectors)</sup></sup>

AWS [access key ID](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `AKIAIOSFODNN7EXAMPLE`.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectors)</sup></sup>

AWS [secret access key](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).
<br/>
        </td>
      </tr></tbody>
</table>

##### SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.caCertificate
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectors)</sup></sup>

CA Certificate file to be used when connecting to the S3 Compatible Service.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).
<br/>
        </td>
      </tr></tbody>
</table>
