---
title: SGInstanceProfile
weight: 2
url: /reference/crd/sginstanceprofile
description: Details about SGInstanceProfile
shotToc: true
---

___
**Kind:** SGInstanceProfile

**listKind:** SGInstanceProfileList

**plural:** sginstanceprofiles

**singular:** sginstanceprofile

**shortNames** sginp
___

The `SGInstanceProfile` custom resource represents the CPU and memory resources assigned to each pod of the Postgres cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
  kind: SGInstanceProfile
  metadata:
    name: size-l
  spec:
    cpu: "4"
    memory: 8Gi
```

See also [Instance Configuration section]({{% relref "04-administration-guide/04-configuration/02-instance-profile" %}}).

{{% include "generated/SGInstanceProfile.md" %}}
