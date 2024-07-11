---
title: SGStream
weight: 14
url: /reference/crd/sgstream
description: Details about SGStream
showToc: true
---

___
**Kind:** SGStream

**listKind:** SGStreamList

**plural:** sgstreams

**singular:** sgstream

**shortNames** sgstr
___

The `SGStream` custom resource represents a stream of Change Data Capture (CDC) events from a source database to a target service.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGStream
metadata:
  name: cloudevent
spec:
  source:
    type: SGCluster
    sgCluster:
      name: my-cluster
  target:
    type: CloudEvent
    cloudEvent:
      binding: http
      format: json
      http:
        url: cloudevent-service
```

{{% include "generated/SGStream.md" %}}
