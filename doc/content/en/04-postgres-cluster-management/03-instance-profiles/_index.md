---
title: Instance profiles
weight: 3
---

The Resource profile CR represent the CPU and memory resources assigned to each Pod of the cluster.

___
**Kind:** StackGresProfile

**listKind:** StackGresProfileList

**plural:** sgprofiles

**singular:** sgprofile
___

**Spec**

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| cpu | ✓ | string  | CPU amount to be used  | 1 |
| memory | ✓ | string  | Memory size to be used  | 2Gi |

Example:

```yaml
apiVersion: stackgres.io/v1alpha1
  kind: StackGresProfile
  metadata:
    name: size-l
  spec:
    cpu: "4"
    memory: 8Gi
```
