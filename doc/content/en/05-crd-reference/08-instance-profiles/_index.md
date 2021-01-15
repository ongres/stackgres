---
title: Instance profiles
weight: 8
url: reference/crd/sginstanceprofile
---

The instance profile CR represent the CPU and memory resources assigned to each Pod of the cluster.

___
**Kind:** SGInstanceProfile

**listKind:** SGInstanceProfileList

**plural:** sginstanceprofiles

**singular:** sginstanceprofile
___

**Spec**

| Property | Required | Updatable | Default | Type   | Description |
|:---------|----------|-----------|:--------|:-------|:------------|
| cpu      |          | ✓         | 1       | string | {{< crd-field-description SGInstanceProfile.spec.cpu >}} |
| memory   |          | ✓         | 2Gi     | string | {{< crd-field-description SGInstanceProfile.spec.memory >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
  kind: SGInstanceProfile
  metadata:
    name: size-l
  spec:
    cpu: "4"
    memory: 8Gi
```
