---
title: SGDbOps
weight: 8
url: /reference/crd/sgdbops
description: Details about SGDbOps
showToc: true
---

___
**Kind:** SGDbOps

**listKind:** SGDbOpsList

**plural:** sgdbops

**singular:** sgdbops

**shortNames** sgdo
___

The `SGDbOps` custom resource represents database operations that are performed on a Postgres cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: benchmark
spec:
 sgCluster: my-cluster
 op: benchmark
 maxRetries: 1
 benchmark:
   type: pgbench
   pgbench:
     databaseSize: 1Gi
     duration: P0DT0H10M0S
     concurrentClients: 10
     threads: 10
   connectionType: primary-service
```

{{% include "generated/SGDbOps.md" %}}
