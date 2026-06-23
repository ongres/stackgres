---
title: Operating clusters
weight: 2
url: /cookbook/operating-clusters
description: Recipes for day-two operations applied to a running SGCluster.
showToc: true
---

The recipes in this section cover **day-two operations**: changes you apply to a cluster
that already exists. Unlike the creation-only options, these fields are reconciled
continuously — you edit the [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}})
(or a referenced custom resource) and the operator converges the running cluster towards
the new desired state.

{{% children style="li" depth="1" description="true" %}}

## What to expect when you change a running cluster

Most changes are applied without downtime, but some have important side effects:

- **Rolling restarts.** Changes that affect the Pod spec (instance profile, resources,
  custom containers, some Postgres settings) are rolled out one Pod at a time, replicas
  first and the primary last, performing a switchover before the primary is restarted.
- **Restart-pending state.** Some Postgres configuration changes require a restart to take
  effect. StackGres marks the cluster as pending-restart and waits for you to run an
  [SGDbOps `restart`]({{% relref "06-crd-reference/08-sgdbops" %}}) operation rather than
  restarting automatically.
- **Update locks.** While an [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) or
  [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}}) operation holds the cluster lock,
  the webhook rejects spec updates to avoid racing with the in-flight operation. Retry once
  the operation finishes.
