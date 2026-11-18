---
title: SGDistributedLogs
weight: 7
url: /reference/crd/sgdistributedlogs
description: Details about SGDistributedLogs
showToc: true
---

___

**Kind:** SGDistributedLogs

**listKind:** SGDistributedLogsList

**plural:** sgdistributedlogs

**singular:** sgdistributedlogs

**shortNames** sgdil
___

The `SGDistributedLogs` custom resource represents a distributed logs cluster.
When a Postgres cluster is configured to use distributed logs, all logs from different sources will be forwarded to the distributed logs cluster.
Under the hood, distributed log cluster use an `SGCluster`.
Therefore, the distributed log cluster can be queried using SQL as well, for example using *postgres-util*.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGDistributedLogs
metadata:
  name: distributedlogs
spec:
  persistentVolume:
    size: 10Gi
```

See also [Distribtued Logs section]({{%  relref "04-administration-guide/12-distributed-logs" %}}).

{{% include "generated/SGDistributedLogs.md" %}}
