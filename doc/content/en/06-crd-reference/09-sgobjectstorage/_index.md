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





{{< crd-properties >}}
[
  {
    "description": "stackgres.io/v1beta1",
    "immutable": true,
    "name": "apiVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "SGObjectStorage",
    "immutable": true,
    "name": "kind",
    "required": true,
    "type": "string"
  },
  {
    "description": "Refer to the Kubernetes API documentation for the fields of the `metadata` field.",
    "link": "https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta",
    "name": "metadata",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespec",
    "description": "Object Storage configuration",
    "name": "spec",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGObjectStorage.spec
<sup><sup>[↩ Parent](#sgobjectstorage)</sup></sup>

Object Storage configuration


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecazureblob",
    "description": "Azure Blob Storage configuration.",
    "name": "azureBlob",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecencryption",
    "description": "Section to configure object storage encryption of stored files.",
    "name": "encryption",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecgcs",
    "description": "Google Cloud Storage configuration.",
    "name": "gcs",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecs3",
    "description": "Amazon Web Services S3 configuration.",
    "name": "s3",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecs3compatible",
    "description": "AWS S3-Compatible API configuration",
    "name": "s3Compatible",
    "required": false,
    "type": "object"
  },
  {
    "description": "Determine the type of object storage used for storing the base backups and WAL segments.\n      Possible values:\n      *  `s3`: Amazon Web Services S3 (Simple Storage Service).\n      *  `s3Compatible`: non-AWS services that implement a compatibility API with AWS S3.\n      *  `gcs`: Google Cloud Storage.\n      *  `azureBlob`: Microsoft Azure Blob Storage.",
    "enum": [
      "s3",
      "s3Compatible",
      "gcs",
      "azureBlob"
    ],
    "name": "type",
    "required": false,
    "type": "enum"
  }
]
{{< /crd-properties >}}

### SGObjectStorage.spec.azureBlob
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

Azure Blob Storage configuration.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecazureblobazurecredentials",
    "description": "The credentials to access Azure Blob Storage for writing and reading.",
    "name": "azureCredentials",
    "required": true,
    "type": "object"
  },
  {
    "description": "Azure Blob Storage bucket name.",
    "name": "bucket",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGObjectStorage.spec.azureBlob.azureCredentials
<sup><sup>[↩ Parent](#sgobjectstoragespecazureblob)</sup></sup>

The credentials to access Azure Blob Storage for writing and reading.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecazureblobazurecredentialssecretkeyselectors",
    "description": "Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core)(s) to reference the Secret(s) that contain the information about the `azureCredentials`. Note that you may use the same or different Secrets for the `storageAccount` and the `accessKey`. In the former case, the `keys` that identify each must be, obviously, different.",
    "name": "secretKeySelectors",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors
<sup><sup>[↩ Parent](#sgobjectstoragespecazureblobazurecredentials)</sup></sup>

Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core)(s) to reference the Secret(s) that contain the information about the `azureCredentials`. Note that you may use the same or different Secrets for the `storageAccount` and the `accessKey`. In the former case, the `keys` that identify each must be, obviously, different.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecazureblobazurecredentialssecretkeyselectorsaccesskey",
    "description": "The [storage account access key](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-keys-manage?tabs=azure-portal).",
    "name": "accessKey",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecazureblobazurecredentialssecretkeyselectorsstorageaccount",
    "description": "The [Storage Account](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-overview?toc=/azure/storage/blobs/toc.json) that contains the Blob bucket to be used.",
    "name": "storageAccount",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors.accessKey
<sup><sup>[↩ Parent](#sgobjectstoragespecazureblobazurecredentialssecretkeyselectors)</sup></sup>

The [storage account access key](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-keys-manage?tabs=azure-portal).


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount
<sup><sup>[↩ Parent](#sgobjectstoragespecazureblobazurecredentialssecretkeyselectors)</sup></sup>

The [Storage Account](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-overview?toc=/azure/storage/blobs/toc.json) that contains the Blob bucket to be used.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGObjectStorage.spec.encryption
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

Section to configure object storage encryption of stored files.


{{< crd-properties >}}
[
  {
    "description": "Select the storage encryption method.\n\nPossible options are:\n\n* `sodium`: will use libsodium to encrypt the files stored.\n* `openpgp`: will use OpenPGP standard to encrypt the files stored.\n\nWhen not set no encryption will be applied to stored files.",
    "name": "method",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgobjectstoragespecencryptionopenpgp",
    "description": "OpenPGP encryption configuration.",
    "name": "openpgp",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecencryptionsodium",
    "description": "libsodium encryption configuration.",
    "name": "sodium",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGObjectStorage.spec.encryption.openpgp
<sup><sup>[↩ Parent](#sgobjectstoragespecencryption)</sup></sup>

OpenPGP encryption configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecencryptionopenpgpkey",
    "description": "To configure encryption and decryption with OpenPGP standard. You can join multiline\n key using `\n` symbols into one line (mostly used in case of daemontools and envdir).",
    "name": "key",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecencryptionopenpgpkeypassphrase",
    "description": "If your private key is encrypted with a passphrase, you should set the passphrase for decryption.",
    "name": "keyPassphrase",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.encryption.openpgp.key
<sup><sup>[↩ Parent](#sgobjectstoragespecencryptionopenpgp)</sup></sup>

To configure encryption and decryption with OpenPGP standard. You can join multiline
 key using `\n` symbols into one line (mostly used in case of daemontools and envdir).


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.encryption.openpgp.keyPassphrase
<sup><sup>[↩ Parent](#sgobjectstoragespecencryptionopenpgp)</sup></sup>

If your private key is encrypted with a passphrase, you should set the passphrase for decryption.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGObjectStorage.spec.encryption.sodium
<sup><sup>[↩ Parent](#sgobjectstoragespecencryption)</sup></sup>

libsodium encryption configuration.

{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecencryptionsodiumkey",
    "description": "To configure encryption and decryption with libsodium an algorithm that only requires\n a secret key is used. libsodium keys are fixed-size keys of 32 bytes. For optimal\n cryptographic security, it is recommended to use a random 32 byte key. To generate a\n random key, you can use something like `openssl rand -hex 32` (set `keyTransform` to `hex`)\n or `openssl rand -base64 32` (set `keyTransform` to `base64`).",
    "name": "key",
    "required": true,
    "type": "object"
  },
  {
    "default": "none",
    "description": "The transform that will be applied to the `key` to get the required 32 byte key.\n Supported transformations are `base64`, `hex` or `none` (default). The option\n none exists for backwards compatibility, the user input will be converted to 32\n byte either via truncation or by zero-padding.",
    "name": "keyTransform",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.encryption.sodium.key
<sup><sup>[↩ Parent](#sgobjectstoragespecencryptionsodium)</sup></sup>

To configure encryption and decryption with libsodium an algorithm that only requires
 a secret key is used. libsodium keys are fixed-size keys of 32 bytes. For optimal
 cryptographic security, it is recommended to use a random 32 byte key. To generate a
 random key, you can use something like `openssl rand -hex 32` (set `keyTransform` to `hex`)
 or `openssl rand -base64 32` (set `keyTransform` to `base64`).


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGObjectStorage.spec.gcs
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

Google Cloud Storage configuration.


{{< crd-properties >}}
[
  {
    "description": "GCS bucket name.",
    "name": "bucket",
    "required": true,
    "type": "string"
  },
  {
    "anchor": "sgobjectstoragespecgcsgcpcredentials",
    "description": "The credentials to access GCS for writing and reading.",
    "name": "gcpCredentials",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGObjectStorage.spec.gcs.gcpCredentials
<sup><sup>[↩ Parent](#sgobjectstoragespecgcs)</sup></sup>

The credentials to access GCS for writing and reading.


{{< crd-properties >}}
[
  {
    "description": "If true, the credentials will be fetched from the GCE/GKE metadata service and the field `secretKeySelectors` have to be set to null or omitted.\n\nThis is useful when running StackGres inside a GKE cluster using [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity).",
    "name": "fetchCredentialsFromMetadataService",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgobjectstoragespecgcsgcpcredentialssecretkeyselectors",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) to reference the Secrets that contain the information about the Service Account to access GCS.",
    "name": "secretKeySelectors",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.gcs.gcpCredentials.secretKeySelectors
<sup><sup>[↩ Parent](#sgobjectstoragespecgcsgcpcredentials)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core) to reference the Secrets that contain the information about the Service Account to access GCS.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecgcsgcpcredentialssecretkeyselectorsserviceaccountjson",
    "description": "A service account key from GCP. In JSON format, as downloaded from the GCP Console.",
    "name": "serviceAccountJSON",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON
<sup><sup>[↩ Parent](#sgobjectstoragespecgcsgcpcredentialssecretkeyselectors)</sup></sup>

A service account key from GCP. In JSON format, as downloaded from the GCP Console.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGObjectStorage.spec.s3
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

Amazon Web Services S3 configuration.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecs3awscredentials",
    "description": "The credentials to access AWS S3 for writing and reading.",
    "name": "awsCredentials",
    "required": true,
    "type": "object"
  },
  {
    "description": "AWS S3 bucket name.",
    "name": "bucket",
    "required": true,
    "type": "string"
  },
  {
    "description": "The AWS S3 region. The Region may be detected using s3:GetBucketLocation, but if you wish to avoid giving permissions to this API call or forbid it from the applicable IAM policy, you must then specify this property.",
    "name": "region",
    "required": false,
    "type": "string"
  },
  {
    "description": "The [Amazon S3 Storage Class](https://aws.amazon.com/s3/storage-classes/) to use for the backup object storage. By default, the `STANDARD` storage class is used. Other supported values include `STANDARD_IA` for Infrequent Access and `REDUCED_REDUNDANCY`.",
    "name": "storageClass",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGObjectStorage.spec.s3.awsCredentials
<sup><sup>[↩ Parent](#sgobjectstoragespecs3)</sup></sup>

The credentials to access AWS S3 for writing and reading.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecs3awscredentialssecretkeyselectors",
    "description": "Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core)(s) to reference the Secrets that contain the information about the `awsCredentials`. Note that you may use the same or different Secrets for the `accessKeyId` and the `secretAccessKey`. In the former case, the `keys` that identify each must be, obviously, different.",
    "name": "secretKeySelectors",
    "required": false,
    "type": "object"
  },
  {
    "default": false,
    "description": "When set to `true` the IAM role credentials will be used instead of the secret key selector.",
    "name": "useIAMRole",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.s3.awsCredentials.secretKeySelectors
<sup><sup>[↩ Parent](#sgobjectstoragespecs3awscredentials)</sup></sup>

Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core)(s) to reference the Secrets that contain the information about the `awsCredentials`. Note that you may use the same or different Secrets for the `accessKeyId` and the `secretAccessKey`. In the former case, the `keys` that identify each must be, obviously, different.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecs3awscredentialssecretkeyselectorsaccesskeyid",
    "description": "AWS [access key ID](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `AKIAIOSFODNN7EXAMPLE`.",
    "name": "accessKeyId",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecs3awscredentialssecretkeyselectorssecretaccesskey",
    "description": "AWS [secret access key](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.",
    "name": "secretAccessKey",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.s3.awsCredentials.secretKeySelectors.accessKeyId
<sup><sup>[↩ Parent](#sgobjectstoragespecs3awscredentialssecretkeyselectors)</sup></sup>

AWS [access key ID](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `AKIAIOSFODNN7EXAMPLE`.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey
<sup><sup>[↩ Parent](#sgobjectstoragespecs3awscredentialssecretkeyselectors)</sup></sup>

AWS [secret access key](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGObjectStorage.spec.s3Compatible
<sup><sup>[↩ Parent](#sgobjectstoragespec)</sup></sup>

AWS S3-Compatible API configuration

{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecs3compatibleawscredentials",
    "description": "The credentials to access AWS S3 for writing and reading.",
    "name": "awsCredentials",
    "required": true,
    "type": "object"
  },
  {
    "description": "Bucket name.",
    "name": "bucket",
    "required": true,
    "type": "string"
  },
  {
    "description": "Enable path-style addressing (i.e. `http://s3.amazonaws.com/BUCKET/KEY`) when connecting to an S3-compatible service that lacks support for sub-domain style bucket URLs (i.e. `http://BUCKET.s3.amazonaws.com/KEY`).\n\nDefaults to false.",
    "name": "enablePathStyleAddressing",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Overrides the default url to connect to an S3-compatible service.\nFor example: `http://s3-like-service:9000`.",
    "name": "endpoint",
    "required": false,
    "type": "string"
  },
  {
    "description": "The AWS S3 region. The Region may be detected using s3:GetBucketLocation, but if you wish to avoid giving permissions to this API call or forbid it from the applicable IAM policy, you must then specify this property.",
    "name": "region",
    "required": false,
    "type": "string"
  },
  {
    "description": "The [Amazon S3 Storage Class](https://aws.amazon.com/s3/storage-classes/) to use for the backup object storage. By default, the `STANDARD` storage class is used. Other supported values include `STANDARD_IA` for Infrequent Access and `REDUCED_REDUNDANCY`.",
    "name": "storageClass",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGObjectStorage.spec.s3Compatible.awsCredentials
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatible)</sup></sup>

The credentials to access AWS S3 for writing and reading.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecs3compatibleawscredentialssecretkeyselectors",
    "description": "Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core)(s) to reference the Secret(s) that contain the information about the `awsCredentials`. Note that you may use the same or different Secrets for the `accessKeyId` and the `secretAccessKey`. In the former case, the `keys` that identify each must be, obviously, different.",
    "name": "secretKeySelectors",
    "required": true,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatibleawscredentials)</sup></sup>

Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#secretkeyselector-v1-core)(s) to reference the Secret(s) that contain the information about the `awsCredentials`. Note that you may use the same or different Secrets for the `accessKeyId` and the `secretAccessKey`. In the former case, the `keys` that identify each must be, obviously, different.


{{< crd-properties >}}
[
  {
    "anchor": "sgobjectstoragespecs3compatibleawscredentialssecretkeyselectorsaccesskeyid",
    "description": "AWS [access key ID](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `AKIAIOSFODNN7EXAMPLE`.",
    "name": "accessKeyId",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecs3compatibleawscredentialssecretkeyselectorssecretaccesskey",
    "description": "AWS [secret access key](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.",
    "name": "secretAccessKey",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgobjectstoragespecs3compatibleawscredentialssecretkeyselectorscacertificate",
    "description": "CA Certificate file to be used when connecting to the S3 Compatible Service.",
    "name": "caCertificate",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectors)</sup></sup>

AWS [access key ID](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `AKIAIOSFODNN7EXAMPLE`.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectors)</sup></sup>

AWS [secret access key](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). For example, `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGObjectStorage.spec.s3Compatible.awsCredentials.secretKeySelectors.caCertificate
<sup><sup>[↩ Parent](#sgobjectstoragespecs3compatibleawscredentialssecretkeyselectors)</sup></sup>

CA Certificate file to be used when connecting to the S3 Compatible Service.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": true,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}
