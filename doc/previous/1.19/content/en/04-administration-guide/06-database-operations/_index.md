---
title: Database Operations
weight: 5
url: /administration/database-operations
description: Run database maintenance operations on StackGres clusters using SGDbOps.
---

StackGres supports declarative database operations through the [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) CRD. These operations are executed as Kubernetes Jobs and their progress is tracked in the SGDbOps status.

Available operations:

{{% children style="li" depth="1" description="true" %}}
