---
title: Rollout strategy
weight: 15
url: /features/rollout
description: Rollout strategy
---

By default Pods are not re-created nor the Postgres instances restart automatically unless it is required by the high availability mechanism (i.e. when the primary is down or failing). This means that whenever a property that requires restarts (special Postgres parameters or Pod configurations) is changed the Pod will require a restart day-2 operation to be scheduled.

It is possible to configure this rollout strategy in order for a restart of the cluster to happen without requiring to create a restart day 2 operation.

You can configure the rollout strategy of the cluster in the [SGCluster CRD updateStrategy section]({{% relref "06-crd-reference/01-sgcluster#sgclusterspecpodsupdatestartegy" %}}).
