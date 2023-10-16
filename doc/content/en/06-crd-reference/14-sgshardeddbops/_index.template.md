---
title: SGShardedDbOps
weight: 14
url: /reference/crd/sgshardeddbops
description: Details about SGShardedDbOps
showToc: true
---

___
**Kind:** SGShardedDbOps

**listKind:** SGShardedDbOpsList

**plural:** sgshardeddbops

**singular:** sgshardeddbops

**shortNames** sgsdo
___

The `SGShardedDbOps` custom resource represents database operations that are performed on a Postgres sharded cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: restart
spec:
 sgShardedCluster: my-cluster
 op: restart
 maxRetries: 1
 restart:
   mode: InPlace
```

{{% include "generated/SGShardedDbOps.md" %}}
