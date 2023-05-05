---
title: Cluster Management
weight: 11
url: features/cluster-management
description: Cluster management, e.g. controlled cluster restarts
---

StackGres fully manages your Postgres clusters in a production-ready manner.

The engineers define the desired target state in the StackGres CRDs, apply changes via the Kubernetes API (or alternatively the web console), and the StackGres operator takes care of the rest.
This approach minimizes careless mistakes from the users side since no procedural steps are required but declarative definitions.
The StackGres CRDs provide a type-safe way to define resources, configurations, and cluster actions, and are understood and validated by the StackGres operator.

Depending on the desired change, StackGres performs changes in a reasonable way, with production-readiness in mind.
This means, for example, that required Postgres cluster restarts are not just performed right away (and potentially jeopardize database availability), but in a controlled manner.

StackGres aims to provide the best combination of a DBA expert and Kubernetes operator.