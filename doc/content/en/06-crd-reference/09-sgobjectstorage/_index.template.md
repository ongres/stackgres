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

{{% include "generated/SGObjectStorage.md" %}}
